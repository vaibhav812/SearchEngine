package engine.simple.Milestone3;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import engine.simple.Milestone1.SimpleEngine;
import engine.simple.Milestone2.DiskPosting;

/**
 * A query parser for Boolean queries integrated with tiered index.
 * @author vaibhavjain
 * @version 1.0
 */
public class TieredBooleanQueryParser {
	
	private TieredDiskInvertedIndex index;
	private Stack<ArrayList<DiskPosting>> stack = new Stack<>();
	private Pattern phrasePattern = Pattern.compile("(-?\".*?\")|(-?[a-zA-Z0-9]+)");
	
	public TieredBooleanQueryParser(String path) {
		index = new TieredDiskInvertedIndex(path);
	}
	
	public TieredBooleanQueryParser(TieredDiskInvertedIndex index) {
		this.index = index;
	}
	
	public ArrayList<DiskPosting> parseQuery(String query, int kPositions, boolean validQuery) {
		query = query.toLowerCase().trim();
		//Check if the query is valid. Every sub query Q must contain at least one positive literal.
		if(!validQuery) {
			if(validateQuery(query)) {
				validQuery = true;
			} else {
				return null;
			}
		}
		if(query.length() < 1){
			return null;
		}
		//Tokenize the query on 'OR'.
		if(query.contains("+")) {
			String[] subQueryArray = query.split("\\+");
			for (String subQuery : subQueryArray) {
				ArrayList<DiskPosting> result = parseQuery(subQuery.trim(), 0, validQuery);
				if(result != null) {
					stack.push(result);
				}
			}
			//Find the union of the entire stack.
			return findUnionOfStack();
		} else if(query.contains("\"")) {	//Evaluating a Phrase query.
			query = optimizeQuery(query);
			Matcher matcher = phrasePattern.matcher(query);
			ArrayList<ArrayList<DiskPosting>> finalList = new ArrayList<ArrayList<DiskPosting>>();
			ArrayList<ArrayList<DiskPosting>> mergeWithNotList = new ArrayList<>();
			while(matcher.find()) {
				if(matcher.start(1) != -1) {
					if(matcher.group(1).indexOf("\"") == 0) {
						String subQuery = matcher.group(1).replace("\"", "").replace("-","").trim();
						ArrayList<DiskPosting> mergedLiteral = parseAND(subQuery, 1, "literal");
						if(mergedLiteral != null && mergedLiteral.size() > 0) {
							finalList.add(mergedLiteral);
						}
					} else if(matcher.group(1).indexOf("-") == 0) {	//A not-phrase query.
						String notQuery = matcher.group(1).replace("\"", "").replace("-", "").trim();
						ArrayList<DiskPosting> mergedLiteral = parseAND(notQuery, 1, "literal");
						if(mergedLiteral != null ) {
							mergeWithNotList.add(mergedLiteral);
						}
					}
				} else if(matcher.start(2) != -1) {	//A query containing only AND's and no phrase queries. Eg: vanilla shake. Query can be a not query.
					if(matcher.group(2).indexOf("-") != 0) {
						ArrayList<DiskPosting> postings = parseQuery(matcher.group(2).replace("-", "").trim(), kPositions, validQuery);
						if(postings != null) {
							finalList.add(postings);
						}
					} else if(matcher.group(2).indexOf("-") == 0) {
						String notQuery = matcher.group(2).replace("-","").trim();
						//ArrayList<DiskPosting> posting = (ArrayList<DiskPosting>) index.getPostings(normalizeToken(notQuery));
						ArrayList<DiskPosting> posting = (ArrayList<DiskPosting>) retrivePostings(normalizeToken(notQuery), 1);
						if(posting != null){
							mergeWithNotList.add(posting);
						}
					}
				}
			}
			ArrayList<DiskPosting> finalMergedList = new ArrayList<>();
			if(finalList.size() == 1) {
				finalMergedList = finalList.get(0);
			}else if(finalList.size() > 1) {
				//AND the full list. That will be the result of a subquery not containing NOT queries.
				finalMergedList = andFullList(finalList);
			} else {
				return null;
			}
			//If any not queries were encountered, their result is saved in mergeWithNotList. Just remove those postings from the answer.
			for(int i = 0; i < mergeWithNotList.size(); i++) {
				if(finalMergedList.size() > 0) {
					finalMergedList = removeNotList(finalMergedList, mergeWithNotList.get(i));
				} else {
					break;
				}
			}
			return finalMergedList;
		} else if(query.contains(" ")){
			query = optimizeQuery(query);
			return parseAND(query, kPositions, "and");
		} else {
			if(query.contains("-")){
				query = query.replace("-", "");
			}
			//return (ArrayList<DiskPosting>) index.getPostings(normalizeToken(query));
			return (ArrayList<DiskPosting>) retrivePostings(normalizeToken(query), 1);
		}
	}
	
	private String[] breakOnAND(String query) {
		return query.split(" ");
	}
	
	/**
	 * Normalizes a token and returns the respective term.
	 * @param token
	 * @return term
	 */
	public static String normalizeToken(String token) {
		token = SimpleEngine.removeApostrophes(SimpleEngine.stripNonAlphaNumericForQuery((token)));
		String term = SimpleEngine.applyPorter2Stemmer(token);
		return term;
	}
	
	/**
	 * Pases a AND query, merges the result in one list and returns it. 
	 * @param query - The query to perform AND on.
	 * @param kPositions - Value is 1 for a phrase query (to check the next position) and 0 for others (to check same positions). Can be extended into
	 * 						a near/k query.
	 * @param querytype - If it a phrase query or a normal query. 'literal' is used to denote a phrase query.
	 * @return
	 */
	private ArrayList<DiskPosting> parseAND(String query, int kPositions, String querytype) {
		String[] subQueryArray = breakOnAND(query);
		ArrayList<ArrayList<DiskPosting>> mergeList = new ArrayList<ArrayList<DiskPosting>>();
		ArrayList<ArrayList<DiskPosting>> notList = new ArrayList<>();
		for (String subQuery : subQueryArray) {
			if(subQuery.indexOf("-") == 0) {
				subQuery = subQuery.substring(1).replace("-", "");
				ArrayList<DiskPosting> positionList;
				if(querytype.equals("literal")) {
					//positionList = (ArrayList<DiskPosting>) index.getPostingsWithPositions(normalizeToken(subQuery));
					positionList = (ArrayList<DiskPosting>) retrivePostingsWithPositions(normalizeToken(subQuery), 1);
				} else {
					//positionList = (ArrayList<DiskPosting>) index.getPostings(normalizeToken(subQuery));
					positionList = (ArrayList<DiskPosting>) retrivePostings(normalizeToken(subQuery), 1);
				}
				if(positionList != null)
					notList.add(positionList);
				else
					return null;
			} else {
				//For query contains a hypen.
				if(subQuery.contains("-")){
					subQuery = subQuery.replace("-", "");
				}
				ArrayList<DiskPosting> positionList;
				if(querytype.equals("literal")) {
					//positionList = (ArrayList<DiskPosting>) index.getPostingsWithPositions(normalizeToken(subQuery));
					positionList = (ArrayList<DiskPosting>) retrivePostingsWithPositions(normalizeToken(subQuery), 1);
				} else {
					//positionList = (ArrayList<DiskPosting>) index.getPostings(normalizeToken(subQuery));
					positionList = (ArrayList<DiskPosting>) retrivePostings(normalizeToken(subQuery), 1);
				}
				if(positionList != null)
					mergeList.add(positionList);
				else
					return null;
			}
		}
		ArrayList<DiskPosting> finalMergedList = null;
		if(mergeList.size() > 1) {
			if(querytype.equals("literal")) {
				finalMergedList = handlePhraseQuery(mergeList, 1);
			} else {
				finalMergedList = andFullList(mergeList);
			}
		} else if(mergeList.size() == 1) {
			finalMergedList = mergeList.get(0);
		}
		for(int i = 0; i < notList.size(); i++) {
			finalMergedList = removeNotList(finalMergedList, notList.get(i));
		}
		return finalMergedList;
	}
	
	/**
	 * Merges all the lists contained into 1 single list. Performs an AND operaion on all the lists.
	 * @param positionList - A List of all Positional Postings to merge. Positional postings are in the form of List too.
	 * @return - A single merged list.
	 */
	private ArrayList<DiskPosting> andFullList(ArrayList<ArrayList<DiskPosting>> positionList) {
		ArrayList<DiskPosting> mergedList = new ArrayList<>();
		ArrayList<DiskPosting> list1 = positionList.get(0);
		ArrayList<DiskPosting> list2 = positionList.get(1);
		for(int m = 0, n = 0; m < list1.size() && n < list2.size();) {
			DiskPosting p1 = (m >= list1.size())?null:list1.get(m);
			DiskPosting p2 = (n >= list2.size())?null:list2.get(n);
			
			if(p1 != null && p2 != null) {
				if(p1.getDocId() == p2.getDocId()) {
					mergedList.add(new DiskPosting(p1.getDocId(), 0 ,0, p1.getPositions()));
					m++;
					n++;
				} else if(p1.getDocId() < p2.getDocId()) {
					m++;
				} else if(p1.getDocId() > p2.getDocId()) {
					n++;
				}
			}
		}
		int i = 1;
		while(i < positionList.size()-1) {
			i++;
			ArrayList<ArrayList<DiskPosting>> newPositionList = new ArrayList<ArrayList<DiskPosting>>();
			newPositionList.add(mergedList);
			newPositionList.add(positionList.get(i));
			mergedList = andFullList(newPositionList);
		}
		return mergedList;
	}
	
	/**
	 * Handles the merging of the phrase query. Checks positions to evaluate a phrase and hence removes false positives.
	 * @param positionList - A list containing positional posting.
	 * @param kPosition - The interval between 2 phrase words. Default is 1. Can be increased to extend and implement a near/k query.
	 * @return A single merged list of positional posting.
	 */
	private ArrayList<DiskPosting> handlePhraseQuery(ArrayList<ArrayList<DiskPosting>> positionList, int kPosition) {
		ArrayList<DiskPosting> mergedList = new ArrayList<>();
		ArrayList<DiskPosting> list1 = positionList.get(0);
		ArrayList<DiskPosting> list2 = positionList.get(1);
		for(int m = 0, n = 0; m < list1.size() && n < list2.size();) {
			DiskPosting p1 = (m >= list1.size())?null:list1.get(m);
			DiskPosting p2 = (n >= list2.size())?null:list2.get(n);
			
			if(p1 != null && p2 != null) {
				if(p1.getDocId() == p2.getDocId()) {
					ArrayList<Integer> positions1 = p1.getPositions();
					ArrayList<Integer> positions2 = p2.getPositions();
					
					for(int i = 0, j = 0; i < positions1.size() && j < positions2.size();) {
						int val1 = positions1.get(i);
						int val2 = positions2.get(j);
						if(val1 + kPosition == val2) {
							//There is a phrase hit. Now we just check if our merged list already has an entry for the current document.
							//If yes, then we just get the positings of the first term and save it in the list. Else, we create a new entry.
							if(mergedList.size() > 0) {
								DiskPosting mergedPosting= mergedList.get(mergedList.size() - 1);
								if(mergedPosting.getDocId() == p1.getDocId()) {
									ArrayList<Integer> mergedPositions = mergedPosting.getPositions();
									if(mergedPositions != null) {
										mergedPositions.add(val1);
									} else {
										ArrayList<Integer> newMergedPositions = new ArrayList<Integer>();
										newMergedPositions.add(val1);
										mergedPosting.setPositions(newMergedPositions);
									}
								} else {
									ArrayList<Integer> newPositions = new ArrayList<Integer>();
									newPositions.add(val1);
									DiskPosting newPositionPosting = new DiskPosting(p1.getDocId(), 0, 0, newPositions);
									mergedList.add(newPositionPosting);
								}
							} else {
								ArrayList<Integer> newPositions = new ArrayList<Integer>();
								newPositions.add(val1);
								DiskPosting newPositionPosting = new DiskPosting(p1.getDocId(), 0, 0,newPositions);
								mergedList.add(newPositionPosting);
							}
							i++;
							j++;
						} else if((val1+kPosition) < val2) {
							i++;
						} else if((val1 + kPosition) > val2) {
							j++;
						}
					}
					m++;
					n++;
				} else if(p1.getDocId() < p2.getDocId()) {
					m++;
				} else if(p1.getDocId() > p2.getDocId()) {
					n++;
				}
			}
		}
		//Check if there are other terms in the phrase query.
		int i = 1;
		while(i < positionList.size()-1) {
			i++;
			ArrayList<ArrayList<DiskPosting>> newPositionList = new ArrayList<ArrayList<DiskPosting>>();
			newPositionList.add(mergedList);
			newPositionList.add(positionList.get(i));
			mergedList = handlePhraseQuery(newPositionList, kPosition + 1);
			kPosition++;
		}
		return mergedList;
	}

	/**
	 * Implements an OR operation on the stack.
	 * @return
	 */
	private ArrayList<DiskPosting> findUnionOfStack() {
		if(stack.size() == 0) {
			return null;
		} else if(stack.size() == 1) {
			return stack.pop();
		} else {
			ArrayList<DiskPosting> list1 = stack.pop();
			ArrayList<DiskPosting> list2 = stack.pop();
			ArrayList<DiskPosting> mergedList = new ArrayList<>();
			
			for(int i = 0, j = 0; i < list1.size() || j < list2.size();) {
				DiskPosting p1 = (i >= list1.size())?null:list1.get(i);
				DiskPosting p2 = (j >= list2.size())?null:list2.get(j);
				
				if(p1 != null && p2 != null) {
					if(p1.getDocId() == p2.getDocId()) {
						DiskPosting newPosting = new DiskPosting(p1.getDocId(), 0, 0, p1.getPositions());
						mergedList.add(newPosting);
						i++;
						j++;
					} else if(p1.getDocId() < p2.getDocId()) {
						DiskPosting newPosting = new DiskPosting(p1.getDocId(), 0, 0, p1.getPositions());
						mergedList.add(newPosting);
						i++;
					} else if(p1.getDocId() > p2.getDocId()) {
						DiskPosting newPosting = new DiskPosting(p2.getDocId(), 0, 0, p2.getPositions());
						mergedList.add(newPosting);
						j++;
					}
				} else if(p1 != null) {
					List<DiskPosting> subList1 = list1.subList(i, list1.size());
					for (DiskPosting posting : subList1) {
						mergedList.add(new DiskPosting(posting.getDocId(), 0, 0, posting.getPositions()));
					}
					break;
				} else if(p2 != null) {
					List<DiskPosting> subList2 = list2.subList(j, list2.size());
					for (DiskPosting posting : subList2) {
						mergedList.add(new DiskPosting(posting.getDocId(), 0, 0,posting.getPositions()));
					}
					break;
				}
			}
			while(stack.size() > 0) {
				stack.push(mergedList);
				mergedList = findUnionOfStack();
			}
			return mergedList;
		}
	}
	
	/**
	 * Finds the number of positive literal in each sub query and returns true if at least 1 positive literal is present in each sub query.
	 * @param query
	 * @return
	 */
	private boolean validateQuery(String query) {
		int positiveQuery=0; 
		if(query.contains("+")) {
			String[] subQueryArray = query.split("\\+");
			boolean valid = true;
			for (String subQuery : subQueryArray) {
				subQuery = subQuery.trim();
				valid = (valid && validateQuery(subQuery));
			}
			if(valid)
				positiveQuery++;
		} else if(query.contains("\"")) {
			Matcher matcher = phrasePattern.matcher(query);
			while(matcher.find()) {
				if(matcher.start(1) != -1) {
					if(matcher.group(1).indexOf("\"") == 0) {
						positiveQuery++;
					}
				} else if(matcher.start(2) != -1) {
					if(matcher.group(2).indexOf("-") != 0) {
						positiveQuery++;
					}
				}
			}
		} else if(query.contains(" ") && !query.contains("\"")) {
			boolean valid = false;
			String[] subQueryArray = query.split(" ");
			for (String subQuery : subQueryArray) {
				subQuery = subQuery.trim(); 
				valid = (valid || validateQuery(subQuery));
			}
			if(valid)
				positiveQuery++;
		} else if(query.indexOf("-") != 0){
			positiveQuery++;
		}
		return (positiveQuery>0)?true:false;
	}
	
	/**
	 * Used  to optimize a sub query. Optimization is important because the order of query evaluation can greatly reduce work. This is also important
	 * for NOT queries as positive literals should computed first and negative literals must be removed from the result after words. This method simply
	 * rearranges the order of a query to put positive first and negative last. It only reassanges in a sub query so that the rearrangement does not 
	 * impact the final result.
	 * @param query - The sub query to  optimize.
	 * @return A optimized query where positive literals are first and negative literals are last.
	 */
	private String optimizeQuery(String query) {
		Queue<String> queue = new LinkedList<String>();
		Stack<String> stack = new Stack<String>();
		Matcher matcher = phrasePattern.matcher(query);
		while(matcher.find()) {
			if(matcher.start(1) != -1) {
				if(matcher.group(1).indexOf("\"") == 0) {
					queue.add(matcher.group(1).trim());
				} else if(matcher.group(1).indexOf("-") == 0) {
					stack.push(matcher.group(1).trim());
				}
			} else if(matcher.start(2) != -1) {
				if(matcher.group(2).indexOf("-") != 0) {
					queue.add(matcher.group(2).trim());
				} else if(matcher.group(2).indexOf("-") == 0) {
					stack.push(matcher.group(2).trim());
				}
			}
		}
		while(!stack.isEmpty()) {
			queue.add(stack.pop());
		}
		StringBuilder builder = new StringBuilder();
		for (String term : queue) {
			builder.append(term +" ");
		}
		return builder.toString().trim();
	}
	
	/**
	 * Removes the elements of the notlist from the list1.
	 * @param list1 - The list from where the elements are to be removed.
	 * @param notList - The elements to be removed.
	 * @return 
	 */
	private ArrayList<DiskPosting> removeNotList(ArrayList<DiskPosting> list1, ArrayList<DiskPosting> notList) {
		ArrayList<DiskPosting> mergedList = new ArrayList<>();
		int i = 0; 
		int j = 0;
		for(i = 0, j = 0; i < list1.size() && j < notList.size();) {
			DiskPosting p1 = list1.get(i);
			DiskPosting p2 = notList.get(j);
			if(p1.getDocId() < p2.getDocId()) {
				DiskPosting pp = new DiskPosting(p1.getDocId(), 0, 0, p1.getPositions());
				mergedList.add(pp);
				i++;
			} else if(p1.getDocId() > p2.getDocId()) {
				j++;
			} else if(p1.getDocId() == p2.getDocId()) {
				i++;
				j++;
			}
		}
		if(i != list1.size()) {
			mergedList.addAll(list1.subList(i, list1.size()));
		}
		return mergedList;
	}
	
	public ArrayList<DiskPosting> retrivePostings(String term, int tier) {
		ArrayList<DiskPosting> postingList = index.getPostingsWithPositions(term, tier);
		if(tier < 3 && postingList != null) {
			ArrayList<DiskPosting> tieredPostingList = retrivePostings(term, ++tier);
			if(tieredPostingList != null) {
				postingList = mergeLists(postingList, tieredPostingList);
			}
		}
		return postingList;
	}

	public ArrayList<DiskPosting> retrivePostingsWithPositions(String term, int tier) {
		ArrayList<DiskPosting> postingList = index.getPostingsWithPositions(term, tier);
		if(tier < 3 && postingList != null) {
			ArrayList<DiskPosting> tieredPostingList = retrivePostings(term, ++tier);
			if(tieredPostingList != null) {
				postingList = mergeLists(postingList, tieredPostingList);
			}
		}
		return postingList;
	}
	
	public ArrayList<DiskPosting> mergeLists(ArrayList<DiskPosting> list1, ArrayList<DiskPosting> list2) {
		int i = 0, j = 0;
		ArrayList<DiskPosting> mergedList = new ArrayList<>();
		while(i < list1.size() && j < list2.size()) {
			DiskPosting dp1 = list1.get(i);
			DiskPosting dp2 = list2.get(j);
			if(dp1.getDocId() < dp2.getDocId()) {
				mergedList.add(dp1);
				i++;
			} else {
				mergedList.add(dp2);
				j++;
			}
		}
		
		if(i < list1.size()) {
			mergedList.addAll(list1.subList(i, list1.size()));
		}
		
		if(j < list2.size()) {
			mergedList.addAll(list2.subList(j, list2.size()));
		}
		return mergedList;
	}
}
