package engine.simple.Milestone1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This class maintains a hashmap of terms and contains the positions thereby maintaining the index itself.
 * @author vaibhavjain
 * @version 1.0
 */
public class PositionalIndex {
	private HashMap<String, ArrayList<PositionalPosting>> positionalIndex; 

    public PositionalIndex() {
        positionalIndex = new HashMap<String, ArrayList<PositionalPosting>>();
    }
    
    public void addTerm(String term, int documentID, int positionToAdd) {
		if(existsInMap(term)) {
			ArrayList<PositionalPosting> postingsList = positionalIndex.get(term);
			PositionalPosting posting = postingsList.get(postingsList.size()-1);
			if(posting.getDocId()==documentID)
			{
		        ArrayList<Integer> positionsList =posting.getPositions();
		        positionsList.add(positionToAdd);
		        posting.setPositions(positionsList);
		        postingsList.remove(postingsList.size()-1);
		        postingsList.add(posting);
		        positionalIndex.put(term, postingsList);
			} else
		    {
		    	ArrayList<Integer> tempPositionList = new ArrayList<Integer>();
		    	tempPositionList.add(positionToAdd);
		    	PositionalPosting tempPosting = new PositionalPosting(documentID, tempPositionList);
		    	postingsList.add(tempPosting);
		    	positionalIndex.put(term, postingsList);
		    }
		} else {
			ArrayList<PositionalPosting> tempPostingList = new ArrayList<>();
			ArrayList<Integer> tempPositionList = new ArrayList<>();
			tempPositionList.add(positionToAdd);
			PositionalPosting tempPosting = new PositionalPosting(documentID, tempPositionList);
			tempPostingList.add(tempPosting);
			positionalIndex.put(term, tempPostingList);
		}
	}
     
     public List<PositionalPosting> getPostings(String term) {
        //return the postings list for the given term from the index map.
        if(existsInMap(term)) {
        	return positionalIndex.get(term);
        }
        return null;
     }
     
     private boolean existsInMap(String term) {
    	 return (positionalIndex.containsKey(term))?true:false;
     }
     
     public int getTermCount() {
        //return the number of terms in the index.
        return positionalIndex.keySet().size();
     }
     
     public String[] getDictionary() {
        // fill an array of Strings with all the keys from the hashtable.
        // Sort the array and return it.
    	 Set<String> allKeys = positionalIndex.keySet();
    	 String[] termArray = allKeys.toArray(new String[allKeys.size()]);
    	 Arrays.sort(termArray);
    	 return termArray;
     }
}
