package model;

import java.util.ArrayList;

import edu.stanford.nlp.optimization.DiffFunction;

public class DiffFunctionThetauser implements DiffFunction {
	private SPORE sp;
	private double[] thetauser;
	private int u;
	private UserProfile up;
	private double normal=1;

	public DiffFunctionThetauser(int u) {
		this.u = u;
		this.sp = SPORE.getObject();
		this.up = sp.user_items.get(u);
	}

	@Override
	public int domainDimension() {
		// TODO Auto-generated method stub
		return Paras.K;
	}

	@Override
	public double valueAt(double[] arg0) {
		this.thetauser=arg0;
		this.setAlpha();
		double fl = 0;
		for (int v = 0; v < up.getSize(); v++) {
			double alphad = up.getAlphad(v);
			double alphan = up.getAlphan(v);
			double alpha = Math.log(alphad / alphan);
			fl += alpha;
		}
		return fl*normal;
	}

	@Override
	public double[] derivativeAt(double[] arg0) {
		this.thetauser=arg0;
		//System.out.println("This is the output from DiffFunctionThetauser.derivativeAt");
		this.setAlpha();
		double[] r = new double[arg0.length];
		double max=0;
		for (int i = 0; i < arg0.length; i++) {
			double duz = sp.duz[this.u][i];
			double alphasum = 0;
			for (int v = 0; v < up.getSize(); v++) {
				double alpha = 0;
				double alphan = up.getAlphans(v)[i];
				double alphad = up.getAlphad(v);
				alpha = alphan / alphad;
				alphasum += alpha;
			}
			r[i] = alphasum - duz;
			if(Math.abs(r[i])>max){
				max=Math.abs(r[i]);
			}
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
		for(int z=0;z<arg0.length;z++){
			r[z]*=normal;
		}
		// System.out.println("");
		return r;
	}

	public void setAlpha() {
		// TODO Auto-generated method stub
		// for the begin u
		int size=up.getSize();
		for (int v=0;v<size;v++) {
			int z = up.getZ(v);
//			int l = up.getL(v);
			ArrayList<Integer> seq=up.getSeq(v);
			double alphad = 0;
			double alphan = 0;
			double[] alphans = new double[Paras.K];
			for (int zz = 0; zz < Paras.K; zz++) {
//				double alphant = this.getAlphaN(l, zz,seq);
				double alphant = this.getAlphaN(zz,seq);
				alphans[zz] = alphant;
				if (zz == z) {
					alphan = alphant;
				}
				alphad += alphant;
			}
			up.setAlphan(v, alphan);
			up.setAlphad(v, alphad);
			up.setAlphans(v, alphans);
			// GeoGM.getGM().user_items.get(u).setAlphad(v, alphad);
		}
	}

	public double getAlphaN(int z, ArrayList<Integer> seq) {
//		public double getAlphaN(int l, int z, ArrayList<Integer> seq) {
		double alphan = 0;
		// infer alphan
		double exp = 0;
//		exp += (sp.theta0[z] + thetauser[z]+sp.thetanative[l][z]);
		exp += (sp.theta0[z] + thetauser[z]);
		for(int pv:seq){
			exp+=sp.thetapre[pv][z];
		}
		alphan = Math.exp(exp);
		return alphan;
	}
}
