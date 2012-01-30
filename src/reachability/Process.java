package reachability;
import java.util.ArrayList;
import java.io.Serializable;

public class Process implements Serializable{
    // initial capacity of the event list
    private static final int INIT_CAP = 50;
    // process id
    private int id;
    // the event list
    private ArrayList events;

    // the owner SR-Sequence
    private SrSeqPO owner;

    public Process (int id) {
	this.id = id;
	events = new ArrayList (INIT_CAP);
    }

    public Process (int id, SrSeqPO owner) {
	this.id = id;
	this.owner = owner;
	events = new ArrayList (INIT_CAP);
    }

    // get the process id
    public int getId () {
	return id;
    }

    public SrSeqPO getOwner () {
	return owner;
    }
    public void setOwner (SrSeqPO seq) {
	this.owner = seq;
    }

    // add an event into the process
    public void addEvent (Event event) {
	if(id == event.getOwner().getId ()) {
	    int i = 0;
	    for (; i < events.size (); i ++) {
		Event it = (Event) events.get (i);
		if (event.getIndex() < it.getIndex ()) {
		    break;
		}
	    }
	    events.add(i, event);
	}
	else {
	    System.out.println("Inside addEvent: Wrong process ID");
	}
    }
    public Event getEvent (int index) {
	Event rval = null;
	if (index < events.size ()) {
	    rval = (Event) events.get (index);
	}
	return rval;
    }

    public Event getLastEvent () {
	return (Event) events.get(events.size() - 1);
    }

    public void initializePrecedingTimestamp () {
	for(int i = 0; i < events.size (); i ++) {
	    Event it = (Event) events.get(i);
	    it.initializePrecedingTimestamp ();
	}
    }

    public void removeEvent (int index) {
	if (index < events.size ()) {
	    // break ties with its partner
	    Event event = (Event) events.get(index);
	    Event partner = event.getPartner ();
	    if (partner != null) {
		if (partner.getType () == Event.SND) {
		    // set receiver before we break the partner link
		    // this is due to our special deepCopy method that
		    // does not copy receiver link
		    partner.setReceiver (this);
		}
		partner.setPartner (null);
	    }
	    
	    events.remove (index);
	}
	else {
	    System.out.println("Inside removeEvent: index out of range!");
	}
    }

    public int getNumOfEvents () {
	return events.size ();
    }

    public Process deepCopy (SrSeqPO owner) {
	Process rval = new Process (id, owner);
	for (int i = 0; i < events.size (); i ++) {
	    Event event = (Event) events.get (i);
	    rval.addEvent(event.deepCopy (rval));
	}
	return rval;
    }

    public boolean equals (Process it) {
	boolean rval = true;

	if (getId () != it.getId ()
	    || getNumOfEvents () != it.getNumOfEvents ()) {
	    rval = false;
	}
	else {
	    for (int i = 0; i < events.size (); i ++) {
		Event event = getEvent (i);
		Event it_event = it.getEvent (i);
		if (!event.equals (it_event)) {
		    rval = false;
		    break;
		}
	    }
	}

	return rval;
    }

    public void dump () {
	System.out.println("Proc ID: " + id);
	for(int i = 0; i < events.size (); i ++) {
	    Event event = (Event) events.get (i);
	    event.dump ();
	}
    }
}
