package efficiency;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

import model.Paras;



public class TA {

	// HashMap<Integer,double[]> items;
	// items[v][z], record F(z,v)
//	public double[][] items;
	// sorted_list[z][v] record the priority list for each topic
//	public int[][] sorted_list;
	public double threshold;
	public double[][] items;
	public int[][] sorted_list;
//	private BufferedReader reader_items;
//	private BufferedReader reader_lists;
	
	public int K;
	public int V;

	public int total_comparison = 0;
	public int total_comparisonEn = 0;
	public int total_comparisonsq = 0;

	public TA(String items_file, String sorted_list) {
		this.K=Paras.K;
		this.V=Paras.countV;
		this.items = Data_reader_writer.load_matrix(items_file, V, K);
		this.sorted_list=Data_reader_writer.load_matrix_int(sorted_list, K, V);
//		this.sorted_list = new int[items[0].length][items.length];
//		this.reader_items=Data_reader_writer.file_handle_read(items_file);
//		this.reader_lists=Data_reader_writer.file_handle_read(sorted_list_file);
	}

//	public void initialize() {
//
//		Comparator<Item> cmp;
//		cmp = new Comparator<Item>() {
//			public int compare(Item p1, Item p2) {
//				double s1 = p1.score;
//				double s2 = p2.score;
//				if (s1 > s2) {
//					return -1;
//				} else if (s2 > s1) {
//					return 1;
//				} else
//					return 0;
//			}
//		};
//		PriorityQueue<Item> L;
//
//		// initialize the sorted list according to the F(v,z) stored in
//		// this.items
//		for (int i = 0; i < this.K; i++)
//
//		{
//			L = new PriorityQueue<Item>(V+1000, cmp);
//			for (int j = 0; j < this.V; j++) {
//				Item v = new Item(j, this.items[j][i]);
//				L.add(v);
//
//			}
//
//			int k = 0;
//			while (!L.isEmpty()) {
//				this.sorted_list[i][k] = L.poll().id;
//				k++;
//
//			}
//
//			L.clear();
//
//		}
//
//	}

	// store top-k, there is no variables corresponding to the PL.
	public Item[] querysquential(double[] query, int k) {
		Item[] results = new Item[k];
		HashSet<Integer> resultset = new HashSet<Integer>();// not corresponding
															// to L, store the
															// scanned items,
															// corresponding to
															// the
															// nextListToCheck

		int numberofexamined = 0;

		Comparator<Item> cmp;
		cmp = new Comparator<Item>() {
			public int compare(Item p1, Item p2) {
				double s1 = p1.score;
				double s2 = p2.score;
				if (s1 > s2) {
					return -1;
				} else if (s2 > s1) {
					return 1;
				} else
					return 0;
			}
		};
		// PriorityQueue<Item> rankedattributes=new
		// PriorityQueue<Item>(2*this.dimension,cmp);
		// store the results, corresponding to L
		PriorityQueue<Item> resultlist = new PriorityQueue<Item>(2 * k, cmp);

		int[] pointers = new int[this.K];
		for (int i = 0; i < this.K; i++) {
			pointers[i] = 0;
		}

		double thresh = computeThreshold(query, pointers);

		boolean iscontinue = true;
		boolean isskip = false;
		while (iscontinue) {

			for (int t = 0; t < this.K; t++) {
				// Item ids=rankedattributes.poll();
				// int list_id=ids.id;
				// double list_score=ids.score;

				int item =this.sorted_list[t][pointers[t]];

				pointers[t] += 1;

				if (!resultset.contains(item)) {
					double score = computescore(query, this.items[item]);

					numberofexamined++;
					Item it = new Item(item, score * -1);
					resultset.add(it.id);

					if (resultlist.size() < k) {
						// resultset.add(item);
						resultlist.add(it);

					} else {
						Item lastone = resultlist.peek();
						double topkscore = lastone.score * -1;
						// Iterator<Item> its = resultlist.iterator();
						// Item lastone=new Item();

						if (topkscore >= thresh) {
							isskip = true;
							break;
						}

						if (topkscore < score) {
							// resultset.remove(lastone.id);

							resultlist.remove();
							resultlist.add(it);

						}

					}

				}

				if (resultset.size() == this.V) {
					isskip = true;
					break;
				}

				if (pointers[t] < this.V) {

					// int itemid=this.sorted_list[list_id][pointers[list_id]];
					// double s=computescore(query,this.items[itemid]);
					// Item diz=new Item(list_id,s);
					// rankedattributes.add(diz);

					thresh = computeThreshold(query, pointers);

				} else {
					isskip = true;
					break;
				}
			}

			if (isskip) {
				break;
			}

		}

		for (int i = k - 1; i >= 0; i--) {

			results[i] = resultlist.poll();
		}

		this.total_comparisonsq += numberofexamined;
		return results;
	}

	// store top-k
	public Item[] query(double[] query, int k) {
		Item[] results = new Item[k];
		HashSet<Integer> resultset = new HashSet<Integer>();

		int numberofexamined = 0;

		Comparator<Item> cmp;
		cmp = new Comparator<Item>() {
			public int compare(Item p1, Item p2) {
				double s1 = p1.score;
				double s2 = p2.score;
				if (s1 > s2) {
					return -1;
				} else if (s2 > s1) {
					return 1;
				} else
					return 0;
			}
		};
		//priority list, stores topic id
		PriorityQueue<Item> rankedattributes = new PriorityQueue<Item>(
				2 * this.K, cmp);
		PriorityQueue<Item> resultlist = new PriorityQueue<Item>(2 * k, cmp);

		for (int i = 0; i < this.K; i++) {

			int item = this.sorted_list[i][0];
			double score = computescore(query, this.items[item]);

			Item t = new Item(i, score);

			rankedattributes.add(t);
		}

		int[] pointers = new int[this.K];
		for (int i = 0; i < this.K; i++) {
			pointers[i] = 0;
		}

		double thresh = computeThreshold(query, pointers);

		boolean iscontinue = true;
		while (iscontinue) {
			int list_id = rankedattributes.poll().id;

			int item = this.sorted_list[list_id][pointers[list_id]];

			pointers[list_id] += 1;

			if (!resultset.contains(item)) {
				double score = computescore(query, this.items[item]);
				numberofexamined++;
				Item it = new Item(item, score * -1);
				if (resultset.size() < k) {
					resultset.add(item);
					resultlist.add(it);

				} else {
					Item lastone = resultlist.peek();
					double topkscore = lastone.score * -1;
					// Iterator<Item> its = resultlist.iterator();
					// Item lastone=new Item();

					if (topkscore >= thresh) {
						break;
					}

					if (topkscore < score) {
						resultset.remove(lastone.id);
						resultset.add(it.id);
						resultlist.remove();
						resultlist.add(it);

					}

				}

			}

			if (pointers[list_id] < this.V) {

				int itemid = this.sorted_list[list_id][pointers[list_id]];
				double s = computescore(query, this.items[item]);
				Item diz = new Item(list_id, s);
				rankedattributes.add(diz);

				thresh = computeThreshold(query, pointers);

			}

		}

		for (int i = k - 1; i >= 0; i--) {

			results[i] = resultlist.poll();
		}

		this.total_comparison += numberofexamined;
		return results;
	}

	// store top-k
	public Item[] queryEn(double[] query, int k) {
		Item[] results = new Item[k];
		HashSet<Integer> resultset = new HashSet<Integer>();

		int numberofexamined = 0;

		Comparator<Item> cmp;
		cmp = new Comparator<Item>() {
			public int compare(Item p1, Item p2) {
				double s1 = p1.score;
				double s2 = p2.score;
				if (s1 > s2) {
					return -1;
				} else if (s2 > s1) {
					return 1;
				} else
					return 0;
			}
		};
		PriorityQueue<Item> rankedattributes = new PriorityQueue<Item>(
				2 * this.K, cmp);
		PriorityQueue<Item> resultlist = new PriorityQueue<Item>(2 * k, cmp);

		for (int i = 0; i < this.K; i++) {

			int item = this.sorted_list[i][0];
			double score = computescore(query, this.items[item]);

			Item t = new Item(i, score);

			rankedattributes.add(t);
		}

		int[] pointers = new int[this.K];
		for (int i = 0; i < this.K; i++) {
			pointers[i] = 0;
		}

		double thresh = computeThreshold(query, pointers);

		boolean iscontinue = true;
		while (iscontinue) {
			Item ids = rankedattributes.poll();
			int list_id = ids.id;
			double list_score = ids.score;

			int item =this.sorted_list[list_id][pointers[list_id]];

			pointers[list_id] += 1;

			if (!resultset.contains(item)) {
				// double score=computescore(query,this.items[item]);
				// numberofexamined++;

				Item it = new Item(item, list_score * -1);

				if (resultlist.size() < k) {
					// resultset.add(item);
					resultlist.add(it);

				} else {
					Item lastone = resultlist.peek();
					double topkscore = lastone.score * -1;
					// Iterator<Item> its = resultlist.iterator();
					// Item lastone=new Item();

					if (topkscore >= thresh) {
						break;
					}

					if (topkscore < list_score) {
						// resultset.remove(lastone.id);

						resultlist.remove();
						resultlist.add(it);

					}

				}

				resultset.add(it.id);
				if (resultset.size() == this.V) {
					break;
				}

			}

			if (pointers[list_id] < this.V) {

				int itemid =this.sorted_list[list_id][pointers[list_id]];

				while (resultset.contains(itemid)) {
					pointers[list_id] += 1;
					if (pointers[list_id] >= this.V)
						break;

					itemid = this.sorted_list[list_id][pointers[list_id]];
				}

				if (pointers[list_id] >= this.V) {
					break;
				}

				double s = computescore(query, this.items[item]);
				numberofexamined++;
				Item diz = new Item(list_id, s);
				rankedattributes.add(diz);

				thresh = computeThreshold(query, pointers);

			} else {
				break;
			}

		}

		for (int i = k - 1; i >= 0; i--) {

			results[i] = resultlist.poll();
		}

		this.total_comparisonEn += numberofexamined;
		return results;
	}

	/*
	 * query[z] stores the W(u,z), what is item
	 */
	public double computescore(double[] query, double[] item) {

		double result = 0;
		assert (query.length == item.length);
		for (int i = 0; i < query.length; i++) {
			result += query[i] * item[i];
		}

		return result;
	}

	/*
	 * query[z] stores the W(u,z), pointers[z] is the priority list; pointers[i]
	 * stores the next item for topic i; ????ignore the situation where
	 * two items in one PL are all considered
	 */
	public double computeThreshold(double[] query, int[] pointers) {
		assert (query.length == pointers.length);
		double max = 0;
		for (int i = 0; i < query.length; i++) {
			int item =this.sorted_list[i][pointers[i]];
			max += query[i] * this.items[item][i];
		}

		return max;
	}

	public Item[] LinearScan(double[] query, int k) {
		Item[] results = new Item[k];

		Comparator<Item> cmp;
		cmp = new Comparator<Item>() {
			public int compare(Item p1, Item p2) {
				double s1 = p1.score;
				double s2 = p2.score;
				if (s1 > s2) {
					return -1;
				} else if (s2 > s1) {
					return 1;
				} else
					return 0;
			}
		};

		PriorityQueue<Item> resultlist = new PriorityQueue<Item>(this.V+1000, cmp);

		for (int i = 0; i < this.V; i++) {
			double score = computescore(query, this.items[i]);
			Item it = new Item(i, score);
			resultlist.add(it);

		}

		int size = 0;
		while (!resultlist.isEmpty()) {
			if (size == k) {
				break;
			} else {

				results[size] = resultlist.poll();
				size++;
			}

		}

		return results;

	}

}
