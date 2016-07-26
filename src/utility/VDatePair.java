package utility;

import java.util.Date;

public class VDatePair {
	private int vid;
	private Date date;

	public VDatePair(int i, Date j) {
		this.vid = i;
		this.date = j;
	}

	public VDatePair() {
	}
	
	public int getVid(){
		return this.vid;
	}
	
	public Date getDate(){
		return this.date;
	}
	
	public boolean before(VDatePair p){
		boolean re=false;
		if(this.date.before(p.getDate()))
			re=true;
		return re;
	}
}