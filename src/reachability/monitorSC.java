package reachability;
// contains classes monitorDebuggingToolbox and monitorTracingAndReplay

import java.util.*;

/*
Vi: the type of this SYN-event
Ti: the thread executing the event
Mi: the called monitor method, qualified with the monitor name if there are multiple monitors
Ci: the condition variable if this event is of type (c) or (d). 
*/

public class monitorSC implements propertyParameters, monitorEventTypeParameters, eventTypeParameters {
// Monitor Toolbox for tracing and replay

	private binarySemaphore mutex = new binarySemaphore(1,NONE);
	private String currentMethodName = null;

	private propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	private propertyParameters.Controllers numControllers = SINGLE;
	private propertyParameters.RandomDelay randomDelay = OFF;
	protected propertyParameters.Strategy strategy = OBJECT;
	protected propertyParameters.GenerateLTS generateLTS = LTSOFF;
	protected propertyParameters.DetectDeadlock detectDeadlock = DETECTIONOFF;
	protected propertyParameters.SymmetryReduction SymmetryReduce = SYMMETRYREDUCTIONOFF;
	
	private final int	delayAmount = 750;
	
	private ArrayList openList = null; // monitor name (representing always open port)

	private String monitorName = null;
	private int ID;
	private vectorTimeStamp vectorTS;
	private int versionNumber = 1;
	public int getVersionNumber() { return versionNumber; }
	public int getAndIncVersionNumber() {
		return versionNumber++;
	}

	private static monitorTracingAndReplay m = null;

	private static final Object classLock =
                            monitorTracingAndReplay.class;

	// now msgBased and monitor controllers implement Control
	private Control control = null;
	
	public int getID() {return ID;}

	private int generateIDFromUserNameRT(String userName) {
		// for RT, monitors get IDs likes threads, since monitors have a slot in the timestamp
		boolean isThread = false;
		return (ThreadIDGenerator.getInstance().getIDFromUserName(userName,isThread));
	}
	
	  private int generateIDRT() { 
		String stringID = null;	// internal name of thread class
		if (Thread.currentThread() instanceof innerThread)
		   // get stringID of parent TDThread
			stringID = ((innerThread)Thread.currentThread()).getThreadName();
		else
			// parent is main thread
			stringID = "";

	   String thisClassName; 
		Class c = this.getClass();
		thisClassName = c.getName(); // class T in "class T extends TDThread"
		if ((stringID.indexOf('_')) >= 0) 
			// parent of currentThread is a TDThread so use its name as prefix
			// e.g., "main_T" is name of thread executing "new TT()"
			stringID = stringID+"_"+thisClassName; // e.g., "main_T_TT
		else
			// parent of currentThread is not a TDThread, so it's the main thread	
			stringID = "main_" + thisClassName;
		boolean isThread = false;
		int myID = (ThreadIDGenerator.getInstance().getID(stringID,isThread));
		return myID;
	}


	private String generateIDFromUserName(String userName) {
		return (monitorIDGenerator.getInstance().getIDFromUserName(userName));
	}


	private String generateID() {
		String stringID = null;	// internal name of thread class
		if (Thread.currentThread() instanceof innerThread)
		   // get stringID of parent TDThread
			stringID = ((innerThread)Thread.currentThread()).getThreadName();
		else
			// parent is main thread
			stringID = "";

	   String thisClassName; 
		Class c = this.getClass();
		thisClassName = c.getName(); // class T in "class T extends TDThread"
		if ((stringID.indexOf('_')) >= 0) 
			// parent of currentThread is a TDThread so use its name as prefix
			// e.g., "main_T" is name of thread executing "new TT()"
			stringID = stringID+"_"+thisClassName; // e.g., "main_T_TT
		else
			// parent of currentThread is not a TDThread, so it's the main thread	
			stringID = "main_" + thisClassName;


		String nextName = (monitorIDGenerator.getInstance().getID(stringID));
		return nextName;
	}

	public monitorSC() { 
		mode = (propertyReader.getInstance().getModeProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		generateLTS = (propertyReader.getInstance().getGenerateLTSProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		SymmetryReduce = (propertyReader.getInstance().getSymmetryReductionProperty());


		if (!(mode==NONE)) {
			if (mode==RT) {
				this.ID = generateIDRT();
				this.monitorName = ThreadIDGenerator.getInstance().getName(ID);
				initVectorTS();
				OpenEvent e = new OpenEvent(this.monitorName,-1);
				openList = new ArrayList(); openList.add(e); // (this.monitorName);
				String traceFileName;
				//if (numControllers==MULTIPLE)
				//	traceFileName = monitorName;
				//else
					traceFileName = "channel";  // same as for class channel
				//if (numControllers == SINGLE) 
					// RT always uses a single Controller
					control = instanceRT(mode,numControllers,strategy,traceFileName);
				//else {
				//	control = new msgTracingAndReplay(mode,numControllers,strategy,traceFileName);
				//	control.start();
				//}
			}
			else {
				this.monitorName = generateID();
				String traceFileName;
				if (numControllers==MULTIPLE)
					traceFileName = monitorName;
				else
					traceFileName = "monitor";
				if (mode == TRACE || mode == REPLAY || mode == TEST || mode == SPECTEST) {
					if (numControllers == SINGLE) 
						control = instance(mode,numControllers,traceFileName);
					else {
							control = new monitorTracingAndReplay(mode,numControllers,traceFileName);
					}
				}			
			}
		}
	}

	public monitorSC(String monitorName) { 
		this.monitorName = monitorName;
		mode = (propertyReader.getInstance().getModeProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		generateLTS = (propertyReader.getInstance().getGenerateLTSProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		SymmetryReduce = (propertyReader.getInstance().getSymmetryReductionProperty());

		if (!(mode==NONE)) {
			if (mode == RT) {
				this.ID = 	generateIDFromUserNameRT(monitorName);
				String traceFileName = ThreadIDGenerator.getInstance().getName(ID);
				initVectorTS();
				OpenEvent e = new OpenEvent(this.monitorName,-1);
				openList = new ArrayList(); openList.add(e); // (monitorName);
				//if (numControllers==SINGLE)
					traceFileName = "channel";
				//if (numControllers == SINGLE) 
					control = instanceRT(mode,numControllers,strategy,traceFileName);
				//else {
				//	control = new msgTracingAndReplay(mode,numControllers,strategy,traceFileName);
				//	control.start();
				//}
			}
			else {
				String traceFileName = generateIDFromUserName(monitorName);
				if (numControllers==SINGLE)
					traceFileName = "monitor";
				if (mode == TRACE || mode == REPLAY || mode == TEST || mode == SPECTEST) {
					if (numControllers == SINGLE) 
						control = instance(mode,numControllers,traceFileName);
					else {
						control = new monitorTracingAndReplay(mode,numControllers,traceFileName);
					}
				}			
			}
		}
	}

	private monitorTracingAndReplay instance(propertyParameters.Mode mode, 
			propertyParameters.Controllers numControllers, String traceFileName) { 
		synchronized(classLock) {
      	if (m == null) {
        		m = new monitorTracingAndReplay(mode, numControllers, traceFileName);
			}
      return m;
    }
  }
 
  	private msgTracingAndReplay instanceRT(propertyParameters.Mode mode, 
		propertyParameters.Controllers numControllers,
		propertyParameters.Strategy strategy, String traceFileName) { 
			return msgTracingAndReplay.getInstance(mode,numControllers,strategy,traceFileName);
		//synchronized(classLock) {
      //	if (mMsg == null) {
      //	   //System.out.println("new instance of controller");
      //		mMsg = new msgTracingAndReplay(mode,numControllers,strategy,traceFileName);
		//		mMsg.start();
		//	}
      //	return mMsg;
    	//}
  }

	public monitorSC(propertyParameters.Mode mode) {
	// This constructor is called from monitorTraceAndReplay so no controller/replay/trace is used
		this.mode=NONE;
	}
	
	   // Returns the integer clock value associated with this thread.
	public int getIntegerTS() { 
		return vectorTS.getIntegerTS(getID());
	}
    
	// Increments the integer clock associated with this thread.
    public void updateIntegerTS() { 
		vectorTS.updateIntegerTS(getID());
    } 

	// Updates the integer clock with the value passed in.
	public void setIntegerTS(int ts){
		vectorTS.setIntegerTS(getID(),ts);
	}

   /*
    * Merges integer clock with the given clock.
    * Sets the integer clock to the larger of two clock values, 
   */
	public void mergeIntegerTS(int ts){
		int clockValue = vectorTS.getIntegerTS(getID());
		setIntegerTS (Math.max(clockValue, ts + 1));
	}

	// Returns a clone of the vector timestamp associated with this thread
	public vectorTimeStamp getVectorTS() { 
		return (vectorTimeStamp) vectorTS.clone(); 
	} 
    
	// Updates vector timestamp with the passed in vector timestamp
	public void updateVectorTS(vectorTimeStamp newTS) { 
		vectorTS.updateVectorTS(newTS);
	} 

	// private method that initializes the vector time stamp
	// for this thread
	private void initVectorTS() {
		vectorTS = new vectorTimeStamp(getID());
	}

	public final class conditionVariable {
		private countingSemaphore threadQueue = new countingSemaphore(0,true); 
		  // true indicates this semaphore is part of monitorSC toolbox
		  // so that V_updateTimestamp() is called in signalC and signalCall
		private int numWaitingThreads = 0;
		private String conditionName = null;


		private String generateID() {
			String stringID = null;	// internal name of thread class
			if (Thread.currentThread() instanceof innerThread)
		   	// get stringID of parent TDThread
				stringID = ((innerThread)Thread.currentThread()).getThreadName();
			else
				// parent is main thread
				stringID = "";

	   	String thisClassName; 
			Class c = this.getClass();
			thisClassName = c.getName(); // class T in "class T extends TDThread"
			if ((stringID.indexOf('_')) >= 0) 
				// parent of currentThread is a TDThread so use its name as prefix
				// e.g., "main_T" is name of thread executing "new TT()"
				stringID = stringID+"_"+thisClassName; // e.g., "main_T_TT
			else
				// parent of currentThread is not a TDThread, so it's the main thread	
				stringID = "main_" + thisClassName;


			String nextName = (conditionVarIDGenerator.getInstance().getID(stringID));
			return nextName;
		}

		private String generateIDFromUserName(String userName) {
			return (conditionVarIDGenerator.getInstance().getIDFromUserName(userName));
		}
	
		public conditionVariable(String conditionName) {
			//mode = (propertyReader.getInstance().getModeProperty());
			this.conditionName = conditionName;
			if (!(mode==NONE))
				this.conditionName = generateIDFromUserName(conditionName);
		}
	
		public conditionVariable() {
			//mode = (propertyReader.getInstance().getModeProperty());
			if (!(mode==NONE))
				this.conditionName = generateID();
		}

		public final void signalC() {
			if (mode == TRACE) {
				monitorEvent m = new monitorEvent(SIGNAL,((innerThread)Thread.currentThread()).getID(),currentMethodName,conditionName);
     			control.trace(m); 
			}
			else if (mode == TEST) {
				control.requestMPermit(SIGNAL,
			  		((innerThread)Thread.currentThread()).getID(),
					currentMethodName,conditionName);			
   		}
			if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE)) {
				if (numWaitingThreads > 0)
					deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
					+ " " + conditionName+".signalC() in " + currentMethodName);					
				else
					deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
					+ " " + conditionName+".signalC() in " + currentMethodName);		
			}
			if (numWaitingThreads > 0) {
				numWaitingThreads--;
				if (mode == RT) {
			      ((innerThread)Thread.currentThread()).updateIntegerTS();
					threadQueue.V_updateTimestamp();  // In RT mode, update timestamp of signaled thread.
				}
				else threadQueue.V();
				if (detectDeadlock == DETECTIONON && (mode == RT || mode == TRACE)) {
					innerThread.decNumBlockedThreads();
				}
   		}
		}
		
		public final void signalCall() {
			if (mode == TRACE) {
				monitorEvent m = new monitorEvent(SIGNAL,((innerThread)Thread.currentThread()).getID(),currentMethodName,conditionName);
     			control.trace(m); 
			}
			else if (mode == TEST) {
				control.requestMPermit(SIGNAL,
			  		((innerThread)Thread.currentThread()).getID(),
					currentMethodName,conditionName);			
   		}	
			if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE)) {
				if (numWaitingThreads > 0)
					deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
					+ " " + conditionName+".signalCall() in " + currentMethodName);					
				else
					deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
					+ " " + conditionName+".signalCall() in " + currentMethodName);		
			}
			while (numWaitingThreads > 0) {
        		--numWaitingThreads;
				if (mode == RT) {
			      ((innerThread)Thread.currentThread()).updateIntegerTS();
					threadQueue.V_updateTimestamp();  // In RT mode, update timestamp of signaled thread.
				}
				else threadQueue.V();
				if (detectDeadlock == DETECTIONON && (mode == RT || mode == TRACE)) {
					innerThread.decNumBlockedThreads();
				}
   		}
		}	  

		public final void waitC() {
			String saveMethodName = currentMethodName;
	      int callerForReceiveEvent = -1;
			if (mode == TRACE) {
				monitorEvent m = new monitorEvent(WAIT,((innerThread)Thread.currentThread()).getID(),currentMethodName,conditionName);
     			control.trace(m); 
			}
			else if (mode == TEST) {
				control.requestMPermit(WAIT,		// no release needed since in critical section
			  		((innerThread)Thread.currentThread()).getID(),
					currentMethodName,conditionName);			
   		}
  			else if (mode == RT) {
				// update monitor timestamp on exit in case send/receive done inside monitor
	     		updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
		   }

			if (detectDeadlock == DETECTIONON && (mode == RT || mode == TRACE)) {
				innerThread.incNumBlockedThreads();
				deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"blocked on waitC() in method " + saveMethodName);
				deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
					+ " blocked on " + conditionName+".waitC() in " + saveMethodName);
			}
   		numWaitingThreads++;
			threadQueue.VP(mutex);
			if (detectDeadlock == DETECTIONON && (mode == RT || mode == TRACE)) {
				deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"running");
				deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
					+ " completed " + conditionName+".waitC() in " + saveMethodName);	

			}
   		if (mode == REPLAY)
	     			control.requestEntryPermit(((innerThread)Thread.currentThread()).getID());
			else if (mode == TEST) {
				control.requestMPermit(REENTRY,
			  		((innerThread)Thread.currentThread()).getID(),
					saveMethodName,"NA");			
   		}
 			else if (mode == SPECTEST) {
				control.requestMPermitSpecTest(((innerThread)Thread.currentThread()).getID()); 
   		}
			else if (randomDelay == ON && mode == TRACE) {
				try {
				int r = (int)(Math.random()*delayAmount);
				Thread.sleep(r); // (int)(Math.random()*delayAmount));	// default delayAmount is 750
				} catch (InterruptedException e) {}
			}
   		else if (mode == RT) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
			control.requestSendPermitMS(((innerThread)Thread.currentThread()).getID(),saveMethodName,((innerThread)Thread.currentThread()).getVersionNumber(),getID());
			
			String label = "unaccepted_send_entry";
			// label is used as the Program Counter for the symmetry reduction. 	
			if (SymmetryReduce == SYMMETRYREDUCTIONON) {
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				label = B.toString();
			}
	      
			srEvent e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							getVersionNumber(),saveMethodName,-1,
							UNACCEPTED_ASYNCH_SEND, ((innerThread)Thread.currentThread()).getVectorTS(),label, false,openList,MONITOR_CALL);
     		control.trace(e); 
     		
			control.msgReceived();
			
			


			//System.out.println(((innerThread)Thread.currentThread()).getID()+" requesting receivePermitX");
			// Note: request made by caller/current thread; version number ignored since
			// really can't say what version number of "monitor thread" is here.
			// The send-->receive receive<--send is just a model for race analysis, not
			// how replay is implemented.
			
			callerForReceiveEvent = control.requestReceivePermitMS(((innerThread)Thread.currentThread()).getID(),getID(),saveMethodName,getVersionNumber());  		

	   }
			mutex.P();
			if (detectDeadlock == DETECTIONON && (mode == RT || mode == TRACE)) {
				deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
					+ " reentered monitor in " + saveMethodName);	
			}
			if (mode == TRACE) {
				currentMethodName = saveMethodName;		// restore name
				monitorEvent m = new monitorEvent(REENTRY,((innerThread)Thread.currentThread()).getID(),currentMethodName,"NA");
     			control.trace(m); 
			}
   		if (mode == REPLAY)
     				control.releaseEntryPermit();
			else if (mode == TEST) {
				currentMethodName = saveMethodName;
				control.releaseMPermit();
			}
			else if (mode == SPECTEST) {
		
			}
	   	else if (mode == RT) {
				currentMethodName = saveMethodName;
				
   			if (callerForReceiveEvent==-2) {
   				control.monitorEnteredRT(((innerThread)Thread.currentThread()).getID(), currentMethodName, ((innerThread)Thread.currentThread()).getVersionNumber());
   			}
   			
				updateIntegerTS();
				
				srEvent e = new srEvent(-2,getID(),-2,getVersionNumber(),
   				currentMethodName,-1,UNACCEPTED_RECEIVE,
         		getVectorTS(),"unacceptedReceive",false,openList,UNACCEPTED_RECEIVE); 
         		         		
   	      control.trace(e);
   	      
		      // update receiver with monitors timestamp: send ----> receive
   	      updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
	  			e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1,
							getAndIncVersionNumber(),currentMethodName,-1,
							ASYNCH_RECEIVE, getVectorTS(),"", true,openList,MONITOR_ENTRY);
     			control.trace(e);

				// report caller and caller version number
				control.msgReceived(((innerThread)Thread.currentThread()).getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1);
     			
				
   	      ((innerThread)Thread.currentThread()).updateVectorTS(getVectorTS());
   	       
			}
		}

		public final boolean empty() {
 			return (numWaitingThreads == 0);
		}

		public final int length() {
			return numWaitingThreads;
		}
	}

	public final void enterMonitor(String methodName) {
      int callerForReceiveEvent = -1;
   	if (mode == REPLAY)
	     		control.requestEntryPermit(((innerThread)Thread.currentThread()).getID()); 
		else if (mode == TEST) {
			control.requestMPermit(ENTRY,
			  ((innerThread)Thread.currentThread()).getID(),
				monitorName + ":" + methodName,"NA");			
   	}
 		else if (mode == SPECTEST) {
			control.requestMPermitSpecTest(((innerThread)Thread.currentThread()).getID()); 
   	}
   	else if (randomDelay == ON && mode == TRACE) {
			try {
				int r = (int)(Math.random()*delayAmount);
				Thread.sleep(r); // (int)(Math.random()*delayAmount));	// default delayAmount is 750
			} catch (InterruptedException e) {}
		}
   	else if (mode == RT) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
			control.requestSendPermitMS(((innerThread)Thread.currentThread()).getID(),monitorName + ":" + methodName,((innerThread)Thread.currentThread()).getVersionNumber(),getID());
			String label = "unaccepted_send_entry";
			// label is used as the Program Counter for the symmetry reduction. 	
			if (SymmetryReduce == SYMMETRYREDUCTIONON) {
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				label = B.toString();
			}
      
			srEvent e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							getVersionNumber(),monitorName + ":" + methodName,-1,
							UNACCEPTED_ASYNCH_SEND, ((innerThread)Thread.currentThread()).getVectorTS(),label, false,openList,MONITOR_CALL);
     		control.trace(e); 
     		
			control.msgReceived();
			

			// Note: request made by caller/current thread; version number ignored since
			// really can't say what version number of "monitor thread" is here.
			// The send-->receive receive<--send is just a model for race analysis, not
			// how replay is implemented.
			callerForReceiveEvent = control.requestReceivePermitMS(((innerThread)Thread.currentThread()).getID(),getID(),monitorName + ":" + methodName,getVersionNumber());  		

	   }

		mutex.P();

		if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE)) {
			deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
			+ " entered monitor method " + methodName);				
		}		
		
   	if (mode == REPLAY)
     			control.releaseEntryPermit();
		else if (mode == TEST) {
				currentMethodName = monitorName + ":" + methodName;
				control.releaseMPermit();
		}
   	else if (mode == TRACE) {
				currentMethodName = monitorName + ":" + methodName;
				monitorEvent m = new monitorEvent(ENTRY,((innerThread)Thread.currentThread()).getID(),currentMethodName,"NA");
     			control.trace(m); 
		}
		else if (mode == SPECTEST) {
		}
   	else if (mode == RT) {

   			currentMethodName = monitorName + ":" + methodName;
   			
  				if (callerForReceiveEvent==-2) {
   				control.monitorEnteredRT(((innerThread)Thread.currentThread()).getID(), currentMethodName, ((innerThread)Thread.currentThread()).getVersionNumber());
   			}			
				updateIntegerTS();
				
				srEvent e = new srEvent(-2,getID(),-2,getVersionNumber(),
   				monitorName + ":" + methodName,-1,UNACCEPTED_RECEIVE,
         		getVectorTS(),"unacceptedReceive",false,openList,UNACCEPTED_RECEIVE); 
         		         		
   	      control.trace(e);
   	      
		      // update receiver with callers timestamp: send ----> receive
   	      updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());

	  			e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1,
							getAndIncVersionNumber(),monitorName + ":" + methodName,-1,
							ASYNCH_RECEIVE, getVectorTS(),"", true,openList,MONITOR_ENTRY);
     			control.trace(e);

				// report caller and caller version number
				control.msgReceived(((innerThread)Thread.currentThread()).getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1);
     			
				
   	      ((innerThread)Thread.currentThread()).updateVectorTS(getVectorTS());
   	      

		}
	}
	
	public final void enterMonitor() {
		mutex.P();
		if (mode != NONE) {
			System.out.println("Error: In trace, test, replay, spectest, or rt mode, the call to ");
			System.out.println("enterMonitor() must include the name of the method being entered, e.g., ");
			System.out.println("   enterMonitor(\"deposit\");");
			System.exit(1);
		}
	}

	public final void exitMonitor() {

		if (mode == TRACE) {
				monitorEvent m = new monitorEvent(EXIT,((innerThread)Thread.currentThread()).getID(),currentMethodName,"NA");
     			control.trace(m); 
		}
		else if (mode == TEST) {
			control.requestMPermit(EXIT,
			  ((innerThread)Thread.currentThread()).getID(),
				currentMethodName,"NA");			
   	}
 		else if (mode == RT) {
			// update monitor timestamp on exit in case send/receive done inside monitor
	     	updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
	   }
		if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE)) {
			deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
			+ " exited monitor method " + currentMethodName);				
		}
		mutex.V();
	}
	final void exerciseEvent(String eventName) {
		int ID=0;
		if (mode == TRACE || mode == SPECTEST) {
			ID = ((innerThread)Thread.currentThread()).getID();
		}
		if (mode == TRACE) {
     		traceCommEvent(ID,eventName); 
		}
		else if (mode == SPECTEST) {
			control.exerciseEvent(ID,eventName);
   	}	
	}
	
	final void traceCommEvent(int ID, String eventName) {
		monitorSpecEvent m = new monitorSpecEvent(ID,eventName);
			control.traceCommEvent(m);
	}
}


