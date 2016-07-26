package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import utility.Statistics;
import utility.data_storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.stanford.nlp.optimization.QNMinimizer;

public class SPORE {
	private QNMinimizer qnm;
	private static SPORE sp = new SPORE("data/" + Paras.dataset + "/train.txt",
			"data/" + Paras.dataset + "/parameters/", 280, 5, 230);

	public static SPORE getObject() {
		return sp;
	}

	public int U; // the number of users
	public int V;// the number of spatial items(corresponds to placeID)
	public int W;// the number of vocabulary
	public int N;// the number of overall activities
	// public int R;// the number of locations(corresponds to the locationID)
	public int K;
	public HashMap<Integer, UserProfile> user_items;
	public ArrayList<TestCases> test_cases;
	public Hashtable<Integer, ArrayList<Integer>> nearbyItems;
	public Hashtable<Integer, ArrayList<Integer>> unvisitedItems;
	// public int[] itemlocations;
	// public ArrayList<Integer>[] DR;// the list of spatial items
	// at each location
	public HashSet<Integer>[] Duv;// the list of spatial items
									// each user has checked in
	// item v
	public ArrayList<Integer>[] Dvw;// the list of words on item v
	// public ArrayList<Pair>[] Dt;// the set of activities
	// occurring on the
	// each time slice
	// public ArrayList<Pair>[] Dln;// the set of activities
	// occurring on each
	// location
	private double extSmall = 0.9;
	public double[] theta0;// the global distribution over topics
	public double[][] thetauser;// the distribution over topics for each user
	public double[] phi0; // the global distribution over words
	public double[][] phitopic;// the distribution over words for each topic;
	public double[][] psitopic;// the distribution over spatial items for each
								// topic;
	public double[] psi0;// the global distribution over spatial items
	// public double[][] thetanative;// the distribution over
	// topics for each
	// location on each
	// level
	public double[][] thetapre;// the distribution over topics for each item in
								// sequence
	public double[][] thetaprecopy;
	public int[][] dvzp; // the number of activities assigned to each topic and
							// contains the spatial item v
	public ArrayList<Pair>[] DPv; // the set of activities containing the
									// spatial item v
	public int[][] duz;// the number of activities assigned to each topic by
						// each user
	// public int[][] dlnz;// the number of activities assigned to
	// each topic at
	// each location assigned by natives
	public int[][] dzw;// the number of activities where the word w is assigned
						// to the topic z
	public int[] dz;// the number of activities assigned to each topic
	public int[][] dzv;
	public int[] dw;
	public int[] dv;
	public int[] du;
	public int[] hit;
	public ArrayList<Integer>[] hitcases;
	public double[][] betas;
	public double[][] gammas;
	public double[][] Fzvs;
	// public int[] dln;// the count of activities at each location assigned by
	// natives

	public int countTestRandom;

	public int ITERATIONS;
	public int SAMPLE_LAG;
	public int BURN_IN;
	public String outputPath;

	private SPORE(String trainFile, String path_output_model, int ITERATIONS, int SAMPLE_LAG, int BURN_IN) {
		this.qnm = new QNMinimizer(10, true);
		qnm.terminateOnRelativeNorm(true);
		qnm.terminateOnNumericalZero(true);
		qnm.terminateOnAverageImprovement(true);

		int[] stas = Statistics.getStatistics(trainFile);
		this.U = Paras.countU;
		this.V = Paras.countV;
		this.W = stas[0];
		// this.R = Paras.countR;
		this.N = stas[1];
		this.K = Paras.K;
		this.outputPath = path_output_model;
		this.ITERATIONS = ITERATIONS;
		this.SAMPLE_LAG = SAMPLE_LAG;
		this.BURN_IN = BURN_IN;

		this.theta0 = new double[K];
		this.thetauser = new double[this.U][K];
		this.thetapre = new double[this.V][K];
		this.phi0 = new double[this.W];
		this.phitopic = new double[K][this.W];
		this.psi0 = new double[this.V];
		this.psitopic = new double[K][this.V];
		// this.thetanative = new double[this.R][K];

		dw = new int[this.W];
		dv = new int[V];
		du = new int[U];
		// dln = new int[R];
		this.dvzp = new int[V][K];
		// this.itemlocations = new int[V];

		this.user_items = new HashMap<Integer, UserProfile>();

		// this.Dln = new ArrayList[this.U];
		// for (int u = 0; u < this.U; u++) {
		// this.Dln[u] = new ArrayList<Pair>();
		// }
		this.DPv = new ArrayList[this.V];
		for (int v = 0; v < this.V; v++) {
			this.DPv[v] = new ArrayList<Pair>();
		}
		// this.DR = new ArrayList[this.R];
		// for (int r = 0; r < this.R; r++) {
		// this.DR[r] = new ArrayList<Integer>();
		// }
		this.Duv = new HashSet[this.U];
		for (int u = 0; u < this.U; u++) {
			this.Duv[u] = new HashSet<Integer>();
			UserProfile up = new UserProfile(stas[u + 2]);
			user_items.put(u, up);
		}
		this.Dvw = new ArrayList[this.V];
		for (int v = 0; v < this.V; v++) {
			this.Dvw[v] = new ArrayList<Integer>();
		}

		this.nearbyItems = new Hashtable<Integer, ArrayList<Integer>>();
		this.unvisitedItems = new Hashtable<Integer, ArrayList<Integer>>();
		try {
			FileReader reader = new FileReader(trainFile);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			// userID/tweetID/lat/lon/time/placeID/contentInfo/state/seq
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				int uid = Integer.parseInt(terms[0]);
				this.du[uid]++;
				// int location = Integer.parseInt(terms[9]);
				int spatialItem = Integer.parseInt(terms[5]);
				// this.itemlocations[spatialItem] = location;
				this.dv[spatialItem]++;
				String contentsS = terms[6];
				StringTokenizer st1 = new StringTokenizer(contentsS, "|");
				ArrayList<Integer> contents = new ArrayList<Integer>();
				while (st1.hasMoreTokens()) {
					int word = new Integer(st1.nextToken());
					contents.add(word);
					this.dw[word]++;
				}
				String seqs = terms[8];
				StringTokenizer st2 = new StringTokenizer(seqs, ",");
				ArrayList<Integer> seq = new ArrayList<Integer>();
				int vid = Integer.parseInt(st2.nextToken());
				if (vid != -1) {
					seq.add(vid);
					while (st2.hasMoreTokens()) {
						vid = Integer.parseInt(st2.nextToken());
						seq.add(vid);
					}
				}
				// if (this.Dvw[spatialItem].size() != 0)
				this.Dvw[spatialItem] = contents;
				// initialize DR and Duv
				// ArrayList<Integer> drr = this.DR[location];
				// if (!drr.contains(spatialItem))
				// drr.add(spatialItem);
				HashSet<Integer> duvv = this.Duv[uid];
				if (!duvv.contains(spatialItem))
					duvv.add(spatialItem);
				user_items.get(uid).addOneRecord(spatialItem, seq, contents);
				Pair p;
				int index = user_items.get(uid).getSize() - 1;
				p = new Pair(uid, index);
				// this.Dln[location].add(p);
				// this.dln[location]++;
				for (int pv : seq) {
					this.DPv[pv].add(p);
				}
			}
			br.close();
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initializeCount() {
		this.duz = new int[this.U][K];
		// this.dlnz = new int[this.R][K];
		this.dzw = new int[K][this.W];
		this.dz = new int[K];
		this.dzv = new int[K][this.V];
		// initialize the phi0 and psi0
		for (int i = 0; i < this.W; i++) {
			// this.phi0[i] = (double) this.dw[i] / this.N;
			double value = this.dw[i];
			if (value == 0) {
				value = sp.extSmall;
				// System.out.println(i);
			}
			this.phi0[i] = Math.log(value);
			// System.out.println(value + "\t" + this.phi0[i]);
		}
		for (int i = 0; i < this.V; i++) {
			// this.psi0[i] = (double) this.dv[i] / this.N;
			double value = this.dv[i];
			if (value == 0)
				value = sp.extSmall;
			this.psi0[i] = Math.log(value);
		}
		// assign each twitter randomly to a topic
		for (int i = 0; i < U; i++) {
			UserProfile up = this.user_items.get(i);
			// System.out.println(i);
			int length = up.getSize();

			for (int j = 0; j < length; j++) {
				int ran = (int) (Math.random() * (K));
				this.dz[ran]++;
				up.setZ(j, ran);
				ArrayList<Integer> seq = up.getSeq(j);
				for (int pv : seq) {
					this.dvzp[pv][ran]++;
				}
				// update the statistics related to topic assignment
				this.duz[i][ran]++;
				// this.dtz[up.getT(j)][ran]++;
				// update dlz for each level
				// int location = up.getL(j);
				// this.dlnz[location][ran]++;
				int item = up.getV(j);
				this.dzv[ran][item]++;
				// this.dzw
				ArrayList<Integer> words = up.getContents(j);
				for (int word : words) {
					this.dzw[ran][word]++;
				}
			}
		}
		for (int i = 0; i < K; i++) {
			if (this.dz[i] == 0)
				this.theta0[i] = Math.log(sp.extSmall);
			else
				this.theta0[i] = Math.log(this.dz[i]);
			for (int j = 0; j < this.W; j++) {
				double value = this.dzw[i][j];
				if (value == 0) {
					value = sp.extSmall;
				}
				this.phitopic[i][j] = Math.log(value) - this.phi0[j];
			}
			for (int j = 0; j < this.V; j++) {
				// this.psitopic[i][j] = (double) (this.dzv[i][j] - dv[j]) /
				// normalz;
				double value = this.dzv[i][j];
				if (value == 0) {
					value = sp.extSmall;
				}
				this.psitopic[i][j] = Math.log(value) - this.psi0[j];
				value = this.dvzp[j][i];
				if (value == 0) {
					value = sp.extSmall;
				}
				this.thetapre[j][i] = Math.log(value);
			}
			for (int j = 0; j < this.U; j++) {
				double value = this.duz[j][i];
				if (value == 0) {
					value = sp.extSmall;
				}
				this.thetauser[j][i] = Math.log(value);
			}
		}
		// for (int l = 0; l < Paras.countR; l++) {
		// for (int z = 0; z < K; z++) {
		// double value = this.dlnz[l][z];
		// if (value == 0) {
		// this.thetanative[l][z] = Math.log(sp.extSmall);
		// } else {
		// this.thetanative[l][z] = Math.log(value);
		// }
		// }
		// }
	}

	public void sampleTopic(int i, int j) {
		// remove the previous topic assignment and modify the statistics
		UserProfile user = this.user_items.get(i);
		// int location = user.getL(j);
		int topic = user.getZ(j);
		int item = user.getV(j);
		ArrayList<Integer> seq = user.getSeq(j);
		ArrayList<Integer> contents = user.getContents(j);
		// the related statistics are: duz, dtz, dzw, dz, dzv, dlzn, dlzt
		// infer and assign the new topic assignment according to the
		// probability distribution
		// probability
		double[] ps = new double[K];
		for (int z = 0; z < K; z++) {
			double exp = 0;
			// exp = this.theta0[z] + this.thetauser[i][z] +
			// this.thetanative[location][z];
			exp = this.theta0[z] + this.thetauser[i][z];
			for (int pv : seq) {
				exp += this.thetapre[pv][z];
			}
			ps[z] = Math.exp(exp);
		}
		// sampling
		for (int ii = 1; ii < K; ii++)
			ps[ii] += ps[ii - 1];
		double t = Math.random() * ps[K - 1];
		int topicnew = 0;
		for (; topicnew < K; topicnew++) {
			if (t < ps[topicnew])
				break;
		}
		// test
		if (topicnew >= K) {
			System.err.println(ps[K - 1] + " " + t);
		}
		// modify the statistics
		this.user_items.get(i).setZ(j, topicnew);
		this.duz[i][topic]--;
		this.duz[i][topicnew]++;
		for (int word : contents) {
			this.dzw[topic][word]--;
			this.dzw[topicnew][word]++;
		}
		this.dz[topic]--;
		this.dz[topicnew]++;
		this.dzv[topic][item]--;
		this.dzv[topicnew][item]++;
		// this.dlnz[location][topic]--;
		// this.dlnz[location][topicnew]++;
		for (int pv : seq) {
			this.dvzp[pv][topic]--;
			this.dvzp[pv][topicnew]++;
		}
	}

	public void updateTheta0() {
		System.out.println("update theta 0.....");
		DiffFunctionTheta0 df = new DiffFunctionTheta0();
		// this.theta0 = qnm.minimize(df, 1e-10, this.theta0);
		this.theta0 = this.qnm.minimize(df, Paras.termate, this.theta0);
		// System.out.println("update theta 0 finished");
	}

	public void updateThetaUser() {
		int maxthreads = Paras.MAXTHREADSM;
		System.out.println("update theta user.....");
		ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads, maxthreads, 1, TimeUnit.SECONDS,
				new LinkedBlockingQueue());
		// TODO Auto-generated method stub
		int count = 0;
		ArrayList<Integer>[] joblists = new ArrayList[maxthreads];
		for (int i = 0; i < maxthreads; i++) {
			joblists[i] = new ArrayList<Integer>();
		}
		for (int u = 0; u < sp.U; u++) {
			joblists[count].add(u);
			count = (count + 1) % maxthreads;
		}
		for (int i = 0; i < maxthreads; i++) {
			executor.submit(new ThetaUserUpdater(joblists[i]));
		}
		executor.shutdown();
		try {
			// while (!executor.isTerminated())
			while (!executor.awaitTermination(60, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("update theta user finished");
	}

	// public void updateThetaNative() {
	// int maxthreads = Paras.MAXTHREADSM;
	// System.out.println("update theta native.........");
	// ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads,
	// maxthreads, 1, TimeUnit.SECONDS,
	// new LinkedBlockingQueue());
	// // TODO Auto-generated method stub
	// int count = 0;
	// ArrayList<Integer>[] joblists = new ArrayList[maxthreads];
	// for (int i = 0; i < maxthreads; i++) {
	// joblists[i] = new ArrayList<Integer>();
	// }
	// for (int l = 0; l < sp.R; l++) {
	// joblists[count].add(l);
	// count = (count + 1) % maxthreads;
	// }
	// for (int i = 0; i < maxthreads; i++) {
	// executor.submit(new ThetaNativeUpdater(joblists[i]));
	// }
	// executor.shutdown();
	// try {
	// // while (!executor.isTerminated())
	// while (!executor.awaitTermination(60, TimeUnit.SECONDS))
	// ;
	// } catch (InterruptedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	public void updateThetaPre() {
		int maxthreads = Paras.MAXTHREADSL;
		System.out.println("update theta pre.........");
		this.thetaprecopy = (double[][]) this.thetapre.clone();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads, maxthreads, 1, TimeUnit.SECONDS,
				new LinkedBlockingQueue());
		// TODO Auto-generated method stub
		int count = 0;
		ArrayList<Integer>[] joblists = new ArrayList[maxthreads];
		for (int i = 0; i < maxthreads; i++) {
			joblists[i] = new ArrayList<Integer>();
		}
		for (int v = 0; v < sp.V; v++) {
			if (this.DPv[v].isEmpty()) {
				continue;
			}
			joblists[count].add(v);
			count = (count + 1) % maxthreads;
		}
		for (int i = 0; i < maxthreads; i++) {
			executor.submit(new ThetaPreUpdater(joblists[i]));
		}
		executor.shutdown();
		try {
			// while (!executor.isTerminated())
			while (!executor.awaitTermination(60, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateThetaPreSingleP() {
		this.thetaprecopy = (double[][]) this.thetapre.clone();
		QNMinimizer qnm = new QNMinimizer(10, true);
		qnm.terminateOnRelativeNorm(true);
		qnm.terminateOnNumericalZero(true);
		qnm.terminateOnAverageImprovement(true);
		// qnm.shutUp();
		for (int v = 0; v < sp.V; v++) {
			if (this.DPv[v].isEmpty()) {
				continue;
			}
			DiffFunctionThetaPre df = new DiffFunctionThetaPre(v);
			double[] temp = sp.thetapre[v];
			temp = qnm.minimize(df, Paras.termate, temp);
			for (int z = 0; z < Paras.K; z++) {
				sp.thetapre[v][z] = temp[z];
			}
		}
	}

	public void updatePhi0() {
		System.out.println("update phi 0..........");
		DiffFunctionPhi0 df = new DiffFunctionPhi0();
		this.phi0 = qnm.minimize(df, Paras.termate, this.phi0);
		// System.out.println("update phi 0 finished");
	}

	public void updatePhiTopic() {
		System.out.println("update phi topic...........");
		for (int z = 0; z < K; z++) {
			DiffFunctionPhitopic df = new DiffFunctionPhitopic(z);
			this.phitopic[z] = qnm.minimize(df, Paras.termate, this.phitopic[z]);
		}
		// System.out.println("update phi topic finished");
	}

	public void updatePsi0() {
		System.out.println("update psi 0.........");
		DiffFunctionPsi0 df = new DiffFunctionPsi0();
		this.psi0 = qnm.minimize(df, Paras.termate, this.psi0);
		// System.out.println("update psi 0 finished");
	}

	public void updatePsiTopic() {
		System.out.println("update psi topic..........");
		// this.qnm.shutUp();
		for (int z = 0; z < K; z++) {
			DiffFunctionPsitopic df = new DiffFunctionPsitopic(z);
			this.psitopic[z] = qnm.minimize(df, Paras.termate, this.psitopic[z]);
		}
		// System.out.println("update psi topic finished");
	}

	public void recommend(String testFile) {
		System.out.println("recommend....");
		this.test_cases = new ArrayList<TestCases>();
		FileReader reader;
		int countTest = 0;
		int maxthreads = Paras.MAXTHREADSS;
		this.hit = new int[maxthreads];
		this.hitcases = new ArrayList[maxthreads];
		for (int i = 0; i < maxthreads; i++) {
			this.hitcases[i] = new ArrayList<Integer>();
		}
		try {
			FileWriter writer = new FileWriter(this.outputPath + "result.txt", true);
			BufferedWriter bw = new BufferedWriter(writer);
			FileWriter writer1 = new FileWriter(this.outputPath + "hitcases.txt", true);
			BufferedWriter bw1 = new BufferedWriter(writer1);
			reader = new FileReader(testFile);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				if (terms.length != 10)
					continue;
				countTest++;
				// System.out.println(countTest);
				// the format of the input file is:
				// userID/tweetID/lat/lon/time/placeID/contentInfo/state/seq/locationID/unvisitedItems
				int u = Integer.parseInt(terms[0]);
				// int l = Integer.parseInt(terms[9]);
				int targetV = Integer.parseInt(terms[5]);
				String contentsS = terms[6];
				StringTokenizer st1 = new StringTokenizer(contentsS, "|");
				ArrayList<Integer> contents = new ArrayList<Integer>();
				while (st1.hasMoreTokens())
					contents.add(new Integer(st1.nextToken()));
				this.Dvw[targetV] = contents;
				String seqs = terms[8];
				st1 = new StringTokenizer(seqs, ",");
				ArrayList<Integer> seq = new ArrayList<Integer>();
				while (st1.hasMoreTokens()) {
					int pvid = Integer.parseInt(st1.nextToken());
					if (pvid == -1) {
						break;
					}
					seq.add(pvid);
				}
				if (!this.nearbyItems.containsKey(targetV)) {
					String itemss = terms[9];
					// String itemss=terms[10];
					st1 = new StringTokenizer(itemss, ",");
					ArrayList<Integer> items = new ArrayList<Integer>();
					while (st1.hasMoreTokens()) {
						int vid = Integer.parseInt(st1.nextToken());
						items.add(vid);
					}
					this.nearbyItems.put(targetV, items);
				}
				TestCases tc = new TestCases(countTest - 1, u, targetV, seq);
				this.test_cases.add(tc);
				// UserProfile up = this.test_cases.get(u);
				// if (up == null) {
				// up = new UserProfile(1000);
				// up.addOneRecord(targetV, seq, null);
				// // up.addOneRecord(targetV, l, seq, null);
				// this.test_cases.put(u, up);
				// } else {
				// // up.addOneRecord(targetV, l, seq, null);
				// up.addOneRecord(targetV, seq, null);
				// }
				// for this user, find her unvisited items
				// if (!this.unvisitedItems.containsKey(u)) {
				// ArrayList<Integer> items=new ArrayList<Integer>();
				// HashSet<Integer> visitedItems = this.Duv[u];
				// for (int v = 0; v < this.V; v++) {
				// if(!visitedItems.contains(v)){
				// items.add(v);
				// }
				// }
				// this.unvisitedItems.put(u, items);
				// }
			}
			ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads, maxthreads, 1, TimeUnit.SECONDS,
					new LinkedBlockingQueue());
			// TODO Auto-generated method stub
			int count = 0;
			ArrayList<TestCases>[] joblists = new ArrayList[maxthreads];
			for (int i = 0; i < maxthreads; i++) {
				joblists[i] = new ArrayList<TestCases>();
			}
			for (TestCases query : this.test_cases) {
				joblists[count].add(query);
				count = (count + 1) % maxthreads;
			}
			for (int i = 0; i < maxthreads; i++) {
				executor.submit(new RecommenderNew(joblists[i], i));
			}
			executor.shutdown();
			try {
				// while (!executor.isTerminated())
				while (!executor.awaitTermination(60, TimeUnit.SECONDS))
					;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int hitsum = 0;
			for (int h : hit) {
				hitsum += h;
			}
			double recall = (double) hitsum / countTest;
			bw.write("\n" + "On the data set of " + Paras.dataset + ", When the k is: " + Paras.k + ", the K is: "
					+ Paras.K + ", the recall is: " + recall);
			StringBuffer sb = new StringBuffer();
			for (ArrayList<Integer> cases : this.hitcases) {
				for (int qid : cases) {
					sb.append(qid + ",");
				}
			}
			bw1.write("\n" + Paras.k + "\t" + sb.toString());
			br.close();
			reader.close();
			bw.close();
			writer.close();
			bw1.close();
			writer1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("the total number of test records is:
		// "+countTest);
		// System.out.println("the total number of records where there is less
		// than 20 items is: "+this.countTestRandom);
	}

	/*
	 * this method make recommendation based on all the candidate items
	 */
	public void recommend_baseUnvisited(String testFile) {
		System.out.println("recommend....");
		this.test_cases = new ArrayList<TestCases>();
		FileReader reader;
		int countTest = 0;
		int maxthreads = Paras.MAXTHREADSS;
		this.hit = new int[maxthreads];
		this.hitcases = new ArrayList[maxthreads];
		for (int i = 0; i < maxthreads; i++) {
			this.hitcases[i] = new ArrayList<Integer>();
		}
		try {
			FileWriter writer = new FileWriter(this.outputPath + "result.txt", true);
			BufferedWriter bw = new BufferedWriter(writer);
			FileWriter writer1 = new FileWriter(this.outputPath + "hitcases.txt", true);
			BufferedWriter bw1 = new BufferedWriter(writer1);
			reader = new FileReader(testFile);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				if (terms.length != 10)
					continue;
				countTest++;
				// System.out.println(countTest);
				// the format of the input file is:
				// userID/tweetID/lat/lon/time/placeID/contentInfo/state/seq/locationID/unvisitedItems
				int u = Integer.parseInt(terms[0]);
				// int l = Integer.parseInt(terms[9]);
				int targetV = Integer.parseInt(terms[5]);
				String contentsS = terms[6];
				StringTokenizer st1 = new StringTokenizer(contentsS, "|");
				ArrayList<Integer> contents = new ArrayList<Integer>();
				while (st1.hasMoreTokens())
					contents.add(new Integer(st1.nextToken()));
				this.Dvw[targetV] = contents;
				String seqs = terms[8];
				st1 = new StringTokenizer(seqs, ",");
				ArrayList<Integer> seq = new ArrayList<Integer>();
				while (st1.hasMoreTokens()) {
					int pvid = Integer.parseInt(st1.nextToken());
					if (pvid == -1) {
						break;
					}
					seq.add(pvid);
				}
				// for this user, find her unvisited items
				if (!this.unvisitedItems.containsKey(u)) {
					ArrayList<Integer> items = new ArrayList<Integer>();
					HashSet<Integer> visitedItems = this.Duv[u];
					for (int v = 0; v < this.V; v++) {
						if (!visitedItems.contains(v)) {
							items.add(v);
						}
					}
					this.unvisitedItems.put(u, items);
				}
				TestCases tc = new TestCases(countTest - 1, u, targetV, seq);
				this.test_cases.add(tc);
				// UserProfile up = this.test_cases.get(u);
				// if (up == null) {
				// up = new UserProfile(1000);
				// up.addOneRecord(targetV, seq, null);
				// // up.addOneRecord(targetV, l, seq, null);
				// this.test_cases.put(u, up);
				// } else {
				// // up.addOneRecord(targetV, l, seq, null);
				// up.addOneRecord(targetV, seq, null);
				// }
			}
			ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads, maxthreads, 1, TimeUnit.SECONDS,
					new LinkedBlockingQueue());
			// TODO Auto-generated method stub
			int count = 0;
			ArrayList<TestCases>[] joblists = new ArrayList[maxthreads];
			for (int i = 0; i < maxthreads; i++) {
				joblists[i] = new ArrayList<TestCases>();
			}
			for (TestCases query : this.test_cases) {
				joblists[count].add(query);
				count = (count + 1) % maxthreads;
			}
			for (int i = 0; i < maxthreads; i++) {
				executor.submit(new RecommenderBaseUnvisited(joblists[i], i));
			}
			executor.shutdown();
			try {
				// while (!executor.isTerminated())
				while (!executor.awaitTermination(60, TimeUnit.SECONDS))
					;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int hitsum = 0;
			for (int h : hit) {
				hitsum += h;
			}
			double recall = (double) hitsum / countTest;
			bw.write("\n" + "On the data set of " + Paras.dataset + ", When the k is: " + Paras.k + ", the K is: "
					+ Paras.K + ", the recall is: " + recall);
			StringBuffer sb = new StringBuffer();
			for (ArrayList<Integer> cases : this.hitcases) {
				for (int qid : cases) {
					sb.append(qid + ",");
				}
			}
			bw1.write("\n" + Paras.k + "\t" + sb.toString());
			br.close();
			reader.close();
			bw.close();
			writer.close();
			bw1.close();
			writer1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("the total number of test records is:
		// "+countTest);
		// System.out.println("the total number of records where there is less
		// than 20 items is: "+this.countTestRandom);
	}

	/*
	 * this method make recommendation based on all the candidate items
	 */
	public void recommend_baseAll(String testFile) {
		System.out.println("recommend....");
		this.test_cases = new ArrayList<TestCases>();
		FileReader reader;
		int countTest = 0;
		int maxthreads = Paras.MAXTHREADSS;
		this.hit = new int[maxthreads];
		this.hitcases = new ArrayList[maxthreads];
		for (int i = 0; i < maxthreads; i++) {
			this.hitcases[i] = new ArrayList<Integer>();
		}
		try {
			FileWriter writer = new FileWriter(this.outputPath + "result.txt", true);
			BufferedWriter bw = new BufferedWriter(writer);
			FileWriter writer1 = new FileWriter(this.outputPath + "hitcases.txt", true);
			BufferedWriter bw1 = new BufferedWriter(writer1);
			reader = new FileReader(testFile);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			int countTemp=0;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				if (terms.length != 10 && terms.length!=9)
					continue;
				countTemp++;
				if(Paras.dataset.equalsIgnoreCase("foursquare")&&countTemp%50!=0)
					continue;
				else if(Paras.dataset.equalsIgnoreCase("twitter")&&countTemp%100!=0)
					continue;
				countTest++;
				// System.out.println(countTest);
				// the format of the input file is:
				// userID/tweetID/lat/lon/time/placeID/contentInfo/state/seq/locationID/unvisitedItems
				int u = Integer.parseInt(terms[0]);
				// int l = Integer.parseInt(terms[9]);
				int targetV = Integer.parseInt(terms[5]);
				String contentsS = terms[6];
				StringTokenizer st1 = new StringTokenizer(contentsS, "|");
				ArrayList<Integer> contents = new ArrayList<Integer>();
				while (st1.hasMoreTokens())
					contents.add(new Integer(st1.nextToken()));
				this.Dvw[targetV] = contents;
				String seqs = terms[8];
				st1 = new StringTokenizer(seqs, ",");
				ArrayList<Integer> seq = new ArrayList<Integer>();
				while (st1.hasMoreTokens()) {
					int pvid = Integer.parseInt(st1.nextToken());
					if (pvid == -1) {
						break;
					}
					seq.add(pvid);
				}
				TestCases tc = new TestCases(countTest - 1, u, targetV, seq);
				this.test_cases.add(tc);
			}
			ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads, maxthreads, 1, TimeUnit.SECONDS,
					new LinkedBlockingQueue());
			// TODO Auto-generated method stub
			int count = 0;
			ArrayList<TestCases>[] joblists = new ArrayList[maxthreads];
			for (int i = 0; i < maxthreads; i++) {
				joblists[i] = new ArrayList<TestCases>();
			}
			for (TestCases query : this.test_cases) {
				joblists[count].add(query);
				count = (count + 1) % maxthreads;
			}
			for (int i = 0; i < maxthreads; i++) {
				executor.submit(new RecommenderBaseAll(joblists[i], i));
			}
			executor.shutdown();
			try {
				// while (!executor.isTerminated())
				while (!executor.awaitTermination(60, TimeUnit.SECONDS))
					;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int hitsum = 0;
			for (int h : hit) {
				hitsum += h;
			}
			double recall = (double) hitsum / countTest;
			bw.write("\n" + "baseall: On the data set of " + Paras.dataset + ", When the k is: " + Paras.k + ", the K is: "
					+ Paras.K + ", the recall is: " + recall);
			StringBuffer sb = new StringBuffer();
			for (ArrayList<Integer> cases : this.hitcases) {
				for (int qid : cases) {
					sb.append(qid + ",");
				}
			}
			bw1.write("\n" + Paras.k + "\t" + sb.toString());
			br.close();
			reader.close();
			bw.close();
			writer.close();
			bw1.close();
			writer1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("the total number of test records is:
		// "+countTest);
		// System.out.println("the total number of records where there is less
		// than 20 items is: "+this.countTestRandom);
	}

	public void train() {
		int it = 0;
		try {
			OutputStreamWriter os = data_storage.file_handle_add(this.outputPath + "log.txt");
			while (it < this.ITERATIONS) {
				System.out.println("the " + it + "'s iteration.......");
//				long begintime = System.currentTimeMillis();
				this.qnm.shutUp();
				if (it % 5 == 0) {
					os.write("the " + it + "'s iteration......." + "\n");
					os.write("update theta 0" + "\n");
					os.flush();
					this.updateTheta0();
					os.write("update theta user" + "\n");
					os.flush();
					this.updateThetaUser();
					os.write("update theta pre" + "\n");
					os.flush();
					// this.updateThetaNative();
					// this.updateThetaPre();
					this.updateThetaPreSingleP();
					os.write("update theta phi0" + "\n");
					os.flush();
					this.updatePhi0();
					os.write("update theta phitopic" + "\n");
					os.flush();
					this.updatePhiTopic();
					os.write("update theta psi0" + "\n");
					os.flush();
					this.updatePsi0();
					os.write("update theta psitopic" + "\n");
					os.flush();
					this.updatePsiTopic();
				}
				for (int i = 0; i < this.U; i++) {
					for (int j = 0; j < this.user_items.get(i).getSize(); j++) {
						this.sampleTopic(i, j);
					}
				}
				it++;
//				System.out.println("over");
//				long duration = System.currentTimeMillis() - begintime;
//				System.out.println(this.formatDuring(duration));
			}
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void output_model() {
		System.out.println("output model ...");

		// parameter
		String parameter_file = outputPath + "hyper_parameter.txt";
		OutputStreamWriter oswpf = data_storage.file_handle(parameter_file);
		output_hyperparameter(oswpf);

		// matrix
		output_learntparameters(outputPath + "matrix/");

		System.out.println("output model ... done");
	}

	public void read_model() {
		System.out.println("read model ...");

		// matrix
		read_learntparameters(outputPath + "matrix/");

		System.out.println("read model ... done");
	}

	public void output_learntparameters(String base_path) {

		try {
			String topicPopularity_file = base_path + "topicPopularity.txt";
			OutputStreamWriter oswpf = data_storage.file_handle(topicPopularity_file);
			oswpf.write(K + "\n");
			for (int z = 0; z < K; z++) {
				oswpf.write(this.dz[z] + ",");
			}
			oswpf.flush();
			oswpf.close();

			String topicDis_file = base_path + "topicDis.txt";
			oswpf = data_storage.file_handle(topicDis_file);
			oswpf.write(K + "\n");
			for (int z = 0; z < K; z++) {
				oswpf.write(this.theta0[z] + ",");
			}
			oswpf.flush();
			oswpf.close();

			String thetauser_file = base_path + "userTopicDis.txt";
			oswpf = data_storage.file_handle(thetauser_file);
			oswpf.write(U + "," + K + "\n");
			for (int u = 0; u < this.U; u++) {
				for (int z = 0; z < K; z++) {
					oswpf.write(this.thetauser[u][z] + ",");
				}
				oswpf.write("\n");
			}

			oswpf.flush();
			oswpf.close();

			// String thetanative_file = base_path + "regionTopicDis.txt";
			// oswpf = data_storage.file_handle(thetanative_file);
			// oswpf.write(this.R + "," + K + "\n");
			// for (int l = 0; l < this.R; l++) {
			// for (int z = 0; z < K; z++) {
			// oswpf.write(this.thetanative[l][z] + ",");
			// }
			// oswpf.write("\n");
			// }
			//
			// oswpf.flush();
			// oswpf.close();

			String usertopic_file = base_path + "userTopicCount.txt";
			oswpf = data_storage.file_handle(usertopic_file);
			oswpf.write(U + "," + K + "\n");
			for (int u = 0; u < this.U; u++) {
				for (int z = 0; z < K; z++) {
					oswpf.write(this.duz[u][z] + ",");
				}
				oswpf.write("\n");
			}

			oswpf.flush();
			oswpf.close();

			// String regionTopic_file = base_path + "regionTopicCount.txt";
			// oswpf = data_storage.file_handle(regionTopic_file);
			// oswpf.write(this.R + "," + K + "\n");
			// for (int l = 0; l < this.R; l++) {
			// for (int z = 0; z < K; z++) {
			// oswpf.write(this.dlnz[l][z] + ",");
			// }
			// oswpf.write("\n");
			// }
			//
			// oswpf.flush();
			// oswpf.close();

			String wordPopularity_file = base_path + "wordPopularity.txt";
			oswpf = data_storage.file_handle(wordPopularity_file);
			oswpf.write(this.W + "\n");
			for (int w = 0; w < W; w++) {
				oswpf.write(this.dw[w] + ",");
			}
			oswpf.flush();
			oswpf.close();

			String wordDis_file = base_path + "wordDis.txt";
			oswpf = data_storage.file_handle(wordDis_file);
			oswpf.write(this.W + "\n");
			for (int w = 0; w < W; w++) {
				oswpf.write(this.phi0[w] + ",");
			}
			oswpf.flush();
			oswpf.close();

			// topicWord
			// get the corresponding word for each id
			FileReader fr = new FileReader("data/" + Paras.dataset + "/wordID.txt");
			BufferedReader br = new BufferedReader(fr);
			String[] words = new String[this.W];
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				String w = terms[0];
				int id = Integer.parseInt(terms[1]);
				words[id] = w;
			}
			br.close();
			fr.close();
			String topicWordDis_file = base_path + "topicWordDistribution.txt";
			oswpf = data_storage.file_handle(topicWordDis_file);
			oswpf.write(K + "," + W + "\n");
			for (int i = 0; i < K; i++) {
				for (int j = 0; j < W; j++) {
					oswpf.write(this.phitopic[i][j] + ",");
				}
				oswpf.write("\n");
			}
			oswpf.flush();
			oswpf.close();
			String topicWordCount_file = base_path + "topicWordCount.txt";
			oswpf = data_storage.file_handle(topicWordCount_file);
			oswpf.write(K + "," + W + "\n");
			for (int i = 0; i < K; i++) {
				for (int j = 0; j < W; j++) {
					oswpf.write(this.dzw[i][j] + ",");
				}
				oswpf.write("\n");
			}
			oswpf.flush();
			oswpf.close();
			String topicWord_file = base_path + "topicWord.txt";
			oswpf = data_storage.file_handle(topicWord_file);
			oswpf.write(K + "," + W + "\n");
			for (int i = 0; i < K; i++) {
				// write top 10 word for this topic
				int n = 10;
				int[] indexes = this.getFirst(this.phitopic[i], n);
				for (int index : indexes) {
					oswpf.write(words[index] + ",");
				}
				oswpf.write("\n");
			}
			oswpf.flush();
			oswpf.close();

			// topic Popularity
			String itemPopularity_file = base_path + "itemPopularity.txt";
			oswpf = data_storage.file_handle(itemPopularity_file);
			oswpf.write(V + "\n");
			for (int j = 0; j < V; j++) {
				oswpf.write(this.dv[j] + ",");
			}
			oswpf.flush();
			oswpf.close();
			String itemDis_file = base_path + "itemDis.txt";
			oswpf = data_storage.file_handle(itemDis_file);
			oswpf.write(V + "\n");
			for (int j = 0; j < V; j++) {
				oswpf.write(this.psi0[j] + ",");
			}
			oswpf.flush();
			oswpf.close();
			String topicItemDis_file = base_path + "topicItemDistribution.txt";
			oswpf = data_storage.file_handle(topicItemDis_file);
			oswpf.write(K + "," + V + "\n");
			for (int i = 0; i < K; i++) {
				for (int j = 0; j < V; j++) {
					oswpf.write(this.psitopic[i][j] + ",");
				}
				oswpf.write("\n");
			}
			oswpf.flush();
			oswpf.close();
			String topicItemCount_file = base_path + "topicItemCount.txt";
			oswpf = data_storage.file_handle(topicItemCount_file);
			oswpf.write(K + "," + V + "\n");
			for (int i = 0; i < K; i++) {
				for (int j = 0; j < V; j++) {
					oswpf.write(this.dzv[i][j] + ",");
				}
				oswpf.write("\n");
			}
			oswpf.flush();
			oswpf.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void output_hyperparameter(OutputStreamWriter oswpf) {
		try {
			oswpf.write("U: " + U + "\n");
			oswpf.write("V: " + V + "\n");
			// oswpf.write("R: " + R + "\n");
			oswpf.write("K: " + K + "\n");
			oswpf.write("W: " + W + "\n");
			oswpf.write("k: " + Paras.k + "\n");
			oswpf.write("ITERATIONS: " + ITERATIONS + "\n");
			oswpf.write("SAMPLE_LAG: " + SAMPLE_LAG + "\n");
			oswpf.write("BURN_IN: " + BURN_IN + "\n");
			oswpf.write("outputPath: " + outputPath + "\n");
			oswpf.flush();
			oswpf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void read_learntparameters(String base_path) {

		try {
			String topicDis_file = base_path + "topicDis.txt";
			BufferedReader br = data_storage.file_handle_read(topicDis_file);
			br.readLine();
			String str = br.readLine();
			String[] terms = str.split(",");
			for (int z = 0; z < K; z++) {
				this.theta0[z] = Double.parseDouble(terms[z]);
			}
			br.close();

			String thetauser_file = base_path + "userTopicDis.txt";
			br = data_storage.file_handle_read(thetauser_file);
			br.readLine();
			for (int u = 0; u < this.U; u++) {
				str = br.readLine();
				terms = str.split(",");
				for (int z = 0; z < K; z++) {
					this.thetauser[u][z] = Double.parseDouble(terms[z]);
				}
			}
			br.close();

			// String thetanative_file = base_path + "regionTopicDis.txt";
			// br = data_storage.file_handle_read(thetanative_file);
			// br.readLine();
			// for (int l = 0; l < this.R; l++) {
			// str = br.readLine();
			// terms = str.split(",");
			// for (int z = 0; z < K; z++) {
			// this.thetanative[l][z] = Double.parseDouble(terms[z]);
			// }
			// }
			// br.close();

			String wordDis_file = base_path + "wordDis.txt";
			br = data_storage.file_handle_read(wordDis_file);
			br.readLine();
			str = br.readLine();
			terms = str.split(",");
			for (int w = 0; w < W; w++) {
				this.phi0[w] = Double.parseDouble(terms[w]);
			}
			br.close();

			String topicWordDis_file = base_path + "topicWordDistribution.txt";
			br = data_storage.file_handle_read(topicWordDis_file);
			br.readLine();
			for (int i = 0; i < K; i++) {
				str = br.readLine();
				terms = str.split(",");
				for (int j = 0; j < W; j++) {
					this.phitopic[i][j] = Double.parseDouble(terms[j]);
				}
			}
			br.close();

			// item Popularity
			String itemDis_file = base_path + "itemDis.txt";
			br = data_storage.file_handle_read(itemDis_file);
			br.readLine();
			str = br.readLine();
			terms = str.split(",");
			for (int j = 0; j < V; j++) {
				this.psi0[j] = Double.parseDouble(terms[j]);
			}
			br.close();

			String topicItemDis_file = base_path + "topicItemDistribution.txt";
			br = data_storage.file_handle_read(topicItemDis_file);
			br.readLine();
			for (int i = 0; i < K; i++) {
				str = br.readLine();
				terms = str.split(",");
				for (int j = 0; j < V; j++) {
					this.psitopic[i][j] = Double.parseDouble(terms[j]);
				}
			}
			br.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * this method infer the beta and gamma offline
	 */
	public void preInfer() {
		System.out.println("preinfer....");
		this.betas = new double[K][this.W];
		this.gammas = new double[K][this.V];
		this.Fzvs = new double[K][V];
		int maxthreads = Paras.MAXTHREADSS;
		ThreadPoolExecutor executor = new ThreadPoolExecutor(maxthreads, maxthreads, 1, TimeUnit.SECONDS,
				new LinkedBlockingQueue());
		// TODO Auto-generated method stub
		int count = 0;
		ArrayList<Integer>[] joblists = new ArrayList[maxthreads];
		for (int i = 0; i < maxthreads; i++)
			joblists[i] = new ArrayList<Integer>();
		for (int z = 0; z < K; z++) {
			joblists[count].add(z);
			count = (count + 1) % maxthreads;
		}
		for (int i = 0; i < maxthreads; i++) {
			executor.submit(new PreInferer(joblists[i]));
		}
		executor.shutdown();
		try {
			// while (!executor.isTerminated())
			while (!executor.awaitTermination(60, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// output betas and gammas
		try {
			// theta0, the whole document has only one line and there are
			// Paras.K values in this line
			// phitopic, similar to phi0
			String fzv_file = outputPath + "matrix/" + "fzvs.txt";
			OutputStreamWriter oswpf = data_storage.file_handle(fzv_file);
			for (int v = 0; v < this.V; v++) {
				for (int z = 0; z < K; z++) {
					oswpf.write(this.Fzvs[z][v] + ",");
				}
				oswpf.write("\n");
			}
			oswpf.flush();
			oswpf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void readFzvs() {
		this.Fzvs = new double[K][this.V];
		try {
			String fzv_file = outputPath + "matrix/" + "fzvs.txt";
			BufferedReader br = data_storage.file_handle_read(fzv_file);
			for (int v = 0; v < this.V; v++) {
				String str = br.readLine();
				String[] terms = str.split(",");
				for (int z = 0; z < K; z++) {
					this.Fzvs[z][v] = Double.parseDouble(terms[z]);
				}
			}
			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// public double inferAlpha(int u, int l, int z, ArrayList<Integer> seq) {
	public double inferAlpha(int u, int z, ArrayList<Integer> seq) {
		double n = this.inferAlphaN(u, z, seq);
		double d = 0;
		for (int zz = 0; zz < Paras.K; zz++) {
			d += this.inferAlphaN(u, zz, seq);
		}
		return n / d;
	}

	public double inferAlphaN(int u, int z, ArrayList<Integer> seq) {
		// public double inferAlphaN(int u, int l, int z, ArrayList<Integer>
		// seq) {
		double exp = 0;
		// exp += (this.theta0[z] +
		// this.thetauser[u][z]+this.thetanative[l][z]);
		exp += (this.theta0[z] + this.thetauser[u][z]);
		for (int pvid : seq) {
			exp += this.thetapre[pvid][z];
		}
		return Math.exp(exp);
	}

	public double inferBeta(int z, int w) {
		double n = this.inferBetaN(z, w);
		double d = 0;
		for (int ww = 0; ww < this.W; ww++) {
			d += this.inferBetaN(z, ww);
		}
		return n / d;
	}

	public double inferBetaN(int z, int w) {
		double exp = 0;
		exp += (this.phi0[w] + this.phitopic[z][w]);
		return Math.exp(exp);
	}

	public double inferGamma(int z, int v) {
		double n = this.inferGammaN(z, v);
		double d = 0;
		for (int vv = 0; vv < this.V; vv++) {
			d += this.inferGammaN(z, vv);
		}
		return n / d;
	}

	public double inferGammaN(int z, int v) {
		double exp = 0;
		exp += (this.psi0[v] + this.psitopic[z][v]);
		return Math.exp(exp);
	}

	public static void main(String[] args) {
		long begintime = System.currentTimeMillis();
		// sp.initializeCount();
		// sp.train();
		// sp.output_model();
		sp.read_model();
		sp.readFzvs();
		// sp.preInfer();
		String testFile = "data/" + Paras.dataset + "/test.txt";
		// sp.recommend(testFile);
		for (Paras.k = 20; Paras.k >= 2; Paras.k = Paras.k - 2) {
			// sp.recommend(testFile);
//			sp.recommend_baseUnvisited(testFile);
			sp.recommend_baseAll(testFile);
		}
		 sp.output_query_vectors(testFile);
		long duration = System.currentTimeMillis() - begintime;
		System.out.println(sp.formatDuring(duration));
	}

	public static String formatDuring(long mss) {
		// long days = mss / (1000 * 60 * 60 * 24);
		long hours = mss / (1000 * 60 * 60);
		long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
		long seconds = (mss % (1000 * 60)) / 1000;
		return hours + " hours " + minutes + " minutes " + seconds + " seconds ";
	}

	/*
	 * return the indexes of the first num
	 */
	public int[] getFirst(double[] array, int num) {
		int[] ri = new int[num];
		double[] r = new double[num];
		double min = array[0];
		int mini = 0;
		for (int i = 0; i < num; i++) {
			r[i] = array[i];
			ri[i] = i;
			if (r[i] < min) {
				min = r[i];
				mini = i;
			}
		}
		for (int i = num; i < array.length; i++) {
			double v1 = array[i];
			min = r[0];
			mini = 0;
			for (int j = 1; j < num; j++) {
				double v2 = r[j];
				if (v2 < min) {
					min = v2;
					mini = j;
				}
			}
			if (v1 > min) {
				r[mini] = v1;
				ri[mini] = i;
			}
		}
		return ri;
	}

	/*
	 * this method will output all the query vectors format: query id(start from
	 * 0) \t vector
	 * 
	 * at the same time, the method will also output the candidate item ids for
	 * each query format: query id \t number of candidate items(include the
	 * target item) \t targetV, vid1, vid2.....
	 */
	public void output_query_vectors_withCandidate(String testFile) {
		System.out.println("output the query vectors....");
		FileReader reader;
		int maxthreads = Paras.MAXTHREADSS;
		// ArrayList<Query> queries=new ArrayList<Query>();
		this.hit = new int[maxthreads];
		try {
			FileWriter writer = new FileWriter(this.outputPath + "query_vectors.txt");
			BufferedWriter bw = new BufferedWriter(writer);
			FileWriter writer1 = new FileWriter(this.outputPath + "query_candidate_items.txt");
			BufferedWriter bw1 = new BufferedWriter(writer1);
			reader = new FileReader(testFile);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			int count=-1;
			int qid = -1;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				if (terms.length != 10)
					continue;
				count++;
				if (count % 10 != 0)
					continue;
				qid++;
				// the format of the input file is:
				// userID/tweetID/lat/lon/time/placeID/contentInfo/state/seq/unvisitedItems
				int u = Integer.parseInt(terms[0]);
				// int l = Integer.parseInt(terms[9]);
				int targetV = Integer.parseInt(terms[5]);
				String seqs = terms[8];
				StringTokenizer st1 = new StringTokenizer(seqs, ",");
				ArrayList<Integer> seq = new ArrayList<Integer>();
				while (st1.hasMoreTokens()) {
					int pvid = Integer.parseInt(st1.nextToken());
					if (pvid == -1) {
						break;
					}
					seq.add(pvid);
				}
				if (!this.nearbyItems.containsKey(targetV)) {
					String itemss = terms[9];
					// String itemss=terms[10];
					st1 = new StringTokenizer(itemss, ",");
					ArrayList<Integer> items = new ArrayList<Integer>();
					while (st1.hasMoreTokens()) {
						int vid = Integer.parseInt(st1.nextToken());
						items.add(vid);
					}
					this.nearbyItems.put(targetV, items);
				}
				ArrayList<Integer> nearby_items = this.nearbyItems.get(targetV);
				// infer the vector
				double[] wqzs = new double[K];
				for (int z = 0; z < K; z++) {
					double ratingz = 0;
					ratingz = this.inferAlpha(u, z, seq);
					wqzs[z] = ratingz;
				}
				StringBuffer sb = new StringBuffer();
				for (double v : wqzs) {
					sb.append(v + ",");
				}
				bw.write(qid + "\t" + sb.toString() + "\n");
				// at the same time, the method will also output the candidate
				// item ids for
				// each query format: query id \t number of candidate
				// items(include the target item) \t targetV, vid1, vid2.....
				sb = new StringBuffer();
				sb.append(targetV + ",");
				for (int item : nearby_items) {
					sb.append(item + ",");
				}
				int size = nearby_items.size() + 1;
				bw1.write(qid + "\t" + size + "\t" + sb.toString() + "\n");
			}
			br.close();
			reader.close();
			bw.close();
			writer.close();
			bw1.close();
			writer1.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void output_query_vectors(String testFile) {
		System.out.println("output the query vectors....");
		FileReader reader;
		int maxthreads = Paras.MAXTHREADSS;
		// ArrayList<Query> queries=new ArrayList<Query>();
		this.hit = new int[maxthreads];
		try {
			FileWriter writer = new FileWriter(this.outputPath + "query_vectors.txt");
			BufferedWriter bw = new BufferedWriter(writer);
			FileWriter writer1 = new FileWriter(this.outputPath + "query_candidate_items.txt");
			BufferedWriter bw1 = new BufferedWriter(writer1);
			reader = new FileReader(testFile);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			int qid = -1;
			int count=-1;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				if (terms.length != 10)
					continue;
				count++;
				if(Paras.dataset.equalsIgnoreCase("foursquare")&&count%50!=0)
					continue;
				else if(Paras.dataset.equalsIgnoreCase("twitter")&&count%100!=0)
					continue;
				qid++;
				// the format of the input file is:
				// userID/tweetID/lat/lon/time/placeID/contentInfo/state/seq/unvisitedItems
				int u = Integer.parseInt(terms[0]);
				// int l = Integer.parseInt(terms[9]);
				int targetV = Integer.parseInt(terms[5]);
				String seqs = terms[8];
				StringTokenizer st1 = new StringTokenizer(seqs, ",");
				ArrayList<Integer> seq = new ArrayList<Integer>();
				while (st1.hasMoreTokens()) {
					int pvid = Integer.parseInt(st1.nextToken());
					if (pvid == -1) {
						break;
					}
					seq.add(pvid);
				}
				// if (!this.nearbyItems.containsKey(targetV)) {
				// String itemss = terms[9];
				// // String itemss=terms[10];
				// st1 = new StringTokenizer(itemss, ",");
				// ArrayList<Integer> items = new ArrayList<Integer>();
				// while (st1.hasMoreTokens()) {
				// int vid = Integer.parseInt(st1.nextToken());
				// items.add(vid);
				// }
				// this.nearbyItems.put(targetV, items);
				// }
				// ArrayList<Integer> nearby_items =
				// this.nearbyItems.get(targetV);
				// infer the vector
				double[] wqzs = new double[K];
				for (int z = 0; z < K; z++) {
					double ratingz = 0;
					ratingz = this.inferAlpha(u, z, seq);
					wqzs[z] = ratingz;
				}
				StringBuffer sb = new StringBuffer();
				for (double v : wqzs) {
					sb.append(v + ",");
				}
				bw.write(qid + "\t" + sb.toString() + "\n");
				// at the same time, the method will also output the candidate
				// item ids for
				// each query format: query id \t number of candidate
				// items(include the target item) \t targetV, vid1, vid2.....
				sb = new StringBuffer();
				sb.append(targetV + "," + "-1");
				// for (int item : nearby_items) {
				// sb.append(item + ",");
				// }
				// int size = nearby_items.size() + 1;
				bw1.write(qid + "\t" + 0 + "\t" + sb.toString() + "\n");
			}
			br.close();
			reader.close();
			bw.close();
			writer.close();
			bw1.close();
			writer1.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
