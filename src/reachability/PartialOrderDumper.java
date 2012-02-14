package reachability;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PartialOrderDumper {
	private static PartialOrderDumper pod = null;
	
	private static final Object classLock = PartialOrderDumper.class;
	
	private final File f = new File("PartialOrder.txt");
	private FileWriter fstream;
	private BufferedWriter out;
	private StringBuffer sb;
	
	private boolean runBegins = false;
	private int curRun = 0;
	
	
	PartialOrderDumper() {
		try {
			fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void beginRun(int runId) {
		if (runBegins) System.err.println("Error: Partial Order Dumper");
		else {
			sb = new StringBuffer();
			runBegins = true;
			curRun = runId;
			sb.append("BeginRun " + curRun + "\n");
		}
	}
	
	public void endRun() {
		if (!runBegins) System.err.println("Error: Partial Order Dumper");
		try {
			runBegins = false;
			sb.append("EndRun " + curRun + "\n");
			out.write(sb.toString());
			out.flush();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public void append(String s) {
		sb.append(s);
	}

	public static PartialOrderDumper getInstance() {
		if (pod == null) {
			synchronized(classLock) {
				if (pod == null) {
					pod = new PartialOrderDumper();
				}
			}
		}
		return pod;
	}
}
