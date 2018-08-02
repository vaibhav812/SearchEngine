package engine.simple.Milestone1;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A query parser for evaluating a boolean query.
 * 
 * This class has been deprecated in favor of BooleanQueryParser and RankedQueryParser.
 * 
 * @author vaibhavjain
 * @version 1.0.3
 */
@Deprecated
public class QueryParser {
	
	@SuppressWarnings("unused")
	private SimpleEngine engine;
	private PositionalIndex index;
	private Stack<ArrayList<PositionalPosting>> stack = new Stack<>();
	private Pattern phrasePattern = Pattern.compile("(-?\".*?\")|(-?[a-zA-Z0-9]+)");
	
	public QueryParser(SimpleEngine engine) {
		this.engine = engine;
		this.index = engine.index;
	}
	
	public ArrayList<PositionalPosting> parseQuery(String query, int kPositions, boolean validQuery) {
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
				ArrayList<PositionalPosting> result = parseQuery(subQuery.trim(), 0, validQuery);
				if(result != null) {
					stack.push(result);
				}
			}
			//Find the union of the entire stack.
			return findUnionOfStack();
		} else if(query.contains("\"")) {	//Evaluating a Phrase query.
			query = optimizeQuery(query);
			Matcher matcher = phrasePattern.matcher(query);
			ArrayList<ArrayList<PositionalPosting>> finalList = new ArrayList<ArrayList<PositionalPosting>>();
			ArrayList<ArrayList<PositionalPosting>> mergeWithNotList = new ArrayList<>();
			while(matcher.find()) {
				if(matcher.start(1) != -1) {
					if(matcher.group(1).indexOf("\"") == 0) {
						String subQuery = matcher.group(1).replace("\"", "").replace("-","").trim();
						ArrayList<PositionalPosting> mergedLiteral = parseAND(subQuery, 1, "literal");
						if(mergedLiteral != null && mergedLiteral.size() > 0) {
							finalList.add(mergedLiteral);
						}
					} else if(matcher.group(1).indexOf("-") == 0) {	//A not-phrase query.
						String notQuery = matcher.group(1).replace("\"", "").replace("-", "").trim();
						ArrayList<PositionalPosting> mergedLiteral = parseAND(notQuery, 1, "literal");
						if(mergedLiteral != null ) {
							mergeWithNotList.add(mergedLiteral);
						}
					}
				} else if(matcher.start(2) != -1) {	//A query containing only AND's and no phrase queries. Eg: vanilla shake. Query can be a not query.
					if(matcher.group(2).indexOf("-") != 0) {
						ArrayList<PositionalPosting> postings = parseQuery(matcher.group(2).replace("-", "").trim(), kPositions, validQuery);
						if(postings != null) {
							finalList.add(postings);
						}
					} else if(matcher.group(2).indexOf("-") == 0) {
						String notQuery = matcher.group(2).replace("-","").trim();
						ArrayList<PositionalPosting> posting = (ArrayList<PositionalPosting>) index.getPostings(normalizeToken(notQuery));
						if(posting != null){
							mergeWithNotList.add(posting);
						}
					}
				}
			}
			ArrayList<PositionalPosting> finalMergedList = new ArrayList<>();
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
			return (ArrayList<PositionalPosting>) index.getPostings(normalizeToken(query));
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
	private ArrayList<PositionalPosting> parseAND(String query, int kPositions, String querytype) {
		String[] subQueryArray = breakOnAND(query);
		ArrayList<ArrayList<PositionalPosting>> mergeList = new ArrayList<ArrayList<PositionalPosting>>();
		ArrayList<ArrayList<PositionalPosting>> notList = new ArrayList<>();
		for (String subQuery : subQueryArray) {
			if(subQuery.indexOf("-") == 0) {
				subQuery = subQuery.substring(1).replace("-", "");
				ArrayList<PositionalPosting> positionList = (ArrayList<PositionalPosting>) index.getPostings(normalizeToken(subQuery));
				if(positionList != null)
					notList.add(positionList);
				else
					return null;
			} else {
				//For query contains a hypen.
				if(subQuery.contains("-")){
					subQuery = subQuery.replace("-", "");
				}
				ArrayList<PositionalPosting> positionList = (ArrayList<PositionalPosting>) index.getPostings(normalizeToken(subQuery));
				if(positionList != null)
					mergeList.add(positionList);
				else
					return null;
			}
		}
		ArrayList<PositionalPosting> finalMergedList = null;
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
	private ArrayList<PositionalPosting> andFullList(ArrayList<ArrayList<PositionalPosting>> positionList) {
		ArrayList<PositionalPosting> mergedList = new ArrayList<>();
		ArrayList<PositionalPosting> list1 = positionList.get(0);
		ArrayList<PositionalPosting> list2 = positionList.get(1);
		for(int m = 0, n = 0; m < list1.size() && n < list2.size();) {
			PositionalPosting p1 = (m >= list1.size())?null:list1.get(m);
			PositionalPosting p2 = (n >= list2.size())?null:list2.get(n);
			
			if(p1 != null && p2 != null) {
				if(p1.getDocId() == p2.getDocId()) {
					mergedList.add(new PositionalPosting(p1.getDocId(), p1.getPositions()));
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
			ArrayList<ArrayList<PositionalPosting>> newPositionList = new ArrayList<ArrayList<PositionalPosting>>();
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
	private ArrayList<PositionalPosting> handlePhraseQuery(ArrayList<ArrayList<PositionalPosting>> positionList, int kPosition) {
		ArrayList<PositionalPosting> mergedList = new ArrayList<>();
		ArrayList<PositionalPosting> list1 = positionList.get(0);
		ArrayList<PositionalPosting> list2 = positionList.get(1);
		for(int m = 0, n = 0; m < list1.size() && n < list2.size();) {
			PositionalPosting p1 = (m >= list1.size())?null:list1.get(m);
			PositionalPosting p2 = (n >= list2.size())?null:list2.get(n);
			
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
								PositionalPosting mergedPosting= mergedList.get(mergedList.size() - 1);
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
									PositionalPosting newPositionPosting = new PositionalPosting(p1.getDocId(), newPositions);
									mergedList.add(newPositionPosting);
								}
							} else {
								ArrayList<Integer> newPositions = new ArrayList<Integer>();
								newPositions.add(val1);
								PositionalPosting newPositionPosting = new PositionalPosting(p1.getDocId(), newPositions);
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
			ArrayList<ArrayList<PositionalPosting>> newPositionList = new ArrayList<ArrayList<PositionalPosting>>();
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
	private ArrayList<PositionalPosting> findUnionOfStack() {
		if(stack.size() == 0) {
			return null;
		} else if(stack.size() == 1) {
			return stack.pop();
		} else {
			ArrayList<PositionalPosting> list1 = stack.pop();
			ArrayList<PositionalPosting> list2 = stack.pop();
			ArrayList<PositionalPosting> mergedList = new ArrayList<>();
			
			for(int i = 0, j = 0; i < list1.size() || j < list2.size();) {
				PositionalPosting p1 = (i >= list1.size())?null:list1.get(i);
				PositionalPosting p2 = (j >= list2.size())?null:list2.get(j);
				
				if(p1 != null && p2 != null) {
					if(p1.getDocId() == p2.getDocId()) {
						PositionalPosting newPosting = new PositionalPosting(p1.getDocId(), p1.getPositions());
						mergedList.add(newPosting);
						i++;
						j++;
					} else if(p1.getDocId() < p2.getDocId()) {
						PositionalPosting newPosting = new PositionalPosting(p1.getDocId(), p1.getPositions());
						mergedList.add(newPosting);
						i++;
					} else if(p1.getDocId() > p2.getDocId()) {
						PositionalPosting newPosting = new PositionalPosting(p2.getDocId(), p2.getPositions());
						mergedList.add(newPosting);
						j++;
					}
				} else if(p1 != null) {
					List<PositionalPosting> subList1 = list1.subList(i, list1.size());
					for (PositionalPosting posting : subList1) {
						mergedList.add(new PositionalPosting(posting.getDocId(), posting.getPositions()));
					}
					break;
				} else if(p2 != null) {
					List<PositionalPosting> subList2 = list2.subList(j, list2.size());
					for (PositionalPosting posting : subList2) {
						mergedList.add(new PositionalPosting(posting.getDocId(), posting.getPositions()));
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
	private ArrayList<PositionalPosting> removeNotList(ArrayList<PositionalPosting> list1, ArrayList<PositionalPosting> notList) {
		ArrayList<PositionalPosting> mergedList = new ArrayList<>();
		int i = 0; 
		int j = 0;
		for(i = 0, j = 0; i < list1.size() && j < notList.size();) {
			PositionalPosting p1 = list1.get(i);
			PositionalPosting p2 = notList.get(j);
			if(p1.getDocId() < p2.getDocId()) {
				PositionalPosting pp = new PositionalPosting(p1.getDocId(), p1.getPositions());
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
}
