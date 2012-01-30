package reachability;
public interface Control { 

	public void trace(monitorEvent m);
	public void trace(srEvent e);
	public void trace(int ID);

	public void requestEntryPermit(int ID);

	public void releaseEntryPermit();
	
	public void requestPermit(int ID);
	
	public void releasePermit();

	public void requestMPermit(monitorEventTypeParameters.eventType op, int ID, String methodName, 
			String conditionName);

	public void releaseMPermit();
	
	public void requestMPermitSpecTest(int ID);
	
	public void requestAndReleaseCommPermit(int ID, String eventName);
	
	public void exerciseEvent(int ID, String eventName);
	
	public void traceCommEvent(monitorSpecEvent m);
	
	public void start();
	public void requestSendPermit(int ID, String channelName, int callerVersionNumber);
	public boolean requestSendPermitX(int ID, String channelName, int callerVersionNumber);
	public boolean requestSendPermitMS(int ID, String channelName, int callerVersionNumber, int calledID);
	public void msgReceived();
	public int requestReceivePermitX(int ID, String channelName, int calledVersionNumber);
	public int requestReceivePermitMS(int ID, int calledID, String channelName, int calledVersionNumber);

	public void msgReceived(int caller, int callerVersionNumber);
	public void sendArrivedRT(int ID, String channelName, int callerVersionNumber);
	public void monitorEnteredRT(int ID, String channelName, int callerVersionNumber);
}
