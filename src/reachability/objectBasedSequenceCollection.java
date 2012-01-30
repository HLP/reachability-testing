package reachability;
import java.util.*;
import java.io.*;

class objectBasedSequenceCollection implements eventTypeParameters {

	private static final Object classLock = objectBasedSequenceCollection.class;

	private static objectBasedSequenceCollection instance = null;

	private HashMap objectBasedSequences = null;

	private static boolean done = false; // "true" indicates that sequences were already output
	private int numberOfThreads = 0;
	//private int numberOfChannels = 0;

	private ObjectInputStream inputChannelNamesAndIDs = null; // input sequence of String names

	private objectBasedSequenceCollection() {
		objectBasedSequences = new HashMap();
	}

	public static objectBasedSequenceCollection getInstance() { 
		synchronized(classLock) {
      	if (instance == null)
        		instance = new objectBasedSequenceCollection();
		}
      return instance;
    }

	public synchronized void updateSequence(srEvent e, Integer key) {
		TreeSet sequence;
		if (objectBasedSequences.containsKey(key)) 
			sequence = (TreeSet)objectBasedSequences.get(key);
		else
			sequence = new TreeSet();
		boolean result = sequence.add(e);
		if (!result)
			System.out.println("*********************Add failed*********************************");

		StringBuffer stringVectorTS = new StringBuffer("[");
		int clockValue;
		for (int i = 1; i<=9; i++) {
			clockValue = e.getVectorTS().getIntegerTS(i);
			if (clockValue == -1)
				stringVectorTS.append("0");
			else
				stringVectorTS.append(clockValue);
			stringVectorTS.append(",");
		}
		stringVectorTS.setCharAt(stringVectorTS.length()-1,']');
		objectBasedSequences.put(key,sequence);
	}

	public synchronized void changeToAccepted(srEvent objectSequenceEvent, 
			int actualCalledThread, int actualCalledVersionNumber) {

		objectSequenceEvent.setEventType(ASYNCH_SEND);
		objectSequenceEvent.setCalled(actualCalledThread);
		objectSequenceEvent.setCalled(actualCalledVersionNumber);

	}

	public synchronized void outputObjectBasedSequences() throws IOException {

		if (done) { // output only once, but called by each object controller
			return;
		}
		LinkedList channelNamesAndIDs = new LinkedList();

      try {
			inputChannelNamesAndIDs = new ObjectInputStream(new FileInputStream("ChannelID.dat"));
      }
      catch (IOException e) {
        	System.out.println("ChannelID file not opened: " + e.toString());
			System.out.flush();
        	System.exit(1);
      }
      ChannelNameAndID nameAndID;
      try {
				System.out.println("Reading channelNames");
				while (true) {
					nameAndID = (ChannelNameAndID)inputChannelNamesAndIDs.readObject();
					String name = nameAndID.getName();
					//numberOfChannels = nameAndID.getID();
					channelNamesAndIDs.add(name);
         	}
      }
      catch (ClassNotFoundException e) {
        	System.out.println("Error while reading ChannelID file: " + e.toString());
			System.out.flush();
        	System.exit(1);
      }	
      catch (EOFException eof) {
			try {
				//System.out.println("EOF");
		  		inputChannelNamesAndIDs.close();
			}
			catch (Exception e) {
				System.out.println("Error closing ChannelID file.");
				System.out.flush();
			}
      }
      catch (IOException e) {
        	System.out.println("Error while reading ChannelID file: " + e.toString());
			System.out.flush();
        	System.exit(1);
      }
		// if numberOfThreads is n then we need n+1 elements since IDs are 1..numberOfThreads.


		ObjectOutputStream outputChannelTest = null; // output sequence of int IDs
		PrintWriter outputChannelTestText = null;
		System.out.println("Output Channel-based sequences");
		try {
			numberOfThreads = ThreadIDGenerator.getInstance().getNumberOfThreads();
			Set s = objectBasedSequences.entrySet();
			Iterator i = s.iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry)(i.next());
				int channelID = ((Integer)e.getKey()).intValue();
				TreeSet sequence = (TreeSet)e.getValue();
				String channelName = (String) channelNamesAndIDs.get(channelID-1);
	  	    	//outputChannelTest = new ObjectOutputStream(new FileOutputStream(channelName+"-trace.dat"));
		   	outputChannelTestText = new PrintWriter(new FileOutputStream(channelName+"-trace.txt"));
 				Iterator it = sequence.iterator();
				while (it.hasNext()) {
					srEvent channelEvent = (srEvent) it.next();
					//outputChannelTest.writeObject(channelEvent);
					//outputChannelTest.flush();

					outputChannelTestText.println("("+channelEvent.getCaller()+","+channelEvent.getCalled()+
						","+channelEvent.getCallerVersionNumber()+","+channelEvent.getCalledVersionNumber()+
						","+channelEvent.getChannelName()+","+channelEvent.getEventType()+")  " + 
						outputVectorTimeStamp(channelEvent.getVectorTS()));	
					outputChannelTestText.flush();

				}
				//outputChannelTest.close();
				outputChannelTestText.close();
			}
		} 	catch (IOException e) {
				//if (outputChannelTest != null)
				//	outputChannelTest.close();
				if (outputChannelTestText != null)
					outputChannelTestText.close();
        		System.out.println("Error while writing channel-based sequences: " + e.toString());
				System.out.flush();
        		System.exit(1);

  			}
		done = true;
      System.out.println("Completed Writing Channel-Based Sequences.");
		System.out.flush();
	}

	private StringBuffer outputVectorTimeStamp(vectorTimeStamp vectorTS) {
	
		StringBuffer stringVectorTS = new StringBuffer("[");
		int clockValue;
		for (int i = 1; i<=numberOfThreads; i++) {
			clockValue = vectorTS.getIntegerTS(i);
			if (clockValue == -1)
				stringVectorTS.append("0");
			else
				stringVectorTS.append(clockValue);
			stringVectorTS.append(",");
		}
		stringVectorTS.setCharAt(stringVectorTS.length()-1,']');
		return stringVectorTS;
	}

}
