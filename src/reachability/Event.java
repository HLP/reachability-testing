package reachability;
import java.util.*;

import java.io.Serializable;



public class Event implements eventTypeParameters, Serializable {

    // constants

    public final static int SND = 0;

    public final static int RCV = 1;

    

    // event type

    private int type;



    // event index relative to a process

    private int index;



    // partner pid

    private int partner_pid;



    // partner index

    private int partner_index;



    // synchronization partner (e.g. the partner of a send event is a

    // receive event) 

    private Event partner;



    // the race set of this event

    private ArrayList races;



    // the owner process

    private Process owner;



    // the receiver process (for unreceived send event)

    private Process receiver;



    // timestamp

    private vectorTimeStamp stamp;



    // channel name

    private String channel_name;



    // open list (support selective waits)

    private ArrayList open_list;


    // caller list
    private callerList inhibited_callers;

    // event label

    private String label;



    // flag to indicate whether it is an entry event

    private boolean is_entry;



    // flag used for preventing redundant variants

    private boolean alive;



    // flag to indicate whether a (receive) event is recollected

    private boolean recollected;



    // NEW field, eventType2

    private eventType eType2;



    // save the preceding timestamp

    private vectorTimeStamp preceding_timestamp;



    // NEW fields added for SBRT

    private int sourceStateSender = -1;

    private int sourceStateReceiver = -1;

    private int destinationStateSender = -1;

    private int destinationStateReceiver = -1;

    // total index (in the entire sequence)
    // NEEDED by special processing for SBRT
    private int tindex;
    
    private boolean destinationStateRevisited;

    public Event (int type, int index, Process owner) {

	this.type = type;

	this.index = index;

	if (index < 0) {

	    System.out.println("Negative index!");

	    System.exit (1);

	}



	this.owner = owner;

	this.partner_pid = -1;

	this.partner_index = -1;

	races = new ArrayList ();

	open_list = new ArrayList ();

	// inhibited list (added for SBRT)
	inhibited_callers = new callerList ();
	inhibited_callers.setCallerList (new ArrayList ());

	// set alive

	alive = true;

	recollected = false;

	// set tindex
	tindex = -1;
	
	// set revisited state to false by default
        destinationStateRevisited = false;
    }



    public void setAlive (boolean alive) {

	this.alive = alive;

    }

    public boolean getAlive () {

	return alive;

    }

    public void setTIndex (int tindex) {
	this.tindex = tindex;
    }
    public int getTIndex () {
	return tindex;
    }

    public void setRecollected (boolean recollected) {

	this.recollected = recollected;

    }

    public boolean isRecollected () {

	return recollected;

    }



    public boolean isOld () {

	boolean rval = false;

	Process proc = getOwner();

	SrSeqPO seq = proc.getOwner();

	rval = getIndex() < seq.getPrefixIndex (proc.getId());

	return rval;

    }



    public int getType () {

	return type;

    }



    public int getIndex () {

	return index;

    }

    

    public Process getOwner () {

	return owner;

    }

    

    public Event getPartner () {

	if (partner == null) {

	    if (partner_pid != -1 && partner_index != -1) { 

		SrSeqPO seq = owner.getOwner();

		Process proc = seq.getProc (partner_pid);

		partner = proc.getEvent (partner_index);

	    }

	}

	return partner;

    }



    public void setPartner (int pid, int index) {

	partner_pid = pid;

	partner_index = index;

    }



    public void setPartner (Event partner) {

	this.partner = partner;

	if (partner == null) {

	    // we never change the partnership

	    //partner_pid = -1;

	    partner_index = -1;

	}

	else {

	    partner_pid = partner.getOwner().getId ();

	    partner_index = partner.getIndex ();

	    receiver = partner.getOwner();

	}

    }



    public int getPartnerPid () {

	return partner_pid;

    }



    public void setReceiver (Process receiver) {

	if(receiver != null) {

	    partner_pid = receiver.getId ();

	}

	this.receiver = receiver;

    }

    public Process getReceiver () {

	if (receiver == null) {

	    receiver = owner.getOwner().getProc(partner_pid);

	}

	if (receiver == null) {

	    System.out.println("NULL receiver: (" + owner.getId() + ", " + index + ", " + partner_pid + ")");

	}

	return receiver;

    }



    public void setChannelName (String channel_name) {

	this.channel_name = channel_name;

    }

    public String getChannelName () {

	return channel_name;

    }



    public ArrayList getOpenList () {

	return open_list;

    }
    public void setOpenList (ArrayList open_list) {

	this.open_list = open_list;

    }

    public callerList getInhibitedCallers () {
	return inhibited_callers;
    }
    public void setInhibitedCallers (callerList inhibited_callers) {
	this.inhibited_callers = inhibited_callers;
    }

    public void addOpenChannel (String channel_name) {

        OpenEvent oe = new OpenEvent(channel_name, -1);

	open_list.add (oe);

    }

    public boolean isOpenChannel (String channel) {

	boolean rval = false;

	if (channel == null) {
	    System.out.println("channel is null");
	    dump ();
	}

	for (int i = 0; i < open_list.size (); i ++) {

	    OpenEvent it = (OpenEvent) open_list.get(i);

	    if (it == null) {
		System.out.println("OpenEvent is null");
		System.out.println("Index is " + i);
	    }

	    if (channel.equals(it.getLabel())) {

		rval = true;

		break;

	    }

	}

	return rval;

    }



    public void setLabel (String label) {

	this.label = label;

    }

    public String getLabel () {

	return label;

    }



    public void setIsEntry (boolean is_entry) {

	this.is_entry = is_entry;

    }

    public boolean getIsEntry () {

	return is_entry;

    }



    public void setEventType2 (eventType eType2) {

	this.eType2 = eType2;

    }

    public eventType getEventType2 () {

	return eType2;

    }



    public boolean isSyncSend () {

	return eType2.equals(eventTypeParameters.SYNCH_SEND)

	    || eType2.equals(eventTypeParameters.MONITOR_CALL)

	    || eType2.equals(eventTypeParameters.SEMAPHORE_CALL)

	    || eType2.equals(eventTypeParameters.LOCK_CALL);

    }



    public void addRaceEvent (Event race) {

	races.add(race);

    }

    public Event getRaceEvent (int i) {

	return (Event) races.get(i);

    }

    public void removeRaceEvent (int index) {

	races.remove (index);

    }

    public ArrayList getRaceSet () {

	return races;

    }

    public int getSizeOfRaceSet () {

	return races.size ();

    }

    public void clearRaceSet () {

	races.clear();

    }



    public vectorTimeStamp getTimestamp () {

	return stamp;

    }

    public void setTimestamp (vectorTimeStamp stamp) {

	this.stamp = stamp;

    }



    public int getSourceStateSender () {

	return sourceStateSender;

    }

    public void setSourceStateSender (int sourceStateSender) {

	this.sourceStateSender = sourceStateSender;

    }



    public int getSourceStateReceiver () {

	return sourceStateReceiver;

    }

    public void setSourceStateReceiver (int sourceStateReceiver) {

	this.sourceStateReceiver = sourceStateReceiver;

    }



    public int getDestinationStateSender () {

	return destinationStateSender;

    }

    public void setDestinationStateSender (int destinationStateSender) {

	this.destinationStateSender = destinationStateSender;

    }



    public int getDestinationStateReceiver () {

	return destinationStateReceiver;

    }

    public void setDestinationStateReceiver (int destinationStateReceiver) {

	this.destinationStateReceiver = destinationStateReceiver;

    }


    public boolean getDestinationStateRevisited () {

	return destinationStateRevisited;

    }

    public void setDestinationStateRevisited (boolean destinationStateRevisited) {

	this.destinationStateRevisited = destinationStateRevisited;

    }


    public int getOpenDestinationState () {

	int rval = -1;

	for (int i = 0; i < open_list.size (); i ++) {

	    OpenEvent it = (OpenEvent) open_list.get(i);

	    if (channel_name.equals(it.getLabel())) {

		rval = it.getDestinationState ();

		break;

	    }

	}
	if (rval < -1) {
		System.out.println("abort: destination is " + rval + "; size of open list is " + open_list.size());
		System.exit(1);
	}

	return rval;

    }



    public Event getPrecedingEvent () {

	Event rval = null;

	if (index > 0) {

	    rval = getOwner().getEvent (index - 1);

	}



	return rval;

    }

  // START of changes
    public Event getSucceedingEvent () {
      Event rval = null;
      if (index + 1 < owner.getNumOfEvents()) {
	rval = getOwner().getEvent (index + 1);
      }
      
      return rval;
    }


    // set the timestamp of the preceding event

  public void initializePrecedingTimestamp () {
    if(index == 0) {
      preceding_timestamp = new vectorTimeStamp (getOwner().getId());
    }
    else {
      Event preceding_event = 
	getOwner().getEvent(index - 1);
      // get a clone of the timestamp of the preceding event
      vectorTimeStamp stamp = preceding_event.getTimestamp ();
      preceding_timestamp = stamp.getVectorTS ();

      if (preceding_event.isSyncSend ()) {
	Event partner = preceding_event.getPartner ();
	if (partner != null) {
	  Event successor = partner.getSucceedingEvent();
	  while (successor != null &&
		 successor.getTimestamp().lessThan (getTimestamp())) {
	    partner = successor;
	    successor = successor.getSucceedingEvent();
	  }
	  preceding_timestamp.updateVectorTS (partner.getTimestamp ());
	}
      }
    }
  }

  // END of changes


  public void setPrecedingTimestamp (vectorTimeStamp preceding_timestamp) {

	this.preceding_timestamp = preceding_timestamp;

    }



    public vectorTimeStamp getPrecedingTimestamp () {

	return preceding_timestamp;

    }



    public boolean isInPrimeStruct (Event it) {

	boolean rval = false;

	vectorTimeStamp my_ts = getTimestamp ();

	vectorTimeStamp it_pts = it.getPrecedingTimestamp ();

	if (my_ts.isSame (it_pts) || my_ts.lessThan(it_pts)) {

	    rval = true;

	}

	return rval;

    }



    public boolean stamped () {

	return stamp != null;

    }



    public Event deepCopy (Process proc) {

	Event rval = new Event (type, index, proc);

	rval.setPartner (partner_pid, partner_index);

	rval.setChannelName (getChannelName ());

	rval.setLabel (getLabel());

	rval.setIsEntry (getIsEntry ());

	rval.setAlive (getAlive());

	// set tindex
	rval.setTIndex (getTIndex ());

	// Event Type 2

	rval.setEventType2 (getEventType2 ());



	if (stamp != null) {

	    rval.setTimestamp (stamp.getVectorTS ());

	    // preceding timestamp

	    rval.setPrecedingTimestamp (getPrecedingTimestamp ().getVectorTS ());

	}



	// we also need to copy the open list
	for (int i = 0; i < getOpenList().size (); i ++) {
	    OpenEvent it = (OpenEvent) getOpenList().get(i);
	    rval.getOpenList().add(it.deepCopy());
	}

	// we also need to copy the inhibited callers
	rval.getInhibitedCallers().getCallerList().addAll 
	    (getInhibitedCallers().getCallerList()); 

	// we do not copy race set (which is indeed tricky)


	// copy source/destination states for SBRT

	rval.setSourceStateSender (getSourceStateSender());

	rval.setSourceStateReceiver (getSourceStateReceiver());

	rval.setDestinationStateSender (getDestinationStateSender ());

	rval.setDestinationStateReceiver (getDestinationStateReceiver ());

	// set recollected flag
	rval.setRecollected (isRecollected());

	return rval;

    }



    public boolean equals (Event it) {

	boolean rval = true;

	if (getType () != it.getType ()

	    || (getOwner().getId () != it.getOwner().getId ())

	    || (getIndex() != it.getIndex ())) {

	    rval = false;

	}

	else {

	    if (getPartner () != null && it.getPartner () != null) {

		if (getPartner().getOwner().getId() != it.getPartner().getOwner().getId ()

		    || getPartner().getIndex () != it.getPartner().getIndex ()) {

		    rval = false;

		}

	    }

	    else if (! (getPartner() == null && it.getPartner () == null)) {

		rval = false;

	    }

	    if (rval) {

		if (getTimestamp () != null && it.getTimestamp () != null) {

		    if (!getTimestamp().equals(it.getTimestamp ())) {

			rval = false;

		    }

		}

		else if (! (getTimestamp() == null && it.getTimestamp () == null)) {

		    rval = false;

		}

	    }

	}



	return rval;

    }



    public String toString () {

	StringBuffer rval = new StringBuffer (15);

	rval.append("(");



	if (getPartner () != null) {

	    if (type == SND) {

		rval.append(owner.getId());

		rval.append(", ");

		rval.append(index);

		rval.append(", ");

		rval.append(getPartner().getOwner().getId());

		rval.append(", ");

		rval.append(getPartner().getIndex());

	    }

	    else {

		rval.append(getPartner().getOwner().getId());

		rval.append(", ");

		rval.append(getPartner().getIndex());

		rval.append(", ");

		rval.append(owner.getId());

		rval.append(", ");

		rval.append(index);

	    }

	}

	else if (type == SND) {

		rval.append(owner.getId());

		rval.append(", ");

		rval.append(index);

		rval.append(", ");

		rval.append(getReceiver().getId());

	}

	else if (type == RCV) {

		rval.append(owner.getId());

		rval.append(", ");

		rval.append(index);

	}



	rval.append(")");

	

	// alive flag

	if (!getAlive ()) {

	    rval.append(" DEAD");

	}



	if (isRecollected ()) {

	    rval.append(" RECOLLECTED");

	}



	rval.append(" " + eType2 + " ");

	rval.append(" " + sourceStateSender + " ");

	rval.append(" " + sourceStateReceiver + " ");

	rval.append(" " + destinationStateSender + " ");

	rval.append(" " + destinationStateReceiver + " ");

	rval.append(" " + destinationStateRevisited + " ");
	

        if (type == RCV) {

	    rval.append (" OPEN LIST (");

	    for (int i = 0; i < open_list.size (); i ++) {

		if (i != 0) {

		    rval.append(", ");

		}

		rval.append((OpenEvent)open_list.get(i));

	    }

	    rval.append(")");

	}



	return rval.toString ();

    }


    // this method dumps all information about this event, in

    // particular, the race set 

    public void dump () {

	System.out.print(toString ());

	System.out.println(getTimestamp ());

	System.out.println("Channel: " + getChannelName());

	if(type == RCV && races.size() > 0) {

	    System.out.print(" Race set: ");

	    for (int i = 0; i < races.size (); i ++) {

		Event event = (Event) races.get (i);

		System.out.print("(" + event.getOwner().getId() + ", " + event.getIndex() + ")");

	    }

	    System.out.println();

	}

    }
    
	public String prettyPrint() {
	// rhc: called by RaceAnalyzer to print the sharedVariable name and operation for a data race
	
		return (channel_name);
	}

}

