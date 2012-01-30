package reachability;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.File;

public class DiskBuffer implements Buffer {
    public static final String VARIANT_DIR = "vars";
    public static final String FNAME_PREFIX = "Group";
    public static final String FNAME_SUFFIX = ".dat";
    public static final int CAPACITY = 2000000;
    private int head;
    private int tail;
    private String name;
    private File subdir;
    private boolean generatorThreadWaiting = false;

    public DiskBuffer (String name) {
	this.name = name;
	head = tail = 0;
	
    }

    // set the points in the restart mode
    public void init (boolean restart) {
    	// create the subdir for the buffer
	subdir = new File (VARIANT_DIR);
	if (subdir.exists()) {
	    if (subdir.isDirectory()) {
		File[] files = subdir.listFiles();
		if (!restart) {
		    // in case of a fresh start, we remove all the
		    // files in the directory
		    for (int i = 0; i < files.length; i ++) {
			files[i].delete();
		    }
		}
		else if (files.length > 0) {
		    // restart moded
		    int min = -1;
		    int max = -1;
		    for (int i = 0; i < files.length; i ++) {
			String fn = files[i].getName ();
			if (fn.startsWith(FNAME_PREFIX)) {
			    int dot = fn.indexOf(FNAME_SUFFIX);
			    String index_str = fn.substring (FNAME_PREFIX.length(), dot);
			    int index = Integer.parseInt (index_str);
			    if ((min == -1) || (index < min))  {
				min = index;
			    }
			    if ((max == -1) || (index > max)) {
				max = index;
			    }
			}
		    }

		    // set the head and tail pointers
		    head = min;
		    tail = max + 1;
		    System.out.println("Restart point: Head " + head + " Tail " + tail);
		}
		else {
		    System.out.println("Disk buffer is empty. Please use the FRESHSTART mode.");
		    System.exit(1);
		}

	    }
	    else {
		subdir.delete ();
		subdir.mkdir ();
	    }
	}
	else {
	    subdir.mkdir ();
	}
    }

    public int size () {
	return tail - head;
    }
    
     public synchronized boolean consumerIsWaiting () {
		 return generatorThreadWaiting;
    }
    
    public synchronized Object withdrawN() {
        return new Object();
    }
   
    
    public synchronized Object withdraw () {
	Object rval = null;
	if (size () == 0) {
	    try { generatorThreadWaiting = true; wait (); generatorThreadWaiting = false; } catch (InterruptedException ex) {}
	}
	try {
	    String fname = FNAME_PREFIX + head + FNAME_SUFFIX;
	    head ++;

	    File file = new File (subdir, fname);
	    FileInputStream fis = new FileInputStream (file);
	    ObjectInputStream ois = new ObjectInputStream (fis);
	    rval = ois.readObject ();
	    ois.close ();
	    if(!file.delete ()) {
		System.out.println("CANNT delete file: " + fname);
	    }
	}
	catch (Exception ex) {
	    ex.printStackTrace (System.out);
	    System.exit(0);
	}

	return rval;
    }

    public synchronized void deposit (Object value) {
	try {
	    String fname = FNAME_PREFIX + tail + ".dat";
	    tail ++;

	    File file = new File (subdir, fname);
	    FileOutputStream fos = new FileOutputStream (file);
	    ObjectOutputStream oos = new ObjectOutputStream (fos);
	    oos.writeObject (value);
	    oos.close ();

	    if (size () == 1) {
		notify ();
	    }
	    
	    if (size () > CAPACITY) {
		System.out.println("Disk Buffer exceeds CAPACITY");
		System.exit(0);
	    }
	}
	catch (IOException ex) {
	    ex.printStackTrace (System.out);
	    System.exit(0);
	}
    }

    private void makeSubdir () {
    	// create the subdir for the buffer
	subdir = new File (VARIANT_DIR);
	if (subdir.exists()) {
	    if (subdir.isDirectory()) {
		File[] files = subdir.listFiles();
		for (int i = 0; i < files.length; i ++) {
		    files[i].delete();
		}
	    }
	    else {
		subdir.delete ();
		subdir.mkdir ();
	    }
	}
	else {
	    subdir.mkdir ();
	}

    }
}
