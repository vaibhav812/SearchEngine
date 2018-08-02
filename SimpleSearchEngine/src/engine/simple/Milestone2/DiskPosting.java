package engine.simple.Milestone2;

import java.util.ArrayList;

public class DiskPosting implements Comparable<DiskPosting>{
	
	private int docId;
	private int termFreq;
	private double wdt;
	private ArrayList<Integer> positionsList;
	private int tdf;
	private double finalScore;

	public DiskPosting(int docId, double wdt, int termFreq, ArrayList<Integer> positionsList) {
        this.docId = docId;
        this.wdt = wdt;
        this.termFreq = termFreq;
        this.positionsList = positionsList;
    }
	
	public DiskPosting(int tdf, int docId, double wdt, int termFreq, ArrayList<Integer> positionsList) {
		this.tdf = tdf;
        this.docId = docId;
        this.wdt = wdt;
        this.termFreq = termFreq;
        this.positionsList = positionsList;
    }
	
	/*public DiskPosting(int tdf, int docId, int termFreq, ArrayList<Integer> positionsList, double finalScore) {
		this.tdf = tdf;
        this.docId = docId;
        this.finalScore = finalScore;
        this.termFreq = termFreq;
        this.positionsList = positionsList;
    }*/
	
	public DiskPosting(int docId, int termFreq, ArrayList<Integer> positionsList, double finalScore) {
        this.docId = docId;
        this.finalScore = finalScore;
        this.termFreq = termFreq;
        this.positionsList = positionsList;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }
    
    public double getWdt() {
		return wdt;
	}

	public void setWdt(double wdt) {
		this.wdt = wdt;
	}
    
    public int getTermFreq() {
        return termFreq;
    }

    public void setTermFreq(int termFreq) {
        this.termFreq = termFreq;
    }

    public ArrayList<Integer> getPositions() {
        return positionsList;
    }

    public void setPositions(ArrayList<Integer> positionsList) {
        this.positionsList = positionsList;
    }
    
    public double getFinalScore() {
		return finalScore;
	}

	public void setFinalScore(double finalScore) {
		this.finalScore = finalScore;
	}

	public int getTdf() {
		return tdf;
	}

	public void setTdf(int tdf) {
		this.tdf = tdf;
	}

	@Override
	public int compareTo(DiskPosting dp) {
		return this.getDocId() - dp.getDocId();
	}
}
