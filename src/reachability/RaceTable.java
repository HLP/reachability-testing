package reachability;
import java.util.ArrayList;
import java.io.Serializable;

public class RaceTable implements Serializable {
    private ArrayList heading;
    private ArrayList indices;

    public RaceTable () {
	heading = new ArrayList ();
	indices = new ArrayList ();
    }

    public Event getHeadingEvent (int index) {
	return (Event) heading.get(index);
    }
    public ArrayList getHeading () {
	return heading;
    }
    public int getNumOfHeadingEvents () {
	return heading.size ();
    }
    public void addHeadingEvent (Event rcv) {
	heading.add(rcv);
    }
    public void addHeadingEvent (int pos, Event rcv) {
	heading.add(pos, rcv);
    }

    public int[] getIndex (int row) {
	return (int[]) indices.get(row);
    }
    public int getNumOfIndices () {
	return indices.size ();
    }
    public void addIndex (int[] index) {
	int[] cloned_index = new int [index.length];
	for (int i = 0; i < index.length; i ++) {
	    cloned_index[i] = index[i];
	}
	indices.add(cloned_index);
    }

    public void dump () {
	System.out.println("Table Heading: ");
	for (int i = 0; i < heading.size (); i ++) {
	    Event event = (Event) heading.get(i);
	    event.dump ();
	}
	for (int i = 0; i < indices.size (); i ++) {
	    System.out.print("[");
	    int[] tmp = getIndex(i);
	    for (int j = 0; j < tmp.length; j ++) {
		if (j > 0) {
		    System.out.print(", ");
		}
		System.out.print(tmp[j]);
	    }
	    System.out.println("]");	    
	}
    }
}
