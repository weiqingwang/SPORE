package model;

import java.util.ArrayList;

public class PreInferer implements Runnable {
	private SPORE sp;
	private ArrayList<Integer> joblist;

	public PreInferer(ArrayList<Integer> joblist) {
		this.sp = SPORE.getObject();
		this.joblist = joblist;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		for (int z : joblist) {
			for (int w = 0; w < sp.W; w++) {
				sp.betas[z][w] = sp.inferBeta(z, w);
			}
			for (int v = 0; v < sp.V; v++) {
				sp.gammas[z][v] = sp.inferGamma(z, v);
			}
		}
		// infer fzvs
		for (int z : joblist) {
			for (int v = 0; v < sp.V; v++) {
				ArrayList<Integer> words = sp.Dvw[v];
				int w = words.get(0);
				double ratingw = sp.betas[z][w];
				double wordSize = words.size();
				for (int wi = 1; wi < words.size(); wi++) {
					w = words.get(wi);
					ratingw += (sp.betas[z][w]);
				}
				ratingw = ratingw / wordSize;
				double fzv= ratingw;
				double gamma = sp.gammas[z][v];
				fzv *= gamma;
				sp.Fzvs[z][v]=fzv;
			}
		}
	}

}
