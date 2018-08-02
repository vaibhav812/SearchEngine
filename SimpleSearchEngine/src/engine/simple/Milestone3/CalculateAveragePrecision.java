package engine.simple.Milestone3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Calculates Average Precision of a single query.
 * @author vaibhavjain
 *
 */
public class CalculateAveragePrecision {
	private HashMap<Integer, ArrayList<Integer>> results;
	private PriorityQueue<Map.Entry<Integer, Double>> priorQ;
	private int queryNum;
	
	public CalculateAveragePrecision(HashMap<Integer, ArrayList<Integer>> results, PriorityQueue<Map.Entry<Integer, Double>> priorQ,
			int queryNum) {
		this.results = results;
		this.priorQ = priorQ;
		this.queryNum = queryNum;
	}
	
	public double calculate() {
		int totalRelevant = results.get(queryNum).size();
		double combinedPrecision = calculatePatI();
		double averagePrecision = combinedPrecision/totalRelevant;
		return averagePrecision;
	}
	
	private boolean isRelevant(int docId) {
		ArrayList<Integer> relavantDocs =  results.get(queryNum);
		if(relavantDocs.contains(docId))
			return true;
		else
			return false;
	}
	
	/**
	 * Calculates P@i for a query. Max value of i is 20.
	 * @return
	 */
	private double calculatePatI() {
		int i = 1;
		double patINum = 0;
		double patIDenom = 0;
		double combinedPrecision = 0;
		while(i <= 20) {
			Map.Entry<Integer, Double> entry = priorQ.remove();
			int docId = entry.getKey() + 1;
			if(isRelevant(docId)) {
				++patINum;
				++patIDenom;
				combinedPrecision += patINum/patIDenom;
			} else {
				++patIDenom;
			}
			++i;
		}
		return combinedPrecision;
	}
}
