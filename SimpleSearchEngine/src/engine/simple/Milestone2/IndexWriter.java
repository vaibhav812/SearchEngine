package engine.simple.Milestone2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import engine.simple.Milestone1.PositionalIndex;
import engine.simple.Milestone1.PositionalPosting;

/**
 * Writes an inverted indexing of a directory to disk.
*/
public class IndexWriter {
	private String mFolderPath;
	
	/**
   	Constructs an IndexWriter object which is prepared to index the given folder.
	*/
	public IndexWriter(String folderPath) {
		mFolderPath = folderPath;
	}
	
	/**
   	Builds and writes an inverted index to disk. Creates three files: 
   	vocab.bin, containing the vocabulary of the corpus; 
   	postings.bin, containing the postings list of document IDs;
   	vocabTable.bin, containing a table that maps vocab terms to postings locations
	 */
	public void buildIndex(PositionalIndex index) {
		buildIndexForDirectory(index, mFolderPath);
	}
	
	/**
   	Builds the normal PositionalInvertedIndex for the folder.
	*/
	private static void buildIndexForDirectory(PositionalIndex index, String folder) {
		// at this point, "index" contains the in-memory inverted index
		// now we save the index to disk, building three files: the postings index,
		// the vocabulary list, and the vocabulary table.
		
		// the array of terms
		String[] dictionary = index.getDictionary();
		// an array of positions in the vocabulary file
		long[] vocabPositions = new long[dictionary.length];
		
		buildVocabFile(folder, dictionary, vocabPositions);
		buildPostingsFile(folder, index, dictionary, vocabPositions);
	}
	
	/**
   	Builds the postings.bin file for the indexed directory, using the given
   	PositionalInvertedIndex of that directory.
	*/
	private static void buildPostingsFile(String folder, PositionalIndex index, 
			String[] dictionary, long[] vocabPositions) {
		FileOutputStream postingsFile = null;
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream(1000);
		try {
			postingsFile = new FileOutputStream(new File(folder, "postings.bin"));
			
			// simultaneously build the vocabulary table on disk, mapping a term index to a
			// file location in the postings file.
			FileOutputStream vocabTable = new FileOutputStream(new File(folder, "vocabTable.bin"));
         
			//the first thing we must write to the vocabTable file is the number of vocab terms.
			byte[] tSize = ByteBuffer.allocate(4)
					.putInt(dictionary.length).array();
			vocabTable.write(tSize, 0, tSize.length);
			int vocabI = 0;
			for (String s : dictionary) {
				// for each String in dictionary, retrieve its postings.
				List<PositionalPosting> postings = index.getPostings(s);
				
				// write the vocab table entry for this term: the byte location of the term in the vocab list file,
				// and the byte location of the postings for the term in the postings file.
				byte[] vPositionBytes = ByteBuffer.allocate(8)
						.putLong(vocabPositions[vocabI]).array();
				vocabTable.write(vPositionBytes, 0, vPositionBytes.length);

				byte[] pPositionBytes = ByteBuffer.allocate(8).putLong(byteArrayStream.size()).array();
				vocabTable.write(pPositionBytes, 0, pPositionBytes.length);
            
				// write the postings file for this term. first, the document frequency for the term, then
				// the document IDs, encoded as gaps.
				byteArrayStream.write(VariableByteEncoding.encodeNumber(postings.size()));
				int lastDocId = 0;
				for (PositionalPosting posting : postings) {
					ArrayList<Integer> variableEncodingNumberList = new ArrayList<>();
					int docId = posting.getDocId();
					ArrayList<Integer> postingList = posting.getPositions();
					byteArrayStream.write(VariableByteEncoding.encodeNumber(docId - lastDocId));
					double wdt = 1 + Math.log(postingList.size());
					byte[] wdtBytes = ByteBuffer.allocate(8).putDouble(wdt).array();
					byteArrayStream.write(wdtBytes);
					byteArrayStream.write(VariableByteEncoding.encodeNumber(postingList.size()));
					int lastPositionId = 0;
					for(int position : postingList) {
						variableEncodingNumberList.add(position - lastPositionId);
						lastPositionId = position;
					}
					lastDocId = docId;
					byte[] encodedBytes =  VariableByteEncoding.encode(variableEncodingNumberList);
					byteArrayStream.write(encodedBytes);
				}
				vocabI++;
			}
			byteArrayStream.flush();
			postingsFile.write(byteArrayStream.toByteArray());
			vocabTable.close();
			postingsFile.close();
		}
		catch (FileNotFoundException ex) {
		}
		catch (IOException ex) {
		}
		finally {
			try {
				postingsFile.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private static void buildVocabFile(String folder, String[] dictionary,
			long[] vocabPositions) {
		OutputStreamWriter vocabList = null;
		try {
			// first build the vocabulary list: a file of each vocab word concatenated together.
			// also build an array associating each term with its byte location in this file.
			int vocabI = 0;
			vocabList = new OutputStreamWriter(new FileOutputStream(new File(folder, "vocab.bin")), "ASCII");
         
			int vocabPos = 0;
			for (String vocabWord : dictionary) {
				// for each String in dictionary, save the byte position where that term will start in the vocab file.
				vocabPositions[vocabI] = vocabPos;
				vocabList.write(vocabWord); // then write the String
				vocabI++;
				vocabPos += vocabWord.length();
			}
		}
		catch (FileNotFoundException ex) {
			System.out.println(ex.toString());
		}
		catch (UnsupportedEncodingException ex) {
			System.out.println(ex.toString());
		}
		catch (IOException ex) {
			System.out.println(ex.toString());
		}
		finally {
			try {
				vocabList.close();
			}
			catch (IOException ex) {
				System.out.println(ex.toString());
			}
		}
	}
}
