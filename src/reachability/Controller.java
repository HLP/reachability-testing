package reachability;
public abstract class Controller extends Thread {
	public Controller() {}
	public Controller(String name) {super(name);}
	public abstract void traceMsg(traceMessage m);

	public abstract void traceSendReceive(traceMessage m);

	public abstract void requestSendPermit(int ID, String channelName, int callerVersionNumber);
	public abstract boolean requestSendPermitX(int ID, String channelName, int callerVersionNumber);

	public abstract void requestReceivePermit(int ID, String channelName, int calledVersionNumber);
	public abstract int requestReceivePermitX(int ID, String channelName, int calledVersionNumber);

	public abstract boolean requestSelectPermit(int ID);

	public abstract void requestElseDelayPermit(int ID, int calledVersionNumber);

	public abstract void requestSendExceptionPermit(int ID, String channelName, int callerVersionNumber, int calledVersionNumber);

	public abstract void requestReceiveExceptionPermit(int ID, String channelName, int calledVersionNumber);
	public abstract void requestSynchSendPermit(int ID, String channelName, int callerVersionNumber);

	public abstract void msgReceived();
	
	public abstract void msgReceived(int caller, int callerVersionNumber);
	public abstract void sendArrivedRT(int ID, String channelName, int callerVersionNumber);
	public abstract void monitorEnteredRT(int ID, String channelName, int callerVersionNumber);
}
