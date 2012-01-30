package reachability;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.File;

public class Splitter {
    private int numOfHosts;
    private static Splitter splitter;
    private File[] dirs;

    private int numOfGroups;
    private int[] fileIndex;
    
    public static Splitter instance (int numOfHosts) {
	if (splitter == null) {
	    splitter = new Splitter (numOfHosts);
	}
	return splitter;
    }

    private Splitter (int numOfHosts) {
	this.numOfHosts = numOfHosts;
	dirs = new File [numOfHosts];
	numOfGroups = 0;
	fileIndex = new int [numOfHosts];

	// initialize file index
	for (int i = 0; i < numOfHosts; i ++) {
	    fileIndex[i] = 0;
	}

	// create sub directories
	for (int i = 0; i < numOfHosts; i ++) {
	    dirs[i] = new File (DiskBuffer.VARIANT_DIR + i);
	    if (dirs[i].exists()) {
		File [] files = dirs[i].listFiles ();
		if (dirs[i].isDirectory()) {
		    for (int j = 0; j < files.length; j ++) {
			files[j].delete();
		    }
		}
		else {
		    dirs[i].delete ();
		    dirs[i].mkdir ();
		}
	    }
	    else {
		dirs[i].mkdir ();
	    }
	}
	
    }

    public void write (VariantGroup group) {
	try {
	    int dirIndex = numOfGroups % numOfHosts;
	    numOfGroups ++;

	    String fname = DiskBuffer.FNAME_PREFIX + fileIndex[dirIndex] + ".dat";
	    fileIndex[dirIndex] ++;

	    File file = new File (dirs[dirIndex], fname);
	    FileOutputStream fos = new FileOutputStream (file);
	    ObjectOutputStream oos = new ObjectOutputStream (fos);
	    oos.writeObject (group);
	    oos.close ();
	}
	catch (IOException ex) {
	    ex.printStackTrace (System.out);
	    System.exit (1);
	}
    }
}
