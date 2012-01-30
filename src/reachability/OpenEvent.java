package reachability;
import java.io.Serializable;

public final class OpenEvent implements Serializable {
	private String label; 
	private int destinationState = -2;
	public OpenEvent(String label, int destinationState) {
		this.label = label;
		this.destinationState = destinationState;
	}
	public OpenEvent(String label) {
		this.label = label;
	}
	public String getLabel() {return label;}
	public int getDestinationState() {return destinationState;}
    public OpenEvent deepCopy () {
	return new OpenEvent(label, destinationState);
    }
	public String toString() {
		return ("("+label + "," + destinationState+")");
	}
}
