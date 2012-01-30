package reachability;
import java.io.Serializable;

final class srThreadEvent implements eventTypeParameters, Serializable, Comparable {
	private int thisThread;
	private int thisThreadVersionNumber;
	private int otherThread;
	private int otherThreadVersionNumber;
	private String channelName;
	private int channelVersionNumber;
	private eventTypeParameters.eventType eType;
	protected vectorTimeStamp vectorTS = null;

	public srThreadEvent(int thisThread, int thisThreadVersionNumber,int otherThread, int otherThreadVersionNumber,
						String channelName, int channelVersionNumber, eventTypeParameters.eventType eType,
						vectorTimeStamp vectorTS) {
		this.thisThread = thisThread;
		this.thisThreadVersionNumber = thisThreadVersionNumber;
		this.otherThread = otherThread;
		this.otherThreadVersionNumber = otherThreadVersionNumber;
		this.channelName = channelName;
		this.channelVersionNumber = channelVersionNumber;
		this.eType = eType;
		this.vectorTS = vectorTS;
	}

	public void setThisThread(int thisThread) {
		this.thisThread = thisThread;
	}
	public void setOtherThread(int otherThread) {
		this.otherThread = otherThread;
	}
	public void setThisThreadVersionNumber(int thisThreadVersionNumber) {
		this.thisThreadVersionNumber = thisThreadVersionNumber;
	}
	public void setOtherThreadVersionNumber(int otherThreadVersionNumber) {
		this.otherThreadVersionNumber = otherThreadVersionNumber;
	}
	public void setChannelVersionNumber(int channelVersionNumber) {
		this.channelVersionNumber = channelVersionNumber;
	}
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	public void setEventType(eventTypeParameters.eventType eType) {
		this.eType = eType;
	}
	public int getThisThreadVersionNumber() {
		return thisThreadVersionNumber;
	}
	public int getThisThread() {
		return thisThread;
	}
	public int getOtherThread() {
		return otherThread;
	}
	public int getOtherThreadVersionNumber() {
		return otherThreadVersionNumber;
	}
	public int getChannelVersionNumber() {
		return channelVersionNumber;
	}
	public String getChannelName() {
		return channelName;
	}
	public eventTypeParameters.eventType getEventType() {
		return eType;
	}
	public vectorTimeStamp getVectorTS() {
		return vectorTS;
	}

	public int compareTo(Object o2) {
		srThreadEvent that = (srThreadEvent) o2;
		if (this.thisThreadVersionNumber < that.thisThreadVersionNumber)
			return -1;
		else if (thisThread == that.thisThread && otherThread == that.otherThread
				&& thisThreadVersionNumber == that.thisThreadVersionNumber
				&& otherThreadVersionNumber == that.otherThreadVersionNumber
				&& channelName.equals(that.channelName) 
				&& channelVersionNumber == that.channelVersionNumber
				&& eType.equals(that.eType))
			return 0;
		else
			return 1;
	}

	public boolean equals(Object o2) {
		System.out.flush();
		if (o2 instanceof srThreadEvent) {
			srThreadEvent e = (srThreadEvent) o2;
			return thisThread == e.thisThread && otherThread == e.otherThread
				&& thisThreadVersionNumber == e.thisThreadVersionNumber
				&& otherThreadVersionNumber == e.otherThreadVersionNumber
				&& channelName.equals(e.channelName) 
				&& channelVersionNumber == e.channelVersionNumber
				&& eType.equals(e.eType);
		}
		else
			return false;
	}  

	public String toString() {	
		return "("+thisThread+","+thisThreadVersionNumber+","+otherThread+","+otherThreadVersionNumber+","+channelName+
			","+channelVersionNumber+","+eType+")";
	}
		
}
