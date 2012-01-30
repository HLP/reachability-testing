package reachability;
import java.util.ArrayList;
import java.io.Serializable;

// interface class
public class srSeq implements Serializable {
    // some global information 

    // the number of processes (not required but probably nice to have)
    private int numOfThreads;

    // prefix info
    private int prefixIndex [];
    private boolean prefixMarker [];
    
    // the list of events
    private ArrayList events;

    // FOR DEBUGGING
    private SrSeqPO source;

    public srSeq () {
	events = new ArrayList ();
    }

    public void setSource (SrSeqPO source) {
	this.source = source;
    }
    public SrSeqPO getSource () {
	return source;
    }

    public void setNumOfThreads (int numOfThreads) {
	this.numOfThreads = numOfThreads;
    }
    public int getNumOfThreads () {
	return numOfThreads;
    }

    public void setPrefixIndex (int [] prefixIndex) {
	this.prefixIndex = new int [prefixIndex.length];
	for (int i = 0; i < prefixIndex.length; i ++) {
	    this.prefixIndex[i] = prefixIndex[i];
	}
    } 
    public int [] getPrefixIndex () {
	return prefixIndex;
    }

    // NEED to check duplicates
    public void addEvent (srEvent event) {
	events.add(event);
    }
    public void addEvent (int index, srEvent event) {
	events.add(index, event);
    }
    public srEvent getEvent (int index) {
	return (srEvent) events.get(index);
    }

    // added by lei
    public srEvent getEvent (int procId, int index) {
	srEvent rval = null;
	for (int i = 0; i < events.size(); i ++) {
	    srEvent event = (srEvent) events.get(i);
	    if (event.getCaller () == procId 
		&& event.getCallerVersionNumber () == index) {
		rval = event;
		break;
	    }
	}
	return rval;
    }

    public ArrayList getEvents () {
     if (events == null) System.out.println("NULL events in srSeq");
	return events;
    }
    public void setEvents(ArrayList events) {
      this.events = events;
    }
    public int getNumOfEvents () {
	return events.size ();
    }

    public boolean isOld (srEvent event) {
        int pid = -1;
	int index = -1;
	if (event.getEventType () == eventTypeParameters.ASYNCH_SEND
	    || event.getEventType () == eventTypeParameters.UNACCEPTED_ASYNCH_SEND) {
	    pid = event.getCaller ();
	    index = event.getCallerVersionNumber () - 1;
	}
	else if (event.getEventType () == eventTypeParameters.ASYNCH_RECEIVE
		 || event.getEventType () == eventTypeParameters.UNACCEPTED_RECEIVE) {
	    pid = event.getCalled();
	    index = event.getCalledVersionNumber () - 1;
	}
	else {
	    System.out.println("Unknown event type");
	}

	return index < prefixIndex [pid];
    }

    public String toString () {
	StringBuffer rval = new StringBuffer (100);
	if (prefixIndex != null) {
	    rval.append("PREFIX INDEX: [");
	    for(int i = 0; i < prefixIndex.length; i ++) {
		if (i > 0) {
		    rval.append(", ");
		}
		rval.append(prefixIndex[i]);
	    }
	    rval.append("]\n");
	}

	if (prefixMarker != null) {
	    rval.append("PREFIX MARKER: [");
	    for(int i = 0; i < prefixMarker.length; i ++) {
		if (i > 0) {
		    rval.append(", ");
		}
		rval.append(prefixMarker[i]);
	    }
	    rval.append("]\n");
	}

	for(int i = 0; i < events.size (); i ++) {
	    rval.append(getEvent(i)).append("\n");
	}

	return rval.toString ();
    }
    
	public String prettyPrint() {
		StringBuffer rval = new StringBuffer (100);
		for(int i = 0; i < events.size (); i ++)
	   	rval.append(getEvent(i).prettyPrint());
		return rval.toString();    
	}
 
}


