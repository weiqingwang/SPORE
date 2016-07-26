package utility;

import java.io.*;

public class Test {
	public static void main(String[] args) {
	      try {
	         double d = 1847.4986;

	         // create a new RandomAccessFile with filename test
	         RandomAccessFile raf = new RandomAccessFile("d:/test.txt", "rw");

	         // write a double in the file
	         raf.writeDouble(d);

	         // set the file pointer at 0 position
	         raf.seek(0);

	         // read double
	         System.out.println("" + raf.readDouble());

	         // set the file pointer at 0 position
	         raf.seek(0);

	         // write a double at the start
	         raf.writeDouble(473.5645);
	         double[] b={1,2};
	         

	         // set the file pointer at 0 position
	         raf.seek(5);

	         // read double
	         System.out.println("" + raf.readDouble());
	      } catch (IOException ex) {
	         ex.printStackTrace();
	      }

	   }
}
