package reachability;
public class VariantCalculator {
    private static final int PRUNE = 0;
    private static final int BASE = 1;
    private static final int FULL = 2;

    // the SR_sequence for which we will derive race variants
    private SrSeqPO seq;

    // keep all the indices
    private RaceTable table;

    private int[] index;

    boolean exhausted;
    int coreAlgorithm;

    public VariantCalculator (SrSeqPO seq) {
	this.seq = seq;
	table = null;
	
	if (propertyReader.getInstance().getCoreAlgorithmProperty () == propertyParameters.PRUNE) {
	    coreAlgorithm = PRUNE;
	}
	else if (propertyReader.getInstance().getCoreAlgorithmProperty () == propertyParameters.BASE) {
	    coreAlgorithm = BASE;
	}
	else {
	    coreAlgorithm = FULL;
	}
    }

    public VariantGroup getVariantGroup () {
	VariantGroup rval = null;

	if (table == null) {
	    constructRaceTable ();
	}

	//System.out.println("Race Table:");
	//table.dump();

	if (table.getNumOfIndices () > 0) {
	    rval = new VariantGroup (seq, table);
	}

	return rval;
    }

    public void reset () {
	exhausted = false;
	for (int i = 0; i < index.length; i ++) {
	    index[i] = 0; 
	}
    }

    private void constructRaceTable () {
	table = new RaceTable ();
	exhausted = false;

	// prune the race set so that the race set of any "old"
	// receive only contains "new" send events.
	prune ();
	
	// collect receive events with non-empty race sets
	collectHeadingEvents ();

	if (propertyReader.getInstance().getInteractionCoverageProperty () == 
	    propertyParameters.INTERACTIONON) {
	  PairwiseRaceTableGenerator generator = 
	    new PairwiseRaceTableGenerator (table);
	  
	  generator.build ();
	}
	else {
	  // initialize the index list
	  index = new int [table.getNumOfHeadingEvents ()];
	  for (int i = 0; i < index.length; i ++) {
	    index[i] = 0;
	  } 
	  nextIndex ();
	  while (!exhausted) {
	    if (indexIsValid ()) {
	      table.addIndex (index);
	    }
	    nextIndex ();
	  }
	}
	//System.out.println("Num of indices: " +
	//	 table.getNumOfIndices());
	//table.dump ();
    }

    // we only consider "new" send events in the race set of an "old" receive
    private void prune () {
	for(int i = 0; i < seq.getNumOfProcs (); i ++) {
	    Process proc = seq.getProc (i);
	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event event = proc.getEvent (j);
		if (event.getType () == Event.RCV 
		    && event.getIndex () < seq.getPrefixIndex(event.getOwner().getId())) {
		    // this event is a "old" receive event
		    for (int k = 0; k < event.getSizeOfRaceSet (); k ++) {
			Event snd = event.getRaceEvent (k);
			Process snd_owner = snd.getOwner ();
			if (snd.getIndex () < seq.getPrefixIndex(snd_owner.getId ())) {
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

    // we only consider "new" receives and "old" receives whose race
    // sets contain at least one "new" send. we also maintain a
    // topological sort of these events (according to the
    // happened-before relation)
    private void collectHeadingEvents () {
	// collect all the receive events into a list and initialize
	// the index array
	for(int i = 0; i < seq.getNumOfProcs (); i ++) {
	    Process proc = seq.getProc (i);
	    for(int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event event = proc.getEvent (j);
		if (event.getType () == Event.RCV) {
		    if (event.getAlive ()
			&& event.getSizeOfRaceSet() > 0) {
			int k = 0;
			for (; k < table.getNumOfHeadingEvents (); k ++) {
			    Event it = (Event) table.getHeadingEvent(k);
			    if(event.getTimestamp().lessThan(it.getTimestamp())) {
				break;
			    }
			}
			table.addHeadingEvent (k, event);
		    }
		}
	    }
	}
    }

    // this method computes the next row in the race table
    public void nextIndex () {
	int i = index.length - 1;
	for(; i > 0; i --) {
	    Event event = table.getHeadingEvent (i);
	    if(index[i] != -1 && event.getSizeOfRaceSet () > 0) {
		// note the original partner of the event is not
		// counted in its race set
		if(index[i] == event.getSizeOfRaceSet ()) {
			clearIndex (i);
		}
		else {
		    break;
		}
	    }
	}
	if ((i < 0) || ((i == 0) && 
	    (index[0] == (table.getHeadingEvent(0)).getSizeOfRaceSet()))) {
	    // the index has reached the maximum
	    exhausted = true;
	}
	else {
	    if (index[i] == 0) {
		Event event = table.getHeadingEvent (i);
		// we are about to change the race outcome of this receive
		// event. so we need to mark those events that happen
		// after this receive event
		for(int j = 0; j < table.getNumOfHeadingEvents (); j ++) {
		    if (j != i) {
			Event it = (Event) table.getHeadingEvent (j);
			if (event.isInPrimeStruct(it)) {
			    index[j] = -1;
			}
		    }
		}
	    }

	    // increment index[i]
	    index[i] ++;
	}
    }

    // set index[i] to zero and remark relevant receive events
    private void clearIndex (int i) {
	index [i] = 0;
	for (int j = i + 1; j < index.length; j ++) {
	    if (index [j] == -1) {
		Event event = table.getHeadingEvent (j);
		if (isConsistent (event)) {
		    index [j] = 0;
		}
	    }
	}
    }

    // check if an event is consistent with the current index. an
    // event e is consistent with the current index if every receive
    // event happens before e receives the original message.
    private boolean isConsistent (Event event) {
	boolean rval = true;
	for (int i = 0; i < index.length; i ++) {
	    if (index [i] > 0) {
		Event it = table.getHeadingEvent (i);
		if (it.isInPrimeStruct(event)) {
		    rval = false;
		    break;
		}
	    }
	}
	return rval;
    }

    // make sure that every send event specified in the index is
    // consistent with the index
    private boolean indexIsValid () {
	boolean rval = true;
	for (int i = 0; i < index.length; i ++) {
	    if (index [i] > 0) {
		Event recv = table.getHeadingEvent (i);
		Event send = recv.getRaceEvent (index[i] - 1);
		if(!isConsistent (send)) {
		    rval = false;
		    break;
		}
	    }
	}
	
	return rval;
    }

    private void dumpIndex () {
	System.out.print("[");
	for (int i = 0; i < index.length; i ++) {
	    if (i > 0) {
		System.out.print(", ");
	    }
	    System.out.print(index[i]);
	}
	System.out.println("]");
    }
}
