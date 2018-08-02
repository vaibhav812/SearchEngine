package engine.simple.Milestone2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import engine.simple.Milestone1.SimpleEngine;

/**
 * Parser for parsing querying using ranked retrieval mode.
 * @author vaibhavjain
 * @version 1.1
 */
public class RankedQueryParser {
	
	private DiskInvertedIndex index;
	private String dirPath;
	private HashMap<Integer, Double> accumulator;
	private PriorityQueue<Map.Entry<Integer, Double>> priorityQ;
	
	public RankedQueryParser(String dirPath) {
		this();
		this.dirPath = dirPath;
		index = new DiskInvertedIndex(this.dirPath);
	}
	
	public RankedQueryParser(DiskInvertedIndex index) {
		this();
		this.index = index;
		this.dirPath = index.getPath();
	}
	
	public RankedQueryParser() {
		accumulator = new HashMap<>();
		priorityQ = new PriorityQueue<>(new Comparator<Entry<Integer, Double>>() {

			@Override
			public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
				if(o1.getValue() < o2.getValue())
					return 1;
				else if (o1.getValue() > o2.getValue())
					return -1;
				else return 0;
			}
		});

	}
	
	/**
	 * Parses queries using the 'term at a time algorithm'.
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public PriorityQueue<Map.Entry<Integer, Double>> parseQuery(String query, int queryNum) throws IOException{
		long starttime = System.nanoTime();
		if(!accumulator.isEmpty())
			accumulator.clear();
		if(!priorityQ.isEmpty())
			priorityQ.clear();
		RandomAccessFile docWeightStream = new RandomAccessFile(new File(dirPath, "docWeights.bin"), "r");
		String[] queryArray = query.split(" ");
		double totalCorpusSize = QueryFrame.getFileNames().length;
		for(String subQuery : queryArray) {
			//Split the token on hyphen. Doesn't matter if the token actually contains hyphen or not because the
			//following code will run for both. We need to write extra code only for the third token for hyphenated words.
			String[] subQueryArray = subQuery.split("-");
			for(String token : subQueryArray) {
				token = SimpleEngine.removeApostrophes(SimpleEngine.stripNonAlphaNumericForQuery(token));
				String term = SimpleEngine.applyPorter2Stemmer(token);
				ArrayList<DiskPosting> documents = index.getPostings(term);
				if(documents == null || documents.size() == 0)
					continue;
				double docFreq = (double) documents.size();
				double wqt = Math.log(1 + (totalCorpusSize / docFreq));
				for(DiskPosting posting : documents) {
					//double wdt = 1 + Math.log10(posting.getTermFreq());
					double wdt = posting.getWdt();
					if(accumulator.containsKey(posting.getDocId())) {
						double score = accumulator.get(posting.getDocId());
						score = score + (wdt * wqt);
						accumulator.put(posting.getDocId(), score);
					} else {
						double score = wdt * wqt;
						accumulator.put(posting.getDocId(), score);
					}
				}
			}
			//Following code is for the concatenated word if it contains hyphen.
			if(subQuery.contains("-")) {
				String token = SimpleEngine.removeApostrophes(SimpleEngine.stripNonAlphaNumericForQuery(subQuery.replaceAll("-", "")));
				String term = SimpleEngine.applyPorter2Stemmer(token);
				ArrayList<DiskPosting> documents = index.getPostings(term);
				if (documents != null && documents.size() != 0) {
					double docFreq = (double) documents.size();
					double wqt = Math.log(1 + (totalCorpusSize / docFreq));
					for(DiskPosting posting : documents) {
						double wdt = 1 + Math.log(posting.getTermFreq());
						if(accumulator.containsKey(posting.getDocId())) {
							double score = accumulator.get(posting.getDocId());
							score = score + (wdt * wqt);
							accumulator.put(posting.getDocId(), score);
						} else {
							double score = wdt * wqt;
							accumulator.put(posting.getDocId(), score);
						}
					}
				}
			}
		}
		//Divide each entry in accumulator by Ld
		accumulator.forEach((k, v) -> {
			try {
				//Locate the appropriate Ld value position.
				docWeightStream.seek(k * 8);
				byte[] ldByteBuffer = new byte[8];
				docWeightStream.read(ldByteBuffer);
				double ld = ByteBuffer.wrap(ldByteBuffer).getDouble();
				double score = accumulator.get(k);
				double finalScore = score / ld;
				accumulator.put(k, finalScore);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		//Add all accumulator values to heap/priority queue.
		priorityQ.addAll(accumulator.entrySet());
		long endtime = System.nanoTime();
		System.out.println("Non-Zero accumulator value: " + accumulator.size());
		System.out.println("Time for query " + queryNum + " in microseconds: " + ((double)(endtime - starttime)/1000));
		docWeightStream.close();
		return priorityQ;
	}
}
