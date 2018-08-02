package engine.simple.Milestone3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;

import engine.simple.Milestone2.BooleanQueryParser;
import engine.simple.Milestone2.QueryFrame;
import engine.simple.Milestone2.RankedQueryParser;

/**
 * A simulator which reads queries from a file and runs either ranked or boolean queries.
 * For the simulator to work, a file named "output.txt" must be present on root level which contains all relevant information in specific format 
 * to calculate MAP, throughput, average response time etc. The best way is to just make a console output redirection to the output.txt file and 
 * everything will work just fine. The queries are read from Cranfield corpus/relevance/queries file.
 * @author vaibhavjain
 *
 */
public class SimulateQueryRun {
	private TieredRankedQueryParser rankedParserTiered = null;
	private TieredBooleanQueryParser booleanParserTiered = null;
	private RankedQueryParser rankedParser= null;
	private BooleanQueryParser booleanParser= null;
	private TieredRankedQueryParserWithScore rankedParserScored = null;
	private TieredBooleanQueryParserWithScore booleanParserWithScore = null;
	private static Scanner scan = new Scanner(System.in);
	
	public static void main(String[] args){
		SimulateQueryRun run = new SimulateQueryRun();
		System.out.println("Select an Option: ");
		System.out.println("1. Boolean Query with Tiered Index ");
		System.out.println("2. Boolean Query with Tiered Index and Score");
		System.out.println("3. Ranked Query with Tiered Index");
		System.out.println("4. Ranked Query with Tiered Index and Score");
		System.out.println("5. Boolean Query with Base Index");
		System.out.println("6. Ranked Query with Base Index");
		int option = scan.nextInt();
		if(option == 1) {
			try {
				run.runSimulatorForBoolean();
				run.averageResults("Time", ":");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(option == 2) {
			try {
				run.runSimulatorForBooleanWithScore();
				run.averageResults("Time", ":");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (option == 3) {
			try {
				run.runSimulatorForRanked();
				run.averageResults("Time", ":");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (option == 4) {
			try {
				run.runSimulatorForRankedWithScore();
				run.averageResults("Time", ":");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(option == 5) {
			try {
				run.runSimulatorForBooleanM2();
				run.averageResults("Time", ":");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (option == 6) {
			try {
				run.runSimulatorRankedM2();
				run.averageResults("Time", ":");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		scan.close();
	}
	
	public void runSimulatorForRanked() throws Exception{
		System.out.println("Enter the index path: ");
		scan = new Scanner(System.in);
		String path = scan.nextLine();
		HashMap<Integer, ArrayList<Integer>> results = formResultMap(path);
		FileReader queryFile = new FileReader(new File(path, "relevance" + File.separator + "queries"));
		BufferedReader bis = new BufferedReader(queryFile);
		QueryFrame qf = new QueryFrame(path);
		String query;
		int queryNum=1;
		ArrayList<Double> meanAvgPrecisionList = new ArrayList<>();
		while((query = bis.readLine()) != null) {
			if(rankedParserTiered == null) {
				rankedParserTiered = new TieredRankedQueryParser(qf.getDiskTieredIndex());
			}
			PriorityQueue<Map.Entry<Integer, Double>> priorQ = rankedParserTiered.parseQuery(query, queryNum);
			CalculateAveragePrecision avgP = new CalculateAveragePrecision(results, priorQ, queryNum);
			meanAvgPrecisionList.add(avgP.calculate());
			queryNum++;
		}
		double avgPrecisionSum = 0;
		for (Double double1 : meanAvgPrecisionList) {
			avgPrecisionSum += double1;
		}
		System.out.println("Mean Average Precision: " + avgPrecisionSum/meanAvgPrecisionList.size());
		averageResults("Non-Zero", ":");
		System.out.println("Percentage of Queries that were satisfied by only top tier: " + topTierPercentage());
		bis.close();
	}
	
	public void runSimulatorForRankedWithScore() throws Exception{
		System.out.println("Enter the index path: ");
		scan = new Scanner(System.in);
		String path = scan.nextLine();
		HashMap<Integer, ArrayList<Integer>> results = formResultMap(path);
		FileReader queryFile = new FileReader(new File(path, "relevance" + File.separator + "queries"));
		BufferedReader bis = new BufferedReader(queryFile);
		QueryFrameWithScore qf = new QueryFrameWithScore(path);
		String query;
		int queryNum=1;
		ArrayList<Double> meanAvgPrecisionList = new ArrayList<>();
		while((query = bis.readLine()) != null) {
			if(rankedParserScored == null) {
				rankedParserScored = new TieredRankedQueryParserWithScore(qf.getDiskIndex());
			}
			PriorityQueue<Map.Entry<Integer, Double>> priorQ = rankedParserScored.parseQuery(query, queryNum);
			CalculateAveragePrecision avgP = new CalculateAveragePrecision(results, priorQ, queryNum);
			meanAvgPrecisionList.add(avgP.calculate());
			queryNum++;
		}
		double avgPrecisionSum = 0;
		for (Double double1 : meanAvgPrecisionList) {
			avgPrecisionSum += double1;
		}
		System.out.println("Mean Average Precision: " + avgPrecisionSum/meanAvgPrecisionList.size());
		averageResults("Non-Zero", ":");
		System.out.println("Percentage of Queries that were satisfied by only top tier: " + topTierPercentage());
		bis.close();
	}
	
	/**
	 * Simulator for boolean queries without score.
	 * @throws Exception
	 */
	public void runSimulatorForBoolean() throws Exception{
		System.out.println("Enter the index path: ");
		scan = new Scanner(System.in);
		String path = scan.nextLine();
		//HashMap<Integer, ArrayList<Integer>> results = formResultMap(path);
		FileReader queryFile = new FileReader(new File(path, "relevance" + File.separator + "queries"));
		BufferedReader bis = new BufferedReader(queryFile);
		QueryFrame qf = new QueryFrame(path);
		String query;
		int queryNum=1;
		while((query = bis.readLine()) != null) {
			if(booleanParserTiered == null) {
				booleanParserTiered = new TieredBooleanQueryParser(qf.getDiskTieredIndex());
			}
			long starttime = System.nanoTime();
			booleanParserTiered.parseQuery(query, 0, false);
			long endtime = System.nanoTime();
			System.out.println("Time for query " + queryNum + " in microseconds: " + ((double)(endtime - starttime)/1000));
			queryNum++;
		}
		bis.close();
	}
	
	/**
	 * Simulator for boolean queries with score.
	 * @throws Exception
	 */
	public void runSimulatorForBooleanWithScore() throws Exception{
		System.out.println("Enter the index path: ");
		scan = new Scanner(System.in);
		String path = scan.nextLine();
		FileReader queryFile = new FileReader(new File(path, "relevance" + File.separator + "queries"));
		BufferedReader bis = new BufferedReader(queryFile);
		QueryFrameWithScore qf = new QueryFrameWithScore(path);
		String query;
		int queryNum=1;
		while((query = bis.readLine()) != null) {
			if(booleanParserWithScore == null) {
				booleanParserWithScore = new TieredBooleanQueryParserWithScore(qf.getDiskIndex());
			}
			long starttime = System.nanoTime();
			booleanParserWithScore.parseQuery(query, 0, false);
			long endtime = System.nanoTime();
			System.out.println("Time for query " + queryNum + " in microseconds: " + ((double)(endtime - starttime)/1000));
			queryNum++;
		}
		bis.close();
	}
	
	public void runSimulatorRankedM2() throws Exception{
		System.out.println("Enter the index path: ");
		scan = new Scanner(System.in);
		String path = scan.nextLine();
		HashMap<Integer, ArrayList<Integer>> results = formResultMap(path);
		FileReader queryFile = new FileReader(new File(path, "relevance" + File.separator + "queries"));
		BufferedReader bis = new BufferedReader(queryFile);
		QueryFrame qf = new QueryFrame(path);
		String query;
		int queryNum=1;
		ArrayList<Double> meanAvgPrecisionList = new ArrayList<>();
		while((query = bis.readLine()) != null) {
			if(rankedParser == null) {
				rankedParser = new RankedQueryParser(qf.getDiskIndex());
			}
			PriorityQueue<Map.Entry<Integer, Double>> priorQ = rankedParser.parseQuery(query, queryNum);
			CalculateAveragePrecision avgP = new CalculateAveragePrecision(results, priorQ, queryNum);
			meanAvgPrecisionList.add(avgP.calculate());
			queryNum++;
		}
		double avgPrecisionSum = 0;
		for (Double double1 : meanAvgPrecisionList) {
			avgPrecisionSum += double1;
		}
		System.out.println("Mean Average Precision: " + avgPrecisionSum/meanAvgPrecisionList.size());
		averageResults("Non-Zero", ":");
		bis.close();
	}
	
	public void runSimulatorForBooleanM2() throws Exception{
		System.out.println("Enter the index path: ");
		scan = new Scanner(System.in);
		String path = scan.nextLine();
		FileReader queryFile = new FileReader(new File(path, "relevance" + File.separator + "queries"));
		BufferedReader bis = new BufferedReader(queryFile);
		QueryFrame qf = new QueryFrame(path);
		String query;
		int queryNum=1;
		while((query = bis.readLine()) != null) {
			if(booleanParser == null) {
				booleanParser = new BooleanQueryParser(qf.getDiskIndex());
			}
			long starttime = System.nanoTime();
			booleanParser.parseQuery(query, 0, false);
			long endtime = System.nanoTime();
			System.out.println("Time for query " + queryNum + " in microseconds: " + ((double)(endtime - starttime)/1000));
			queryNum++;
		}
		bis.close();
	}
	
	/**
	 * Make sure Console Output redirection is enabled. Since results are being printed on console, due to redirection they are also 
	 * saved in output.txt which are then picked up by the following function to process the numbers printed.
	 */
	public void averageResults(String differentiator, String splitter) throws Exception{
		BufferedReader br = new BufferedReader(new FileReader("output.txt"));
		String line;
		double total = 0;
		int lineIndex = 0;
		while((line = br.readLine()) != null) {
			if(line.startsWith(differentiator)) {
				++lineIndex;
				String[] timeArray = line.split(splitter);
				total += Double.parseDouble(timeArray[1]);
			}
		}
		if(differentiator.equals("Time")) {
			double microseconds = total/lineIndex;
			System.out.println("Average time: " + microseconds + " per Query");
			double timeSeconds = microseconds/1000000;
			double numOfQueries = 1/timeSeconds;
			System.out.println("Number of Queries per second: " + numOfQueries);
		} else if(differentiator.equals("Non-Zero")) {
			double average = total/lineIndex;
			System.out.println("The average number of non-zero accumulators used: " + average);
		}
		
		br.close();
	}
	
	/**
	 * Calculates the average percentage of queries that executed using only top tier.
	 * @return
	 * @throws Exception
	 */
	private double topTierPercentage() throws Exception{
		BufferedReader br = new BufferedReader(new FileReader("output.txt"));
		String line;
		int lineIndex = 0;
		int topTierIndex = 0;
		while((line = br.readLine()) != null) {
			if(line.startsWith("Only")) {
				++lineIndex;
				String[] timeArray = line.split(":");
				if(timeArray[1].trim().equals("true")) {
					topTierIndex++;
				}
			}
		}
		br.close();
		return ((double)topTierIndex/lineIndex) * 100;
	}
	
	/**
	 * Gives the relevant results for a query as present in the qrel file.
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public HashMap<Integer, ArrayList<Integer>> formResultMap(String path) throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(new File(path, "relevance" + File.separator + "qrel")));
		HashMap<Integer, ArrayList<Integer>> resultMap = new HashMap<>();
		String line;
		int lineIndex = 1;
		while((line = br.readLine()) != null) {
			String[] resultArray = line.split(" ");
			ArrayList<Integer> resultDocs = new ArrayList<>();
			for (String string : resultArray) {
				resultDocs.add(Integer.parseInt(string));
			}
			resultMap.put(lineIndex, resultDocs);
			++lineIndex;
		}
		br.close();
		return resultMap;
	}
}
