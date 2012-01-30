package reachability;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.io.Serializable;

// a class that encodes all the variants of a srSeq in an efficient
// manner
public class VariantGroup implements Serializable{
    private SrSeqPO seq;
    private RaceTable tab;

    // a pointer to the next index
    private int next;

  // a reference to the race analyzer
  //RaceAnalyzer analyzer ;


    public VariantGroup (SrSeqPO seq, RaceTable tab) {
	this.seq = seq;
	this.tab = tab;

	// initialize the next pointer
	next = 0;

	//analyzer = RaceAnalyzer.getInstance();
	//analyzer.setModel(RaceAnalyzer.FIFO);
    }

    public SrSeqPO getOrigSeq () {
	return seq;
    } 

    public int getNumOfVariants () {
	return tab.getNumOfIndices ();
    }

    public int getCurrentVarID () {
	return next - 1;
    }

    public int getSeqID () {
	return seq.getID ();
    }

    public SrSeqPO next () {
	SrSeqPO rval = null;
	if (next < tab.getNumOfIndices ()) {
	    rval = nextVariant ();
       if (propertyReader.getInstance().getPrintGeneProperty () == propertyParameters.GENEON) {
		    // set gene string
		    StringBuffer gene = new StringBuffer (rval.getGene ());
	   	 rval.setGene (gene.append ("." + next).toString());
	    }
	    next ++;
	}
	else {
	    throw new NoSuchElementException ("Invalid variant access!");
	}

	return rval;
    }

    public boolean hasNext () {
	return next < tab.getNumOfIndices ();
    }

    private SrSeqPO nextVariant () {
	// VERY IMPORTANT: clone a copy of the original sequence
	SrSeqPO rval = seq.deepCopy ();

	// set the source for this variant
	if (propertyReader.getInstance().getDuplicateCheckProperty () == propertyParameters.CHECKON) {
	    rval.setSource (seq);
	}

	// get the next index in the race table
	int[] index = tab.getIndex (next);

	// get the table heading
	ArrayList heading = tab.getHeading ();

	// initialize prefix index
	int [] prefix_index = new int [rval.getNumOfProcs ()];

	// perform race analysis before set alive flags
	// compute the race set
	RaceAnalyzer.getInstance().analyze (rval);

	// IMPORTANT: We need to prune the old sends 
	prune (rval, seq.getPrefixIndex());

	Event [] orig_snds = new Event [index.length];

	for (int i = 0; i < index.length; i ++) {
	    if (index[i] > 0) {
		Event recv = (Event) heading.get(i);
		Event cloned_recv  = 
		    rval.getProc(recv.getOwner().getId()).getEvent(recv.getIndex());
		Event orig_snd = cloned_recv.getPartner ();
		orig_snds[i] = orig_snd;
	    }
	    else {
		orig_snds[i] = null;
	    }
	}
	for (int i = 0; i < index.length; i ++) {
	    if (index[i] > 0) {
		// VERY IMPOTANT: WE are working on the clone
		Event recv = (Event) heading.get(i);
		Event cloned_recv = 
		    rval.getProc(recv.getOwner().getId()).getEvent(recv.getIndex());

		// break the original partnership with recv
		Event orig_snd = cloned_recv.getPartner ();
		if (orig_snd != null) {
		    orig_snd.setReceiver (cloned_recv.getOwner());
		    orig_snd.setPartner (null);
		}
		
		// break the original partnership with new_send
		Event cloned_new_send = cloned_recv.getRaceEvent (index[i] - 1);
		Event orig_recv = cloned_new_send.getPartner ();
		if (orig_recv != null) {
		    orig_recv.setPartner (null);
		}
		
		// establish new partnership
		cloned_recv.setPartner (cloned_new_send);
		cloned_new_send.setPartner (cloned_recv);

		// change the channel name of the recv to reflect the
		// fact that now it receives a different send
		cloned_recv.setChannelName (cloned_new_send.getChannelName());

		// set destination state
		cloned_recv.setDestinationStateReceiver (cloned_recv.getOpenDestinationState ());

		// change the open list
		// for now, we only change open list for shared variables
		changeOpenList (cloned_recv);

		// change the label of the recv event
		//cloned_recv.setLabel (cloned_recv.getChannelName() + "[R]");
		//cloned_recv.setLabel (cloned_recv.getLabel()); // changed by RHC: 3/14/07
		cloned_recv.setLabel (cloned_new_send.getLabel()); // in mapped sequence, send has _S
		                                                   // will be changed to _R when variant is preprocessed.
		                                                   // note: cloned_recv's label was for a different event

		// reset recollected flag 
		cloned_recv.setRecollected (false);

		// remove all the events happening after this event
		rval.removeAllEventsAfter (cloned_recv);

		// update timestamp
		vectorTimeStamp stamp = cloned_recv.getPrecedingTimestamp();
		stamp.updateIntegerTS (cloned_recv.getOwner().getId());
		stamp.updateVectorTS(cloned_new_send.getTimestamp());
		cloned_recv.setTimestamp (stamp);
	    }
	}

	if (propertyReader.getInstance().getCoreAlgorithmProperty () != propertyParameters.PRUNE) {
	    // set the alive flags
	    for (int i = 0; i < index.length; i ++) {
		if (index[i] > 0) {
		    // VERY IMPOTANT: WE are working on the clone
		    Event recv = (Event) heading.get(i);
		    Event cloned_recv = 
			rval.getProc(recv.getOwner().getId()).getEvent(recv.getIndex());

		    // set alive flags
		    setAliveFlags (cloned_recv);
		}
	    }
	}

	// set prefix index
	for (int i = 0; i < prefix_index.length; i ++) {
	    Process proc = rval.getProc (i);
	    prefix_index[i] = proc.getNumOfEvents ();
	}
	rval.setPrefixIndex (prefix_index);

	// set timestamp for recollected events
	setTimestampForRecollectedEvents (rval);

	return rval;
    }

    // set the alive flags to false for all the events that happen
    // before a given event
    public void setAliveFlags (Event event) {
	SrSeqPO seq = event.getOwner().getOwner ();

	event.setAlive (false);

	// set the flag for all the events happening before this event
	for (int i = 0; i < seq.getNumOfProcs (); i ++) {
	    Process pit = seq.getProc (i);
	    for (int j = 0; j < pit.getNumOfEvents (); j ++) {
		Event eit = pit.getEvent (j);
		if (eit.getType () == Event.RCV
		    && eit.getTimestamp().lessThan(event.getTimestamp())) {
		    eit.setAlive (false);
		}
	    }
	}
    }

    // we only consider "new" send events in the race set of an "old" receive
    private void prune (SrSeqPO seq_po, int[] prefix_index) {
	for(int i = 0; i < seq_po.getNumOfProcs (); i ++) {
	    Process proc = seq_po.getProc (i);
	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event event = proc.getEvent (j);
		if (event.getType () == Event.RCV 
		    && event.getIndex () < prefix_index[event.getOwner().getId()]) {
		    // this event is a "old" receive event
		    for (int k = 0; k < event.getSizeOfRaceSet (); k ++) {
			Event snd = event.getRaceEvent (k);
			Process snd_owner = snd.getOwner ();
			if (snd.getIndex () < prefix_index[snd_owner.getId ()]) {
			    // this send event is "old" and should be
			    // pruned
			    event.removeRaceEvent (k);
			    k --;
			}
		    }
		}
	    }
	}
    }

    private void setTimestampForRecollectedEvents (SrSeqPO seq) {
	Timestamp ts = new Timestamp (seq.getNumOfProcs ());
	for (int i = 0; i < ts.getDimension (); i ++) {
	    ts.setValue (i, seq.getPrefixIndex(i));
	}

	vectorTimeStamp vts = new vectorTimeStamp ();
	vts.makeVectorTimeStamp(ts);
	
	for (int i = 0; i < seq.getNumOfProcs (); i ++) {
	    Process proc = seq.getProc(i);
	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event event = proc.getEvent (j);
		if (event.isRecollected ()) {
		    event.setTimestamp (vts);
		}
	    }
	}
    }


    // for now we only need to change open list for shared variables
    private void changeOpenList (Event rcv) {
	eventTypeParameters.eventType type2 = rcv.getEventType2 ();
	if (type2 == eventTypeParameters.SHAREDVARIABLE_COMPLETION) {
	    String channel = rcv.getChannelName ();
	    String op = channel.substring (channel.indexOf (":") + 1);
	    if (op.equals("Read")) {
		ArrayList Read_openList = new ArrayList ();
		Read_openList.add("Write");
		rcv.setOpenList (Read_openList);
	    }
	    else if (op.equals("Write")) {
		ArrayList Write_openList = new ArrayList ();
		Write_openList.add("Write");
		Write_openList.add("Read");
		rcv.setOpenList (Write_openList);
	    }
	    else {
		System.out.println("changeOpenList: This should not happen!!!");
	    }
	}
    } 

    private void dumpIndex (int[] index) {
	System.out.print("[");
	for (int i = 0; i < index.length; i ++) {
	    if (i > 0) {
		System.out.print(", ");
	    }
	    System.out.print(index[i]);
	}
	System.out.println("]");
	
	System.out.print("[");
	for (int i = 0; i < index.length; i ++) {
	    if (i > 0) {
		System.out.print("; ");
	    }
	    System.out.print((Event) tab.getHeading().get(i));
	}
	System.out.println("]");
    }
}
