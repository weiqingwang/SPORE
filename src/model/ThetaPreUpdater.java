package model;

import java.util.ArrayList;

import edu.stanford.nlp.optimization.QNMinimizer;

public class ThetaPreUpdater implements Runnable {
	private ArrayList<Integer> joblist;

	public ThetaPreUpdater(ArrayList<Integer> joblist) {
		this.joblist = joblist;
	}

	public void run() {
		// TODO Auto-generated method stub
		if (joblist.size() == 0) {
			System.out.println("Don't worry!");
			return;
		}
		SPORE sp = SPORE.getObject();
		QNMinimizer qnm = new QNMinimizer(10, true);
		qnm.terminateOnRelativeNorm(true);
		qnm.terminateOnNumericalZero(true);
		qnm.terminateOnAverageImprovement(true);
		qnm.shutUp();
		for (int v : joblist) {
			DiffFunctionThetaPre df = new DiffFunctionThetaPre(v);
			double[] temp = (double[])sp.thetapre[v].clone();
			temp = qnm.minimize(df, Paras.termate, temp);
			for (int z = 0; z < Paras.K; z++) {
				sp.thetapre[v][z] = temp[z];
			}
		}
		System.out.println("one thread has completed!");
	}
}