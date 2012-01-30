package reachability;
import java.util.*;
import java.io.*;

class ThreadBasedSequenceCollection implements eventTypeParameters {

	private static final Object classLock = ThreadBasedSequenceCollection.class;

	private static ThreadBasedSequenceCollection instance = null;

	private HashMap threadBasedSequences = null;

	private static boolean done = false; // "true" indicates that sequences were already output
	private int numberOfThreads=0;

	private ObjectInputStream inputThreadNameAndIDs = null; // input sequence of int IDs

	private ThreadBasedSequenceCollection() {
		threadBasedSequences = new HashMap();
	}

	public static ThreadBasedSequenceCollection getInstance() { 
		synchronized(classLock) {
      	if (instance == null)
        		instance = new ThreadBasedSequenceCollection();
		}
      return instance;
    }

	public synchronized void updateSequence(srThreadEvent e, Integer key) {
		TreeSet sequence;
		if (threadBasedSequences.containsKey(key)) 
			sequence = (TreeSet)threadBasedSequences.get(key);
		else
			sequence = new TreeSet();
		//System.out.println(key.intValue()+ ": event: " + e + " sequence before: "+sequence);
		boolean result = sequence.add(e);
		if (!result)
			System.out.println("*********************Add failed*********************************");
		//System.out.println(key.intValue()+": sequence right after add: "+sequence);


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
		//System.out.println("update threadsequence:" + stringVectorTS);

		//System.out.println(key.intValue()+" before: "+sequence);
		//System.out.println("Putting new event for key " + key.intValue());
		threadBasedSequences.put(key,sequence);
		//System.out.println(key.intValue()+" after: " + (TreeSet)threadBasedSequences.get(key));
	}

	public synchronized void changeToAccepted(srThreadEvent threadSequenceEvent, 
		int actualCalledThread, int actualCalledVersionNumber) {
		//System.out.println("thread event before: " + threadSequenceEvent);
		threadSequenceEvent.setEventType(ASYNCH_SEND);
		threadSequenceEvent.setOtherThread(actualCalledThread);
		threadSequenceEvent.setOtherThread(actualCalledVersionNumber);	
		//System.out.println("thread event after: " + threadSequenceEvent);
	}
	public synchronized void outputThreadBasedSequences() throws IOException {

		if (done) { // output only once, but called by each object controller
			return;
		}
		LinkedList threadNamesAndIDs = new LinkedList();

      try {
			inputThreadNameAndIDs = new ObjectInputStream(new FileInputStream("ThreadID.dat"));
      }
      catch (IOException e) {
        	System.out.println("ThreadID file not opened: " + e.toString());
			System.out.flush();
        	System.exit(1);
      }
      ThreadNameAndID nameAndID;
      try {
				//System.out.println("Reading ThreadIDs");
				while (true) {
					nameAndID = (ThreadNameAndID)inputThreadNameAndIDs.readObject();
					String name = nameAndID.getName();
					numberOfThreads = nameAndID.getID();
					threadNamesAndIDs.add(name);
         	}
      }
      catch (ClassNotFoundException e) {
        	System.out.println("Error while reading ThreadID file: " + e.toString());
			System.out.flush();
        	System.exit(1);
      }	
      catch (EOFException eof) {
			try {
				//System.out.println("number of Threads is " + numberOfThreads);
		  		inputThreadNameAndIDs.close();
			}
			catch (Exception e) {
				System.out.println("Error closing ThreadID file.");
				System.out.flush();
			}
      }
      catch (IOException e) {
        	System.out.println("Error while reading ThreadID file: " + e.toString());
			System.out.flush();
        	System.exit(1);
      }
		// if numberOfThreads is n then we need n+1 elements since IDs are 1..numberOfThreads.


		ObjectOutputStream outputThreadTest = null; // output sequence of int IDs
		PrintWriter outputThreadTestText = null;
		//ObjectOutputStream outputThreadReplay = null; // output sequence of int IDs
		//PrintWriter outputThreadReplayText = null;
		//System.out.println("Output thread-based sequences");
		try {
			Set s = threadBasedSequences.entrySet();
			Iterator i = s.iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry)(i.next());
				int threadID = ((Integer)e.getKey()).intValue();
				TreeSet sequence = (TreeSet)e.getValue();
				//System.out.println(threadID+": "+sequence);
				String threadName = (String) threadNamesAndIDs.get(threadID-1);
	  	    	//outputThreadTest = new ObjectOutputStream(new FileOutputStream(threadName+"-trace.dat"));
		   	outputThreadTestText = new PrintWriter(new FileOutputStream(threadName+"-trace.txt"));
  		    	//outputThreadReplay = new ObjectOutputStream(new FileOutputStream(threadName+"-replay.dat"));
	  		 	//outputThreadReplayText = new PrintWriter(new FileOutputStream(threadName+"-replay.txt"));
				Iterator it = sequence.iterator();
				while (it.hasNext()) {
					srThreadEvent threadEvent = (srThreadEvent) it.next();
					//outputThreadTest.writeObject(threadEvent);
					//outputThreadTest.flush();
					outputThreadTestText.println("("+threadEvent.getOtherThread()+","+threadEvent.getOtherThreadVersionNumber()+","+
						threadEvent.getThisThread()+","+threadEvent.getThisThreadVersionNumber()+","+threadEvent.getChannelName()+
						","+threadEvent.getChannelVersionNumber()+","+threadEvent.getEventType()+")  " + 
						outputVectorTimeStamp(threadEvent.getVectorTS()));
					outputThreadTestText.flush();
				}
				//outputThreadTest.close();
				outputThreadTestText.close();
			}
		} 	catch (IOException e) {
				System.out.println("IOExcpetion in outputThreadBasedSequences");
				System.out.flush();
				//if (outputThreadTest != null)
				//	outputThreadTest.close();
				if (outputThreadTestText != null)
					outputThreadTestText.close();
				//if	(outputThreadReplay != null)
				//	outputThreadReplay.close();
				//if (outputThreadReplayText != null)
				//	outputThreadReplayText.close();
				throw e;
  			}
		done = true;
		//System.out.println("outputThreadBased completed normally");
		//System.out.flush();
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
