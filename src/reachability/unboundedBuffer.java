package reachability;
import java.util.LinkedList;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.File;

final class unboundedBuffer implements Buffer {
    public static final String VARIANT_DIR = "vars";
    public static final String FNAME_PREFIX = "Group";
    public static final String FNAME_SUFFIX = ".dat";
    private String name;
    private LinkedList buffer = null;
    private int in = 0, out = 0;
    private boolean generatorThreadWaiting = false;

    public unboundedBuffer(String name) {
	this.name = name;
	buffer = new LinkedList ();
    }

    // set the points in the restart mode
    public void init (boolean restart) {
    	// create the subdir for the buffer
	if (restart) {
	    File subdir = new File (VARIANT_DIR);
	    if (subdir.exists()) {
		if (subdir.isDirectory()) {
		    File[] files = subdir.listFiles();
		    if (files.length > 0) {
			for (int i = 0; i < files.length; i ++) {
			    try {
				FileInputStream fis = new FileInputStream (files[i]);
				ObjectInputStream ois = new ObjectInputStream (fis);
				Object value = ois.readObject ();
				ois.close ();
				// deposit to the buffer
				deposit (value);
				files[i].delete ();
			    }
			    catch (Exception ex) {
				ex.printStackTrace (System.out);
				System.exit(1);
			    }
			}
		    }
		    else {
			System.out.println("Restart directory is empty.");
			System.exit (1);
		    }
		}
		else {
		    System.out.println("No restart directory.");
		    System.exit (1);
		}
	    }
	    else {
		System.out.println("No restart directory.");
		System.exit (1);
	    }
	}
    }

    public synchronized int size () {
	return buffer.size ();
    }

    public synchronized boolean consumerIsWaiting () {
		 return generatorThreadWaiting;
    }
    
    public synchronized Object withdraw () {
	Object rval = null;
	if (buffer.size() == 0)
	    try { generatorThreadWaiting = true; wait(); generatorThreadWaiting=false; } catch (InterruptedException ex) {}
	rval = buffer.removeLast ();

	return rval;
    }
    
    public synchronized Object withdrawN() {
	// other clients/server that ask for more groups get 1/2 of the groups
		ArrayList groups = new ArrayList();
		//if (buffer.size() > 10) {
	  		int partOfGroups = buffer.size() / 2;
	  		//System.out.println("*******withdrawN: returning: " + partOfGroups);
	  		//if (partOfGroups > 30) partOfGroups = 30;
	  		// Objects that are too large result in Broken Pipes?
	  		for (int i=0; i<partOfGroups; i++) {
		  		Object rval = buffer.removeLast();
		  		groups.add(rval);
			}
		//}
		return groups;
    } 

    public synchronized void deposit (Object value) {
	buffer.addLast (value);
	if (buffer.size() == 1)
	    notify ();
    }

    // write objects into files
    public synchronized void serialize () {
    	// create the subdir for the buffer
	File subdir = new File (VARIANT_DIR);
	if (subdir.exists()) {
	    if (subdir.isDirectory()) {
		File[] files = subdir.listFiles();
		// remove all the files in the directory
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

	// write all the objects into files
	for (int i = 0; i < buffer.size (); i ++) {
	    try {
		Object value = buffer.get (i);
		String fname = FNAME_PREFIX + i + FNAME_SUFFIX;
		File file = new File (subdir, fname);
		FileOutputStream fos = new FileOutputStream (file);
		ObjectOutputStream oos = new ObjectOutputStream (fos);
		oos.writeObject (value);
		oos.close ();
	    }
	    catch (IOException ex) {
		ex.printStackTrace (System.out);
		System.exit(0);
	    }
	}
    }
}
