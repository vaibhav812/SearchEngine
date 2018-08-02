package engine.simple.Milestone3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import engine.simple.Milestone1.PositionalIndex;
import engine.simple.Milestone1.PositionalPosting;
import engine.simple.Milestone2.DiskPosting;
import engine.simple.Milestone2.VariableByteEncoding;

/**
 * A copy of TieredIndexWriter and also integrates scoring.
 * @author vaibhavjain
 *
 */
public class TieredIndexWriterWithScore {

	private String mFolderPath;
	private static RandomAccessFile mDocWeights;
	private static double totalCorpusSize;
	/**
   	Constructs a TieredIndexWriter object which is prepared to index the given folder.
	*/
	public TieredIndexWriterWithScore(String folderPath, double totalCorpusSize) {
		mFolderPath = folderPath;
		TieredIndexWriterWithScore.totalCorpusSize = totalCorpusSize;
		try {
			mDocWeights = new RandomAccessFile(new File(folderPath, "docWeights.bin"), "r");
		} catch (FileNotFoundException fe) {
			fe.printStackTrace();
		}
	}
	
	public void buildIndex(PositionalIndex index) {
		buildIndexForDirectory(index, mFolderPath);
	}
	
	private static void buildIndexForDirectory(PositionalIndex index, String folder) {
		// at this point, "index" contains the in-memory inverted index
		// now we save the index to disk, building three files: the postings index,
		// the vocabulary list, and the vocabulary table.
		// the array of terms
		String[] dictionary = index.getDictionary();
		long[] vocabPositions = new long[dictionary.length];
		
		buildVocabFile(folder, dictionary, vocabPositions);
		buildPostingsFile(folder, index, dictionary, vocabPositions);
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
	
	private static void buildPostingsFile(String folder, PositionalIndex index, 
			String[] dictionary, long[] vocabPositions) {
		
		ByteArrayOutputStream posting1Stream = new ByteArrayOutputStream(1000);
		ByteArrayOutputStream posting2Stream = new ByteArrayOutputStream(1000);
		ByteArrayOutputStream posting3Stream = new ByteArrayOutputStream(1000);
		ByteArrayOutputStream vocab1Stream = new ByteArrayOutputStream(1000);
		ByteArrayOutputStream vocab2Stream = new ByteArrayOutputStream(1000);
		ByteArrayOutputStream vocab3Stream = new ByteArrayOutputStream(1000);
		int vocabI = 0;
		//the first thing we must write to the vocabTable file is the number of vocab terms.
		byte[] tSize = ByteBuffer.allocate(4).putInt(dictionary.length).array();
		vocab1Stream.write(tSize, 0, tSize.length);
		vocab2Stream.write(tSize, 0, tSize.length);
		vocab3Stream.write(tSize, 0, tSize.length);
		for (String s : dictionary) {
			List<PositionalPosting> postingList = index.getPostings(s);
			HashMap<Integer, DiskPosting> accumulator = new HashMap<>();
			PriorityQueue<DiskPosting> priorityQueue = new PriorityQueue<>(new Comparator<DiskPosting>() {

				@Override
				public int compare(DiskPosting o1, DiskPosting o2) {
					if(o1.getFinalScore() < o2.getFinalScore())
						return 1;
					else if (o1.getFinalScore() > o2.getFinalScore())
						return -1;
					else return 0;
				}
			});
			//double totalCorpusSize = QueryFrame.getFileNames().length;
			double docFreq = postingList.size();
			double wqt = Math.log(1 + (totalCorpusSize / docFreq));
			//Compute score for all documents.
			for (PositionalPosting posting : postingList) {
				double wdt = 1 + Math.log(posting.getPositions().size());
				if(accumulator.containsKey(posting.getDocId())) {
					DiskPosting dp = accumulator.get(posting.getDocId());
					double score = dp.getFinalScore();
					score = score + (wdt * wqt);
					dp.setFinalScore(score);
					accumulator.put(dp.getDocId(), dp);
				} else {
					double score = wdt * wqt;
					accumulator.put(posting.getDocId(), new DiskPosting(posting.getDocId(), posting.getPositions().size(), posting.getPositions(), score));
				}
			}
			/*accumulator.forEach((k, v) -> {
				try {
					DiskPosting dp = accumulator.get(k);
					double score = dp.getFinalScore();
					double ld = getWeightForDoc(k);
					double finalScore = score / ld;
					dp.setFinalScore(finalScore);
					accumulator.put(k, dp);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});*/
			priorityQueue.addAll(accumulator.values());
			int size = priorityQueue.size();
			//Tier1
			writeToStream(priorityQueue, posting1Stream, vocab1Stream, vocabPositions, size, Math.ceil((double)size/10), vocabI);
			//Tier2
			writeToStream(priorityQueue, posting2Stream, vocab2Stream, vocabPositions, size, Math.ceil((double)size/3), vocabI);
			//Tier3
			writeToStream(priorityQueue, posting3Stream, vocab3Stream, vocabPositions, size, priorityQueue.size(), vocabI);
			vocabI++;
		}
		writeToFile(posting1Stream, folder, "postings1.bin");
		writeToFile(vocab1Stream, folder, "vocabTable1.bin");
		writeToFile(posting2Stream, folder, "postings2.bin");
		writeToFile(vocab2Stream, folder, "vocabTable2.bin");
		writeToFile(posting3Stream, folder, "postings3.bin");
		writeToFile(vocab3Stream, folder, "vocabTable3.bin");
	}
	
	private static void writeToStream(PriorityQueue<DiskPosting> priorityQueue, ByteArrayOutputStream postingStream, 
			ByteArrayOutputStream vocabStream, long[] vocabPositions, int size, double totalNumberToWrite, int vocabI) {
		int writeIndex = 0;
		ArrayList<DiskPosting> reducedList = new ArrayList<>();
		if(priorityQueue.isEmpty()){
			//No more docs to write. So we put in a -1 in postings so that we know that this term is not present in this tier. The
			//reason for doing it this way is because it makes sure all vocabTables contain same number of terms.
			try{
				byte[] vPositionBytes = ByteBuffer.allocate(8).putLong(vocabPositions[vocabI]).array();
				vocabStream.write(vPositionBytes);
	
				byte[] pPositionBytes = ByteBuffer.allocate(8).putLong(-1).array();
				vocabStream.write(pPositionBytes);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		//Add only those many terms as specified by the tier range.
		while(writeIndex < totalNumberToWrite && !priorityQueue.isEmpty()) {
			reducedList.add(priorityQueue.remove());
			writeIndex++;
		}
		
		//Sort with doc ID.
		Collections.sort(reducedList);
		
		try {
			byte[] vPositionBytes = ByteBuffer.allocate(8)
					.putLong(vocabPositions[vocabI]).array();
			vocabStream.write(vPositionBytes);

			byte[] pPositionBytes = ByteBuffer.allocate(8).putLong(postingStream.size()).array();
			vocabStream.write(pPositionBytes);
			//Write the total document freq across tiers
			//postingStream.write(VariableByteEncoding.encodeNumber(size));
			//Total doc freq in this tier
			postingStream.write(VariableByteEncoding.encodeNumber(reducedList.size()));
			int lastDocId = 0;
			for (DiskPosting posting : reducedList) {
				ArrayList<Integer> variableEncodingNumberList = new ArrayList<>();
				int docId = posting.getDocId();
				ArrayList<Integer> postingList = posting.getPositions();
				postingStream.write(VariableByteEncoding.encodeNumber(docId - lastDocId));
				//double wdt = 1 + Math.log(postingList.size());
				//byte[] wdtBytes = ByteBuffer.allocate(8).putDouble(wdt).array();
				//byteArrayStream.write(wdtBytes);
				byte[] scoreBytes = ByteBuffer.allocate(8).putDouble(posting.getFinalScore()).array();
				postingStream.write(scoreBytes);
				postingStream.write(VariableByteEncoding.encodeNumber(postingList.size()));
				int lastPositionId = 0;
				for(int position : postingList) {
					variableEncodingNumberList.add(position - lastPositionId);
					lastPositionId = position;
				}
				lastDocId = docId;
				byte[] encodedBytes =  VariableByteEncoding.encode(variableEncodingNumberList);
				postingStream.write(encodedBytes);
			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private static void writeToFile(ByteArrayOutputStream stream, String folder, String filePath) {
		
		try{
			FileOutputStream fos = new FileOutputStream(new File(folder, filePath));
			stream.flush();
			fos.write(stream.toByteArray());
			fos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static double getWeightForDoc(int docId) {
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
