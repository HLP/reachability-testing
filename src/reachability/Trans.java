package reachability;
final class Trans {
	//private int number;
	private int destinationState;
	private String label;
	private String unmappedLabel;
	private int C;
	private int U;  // C, U, channelName and eventType used for mapping
	private String channelName;
	private String eventType;
	public String getLabel() {return label;}
	public int getDestinationState() {return destinationState;}
	public int getC() {return C;}
	public int getU() {return U;}
	public String getChannelName() {return channelName;}
	public String getEventType() {return eventType;}
	public String getUnMappedLabel() {return unmappedLabel;}
	public Trans(int destination,int number,String label) {
		//this.number = number; 
		this.destinationState = destination;
		this.label = label;
	}
	public Trans(int destination,int number,String label,int C, int U,
	             String channelName, String eventType, String unmappedLabel) {
		//this.number = number; 
		this.destinationState = destination;
		this.label = label;
		this.C = C; this.U = U;
		this.channelName = channelName; this.eventType = eventType;
		this.unmappedLabel = unmappedLabel;
	}
	public String toString() {
	  return "label:" + label + " destination state:" + destinationState + " C:" + C + 
	   "U:" + U + "channelName:" + channelName + " eventType:" + eventType + "unmappedLabel:" + unmappedLabel;
	}
}
