package utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

public class Preprocessor {
	private int U = 3683;// Foursquare
	private int V = 425286;
	private final int m = 5;// every location has to be visited by at least m
							// times
	private final int n = 20;// every user has at least n check-ins
	private String dataset = "twitter";
	private String preAdd;

	public Preprocessor() {
		if (this.dataset.equalsIgnoreCase("twitter")) {
			U = 61413;
			V = 42304;
		}
		preAdd = "data/" + dataset + "/";
	}

	public static void main(String[] args) {
		Preprocessor obj = new Preprocessor();
		// userID£¬ twitterID£¬ lat£¬ lon, time, poiID, contentWords,state,
		// hometown(CA), friends
//		obj.checkLoc(obj.preAdd + "checkinsRaw.txt", obj.preAdd + "checkinsPre1.txt");
//		obj.checkUser(obj.preAdd + "checkinsPre1.txt", obj.preAdd + "checkinsPre2.txt");
		obj.transID(obj.preAdd + "checkinsPre2.txt", obj.preAdd + "checkinsPre.txt", obj.preAdd + "locTransPre.txt",
				obj.preAdd + "userTransPre.txt", obj.preAdd + "wordID.txt");
	}

	/**
	 * every location has to be visited by at least m times
	 */
	public void checkLoc(String souFile, String tarFile) {
		FileReader reader;
		FileWriter writer;
		int[] counts = new int[this.V];
		try {
			reader = new FileReader(souFile);
			BufferedReader br = new BufferedReader(reader);
			writer = new FileWriter(tarFile);
			BufferedWriter bw = new BufferedWriter(writer);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				int vid = Integer.parseInt(terms[5]);
				counts[vid]++;
			}
			br.close();
			reader.close();
			reader = new FileReader(souFile);
			br = new BufferedReader(reader);
			str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				int vid = Integer.parseInt(terms[5]);
				if (counts[vid] < this.m)
					continue;
				bw.write(str + "\n");
			}
			br.close();
			reader.close();
			bw.close();
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void checkUser(String souFile, String tarFile) {
		FileReader reader;
		FileWriter writer;
		int[] counts = new int[this.U];
		try {
			reader = new FileReader(souFile);
			BufferedReader br = new BufferedReader(reader);
			writer = new FileWriter(tarFile);
			BufferedWriter bw = new BufferedWriter(writer);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				int uid = Integer.parseInt(terms[0]);
				counts[uid]++;
			}
			br.close();
			reader.close();
			reader = new FileReader(souFile);
			br = new BufferedReader(reader);
			str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				int uid = Integer.parseInt(terms[0]);
				if (counts[uid] < this.n)
					continue;
				bw.write(str + "\n");
			}
			br.close();
			reader.close();
			bw.close();
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * tarFile1 is used to store the uid transformation and tarFile2 is used to
	 * store the vid transformation
	 */
	public void transID(String souFile, String tarFile, String tarFile1, String tarFile2, String tarFile3) {
		FileReader reader;
		FileWriter writer;
		Hashtable<Integer, Integer> uids = new Hashtable<Integer, Integer>();// pair:
																				// oldUid,
																				// newUid
		Hashtable<Integer, Integer> vids = new Hashtable<Integer, Integer>();
		Hashtable<String, Integer> wids = new Hashtable<String, Integer>();
		int countUid = -1;
		int countVid = -1;
		int countWid=-1;
		try {
			reader = new FileReader(souFile);
			BufferedReader br = new BufferedReader(reader);
			writer = new FileWriter(tarFile);
			BufferedWriter bw = new BufferedWriter(writer);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				int ouid = Integer.parseInt(terms[0]);
				int ovid = Integer.parseInt(terms[5]);
				String wordss=terms[6];
				String[] words=wordss.split("\\|");
				StringBuffer sb=new StringBuffer("");
				int nwid=-1;
				int nuid = -1;
				int nvid = -1;
				if (uids.containsKey(ouid)) {
					nuid = uids.get(ouid);
				} else {
					countUid++;
					nuid = countUid;
					uids.put(ouid, nuid);
				}
				if (vids.containsKey(ovid)) {
					nvid = vids.get(ovid);
				} else {
					countVid++;
					nvid = countVid;
					vids.put(ovid, nvid);
				}
				for(String word:words){
					if(wids.containsKey(word)){
						nwid=wids.get(word);
					}
					else{
						countWid++;
						nwid=countWid;
						wids.put(word, nwid);
					}
					sb.append(nwid+"|");
				}
				sb.deleteCharAt(sb.length()-1);
				bw.write(nuid + "\t" + terms[1] + "\t" + terms[2] + "\t" + terms[3] + "\t" + terms[4] + "\t" + nvid
						+ "\t" + sb.toString() + "\t" + terms[7] + "\n");
			}
			br.close();
			reader.close();
			bw.close();
			writer.close();
			writer=new FileWriter(tarFile2);
			bw=new BufferedWriter(writer);
			Iterator<Integer> it=uids.keySet().iterator();
			while(it.hasNext()){
				int oid=it.next();
				int nid=uids.get(oid);
				bw.write(oid+"\t"+nid+"\n");
			}
			bw.close();
			writer.close();
			writer=new FileWriter(tarFile1);
			bw=new BufferedWriter(writer);
			Iterator<Integer> it1=vids.keySet().iterator();
			while(it1.hasNext()){
				int oid=it1.next();
				int nid=vids.get(oid);
				bw.write(oid+"\t"+nid+"\n");
			}
			bw.close();
			writer.close();
			writer=new FileWriter(tarFile3);
			bw=new BufferedWriter(writer);
			Iterator<String> it2=wids.keySet().iterator();
			while(it2.hasNext()){
				String word=it2.next();
				int nid=wids.get(word);
				bw.write(word+"\t"+nid+"\n");
			}
			bw.close();
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
