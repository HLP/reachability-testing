package reachability;
import java.util.*;
import java.io.*;

class monitorIDGenerator {
	private HashMap names = new HashMap();


	private static final Object classLock = monitorIDGenerator.class;

	private static monitorIDGenerator instance = null;

	private PrintWriter outputThreadID = null;

	private monitorIDGenerator() {
		try {
	   	outputThreadID = new PrintWriter(new FileOutputStream("monitorID.txt"));
		}
      catch (IOException e) {
       System.err.println("File not opened: " + e.toString());
        System.exit(1);
     }
	}


	public static monitorIDGenerator getInstance() { 
		synchronized(classLock) {
      	if (instance == null)
        		instance = new monitorIDGenerator();
		}
      return instance;
    }


	private synchronized boolean containsName(String stringID) {
		return names.containsKey(stringID);
	}

	private synchronized void putName(String name, Integer num) {
	// associates name with num
		names.put(name, num);
	   outputThreadID.println(name+num);
		outputThreadID.flush();
	}

	private synchronized void putUserName(String name, Integer num) {
	// associates name with num
		names.put(name, num);
	   outputThreadID.println(name);
		outputThreadID.flush();
	}

	private synchronized int getNum(String name) {
	// gets num associated with name
		return ((Integer)names.get(name)).intValue();
	}

		public synchronized String getID(String stringID) {
			String nextName=null;
			// check to see if this is first instance of this thread class
			// id has two tables:
			//	1. instance table of (className, #instances)
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
			return nextName;
		}

		public synchronized String getIDFromUserName(String userName) {
			if (!containsName(userName)){
					// first thread of this class
					putUserName(userName, new Integer(1)); // add to instance table
				}
			else {
				// at least one instance already exists
				throw new InvalidMonitorName("Use of duplicate thread name: " + "\""+userName+"\"");
			}
     	 return userName;
		}

  }

final class InvalidMonitorName extends InvalidIDException {
	InvalidMonitorName() { }
	InvalidMonitorName(String msg) {super(msg);}
}
