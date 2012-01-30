package reachability;
import java.io.*;
	
class monitorEvent implements monitorEventTypeParameters, Serializable {


	protected monitorEventTypeParameters.eventType eType;
	protected int threadID;
	protected int callerID;
	protected String methodName;
	protected String conditionName;
	protected vectorTimeStamp vectorTS = null;

	public monitorEvent(monitorEventTypeParameters.eventType eType, int threadID, String methodName, 
		String conditionName) {
		this.eType = eType;
		this.threadID = threadID;
		this.methodName = methodName;
		this.conditionName = conditionName;
	}
	
	public monitorEvent(monitorEventTypeParameters.eventType eType, int callerID, int calledID, String methodName, 
		String conditionName, vectorTimeStamp vectorTS) {
		this.eType = eType;
		this.threadID = calledID;
		this.callerID = callerID;
		this.methodName = methodName;
		this.conditionName = conditionName;
      this.vectorTS = vectorTS;
	}

	public void setEventType(monitorEventTypeParameters.eventType eType) {
		this.eType = eType;
	}

	public void setThreadID(int threadID) {
		this.threadID = threadID;
	}
	
	public void setCallerID(int callerID) {
		this.callerID = callerID;
	}

	public void setmethodName(String monitorName) {
		this.methodName = monitorName;
	}

	public void setconditionName(String conditionName) {
		this.conditionName = conditionName;
	}

	public monitorEventTypeParameters.eventType getEventType() {
		return eType;
	}

	public int getThreadID() {
		return threadID;
	}
	
	public int getCallerID() {
		return callerID;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getConditionName() {
		return conditionName;
	}
	
	public vectorTimeStamp getVectorTS() {
		return vectorTS;
	}


	public String toString() {	
		return "("+	eType + "," + threadID + "," + methodName
					+ "," + conditionName + ")";
	}

}
