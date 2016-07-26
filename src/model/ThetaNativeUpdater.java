//package model;
//
//import java.util.ArrayList;
//
//import edu.stanford.nlp.optimization.QNMinimizer;
//
//public class ThetaNativeUpdater implements Runnable {
//	private ArrayList<Integer> joblist;
//
//	public ThetaNativeUpdater(ArrayList<Integer> joblist) {
//		this.joblist = joblist;
//	}
//
//	public void run() {
//		// TODO Auto-generated method stub
//		if (joblist.size() == 0) {
//			System.out.println("Don't worry!");
//			return;
//		}
//		SPORE sp=SPORE.getObject();
//		QNMinimizer qnm = new QNMinimizer(10, true);
//		qnm.terminateOnRelativeNorm(true);
//		qnm.terminateOnNumericalZero(true);
//		qnm.terminateOnAverageImprovement(true);
//		qnm.shutUp();
//		for (int l : joblist) {
//			DiffFunctionThetaNative df = new DiffFunctionThetaNative(l);
//			double[] temp=sp.thetanative[l];
//			temp = qnm.minimize(df, Paras.termate, sp.thetanative[l]);
//		}
//		System.out.println("one thread has completed!");
//	}
//}