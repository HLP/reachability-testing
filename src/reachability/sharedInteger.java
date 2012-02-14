package reachability;
// derived from countingSemaphore RT code.
// 1. use sharedVariableIDGenerator instead of semaphore generator
// 2. version numbers can be computed: start with 0, inc on write

import java.util.*;


public final class sharedInteger implements propertyParameters, monitorEventTypeParameters, eventTypeParameters {
	// a shared variable wrapper for ints

	int value;

	private propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	private propertyParameters.Controllers numControllers = SINGLE;
	private propertyParameters.RandomDelay randomDelay = OFF;
	protected propertyParameters.Strategy strategy = OBJECT;
	protected propertyParameters.GenerateLTS generateLTS = LTSOFF;

	private final int	delayAmount = 750;

	private ArrayList Write_openList; // openList with "Write" and "Read" in it.
	private ArrayList Read_openList;  // openList with "Write" and "Read" in it
	private ArrayList nullOpenList = new ArrayList();

	private String sharedVariableName = null;
	private int ID;
	private vectorTimeStamp vectorTS;
	private int versionNumber = 1;
	public int getVersionNumber() { return versionNumber; }
	public int getAndIncVersionNumber() {
		return versionNumber++;
	}

	private static monitorTracingAndReplay m = null;
	//private static msgTracingAndReplay mMsg = null;

	private static final Object classLock =
			monitorTracingAndReplay.class;

	//private monitorTracingAndReplay control = null;
	// now msgBased and monitor controllers implement Control
	private Control control = null;

	public int getID() {return ID;}
	public String getName() {return sharedVariableName;}

	private int generateIDFromUserNameRT(String userName) {
		//	System.out.println("User Name: " + userName); 
		//return (sharedVarIDGenerator.getInstance().getIDFromUserName(userName));
		// for RT, monitors get IDs likes threads, since monitors have a slot in the timestamp
		boolean isThread = false;
		return (ThreadIDGenerator.getInstance().getIDFromUserName(userName,isThread));
	}


	private int generateIDRT() { 
		String stringID = null;	// internal name of thread class
		if (Thread.currentThread() instanceof innerThread)
			// get stringID of parent innerThread
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
		//String nextName = (semaphoreIDGenerator.getInstance().getID(stringID));
		//String nextName = (ThreadIDGenerator.getInstance().getID(stringID));
		boolean isThread = false;
		int myID = (ThreadIDGenerator.getInstance().getID(stringID,isThread));
		return myID;
	}

	private String generateIDFromUserName(String userName) {
		//	System.out.println("User Name: " + userName);
		return (sharedVariableIDGenerator.getInstance().getIDFromUserName(userName));
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


		String nextName = (sharedVariableIDGenerator.getInstance().getID(stringID));
		return nextName;
	}

	public sharedInteger(int initialValue, String sharedVariableName) {
		this.value = initialValue; 
		this.sharedVariableName = sharedVariableName;
		mode = (propertyReader.getInstance().getModeProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		generateLTS = (propertyReader.getInstance().getGenerateLTSProperty());


		if (!(mode==NONE)) {
			if (mode == RT) {
				this.ID = 	generateIDFromUserNameRT(sharedVariableName);
				String traceFileName = ThreadIDGenerator.getInstance().getName(ID);
				initVectorTS();
				OpenEvent Read = new OpenEvent("Read",-1);
				OpenEvent Write = new OpenEvent("Write",-1);
				Write_openList = new ArrayList(); Write_openList.add(Write);Write_openList.add(Read);
				Read_openList = new ArrayList(); Read_openList.add(Write);  Read_openList.add(Read);		
				if (numControllers==SINGLE)
					traceFileName = "channel";
				//if (mode == TRACE || mode == REPLAY || mode == TEST || mode == SPECTEST || mode == RT) {
				if (numControllers == SINGLE) 
					control = instanceRT(mode,numControllers,strategy,traceFileName);
				else {
					System.out.println("Error: the number of controllers must be one for reachability testing.");
					System.exit(1);
					//control = new msgTracingAndReplay(mode,numControllers,strategy,traceFileName);
					//control.start();
				}
				//}
			}
			else {
				/* not currently implemented
				String traceFileName = generateIDFromUserName(sharedVariableName);
				if (numControllers==SINGLE)
					traceFileName = "sharedVariables";
				if (mode == TRACE || mode == REPLAY || mode == TEST || mode == SPECTEST) {
					if (numControllers == SINGLE) 
						control = instance(mode,numControllers,traceFileName);
					else {
						control = new monitorTracingAndReplay(mode,numControllers,traceFileName);
					}
				}			
				 */
			}
		}
	}

	sharedInteger(int initialValue, propertyParameters.Mode mode) {
		// This constructor is called from monitorTraceAndReplay with mode=NONE so no 
		// controller/replay/trace is used
		this.value = initialValue;
		this.mode=NONE;
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
		//System.out.println("get controller");
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

	// Returns the integer clock value associated with this thread.
	public int getIntegerTS() { 
		return vectorTS.getIntegerTS(getID());
	}

	// Increments the integer clock associated with this thread.
	public void updateIntegerTS() { 
		//System.out.println("TDThread: updating integer timestamp for " + getID());
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

	public int Read() {
		int callerForReceiveEvent = -1;
		//replyForRequestPermit ret = null;
		int ret = -1;

		if (mode == RT) {
			((innerThread)Thread.currentThread()).updateIntegerTS();

			String label = "'"+sharedVariableName + ":" + "Read" + value + "[S]";
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" requesting sendPermitMS");
			control.requestSendPermitMS(((innerThread)Thread.currentThread()).getID(),sharedVariableName + ":" + "Read",((innerThread)Thread.currentThread()).getVersionNumber(),getID());
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" received sendPermitMS");
			srEvent e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
					getVersionNumber(),sharedVariableName + ":" + "Read",-1,
					UNACCEPTED_ASYNCH_SEND, ((innerThread)Thread.currentThread()).getVectorTS(),label, false,nullOpenList,SHAREDVARIABLE_CALL);
			control.trace(e); 

			control.msgReceived();

			// callerForReceiveEvent is not used here
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" requesting receivePermitMS");
			// Note: request made by caller/current thread; 
			// Note: version number could be computed if needed
			// The send-->receive receive<--send is just a model for race analysis, not
			// how replay is implemented.
			ret = control.requestReceivePermitMS(((innerThread)Thread.currentThread()).getID(),getID(),sharedVariableName + ":" + "Read",getVersionNumber());  		
			//callerForReceiveEvent = ret.getCallerForReceiveEvent();
			callerForReceiveEvent = ret;
			//isChanged = ret.getIsChanged();
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" received receivePermitMS");
		}
		synchronized (this) { // lock variable
			if (mode == RT) {
				if (callerForReceiveEvent==-2) {
					//System.out.println("send ack");
					control.monitorEnteredRT(((innerThread)Thread.currentThread()).getID(), sharedVariableName + ":" + "Read", ((innerThread)Thread.currentThread()).getVersionNumber());
				}
				updateIntegerTS();
				srEvent e = new srEvent(-2,getID(),-2,getVersionNumber(),
						sharedVariableName + ":" + "Read",-1,UNACCEPTED_RECEIVE,
						getVectorTS(),"unacceptedReceive",false,nullOpenList,UNACCEPTED_RECEIVE); 

				//System.out.println(((innerThread)Thread.currentThread()).getID()+"started Read");

				control.trace(e);

				String label = sharedVariableName + ":" + "Read" + value + "[R]";
				// update variable with callers timestamp: send ----> receive
				updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
				e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1,
						getAndIncVersionNumber(),sharedVariableName + ":" + "Read",-1,
						ASYNCH_RECEIVE, getVectorTS(),label, true,Read_openList,SHAREDVARIABLE_COMPLETION);
				control.trace(e);
				// report caller and caller's version number
				control.msgReceived(((innerThread)Thread.currentThread()).getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1);

				// update caller with variable's timestamp: receive <------ send
				((innerThread)Thread.currentThread()).updateVectorTS(getVectorTS());
			}				
			// Yenjung: dump value and TS
			//dumpInfo();
			return value;
		} // end synchronized (this) 
	} // end P


	public void Write(int value) {
		int callerForReceiveEvent = -1;
		//replyForRequestPermit ret = null;
		int ret = -1;

		if (mode == RT) {
			((innerThread)Thread.currentThread()).updateIntegerTS();
			control.requestSendPermitMS(((innerThread)Thread.currentThread()).getID(),sharedVariableName + ":" + "Write",((innerThread)Thread.currentThread()).getVersionNumber(),getID());

			String label = "'"+sharedVariableName + ":" + "Write" + value + "[S]";

			srEvent e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
					getVersionNumber(),sharedVariableName + ":" + "Write",-1,
					UNACCEPTED_ASYNCH_SEND, ((innerThread)Thread.currentThread()).getVectorTS(),label, false,nullOpenList,SHAREDVARIABLE_CALL);
			control.trace(e); 

			control.msgReceived();


			// callerForReceiveEvent is not used here
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" requesting receivePermitX");
			// Note: request made by caller/current thread; 

			// Note" version number could be computed if needed.

			// The send-->receive receive<--send is just a model for race analysis, not
			// how replay is implemented.
			ret = control.requestReceivePermitMS(((innerThread)Thread.currentThread()).getID(),getID(),sharedVariableName + ":" + "Write",getVersionNumber());  		
			//callerForReceiveEvent = ret.getCallerForReceiveEvent();
			callerForReceiveEvent = ret;
			//isChanged = ret.getIsChanged();			
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" got receivePermitX");
		}
		synchronized (this) { // lock sharedvar

			if (mode==RT) {
				if (callerForReceiveEvent==-2) {
					//System.out.println("send ack");
					control.monitorEnteredRT(((innerThread)Thread.currentThread()).getID(), sharedVariableName + ":" + "Write", ((innerThread)Thread.currentThread()).getVersionNumber());
				}
				updateIntegerTS(); // tick variables's clock
				srEvent e = new srEvent(-2,getID(),-2,getVersionNumber(),
						sharedVariableName + ":" + "Write",-1,UNACCEPTED_RECEIVE,
						getVectorTS(),"unacceptedReceive",false,nullOpenList,UNACCEPTED_RECEIVE); 

				//System.out.println(((innerThread)Thread.currentThread()).getID()+"completed Write");

				control.trace(e);

				// update variable with callers timestamp: send ----> receive
				updateVectorTS(((innerThread)Thread.currentThread()).getVectorTS());

				String label = sharedVariableName + ":" + "Write" + value + "[R]";

				//System.out.println("Write:"+sharedVariableName+":"+value);
				e = new srEvent(((innerThread)Thread.currentThread()).getID(),getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1,
						getAndIncVersionNumber(),sharedVariableName + ":" + "Write",-1,
						ASYNCH_RECEIVE, getVectorTS(),label, true,Write_openList,SHAREDVARIABLE_COMPLETION);

				control.trace(e);

				// report caller and caller's version number
				control.msgReceived(((innerThread)Thread.currentThread()).getID(),((innerThread)Thread.currentThread()).getVersionNumber()-1);

				// update caller with variables's timestamp: receive <------ send
				((innerThread)Thread.currentThread()).updateVectorTS(getVectorTS());
			}
			this.value = value;
			// Yenjung: dump value and TS
			dumpInfo();
		}	
	} // end
	
	// Yenjung
	private void dumpInfo() {
		System.out.println("<ID=" + getID() + ", value=" + value + ", TS="+ vectorTS + ">");
		//PartialOrderDumper.getInstance().append("<ID=" + getID() + ", value=" + value + ", TS="+ vectorTS + ">\n");
		PartialOrderDumper.getInstance().append(getID() + " " + value + " ");
		PartialOrderDumper.getInstance().append(vectorTS.getIntegerTS(1) + " " + vectorTS.getIntegerTS(2) + "\n");
	}
}// end  sharedInteger

