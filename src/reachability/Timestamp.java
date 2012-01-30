package reachability;
import java.io.Serializable;

public class Timestamp implements vectorTimeStampJ, Serializable {
    // the number of processes
    private int dimension;

    // the values in the timestamp
    private int values[];

    public Timestamp (int dimension) {
	this.dimension = dimension;
	values = new int [dimension];
    }

    public int getDimension () {
	return dimension;
    }

    public int getValue (int index) {
	return values[index];
    }
    public void setValue (int index, int value) {
	values[index] = value;
    }

    // update methods
    public void advance (int pid) {
	values[pid] ++;
    }
    public void advance (Timestamp stamp) {
	for(int i = 0; i < dimension; i ++) {
	    if(values[i] < stamp.getValue (i)) {
		values[i] = stamp.getValue (i);
	    }
	}
    }

    public boolean equalTo (Timestamp stamp) {
	boolean rval = true;
	if(dimension != stamp.getDimension ()) {
	    System.out.println ("The two timestamps are not comparable.");
	    rval = false;
	}
	else {
	    for (int i = 0; i < dimension; i ++) {
		if (values[i] != stamp.getValue(i)) {
		    rval = false;
		    break;
		}
	    }
	}
	return rval;
    }
    
    public boolean lessThan (vectorTimeStampJ ts) {
	boolean rval = true;
	Timestamp stamp = (Timestamp) ts;
	if(dimension != stamp.getDimension ()) {
	    System.out.println ("The two timestamps are not comparable.");
	    rval = false;
	}
	else if (equalTo (stamp)) {
	    rval = false;
	}
	else {
	    for (int i = 0; i < dimension; i ++) {
		if (values[i] > stamp.getValue(i)) {
		    rval = false;
		    break;
		}
	    }
	}
	return rval;
    } 

    public Timestamp deepCopy () {
	Timestamp rval = new Timestamp (dimension);
	for (int i = 0; i < dimension; i ++) {
	    rval.setValue (i, getValue(i));
	}
	return rval;
    }

    public String toString () {
	StringBuffer rval = new StringBuffer (15);
	rval.append("[");
	for(int i = 0; i < dimension; i ++) {
	    if(i > 0) {
		rval.append("-");
	    }
	    rval.append(values[i]);
	}
	rval.append("]");
	return rval.toString ();
    }
}
