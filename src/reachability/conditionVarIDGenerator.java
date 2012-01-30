package reachability;
import java.util.*;
import java.io.*;

class conditionVarIDGenerator implements propertyParameters {
	private HashMap names = new HashMap();


	private static final Object classLock = conditionVarIDGenerator.class;

	private static conditionVarIDGenerator instance = null;

	private PrintWriter outputThreadID = null;
	private boolean firstRT = true; // true if first execution during RT
	private propertyParameters.Mode traceOrReplay = NONE;

	private conditionVarIDGenerator() {
		traceOrReplay = (propertyReader.getInstance().getModeProperty());
		try {
	   	outputThreadID = new PrintWriter(new FileOutputStream("conditionVarID.txt"));
		}
      catch (IOException e) {
       System.err.println("File not opened: " + e.toString());
        System.exit(1);
     }
	}


	public static conditionVarIDGenerator getInstance() { 
		synchronized(classLock) {
      	if (instance == null)
        		instance = new conditionVarIDGenerator();
		}
      return instance;
    }

   public synchronized void resetIDs() {
     	firstRT = false;
		names.clear();
   }

	private synchronized boolean containsName(String stringID) {
		return names.containsKey(stringID);
	}

	private synchronized void putName(String name, Integer num) {
	// associates name with num
		names.put(name, num);
		if (!(traceOrReplay==NONE)) { 
			if (traceOrReplay==RT) {
				if (firstRT) {
				   outputThreadID.println(name+num);
					outputThreadID.flush();
				}
			}
			else {
			   outputThreadID.println(name+num);
				outputThreadID.flush();		
			}
		}
	}

	private synchronized void putUserName(String name, Integer num) {
	// associates name with num
		names.put(name, num);
		if (!(traceOrReplay==NONE)) { 
			if (traceOrReplay==RT)  {
				if (firstRT) {
		   		outputThreadID.println(name);
					outputThreadID.flush();
				}
			}
			else {
	   		outputThreadID.println(name);
				outputThreadID.flush();			
			}
		}
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
				throw new InvalidConditionVarName("Use of duplicate thread name: " + "\""+userName+"\"");
			}
     	 return userName;
		}

  }

final class InvalidConditionVarName extends InvalidIDException {
	InvalidConditionVarName() { }
	InvalidConditionVarName(String msg) {super(msg);}
}
