package reachability;
import java.io.*;
import java.util.*;

final class monitorTracingAndReplay extends monitorSU implements Control {
// Provides Trace and Replay for monitors. SMseq is a simple monitor
// sequence. Trace or Replay is selected by the user in the
// Monitor Toolbox

	private int index = 0;
   private Vector smSequence = new Vector();
   private Vector MSequence = new Vector();
   private Vector CommSequence = new Vector();
	private int numberOfThreads; // number of threads in ThreadID file: 1..numberOfThreads
	private conditionVariable[] threads;

	private DataOutputStream outputReplayTrace = null; // output sequence of int IDs
	private DataInputStream inputReplayTrace = null; // input sequence of int IDs

	private ObjectOutputStream outputTestTrace = null; // output sequence of int IDs
	private ObjectInputStream inputTestTrace = null; 
	
	private ObjectOutputStream outputSpecTestTrace = null; // output sequence of int IDs
	//private ObjectInputStream inputSpecTestTrace = null; 

	private PrintWriter outputReplayText = null;
	private BufferedReader inputReplayText = null;

	private PrintWriter outputTestText = null;
	private BufferedReader inputTestText = null;
	
	private PrintWriter outputSpecTestText = null;
	private BufferedReader inputSpecTestText = null;
	
	//private ObjectInputStream inputThreadNameAndIDs = null; // input sequence of int IDs

	//private propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	//private propertyParameters.Controllers numControllers = SINGLE;
	String traceFileName;

	public monitorTracingAndReplay(propertyParameters.Mode mode, 
			propertyParameters.Controllers numControllers, String traceFileName) {
		super(NONE);
		//this.mode = mode;
		//this.numControllers = numControllers;
		this.traceFileName = traceFileName;

    if (mode == TRACE) { // trace
      index = 0;
      try {
        //outputReplayTrace = new DataOutputStream(new FileOutputStream(traceFileName+"-replay.dat"));
	     outputReplayText = new PrintWriter(new FileOutputStream(traceFileName+"-replay.txt"));
        //outputTestTrace = new ObjectOutputStream(new FileOutputStream(traceFileName+"-test.dat"));
	     outputTestText = new PrintWriter(new FileOutputStream(traceFileName+"-test.txt"));
        //outputSpecTestTrace = new ObjectOutputStream(new FileOutputStream(traceFileName+"-spectest.dat"));
	     outputSpecTestText = new PrintWriter(new FileOutputStream(traceFileName+"-spectest.txt"));
      }
      catch (IOException e) {
        System.err.println("File not opened: " + e.toString());
        System.exit(1);
      }
    }
    else if (mode == REPLAY) { // replay
		System.out.println("Reading Simple M-sequence.");
      index = 0;
      try {
        //inputReplayTrace = new DataInputStream(new FileInputStream(traceFileName+"-replay.dat"));
        inputReplayText = new BufferedReader(new FileReader(traceFileName+"-replay.txt"));
      }
      catch (IOException e) {
        System.err.println("Trace file not opened: " + e.toString());
        System.exit(1);
      }
      Integer ID;
      try {
		  String sID = inputReplayText.readLine();
		  while (sID != null) {
          //*ID = inputTrace.readInt();
			 if (sID != null) {
			 	ID = Integer.valueOf(sID); 
            //System.out.println("Trace ID is: " + ID);
          	smSequence.addElement(ID); 
			 }
		    sID = inputReplayText.readLine();
        }
        if (smSequence.size() == 0) {
           System.out.println("Warning: Size of simple M-sequence being replayed is 0.");
        }
		  inputReplayText.close();
		  //inputReplayTrace.close();
      }
      catch (EOFException eof) {
        // do nothing
      }
      catch (IOException e) {
        System.err.println("Error while reading trace file: " + e.toString());
        System.exit(1);
      }
		//System.out.println("Read trace file");
		System.out.flush();
    }
    else if (mode == TEST) { // test
		System.out.println("Reading M-sequence.");
      index = 0;
      try {
		  //inputTestTrace = new ObjectInputStream(new FileInputStream(traceFileName+"-test.dat"));
        inputTestText = new BufferedReader(new FileReader(traceFileName+"-test.txt"));
      }
      catch (IOException e) {
        System.err.println("Trace file not opened: " + e.toString());
        System.exit(1);
      }
//      monitorEvent event;
//      try {
//			while (true) {
//				event = (monitorEvent)inputTestTrace.readObject();
//          	MSequence.addElement(event); 
//         }
//      }
//      catch (ClassNotFoundException e) { }
		try {
			int lineNo = 1;
			String line = null;
	      while ((line = inputTestText.readLine()) != null) {
	      	StringTokenizer t = new StringTokenizer(line," (,)");
	      	if (t.countTokens() != 4) {
	      		System.out.println("Format Error in trace file line "+lineNo+":\n"+line
	      			+ ": Expecting an event with 4 fields, read an event with " 
	      			+ t.countTokens() + " fields");
	      		System.exit(1);
	      	}
	      	String eType_S = t.nextToken(); String ID_S = t.nextToken();
	      	String methodName = t.nextToken(); String conditionName = t.nextToken();
	      	monitorEventTypeParameters.eventType eType = null;
	      	if (eType_S.equals("entry")) {
	        		eType = monitorEventTypeParameters.ENTRY;    	
	      	}
	      	else if (eType_S.equals("exit")) {
	      		eType = monitorEventTypeParameters.EXIT;
	      	}
	      	else if (eType_S.equals("wait")) {
	       		eType = monitorEventTypeParameters.WAIT;     	
	      	}
	      	else if (eType_S.equals("signal")) {
	       		eType = monitorEventTypeParameters.SIGNAL;     	
   	   	}
	      	else if (eType_S.equals("signalandexit")) {
	       		eType = monitorEventTypeParameters.SIGNALANDEXIT;     	
	      	}
   	   	else if (eType_S.equals("reentry")) {
	       		eType = monitorEventTypeParameters.REENTRY;     	
	      	}
	      	else {
	    			System.out.println("Format Error in trace file line "+lineNo+":\n"+line
	      			+ ": Unknown event type " + eType_S);
	      		System.exit(1);
	      	}
	      	int ID=-1;
	      	try {
		      	ID = Integer.parseInt(ID_S);
		      }
		      catch(NumberFormatException e) {
			      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
			      + ": integer ID expected, actual value was: " + ID_S); 
			      System.exit(1);
		      }
	      	monitorEvent event = new monitorEvent(eType,ID,methodName,conditionName);
	      	MSequence.addElement(event);
	      	//System.out.println(event);
	      	++lineNo;
   	   }
		}
      catch (IOException e) {
        System.err.println("Error while reading trace file: " + e.toString());
        System.exit(1);
      }
		//System.out.println("Read trace file");
		System.out.flush();
		try {
			if (MSequence.size() == 0) {
          	System.out.println("Warning: Size of M-sequence is 0.");
         }
		  	//inputTestTrace.close();
		  	inputTestText.close();
		}
		catch (Exception e) {
			System.err.println("Error closing trace file.");
		}
    }
    else if (mode == SPECTEST) { // test
		System.out.println("Reading trace file.");
      index = 0;
      try {
		  //inputSpecTestTrace = new ObjectInputStream(new FileInputStream(traceFileName+"-spectest.dat"));
        inputSpecTestText = new BufferedReader(new FileReader(traceFileName+"-spectest.txt"));
      }
      catch (IOException e) {
        System.err.println("Trace file not opened: " + e.toString());
        System.exit(1);
      }
      monitorSpecEvent event;   
      try {
		  String line = inputSpecTestText.readLine();
   	  int max = 0;
		  while (line != null) {
		     if (line.length()>5) {
		  		int first=0;
				first = line.indexOf("(");
		   	line = line.substring(first+1,line.length()); // 1,request)
		   	first = line.indexOf(","); 
		   	String ID = line.substring(0,first); // 1
		   	int threadID = (new Integer(ID)).intValue();
				if (threadID>max) max = threadID;
				line = line.substring(first+1,line.length()); // request);
				first = line.indexOf(")"); 
				String name = line.substring(0,first); // request
				event = new monitorSpecEvent(threadID,name);
				
         	CommSequence.addElement(event); 
         	
         	System.out.println("("+threadID+","+name+")");
         } 	
            line = inputSpecTestText.readLine();
		  }
 		  numberOfThreads = max;
		  inputSpecTestText.close();
      }
      catch (EOFException eof) {

      }
      catch (IOException e) {
        System.err.println("Error while reading trace file: " + e.toString());
        System.exit(1);
      }
		System.out.println("Read trace file");
      if (CommSequence.size() == 0) {
         System.out.println("Warning: Size of Communication-sequence is 0.");
      }
		System.out.println("");
		System.out.flush();
    }      
     

	if (mode == REPLAY) {
		numberOfThreads = ThreadIDGenerator.getInstance().getNumberOfThreads();
		//System.out.println("Number of threads is " + numberOfThreads);
		//System.out.flush();
		// if numberOfThreads is n then we need n+1 elements since IDs are 1..numberOfThreads.
		threads = new conditionVariable[(numberOfThreads+1)];
		for (int i=0; i<(numberOfThreads+1);i++)
			threads[i] = new conditionVariable();
    }
	else 	if (mode == TEST) {
		numberOfThreads = ThreadIDGenerator.getInstance().getNumberOfThreads();
		//System.out.println("maximum thread ID is " + numberOfThreads);
		//System.out.flush();
		// if numberOfThreads is n then we need n+1 elements since IDs are 
		// 1..numberOfThreads.
		threads = new conditionVariable[(numberOfThreads+1)];
		for (int i=0; i<(numberOfThreads+1);i++)
			threads[i] = new conditionVariable();

		testWatchDog woof = new testWatchDog();
		woof.start();
	}
	else if (mode == SPECTEST) {
		//numberOfThreads = ThreadIDGenerator.getInstance().getNumberOfThreads();
		//System.out.println("maximum thread ID is " + numberOfThreads);
		//System.out.flush();
		// if numberOfThreads is n then we need n+1 elements since IDs are 
		// 1..numberOfThreads.
		// plus add another space where we can put extra threads, 
		// i.e., threads that are not part of test sequence
		threads = new conditionVariable[(numberOfThreads+2)];
		for (int i=0; i<(numberOfThreads+2);i++)
			threads[i] = new conditionVariable();
		spectestWatchDog woof = new spectestWatchDog();
		woof.start();
	}
}

	// these methods used in msg controller so in Control interface so here too
	public void start() {} 
	public void requestSendPermit(int ID, String channelName, int callerVersionNumber) {}
	public boolean requestSendPermitX(int ID, String channelName, int callerVersionNumber) {return false;}
	public boolean requestSendPermitMS(int ID, String channelName, int callerVersionNumber, int calledID) {return false;}
	public int requestReceivePermitX(int ID, String channelName, int calledVersionNumber) {return -2;}
	public int requestReceivePermitMS(int ID, int calledID, String channelName, int calledVersionNumber) {return -2;}
	public void msgReceived() {}
	public void msgReceived(int caller, int callerVersionNumber) {}
	public void trace(srEvent e) {}
	public void sendArrivedRT(int ID, String channelName, int callerVersionNumber) {}
	public void monitorEnteredRT(int ID, String channelName, int callerVersionNumber) {}
	public void requestPermit(int ID) {}
	public void releasePermit() {}
	public void trace(int ID) {}

		
	
	private void outputTrace(monitorEvent m) throws Exception {
		try {
			//outputTestTrace.writeObject(m);
			//outputTestTrace.flush();
			outputTestText.println(m);
			outputTestText.flush();
			if (m.getEventType() == ENTRY || m.getEventType() == REENTRY) {
				outputReplayText.println(m.getThreadID());
				outputReplayText.flush();
				//outputReplayTrace.writeInt(m.getThreadID());
				//outputReplayTrace.flush();
				System.out.flush();
			}
			//trace.reply();			// make sure trace gets written
		}	catch (Exception e) {
				//if (outputTestTrace != null)
				//	outputTestTrace.close();
				if (outputTestText != null)
					outputTestText.close();
				//if (outputReplayTrace != null)
				//	outputReplayTrace.close();
				if (outputReplayText != null)
					outputReplayText.close();
				throw e;
    		}
	}

	public void trace(monitorEvent m) {
		enterMonitor("trace");
		try {
			outputTrace(m);
		} catch (Exception e) {
			System.err.println("Error while writing trace file: " + e.toString());
			System.exit(1);
		}
    	exitMonitor();
	}

	public void requestEntryPermit(int ID) {
     enterMonitor("requestEntryPermit");
     if (ID < numberOfThreads+1) {
		   if (index < smSequence.size()) {
				if (ID != ((Integer)smSequence.elementAt(index)).intValue()) {
				  threads[ID].waitC();
				} 
			}
			else
				  threads[ID].waitC(); // replaying a partial sequence, such as a sequence
				  							  // produced by an execution with a deadlock. Block thread indef.
	  }
	  else {	// thread is not in trace; which can occur if there was a deadlock
				// when the trace was recorded.
			Object o = new Object();
			synchronized(o) {	
				try { o.wait();} catch(InterruptedException e) {}		// block thread indefinitely		
			}
	  }
     exitMonitor();
	}

	public void releaseEntryPermit() {
    enterMonitor("releaseEntryPermit");
    ++index;
    if (index < smSequence.size())
      threads[((Integer)smSequence.elementAt(index)).intValue()].signalC();
    else
      System.out.println("\n\n***Sequence completed***\n");
    exitMonitor();
	}

	public void requestMPermit(monitorEventTypeParameters.eventType op, int ID, String methodName, 
			String conditionName) {
   	enterMonitor("requestMPermit");
      if (ID < numberOfThreads+1) {
		   if (index < MSequence.size()) {
				monitorEvent nextEvent = (monitorEvent)MSequence.elementAt(index);
    			if (ID != nextEvent.getThreadID()) {
      			threads[ID].waitC();
					nextEvent = (monitorEvent)MSequence.elementAt(index);  //update fields with correct event
				}  
				if (!(op.equals(nextEvent.getEventType()))) {
					System.out.println("Infeasible Sequence at event " + (index+1) + 
					", unexpected event type " + op);
					System.out.println("Expected event " + (index+1) + " was: ("+nextEvent.getEventType()+","+
						nextEvent.getThreadID()+","+nextEvent.getMethodName()+
					","+nextEvent.getConditionName()+")");
					System.exit(1);
				}
				if (!(methodName.equals(nextEvent.getMethodName()))) {
					System.out.println("Infeasible Sequence at event " + (index+1) + 
					", unexpected method name " + methodName + " on " + op);
					System.out.println("Expected event " + (index+1) + " was: ("+nextEvent.getEventType()+","+
						nextEvent.getThreadID()+","+nextEvent.getMethodName()+
						","+nextEvent.getConditionName()+")");
					System.exit(1);
				}
				if (op.equals(WAIT) || op.equals(SIGNAL) || op.equals(SIGNALANDEXIT)) {
					if (!(conditionName.equals(nextEvent.getConditionName()))) {
						System.out.println("Infeasible Sequence at event " + (index+1) + 
							", unexpected condition variable name " + conditionName + " on " + op);
						System.out.println("Expected event " + (index+1) + " was: ("+nextEvent.getEventType()+","+
							nextEvent.getThreadID()+","+nextEvent.getMethodName()+
							","+nextEvent.getConditionName()+")");
						System.exit(1);
					}
					++index;
		    		if (index < MSequence.size())
		      		threads[((monitorEvent)MSequence.elementAt(index)).getThreadID()].signalC();
					else {
						System.out.println("\n\n***Sequence Completed***\n");
						//System.exit(0);
					}
				}
				else if (op.equals(EXIT)) {
					++index;
		    		if (index < MSequence.size())
		      		threads[((monitorEvent)MSequence.elementAt(index)).getThreadID()].signalC();
		     		else {
						System.out.println("\n\n***Sequence Completed***\n");
						//System.exit(0);
					}
				}						
			}
			else
				  threads[ID].waitC(); // replaying a partial sequence, such as a sequence
				  							  // produced by an execution with a deadlock. Block thread indef.
	   }
	   else {	// thread is not in trace; which can occur if there was a deadlock
				// when the trace was recorded.
			Object o = new Object();
			synchronized(o) {	
				try { o.wait();} catch(InterruptedException e) {}		// block thread indefinitely		
			}
	   }		
    exitMonitor();
	}				

	public void releaseMPermit() {
   	enterMonitor("releaseMPermit");
    	++index;
    	if (index < MSequence.size()) {
      	threads[((monitorEvent)MSequence.elementAt(index)).getThreadID()].signalC();
		}
		else {
			System.out.println("\n\n***Sequence Completed***\n");
		}
    	exitMonitor();
	}
	
	public void requestMPermitSpecTest(int ID) {
	// called when thread tries to (re)enter monitor
   	enterMonitor("requestMPermitSpecTest");
 		if (index < CommSequence.size()) {
			monitorSpecEvent nextEvent = (monitorSpecEvent)CommSequence.elementAt(index);
			if (ID != nextEvent.getThreadID()) {
				if (ID < numberOfThreads+1) {
      			threads[ID].waitC();
					nextEvent = (monitorSpecEvent)CommSequence.elementAt(index);  //update fields with correct event
				}
				else {
					threads[numberOfThreads+1].waitC();
				}
			}
		}
		else { // sequence is empty so delay threads indef.
				threads[numberOfThreads+1].waitC();
		}
     	exitMonitor();
	}
	
	public void requestAndReleaseCommPermit(int ID, String eventName) {
	// called when exerciseEvent("eventLabel") is performed
 		enterMonitor("requestAndReleaseCommPermit");
 		monitorSpecEvent nextEvent = null;
 		if (index < CommSequence.size()) {
			nextEvent = (monitorSpecEvent)CommSequence.elementAt(index);
	    	if (ID != nextEvent.getThreadID()) {
				if (ID < numberOfThreads+1) {
	      			threads[ID].waitC();
					nextEvent = (monitorSpecEvent)CommSequence.elementAt(index);  //update fields with correct event
				}
				else {
					threads[numberOfThreads+1].waitC();
				}
			}
		}
		else { // sequence is empty so delay threads indef.
				threads[numberOfThreads+1].waitC();
		}
		if(!eventName.equals(nextEvent.geteventName())) {
			System.out.println("Infeasible Sequence at event " + (index+1) +
				", unexpected event name " + eventName);
			System.out.println("Expected event "+ (index+1) + " was: (" +
				nextEvent.getThreadID() + "," + nextEvent.geteventName() + ")");
			System.exit(1);
		}

		// release
	    ++index;
		if (index < CommSequence.size()) {
			threads[((monitorSpecEvent)CommSequence.elementAt(index)).getThreadID()].signalC();
			exitMonitor();
			return;
		}
		else {
			System.out.println("\n\n***Sequence Completed***\n");
			//System.exit(0);
		}
		exitMonitor();	
	}
	
	public void exerciseEvent(int ID, String eventName) {
		requestAndReleaseCommPermit(ID, eventName);
	}
	
	public void traceCommEvent(monitorSpecEvent m)  {
		enterMonitor("traceCommEvent");
		try {
			//outputSpecTestTrace.writeObject(m);
			//outputSpecTestTrace.flush();
			outputSpecTestText.println(m);
			outputSpecTestText.flush();
		}	
		catch (Exception e) {
			System.err.println("Error while writing trace file: " + e.toString());
			System.exit(1);
		}
		exitMonitor();
	}


	final class testWatchDog extends Thread {
		public void run() {
		//System.out.println("watchdog running");
			while (index < MSequence.size()) {
				int saveIndex = index;
				try {
					Thread.sleep(2000);
				}
				catch (InterruptedException e) {}
				if (saveIndex == index) {
						monitorEvent infeasibleEvent = (monitorEvent)MSequence.elementAt(index);
						System.out.println("Infeasible Sequence - timeout waiting for event " +
							(index+1) + ": ("+infeasibleEvent.getEventType()+","+infeasibleEvent.getThreadID()+
							","+infeasibleEvent.getMethodName()+","+infeasibleEvent.getConditionName()+")");
						System.out.flush();
						System.exit(1);
				}
			}
		}
	}
	final class spectestWatchDog extends Thread {
		public void run() {
		//System.out.println("watchdog running");
			while (index < CommSequence.size()) {
				int saveIndex = index;
				try {
					Thread.sleep(2000);
				}
				catch (InterruptedException e) {}
				if (saveIndex == index) {
						monitorSpecEvent infeasibleEvent = (monitorSpecEvent)CommSequence.elementAt(index);
						System.out.println("Infeasible Sequence - timeout waiting for event " +
							(index+1) + ": (" + infeasibleEvent.getThreadID()+
							","+infeasibleEvent.geteventName()+")");
						System.out.flush();
						System.exit(1);
				}
			}
		}
	}
}

