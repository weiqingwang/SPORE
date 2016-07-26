package model;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.stanford.nlp.optimization.DiffFunction;

public class DiffFunctionTheta0 implements DiffFunction {
	private SPORE sp;
	private double[] theta0;
	private int maxthreads;
	private double normal=1;
	
	public DiffFunctionTheta0(){
		this.sp=SPORE.getObject();
		this.maxthreads=Paras.MAXTHREADSL;
	}

	@Override
	public int domainDimension() {
		// TODO Auto-generated method stub
		return Paras.K;
	}

	@Override
	public double valueAt(double[] arg0) {
		this.theta0=arg0;
		// TODO Auto-generated method stub
		ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads, maxthreads, 1, TimeUnit.SECONDS, new LinkedBlockingQueue());
		// TODO Auto-generated method stub
		int count=0;
		ArrayList<Pair>[] joblists = new ArrayList[maxthreads];
		for (int i = 0; i < maxthreads; i++)
		{
			joblists[i] = new ArrayList<Pair>();
		}
		for(int u=0;u<sp.U;u++)
		{
			UserProfile up=sp.user_items.get(u);
			for(int v=0 ; v < up.getSize(); v++)
			{
				joblists[count].add(new Pair(u, v));
				count = (count + 1) % maxthreads;
			}
		}
		for (int i = 0; i < maxthreads; i++)
		{
			executor.submit(new AlphaSetter(joblists[i]));
		}
		executor.shutdown();
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		double fl=0;
//		System.out.println("valueAT");
		for(int u=0;u<sp.U;u++)
		{
			UserProfile up=sp.user_items.get(u);
			for(int v=0 ; v < up.getSize(); v++)
			{
				double alphad=up.getAlphad(v);
				double alphan=up.getAlphan(v);
				double alpha=Math.log(alphad/alphan);
				fl+=alpha;
			}
		}
//		System.out.println(fl);
		return fl*normal;
	}

	@Override
	public double[] derivativeAt(double[] arg0) {
		this.theta0=arg0;
		ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads, maxthreads, 1, TimeUnit.SECONDS, new LinkedBlockingQueue());
		// TODO Auto-generated method stub
		//get the alphad first as all the topic share the same alphad
		int count=0;
		ArrayList<Pair>[] joblists = new ArrayList[maxthreads];
		for (int i = 0; i < maxthreads; i++)
		{
			joblists[i] = new ArrayList<Pair>();
		}
		for(int u=0;u<sp.U;u++)
		{
			UserProfile up=sp.user_items.get(u);
			for(int v=0 ; v < up.getSize(); v++)
			{
				joblists[count].add(new Pair(u, v));
				count = (count + 1) % maxthreads;
			}
		}
		for (int i = 0; i < maxthreads; i++)
		{
			executor.submit(new AlphaSetter(joblists[i]));
		}
		executor.shutdown();
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		double[] r=new double[arg0.length];
		double max=0;
		for(int i=0;i<arg0.length;i++){
			double dz=sp.dz[i];
			double alphasum=0;
			for(int u=0;u<sp.U;u++){
				UserProfile up=sp.user_items.get(u);
				for(int v=0;v<up.getSize();v++){
					double alpha=0;
					double alphan=up.getAlphans(v)[i];
					double alphad=up.getAlphad(v);
					alpha=alphan/alphad;
					alphasum+=alpha;
				}
			}
			r[i]=(alphasum-dz);
			if(Math.abs(r[i])>Math.abs(max))
				max=r[i];
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
	
	private class AlphaSetter implements Runnable{
		private ArrayList<Pair> joblist;
		
		public AlphaSetter(ArrayList<Pair> joblist){
			this.joblist = joblist;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			//for the begin u
			for (Pair pair : joblist)
			{
				UserProfile up=sp.user_items.get(pair.getFirst());
				int u = pair.getFirst();
				int v = pair.getSec();
				int z=up.getZ(v);
//				int l=up.getL(v);
				ArrayList<Integer> seq=up.getSeq(v);
				double alphad=0;
				double alphan=0;
				double[] alphans=new double[Paras.K];
				for(int zz=0;zz<Paras.K;zz++){
					double alphant=this.getAlphaN(u, zz, seq);
//					double alphant=this.getAlphaN(u, l, zz, seq);
					alphans[zz]=alphant;
					if(zz==z){
						alphan=alphant;
					}
					alphad+=alphant;
				}
				up.setAlphan(v, alphan);
				up.setAlphad(v, alphad);
				up.setAlphans(v, alphans);
			}
		}
		
		public double getAlphaN(int u, int z, ArrayList<Integer> seq){
//			public double getAlphaN(int u, int l, int z, ArrayList<Integer> seq){
			double alphan=0;
			//infer alphan
			double exp=0;
//			exp+=(theta0[z]+sp.thetauser[u][z]+sp.thetanative[l][z]);
			exp+=(theta0[z]+sp.thetauser[u][z]);
			for(int pv:seq){
				exp+=sp.thetapre[pv][z];
			}
			alphan=Math.exp(exp);
			return alphan;
		}
		
	}
}
