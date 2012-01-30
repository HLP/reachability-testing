package reachability;
import java.util.*;
import java.io.*;

class ChannelNameAndID implements Serializable {
	String name;
	int ID;
	ChannelNameAndID(String name, int ID) {
		this.name = name;
		this.ID = ID;
	}
	public int getID() { return ID;}
	public String getName() {return name;}
}

class channelIDGenerator implements propertyParameters {
	private HashMap names = new HashMap();
	private HashMap IDs = new HashMap();
	// when replay or test, use IDs generated during trace (not new IDs).
	private HashMap replayIDsByName = new HashMap(); 
	private int nextID = 0;
	private boolean firstRT = true; // true if first execution of RT; 
											  // set to false when reset is called
	private propertyParameters.Mode mode = NONE;

	private static final Object classLock = channelIDGenerator.class;

	private static channelIDGenerator instance = null;

	private PrintWriter outputChannelNamesText = null;
	private ObjectOutputStream outputChannelNamesObject = null;

	private int numberOfChannels = 0; // number of channels
	private ObjectInputStream inputChannelNameAndIDs = null; // input sequence of int IDs

	private channelIDGenerator() {
		mode = (propertyReader.getInstance().getModeProperty());
		if (mode == TRACE) {
			// In RT mode, files are not opened until first ID is generated
			try {
	   		outputChannelNamesText = new PrintWriter(new FileOutputStream("ChannelID.txt"));
	   		outputChannelNamesObject = new ObjectOutputStream(new FileOutputStream("ChannelID.dat"));
			}
      	catch (IOException e) {
       		System.err.println("File not opened: " + e.toString());
        		System.exit(1);
     		}
		}
		else if (mode == REPLAY || mode == TEST) {
			try {
				inputChannelNameAndIDs = new ObjectInputStream(new FileInputStream("ChannelID.dat"));
  	  	  	}
			catch (IOException e) {
				System.err.println("ChannelID file not opened: " + e.toString());
				System.exit(1);
      	}
      	ChannelNameAndID nameAndID;
      	try {
				while (true) {
					nameAndID = (ChannelNameAndID)inputChannelNameAndIDs.readObject();
          		numberOfChannels = nameAndID.getID(); // last value of numberOfChannels will be used
					replayIDsByName.put(nameAndID.getName(),new Integer(numberOfChannels));
         	}
      	}
      	catch (ClassNotFoundException e) {
        		System.err.println("Error while reading ChannelID file: " + e.toString());
        		System.exit(1);
      	}	
      	catch (EOFException eof) {
				try {
		  			inputChannelNameAndIDs.close();
				}
				catch (Exception e) {
					System.err.println("Error closing ChannelID file.");
				}
      	}
      	catch (IOException e) {
        		System.err.println("Error while reading ChannelID file: " + e.toString());
        		System.exit(1);
      	}
		}		
	
	}


	public static channelIDGenerator getInstance() { 
		if (instance == null) {
			synchronized(classLock) {
      		if (instance == null)
        			instance = new channelIDGenerator();
			}
		}
      return instance;
    }

   public synchronized void resetIDs() {
   	nextID = 0;
   	firstRT=false;
		names.clear();
		IDs.clear();
   }
   
	private synchronized int getNextID() {return ++nextID;}

	private synchronized boolean containsName(String stringID) {
		return names.containsKey(stringID);
	}

	private synchronized void putName(String name, Integer num) {
	// associates name with num
			names.put(name, num);
	}


	private synchronized int getNum(String name) {
	// gets ID associated with name in names
		return ((Integer)names.get(name)).intValue();
	}

	public synchronized int getChannelID(String name) {
	// gets ID associated with name in IDs (global ID)
		if (mode == TRACE || mode == RT)
			return ((Integer)IDs.get(name)).intValue();
		else
			return ((Integer)replayIDsByName.get(name)).intValue();		

	}

	private synchronized void generateID(String name) {
	// associates name with num
		int channelID = 0;
		if (mode == TRACE || mode == RT) { 
			if (mode == RT && firstRT && outputChannelNamesText==null && outputChannelNamesObject==null) {
			// In RT mode, files are not opened until first ID is generated
				try {
		   		outputChannelNamesText = new PrintWriter(new FileOutputStream("ChannelID.txt"));
	   			outputChannelNamesObject = new ObjectOutputStream(new FileOutputStream("ChannelID.dat"));
				}
   	   	catch (IOException e) {
      	 		System.err.println("File not opened: " + e.toString());
        			System.exit(1);
	     		}			
			}
			channelID = getNextID();
			IDs.put(name, new Integer(channelID));
			if (firstRT) {
		   	outputChannelNamesText.println(name + " " + channelID);
				outputChannelNamesText.flush();
			}
			if (firstRT) {
				ChannelNameAndID nameAndID = new ChannelNameAndID(name,channelID);
				try {
					outputChannelNamesObject.writeObject(nameAndID);
					outputChannelNamesObject.flush();
				} catch (IOException e) {
					System.out.println("Error writing ChannelID file");
					System.exit(1);
				  }
			}
		}
		// channelID is not returned so no need to get it here 
		// during replay/test. Used later to collect channel events 
		// of channel i in trace sequences
		//else if (mode == REPLAY || mode == TEST) {
		//	channelID = ((Integer)replayIDsByName.get(name)).intValue();
		//}
	}

	public synchronized String getID(String stringID) {
		String nextName=null;
		// check to see if this is first instance of this channel class			
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
		generateID(nextName); // ID used during trace to collect events of channel i
		return nextName;
	}

	public synchronized String getIDFromUserName(String userName) {
		if (!containsName(userName)){
				// first thread of this class
				putName(userName, new Integer(1)); // add to instance table
			}
		else {
			// at least one instance already exists
			throw new InvalidChannelName("Use of duplicate thread name: " + "\""+userName+"\"");
		}
		generateID(userName); // ID used during trace to collect events of channel i
      return userName+"1";
	}

  }

final class InvalidChannelName extends InvalidIDException {
	InvalidChannelName() { }
	InvalidChannelName(String msg) {super(msg);}
}
