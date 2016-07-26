package model;

import java.util.ArrayList;

public class TestCases {
	private int queryID;
	private int uq;
	private int targetV;
	private ArrayList<Integer> Pq;
	public TestCases(int qid, int uq, int tarV, ArrayList<Integer> Pq){
		this.queryID=qid;
		this.uq=uq;
		this.targetV=tarV;
		this.Pq=Pq;
	}
	public int getQueryID() {
		return queryID;
	}
	public void setQueryID(int queryID) {
		this.queryID = queryID;
	}
	public int getUq() {
		return uq;
	}
	public void setUq(int uq) {
		this.uq = uq;
	}
	public int getTargetV() {
		return targetV;
	}
	public void setTargetV(int targetV) {
		this.targetV = targetV;
	}
	public ArrayList<Integer> getPq() {
		return Pq;
	}
	public void setPq(ArrayList<Integer> pq) {
		Pq = pq;
	}
}
