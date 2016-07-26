package efficiency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import model.Paras;

public class EfficiencyTest {
	private static String path="src/efficiency/data/" + Paras.dataset + "/";
	private static String items_file=path+"total_items.txt";
	private static String sorted_list_file=path+"sorted_list.txt";
	private static String query_file=path+"query_vectors.txt";
	private static String result_file=path+"result.txt";
	private static ArrayList<double[]> queries=new ArrayList<double[]>();
	
	public static void generate_sorted_list(){//read items_file
		double[][] items=new double[Paras.countV][Paras.K];
		int[][] sorted_list=new int[Paras.K][Paras.countV];
		BufferedReader br=Data_reader_writer.file_handle_read(items_file);
		items=Data_reader_writer.load_matrix(items_file, Paras.countV, Paras.K);
		
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
		PriorityQueue<Item> L;

		// initialize the sorted list according to the F(v,z) stored in
		// this.items
		for (int i = 0; i < Paras.K; i++)

		{
			L = new PriorityQueue<Item>(Paras.countV+1000, cmp);
			for (int j = 0; j < Paras.countV; j++) {
				Item v = new Item(j, items[j][i]);
				L.add(v);

			}

			int k = 0;
			while (!L.isEmpty()) {
				sorted_list[i][k] = L.poll().id;
				k++;

			}

			L.clear();

		}
		
		//write the sorted list into the file
		OutputStreamWriter osw=Data_reader_writer.file_handle(sorted_list_file);
		Data_reader_writer.store_matrix(osw, sorted_list, Paras.K, Paras.countV);		
	}
	
	public static void readQueries(){
		BufferedReader br=Data_reader_writer.file_handle_read(query_file);
		String line="";
		try {
			while((line=br.readLine())!=null){
				String[] terms=line.split("\t");
				String vectorS=terms[1];
				String[] terms1=vectorS.split(",");
				double[] query=new double[Paras.K];
				for(int z=0;z<Paras.K;z++){
					query[z]=Double.parseDouble(terms1[z]);
				}
				queries.add(query);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		generate_sorted_list();
		TA ta=new TA(items_file,sorted_list_file);
		readQueries();
		for(Paras.k=20;Paras.k>=5;Paras.k=Paras.k-5){
			System.out.println("TA, k="+Paras.k);
			long begintime = System.currentTimeMillis();
			for(double[] query:queries){
				ta.query(query, Paras.k);
			}
			long duration = System.currentTimeMillis() - begintime;
			System.out.println(duration+"\t"+queries.size());
			double average=(double)duration/queries.size();
			OutputStreamWriter osw=Data_reader_writer.file_handle_append(result_file);
			try {
				osw.write(Paras.dataset+", 0.5million, TA: when k="+Paras.k+", the average processing time is "+average+"\n");
				osw.flush();
				osw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Paras.k=1;
		System.out.println("TA, k="+Paras.k);
		long begintime1 = System.currentTimeMillis();
		for(double[] query:queries){
			ta.query(query, Paras.k);
		}
		long duration1 = System.currentTimeMillis() - begintime1;
		System.out.println(duration1+"\t"+queries.size());
		double average1=(double)duration1/queries.size();
		OutputStreamWriter osw1=Data_reader_writer.file_handle_append(result_file);
		try {
			osw1.write(Paras.dataset+", 0.5million, TA: when k="+Paras.k+", the average processing time is "+average1+"\n");
			osw1.flush();
			osw1.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(Paras.k=20;Paras.k>=5;Paras.k=Paras.k-5){
			System.out.println("LS, k="+Paras.k);
			long begintime = System.currentTimeMillis();
			for(double[] query:queries){
				ta.LinearScan(query, Paras.k);
			}
			long duration = System.currentTimeMillis() - begintime;
			System.out.println(duration+"\t"+queries.size());
			double average=(double)duration/queries.size();
			OutputStreamWriter osw=Data_reader_writer.file_handle_append(result_file);
			try {
				osw.write(Paras.dataset+", 0.5million, LS: when k="+Paras.k+", the average processing time is "+average+"\n");
				osw.flush();
				osw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Paras.k=1;
		System.out.println("LS, k="+Paras.k);
		long begintime = System.currentTimeMillis();
		for(double[] query:queries){
			ta.LinearScan(query, Paras.k);
		}
		long duration = System.currentTimeMillis() - begintime;
		System.out.println(duration+"\t"+queries.size());
		double average=(double)duration/queries.size();
		OutputStreamWriter osw=Data_reader_writer.file_handle_append(result_file);
		try {
			osw.write(Paras.dataset+", 0.5million, LS: when k="+Paras.k+", the average processing time is "+average+"\n");
			osw.flush();
			osw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
