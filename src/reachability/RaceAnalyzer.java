package reachability;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.io.FileNotFoundException;
import java.io.IOException;

// compute the race set of each receive event
// the default communication model is ASYN.
public class RaceAnalyzer {
    public final static int ASYN = 0;
    public final static int FIFO = 1;
    public final static int CAUSAL = 2;
    private static int model;
    public static RaceAnalyzer instance = null;
    static private int[] symmetryGroups = null;
  	 static private propertyParameters.SymmetryReduction SymmetryReduce = (propertyReader.getInstance().getSymmetryReductionProperty());


    public static RaceAnalyzer getInstance () {
	synchronized(RaceAnalyzer.class) {
	    if (instance == null) {
		instance = new RaceAnalyzer ();
	    }

	    return instance;
	}
    }

    private RaceAnalyzer () {
	model = FIFO;
	if (SymmetryReduce==propertyParameters.SYMMETRYREDUCTIONON)
		if (symmetryGroups==null)
			readSymmetryGroups();
    }

    public void setModel (int model) {
	this.model = model;
    }

	static private void readSymmetryGroups() {
		try {
			BufferedReader r = new BufferedReader(new FileReader("symmetry.txt"));
			int maxThreads = (propertyReader.getInstance()).getMaxThreadsProperty();
			symmetryGroups = new int[maxThreads+1];
			for (int i=0; i<maxThreads+1; i++)
				symmetryGroups[i] = -1;
			String line; String delim = " ,";
			int groupNumber = 0;
        	while ((line = r.readLine()) != null)  {
            groupNumber++;
            StringTokenizer tokens =  new StringTokenizer(line, delim);    // Read each token:
            while (tokens.hasMoreTokens()){
                String token = tokens.nextToken();
                int ID=0;
                try {
                	ID = Integer.parseInt(token);
                }
                catch (NumberFormatException e) { 
						System.out.println();
						System.out.println("Error: Format error in file symmetry.txt. This file is used");
						System.out.println("to specify groups of symmetric threads. For example:" );
						System.out.println("1 2 3" );
						System.out.println("4 5 6" );
						System.out.println("specifies that threads 1, 2, and 3 are in one group and");
						System.out.println("threads 4, 5, and 6 are in the other. Every thread must" );
						System.out.println("be in one group. Thread identifiers, 1, 2, 3, etc, must"); 
						System.out.println("be numbers. Use the IDs in ThreadID.txt."); 
						System.out.println("The line with the error was:");
						System.out.println("   " + line);
						System.out.println();
						System.exit(1);                
                }
                try {
	                symmetryGroups[ID] = groupNumber;
	             }
	             catch(ArrayIndexOutOfBoundsException e) {
	             	System.out.println();
					  	System.out.println("Error: Invalid thread ID " + ID + " in symmetry.txt.");
					  	System.out.println("The default maximum ID is 15. The minimum ID is 1.");
					  	System.out.println("Use -DmaxThreads=n to raise the maximum ID.");
	  					System.exit(1);
	  				}
            }
          }
			 //for (int j=0; j<maxThreads+1; j++)
			 // System.out.println(j+":"+symmetryGroups[j]);
		}
		catch (FileNotFoundException e) {
			System.out.println();
			System.out.println("Error: File symmetry.txt not found. This file is used to");
			System.out.println("specify groups of symmetric threads. For example:" );
			System.out.println("1 2 3" );
			System.out.println("4 5 6" );
			System.out.println("specifies that threads 1, 2, and 3 are in one group and");
			System.out.println("threads 4, 5, and 6 are in the other. Every thread must" );
			System.out.println("be in one group. Use the IDs in ThreadID.txt. "); 
			System.out.println();
			System.exit(1);
		}
		catch (IOException e) {}
	}

    public int getCommModel () {
	return model;
    }
    public void setCommModel (int model) {
	this.model = model;
    }

    public boolean isRequest (Event event) {
	return event.getType () == Event.SND;
    }

    public boolean isRecv (Event event) {
	return event.getType () == Event.RCV;
    }

    // the method assumes that the SR_sequence is sorted and timestamped
    public void analyze (SrSeqPO seq) {
	// initialize the race set of each receive to be empty
	clear (seq);

	for (int i = 0; i < seq.getNumOfProcs (); i ++) {
	    Process proc = seq.getProc (i);
	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event event = proc.getEvent (j);
		if(isRequest(event)) {
		    // get the receiver process
		    Process receiver = event.getReceiver ();

		    // decide the starting point
		    int k = 0;
		    Event partner = event.getPartner ();
		    if (partner != null) {
			k = partner.getIndex() - 1;
		    }
		    else {
			// for FIFO and CAUSAL we only consider the
			// first non-received event
			if (model == ASYN || isFirstNonReceived (event)) {
			    k = receiver.getNumOfEvents () - 1;
			}
			else {
			    continue;
			}
		    }

		    // partner_proc is sorted
		    for(; k >= 0; k --) {
			Event tmp = receiver.getEvent (k);
			if ( !isRequest(tmp) && tmp.getPartner () != null
			     && isReceivable (tmp, event) && !isInhibited(tmp, event)) {
			    if (model == ASYN) {
				if (tmp.getTimestamp ().lessThan(event.getTimestamp ())) {
				    break;
				}
			    }
			    else if (model == FIFO) {
				Event tmp_partner = tmp.getPartner ();
				if (tmp.getTimestamp().lessThan(event.getTimestamp ())
				    || tmp_partner.getOwner().getId () == event.getOwner().getId()) {
				    break;
				}
			    }
			    else if (model == CAUSAL) {
				Event tmp_partner = tmp.getPartner ();
				if (tmp_partner.getTimestamp().lessThan(event.getTimestamp ()))
				    break;
			    }
			    // add event into the race set of the tmp event
			    tmp.addRaceEvent (event);
			}
		    }
		}
	    } 
	}
	if (SymmetryReduce==propertyParameters.SYMMETRYREDUCTIONON)
	    prune(seq);

	// detect data races
	if (propertyReader.getInstance().getDataRaceProperty()
	    == propertyParameters.DATARACEON) {
	    detectDataRace (seq);
	}
    }

    // check if the event is the first non-received send event that
    // destined to the same channel in its owner process
    private boolean isFirstNonReceived (Event event) {
	boolean rval = true;
	if (!isRequest (event) || (event.getPartner() != null)) {
	    rval = false;
	}
	else {
	    Process proc = event.getOwner ();
	    for (int i = event.getIndex () - 1; i >= 0; i --) {
		Event tmp = (Event) proc.getEvent (i);
		if (isRequest (tmp) && (tmp.getPartner () == null)) {
		    // NEED to deal with the channel names for
		    // non-received send events later.
		    rval = false;
		    break;
		}
	    }
	}
	
	return rval;
    }
    
    // check if the channel of send is on the open list of recv
    public boolean isReceivable (Event recv, Event send) {
	boolean rval = false;
	eventTypeParameters.eventType type2 = send.getEventType2 ();
	if (type2.equals(eventTypeParameters.SEMAPHORE_CALL)
	    || type2.equals(eventTypeParameters.LOCK_CALL)
	    || type2.equals(eventTypeParameters.SYNCH_SEND)
	    || type2.equals(eventTypeParameters.UNACCEPTED_SYNCH_SEND)
 	    || type2.equals(eventTypeParameters.SHAREDVARIABLE_CALL)
	    || type2.equals(eventTypeParameters.ASYNCH_SEND)
	    || type2.equals(eventTypeParameters.UNACCEPTED_ASYNCH_SEND)) {
	    // check if the channel of send is on the open list of recv
	    if (getChannelName (send) == null) {
	    	System.out.println("Null channelName for send: " +
				   send);
		System.exit(1);
	    }
	    else {
	      rval = recv.isOpenChannel (getChannelName (send));
	    }
	}
	else {
	    // for all the other types, we do not need to check the
	    // open list. at least for now.
	  if (!type2.equals(eventTypeParameters.MONITOR_CALL)) {
	   System.out.println("something is wrong " + type2);
	 }
	 rval = true;
	}
	return rval;
    }

    // check if the calling thread of a send event is on the inhibited
    // list of recv 
    public boolean isInhibited (Event recv, Event send) {
	boolean rval = false;
	int callerID = send.getOwner().getId ();
	if (recv.getInhibitedCallers() != null) {
	    rval = recv.getInhibitedCallers().contains(callerID);
	}
	return rval;
    }

    private void clear (SrSeqPO seq) {
	for (int i = 0; i < seq.getNumOfProcs (); i ++) {
	    Process proc = seq.getProc (i);
	    for (int j = 0; j < proc.getNumOfEvents (); j ++) {
		Event event = proc.getEvent (j);
		event.clearRaceSet ();
	    }
	}
    }

    private String getChannelName (Event send) {
	String rval = null;
	eventTypeParameters.eventType type2 = send.getEventType2 ();
	if (type2.equals(eventTypeParameters.SEMAPHORE_CALL)
	    || type2.equals(eventTypeParameters.LOCK_CALL)
	    || type2.equals(eventTypeParameters.SHAREDVARIABLE_CALL)) {
	    // check if the channel of send is on the open list of recv
	    String full_name = send.getChannelName ();
	    rval = full_name.substring (full_name.indexOf (":") + 1);
	    //System.out.println("Full name: " + full_name);
	    //System.out.println("Extract name: " + rval);
	}
	else if (type2.equals(eventTypeParameters.SYNCH_SEND)
		 || type2.equals(eventTypeParameters.UNACCEPTED_SYNCH_SEND)
		 || type2.equals(eventTypeParameters.ASYNCH_SEND)
		 || type2.equals(eventTypeParameters.UNACCEPTED_ASYNCH_SEND)) {
	  rval = send.getChannelName ();
	}

	return rval;
    }
    
    private void prune (SrSeqPO seq) {

	// dump the sequence with race set information before prunning
	//System.out.println("Before pruning:");
	//seq.dump ();
	for (int i = 0; i < seq.getNumOfProcs (); i ++) {
	   Process proc = seq.getProc (i);
	   for (int j = 0; j < proc.getNumOfEvents (); j ++) {
 		 Event event = proc.getEvent (j);
		
		//if (!((event.getChannelName()).equals("replyChannel0") || 
  		//   (event.getChannelName()).equals("replyChannel1")  ||
  		//   (event.getChannelName()).equals("replyChannel2")))
  		// continue;
  		
		// get the group id of its partner
		Event partner = event.getPartner ();
		int partner_gid = -1;
		if (partner != null) {
			try {
		    partner_gid = symmetryGroups[partner.getOwner().getId ()]; // getGroupId (partner.getOwner().getId ());
		   } catch(ArrayIndexOutOfBoundsException e) {
		  		System.out.println("Error: Too many threads/synchronization objects. The default maximum is 15.  ");
		  		System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  		System.exit(1);
		  	  }
		}
		// prune the race set
		ArrayList races = event.getRaceSet ();
		ArrayList pruned = new ArrayList ();
		for (int k = 0; k < races.size(); k ++) {
		    Event race = (Event) races.get(k);
		    int race_gid = -1;
			 try {
		   	race_gid = symmetryGroups[race.getOwner().getId ()]; // getGroupId (race.getOwner().getId ());
		    	if (race_gid == -1) {
		  			System.out.println("Error: The symmetry group for thread " + (race.getOwner().getId()) + " is ");
		  			System.out.println("not specified in file symmetry.txt.");
		  			System.exit(1);		    
		    	}
		    } catch(ArrayIndexOutOfBoundsException e) {
		  		System.out.println("Error: Too many threads/synchronization objects. The default maximum is 15.  ");
		  		System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  		System.exit(1);
		  	 }
		    // the current race event should be pruned if it
		    // is in the same group as the partner event
		    // Note: un-prunded events are added to list prune
			 //System.out.println("partner:"+partner.getChannelName()+","+partner.getLabel());
			 //System.out.println("race:"+race.getChannelName()+","+race.getLabel());
		    if ((partner_gid != -1 && partner_gid == race_gid) &&
		    		(partner.getChannelName().equals(race.getChannelName()))
		    		&& (partner.getLabel().equals(race.getLabel()))  ) {
					//System.out.println("Same name: " + partner.getChannelName() + " and same group:"+partner_gid);
					continue; // current race event in same group so don't add it to pruned; get next race event
		    }
		    // remove all the events, but one, in the same group
		    // Note: current race event is not in same group, add it to pruned unless there is 
		    //       already a race event in pruned that is in the same group
		    boolean flag = true;
		    for (int l = 0; l < pruned.size (); l ++) {
				Event prune = (Event) pruned.get(l);
			 	//System.out.println("prune:"+prune.getChannelName()+","+prune.getLabel());
				//System.out.println("race:"+race.getChannelName()+","+race.getLabel());
				int prune_gid = -1;
				try {
		    		prune_gid = symmetryGroups[prune.getOwner().getId()]; // getGroupId (prune.getOwner().getId ());
		    		if (prune_gid == -1) {
		  				System.out.println("Error: The symmetry group for thread " + (prune.getOwner().getId()) + " is ");
		  				System.out.println("not specified in file symmetry.txt.");
		  				System.exit(1);		    
		    		}
		   	} catch(ArrayIndexOutOfBoundsException e) {
		  			System.out.println("Error: Too many threads/synchronization objects. The default maximum is 15.  ");
		  			System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  			System.exit(1);
		  	  	  }
				if (prune_gid == race_gid && 
						prune.getChannelName().equals(race.getChannelName())	
						&& (prune.getLabel().equals(race.getLabel()))
						) {
					//System.out.println("Same name: " + prune.getChannelName() + " and same group:"+prune_gid);
			    	flag = false; // already a race event in the same group so don't add this one to pruned too
			    	break;
				}
		    }
		    if (flag) {
				//System.out.println("Add into pruned: " + race);
				pruned.add(race);
		    }
		}
		// transfer events from "pruned" to "races"
		races.clear ();
		for (int k = 0; k < pruned.size (); k ++) {
		    races.add (pruned.get(k));
		}
    }
	}
	// dump the sequence with race set information after pruning
	//System.out.println("After pruning:");
	//seq.dump ();
    }    

    // detect data race for shared variables
    void detectDataRace (SrSeqPO seq) {
		for (int i = 0; i < seq.getNumOfProcs (); i ++) {
	   	Process proc = seq.getProc (i);
	    	for (int j = 0; j < proc.getNumOfEvents (); j ++) {
				Event event = proc.getEvent (j);
				if (event.getType() == Event.RCV) {
		    		if (event.getEventType2 () == eventTypeParameters.SHAREDVARIABLE_COMPLETION) {
						if (event.getSizeOfRaceSet() > 0) {
							System.out.println();
			    			System.out.println("Data race detected for event " + event.prettyPrint() + ", event "
			    			           + (event.getPartner().getIndex()+1) + " of Thread " 
			                       + event.getPartner().getOwner().getId() + ", and event "
			                       + event.getRaceEvent(0).prettyPrint() + ", event " + (event.getRaceEvent(0).getIndex()+1)
			                       + " of Thread " + event.getRaceEvent(0).getOwner().getId() +
			                       ", in the sequence:");
			    			System.out.println((SrSeqTranslator.convert (seq)).prettyPrint());
			    			System.out.println();
			    			System.out.println("Thread IDs and names are listed in file ThreadID.txt");
			    			System.exit (1);
						}
		    		}
				}
	    	}
		}
    }
    
}




