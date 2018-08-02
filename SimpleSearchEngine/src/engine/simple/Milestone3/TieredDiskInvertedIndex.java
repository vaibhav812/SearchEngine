package engine.simple.Milestone3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import engine.simple.Milestone2.DiskPosting;
import engine.simple.Milestone2.VariableByteEncoding;

/**
 * A copy of DikInvertedIndex modified to integrate Tiered Indexing.
 * @author vaibhavjain
 *
 */
public class TieredDiskInvertedIndex {

	private String mPath;
	private RandomAccessFile mVocabList, mDocWeights;
	private RandomAccessFile mPostings, mPostings1, mPostings2, mPostings3;
	private long[] mVocabTable, mVocabTable1, mVocabTable2, mVocabTable3;

	// Opens a disk inverted index that was constructed in the given path.
	public TieredDiskInvertedIndex(String path) {
		try {
			mPath = path;
			mVocabList = new RandomAccessFile(new File(path, "vocab.bin"), "r");
			mPostings1 = new RandomAccessFile(new File(path, "postings1.bin"), "r");
			mPostings2 = new RandomAccessFile(new File(path, "postings2.bin"), "r");
			mPostings3 = new RandomAccessFile(new File(path, "postings3.bin"), "r");
			mVocabTable1 = readVocabTable(path, "vocabTable1.bin");
			mVocabTable2 = readVocabTable(path, "vocabTable2.bin");
			mVocabTable3 = readVocabTable(path, "vocabTable3.bin");
			mDocWeights = new RandomAccessFile(new File(path, "docWeights.bin"), "r");
		}
		catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	public ArrayList<DiskPosting> readPostingsFromFile(RandomAccessFile postingsFile, long postingsPosition) {
		ArrayList<DiskPosting> postingsList = null;
		try {
			postingsFile.seek(postingsPosition);
			int totaldf = VariableByteEncoding.decodeNextNumber(postingsFile);
			int documentFrequency = VariableByteEncoding.decodeNextNumber(postingsFile);
			postingsList = new ArrayList<>(documentFrequency);
			int lastDocId = 0, docId, termFreq;
			for (int i = 0; i < documentFrequency; i++) {
				docId = VariableByteEncoding.decodeNextNumber(postingsFile) + lastDocId;
				termFreq = VariableByteEncoding.decodeNextNumber(postingsFile);
				postingsList.add(new DiskPosting(totaldf, docId, 0, termFreq, null));
				for(int j = 0; j < termFreq; j++) {
					VariableByteEncoding.decodeNextNumber(postingsFile);
				}
				lastDocId = docId;
			}	
		} catch(Exception e) {
			e.printStackTrace();
		}
		return postingsList;
	}
   
	/**
	 * Reads postings for a particular term from postings.bin file. The returned array does not contain positions.
	 * @param term
	 * @return ArrayList
	 */
	public ArrayList<DiskPosting> getPostings(String term, int tier) {
		if(tier == 1) {
			mVocabTable = mVocabTable1;
			mPostings = mPostings1;
		} else if(tier == 2) {
			mVocabTable = mVocabTable2;
			mPostings = mPostings2;
		} else if(tier == 3) {
			mVocabTable = mVocabTable3;
			mPostings = mPostings3;
		}
		long postingsPosition = binarySearchVocabulary(term);
		if (postingsPosition >= 0) {
			return readPostingsFromFile(mPostings, postingsPosition);
		}
		return null;
	}
	
	/**
	 * Reads postings for a particular term from postings.bin file. The returned array includes positions too.
	 * @param term
	 * @return ArrayList
	 */
	public ArrayList<DiskPosting> getPostingsWithPositions(String term, int tier) {
		if(tier == 1) {
			mVocabTable = mVocabTable1;
			mPostings = mPostings1;
		} else if(tier == 2) {
			mVocabTable = mVocabTable2;
			mPostings = mPostings2;
		} else if(tier == 3) {
			mVocabTable = mVocabTable3;
			mPostings = mPostings3;
		}
		long postingsPosition = binarySearchVocabulary(term);
		if (postingsPosition >= 0) {
			return readPostingsWithPositionsFromFile(mPostings, postingsPosition);
		}
		return null; 
	}
	
	/**
	 * Returns the actual posting list for a particular term.
	 * @param postings - A file handler to postings.bin file
	 * @param postingsPosition - A long value indicating the start position for this particular posting.
	 * @return
	 */
	private ArrayList<DiskPosting> readPostingsWithPositionsFromFile(RandomAccessFile postings, long postingsPosition) {
		ArrayList<DiskPosting> postingsList;
		try {
			postings.seek(postingsPosition);
			int totaldf = VariableByteEncoding.decodeNextNumber(postings);
			int documentFrequency = VariableByteEncoding.decodeNextNumber(postings);
         
			postingsList = new ArrayList<>(documentFrequency);

			int lastDocId = 0, docId, termFreq;
			for (int i = 0; i < documentFrequency; i++) {
				docId = VariableByteEncoding.decodeNextNumber(postings) + lastDocId;
				termFreq = VariableByteEncoding.decodeNextNumber(postings);
				
				ArrayList<Integer> positions = new ArrayList<Integer>(termFreq);
				int position = 0, lastPosition = 0;
				while(position < termFreq) {
					int currentPosition = VariableByteEncoding.decodeNextNumber(postings);
					positions.add(currentPosition + lastPosition);
					lastPosition += currentPosition;
					position++;
				}
				postingsList.add(new DiskPosting(totaldf, docId, 0, termFreq, positions));
				lastDocId = docId;
			}
			return postingsList;
		}
		catch (IOException ex) {
			System.out.println(ex.toString());
		}	
		return null;
	}
   
	// Locates the byte position of the postings for the given term.
	private long binarySearchVocabulary(String term) {
		// do a binary search over the vocabulary, using the vocabTable and the file vocabList.
		int i = 0, j = mVocabTable.length / 2 - 1;
		while (i <= j) {
			try {
				int m = (i + j) / 2;
				long vListPosition = mVocabTable[m * 2];
				int termLength;
				if (m == mVocabTable.length / 2 - 1) {
					termLength = (int)(mVocabList.length() - mVocabTable[m*2]);
				}	
				else {
					termLength = (int) (mVocabTable[(m + 1) * 2] - vListPosition);
				}

				mVocabList.seek(vListPosition);
				
				byte[] buffer = new byte[termLength];
				mVocabList.read(buffer, 0, termLength);
				String fileTerm = new String(buffer, "ASCII");
				
				int compareValue = term.compareTo(fileTerm);
				if (compareValue == 0) {
					// found it!
					return mVocabTable[m * 2 + 1];
				}
				else if (compareValue < 0) {
					j = m - 1;
				}
				else {
					i = m + 1;
				}
			}
			catch (IOException ex) {
				System.out.println(ex.toString());
			}
		}
		return -1;
	}

	// Reads the file vocabTable.bin into memory.
	private static long[] readVocabTable(String indexName, String fileName) {
		try {
			long[] vocabTable;
			
			RandomAccessFile tableFile = new RandomAccessFile(
					new File(indexName, fileName),
					"r");	
         
			byte[] byteBuffer = new byte[4];
			tableFile.read(byteBuffer, 0, byteBuffer.length);
        
			int tableIndex = 0;
			vocabTable = new long[ByteBuffer.wrap(byteBuffer).getInt() * 2];
			byteBuffer = new byte[8];
         
			while (tableFile.read(byteBuffer, 0, byteBuffer.length) > 0) { // while we keep reading 4 bytes
				vocabTable[tableIndex] = ByteBuffer.wrap(byteBuffer).getLong();
				tableIndex++;
			}
			tableFile.close();
			return vocabTable;
		}
		catch (FileNotFoundException ex) {
			System.out.println(ex.toString());
		}
		catch (IOException ex) {
			System.out.println(ex.toString());
		}
		return null;
	}
   
	public int getTermCount() {
		return mVocabTable.length / 2;
	}
	
	/**
	 * Returns a String array of vocabulary terms in the index.
	 * @return String[]
	 */
	public String[] getDictionary() {
        String[] dictionary = new String[getTermCount()];
        int i = 0;
        long vListPosition;
        int termLength;
        while (i < getTermCount()) {
            try {
                vListPosition = mVocabTable[i * 2];
                if (i == mVocabTable.length / 2 - 1) {
                    termLength = (int) (mVocabList.length() - mVocabTable[i * 2]);
                } else {
                    termLength = (int) (mVocabTable[(i + 1) * 2] - vListPosition);
                }
                mVocabList.seek(vListPosition);
                
                byte[] buffer = new byte[termLength];
                mVocabList.read(buffer, 0, termLength);
                dictionary[i] = new String(buffer, "ASCII");
                i++;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return dictionary;
    }
	
	public String getPath() {
		return mPath;
	}
	
	public double getWeightForDoc(int docId) {
		double ld = 0;
		try{
			mDocWeights.seek(docId * 8);
			byte[] ldByteBuffer = new byte[8];
			mDocWeights.read(ldByteBuffer);
			ld = ByteBuffer.wrap(ldByteBuffer).getDouble();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return ld;
	}
}
