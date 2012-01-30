package reachability;
import java.io.*;
public final class traceit {
	private static traceit instance = null;
	private static final Object classLock = traceit.class;
	private static PrintWriter outFile = null;
	public static traceit getInstance() { 
		synchronized(classLock) {
  			if (instance == null) {
     			instance = new traceit();
  			}
		}
  		return instance;
  	}
  	public static synchronized void open() {
  	   try {
	  		outFile = new PrintWriter(new FileOutputStream("tracedMessages.txt"));
	  	}
	  	catch (IOException e) {
	     		System.err.println("Error while opening trace file: " + e.toString());
   	  		System.exit(1); 
		}
  	}
  	public static synchronized void close() {
  		outFile.close();
  	}
  	
  	public static synchronized void trace(String s) {
  		outFile.println(s);outFile.flush();
  	}
}
