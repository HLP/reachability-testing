package reachability;
import java.util.ArrayList;



// this class translates a totally-ordered sequence to a

// partially-ordered one and vice versa

public class SrSeqTranslator {

    // translate a total order to a partial order

    public static SrSeqPO convert (srSeq total_order) {

	SrSeqPO rval = new SrSeqPO ();

	

	// set perfix info

	rval.setPrefixIndex (total_order.getPrefixIndex ());



	// convert each event

	for(int i = 0; i < total_order.getNumOfEvents (); i ++) {

	    srEvent e = total_order.getEvent (i);

	    if (isCompletedSend (e)) {

		addSend (rval, e);

	    }

	    else if (isCompletedReceive (e)) {

		addRecv (rval, e);

	    }

	    else if (isUnacceptedSend (e)) {

		addUnacceptedSend (rval, e);

	    }

	    else if (isUnacceptedReceive (e)) {

		addUnacceptedRecv (rval, e);

	    }

	}



	// add empty threads for those, e.g. the main thread, do not

	// record any SYN-events.

	rval.addMissingProcs ();



	// set alive flags
	if (total_order.getSource () != null) 
	    setAlive (rval, total_order);


	// set preceding timestamp

	rval.initializePrecedingTimestamp ();
	
	// set gene string
	if (total_order.getSource () != null)
	    rval.setGene(total_order.getSource().getGene());
	    

	if (propertyReader.getInstance().getDuplicateCheckProperty() == propertyParameters.CHECKON) {

	    // source is needed for debugging info

	    rval.setSource(total_order.getSource());

	}

	else {

	    // help GC

	    if (propertyReader.getInstance().getDuplicateCheckProperty() == propertyParameters.CHECKOFF) {

		total_order.setSource (null);

	    }

	}



	if (!isValid (rval)) {

	    System.out.println("Invalid srSeq object! Abort ...");

	    System.exit(1);

	}



	// print out the converted SrSeqPO

	//System.out.println("Converted SrSeqPO: ");

	//rval.dump ();



	return rval;

    }


    public static srSeq convert (SrSeqPO partial_order) {
	return convert (partial_order, false);
    }


    public static srSeq convert (SrSeqPO partial_order, boolean convertRaceSet) {

	// check if the SrSeqPO object is valid

	if (!isValid (partial_order)) {

	    System.out.println("Invalid srSeqPO object. Abort ...");

	    System.exit (1);

	}



	srSeq rval = new srSeq ();

	rval.setSource (partial_order);



	// set prefix info

	rval.setPrefixIndex (partial_order.getPrefixIndex ());



	// put all the events into a total order

	ArrayList events = new ArrayList ();

	for (int i = 0; i < partial_order.getNumOfProcs (); i ++) {

	    Process proc = partial_order.getProc (i);

	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {

		Event event = proc.getEvent (j);

		if (event.isRecollected ()) {

		  events.add(event);

		  continue;

		}

		int k = 0;

		for (; k < events.size (); k ++) {

		    Event it = (Event) events.get (k);

		    // try to preserve the same total order given by

		    // the controller

		    if (it.isRecollected() || 

			event.getTimestamp().lessThan(it.getTimestamp())) {

		      break;

		    }

		    else if (event.getTIndex() != -1 && it.getTIndex() != -1 &&

			     !it.getTimestamp().lessThan(event.getTimestamp()) && 

			     event.getTIndex() < it.getTIndex()) {

		      boolean flag = true;

		      for (int l = k + 1; 

			   l < events.size (); l ++) {

			Event tmp = (Event) events.get(l);

			if (tmp.getTimestamp().lessThan(event.getTimestamp())) {

			  flag = false;

			  break;

			}

		      }

		      if (flag) {

			  break;

		      }

		    }

		}

		events.add (k, event);

	    }

	}

	

	// convert each event and add it into the srSeq object

	for (int i = 0; i < events.size (); i ++) {

	    Event event = (Event) events.get (i);

	    srEvent sr_event = translate (event);

	    rval.addEvent (sr_event);

	}



	//System.out.println("Race Variant (after translation): ");

	//System.out.println(rval);

	// convert race set
	if (convertRaceSet) {
	    for (int i = 0; i < events.size (); i ++) {
		Event event = (Event) events.get(i);
		srEvent sre = (srEvent) rval.getEvent (i);
		
		if (event.getSizeOfRaceSet() >= 0) {
		    ArrayList races = event.getRaceSet ();
		    ArrayList srRaces = new ArrayList ();
		    for (int j = 0; j < races.size (); j ++) {
			Event race = (Event) races.get (j);
			srEvent srRace = rval.getEvent (race.getOwner().getId(), race.getIndex() + 1);
			srRaces.add (srRace);
		    }
		    sre.setRaceSet (srRaces);
		}
		
	    }
	}

	return rval;

    }



    // private methods

    private static void addSend (SrSeqPO seq, srEvent e) {

	int s_pid = e.getCaller ();

	int s_index = e.getCallerVersionNumber () - 1;

	int r_pid = e.getCalled ();

	int r_index = e.getCalledVersionNumber () - 1;

	String channel_name = e.getChannelName ();

	String label = e.getLabel ();

	// add procs

	Process s_proc = seq.addProc (s_pid);

		

	// add send event

	Event event = new Event (Event.SND, s_index, s_proc);

	event.setChannelName (channel_name);

	event.setLabel (label);

	event.setIsEntry (e.getIsEntry()); // uncommented rhc

	event.setTimestamp (e.getVectorTS ());

	

	// for now, just copy the field EventType2

	event.setEventType2 (e.getEventType2 ());

	

	// NEW fields for SBRT

	event.setSourceStateSender (e.getSourceStateSender ());

	event.setSourceStateReceiver (e.getSourceStateReceiver ());

	event.setDestinationStateSender (e.getDestinationStateSender ());

	event.setDestinationStateReceiver (e.getDestinationStateReceiver ());

	

	//s_proc.addEvent (event);

	// NEED to set TIndex

	seq.addEvent(s_pid, event);



	// set partners

	event.setPartner (r_pid, r_index);

    }

    private static void addRecv (SrSeqPO seq, srEvent e) {

	int s_pid = e.getCaller ();

	int s_index = e.getCallerVersionNumber () - 1;

	int r_pid = e.getCalled ();

	int r_index = e.getCalledVersionNumber () - 1;

	String channel_name = e.getChannelName ();

		

	// add procs

	Process r_proc = seq.addProc (r_pid);

		

	// add recv event

	Event event = new Event (Event.RCV, r_index, r_proc);

	event.setChannelName (channel_name);

	event.setLabel (e.getLabel());

	event.setIsEntry (e.getIsEntry ()); // uncommented rhc

	event.setTimestamp (e.getVectorTS ());



	// for now, just copy the field EventType2

	event.setEventType2 (e.getEventType2 ());



	//r_proc.addEvent (event);

	// NEED to set TIndex

	seq.addEvent(r_pid, event);

	    

	// set partners

	event.setPartner (s_pid, s_index);



	// set open list

	setOpenList (event, e);



	// set inhibited callers

	setInhibitedCallers (event, e);



	// NEW fields for SBRT

	event.setSourceStateSender (e.getSourceStateSender ());

	event.setSourceStateReceiver (e.getSourceStateReceiver ());

	event.setDestinationStateSender (e.getDestinationStateSender ());

	event.setDestinationStateReceiver (e.getDestinationStateReceiver ());

	// set recollected flag
	event.setRecollected (e.getIsRecollected());
	
	// NEW field for stateful testing

	event.setDestinationStateRevisited (e.getDestinationStateRevisited ());
    }

    private static void addUnacceptedSend (SrSeqPO seq, srEvent e) {

	int s_pid = e.getCaller ();

	int s_index = e.getCallerVersionNumber () - 1;

	int r_pid = e.getCalled ();

	String channel_name = e.getChannelName ();

		

	// add procs

	Process s_proc = seq.addProc (s_pid);

	Process r_proc = seq.addProc (r_pid);

		

	// add send event

	Event event = new Event (Event.SND, s_index, s_proc);

	event.setReceiver (r_proc);

	event.setChannelName (channel_name);

	event.setLabel (e.getLabel());

	event.setTimestamp (e.getVectorTS ());

	event.setIsEntry (e.getIsEntry ());  // uncommented rhc



	// for now, just copy the field EventType2

	event.setEventType2 (e.getEventType2 ());



	//s_proc.addEvent (event);

	seq.addEvent (s_pid, event);



	// NEW fields for SBRT

	event.setSourceStateSender (e.getSourceStateSender ());

	event.setSourceStateReceiver (e.getSourceStateReceiver ());

	event.setDestinationStateSender (e.getDestinationStateSender ());

	event.setDestinationStateReceiver (e.getDestinationStateReceiver ());

    }

    private static void addUnacceptedRecv (SrSeqPO seq, srEvent e) {

	int pid = e.getCalled ();

	int index = e.getCalledVersionNumber () - 1;

	String channel_name = e.getChannelName ();

	

	Process proc = seq.addProc (pid);

	Event event = new Event (Event.RCV, index, proc);

	event.setChannelName (channel_name);

	event.setLabel (e.getLabel());

	event.setTimestamp (e.getVectorTS ());

	event.setIsEntry (e.getIsEntry ());  // uncommented rhc



	// for now, just copy the field EventType2

	event.setEventType2 (e.getEventType2 ());



	// set open list

	setOpenList (event, e);



	// set inhibited callers

	setInhibitedCallers (event, e);



	//proc.addEvent (event);

	seq.addEvent (pid, event);



	// NEW fields for SBRT

	event.setSourceStateSender (e.getSourceStateSender ());

	event.setSourceStateReceiver (e.getSourceStateReceiver ());

	event.setDestinationStateSender (e.getDestinationStateSender ());

	event.setDestinationStateReceiver (e.getDestinationStateReceiver ());

	// set recollected flag
	event.setRecollected (e.getIsRecollected());
	
	// NEW field for stateful testing

	event.setDestinationStateRevisited (e.getDestinationStateRevisited ());

    }



    private static srEvent translate (Event event) {

	srEvent rval = null;

	if (event.getType () == Event.SND) {

	    Event partner = event.getPartner ();

	    if (partner != null) {

		int caller = event.getOwner().getId ();

		int callerVersionNumber = event.getIndex () + 1;

		int called = partner.getOwner().getId ();

		int calledVersionNumber = partner.getIndex () + 1;

		String channelName = event.getChannelName ();

		int channelVersionNumber = -1;

		vectorTimeStamp ts = event.getTimestamp ();		

		eventTypeParameters.eventType eType = null;

		if (isSynch (event)) {

		    eType = eventTypeParameters.SYNCH_SEND;

		}

		else {

		    eType = eventTypeParameters.ASYNCH_SEND;

		}

		rval = new srEvent (caller, called, 

				    callerVersionNumber, calledVersionNumber,

				    channelName, channelVersionNumber,

				    eType,

				    ts,

				    null, // open list

				    event.getEventType2 ());

	    }

	    else {

		int caller = event.getOwner().getId ();

		int callerVersionNumber = event.getIndex () + 1;

		int called = event.getReceiver().getId ();

		int calledVersionNumber = -1;

		String channelName = event.getChannelName ();

		int channelVersionNumber = -1;

		vectorTimeStamp ts = event.getTimestamp ();

		eventTypeParameters.eventType eType = null;

		if (isSynch (event)) {

		    eType = eventTypeParameters.UNACCEPTED_SYNCH_SEND;

		}

		else {

		    eType = eventTypeParameters.UNACCEPTED_ASYNCH_SEND;

		}

		rval = new srEvent (caller, called, 

				    callerVersionNumber, calledVersionNumber,

				    channelName, channelVersionNumber,

				    eType,

				    ts,

				    null, // NO need to pass an open list back

				    event.getEventType2());

	    }

	}

	else if (event.getType () == Event.RCV) {

	    Event partner = event.getPartner ();

	    if (partner != null) {

		int caller = partner.getOwner().getId ();

		int callerVersionNumber = partner.getIndex () + 1;

		int called = event.getOwner().getId ();

		int calledVersionNumber = event.getIndex () + 1;

		String channelName = event.getChannelName ();

		int channelVersionNumber = -1;

		vectorTimeStamp ts = event.getTimestamp ();

		ArrayList openList = new ArrayList ();

		openList.addAll(event.getOpenList ());

		eventTypeParameters.eventType eType = null;

		if (isSynch (event)) {

		    eType = eventTypeParameters.SR_SYNCHRONIZATION;

		}

		else {

		    eType = eventTypeParameters.ASYNCH_RECEIVE;

		}



		rval = new srEvent (caller, called, 

				    callerVersionNumber, calledVersionNumber,

				    channelName, channelVersionNumber,

				    eType,

				    ts,

				    openList, // openList 

				    event.getEventType2());



		// set inhibited callers

		rval.setInhibitedCallers (event.getInhibitedCallers().getCallerList());

	    }

	    else {

		int caller = -1;

		int callerVersionNumber = -1;

		int called = event.getOwner().getId ();

		int calledVersionNumber = event.getIndex () + 1;

		String channelName = event.getChannelName ();

		int channelVersionNumber = -1;

		vectorTimeStamp ts = event.getTimestamp ();

		ArrayList openList = new ArrayList ();

		openList.addAll(event.getOpenList ());

		rval = new srEvent (caller, called, 

				    callerVersionNumber, calledVersionNumber,

				    channelName, channelVersionNumber,

				    eventTypeParameters.UNACCEPTED_RECEIVE,

				    ts,

				    openList, // open list

				    event.getEventType2 ());

	    }



	    // set inhibited callers

	    rval.setInhibitedCallers (event.getInhibitedCallers().getCallerList());

	}



	// set label

	rval.setLabel (event.getLabel ());

	rval.setIsEntry (event.getIsEntry ());  // uncommented rhc

	rval.setIsRecollected (event.isRecollected());

	

	// set source/destination states for SBRT

	rval.setSourceStateSender (event.getSourceStateSender ());

	rval.setSourceStateReceiver (event.getSourceStateReceiver ());

	rval.setDestinationStateSender (event.getDestinationStateSender ());

	rval.setDestinationStateReceiver (event.getDestinationStateReceiver ());

	return rval;

    }



    // methods to verify the validity of a SrSeqPO object

    // ideally, we need to verify the validity of the srSeq object

    // before translation. however, it is a little involved to do

    // that. we assume the translation is performed correctly. so, if

    // the translated object is invalid, then it follows that the

    // input object is invalid.

    public static boolean isValid (SrSeqPO seq) {

	boolean rval = true;



	// the event index shall be consecutive within a process

	for (int i = 0; i < seq.getNumOfProcs (); i ++) {

	    Process proc = seq.getProc (i);

	    if (proc == null) {

		System.out.println("index: " + i);

		seq.dump ();

	    }

	    if (proc.getId () != i) {

		System.out.println("Internal error: Some procs are missing!");

		rval = false;

		break;

	    }

	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {

		Event event = proc.getEvent (j);

		if (event.getIndex () != j) {

		    System.out.println("Thread " + proc.getId() + 

				       ": Event indices are not consecutive!");

			rval = false;

			break;

		}

	    }

	    if (!rval) break;

	}

	

	if (rval) {

	    // check the dimension of prefix info is the same as the

	    // number of procs

	    int index_len = seq.getPrefixIndex().length;

	    int num_of_procs = seq.getNumOfProcs ();

	    if (index_len != num_of_procs) {

		System.out.println("index_len: " + index_len);

		System.out.println("num_of_procs: " + num_of_procs);



		System.out.println("The dimension of prefix info"

				   + " is not equal to the number of procs!");

		rval = false;

	    }


/*
	    if (rval) {

			// check the value of prefix index must be no greater
	
			// than the length of the event list of each process
	
			int [] prefix_index = seq.getPrefixIndex ();
		
			for (int i = 0; i < prefix_index.length; i ++) {
	
			    //Process proc = seq.getProc (i);

			    //if(prefix_index[i] > proc.getNumOfEvents ()) {

				//System.out.println("Inconsistent prefix index info!");

				//rval = false;

				//break;

			    //} 
	
			}

	    }
*/

	}



	return rval;

    }



    // set the alive flags for the events in the prefix

    private static void setAlive (SrSeqPO porder, srSeq torder) {

	SrSeqPO prefix = torder.getSource ();

	for (int i = 0; i < prefix.getNumOfProcs (); i ++) {

	    Process proc = prefix.getProc (i);

	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {

		Event event = proc.getEvent (j);

		int pid = event.getOwner().getId ();

		int index = event.getIndex ();

		Event it = null;

		try {

		    it = porder.getProc (pid).getEvent(index);

		} catch (Exception e) {

		    System.out.println("Exception: pid is " + pid+ " and j is " + j

				       + " and index is " + index);

		    System.out.println("prefix is: " + prefix);

		    System.out.println("torder is: " + torder);

		    System.out.println("porder is: " + porder);

		}



		//Event it = porder.getProc (pid).getEvent(index);

		if (it != null) {

		    it.setAlive (event.getAlive());

		}

	    }

	}

    }



    // copy open lists

    private static void setOpenList (Event to, srEvent src) {

	to.getOpenList().addAll (src.getOpenList ());

    }



    // copy inhibited caller list

    private static void setInhibitedCallers (Event to, srEvent src) {

	if (src.getInhibitedCallers() != null) {

	    to.getInhibitedCallers().getCallerList().addAll (src.getInhibitedCallers().getCallerList());

	}

    }



    // check if it is a completed send event

    private static boolean isCompletedSend (srEvent e) {

	return e.getEventType ().equals(eventTypeParameters.ASYNCH_SEND)

	    || e.getEventType ().equals(eventTypeParameters.SYNCH_SEND);

    }



    // check if it is a completed receive event

    private static boolean isCompletedReceive (srEvent e) {

	return e.getEventType ().equals(eventTypeParameters.ASYNCH_RECEIVE)

	    || e.getEventType ().equals(eventTypeParameters.SYNCH_RECEIVE)

	    || e.getEventType ().equals(eventTypeParameters.SR_SYNCHRONIZATION);

    }



    // check if it is an unaccepted send event

    private static boolean isUnacceptedSend (srEvent e) {

	return e.getEventType ().equals(eventTypeParameters.UNACCEPTED_ASYNCH_SEND)

	    || e.getEventType ().equals(eventTypeParameters.UNACCEPTED_SYNCH_SEND);

    }



    // check if it is an unaccepted receive event

    private static boolean isUnacceptedReceive (srEvent e) {

	return e.getEventType ().equals(eventTypeParameters.UNACCEPTED_RECEIVE);

    }



    // check if it is a synchronous send or receive event

    private static boolean isSynch (Event e) {

	return e.getEventType2 ().equals(eventTypeParameters.SYNCH_SEND)

	    || e.getEventType2 ().equals(eventTypeParameters.SYNCH_RECEIVE);

    }



}





