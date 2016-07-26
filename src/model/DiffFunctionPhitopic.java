package model;

import edu.stanford.nlp.optimization.DiffFunction;

public class DiffFunctionPhitopic implements DiffFunction {
	private SPORE sp;
	private double[] phitopic;
	private int z;
	private double betadz;
	private double[] betans;
	private double normal=1;

	public DiffFunctionPhitopic(int z) {
		this.sp = SPORE.getObject();
		this.z = z;
		this.betans=new double[sp.W];
	}

	@Override
	public int domainDimension() {
		// TODO Auto-generated method stub
		return sp.W;
	}

	@Override
	public double valueAt(double[] arg0) {
		// TODO Auto-generated method stub
		this.phitopic = arg0;
		// intialize betads
		this.setBetas();
		double sum = 0;
		for (int w = 0; w < sp.W; w++) {
			double sumtemp=sp.dzw[z][w]*(Math.log(this.betadz/this.betans[w]));
			sum+=sumtemp;
		}
		return sum*normal;
	}

	@Override
	public double[] derivativeAt(double[] arg0) {
		this.phitopic = arg0;
		this.setBetas();
		double max=0;
		double[] r = new double[arg0.length];
		for (int w = 0; w < arg0.length; w++) {
			int dzw = sp.dzw[this.z][w];
			double betadz1 = sp.dz[this.z] * this.betans[w]/this.betadz;
			r[w] = (betadz1 - dzw);
			if(Math.abs(r[w])>Math.abs(max))
				max=r[w];
		}
		if(normal==1){
			if(max<=50){
				normal=0.9;
			}
			//get normal
			else if(max>50&&max<100){
				normal=0.1;
			}
			else if(max>=100&&max<1000){
				normal=0.01;
			}
			else if(max>=1000&&max<1e4){
				normal=1e-3;
			}
			else if(max>=1e4 && max<1e5){
				normal=1e-4;
			}
			else{
				normal=1e-5;
			}
		}
		for(int w=0;w<arg0.length;w++){
			r[w]*=normal;
		}
		return r;
	}

	public void setBetas(){
		double temp=0;
		for (int w = 0; w < sp.W; w++) {
			double betazw=(this.getBetaN(w));
			temp += betazw;
			this.betans[w]=betazw;
		}
		this.betadz=temp;
	}

	public double getBetaN(int w) {
		double exp = 0;
		exp=this.phitopic[w]+sp.phi0[w];
		return Math.exp(exp);
	}
}
