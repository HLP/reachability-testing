package reachability;
import java.util.ArrayList;
import java.io.Serializable;

// partial order representation of a SR-sequence
public final class SrSeqPO implements Serializable {
    // a special empty sequence object
    public static final SrSeqPO EMPTY_SEQ = new SrSeqPO ();

    // the list of processes
    private ArrayList procs;
    
    // an index indicating the prefix part (i.e., the variant used to
    // derive this sequence)
    private int prefix_index [];

    // FOR INTERNAL DEBUGGING
    // for a variant, source points to the sequence from which the
    // variant is derived 
    private SrSeqPO source;

    // total order index
    // NEEDED for special handling in SBRT
    private int tindex;

    // sequence ID
    private int id;

    // string that indicates where the sequence comes from
    private String gene;

    public SrSeqPO () {
	procs = new ArrayList ();
	tindex = 0;
    }

    public void setID (int id) {
	this.id = id;
    }
    public int getID () {
	return id;
    }

    public void setPrefixIndex (int [] prefix_index) {
	if (prefix_index != null) {
	    this.prefix_index = new int [prefix_index.length];
	    for (int i = 0; i < prefix_index.length; i ++) {
		this.prefix_index[i] = prefix_index[i];
	    }
	}
    } 

    public void setSource (SrSeqPO source) {
	this.source = source;
    }
    public SrSeqPO getSource () {
	return source;
    }

    public void setGene (String gene) {
	this.gene = gene;
    }
    public String getGene () {
	return gene;
    }

    public int getPrefixIndex (int pid) {
	return getPrefixIndex()[pid];
    }
    public int [] getPrefixIndex () {
	if (prefix_index == null
	    || prefix_index.length == 0) {
	    // if prefix index has not been set, then return the
	    // default one with all indices as zero 
	    prefix_index = new int [getNumOfProcs ()];
	    for (int i = 0; i < prefix_index.length; i ++) {
		prefix_index[i] = 0;
	    }
	}
	return prefix_index;
    }

    public void addProc (Process proc) {
	if (getProc (proc.getId ()) == null) {
	    int i = 0;
	    for (; i < procs.size (); i ++) {
		Process tmp = (Process) procs.get (i);
		if(proc.getId () < tmp.getId ()) {
		    break;
		}
	    }
	    procs.add(i, proc);
	}
	else {
	    System.out.println("Inside addProc: The process already exists!");
	}
    }

    public Process addProc (int pid) {
	Process rval = getProc (pid);
	if (rval == null) {
	    rval = new Process (pid, this);
	    addProc(rval);
	}
	return rval;
    }

    public void addMissingProcs () {
	int max_pid = -1;
	for (int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get(i);
	    if (max_pid < proc.getId ()) {
		max_pid = proc.getId ();
	    }
	}
	// we add empty procs for those missing processes;
	for (int i = 0; i < max_pid; i ++) {
	    addProc(i);
	}	
    }
    
    public ArrayList getProcs () {
	return procs;
    }

    public Process getProc (int pid) {
	Process rval = null;
	for(int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get(i);
	    if(pid == proc.getId ()) {
		rval = proc;
		break;
	    }
	}
	return rval;
    }

    public int getNumOfProcs () {
	return procs.size();
    }

    public void addEvent (int pid, Event event) {
	Process proc = getProc(pid);
	if (proc != null) {
	    proc.addEvent (event);

	    // update tindex
	    tindex ++;
	    event.setTIndex (tindex);
	}
	else {
	    System.out.println("Inside addEvent: Wrong process id!");
	}
    }

    public void initializePrecedingTimestamp () {
	for (int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get (i);
	    proc.initializePrecedingTimestamp ();
	}
    }

    public ArrayList getEventsHappenBefore (Event event) {
	ArrayList rval = new ArrayList ();
	for (int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get (i);
	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event it = proc.getEvent (j);
		if ((it.getTimestamp () != null) 
		    && it.getTimestamp().lessThan (event.getTimestamp())) {
		    rval.add (it);
		}
	    }
	}
	return rval;
    }

    public void removeAllEventsAfter (Event event) {
	for (int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get (i);
	    for (int j = proc.getNumOfEvents() - 1; j >= 0; j --) {
		Event it = proc.getEvent (j);
		if (event.isInPrimeStruct (it)) {
		    // break partner links
		    Event partner = it.getPartner ();
		    if (partner != null) {
			partner.setPartner (null);
		    }

		    proc.removeEvent (j);
		}
		else {
		    if (event.getTimestamp().lessThan(it.getTimestamp())) {
			it.setRecollected (true);
		    }

		    break;
		}
	    }
	}
    }

    public boolean couldBeConcurrent (Event r1, Event r2) {
	boolean rval = false;
	if ((r1.getType () == Event.RCV) && (r2.getType () == Event.RCV)) {
	    rval = r1.getTimestamp().lessThan(r2.getTimestamp())
		&& !r1.getTimestamp().lessThan(r2.getPrecedingTimestamp())
		&& !r1.getTimestamp().equals(r2.getPrecedingTimestamp());
	}

	return rval;
    }

    public void resetAlive () {
	for (int i = 0; i < getNumOfProcs (); i ++) {
	    Process proc = getProc (i);
	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event event = proc.getEvent (j);
		event.setAlive (true);
	    }
	}
    }

    // ADDED for SBRT
    public ArrayList getSBRTStartStates () {
	ArrayList rval = new ArrayList ();
	for (int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get (i);
	    if (proc.getNumOfEvents() == 0) {
		rval.add(new Integer(0));
	    }
	    else {
		Event event = proc.getLastEvent ();
		if (event.getEventType2().equals(eventTypeParameters.SYNCH_SEND)
		    && event.getPartner () == null) {
		    rval.add(new Integer(event.getSourceStateSender()));
		}
		else if (event.getEventType2().equals(eventTypeParameters.ASYNCH_SEND)
		    && event.getPartner () == null) {
		    rval.add(new Integer(event.getDestinationStateSender()));
		}
		else if (event.isRecollected ()) {
		    rval.add(new Integer(event.getSourceStateReceiver()));
		}
		else if (event.getEventType2().equals(eventTypeParameters.ASYNCH_SEND)) {
		    rval.add(new Integer(event.getDestinationStateSender()));
		}
		else if (event.getEventType2().equals(eventTypeParameters.ASYNCH_RECEIVE)) {
		    rval.add(new Integer(event.getDestinationStateReceiver()));
		}
		else {
		    if (event.getType() == Event.SND) {
				rval.add(new Integer(event.getDestinationStateSender()));
		    }
		    else {
			 	rval.add(new Integer(event.getDestinationStateReceiver ()));
		    }
		}
	    }
	}
	return rval;
    }

    public SrSeqPO deepCopy () {
	SrSeqPO rval = new SrSeqPO ();
	for (int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get (i);
	    rval.addProc (proc.deepCopy (rval));
	}

	// copy gene string
	rval.setGene (getGene());
	return rval;
    }

    public boolean equals (SrSeqPO it) {
	boolean rval = true;

	if (getNumOfProcs () != it.getNumOfProcs ()) {
	    rval = false;
	}
	else {
	    for (int i = 0; i < getNumOfProcs () ; i ++) {
		Process proc = getProc (i);
		Process it_proc = it.getProc (i);
		if (!proc.equals (it_proc)) {
		    rval = false;
		    break;
		}
	    }
	}

	return rval;
    }

    // output a standard format
    public String toString () {
	StringBuffer rval = new StringBuffer ();
	if (prefix_index != null) {
	    rval.append("PREFIX INDEX: [");
	    for(int i = 0; i < prefix_index.length; i ++) {
		if (i > 0) {
		    rval.append(", ");
		}
		rval.append(prefix_index[i]);
	    }
	    rval.append("]\n");
	}

	for (int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get (i);
	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event event = proc.getEvent (j);
		if (event.getType () == Event.SND) {
		    rval.append(event.toString ());
		    rval.append("\n");
		}
		else if (event.getType () == Event.RCV) {
		    if (event.getPartner () == null
			|| !event.getAlive ()) {
			// NOTE that if partner is not null, then this
			// event must have been printed out before by
			// its partner
			rval.append(event.toString ());
			rval.append("\n");
		     }
		}
	    }
	}
	// print out source info
	if (getSource () != null) {
	    rval.append("Source: \n");
	    rval.append(getSource());
	}
	return rval.toString ();
    }

    public void dump () {
	for (int i = 0; i < procs.size (); i ++) {
	    Process proc = (Process) procs.get (i);
	    proc.dump ();
	}
	
	// print out source info
	if (getSource () != null) {
	    System.out.println("\nSource:");
	    getSource().dump ();
	}
    }
}
