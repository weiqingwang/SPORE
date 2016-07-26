package efficiency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import exception.length_check_exception;
import model.Paras;

public class Data_reader_writer {
	public static BufferedReader file_handle_read(String name)

	{

		try {

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(name)));

			return br;

		}

		catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	// create a streamwriter

	public static OutputStreamWriter file_handle(String name)

	{

		try {

			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(new File(name)));

			return osw;

		}

		catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}
	
	// create a streamwriter

		public static OutputStreamWriter file_handle_append(String name)

		{

			try {

				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(new File(name),true));

				return osw;

			}

			catch (Exception e) {

				e.printStackTrace();

			}

			return null;

		}
	public static double[][] load_matrix(String file, int len1, int len2)

	{

		try{

			String line;

			String part[];
			
			BufferedReader br=file_handle_read(file);

			double matrix[][]=new double[len1][len2];

			for(int i=0;i<len1;i++){

				line=br.readLine();

				part=line.split(",");

				if(part.length!=len2){

					System.out.println(part.length);

					throw new length_check_exception();

				}

				for(int j=0;j<len2;j++){

					matrix[i][j]=Double.parseDouble(part[j]);

				}

			}

//			System.out.println("Load matrix complete!");

			return matrix;

		}

		catch(length_check_exception l){

			l.printStackTrace();

		}

		catch(Exception e){

			e.printStackTrace();

		}

		return null;

	}
	
	public static int[][] load_matrix_int(String file, int len1, int len2)

	{

		try{

			String line;

			String part[];
			
			BufferedReader br=file_handle_read(file);

			int matrix[][]=new int[len1][len2];

			for(int i=0;i<len1;i++){

				line=br.readLine();

				part=line.split(",");

				if(part.length!=len2){

					System.out.println(part.length);

					throw new length_check_exception();

				}

				for(int j=0;j<len2;j++){

					matrix[i][j]=Integer.parseInt(part[j]);

				}

			}

//			System.out.println("Load matrix complete!");

			return matrix;

		}

		catch(length_check_exception l){

			l.printStackTrace();

		}

		catch(Exception e){

			e.printStackTrace();

		}

		return null;

	}
	
	
	public static void store_matrix(OutputStreamWriter osw, int matrix[][],int len1, int len2)

	{

		try{

			for(int i=0;i<len1;i++){

				for(int j=0;j<len2;j++){

					osw.write(matrix[i][j]+",");

				}

				osw.write("\n");

				osw.flush();

			}

			osw.close();

		}

		catch(Exception e){

			e.printStackTrace();

		}

	}



	public static double[] load_item(String file, int item)

	{

		try {

			double[] vector = new double[Paras.K];
			BufferedReader br=file_handle_read(file);

			for (int i = 0; i < Paras.countV; i++) {
				String line = br.readLine();
				if (i != item)
					continue;
				String[] terms = line.split(",");
				for (int j = 0; j < Paras.K; j++) {
					vector[j] = Double.parseDouble(terms[j]);
				}
			}
			
			br.close();

//			System.out.println("Load matrix complete!");

			return vector;

		}

		catch (Exception e) {

			e.printStackTrace();

		}

		return null;
	}

	public static int load_sortedlist(String file, int i, int j) {
		try {

			int result = -1;
			
			BufferedReader br=file_handle_read(file);

			for (int z = 0; z < Paras.K; z++) {
				String line = br.readLine();
				if (z == i) {
					String[] terms = line.split(",");
					result=Integer.parseInt(terms[j]);
					break;
				}
			}
			br.close();
//			System.out.println("Load matrix complete!");

			return result;

		}

		catch (Exception e) {

			e.printStackTrace();

		}

		return -1;
	}

}
