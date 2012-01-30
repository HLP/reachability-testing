package reachability;
public final class recollectedEvent {
	private  Integer calledID;
	private  String channelName;
	public recollectedEvent(Integer calledID, String channelName) {
	  this.calledID = calledID;
	  this.channelName = channelName;
	}
	public boolean equals(Object o) {
		if (o instanceof recollectedEvent) {
			recollectedEvent e = (recollectedEvent) o;
			return (calledID.intValue() == e.calledID.intValue() &&
				channelName.equals(e.channelName));
		}
		else return false;
	}
}
