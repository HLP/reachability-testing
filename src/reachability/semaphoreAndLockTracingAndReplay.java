package reachability;
import java.io.*;
import java.util.*;

public final class semaphoreAndLockTracingAndReplay extends monitorSC 
	implements propertyParameters, Control {
// Provides Trace and Replay for semaphores or locks. SYNseq is a simple 
// PV-sequence or LockUnlock-sequence. Trace or Replay is selected by the user.
	private int index = 0;
   private Vector SYNsequence = new Vector();
	private int numberOfThreads; // number of threads in ThreadID file: 1..numberOfThreads
	private conditionVariable[] threads;

	private DataOutputStream outputReplayTrace = null; // output sequence of int IDs
	private DataInputStream inputReplayTrace = null; // input sequence of int IDs

	private PrintWriter outputReplayText = null;
	private BufferedReader inputReplayText = null;

	//private propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	//private propertyParameters.Controllers numControllers = SINGLE;
	String traceFileName;
	
	private static final Object classLock = semaphoreAndLockTracingAndReplay.class;
	private static semaphoreAndLockTracingAndReplay instance = null;
	
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
	public void traceCommEvent(monitorSpecEvent m) {}
	
	public void trace(monitorEvent m) {}
	public void requestEntryPermit(int ID) {}
	public void releaseEntryPermit() {}
	public void requestMPermit(monitorEventTypeParameters.eventType op, int ID, String methodName, 
			String conditionName) {}
	public void releaseMPermit() {}
	public void requestMPermitSpecTest(int ID) {}
	public void requestAndReleaseCommPermit(int ID, String eventName) {}
	public void exerciseEvent(int ID, String eventName) {}


public semaphoreAndLockTracingAndReplay(propertyParameters.Mode mode, 
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
      }
      catch (IOException e) {
        System.err.println("File not opened: " + e.toString());
        System.exit(1);
      }
    }
    else if (mode == REPLAY) { // replay
		System.out.println("Reading trace file.");
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
			 if (sID != null) {
			 	ID = Integer.valueOf(sID); 
            //System.out.println("Trace ID is: " + ID);
          	SYNsequence.addElement(ID); 
			 }
		    sID = inputReplayText.readLine();
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
		System.out.println("Read trace file");
		System.out.flush();
		numberOfThreads = ThreadIDGenerator.getInstance().getNumberOfThreads();
		// if numberOfThreads is n then we need n+1 elements since IDs are 1..numberOfThreads.
		threads = new conditionVariable[(numberOfThreads+1)];
		for (int i=0; i<(numberOfThreads+1);i++)
			threads[i] = new conditionVariable();
    }
	
	}
	
	public static semaphoreAndLockTracingAndReplay getInstance(propertyParameters.Mode mode, 
			propertyParameters.Controllers numControllers, String traceFileName) { 
		synchronized(classLock) {
      	if (instance == null) {
        		instance = new semaphoreAndLockTracingAndReplay(mode,numControllers,traceFileName);
        	}
		}
      return instance;
   }

	private void outputTrace(int ID) throws Exception {
		try {
			outputReplayText.println(ID);
			outputReplayText.flush();
			//outputReplayTrace.writeInt(ID);
			//outputReplayTrace.flush();
		} catch (Exception ex) {
			//if (outputReplayTrace != null)
			//	outputReplayTrace.close();
			if (outputReplayText != null)
				outputReplayText.close();
			throw ex;
	     }
	}
	
	public void trace(int ID) {
		enterMonitor("trace");
		try {
			outputTrace(ID);
		} catch (Exception e) {
			System.err.println("Error while writing trace file: " + e.toString());
			System.exit(1);
		}
		exitMonitor();
	}
	public void requestPermit(int ID) {
	   enterMonitor("requestPermit");
	   if (ID < numberOfThreads+1) {
		   if (index < SYNsequence.size()) {
				if (ID != ((Integer)SYNsequence.elementAt(index)).intValue()) {
				  threads[ID].waitC();
				}  
			}
			else
				  threads[ID].waitC(); // replaying a partial sequence, such as a sequence
				  							  // produced by an execution with a deadlock. Block thread indef.
		}
		else {	// thread is not in trace; which can occur if there was a deadlock
					// when the trace was recorded
			Object o = new Object();
			synchronized(o) {	
				try { o.wait();} catch(InterruptedException e) {}		// block thread indefinitely		
			}
		}
		exitMonitor();
	}
	public void releasePermit() {
	   enterMonitor("releasePermit");
	   ++index;
	   if (index < SYNsequence.size())
	      threads[((Integer)SYNsequence.elementAt(index)).intValue()].signalC();
	   else System.out.println("Sequence Completed");
  		exitMonitor();
	}
}

