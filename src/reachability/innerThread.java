package reachability;
import java.util.*;

final class innerThread extends Thread implements propertyParameters {
	int result = 0; TDThread myT = null;
	protected Runnable runnable = null;

	private static int numRunningThreads = 0;
	private static int numBlockedThreads = 0;
	private static final Object thisClassLock = innerThread.class;
	static void incNumRunningThreads() {synchronized(thisClassLock){numRunningThreads++;}}
	static void decNumRunningThreads() {synchronized(thisClassLock){numRunningThreads--;}}
	static void incNumBlockedThreads() {synchronized(thisClassLock){numBlockedThreads++;}}
	static void decNumBlockedThreads() {synchronized(thisClassLock){numBlockedThreads--;}}
	static int getNumRunningThreads() {synchronized(thisClassLock){return numRunningThreads;}}
	static int getNumBlockedThreads() {synchronized(thisClassLock){return numBlockedThreads;}}
	static void resetNumBlockedRunningThreads() {synchronized(thisClassLock){numRunningThreads=0;numBlockedThreads=0;}}
	static void setNumBlockedThreads(int value) {synchronized(thisClassLock){numBlockedThreads=value;}}



	private String threadName = null;	// internal name of thread class
	private int ID;
	private int versionNumber = 1;
	private vectorTimeStamp vectorTS;

	public int getID() {return ID;}
	public int getVersionNumber() { return versionNumber; }
	public int getAndIncVersionNumber() {
		return versionNumber++;
	}

	protected propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	protected propertyParameters.Controllers numControllers = SINGLE;
	protected propertyParameters.Strategy strategy = OBJECT;
	protected propertyParameters.DetectDeadlock detectDeadlock = DETECTIONOFF;

	private final int	delayAmount = 750;
	private propertyParameters.RandomDelay randomDelay = OFF;


	protected String traceFileName = null; // new String("program");
	private static threadBasedMsgTracingAndReplay m = null;
	private static final Object classLock =
			threadBasedMsgTracingAndReplay.class;
	protected Controller control = null;

	protected ArrayList state = new ArrayList();


	private threadBasedMsgTracingAndReplay instance(propertyParameters.Mode mode, 
			propertyParameters.Controllers numControllers,
			propertyParameters.Strategy strategy, String traceFileName) { 
		synchronized(classLock) {
			if (m == null) {
				m = new threadBasedMsgTracingAndReplay(mode,numControllers,strategy, traceFileName);
				m.start();
			}
			return m;
		}
	}

	innerThread() {
		// assumes all threads inherit from TDThread
		mode = (propertyReader.getInstance().getModeProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		if (detectDeadlock==DETECTIONON && mode==NONE) {
			System.out.println("Error: -DdeadlockDetection=on requires -Dmode=trace or -Dmode=rt");
			System.exit(1);
		}
		if (!(mode==NONE)) {
			ID = generateID();
			threadName = ThreadIDGenerator.getInstance().getName(ID);
			initVectorTS();
			constructController(); 
		}
	}

	innerThread(String userName) {
		super(userName);
		threadName = userName;
		// assumes all threads inherit from TDThread
		mode = (propertyReader.getInstance().getModeProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		if (detectDeadlock==DETECTIONON && mode==NONE) {
			System.out.println("Error: -DdeadlockDetection=on requires -Dmode=trace or -Dmode=rt");
			System.exit(1);
		}
		if (!(mode==NONE)) {
			ID = 	generateIDFromUserName(userName);
			threadName = userName;
			initVectorTS();
			constructController();  
		}
	}

	public innerThread(Runnable runnable) {
		super(runnable);
		this.runnable = runnable;
		mode = (propertyReader.getInstance().getModeProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		if (detectDeadlock==DETECTIONON && mode==NONE) {
			System.out.println("Error: -DdeadlockDetection=on requires -Dmode=trace or -Dmode=rt");
			System.exit(1);
		}
		if (!(mode==NONE)) {
			ID = generateID();
			threadName = ThreadIDGenerator.getInstance().getName(ID);
			initVectorTS();
			constructController();
		}
	}

	public innerThread(ThreadGroup group, Runnable runnable) {
		super(group, runnable);
		this.runnable = runnable;
		mode = (propertyReader.getInstance().getModeProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		if (detectDeadlock==DETECTIONON && mode==NONE) {
			System.out.println("Error: -DdeadlockDetection=on requires -Dmode=trace or -Dmode=rt");
			System.exit(1);
		}
		if (!(mode==NONE)) {
			ID = generateID();
			threadName = ThreadIDGenerator.getInstance().getName(ID);
			initVectorTS();
			constructController();
		}
	}

	public innerThread(ThreadGroup group, String userName) {
		super(group, userName);
		threadName = userName;
		mode = (propertyReader.getInstance().getModeProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		if (detectDeadlock==DETECTIONON && mode==NONE) {
			System.out.println("Error: -DdeadlockDetection=on requires -Dmode=trace or -Dmode=rt");
			System.exit(1);
		}
		if (!(mode==NONE)) {
			ID = 	generateIDFromUserName(userName);
			threadName = userName;
			initVectorTS();
			constructController();
		}
	}


	public innerThread(Runnable runnable, String userName) {
		super(runnable, userName);
		threadName = userName;
		this.runnable = runnable;
		mode = (propertyReader.getInstance().getModeProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		if (detectDeadlock==DETECTIONON && mode==NONE) {
			System.out.println("Error: -DdeadlockDetection=on requires -Dmode=trace or -Dmode=rt");
			System.exit(1);
		}
		if (!(mode==NONE)) {
			ID = 	generateIDFromUserName(userName);
			threadName = userName;
			initVectorTS();
			constructController();
		}
	}

	public innerThread(ThreadGroup group, Runnable runnable, String userName) {
		super(group, runnable, userName);
		threadName = userName;
		this.runnable = runnable;
		mode = (propertyReader.getInstance().getModeProperty());
		detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		if (detectDeadlock==DETECTIONON && mode==NONE) {
			System.out.println("Error: -DdeadlockDetection=on requires -Dmode=trace or -Dmode=rt");
			System.exit(1);
		}
		if (!(mode==NONE)) {
			ID = 	generateIDFromUserName(userName);
			threadName = userName;
			initVectorTS();
			constructController();
		}
	}


	public String getThreadName() { return threadName; }

	private int generateIDFromUserName(String userName) {
		boolean isThread = true;
		return ThreadIDGenerator.getInstance().getIDFromUserName(userName,isThread);
	}

	private int generateID() {
		if (Thread.currentThread() instanceof innerThread)
			// get stringID of parent TDThread
			threadName = ((innerThread)Thread.currentThread()).getThreadName();
		else
			// parent is main thread
			threadName = "";
		String thisClassName; 
		Class c = this.getClass();
		thisClassName = c.getName(); // class T in "class T extends TDThread"
		if ((threadName.indexOf('_')) >= 0) 
			// parent of currentThread is a TDThread so use its name as prefix
			// e.g., "main_T" is name of thread executing "new TT()"
			threadName = threadName+"_"+thisClassName; // e.g., "main_T_TT
		else
			// parent of currentThread is not a TDThread, so it's the main thread	
			threadName = "main_" + thisClassName;
		boolean isThread = true;				
		int myID = ThreadIDGenerator.getInstance().getID(threadName,isThread);
		return myID;
	}

	private void constructController() {
		numControllers = (propertyReader.getInstance().getControllersProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		if (numControllers == MULTIPLE)
			traceFileName = threadName;
		else
			traceFileName = "channel";
		if (strategy == THREAD) {
			if (mode == TRACE || mode == REPLAY || mode == TEST || mode == RT) {
				if (mode == RT || numControllers == SINGLE) // RT always uses a single controller
					if (mode == RT)
						control = msgTracingAndReplay.getInstance(mode,numControllers,strategy,traceFileName);
					else
						control = instance(mode,numControllers,strategy,traceFileName);
				else {
					control = new threadBasedMsgTracingAndReplay(mode,numControllers,strategy,traceFileName);
					control.start();
				}
			}
		}
	}

	//threadBasedMsgTracingAndReplay getController() {
	Controller getController() {
		if (mode == RT) 
			return (msgTracingAndReplay)control;
		else
			return (threadBasedMsgTracingAndReplay)control;
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
		try {
			((innerThread)Thread.currentThread()).updateIntegerTS();
			updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
		}
		// if current thread is main thread and is not a TDThread, then main has no VTS
		catch (Exception e) {} 
	}


	public void run() {
		try {
			if (randomDelay == ON && mode == TRACE) {
				try {
					int r = (int)(Math.random()*delayAmount);
					Thread.sleep(r);	// default delayAmount is 750
				} catch (InterruptedException e) {}
			}
			if (detectDeadlock == DETECTIONON && (mode == TRACE || mode == RT)) {
				incNumRunningThreads();
				deadlockWatchdog.startInstance();
				deadlockWatchdog.changeStatus(this,"running");
			}
			if (runnable == null) /*result =*/ myT.run();
			else /*result =*/ runnable.run();
		}
		finally {
			if (detectDeadlock == DETECTIONON && (mode == TRACE || mode == RT)) {
				decNumRunningThreads();
				deadlockWatchdog.removeThread(this);
			} 
		}
	}
	void startT(TDThread t) {	
		try { myT = t;	this.start(); } 
		catch (Exception e) {System.out.println("Exception in inner::startT");}
	}
	void startRunnable(TDThread t) {
		try {myT = t;	this.start();} 
		catch (Exception e) {System.out.println("Exception in inner::startRunnable");}
	}
	int getResult() {return result;}


	final String getThreadState() {
		StringBuffer stateString = new StringBuffer();
		ListIterator p = (ListIterator) state.listIterator();
		while (p.hasNext()) {
			stateString.append(p.next().toString());
			if (p.hasNext())
				stateString.append("_");
		}
		return stateString.toString();
	}

	final  void addToState(Object o) {
		state.add(o);
	}
}
