package reachability;
class programTransition {
  	public String ID;
	public String state;
	public String destinationState;
	public String label;
	public programTransition(String ID, String sourceState,String label) {
	// ID executes transition labeled label in state represented by state
	   this.ID = ID;
		this.state = sourceState;
		this.destinationState = new String("");
		this.label = label;
	}
	public programTransition(String ID, String sourceState, String destinationState, String label) {
	// ID executes transition labeled label in state represented by state
	   this.ID = ID;
		this.state = sourceState;
		this.destinationState = destinationState;
		this.label = label;
	}
	public String toString() {
	   if (destinationState == "")
			return (ID+":"+state+"--"+label+"-->");
		else
			return (ID+":"+state+"--"+label+"-->"+destinationState);
	}
}

