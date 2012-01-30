package reachability;
import java.util.*;
import java.io.*;

public class srEvent implements eventTypeParameters, Serializable, Comparable, Cloneable {
	protected int caller;
	protected int called;
	protected int callerVersionNumber;
	protected int calledVersionNumber;
	protected String channelName;
	protected int channelVersionNumber;
	protected eventTypeParameters.eventType eType;
	protected vectorTimeStamp vectorTS = null;
	protected String label;
	protected boolean isEntry;
   protected ArrayList openList;    // default init is null
   protected eventTypeParameters.eventType eType2;    // added
   protected boolean isRecollected = false;
   // used by SBRT
   protected int sourceStateSender = -1;
   protected int sourceStateReceiver = -1;
   protected int destinationStateSender = -1;
   protected int destinationStateReceiver = -1;
   protected stateVector sourceGlobalState = null;
   protected stateVector destinationGlobalState = null;  
   protected callerList inhibitedCallers = null;
   protected boolean destinationStateRevisited = false;
   
    // add by lei
    private ArrayList races;

   public srEvent() {} // used by srEventPool to create a pool of empty events


	public srEvent(int caller, int called, int callerVersionNumber,
						int calledVersionNumber, String channelName, int channelVersionNumber, 
						eventTypeParameters.eventType eType, vectorTimeStamp vectorTS) {
		this.caller = caller;
		this.called = called;
		this.callerVersionNumber = callerVersionNumber;
		this.calledVersionNumber = calledVersionNumber;
		this.channelName = channelName;
		this.channelVersionNumber = channelVersionNumber;
		this.eType = eType;
		this.vectorTS = vectorTS;
		this.label = "";
		this.isEntry = false;
		this.isRecollected = false;
		sourceStateSender = -1; sourceStateReceiver = -1;
		destinationStateSender = -1; destinationStateReceiver = -1;
		sourceGlobalState = null; destinationGlobalState = null;
		inhibitedCallers = null;
	}
	
	public srEvent(int caller, int called, int callerVersionNumber,
						int calledVersionNumber, String channelName, int channelVersionNumber, 
						eventTypeParameters.eventType eType, vectorTimeStamp vectorTS,	
						ArrayList openList, eventTypeParameters.eventType eType2) {
		this.caller = caller;
		this.called = called;
		this.callerVersionNumber = callerVersionNumber;
		this.calledVersionNumber = calledVersionNumber;
		this.channelName = channelName;
		this.channelVersionNumber = channelVersionNumber;
		this.eType = eType;
		this.vectorTS = vectorTS;
		this.label = "";
		this.isEntry = false;
		this.isRecollected = false;
		this.openList = openList;
		this.eType2 = eType2; // I think this is for RT, e.g., to identify that an
		                      // asynch_receive was really a semaphore_completion, etc
		sourceStateSender = -1; sourceStateReceiver = -1;
		destinationStateSender = -1; destinationStateReceiver = -1;
		sourceGlobalState = null; destinationGlobalState = null;
		inhibitedCallers = null;
	}
	public srEvent(int caller, int called, int callerVersionNumber,
						int calledVersionNumber, String channelName, int channelVersionNumber, 
						eventTypeParameters.eventType eType, vectorTimeStamp vectorTS,
						String label,
						ArrayList openList, eventTypeParameters.eventType eType2) {
		this.caller = caller;
		this.called = called;
		this.callerVersionNumber = callerVersionNumber;
		this.calledVersionNumber = calledVersionNumber;
		this.channelName = channelName;
		this.channelVersionNumber = channelVersionNumber;
		this.eType = eType;
		this.vectorTS = vectorTS;
		this.label = label;
		this.isEntry = false;
		this.isRecollected = false;
		this.openList = openList;
		this.eType2 = eType2;
		sourceStateSender = -1; sourceStateReceiver = -1;
		destinationStateSender = -1; destinationStateReceiver = -1;
		sourceGlobalState = null; destinationGlobalState = null; 
		inhibitedCallers = null;
	}
	
	public srEvent(int caller, int called, int callerVersionNumber,
						int calledVersionNumber, String channelName, int channelVersionNumber, 
						eventTypeParameters.eventType eType, vectorTimeStamp vectorTS,
						String label, boolean isEntry,
						ArrayList openList, eventTypeParameters.eventType eType2) {						
		this.caller = caller;
		this.called = called;
		this.callerVersionNumber = callerVersionNumber;
		this.calledVersionNumber = calledVersionNumber;
		this.channelName = channelName;
		this.channelVersionNumber = channelVersionNumber;
		this.eType = eType;
		this.vectorTS = vectorTS;
		this.label = label;
		this.isEntry = isEntry;
		this.isRecollected = false;
		this.openList = openList;
		this.eType2 = eType2;
		sourceStateSender = -1; sourceStateReceiver = -1;
		destinationStateSender = -1; destinationStateReceiver = -1;
		sourceGlobalState = null; destinationGlobalState = null; 
		inhibitedCallers = null;
	}

	public void setCaller(int caller) {
		this.caller = caller;
	}
	public void setCalled(int called) {
		this.called = called;
	}
	public void setCallerVersionNumber(int callerVersionNumber) {
		this.callerVersionNumber = callerVersionNumber;
	}
	public void setCalledVersionNumber(int calledVersionNumber) {
		this.calledVersionNumber = calledVersionNumber;
	}
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	public void setChannelVersionNumber(int channelVersionNumber) {
		this.channelVersionNumber = channelVersionNumber;
	}
	public void setEventType(eventTypeParameters.eventType eType) {
		this.eType = eType;
	}
	public void setLabel(String label) {
	   this.label = label;
	}
	public void setIsEntry(boolean isEntry) {
	   this.isEntry = isEntry;
	}
	public void setOpenList(ArrayList openList) {
	   this.openList = openList;
	}
	public void setEventType2(eventTypeParameters.eventType eType2) {
		this.eType2 = eType2;
	}
	public void setIsRecollected(boolean isRecollected) {
		this.isRecollected = isRecollected;
	}
	public final void setVectorTimestamp(vectorTimeStamp v) {
		this.vectorTS = v;
	}	
	public final void setSourceStateSender(int sourceStateSender) {
		this.sourceStateSender = sourceStateSender;
	}
	public final void setDestinationStateSender(int destinationStateSender) {
		this.destinationStateSender = destinationStateSender;
	}
	public final void setSourceStateReceiver(int sourceStateReceiver) {
		this.sourceStateReceiver = sourceStateReceiver;
	}
	public final void setDestinationStateReceiver(int destinationStateReceiver) {
		this.destinationStateReceiver = destinationStateReceiver;
	}
	public final void setSourceGlobalState(stateVector globalState) {
		this.sourceGlobalState = globalState;
	}
	public final void setDestinationGlobalState(stateVector globalState) {
		this.destinationGlobalState = globalState;
	}
	public final void setInhibitedCallers(ArrayList inhibitedCallers) {
	// called at most once per event 
		if (inhibitedCallers != null && inhibitedCallers.size()>0) {
			this.inhibitedCallers = new callerList();
			this.inhibitedCallers.setCallerList(inhibitedCallers);	
		}
	}
	public final void setDestinationStateRevisited() {
		this.destinationStateRevisited = true;
	}
	public int getCaller() {
		return caller;
	}
	public int getCalled() {
		return called;
	}
	public int getCallerVersionNumber() {
		return callerVersionNumber;
	}
	public int getCalledVersionNumber() {
		return calledVersionNumber;
	}
	public String getChannelName() {
		return channelName;
	}
	public int getChannelVersionNumber() {
		return channelVersionNumber;
	}
	public eventTypeParameters.eventType getEventType() {
		return eType;
	}
	public vectorTimeStamp getVectorTS() {
		return vectorTS;
	}
	public String getLabel() {
		return label;
	}
	public boolean getIsEntry() {
		return isEntry;
	}
	public ArrayList getOpenList() {
		return openList;
	}	
	public eventTypeParameters.eventType getEventType2() {
		return eType2;
	}
	public boolean getIsRecollected() {
		return isRecollected;
	}
	public final int getSourceStateSender() {
		return sourceStateSender;
	}
	public final int getDestinationStateSender() {
		return destinationStateSender;
	}
	public final int getSourceStateReceiver() {
		return sourceStateReceiver;
	}
	public final int getDestinationStateReceiver() {
		return destinationStateReceiver;
	}
	public final stateVector getSourceGlobalState() {
		return sourceGlobalState;
	}
	public final stateVector getDestinationGlobalState() {
		return destinationGlobalState;
	}
	public final callerList getInhibitedCallers() {
		return inhibitedCallers;
	}
	public final boolean getDestinationStateRevisited() {
		return destinationStateRevisited;
	}
	
    // added by lei
    public void setRaceSet (ArrayList races) {
	this.races = races;
    }
    public ArrayList getRaceSet () {
	return races;
    }



    public String toString() {
	   StringBuffer openListString = new StringBuffer();
	   boolean first = true;
	   openListString.append("[");
	   if (openList!=null) {
	   	Iterator i = openList.iterator();
	   	while (i.hasNext()) {
	   	  if (!first) {
	   	    openListString.append(",");
	   	  }
	   	  else 
  	   	    first = false;
	   	  openListString.append(i.next());
	   	}
	   }
  	   openListString.append("]");
  	   
  	  
	   StringBuffer raceString = new StringBuffer();
	   first = true;
	   if (races==null || races.size()==0 || eType == ASYNCH_SEND || eType == SYNCH_SEND)
		   raceString.append("[]");
 	   else {
	 	   raceString.append("\nRaceSet=[");
	   	Iterator i = races.iterator();
   		while (i.hasNext()) {
	   	  if (!first) {
	   	    raceString.append(",\n");
   		  }
	   	  else 
 		   	    first = false;
   		  raceString.append((srEvent) i.next());
	   	}
 	   	raceString.append("\n]end RaceSet");
  	   }
	   
	   if (label.equals("")) 
			return "("+	caller + "," + called + "," + callerVersionNumber
					+ "," + calledVersionNumber + "," + channelName
					+ "," + channelVersionNumber + "," + eType + "," + isEntry + 
					"," + openListString + "," + eType2 + "," + isRecollected + 
					"," + sourceStateSender + "," + destinationStateSender + 
					"," + sourceStateReceiver + "," + destinationStateReceiver + 					
					"," + sourceGlobalState + "," + destinationGlobalState + "," + inhibitedCallers + 
					"," + destinationStateRevisited + ")" + vectorTS + raceString;			
		else
			return "("+	caller + "," + called + "," + callerVersionNumber
					+ "," + calledVersionNumber + "," + channelName
					+ "," + channelVersionNumber + "," + eType + "," + isEntry + "," + label 
					+ "," + openListString + "," + eType2 + "," + isRecollected +
					"," + sourceStateSender + "," + destinationStateSender +
					"," + sourceStateReceiver + "," + destinationStateReceiver + 					
					"," + sourceGlobalState + "," + destinationGlobalState + "," + inhibitedCallers + 
					"," + destinationStateRevisited + ")" + vectorTS + raceString;
		
	}
	
    public String prettyPrint() {
      if (eType2.equals(SHAREDVARIABLE_CALL)) {
     	    return "";   	
    	}
    	else if (eType2.equals(SHAREDVARIABLE_COMPLETION)) {
     	    return ("Thread " + caller + " completes " + channelName + "\n");   	
    	}
    	else if (eType2.equals(SEMAPHORE_CALL)) {
    	    return ""; // return ("Thread " + caller + " calls " + channelName + "\n");
    	}
    	else if (eType2.equals(SEMAPHORE_COMPLETION)) {
     	    return ("Thread " + caller + " completes " + channelName + "\n");   	
    	}
    	else if (eType2.equals(MONITOR_CALL)) {
    	    return ""; // return ("Thread " + caller + " calls " + channelName + "\n");
    	}
    	else if (eType2.equals(MONITOR_ENTRY)) {
     	    return ("Thread " + caller + " enters " + channelName + "\n");   	
    	}
    	else if (eType2.equals(LOCK_CALL)) {
    	    return ""; // return ("Thread " + caller + " calls " + channelName + "\n");
    	}
    	else if (eType2.equals(LOCK_COMPLETION)) {
     	    return ("Thread " + caller + " completes " + channelName + "\n");   	
    	}
    	else if (eType2.equals(SYNCH_SEND)) {
    	    return ("Thread " + caller + " issues a send on synchronous channel " + channelName + "\n");
    	}
    	else if (eType2.equals(SYNCH_RECEIVE)) {
     	    return ("Thread " + called + " executes a receive on synchronous channel " + channelName + "\n");   	
    	}
    	else if (eType2.equals(ASYNCH_SEND)) {
    	    return ("Thread " + caller + " issues a send on asynchronous channel " + channelName + "\n");
    	}
    	else if (eType2.equals(ASYNCH_RECEIVE)) {
     	    return ("Thread " + called + " executes a receive on asynchronous channel " + channelName + "\n");   	
    	}
    	else if (eType2.equals(UNACCEPTED_ASYNCH_SEND)) {
     	    return ("Thread " + caller + " executes a send on asynchronous channel " + channelName + "\n");   	
    	}
    	else if (eType2.equals(UNACCEPTED_SYNCH_SEND)) {
     	    return ("Thread " + caller + " executes a send on synchronous channel " + channelName + "\n");   	
    	}
    	else
			return ""; // "("+	caller + "," + called + "," + channelName	+ "," + eType + "," + isEntry + "," +  eType2  + ")";
		
	}

	public int compareTo(Object o2) {
		srEvent that = (srEvent) o2;
		if (this.channelVersionNumber < that.channelVersionNumber)
			return -1;
		else if (caller == that.caller && called == that.called
				&& callerVersionNumber == that.callerVersionNumber
				&& calledVersionNumber == that.calledVersionNumber
				&& channelName.equals(that.channelName) 
				&& channelVersionNumber == that.channelVersionNumber
				&& eType.equals(that.eType)) {
			return 0;
		}
		else
			return 1;
	} 


	public boolean equals(Object o2) {
		if (o2 instanceof srEvent) {
			srEvent e = (srEvent) o2;
			boolean result = caller == e.caller && called == e.called
				&& callerVersionNumber == e.callerVersionNumber
				&& calledVersionNumber == e.calledVersionNumber
				&& channelName.equals(e.channelName) 
				&& channelVersionNumber == e.channelVersionNumber
				&& eType.equals(e.eType);
			return result;
		}
		else {
			return false;
		}
	} 
	
	public Object clone () {
		try {
			srEvent e = (srEvent)super.clone();			
			return e;
		} catch (CloneNotSupportedException e) {}
		return null;
	}
}

