package utility;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import model.Paras;

public class Statistics {
	//this method return the count of words(result[0]), total number of activities(result[1]), and the total number of activities for each user
	public static int[] getStatistics(String file) {
		FileReader reader;
		int maxw = 0;
		int N=0;
		int[] results=new int[Paras.countU+2];
		try {
			// infer the id of the home place for each user
			reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			while ((str = br.readLine()) != null) {
				N++;
				String[] terms = str.split("\t");
				int uid=Integer.parseInt(terms[0]);
				results[uid+2]++;
				String contents = terms[6];
				StringTokenizer st=new StringTokenizer(contents,"|");
				while(st.hasMoreTokens()){
					int index=Integer.parseInt(st.nextToken());
					if(index>maxw)
						maxw=index;
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
		results[0]=maxw+1;
		results[1]=N;
		return results;
	}
}
