package engine.simple.Milestone3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import engine.simple.Milestone1.SimpleEngine;
import engine.simple.Milestone2.DiskPosting;

/**
 * A copy of TieredRankedQueryParser and integrated scoring.
 * @author vaibhavjain
 *
 */
public class TieredRankedQueryParserWithScore {
	
	private TieredDiskInvertedIndexWithScore index;
	private String dirPath;
	private HashMap<Integer, Double> accumulator;
	private PriorityQueue<Map.Entry<Integer, Double>> priorityQ;
	private boolean isTier1 = true;
	
	public TieredRankedQueryParserWithScore(String dirPath) {
		this();
		this.dirPath = dirPath;
		index = new TieredDiskInvertedIndexWithScore(this.dirPath);
	}
	
	public TieredRankedQueryParserWithScore(TieredDiskInvertedIndexWithScore index) {
		this();
		this.index = index;
		this.dirPath = index.getPath();
	}
	
	public TieredRankedQueryParserWithScore() {
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
		isTier1 = true;
		if(!accumulator.isEmpty())
			accumulator.clear();
		if(!priorityQ.isEmpty())
			priorityQ.clear();
		String[] queryArray = query.split(" ");
		//double totalCorpusSize = QueryFrame.getFileNames().length;
		for(String subQuery : queryArray) {
			//Split the token on hyphen. Doesn't matter if the token actually contains hyphen or not because the
			//following code will run for both. We need to write extra code only for the third token for hyphenated words.
			String[] subQueryArray = subQuery.split("-");
			for(String token : subQueryArray) {
				token = SimpleEngine.removeApostrophes(SimpleEngine.stripNonAlphaNumericForQuery(token));
				String term = SimpleEngine.applyPorter2Stemmer(token);
				//ArrayList<DiskPosting> documents = index.getPostings(term);
				ArrayList<DiskPosting> documents = retrivePostings(term, 1, 0);
				if(documents == null || documents.size() == 0)
					continue;
				for (DiskPosting diskPosting : documents) {
					if(accumulator.containsKey(diskPosting.getDocId())) {
						double score = accumulator.get(diskPosting.getDocId());
						accumulator.put(diskPosting.getDocId(), score + diskPosting.getFinalScore());
					} else {
						accumulator.put(diskPosting.getDocId(), diskPosting.getFinalScore());
					}
				}
			}
			//Following code is for the concatenated word if it contains hyphen.
			if(subQuery.contains("-")) {
				String token = SimpleEngine.removeApostrophes(SimpleEngine.stripNonAlphaNumericForQuery(subQuery.replaceAll("-", "")));
				String term = SimpleEngine.applyPorter2Stemmer(token);
				//ArrayList<DiskPosting> documents = index.getPostings(term);
				ArrayList<DiskPosting> documents = retrivePostings(term, 1, 0);
				if (documents != null && documents.size() != 0) {
					for (DiskPosting diskPosting : documents) {
						if(accumulator.containsKey(diskPosting.getDocId())) {
							double score = accumulator.get(diskPosting.getDocId());
							accumulator.put(diskPosting.getDocId(), score + diskPosting.getFinalScore());
						} else {
							accumulator.put(diskPosting.getDocId(), diskPosting.getFinalScore());
						}
					}
				}
			}
		}
		//Divide each entry in accumulator by Ld
		accumulator.forEach((k, v) -> {
			try {
				double score = accumulator.get(k);
				double ld = index.getWeightForDoc(k);
				double finalScore = score / ld;
				accumulator.put(k, finalScore);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		//Add all accumulator values to heap/priority queue.
		priorityQ.addAll(accumulator.entrySet());
		long endtime = System.nanoTime();
		//docWeightStream.close();
		System.out.println("Non-Zero accumulator value: " + accumulator.size());
		System.out.println("Only top tier: " + isTier1);
		System.out.println("Time for query " + queryNum + " with score in microseconds: " + ((double)(endtime - starttime)/1000));
		return priorityQ;
	}
	
	public ArrayList<DiskPosting> retrivePostings(String term, int tier, int foundNumber) {
		if(tier > 3) return new ArrayList<DiskPosting>();
		if(tier > 1) isTier1 &= false;
		ArrayList<DiskPosting> postingList = index.getPostings(term, tier);
		if(postingList != null && postingList.size() < 20 - foundNumber) {
			postingList.addAll(retrivePostings(term, ++tier, postingList.size() + foundNumber));
		}
		return postingList==null?new ArrayList<>():postingList;
	}
}
