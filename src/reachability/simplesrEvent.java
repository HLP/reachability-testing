package reachability;
import java.io.*;

final class simplesrEvent implements Serializable {
	private int caller;
	private int called;
	private eventTypeParameters.eventType eType;
	
	public simplesrEvent(int caller, int called) {
		this.caller = caller;
		this.called = called;
	}
	
	public simplesrEvent(int caller, int called, eventTypeParameters.eventType eType) {
		this.caller = caller;
		this.called = called;
		this.eType = eType;
	}

	public void setCaller(int caller) {
		this.caller = caller;
	}
	public void setCalled(int called) {
		this.called = called;
	}
	public void setEventType(eventTypeParameters.eventType eType) {
		this.eType = eType;
	}
	public int getCaller() {
		return caller;
	}
	public int getCalled() {
		return called;
	}
	public eventTypeParameters.eventType getEventType() {
		return eType;
	}
	public String toString() {
	   return "("+caller+","+"called"+","+eType+")";
	}

}	
