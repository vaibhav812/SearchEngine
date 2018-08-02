package engine.simple.Milestone1;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A Java bean class for holding document id and its respective postings.
 * @author vaibhavjain
 *
 */
public class PositionalPosting implements Serializable{

	private static final long serialVersionUID = 1L;
	private int docId;
    private ArrayList<Integer> positionsList;

    public PositionalPosting(int docId, ArrayList<Integer> positionsList) {
        this.docId = docId;
        this.positionsList = positionsList;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public ArrayList<Integer> getPositions() {
        return positionsList;
    }

    public void setPositions(ArrayList<Integer> positionsList) {
        this.positionsList = positionsList;
    }
}
