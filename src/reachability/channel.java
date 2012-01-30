package reachability;
import java.io.*;

public abstract class channel implements eventTypeParameters, propertyParameters, Serializable {

   public abstract void send(Object m);
   public abstract void send();
   public abstract Object receive();


	protected propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none


	protected propertyParameters.Controllers numControllers = SINGLE;

	protected propertyParameters.Strategy strategy = OBJECT;
	
	protected propertyParameters.GenerateLTS generateLTS = LTSOFF;
	
	protected propertyParameters.SymmetryReduction SymmetryReduce = SYMMETRYREDUCTIONOFF;
	
	protected propertyParameters.RandomDelay randomDelay = OFF;



	protected String channelName = null;
	protected String traceFileName = null; // new String("program");
	private int versionNumber = 1;
	public int getVersionNumber() { return versionNumber; }
	public int getAndIncVersionNumber() {
		return versionNumber++;
	}
	public String getChannelName() { return channelName;}

	private static msgTracingAndReplay m = null;

	private static final Object classLock =
                            msgTracingAndReplay.class;

	protected Controller control = null;

	protected final class debugMessage implements Cloneable {
		private int caller;
		private Object msg;
		private int versionNumber;
		private vectorTimeStamp vectorTS;
		private String label;
		private boolean isOld = false;  // true if this is message sent by an old send during RT

     public Object clone() {
        try {
         return super.clone();
        } catch (CloneNotSupportedException e) {
            // this should not happen since we are Cloneable
            return null;
          }
      }

		public Object getMsg() {
			return msg;
		}
		public void setMsg(Object msg) {
			this.msg = msg;
		}
		public int getCaller() {
			return caller;
		}
		public vectorTimeStamp getVectorTS() {
			return vectorTS;
		}
		public void setCaller(int caller) {
			this.caller = caller;
		}
		public int getVersionNumber() {
			return versionNumber;
		}
		public void setVersionNumber(int versionNumber) {
			this.versionNumber = versionNumber;
		}
		public void setVectorTS(vectorTimeStamp v) {
			this.vectorTS = v;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public String getLabel() {
			return label;
		}
		public boolean getIsOld() {
			return isOld;
		}
		public void setIsOld(boolean isOld) {
			this.isOld = isOld;
		}
	}



	private String generateIDFromUserName(String userName) {
		return (channelIDGenerator.getInstance().getIDFromUserName(userName));
	}

	private String stringID = null;	// internal name of thread class
	private String generateID() {
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


		String nextName = (channelIDGenerator.getInstance().getID(stringID));
		return nextName;
	}



	public channel(String channelName) {
		this.channelName = channelName;
		mode = (propertyReader.getInstance().getModeProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		generateLTS = (propertyReader.getInstance().getGenerateLTSProperty());
		SymmetryReduce = (propertyReader.getInstance().getSymmetryReductionProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());

		if (!(mode==NONE)) {
			this.traceFileName = generateIDFromUserName(channelName); // check for uniqueness and gen channelID file
			if (numControllers == SINGLE) {
				this.traceFileName = "channel";
			}
		 	if (strategy == OBJECT) { //  || this instanceof mailbox) {
				if (mode == TRACE || mode == REPLAY || mode == TEST || mode == RT) {
					if (mode == RT || numControllers == SINGLE) 
						// RT always uses a single controller
						control = instance(mode,numControllers,strategy,traceFileName);
					else {
						control = new msgTracingAndReplay(mode,numControllers,strategy,traceFileName);
						control.start();
					}
				}
		 	}
		}
		if (mode == SPECTEST) {
			System.out.println();
			System.out.println("Error: Mode spectest is not yet supported for message channels.");
			System.exit(1);
		}
	}


	public channel() {
		mode = (propertyReader.getInstance().getModeProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		generateLTS = (propertyReader.getInstance().getGenerateLTSProperty());
		SymmetryReduce = (propertyReader.getInstance().getSymmetryReductionProperty());
		randomDelay = (propertyReader.getInstance().getRandomDelayProperty());
	
		if (!(mode==NONE)) {
			channelName = generateID();
			if (numControllers == MULTIPLE)
				this.traceFileName = channelName;
			else
				this.traceFileName = "channel";
			 if (strategy == OBJECT) { //  || this instanceof mailbox) {
				if (mode == TRACE || mode == REPLAY || mode == TEST || mode == RT) {
					if (mode == RT || numControllers == SINGLE) 
						// RT always uses a single controller
						control = instance(mode,numControllers,strategy,traceFileName);
					else {
						control = new msgTracingAndReplay(mode,numControllers,strategy,traceFileName);
						control.start();
					}
				}
		 	}
		}
		if (mode == SPECTEST) {
			System.out.println();
			System.out.println("Error: Mode spectest is not yet supported for message channels.");
			System.exit(1);
		}
	}

   public static void resetController() {
   	m = null;
   	msgTracingAndReplay.resetController();
   }
   
	private msgTracingAndReplay instance(propertyParameters.Mode mode, 
		propertyParameters.Controllers numControllers,
		propertyParameters.Strategy strategy, String traceFileName) { 
		synchronized(classLock) {
      	if (m == null) {
      	   m = msgTracingAndReplay.getInstance(mode,numControllers,strategy,traceFileName);
			}
      	return m;
    	}
  }

	Controller getController() {return control;}
	// called from selectiveWait, which implies "thread" which means
   // this will be a threadBasedMsgTracingAndReplay instance 
   // It is cast in selectiveWait
   
  	static msgTracingAndReplay getObjectBasedController() {
  		propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	   propertyParameters.Controllers numControllers = SINGLE;
	   propertyParameters.Strategy strategy = OBJECT;
  		mode = (propertyReader.getInstance().getModeProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		return msgTracingAndReplay.getInstance(mode,numControllers,strategy,"channel");
  	}

}

