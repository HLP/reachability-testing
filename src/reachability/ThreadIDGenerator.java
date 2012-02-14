package reachability;
import java.util.*;
import java.io.*;

class ThreadNameAndID implements Serializable {
	String name;
	int ID;
	ThreadNameAndID(String name, int ID) {
		this.name = name;
		this.ID = ID;
	}
	public int getID() { return ID;}
	public String getName() {return name;}
}

class ThreadIDGenerator implements propertyParameters {
	private HashMap names = new HashMap();
	private HashMap IDs = new HashMap();
	// when replay or test, use IDs generated during trace (not new IDs).
	private HashMap replayIDsByName = new HashMap(); // keyed on Name
	private HashMap replayIDsByID = new HashMap(); // keyed on ID
	private boolean firstRT = true; // true if first execution during RT

	private propertyParameters.Mode traceOrReplay = NONE;

	private static final Object classLock = ThreadIDGenerator.class;

	private static ThreadIDGenerator instance = null;

	private PrintWriter outputThreadIDsText = null;
	private ObjectOutputStream outputThreadIDsObject = null;

	private int numberOfThreads = 0; // number of threads in ThreadID file: 1..numberOfThreads
	private int saveNumberOfThreads = 0;
	private ObjectInputStream inputThreadNameAndIDs = null; // input sequence of int IDs
	private int maxNumberOfThreads=0;


	private ThreadIDGenerator() {
		traceOrReplay = (propertyReader.getInstance().getModeProperty());
		maxNumberOfThreads = (propertyReader.getInstance().getMaxThreadsProperty());
		if (traceOrReplay == TRACE || traceOrReplay == RT) {
			try {
				outputThreadIDsText = new PrintWriter(new FileOutputStream("ThreadID.txt"));
				outputThreadIDsObject = new ObjectOutputStream(new FileOutputStream("ThreadID.dat"));
			}
			catch (IOException e) {
				System.err.println("File not opened: " + e.toString());
				System.exit(1);
			}
			if (firstRT) {
				outputThreadIDsText.println("(*This file is read-only; the Thread IDs cannot be changed.*)");
				outputThreadIDsText.flush();
			}
		}
		else if (traceOrReplay == REPLAY || traceOrReplay == TEST || traceOrReplay == SPECTEST) {
			try {
				inputThreadNameAndIDs = new ObjectInputStream(new FileInputStream("ThreadID.dat"));
			}
			catch (IOException e) { // won't be there for first pass of RT
				//if (traceOrReplay == REPLAY || traceOrReplay == TEST) {
				System.out.println("ThreadID file not opened: " + e.toString());
				System.exit(1);
				//}
			}
			ThreadNameAndID nameAndID;
			try {
				//System.out.println("Reading ThreadIDs");
				while (true) {
					nameAndID = (ThreadNameAndID)inputThreadNameAndIDs.readObject();
					numberOfThreads = nameAndID.getID(); // last value of numberOfThreads will be used
					replayIDsByName.put(nameAndID.getName(),new Integer(numberOfThreads));
					replayIDsByID.put(new Integer(numberOfThreads),nameAndID.getName());
				}
			}
			catch (ClassNotFoundException e) {
				System.out.println("Error while reading ThreadID file: " + e.toString());
				System.exit(1);
			}	
			catch (EOFException eof) {
				try {
					//System.out.println("number of Threads is " + numberOfThreads);
					inputThreadNameAndIDs.close();
				}
				catch (Exception e) {
					System.out.println("Error closing ThreadID file.");
				}
			}
			catch (IOException e) {
				System.out.println("Error while reading ThreadID file: " + e.toString());
				System.exit(1);
			}
			// if numberOfThreads is n then we need n+1 elements since IDs are 1..numberOfThreads.
		}   
	}

	public static ThreadIDGenerator getInstance() { 
		synchronized(classLock) {
			if (instance == null)
				instance = new ThreadIDGenerator();
		}
		return instance;
	}
	public synchronized void resetIDs() {
		saveNumberOfThreads = numberOfThreads;
		numberOfThreads = 0;
		firstRT = false;
		names.clear();
		IDs.clear();
		replayIDsByName.clear();
		replayIDsByID.clear();
	}

	private synchronized int getNextID() {return ++numberOfThreads;}

	private synchronized boolean containsName(String stringID) {
		return names.containsKey(stringID);
	}

	private synchronized void putName(String name, Integer num) {
		// associates name with num
		names.put(name, num);
	}

	private synchronized int getNum(String name) {
		// gets num associated with name
		return ((Integer)names.get(name)).intValue();
	}

	private synchronized int generateID(String name, boolean isThread) {

		// associates name with num
		int threadID=0;
		if (traceOrReplay == TRACE || traceOrReplay == RT) { 
			threadID = getNextID();
			IDs.put(new Integer(threadID), name);
			if (firstRT) {
				outputThreadIDsText.println(name + " " + threadID);
				outputThreadIDsText.flush();
				ThreadNameAndID nameAndID = new ThreadNameAndID(name,threadID);
				try {
					outputThreadIDsObject.writeObject(nameAndID);
					outputThreadIDsObject.flush();
				} 	catch (IOException e) {
					System.out.println("Error writing ThreadID file");
					System.exit(1);
				}
			}
		}
		else if (traceOrReplay == REPLAY || traceOrReplay == TEST || traceOrReplay == SPECTEST) {
			threadID = ((Integer)replayIDsByName.get(name)).intValue();
		}
		if (isThread && threadID > maxNumberOfThreads) {
			System.out.println("Error: Too many threads/synchronization objects. The default maximum is 15.  ");
			System.out.println("       Use -DmaxThreads=n to raise the limit. (Or try to define all threads ");
			System.out.println("       before defining any synchronization objects (semaphores, monitors, etc)).");
			System.exit(1);
		}
		return threadID;
	}

	public synchronized String getName(int ID) {
		if (traceOrReplay == TRACE || traceOrReplay == RT)
			return (String)(IDs.get(new Integer(ID)));
		else
			return (String)(replayIDsByID.get(new Integer(ID)));		
	}

	public synchronized int getID(String stringID,boolean isThread) {
		// check to see if this is first instance of this thread class
		// id has two tables:
		//	1. instance table of (className, #instances)
		// 2. identifier table of (className+instance#,ID)
		// isThread is true if it is a thread calling to get an ID. Synch objects
		// also call to get ID during RT (for use as index into vector timestamps)
		String nextName=null;
		if (!containsName(stringID)){
			// first thread of this class
			nextName = stringID + "1"; // next className+instance# for identifier table
			putName(stringID, new Integer(1)); // add to instance table
		}
		else {
			// at least one instance already exists
			int nextNum = getNum(stringID)+1;
			nextName = stringID + nextNum; // next className+instance# for identifier table
			putName(stringID,new Integer(nextNum)); // add to instance table
		}
		return generateID(nextName,isThread);
	}

	public synchronized int getIDFromUserName(String userName,boolean isThread) {
		// isThread is true if it is a thread calling to get an ID. Synch objects
		// also call to get ID during RT (for use as index into vector timestamps)
		if (!containsName(userName)){
			// first thread of this class
			putName(userName, new Integer(1)); // add to instance table
		}
		else {
			// at least one instance already exists
			throw new InvalidThreadName("Use of duplicate thread name: " + "\""+userName+"\"");
		}
		return generateID(userName,isThread);
	}

	// if tracing, it's number of IDs that have been created; if replay or test, it's read from ThreadID file.
	public synchronized int getNumberOfThreads() {
		if( traceOrReplay == RT) 
			return saveNumberOfThreads;
		else
			return numberOfThreads;
	}

	public synchronized void printNames() {
		System.out.println("");
		System.out.println("names table is");
		System.out.println(names);
	}

	public synchronized void printIDs() {
		System.out.println("");
		System.out.println("IDs table is");
		System.out.println(IDs);
		System.out.println("");
	}


}

final class InvalidThreadName extends InvalidIDException {
	InvalidThreadName() { }
	InvalidThreadName(String msg) {super(msg);}
}
