package reachability;
import java.io.*;

class monitorSpecEvent implements Serializable {
	public monitorSpecEvent(int threadID, String eventName) {
	  this.threadID = threadID;
	  this.eventName = eventName;
	}

	public monitorSpecEvent() {}


	public void setThreadID(int threadID) {
		this.threadID = threadID;
	}

	public void seteventName(String eventName) {
		this.eventName = eventName;
	}

	public int getThreadID() {
		return threadID;
	}

	public String geteventName() {
		return eventName;
	}
	
	public String toString() {	
		return "("+	threadID + "," + eventName + ")";
	}

	private int threadID;
	private String eventName;
}
