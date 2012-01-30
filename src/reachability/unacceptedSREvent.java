package reachability;

final class unacceptedSREvent extends srEvent implements eventTypeParameters {

	private int testAndReplayIndex = 0; // index of an unaccepted_asynch_send in test/replay sequence
	private srEvent objectSequenceEvent = null;
	private srThreadEvent threadSequenceEvent = null;


	public int getTestAndReplayIndex() { return testAndReplayIndex;}
	public void setTestAndReplayIndex(int index) { this.testAndReplayIndex = index;}

	public srEvent getObjectSequenceEvent() { return objectSequenceEvent;}
	public void setObjectSequenceEvent(srEvent event) { objectSequenceEvent = event;}
	public srThreadEvent getThreadSequenceEvent() { return threadSequenceEvent;}
	public void setThreadSequenceEvent(srThreadEvent event) { threadSequenceEvent = event;}

	public unacceptedSREvent(srEvent e) {
		super(e.getCaller(),e.getCalled(),e.getCallerVersionNumber(),e.getCalledVersionNumber(),
		e.getChannelName(),e.getChannelVersionNumber(),e.getEventType(),
		e.getVectorTS(),e.getLabel(),e.getOpenList(),e.getEventType2());
	}

	public String toString() {	
		if (super.label.equals(""))
			return "("+	super.caller + "," + super.called + "," + super.callerVersionNumber
					+ "," + super.calledVersionNumber + "," + super.channelName
					+ "," + super.channelVersionNumber + "," + super.eType + "," + 
					testAndReplayIndex + ")";
		else
			return "("+	super.caller + "," + super.called + "," + super.callerVersionNumber
					+ "," + super.calledVersionNumber + "," + super.channelName
					+ "," + super.channelVersionNumber + "," + super.eType + "," + super.label + "," + 
					testAndReplayIndex + ")";	
	}

	public boolean equals(Object o2) {
		System.out.flush();
		if (o2 instanceof unacceptedSREvent) {
			unacceptedSREvent e = (unacceptedSREvent) o2;
			return super.channelName.equals(e.channelName) &&
				super.eType.equals(e.eType) && super.caller == e.caller;
		}
		else
			return false;
	} 
}	
