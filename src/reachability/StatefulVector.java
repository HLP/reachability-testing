package reachability;
import java.util.Vector;

public final class StatefulVector {
    public Vector value;
    public StatefulVector(int capacity) {
		value = new Vector(capacity);
    }
    public String toString() {
		StringBuffer state = new StringBuffer();
		for (int i=0; i<value.size(); i++)
	    state.append(value.elementAt(i).toString());
		return state.toString();
    }
}
