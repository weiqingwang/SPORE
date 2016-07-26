package utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import model.Paras;

/*
 * target files: checkinsRaw.txt
 * 
 * file format: userID£¬ twitterID£¬ lat£¬ lon, time, poiID, contentWords, state, hometown(CA), friends(only exists in Foursquare)
 * 
 * the content: sequences for each individual user (v1,t1, v2, t2)
 * 
 */
public class SequenceAbstractor {
	private int U = Paras.countU;// Foursquare
	private int V = Paras.countV;
	private ArrayList<Integer>[] sequentialVs;
	private ArrayList<Date>[] sequentialTs;
	private ArrayList<VDatePair>[] vDates;// record the poi and dates for each
											// user before ordering
	private String preAdd;
	private final static double deltaT = 0.1;
	private final static int numP = 3;

	public SequenceAbstractor() {
		preAdd = "data/" + Paras.dataset + "/";
		this.sequentialVs = new ArrayList[U];
		this.sequentialTs = new ArrayList[U];
		this.vDates = new ArrayList[U];
		for (int u = 0; u < U; u++) {
			this.sequentialVs[u] = new ArrayList<Integer>();
			this.sequentialTs[u] = new ArrayList<Date>();
			this.vDates[u] = new ArrayList<VDatePair>();
		}
	}

	public static void main(String[] args) {
		SequenceAbstractor sa = new SequenceAbstractor();
		// first read the vDates for each user
		sa.getVDates(sa.preAdd + "checkinsPre.txt");
		// for each user, order the vDates to get the sequentialVs and
		// sequentialTs
		sa.order();
		// output the result
		sa.outputP(sa.preAdd + "checkinsPre.txt", sa.preAdd + "checkins.txt");
	}

	/*
	 * this method read vDates for each user from the file
	 * 
	 * file format: userID£¬ twitterID£¬ lat£¬ lon, time, poiID, contentWords,
	 * state, hometown(CA), friends
	 */
	public void getVDates(String file) {
		FileReader reader;
		try {
			reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				int uid = Integer.parseInt(terms[0]);
				String dateS = terms[4];
				int vid = Integer.parseInt(terms[5]);
				SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				Date date = new Date(0);
				try {
					date = ft.parse(dateS);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.vDates[uid].add(new VDatePair(vid, date));
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

	/*
	 * this method order the vDates according to the date for each user
	 * 
	 * we need to notice that the twitter is sequential while foursquare is
	 * reverse
	 */
	public void order() {
		for (int u = 0; u < U; u++) {
			ArrayList<VDatePair> list = this.vDates[u];
			int size = list.size();
			for (int i = 0; i < size; i++) {
				VDatePair vd = list.get(i);
				if (Paras.dataset.equalsIgnoreCase("foursquare")) {
					vd = list.get(size - i - 1);
				}
				this.sequentialVs[u].add(vd.getVid());
				this.sequentialTs[u].add(vd.getDate());
			}
		}
	}

	/*
	 * this method get the predecessor set for each record and output the result
	 * to the tarFile userID£¬ twitterID£¬ lat£¬ lon, time, poiID,
	 * contentWords,state for each record, we first get its uid, vid, date;
	 * according to uid, we can get the sequentialT and sequentialV. then
	 * linearly scan sequentialT, if the intervals are larger than deltaT, we
	 * delete the previous V; if the currentT and current sequentialV are equal
	 * to the found vid,date, return the previous V
	 * 
	 * if the size of the found P is less than 3, we skip this record
	 */
	public void outputP(String souFile, String tarFile) {
		int uid, vid;
		Date date;
		ArrayList<Integer> seqvsu;
		ArrayList<Date> seqtsu;
		double inter;
		ArrayList<Integer> prevsu;
		FileReader reader;
		FileWriter writer;
		try {
			reader = new FileReader(souFile);
			BufferedReader br = new BufferedReader(reader);
			writer = new FileWriter(tarFile);
			BufferedWriter bw = new BufferedWriter(writer);
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split("\t");
				uid = Integer.parseInt(terms[0]);
				String dateS = terms[4];
				vid = Integer.parseInt(terms[5]);
				SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				date = new Date(0);
				try {
					date = ft.parse(dateS);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				seqvsu = this.sequentialVs[uid];
				seqtsu = this.sequentialTs[uid];
				prevsu = new ArrayList<Integer>();
				int s = seqtsu.size();
				// Date predate = new Date(0);
				// find the begin index of the predecessor set
				int index = 0;
				boolean found = false;
				for (; index < s; index++) {
					Date scandate = seqtsu.get(index);
					int scanvid = seqvsu.get(index);
					// get the interval between scandate and predate
					inter = this.getDayInter(scandate, date);
					if ((!scandate.equals(date)) && inter <= deltaT) {
						found = true;
						break;
					}
					if ((scandate.equals(date) && scanvid == vid)) {
						break;
					}
				}
				// three senarios:1.index=s; 2. the begin index of the
				// predecessor set is found; 3. find the target date
				if (index == s) {
					System.out.println("error");
					System.exit(1);
				}
				if (found) {
					for (; index < s; index++) {
						Date scandate = seqtsu.get(index);
						int scanvid = seqvsu.get(index);
						if ((scandate.equals(date) && scanvid == vid)) {
							break;
						}
						boolean exist = false;
						for (int id : prevsu) {
							if (id == scanvid)
								exist = true;
						}
						if (!exist)
							prevsu.add(scanvid);
					}
				}
				int size = prevsu.size();
				if (size == 0) {
					bw.write(str + "\t" + "-1" + "\n");
					continue;
				}
				StringBuffer psb = new StringBuffer();
				if (size > numP) {
					for (int i = size - numP; i < size; i++) {
						psb.append(prevsu.get(i) + ",");
					}
				} else {
					for (int i = 0; i <= size - 1; i++) {
						psb.append(prevsu.get(i) + ",");
					}
				}
				psb.deleteCharAt(psb.length() - 1);
				bw.write(str + "\t" + psb.toString() + "\n");
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
	 * this method returns the number of days between the two dates
	 */
	public double getDayInter(Date date1, Date date2) {
		double result = 0;
		long date1long = date1.getTime();
		long date2long = date2.getTime();
		double interval = Math.abs(date1long - date2long);
		result = interval / 1000 / 3600 / 24;
		return result;
	}

	// /*
	// * tarFile1 is used to store the uid transformation and tarFile2 is used
	// to
	// * store the vid transformation
	// */
	// public void transID(String souFile, String tarFile, String tarFile1,
	// String tarFile2) {
	// FileReader reader;
	// FileWriter writer;
	// Hashtable<Integer, Integer> uids = new Hashtable<Integer, Integer>();//
	// pair:
	// // oldUid,
	// // newUid
	// Hashtable<Integer, Integer> vids = new Hashtable<Integer, Integer>();
	// int countUid = -1;
	// int countVid = -1;
	// try {
	// reader = new FileReader(souFile);
	// BufferedReader br = new BufferedReader(reader);
	// writer = new FileWriter(tarFile);
	// BufferedWriter bw = new BufferedWriter(writer);
	// String str = null;
	// //transform the ids
	// while ((str = br.readLine()) != null) {
	// String[] terms = str.split("\t");
	// int ouid = Integer.parseInt(terms[0]);
	// int ovid = Integer.parseInt(terms[5]);
	// int nuid = -1;
	// int nvid = -1;
	// if (!uids.containsKey(ouid)) {
	// countUid++;
	// uids.put(ouid, nuid);
	// }
	// if (!vids.containsKey(ovid)) {
	// countVid++;
	// vids.put(ovid, nvid);
	// }
	// }
	// br.close();
	// reader.close();
	// reader = new FileReader(souFile);
	// br = new BufferedReader(reader);
	// str = null;
	// //write the new ids into the target file
	// while ((str = br.readLine()) != null) {
	// String[] terms = str.split("\t");
	// int ouid = Integer.parseInt(terms[0]);
	// int ovid = Integer.parseInt(terms[5]);
	// String ops=terms[8];
	// String[] terms1=ops.split(",");
	// int nuid = uids.get(ouid);
	// int nvid = vids.get(ovid);
	// StringBuffer npsb=new StringBuffer();
	// for(String term:terms1){
	// int ovidp=Integer.parseInt(term);
	// int nvidp=vids.get(ovidp);
	// npsb.append(nvidp+",");
	// }
	// npsb.deleteCharAt(npsb.length()-1);
	// bw.write(nuid + "\t" + terms[1] + "\t" + terms[2] + "\t" + terms[3] +
	// "\t" + terms[4] + "\t" + nvid
	// + "\t" + terms[6] + "\t" + terms[7]+ "\t" + npsb.toString() + "\n");
	// }
	// bw.close();
	// writer.close();
	// //write the transformations
	// writer=new FileWriter(tarFile2);
	// bw=new BufferedWriter(writer);
	// Iterator<Integer> it=uids.keySet().iterator();
	// while(it.hasNext()){
	// int oid=it.next();
	// int nid=uids.get(oid);
	// bw.write(oid+"\t"+nid+"\n");
	// }
	// bw.close();
	// writer.close();
	// writer=new FileWriter(tarFile1);
	// bw=new BufferedWriter(writer);
	// Iterator<Integer> it1=vids.keySet().iterator();
	// while(it1.hasNext()){
	// int oid=it1.next();
	// int nid=vids.get(oid);
	// bw.write(oid+"\t"+nid+"\n");
	// }
	// bw.close();
	// writer.close();
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
}
