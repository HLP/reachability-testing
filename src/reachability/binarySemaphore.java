package reachability;
import java.util.*;

public final class binarySemaphore extends semaphore implements propertyParameters, monitorEventTypeParameters, eventTypeParameters {
	// a FIFO binary semaphore

	private ArrayList waitingP = new ArrayList(); // queue of threads blocked on P
	private ArrayList waitingV = new ArrayList(); // queue of threads blocked on V

	private propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	private propertyParameters.Controllers numControllers = SINGLE;
	private propertyParameters.RandomDelay randomDelay = OFF;
	protected propertyParameters.Strategy strategy = OBJECT;
	protected propertyParameters.GenerateLTS generateLTS = LTSOFF;
	protected propertyParameters.DetectDeadlock detectDeadlock = DETECTIONOFF;
	protected propertyParameters.SymmetryReduction SymmetryReduce = SYMMETRYREDUCTIONOFF;

	private final int	delayAmount = 750;

	private boolean toolboxSemaphore = false;
	private ArrayList VP_openList; // openList with "P" and "V" in it.
	private ArrayList P_openList; // openList with "P" in it
	private ArrayList V_openList; // openList with "V" in it
	private ArrayList nullOpenList = new ArrayList();
	// set to true if this sem is part of monitorSC implementation so 
	// timestamp of awakened thread is updated with timestamp of caller on V operation

	private String semaphoreName = null;
	private int ID;
	private vectorTimeStamp vectorTS;
	private int versionNumber = 1;
	public int getVersionNumber() { return versionNumber; }
	public int getAndIncVersionNumber() {
		return versionNumber++;
	}

	// msgBased and monitor controllers implement Control
	private Control control = null;

	public int getID() {return ID;}

	private int generateIDFromUserNameRT(String userName) {
		// for RT, monitors get IDs likes threads, since monitors have a slot in the timestamp
		boolean isThread = false;
		return (ThreadIDGenerator.getInstance().getIDFromUserName(userName,isThread));
	}


	private int generateIDRT() { 
		// IDs generated by ThreadIDGenerator (since need position in vector timestamps)
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
		//	System.out.println("User Name: " + userName);
		return (semaphoreIDGenerator.getInstance().getIDFromUserName(userName));
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


		String nextName = (semaphoreIDGenerator.getInstance().getID(stringID));
		return nextName;
	}

	public binarySemaphore(int initialPermits) {
		super(initialPermits);
		if (initialPermits != 0 && initialPermits != 1)
			throw new IllegalArgumentException("initial value of binarySemaphore must be 0 or 1");
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
				this.semaphoreName = ThreadIDGenerator.getInstance().getName(ID);
				initVectorTS();
				VP_openList = new ArrayList(); 
				OpenEvent v = new OpenEvent("V",-1);
				VP_openList.add(v);
				OpenEvent p = new OpenEvent("P",-1);
				VP_openList.add(p);
				V_openList = new ArrayList(); V_openList.add(v);
				P_openList = new ArrayList(); P_openList.add(p);	
				String traceFileName;
				//if (numControllers==MULTIPLE)
				//	traceFileName = semaphoreName;
				//else
				traceFileName = "channel";  // same as for class channel
				//if (numControllers == SINGLE) 
				control = instanceRT(mode,numControllers,strategy,traceFileName);
				//else {
				//	System.out.println("Error: the number of controllers must be one for reachability testing.");
				//	System.exit(1);
				//control = new msgTracingAndReplay(mode,numControllers,strategy,traceFileName);
				//control.start();
				//}
			}
			else {
				this.semaphoreName = generateID();
				String traceFileName;
				if (numControllers==MULTIPLE)
					traceFileName = semaphoreName;
				else
					traceFileName = "semaphores";
				if (mode == TRACE || mode == REPLAY) {
					if (numControllers == SINGLE)
						control = semaphoreAndLockTracingAndReplay.getInstance(mode,numControllers,traceFileName);
					else {
						control = new semaphoreAndLockTracingAndReplay(mode,numControllers,traceFileName);
					}
				}
				else {
					System.out.println("Error: Only trace, replay, and reachability testing modes are supported for semaphores.");
					System.exit(1);
				}
			}
		}
	}

	public binarySemaphore(int initialPermits, String semaphoreName) {
		super(initialPermits);
		if (initialPermits != 0 && initialPermits != 1)
			throw new IllegalArgumentException("initial value of binarySemaphore must be 0 or 1");
		this.semaphoreName = semaphoreName;
		mode = (propertyReader.getInstance().getModeProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		generateLTS = (propertyReader.getInstance().getGenerateLTSProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		SymmetryReduce = (propertyReader.getInstance().getSymmetryReductionProperty());

		if (!(mode==NONE)) {
			if (mode == RT) {
				this.ID = 	generateIDFromUserNameRT(semaphoreName);
				String traceFileName = ThreadIDGenerator.getInstance().getName(ID);
				initVectorTS();
				VP_openList = new ArrayList(); 
				OpenEvent v = new OpenEvent("V",-1);
				VP_openList.add(v);
				OpenEvent p = new OpenEvent("P",-1);
				VP_openList.add(p);
				V_openList = new ArrayList(); V_openList.add(v);
				P_openList = new ArrayList(); P_openList.add(p);
				//if (numControllers==MULTIPLE)
				//	traceFileName = semaphoreName;
				//else
				traceFileName = "channel";  // same as for class channel
				//if (numControllers == SINGLE) 
				control = instanceRT(mode,numControllers,strategy,traceFileName);
				//else {
				//	System.out.println("Error: the number of controllers must be one for reachability testing.");
				//	System.exit(1);
				//}
			}
			else {
				String traceFileName = generateIDFromUserName(semaphoreName);
				if (numControllers==SINGLE)
					traceFileName = "semaphores";
				if (mode == TRACE || mode == REPLAY) {
					if (numControllers == SINGLE) 
						control = semaphoreAndLockTracingAndReplay.getInstance(mode,numControllers,traceFileName);
					else {
						control = new semaphoreAndLockTracingAndReplay(mode,numControllers,traceFileName);
					}
				}
				else {
					System.out.println("Error: Only trace, replay, and reachability testing modes are supported for semaphores.");
					System.exit(1);
				}
			}
		}
	}

	public binarySemaphore(int initialPermits, propertyParameters.Mode mode) {
		// This constructor is called from monitorTraceAndReplay with mode=NONE so no 
		// controller/replay/trace is used
		super(initialPermits);
		if (initialPermits != 0 && initialPermits != 1)
			throw new IllegalArgumentException("initial value of binarySemaphore must be 0 or 1");
		this.mode=NONE;
	}

	public binarySemaphore(int initialPermits, boolean toolboxSemaphore) {
		// toolboxSemaphore is used if this semaphore is part of monitorSC implementation.
		// During RT, timestamp of thread awakened by V must be updated with timestamp
		//  of thread calling V. Toolbox uses countingSemaphore so this constructor is not used.
		// 
		super(initialPermits);
		if (initialPermits != 0 && initialPermits != 1)
			throw new IllegalArgumentException("initial value of binarySemaphore must be 0 or 1");
		this.toolboxSemaphore = true;
		// Note: mode will be NONE so no trace/test/replay/RT will be performed
	}	

	private msgTracingAndReplay instanceRT(propertyParameters.Mode mode, 
			propertyParameters.Controllers numControllers,
			propertyParameters.Strategy strategy, String traceFileName) { 
		//System.out.println("get controller");
		return msgTracingAndReplay.getInstance(mode,numControllers,strategy,traceFileName);
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


	public void down() {P();} 			// up - down
	public void decrement() {P();}	// increment - decrement
	public void waitS() {P();} 		// wait() is final in class Object and cannot be overridden
	public void P() {
		int callerForReceiveEvent = -1;
		if (mode == REPLAY)
			control.requestPermit(((innerThread)Thread.currentThread()).getID()); 
		else if (randomDelay == ON && mode == TRACE) {
			try {
				int r = (int)(Math.random()*delayAmount);
				Thread.sleep(r); // (int)(Math.random()*delayAmount));	// default delayAmount is 750
			} catch (InterruptedException e) {}
		}
		else if (mode == RT) {
			((innerThread)Thread.currentThread()).updateIntegerTS();

			String label = "'"+semaphoreName + ":" + "P" + "[S]";

			// label is used as the Program Counter for the symmetry reduction. 	
			if (SymmetryReduce == SYMMETRYREDUCTIONON) {
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				label = B.toString();
			}

			control.requestSendPermitMS(((innerThread)Thread.currentThread()).getID(),semaphoreName + ":" + "P",((innerThread)Thread.currentThread()).getVersionNumber(),getID());
			srEvent e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
					-1,semaphoreName + ":" + "P",-1,
					UNACCEPTED_ASYNCH_SEND, ((innerThread)Thread.currentThread()).getVectorTS(),label, false,nullOpenList,SEMAPHORE_CALL);
			control.trace(e); 

			control.msgReceived();


			// callerForReceiveEvent is not used here
			// Note: request made by caller/current thread; version number ignored since
			// really can't say what version number of "monitor thread" is here.
			// The send-->receive receive<--send is just a model for race analysis, not
			// how replay is implemented.
			if (detectDeadlock == DETECTIONON && mode == RT || mode == TRACE) {
				deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"calling P operation of "+semaphoreName);
			}
			callerForReceiveEvent = control.requestReceivePermitMS(((innerThread)Thread.currentThread()).getID(),getID(),semaphoreName + ":" + "P",getVersionNumber());  		
			if (detectDeadlock == DETECTIONON && mode == RT || mode == TRACE) {
				deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"running");
			}
		}
		queueObject o = new queueObject(); // each thread blocks on its own conditionVar object
		synchronized (o) { //lock conditionVar 
			synchronized (this) { // lock semaphore
				if (permits==1)  { // then no need to block thread
					if (mode == RT) {
						if (callerForReceiveEvent==-2) {
							control.monitorEnteredRT(((innerThread)Thread.currentThread()).getID(), semaphoreName + ":" + "P", ((innerThread)Thread.currentThread()).getVersionNumber());
						}
						updateIntegerTS();
						srEvent e = new srEvent(-2,getID(),-2,getVersionNumber(),
								semaphoreName + ":" + "P",-1,UNACCEPTED_RECEIVE,
								getVectorTS(),"unacceptedReceive",false,nullOpenList,UNACCEPTED_RECEIVE); 

						control.trace(e);

						String label = semaphoreName + ":" + "P" + "[R]";

						// update semaphore with callers timestamp: send ----> receive
						updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
						e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1,
								getAndIncVersionNumber(),semaphoreName + ":" + "P",-1,
								ASYNCH_RECEIVE, getVectorTS(),label,true,P_openList,SEMAPHORE_COMPLETION);	
						control.trace(e);

						// report caller and caller's version number
						control.msgReceived(((innerThread)Thread.currentThread()).getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1);

						// update caller with semaphore's timestamp: receive <------ send
						((innerThread)Thread.currentThread()).updateVectorTS(getVectorTS());
					}					

					if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE))
						deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
								+" completed " + semaphoreName + "." + "P()");

					if (waitingV.size() > 0) {  // signal thread blocked in V()
						queueObject oldest = (queueObject) waitingV.get(0);
						waitingV.remove(0);
						synchronized(oldest) {
							if (mode==RT) {
								updateIntegerTS();


								srEvent e = new srEvent(-2,getID(),-2,getVersionNumber(),
										semaphoreName + ":" + "P",-1,UNACCEPTED_RECEIVE,
										getVectorTS(),"unacceptedReceive",false,nullOpenList,UNACCEPTED_RECEIVE); 

								control.trace(e);
								// update semaphore with blocked thread's timestamp: send ----> receive
								updateVectorTS(((innerThread)oldest.blockedThread).getVectorTS());			

								String label = semaphoreName + ":" + "P" + "[R]";

								e = new srEvent(((innerThread)oldest.blockedThread).getID(),getID(),((innerThread)oldest.blockedThread).getVersionNumber()-1,
										getAndIncVersionNumber(),semaphoreName + ":" + "V",-1,
										ASYNCH_RECEIVE, getVectorTS(),label, true,V_openList,SEMAPHORE_COMPLETION);
								control.trace(e);

								// report caller and caller's version number
								control.msgReceived(((innerThread)oldest.blockedThread).getID(),((innerThread)oldest.blockedThread).getVersionNumber()-1);

								// update blockedThread with semaphore's timestamp: receive <------ send
								((innerThread)oldest.blockedThread).updateVectorTS(getVectorTS());
							}							
							oldest.notify();
							if (mode == TRACE) {
								control.trace(((innerThread)Thread.currentThread()).getID());
								control.trace(((innerThread)oldest.blockedThread).getID());
							}
							if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE)) {
								innerThread.decNumBlockedThreads();
							}
						}
					}
					else {
						if (mode == REPLAY)
							control.releasePermit();
						else if (mode == TRACE)
							control.trace(((innerThread)Thread.currentThread()).getID()); 						
						permits=0;
					}
					return;
				}
				/*deadlock detection */
				if (detectDeadlock == DETECTIONON && (mode == RT || mode == TRACE)) {
					innerThread.incNumBlockedThreads();
					deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"blocked on P operation of "+semaphoreName);
					deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
							+ " blocking on " + semaphoreName + "." + "P()");
				}
				o.blockedThread = Thread.currentThread();
				waitingP.add(o); // otherwise append blocked thread
			} // end synchronized (this) to avoid o.wait() while holding lock on this
			try {o.wait();} catch (InterruptedException ex) {}
			/* deadlock detection */
			if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE)) {
				deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"running");
				deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
						+" completed " + semaphoreName + "." + "P()");
			}
		} // end synchronized (conditionVar)
	} // end P()

	public void up() {V();}				// up - down
	public void increment() {V();}	// increment - decrement
	public void signalS() {V();}		// signalS - waitS
	public void V() {
		int callerForReceiveEvent = -1;
		if (mode == REPLAY)
			control.requestPermit(((innerThread)Thread.currentThread()).getID()); 
		else if (randomDelay == ON && mode == TRACE) {
			try {
				int r = (int)(Math.random()*delayAmount);
				Thread.sleep(r); // (int)(Math.random()*delayAmount));	// default delayAmount is 750
			} catch (InterruptedException e) {}
		}
		else if (mode == RT) {
			((innerThread)Thread.currentThread()).updateIntegerTS();
			control.requestSendPermitMS(((innerThread)Thread.currentThread()).getID(),semaphoreName + ":" + "V",((innerThread)Thread.currentThread()).getVersionNumber(),getID());

			String label = "'"+semaphoreName + ":" + "V" + "[S]";

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
					-1,semaphoreName + ":" + "V",-1,
					UNACCEPTED_ASYNCH_SEND, ((innerThread)Thread.currentThread()).getVectorTS(),label, false,nullOpenList,SEMAPHORE_CALL);
			control.trace(e); 

			control.msgReceived();


			// callerForReceiveEvent is not used here
			// Note: request made by caller/current thread; version number ignored since
			// really can't say what version number of "monitor thread" is here.
			// The send-->receive receive<--send is just a model for race analysis, not
			// how replay is implemented.
			if (detectDeadlock == DETECTIONON && mode == RT || mode == TRACE) {
				deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"calling V operation of "+semaphoreName);
			}
			callerForReceiveEvent = control.requestReceivePermitMS(((innerThread)Thread.currentThread()).getID(),getID(),semaphoreName + ":" + "V",getVersionNumber());  		
			if (detectDeadlock == DETECTIONON && mode == RT || mode == TRACE) {
				deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"running");
			}
		}

		queueObject o = new queueObject(); // each thread blocks on its own conditionVar object
		synchronized (o) { // lock conditionVar
			synchronized (this) { // lock semaphore
				if (permits==0) { // then no need to block thread
					if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE))
						deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
								+" completed " + semaphoreName + "." + "V()");				
					if (mode==RT) {
						if (callerForReceiveEvent==-2) {
							control.monitorEnteredRT(((innerThread)Thread.currentThread()).getID(), semaphoreName + ":" + "V", ((innerThread)Thread.currentThread()).getVersionNumber());
						}
						updateIntegerTS(); // tick semaphore's clock
						srEvent e = new srEvent(-2,getID(),-2,getVersionNumber(),
								semaphoreName + ":" + "V",-1,UNACCEPTED_RECEIVE,
								getVectorTS(),"unacceptedReceive",false,nullOpenList,UNACCEPTED_RECEIVE); 

						control.trace(e);

						// update semaphore with callers timestamp: send ----> receive
						updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());

						String label = semaphoreName + ":" + "V" + "[R]";

						e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1,
								getAndIncVersionNumber(),semaphoreName + ":" + "V",-1,
								ASYNCH_RECEIVE, getVectorTS(),label, true,V_openList,SEMAPHORE_COMPLETION);

						control.trace(e);

						// report caller and caller's version number
						control.msgReceived(((innerThread)Thread.currentThread()).getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1);

						// update caller with semaphore's timestamp: receive <------ send
						((innerThread)Thread.currentThread()).updateVectorTS(getVectorTS());
					}				

					if (waitingP.size() > 0) {  // signal thread blocked in P()
						queueObject oldest = (queueObject) waitingP.get(0);
						waitingP.remove(0);
						synchronized(oldest) {
							if (mode==RT) {
								updateIntegerTS();


								srEvent e = new srEvent(-2,getID(),-2,getVersionNumber(),
										semaphoreName + ":" + "P",-1,UNACCEPTED_RECEIVE,
										getVectorTS(),"unacceptedReceive",false,nullOpenList,UNACCEPTED_RECEIVE); 

								control.trace(e);
								// update semaphore with blocked thread's timestamp: send ----> receive
								updateVectorTS(((innerThread)oldest.blockedThread).getVectorTS());			

								String label = semaphoreName + ":" + "P" + "[R]";

								e = new srEvent(((innerThread)oldest.blockedThread).getID(),getID(),((innerThread)oldest.blockedThread).getVersionNumber()-1,
										getAndIncVersionNumber(),semaphoreName + ":" + "P",-1,
										ASYNCH_RECEIVE, getVectorTS(),label, true,P_openList,SEMAPHORE_COMPLETION);
								control.trace(e);

								// report caller and caller's version number
								control.msgReceived(((innerThread)oldest.blockedThread).getID(),((innerThread)oldest.blockedThread).getVersionNumber()-1);

								// update blockedThread with semaphore's timestamp: receive <------ send
								((innerThread)oldest.blockedThread).updateVectorTS(getVectorTS());
							}							
							oldest.notify();
							if (mode == TRACE) {
								control.trace(((innerThread)Thread.currentThread()).getID());
								control.trace(((innerThread)oldest.blockedThread).getID());
							}
							if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE)) {
								innerThread.decNumBlockedThreads();
							}
						}
					}
					else {
						permits=1;
						if (mode == REPLAY)
							control.releasePermit();
						else if (mode == TRACE)
							control.trace(((innerThread)Thread.currentThread()).getID()); 						
					}
					return;
				}
				/*deadlock detection */
				if (detectDeadlock == DETECTIONON && (mode == RT || mode == TRACE)) {
					innerThread.incNumBlockedThreads();
					deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"blocked on V operation of "+semaphoreName);
					deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
							+" blocking on " + semaphoreName + "." + "V()");
				}
				o.blockedThread = Thread.currentThread();
				waitingV.add(o); // otherwise append blocked thread
			} // end synchronized (this) to avoid o.wait() while holding lock on this
			try {o.wait();} catch (InterruptedException ex) {}
			/* deadlock detection */
			if (detectDeadlock == DETECTIONON  && (mode == RT || mode == TRACE)) {
				deadlockWatchdog.changeStatus(((innerThread)Thread.currentThread()),"running");
				deadlockWatchdog.deadlockTrace("Thread "+((innerThread)Thread.currentThread()).getThreadName()
						+ " completed " + semaphoreName + "." + "V()");
			}
		} // end synchronized (conditionVar)
	} // end V()

	protected boolean doP() {
		// Called by VP() operation; checks permit and unblocks a thread waiting
		// in V() if necessary. Returns true if P() should block; false otherwise.
		if (permits==1)  { // P() does not block
			if (waitingV.size() > 0) {  // signal thread blocked in V()
				queueObject oldest = (queueObject) waitingV.get(0);
				waitingV.remove(0);
				synchronized(oldest) {oldest.notify();}
			}
			else
				permits=0;
			return false;
		} // P() does not block
		else
			return true;	// P() does block
	}

	public int getSemaphoreID() { return semaphoreID;}

	public void VP(semaphore vSem) {
		// execute {vSem.V(); this.P();} without any intervening P() or V() operations on this or vSem.	
		// Note: tracing, replay, and reachability testing are not yet implemented for VP()

		// lock semaphores in ascending order of IDs to prevent circular deadlock (i.e. T1 holds
		// this's lock and waits for vSem's lock while T2 holds vSem's lock and waits for this's lock.)
		if (mode != NONE) {
			System.out.println();
			System.out.println("The testing and debugging functions are not yet supported ");
			System.out.println("  for the VP() operation. Use V(); P(); instead. ");
			System.exit(1);
		}
		semaphore first = this;
		semaphore second = vSem;
		if (this.getSemaphoreID() > vSem.getSemaphoreID()) { 
			first = vSem;
			second = this;
		}
		queueObject conditionVar = new queueObject(); // each thread blocks on its own conditionVar
		synchronized(conditionVar) {
			synchronized(first) { 
				synchronized(second) { 

					//vSem.V() must not block
					if (vSem instanceof binarySemaphore && vSem.permits == 1)
						throw new IllegalArgumentException("V() will block");

					// perform vSem.V()
					vSem.V(); 		// it's okay that we already hold vSem's lock
					// perform this.P()
					boolean blockingP = doP();
					if (!blockingP)
						return;
					waitingP.add(conditionVar); // append blocked thread
				} // end synchronized(second)
			} // end synchronized(first)
			try {conditionVar.wait();} catch (InterruptedException ex) {}
		} // end synchronized (conditionVar)
	} // end VP()

}// end  Semaphore

