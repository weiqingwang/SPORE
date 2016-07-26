package utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import model.Paras;

public class Divider {
	private final double tp = 0.8;
	private int[] counts;
	private int[] trCounts;//store the number of trCounts for each user
	private PairDouble[] locations;//store the lat and lon for each item
	private HashSet<Integer> trainuids;
	private HashSet<Integer> trainvids;
	private String preAdd = "data/" + Paras.dataset + "/";
	private int U = Paras.countU;
	private int V=Paras.countV;
//	private Hashtable<Integer,ArrayList<Integer>> nearbyItems;

	public Divider() {
		this.counts = new int[U];
		this.trCounts=new int[U];
		this.trainuids=new HashSet<Integer>();
		this.trainvids=new HashSet<Integer>();
		this.locations=new PairDouble[this.V];
//		this.nearbyItems=new Hashtable<Integer, ArrayList<Integer>>();
	}

	public static void main(String[] args) {
		Divider obj = new Divider();
		obj.getCount(obj.preAdd + "checkins.txt");
		if (Paras.dataset.equalsIgnoreCase("foursquare"))
			obj.divideFou(obj.preAdd + "checkins.txt", obj.preAdd + "train.txt", obj.preAdd + "test1.txt");
		else
			obj.divideTwi(obj.preAdd + "checkins.txt", obj.preAdd + "train.txt", obj.preAdd + "test1.txt");
		obj.filter(obj.preAdd + "train.txt", obj.preAdd + "test1.txt",
				obj.preAdd + "test.txt");
	}

	/*
	 * this method counts the records for each user£¬ result stored in counts
	 */
	public void getCount(String file) {
		FileReader reader;
		try {
			reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				int uid = Integer.parseInt(terms[0]);
				this.counts[uid]++;
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
		for(int u=0;u<U;u++){
			this.trCounts[u]=(int)(this.counts[u]*this.tp);
		}
	}

	/*
	 * this method first divide the records into train and test according to the
	 * tp, there is difference between the dataset twitter and foursquare
	 */
	public void divideFou(String souFile, String trFile, String teFile) {
		FileReader reader;
		FileWriter writertr;
		FileWriter writerte;
		int[] countsT=new int[this.U];//store the counts of read record for each user
		try {
			reader = new FileReader(souFile);
			BufferedReader br = new BufferedReader(reader);
			writertr = new FileWriter(trFile);
			BufferedWriter bwtr = new BufferedWriter(writertr);
			writerte = new FileWriter(teFile);
			BufferedWriter bwte = new BufferedWriter(writerte);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms=str.split("\t");
				int uid=Integer.parseInt(terms[0]);
				int vid=Integer.parseInt(terms[5]);
				if(this.locations[vid]==null){
					double lat=Double.parseDouble(terms[2]);
					double lon=Double.parseDouble(terms[3]);
					PairDouble p=new PairDouble(lat,lon);
					this.locations[vid]=p;
				}
				countsT[uid]++;
				if(countsT[uid]>(this.counts[uid]-this.trCounts[uid])){
					bwtr.write(str+"\n");
					this.trainuids.add(uid);
					this.trainvids.add(vid);
				}
				else{
					bwte.write(str+"\n");
				}
			}
			br.close();
			reader.close();
			bwtr.close();
			writertr.close();
			bwte.close();
			writerte.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void divideTwi(String souFile, String trFile, String teFile) {
		FileReader reader;
		FileWriter writertr;
		FileWriter writerte;
		int[] countsT=new int[this.U];//store the counts of read record for each user
		try {
			reader = new FileReader(souFile);
			BufferedReader br = new BufferedReader(reader);
			writertr = new FileWriter(trFile);
			BufferedWriter bwtr = new BufferedWriter(writertr);
			writerte = new FileWriter(teFile);
			BufferedWriter bwte = new BufferedWriter(writerte);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms=str.split("\t");
				int uid=Integer.parseInt(terms[0]);
				int vid=Integer.parseInt(terms[5]);
				if(this.locations[vid]==null){
					double lat=Double.parseDouble(terms[2]);
					double lon=Double.parseDouble(terms[3]);
					PairDouble p=new PairDouble(lat,lon);
					this.locations[vid]=p;
				}
				countsT[uid]++;
				if(countsT[uid]<=this.trCounts[uid]){
					bwtr.write(str+"\n");
					this.trainuids.add(uid);
					this.trainvids.add(vid);
				}
				else{
					bwte.write(str+"\n");
				}
			}
			br.close();
			reader.close();
			bwtr.close();
			writertr.close();
			bwte.close();
			writerte.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * this method filters the new users and new items in the test dataset
	 */
	public void filter(String trFile, String teSou, String teTar) {
		FileReader reader;
		FileWriter writertr;
		FileWriter writerte;
		try {
			reader = new FileReader(teSou);
			BufferedReader br = new BufferedReader(reader);
			writertr = new FileWriter(trFile, true);
			BufferedWriter bwtr = new BufferedWriter(writertr);
			writerte = new FileWriter(teTar);
			BufferedWriter bwte = new BufferedWriter(writerte);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms=str.split("\t");
				int uid=Integer.parseInt(terms[0]);
				int vid=Integer.parseInt(terms[5]);
				if(this.trainuids.contains(uid)&&this.trainvids.contains(vid)){
//					ArrayList<Integer> items=new ArrayList<Integer>();
//					if(this.nearbyItems.containsKey(vid))
//						items=this.nearbyItems.get(vid);
//					else{
//						//find the nearby items
//						this.find_nearby(vid, items);
//						this.nearbyItems.put(vid, items);
//					}
//					StringBuffer sb=new StringBuffer();
//					for(int item:items){
//						sb.append(item+",");
//					}
//					sb.deleteCharAt(sb.length()-1);
					bwte.write(str+"\n");
				}
				else{
					bwtr.write(str+"\n");
					this.trainuids.add(uid);
					this.trainvids.add(vid);
				}
			}
			br.close();
			reader.close();
			bwtr.close();
			writertr.close();
			bwte.close();
			writerte.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
