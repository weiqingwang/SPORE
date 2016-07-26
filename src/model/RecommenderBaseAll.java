package model;
import java.util.ArrayList;
import java.util.HashSet;


public class RecommenderBaseAll implements Runnable {
	private SPORE sp;
	private ArrayList<TestCases> joblist;
	private int index;
//	private ArrayList<Pair> hitPairs;

	// private double normal=1e5;
	public RecommenderBaseAll(ArrayList<TestCases> joblist, int index) {
		this.sp = SPORE.getObject();
		this.joblist = joblist;
		this.index = index;
//		this.hitPairs = new ArrayList<Pair>();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// for each pair, infer the hit
		for (TestCases query : joblist) {
			int qid=query.getQueryID();
			int uq = query.getUq();
			int targetV = query.getTargetV();
			ArrayList<Integer> Pq = query.getPq();
			// for targetV and unrated items, we infer ratings for them
			// infer the rating for the target item
			double ratingt = 0;
			double[] wqzs=new double[Paras.K];
			for (int z = 0; z < Paras.K; z++) {
				double ratingz = 0;
				ratingz = sp.inferAlpha(uq, z, Pq);
//				ratingz = sp.inferAlpha(uq, lq, z, Pq);
				wqzs[z]=ratingz;
				ratingz*=sp.Fzvs[z][targetV];
				ratingt += ratingz;
			}
			int countR = 0;
			for (int v = 0; v < Paras.countV; v++) {
				if(v==targetV)
					continue;
				double rating = 0;
				for (int z = 0; z < Paras.K; z++) {
					double ratingz = 0;
					ratingz = wqzs[z];
					// ratingz *= normal;
					ratingz*=sp.Fzvs[z][v];
					rating += ratingz;
				}
				if (rating > ratingt) {
					countR++;
				}
			}
			// if the rating is smaller than Paras.k, then hit++
			if (countR < Paras.k) {
				sp.hit[index]++;
				sp.hitcases[index].add(qid);
			}
		}
		System.out.println("one thread ends");
	}

}
