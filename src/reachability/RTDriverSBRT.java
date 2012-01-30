package reachability;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

public final class RTDriverSBRT implements eventTypeParameters, propertyParameters {
// the driver for RT
	private static srEventPool eventPool = new srEventPool(50);
	private static IntegerSpace intSpace = new IntegerSpace(10);
	private static boolean mappedSequence = true;
	private static int count = 0;
	private static int collected = 0;
	private static int mediumCount=0;
	private static SBRTWatchDog watchDog;
	private static boolean sendEmail = false;
   private static sendMail t;
	private static sequenceCollector collector;
	private static propertyParameters.ClientServer clientServer = propertyParameters.StandAlone;
	private static propertyParameters.ModularTesting modularTesting = propertyParameters.MTOFF;
	private static int modularTestingThreads = 0;
   private static int hash = propertyReader.getInstance().getHashProperty(); // progress displayed every hash executions, user can set this value
   private static int numWorkers;

   private static PrintStream out;
   private static String myID;
	
	public static void main (String args[]) {
		ArrayList fileNames = new ArrayList();
		ArrayList LTS = new ArrayList();
  		clientServer = propertyReader.getInstance().getClientServerProperty();
      if (clientServer != propertyParameters.StandAlone) {
		   myID = args[0];
		   try {
		      out = new PrintStream(new FileOutputStream("outDME4-"+myID+".txt"));
		   }
		   catch (Exception e) {e.printStackTrace(); System.exit(1);}
		   if (clientServer == propertyParameters.Server) {
		   	// need numWorkers if saving modular test sequences
        		numWorkers = propertyReader.getInstance().getNumWorkersProperty();
        	}
		}
		else {
			out = System.out;
			myID = "";
		}
		modularTesting = propertyReader.getInstance().getModularTestingProperty();		
		if (modularTesting == propertyParameters.MTON)
		  modularTestingThreads = propertyReader.getInstance().getModularTestingThreadsProperty();	
		  
		if (sendEmail)
			t = new sendMail();
		int numLTS = 0;
		StringBuffer name = new StringBuffer(); // used to hold various names.
		try {
		
			//ObjectInputStream inputInfeasibleSequence = new ObjectInputStream(new FileInputStream("sequence.dat"));
			//ArrayList srSequence = (ArrayList)inputInfeasibleSequence.readObject();
			//System.exit(1);
	  		clientServer = propertyReader.getInstance().getClientServerProperty();
			watchDog = new SBRTWatchDog(); watchDog.start();
			if (modularTesting == propertyParameters.MTON) {
				collector = new sequenceCollector();
			}
			
			//FileInputStream fstream = new FileInputStream("LTSFiles.txt");
			//DataInputStream in = new DataInputStream(fstream);
			FileReader fstream = new FileReader("LTSFiles.txt");
			BufferedReader in = new BufferedReader(fstream);
			
			//while (in.available()!=0) {
			String fileName;
			while ((fileName = in.readLine()) != null) {
				//String fileName = in.readLine();
				fileNames.add(fileName);
			}
			numLTS = fileNames.size();
			Graph nullGraph = new Graph();
			//nullGraph.setTerminating();
			nullGraph.ID = 0;
			LTS.add(nullGraph);
			for (int i=0; i<numLTS; i++) {
				Graph g = new Graph();
				g.inputFileName = (String) fileNames.get(i);
				g.ID = i+1;
				if ((g.inputFileName.indexOf("MEDIUM")>=0 || g.inputFileName.indexOf("Medium")>=0 || 
    	           g.inputFileName.indexOf("medium")>=0)) {
    	      	g.isMedium = true;
    	      	mediumCount++;
    	      }
    	      out.println(g.inputFileName);
				g.input();
				//g.output();
				LTS.add(g);
	/*
				Iterator p = g.eventsToThreads.entrySet().iterator();
				while (p.hasNext()) {
					Map.Entry e = (Map.Entry) p.next();
					//System.out.println(e.getKey()+":");
					LinkedList threads = (LinkedList) e.getValue();
					Iterator threadItr = threads.iterator();
					while (threadItr.hasNext()) {
						eventInfo f = (eventInfo) threadItr.next();
						//System.out.println(f.ID+","+f.source+"--"+f.label
							+"->"+f.destination);
					}
				}
	*/
			}
		} catch (Exception e) {};
	
/**/

out.println("mediumCount is " + mediumCount);
   VariantGenerator generator = VariantGenerator.getInstance();
   
	HashMap recollectedChannels = new HashMap(); // channelNames->receivingThread of recollected receives
	HashMap recollectedThreads = new HashMap(); // receiving threads of recollected receive -> OpenList of recoll. receive
	//HashSet recollectedPermitted = new HashSet(); // recollected recieves permitted but not yet executed
	HashSet recollectedNoOldOrNewSends = new HashSet(); // recollected recieves with no old or new sends 
	HashSet recollectedWithOldPending = new HashSet(); // thread IDs for threads with a recollected event that has an
	                                                   // old event pending. If no new sends and pending old then no deadlock
	HashMap recollectedOldSenders = new HashMap(); // IDs of threads in variant that are issuing old Sends
	HashMap messages = new HashMap(); // each thread has a list of outstanding messages containing senders version number
	                                  // so the receiver can grab it on receive events
	LinkedList inhibited = new LinkedList(); // old transitions that are still disabled in new state

   out.println("start:" + java.util.Calendar.getInstance().getTime());

   ArrayList nonTerminatedLTS = new ArrayList(numLTS+1);
	ArrayList startStates = new ArrayList(numLTS+1);
	int[] versionNumbers = new int[numLTS+1];
	vectorTimeStamp[] vectorTimeStamps = new vectorTimeStamp[numLTS+1];
	/* srSequence is the variant */
	ArrayList variantsrSequence = /* variant.getEvents();*/ new ArrayList();
	/* sequence is the trace */
	//ArrayList tracedsrSequence = new ArrayList();
	
	propertyParameters.SBRTMode sbrtMode = PBRT;
	
	sbrtMode = (propertyReader.getInstance().getSBRTModeProperty());
	
	mutableBoolean cycleState = new mutableBoolean(false); // set to true when a cycle is detected
	mutableBoolean atLeastOne = new mutableBoolean(false); 
	mutableBoolean potentialMatch = new mutableBoolean(false); 
	stateVector globalState = null;
	stateVector currentState = null;
	HashSet globalStates = null;
	HashMap statesToEvents = null;
	if (sbrtMode == CycleDetection) {
		globalState = new stateVector(numLTS+1); // initial global state is all 0's
		globalStates = new HashSet();
		statesToEvents = new HashMap();
	}
	
	for (int i=0; i<numLTS+1; i++) {
		versionNumbers[i]=1;
		vectorTimeStamps[i] = new vectorTimeStamp(i);
	}
	
	int numNonTerminating=0;
	Iterator n = LTS.iterator();
	while (n.hasNext()) {
		if (!((((Graph)n.next())).isTerminating())) {
				numNonTerminating++; // LTSs with terminal state are expected to terminate
		}
	}
	out.println("Number of non-terminating LTSs: " + numNonTerminating);

   //int count=0;
   boolean RTAllRecollected = false;
	while (true) {
		//srSeq variant = new srSeq();
		boolean added;
		if (sbrtMode == CycleDetection) {
			added = globalStates.add((stateVector)globalState.clone());
			currentState = (stateVector)globalState.clone();
		}

		srSeq variant;
		ArrayList tracedsrSequence;
		boolean nameFound = false;
		int variantSize = 0;
		boolean validVariant = true;
		do {
		
		//System.out.println("start do");
		RTAllRecollected = false;
		for (int i=0; i<numLTS+1; i++)  {
			versionNumbers[i]=1;
			vectorTimeStamps[i].reset(i);
		}
		if (sbrtMode == CycleDetection) {
			globalState.reset();
			globalStates.clear();
			statesToEvents.clear();
		}
		
		recollectedChannels.clear(); 
		recollectedThreads.clear();
		recollectedNoOldOrNewSends.clear();
		recollectedWithOldPending.clear();
		recollectedOldSenders.clear();

		if (sbrtMode == CycleDetection) {
			globalState = new stateVector(numLTS+1); // initial global state is all 0's
			globalStates = new HashSet();
			statesToEvents = new HashMap();
		}
		
		validVariant = true;
		//System.out.println("Get Variant.");
		variant = generator.getVariant ();
		if (variant==null) {
			out.println("variant is null");
			out.flush();
		}
		

		variantsrSequence = variant.getEvents(); /*new ArrayList();*/
	   //if (variantsrSequence.size() != 0) 
	   //  System.exit(1);
	     
		//variantsrSequence.clear();
		
		if (sbrtMode == CycleDetection) {
			inhibited.clear();
		}
		
/* Jeff Q: */
/* Are we done with srEvents in variantsrSequence (or are events in tracedsrSequence used in variants)? */
/* If we are done, should we release them here? */
		


		
//1
			//System.out.println("variant is:");
		   //for(int i=0; i< variantsrSequence.size(); i++)
		   //	System.out.println((srEvent)variantsrSequence.get(i));

		//if (variantsrSequence.size() == 0) {
		//	System.out.println("Sequence size is " + variantsrSequence.size());
		//}
		//else
		//	System.exit(1);
		
		   //System.out.println(variant);
			//System.exit(0);
		//}
		//if (variantsrSequence.size() != 0) 
	   //  System.exit(1);
	
		int numRecollected=0;
		RTAllRecollected = false;
		for (int i = variantsrSequence.size()-1; i >=0; i--) {
			srEvent e = (srEvent) variantsrSequence.get(i);
			if (e.getIsRecollected()) { // true if e is recollected and has no send partner
				// note: a recollected event e that has its send partner changed
				// has e.getIsRecollected() == false, so we treat it as a regular receive
				// i.e., that is allowed to receive the send it is matched with
				numRecollected++;
				//if (e.getIsEntry() == false) {
					//***recollectedThreads.put(new Integer(e.getCalled()),e.getOpenList());
					recollectedThreads.put(intSpace.get(e.getCalled()),e);
	 				recollectedNoOldOrNewSends.add(intSpace.get(e.getCalled()));
					//String label = e.getLabel();
/*relabel 2*/
//					String newLabel = label.substring(0,label.indexOf("!")-1)+"R"+label.substring(label.indexOf("!"),label.length()); // .replaceAll("_S","_R");
//					String newLabel = label.substring(0,label.indexOf("!")-1)+"S"+label.substring(label.indexOf("!"),label.length());
					//System.out.println("new label for recollected event is " + newLabel);
				 	ArrayList OpenList = e.getOpenList();
					for (int j=0; j<OpenList.size(); j++) {
	  					//recollectedChannels.put(e.getChannelName(),intSpace.get(e.getCalled()));
	  					if (!recollectedChannels.containsKey(((OpenEvent)OpenList.get(j)).getLabel())) {
		  					//System.out.println("adding channel to recollectedChannels:" +((OpenEvent)OpenList.get(j)).getLabel());
			  				recollectedChannels.put(/*newLabel*//*label*/((OpenEvent)OpenList.get(j)).getLabel(),intSpace.get(e.getCalled()));
			  			}
		  			}
		  			//System.out.println("recollected event found:" + e);
	  			//}
			}
			else {
				//System.out.println(((variantsrSequence.size()-1)-i)+ 
				//	" recollected event(s) found");
				break;
			}
		}
		
		// copy variant events to tracedsrSequence (the sequence of collected events) and
		// detect any old send to recollected events

		startStates.clear();
		if (variantsrSequence.size() != 0) {
			startStates = variant.getSource().getSBRTStartStates();
	      //System.out.println("startStates is");
	      //for (int i=0; i<startStates.size(); i++)
		   //   System.out.println(startStates.get(i));
			//System.out.println("startStates.size() is " + startStates.size());
			// No: need to count mediums above
			if (startStates.size() + mediumCount != numLTS+1) {
				out.println("internal error: starStates.size() + mediumCount != numLTS+1.");
				System.exit(1);
			}
			//startStates = variant.getStartStates();
		}
		else {
			//startStates = new ArrayList(numLTS);
			for (int i=0; i<numLTS+1; i++)
				startStates.add(intSpace.get(0));	
		}
		for (int i = 0; i<startStates.size(); i++) {
			int currentLocalState = ((Integer) startStates.get(i)).intValue();
			//System.out.println("startState["+i+"]="+currentLocalState);
			((Graph) LTS.get(i)).setCurrentState(currentLocalState);
		}
		if (mappedSequence) {
			// set mediums
			for (int i = startStates.size(); i<numLTS+1; i++) {
				//System.out.println("startState["+i+"]="+currentState);
				((Graph) LTS.get(i)).setCurrentState(0);
			}		
		}

		//for (int i = 0; i<numLTS+1; i++) {
		//		System.out.println("LTS " +  i + " starting in state: "+((Graph) LTS.get(i)).getCurrentState());		
		//}
		
//if (variantsrSequence.size() != 0) 
 //  System.exit(1);
 
	   //tracedsrSequence.clear();
   	tracedsrSequence = new ArrayList();
		nameFound = false;
		cycleState.set(false);
		variantSize = variantsrSequence.size() - numRecollected;
		
		for (int i=0; i < variantsrSequence.size() - numRecollected; i++) {
			srEvent e = (srEvent) variantsrSequence.get(i);
			//System.out.println("e is " + e);
			
			
			if (mappedSequence) {
				if (e.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
				// if this old send can be synchronized with a recollected receive
				// remove the receiving thread's ID from approp. sets . 
				// (All threads that remain in these sets at the end have no old or new sends)
					tracedsrSequence.add(e);
					//System.out.println("checking recollectedChannels for " + e.getChannelName());
 					if (recollectedChannels.containsKey(e.getChannelName())) {
						//System.out.println("recollectedChannels.e.getChannelName()):" + e.getChannelName());
						// this is an old send; can it be synched with recollected receive?
						// yes if asynch or synch w/ no select. With select depends on OpenList?
						Integer ID = (Integer) recollectedChannels.get(e.getChannelName());;
						srEvent r = (srEvent) recollectedThreads.get(ID);
					 	ArrayList OpenList = r.getOpenList();
					 	name.append(e.getChannelName());

					 	for (int j=0; j<OpenList.size(); j++) {
					 		//System.out.println("comparing:" + name + " and " + ((OpenEvent)OpenList.get(j)).getLabel());
					 	 	if ((name.toString()).equals(((OpenEvent)OpenList.get(j)).getLabel())) {
					 	 		nameFound = true;
					 	 		break;
					 	 	}
				 		}
					 	name.delete(0,name.length());
						if (nameFound) {
						   //System.out.println("found name in openList"); 
						   //System.exit(1);
							nameFound = false;
					    	recollectedNoOldOrNewSends.remove(ID); // okay if 2nd remove => noop
					    	//System.out.println("Add " + ID + " to recollectedWithOldPending");
						 	recollectedWithOldPending.add(ID); // old send pending for thread ID; okay if 2nd add (ignored)
                     /* Record ID of sender so know sender is executing an old send */
							if (!(recollectedOldSenders.containsKey(ID))) { // put sender in map
			   				//System.out.println("new linked list for " + ID);
								recollectedOldSenders.put(ID,new LinkedList());
							}
							//else System.out.println("no new linked list for " + ID);
							LinkedList oldSenders = (LinkedList)recollectedOldSenders.get(ID);
							oldSenders.addLast(intSpace.get(e.getCaller())); // add sender's ID to end of list
							//recollectedOldSenders.add(intSpace.get(e.getCaller()));
							//System.out.println("recollectedNoOldOrNewSends size is " 
							//	+ 	recollectedNoOldOrNewSends.size());
					 	} // nameFound
					} // if recollectedChannels contains name 
					//System.out.println("recollectedNoOldOrNewSends size is " 
					//	+ 	recollectedNoOldOrNewSends.size());
					
               // track caller's version number so can grab it on receives
               // and can set called version number in send event when receive occurs
               Integer callerID = intSpace.get(e.getCaller());
					if (!(messages.containsKey(callerID))) { // put sender in map
						//System.out.println("new messages linked list for " + ID);
						messages.put(callerID,new LinkedList());
					}
					//else System.out.println("no new messages linked list for " + ID);
					LinkedList sends = (LinkedList)messages.get(callerID);
					//sends.addLast(intSpace.get(e.getCallerVersionNumber())); 
					sends.addLast(e); 					
	            versionNumbers[e.getCaller()] = e.getCallerVersionNumber()+1;
					vectorTimeStamps[e.getCaller()].updateVectorTS(e.getVectorTS());
					
					// update medium's state
/*					
					String aBar = e.getLabel();
					String newLabel = aBar.substring(0,aBar.indexOf("!")-1)+"S"+aBar.substring(aBar.indexOf("!"),aBar.length()); // .replaceAll("_R","_S");
					//System.out.println("aBar is " + aBar);
					//System.out.println("new label is " + newLabel);
					LinkedList aBarEvents = (LinkedList) Graph.eventsToThreads.get(newLabel);
					if (aBarEvents == null) {
						out.println("Error: no threads execute event " + aBar);
						System.exit(1);
					}
					//			
					//if (a is for a send) then aBarEvents can have only one thread
					//			
					// aBarEvents is a list of potentially matching events of a.getLabel()
					Iterator j = aBarEvents.iterator();
					boolean found = false;
					eventInfo otherEvent = null;
					Graph otherGraph = null;
					while (j.hasNext()) {
						otherEvent = (eventInfo)j.next();
						//System.out.println("otherEvent: "+otherEvent);
						// Q:check here otherEvent.ID != firstID, i.e., don't send msg to yourself //
						otherGraph = (Graph) LTS.get(otherEvent.ID);
						//System.out.println("check for match with " + otherEvent.ID+ " in state " + otherEvent.source);
						//System.out.println(otherEvent.ID + " currently in state " + otherGraph.getCurrentState());
						//System.out.println("otherGraph.isMedium is " + otherGraph.isMedium());
						// Note: if otherGraph is a medium, it is not the same graph as this graph
						if (otherGraph.isMedium() && otherGraph.getCurrentState() == otherEvent.source
						    && e.getCaller() == otherEvent.C && e.getCalled() == otherEvent.U 
						    //&& e.getChannelName() == otherEvent.channelName 
						    && (e.getEventType().toString().equalsIgnoreCase(otherEvent.eventType)
						       ||(e.getEventType().toString().equals("unaccepted_asynch_send") && otherEvent.eventType.equals("ASYNCH_SEND")))) {
						 	found = true;
							break;						
						}
					}
					if (!found) {
						out.println("medium not found for update");
						System.exit(1);
					}
					otherGraph.setCurrentState(otherEvent.destination);
					//System.out.println("otherEvent.destination is " + otherEvent.destination);
*/
					{ // open block
						String aBar = e.getLabel();
						String newLabel = aBar.substring(0,aBar.indexOf("!")-1)+"S"+aBar.substring(aBar.indexOf("!"),aBar.length()); // .replaceAll("_R","_S");
						//System.out.println("for ASYNCH_SEND medium update, aBar is " + aBar + " and newLabel is " + newLabel);
						HashMap threadMap = (HashMap) Graph.eventsToTransitions.get(newLabel);	
						if (threadMap == null) {
							out.println("Error: no threads execute event " + newLabel);
							System.exit(1);
						}
						if (threadMap.size() != 2) {
							out.println("Error: more than 2 threads synchronize on asynch_send " + newLabel);
							System.exit(1);
						}
	
						Set s = threadMap.entrySet();
						Iterator iMap = s.iterator();
						boolean found = false;
						eventInfo otherEvent = null;
						Graph otherGraph = null;
						while (iMap.hasNext()) {
							Map.Entry entry = (Map.Entry) (iMap.next());
							Integer ID = (Integer) entry.getKey();
							//System.out.println("ID is " + ID);
							otherGraph = (Graph) /*nonTerminated*/LTS.get(ID.intValue());
							if (otherGraph.isMedium()) {
								HashMap stateMap = (HashMap) entry.getValue();
								//System.out.println("otherGraph.getCurrentState() " + otherGraph.getCurrentState());
								otherEvent = (eventInfo) stateMap.get(new Integer(otherGraph.getCurrentState()));
								//System.out.println(otherEvent);
								//if (otherEvent.destination != otherEvent.destination) {
								//	System.out.println("no destination match");
								//	System.exit(1);
								//}
								found = true;
								break;
							}
							}	
						if (!found) {
							out.println("medium not found for new update");
							System.exit(1);
						}
						otherGraph.setCurrentState(otherEvent.destination);
						// No, since for changed send the source/destination will be different?
						//if (e.getEventType().equals(ASYNCH_SEND) && otherEvent2.destination != e.getDestinationStateReceiver()) {
						//	System.out.println("dest. state of found medium " + otherEvent2.destination + " != destination state of receiver in asynch_send event " + e.getDestinationStateReceiver() );
						//	System.exit(1);		
						//}
					} // close block
				} // unnaccepted_asynch_Send
				else { // not unnaccepted_asynch_send
					tracedsrSequence.add(e); 
					//System.out.println("added: " + e);
					LinkedList savedExercisedTransitions = null;
					if (e.getEventType().equals(ASYNCH_SEND) ||
				       e.getEventType().equals(ASYNCH_RECEIVE)){
						if (e.getEventType().equals(ASYNCH_SEND)) {
						   // track caller's version number so can grab it on receives
               		Integer callerID = intSpace.get(e.getCaller());
							if (!(messages.containsKey(callerID))) { // put sender in map
								//System.out.println("new messages linked list for " + callerID);
								messages.put(callerID,new LinkedList());
							}
							//else System.out.println("no new messages linked list for " + callerID);
							LinkedList sends = (LinkedList)messages.get(callerID);
							//sends.addLast(intSpace.get(e.getCallerVersionNumber())); 
							sends.addLast(e);
							//System.out.println("added e");
			            versionNumbers[e.getCaller()] = e.getCallerVersionNumber()+1;
							vectorTimeStamps[e.getCaller()].updateVectorTS(e.getVectorTS());
						}
					  	else if (e.getEventType().equals(ASYNCH_RECEIVE)) {
					  		String label = e.getLabel();
					  		//System.out.println("label is " + label);
					  		//System.out.println("label.indexOf(!)-1 is " + (label.indexOf("!")-1));
		
/*relabel 3*/
					  		//String newLabel = label.substring(0,label.indexOf("!")-1)+"R"+label.substring(label.indexOf("!"),label.length()); // .replaceAll("_S","_R");
					  		//System.out.println("new label is " + newLabel);
					  		//e.setLabel(newLabel);
					  		//e.setChannelName(newLabel);
		  					//System.out.println("variant e is " + e);
	              		Integer callerID = intSpace.get(e.getCaller());
							if (!(messages.containsKey(callerID))) {
								out.println("internal error: no message linked list for " + callerID);
								System.exit(1);
							}
							LinkedList sends = (LinkedList)messages.get(callerID);
							if (sends.size() == 0) {
								out.println("internal error: empty version number list for caller " + callerID);
								System.exit(1);
							}
							ListIterator L = sends.listIterator();
							boolean found = false;
							srEvent sendEvent = null;
							while (L.hasNext()) {
								srEvent s = (srEvent) L.next();
								if (s.getCalled() == e.getCalled()) {
									found = true;
									sendEvent = s;
									L.remove();
									break;									
								}
							}
							if (!found) {
								out.println("internal error: processing variant asynch_receive, no send event found for caller  " + callerID + " and called " + e.getCalled());
								System.exit(1);					
							}
/* Need to compute timestamps, etc */
							//Integer versionNumber = (Integer) sends.removeFirst(); 
							//sendEvent = (srEvent) sends.removeFirst(); 
							//System.out.println("removed event for " + callerID);
							// in variant, all asynch_receives are synched with accepted sends
							//if (versionNumber.intValue() != e.getCallerVersionNumber()) {
							if (!sendEvent.getEventType().equals(ASYNCH_SEND)) {
								out.println("internal error: received asynch_send in variant is not a type ASYNCH_SEND event"
								   + ", it is a type " +  sendEvent.getEventType());
								System.exit(1);
							}
							if (!sendEvent.getEventType2().equals(ASYNCH_SEND)) {
								out.println("internal error: received asynch_send in variant is not a type2 ASYNCH_SEND event."
								+ ", it is a type " +  sendEvent.getEventType2());
								System.exit(1);
							}
							if (sendEvent.getCallerVersionNumber() != e.getCallerVersionNumber()) {
								out.println("internal error: caller version number mismatch on asynch_receive");
								System.exit(1);
							}
							if (sendEvent.getCalledVersionNumber() != e.getCalledVersionNumber()) {
								out.println("internal error: called version number mismatch on asynch_receive");
								System.exit(1);
							}
		         	   versionNumbers[e.getCalled()] = e.getCalledVersionNumber()+1;
							vectorTimeStamps[e.getCalled()].updateVectorTS(e.getVectorTS());		  
						}
				   	/* SKIPPED cycle detection stuff here; see below */
				   	
						// update medium's state
/*						
						LinkedList aBarEvents;
						if (e.getEventType().equals(ASYNCH_SEND)) {
							String aBar = e.getLabel();
							String newLabel = aBar.substring(0,aBar.indexOf("!")-1)+"S"+aBar.substring(aBar.indexOf("!"),aBar.length()); // .replaceAll("_R","_S");
							//System.out.println("for ASYNCH_SEND medium update, aBar is " + aBar + " and newLabel is " + newLabel);
							aBarEvents = (LinkedList) Graph.eventsToThreads.get(newLabel);	
							if (aBarEvents == null) {
								out.println("Error: no threads execute event " + newLabel);
								System.exit(1);
							}
						}
						else {
							String aBar = e.getLabel();
							//System.out.println("for ASYNCH_RECEIVE medium update, aBar is " + aBar);
							aBarEvents = (LinkedList) Graph.eventsToThreads.get(aBar);
							if (aBarEvents == null) {
								out.println("Error: no threads execute event " + aBar);
								System.exit(1);
							}
						}

						//			
						//if (a is for a send) then aBarEvents can have only one thread
						//			
						// aBarEvents is a list of potentially matching events of e.getLabel()
						Iterator j = aBarEvents.iterator();
						boolean found = false;
						eventInfo otherEvent = null;
						Graph otherGraph = null;
						while (j.hasNext()) {
							otherEvent = (eventInfo)j.next();
							//System.out.println("otherEvent: "+otherEvent);
							// Q:check here otherEvent.ID != firstID, i.e., don't send msg to yourself 
							otherGraph = (Graph) LTS.get(otherEvent.ID);
							//System.out.println("check for match with " + otherEvent.ID+ " in state " + otherEvent.source);
							//System.out.println(otherEvent.ID + " currently in state " + otherGraph.getCurrentState());
							if (otherGraph.isMedium() && otherGraph.getCurrentState() == otherEvent.source
							    && e.getCaller() == otherEvent.C && e.getCalled() == otherEvent.U 
							    //&& e.getChannelName() == otherEvent.channelName 
							    && e.getEventType().toString().equalsIgnoreCase(otherEvent.eventType)) {
								 	found = true;
									break;						
							}
						}
						if (!found) {
							out.println("medium not found for update");
							System.exit(1);
						}
						int saveState = otherGraph.getCurrentState();
						otherGraph.setCurrentState(otherEvent.destination);
*/
						// use event info for check: if works, can use it directly instead of finding medium
						// No: destination states may change due to changing the send for a receive
						//if (e.getEventType().equals(ASYNCH_SEND)) {
						//Q: need to check whether g or other is medium?
						//	if (otherEvent.destination != e.getDestinationStateReceiver()) {
						//		System.out.println("dest. state of found medium != destination state of receiver in asynch_send event");
						//		System.exit(1);		
						//	}
						//}
						//else if (e.getEventType().equals(ASYNCH_RECEIVE)) {
						//	if (otherEvent.destination != e.getDestinationStateSender()) {
						//		System.out.println("dest. state of found medium != destination state of sender in asynch_receive event");
						//		System.exit(1);		
						//	}						
						//}
						//System.out.println("otherEvent.destination is " + otherEvent.destination);
						{ // open block
							HashMap threadMap = null;
							if (e.getEventType().equals(ASYNCH_SEND)) {
								String aBar = e.getLabel();
								String newLabel = aBar.substring(0,aBar.indexOf("!")-1)+"S"+aBar.substring(aBar.indexOf("!"),aBar.length()); // .replaceAll("_R","_S");
								//System.out.println("for ASYNCH_SEND medium update, aBar is " + aBar + " and newLabel is " + newLabel);
								threadMap = (HashMap) Graph.eventsToTransitions.get(newLabel);	
								if (threadMap == null) {
									out.println("Error: no threads execute event " + newLabel);
									System.exit(1);
								}
								if (threadMap.size() != 2) {
									out.println("Error: more than 2 threads synchronize on asynch_send " + newLabel);
									System.exit(1);
								}
							}
							else {
								String aBar = e.getLabel();
								//System.out.println("for ASYNCH_RECEIVE medium update, aBar is " + aBar);
								threadMap = (HashMap) Graph.eventsToTransitions.get(aBar);	
								if (threadMap == null) {
									out.println("Error: no threads execute event " + aBar);
									System.exit(1);
								}
								if (threadMap.size() != 2) {
									out.println("Error: more than 2 threads synchronize on asynch_receive " + aBar);
									System.exit(1);
								}
					
							}	
							Set s = threadMap.entrySet();
							Iterator iMap = s.iterator();
							boolean found = false;
							eventInfo otherEvent = null;
							Graph otherGraph = null;
							while (iMap.hasNext()) {
								Map.Entry entry = (Map.Entry) (iMap.next());
								Integer ID = (Integer) entry.getKey();
								//System.out.println("ID is " + ID);
								otherGraph = (Graph) /*nonTerminated*/LTS.get(ID.intValue());
								if (otherGraph.isMedium()) {
									HashMap stateMap = (HashMap) entry.getValue();
									//System.out.println("otherGraph.getCurrentState() " + otherGraph.getCurrentState());
									otherEvent = (eventInfo) stateMap.get(new Integer(otherGraph.getCurrentState()));
									//System.out.println(otherEvent2);
									//if (otherEvent.destination != otherEvent.destination) {
									//	System.out.println("no destination match");
									//	System.exit(1);
									//}
									found = true;
									break;
								}
							}	
							if (!found) {
								out.println("medium not found for new update");
								System.exit(1);
							}
							otherGraph.setCurrentState(otherEvent.destination);
							// No, since for changed send the source/destination will be different?
							//if (e.getEventType().equals(ASYNCH_SEND) && otherEvent.destination != e.getDestinationStateReceiver()) {
							//	System.out.println("dest. state of found medium " + otherEvent.destination + " != destination state of receiver in asynch_send event " + e.getDestinationStateReceiver() );
							//	System.exit(1);		
							//}
						} //close block
					}
					else {
						out.println("Internal error: Prefix event not unaccepted_asynch_send "
				       + "or asynch_send or asynch_receive (or recollected receive - removed earlier)");
						out.println("variant is:");
				   	for(int k=0; k< variantsrSequence.size(); k++)
				   		out.println((srEvent)variantsrSequence.get(k));				   
				   	System.exit(1);
					}	
				} // not unnaccepted_asynch_send
			}
			
			
			else { // non-mapped
			if (e.getEventType().equals(UNACCEPTED_SYNCH_SEND)) {
				// if this old send can be synchronized with a recollected receive
				// remove the receiving thread's ID from approp. sets . 
				// (All threads that remain in these sets at the end have no old or new sends)
				if (recollectedChannels.containsKey(e.getChannelName())) {
					// this is an old send; can it be synched with recollected receive?
					// yes if asynch or synch w/ no select. With select depends on OpenList?
					Integer ID = (Integer) recollectedChannels.get(e.getChannelName());;
					srEvent r = (srEvent) recollectedThreads.get(ID);
				 	ArrayList OpenList = r.getOpenList();
				 	name.append(e.getChannelName());

				 	for (int j=0; j<OpenList.size(); j++) {
				 	 if ((name.toString()).equals(((OpenEvent)OpenList.get(j)).getLabel())) {
				 	 	nameFound = true;
				 	 	break;
				 	 }
				 	}
				 	name.delete(0,name.length());
					if (nameFound) {
						nameFound = false;
				    	recollectedNoOldOrNewSends.remove(ID); // okay if 2nd remove => noop
				    	//System.out.println("Add " + ID + " to recollectedWithOldPending");
					 	recollectedWithOldPending.add(ID); // old send pending for thread ID; okay if 2nd add (ignored)
/* Record ID of sender so know sender is executing an old send */
						if (!(recollectedOldSenders.containsKey(ID))) { // put sender in map
		   				//System.out.println("new linked list for " + ID);
							recollectedOldSenders.put(ID,new LinkedList());
						}
						//else System.out.println("no new linked list for " + ID);
						LinkedList oldSenders = (LinkedList)recollectedOldSenders.get(ID);
						oldSenders.addLast(intSpace.get(e.getCaller())); // add sender's ID to end of list
						//recollectedOldSenders.add(intSpace.get(e.getCaller()));
						//System.out.println("recollectedNoOldOrNewSends size is " 
						//	+ 	recollectedNoOldOrNewSends.size());
				 	} // nameFound
				} // if recollectedChannels contains name 
				//System.out.println("recollectedNoOldOrNewSends size is " 
				//	+ 	recollectedNoOldOrNewSends.size());
			} // if UNACCEPTED_SYNCH_SEND	
			else {
				/* Assume all receives are completed except for recollected receives */
				tracedsrSequence.add(e); //System.out.println("added: " + e);
				LinkedList savedExercisedTransitions = null;
				if ( e.getEventType().equals(SR_SYNCHRONIZATION)) {
            	versionNumbers[e.getCaller()] = e.getCallerVersionNumber()+1;
	            versionNumbers[e.getCalled()] = e.getCalledVersionNumber()+1;
					vectorTimeStamps[e.getCaller()].updateVectorTS(e.getVectorTS());
					vectorTimeStamps[e.getCalled()].updateVectorTS(e.getVectorTS());
					if (sbrtMode == CycleDetection) {
						if (!cycleState.get()) { 
							System.out.println("prefix event from non-cycle state "+ currentState);
//5
						}
						else {
							System.out.println("prefix event from cycle state " + currentState);
							LinkedList exercisedTransitions = (LinkedList) (statesToEvents.get(currentState));
							if (exercisedTransitions != null)
								savedExercisedTransitions = new LinkedList(exercisedTransitions);
							if (exercisedTransitions != null) {
//6
								System.out.println("in prefix, exercisedTransitions.size() is " + exercisedTransitions.size());
								Iterator b = exercisedTransitions.iterator();
								boolean found = false;
								while (b.hasNext()) {
									srEvent f = (srEvent) b.next();
//7
									System.out.println("check f, e-caller is " + f.getCaller() + 
										", found caller is " +  e.getCaller() +
										", f-called is " + f.getCalled() +
										", found called is " +  e.getCalled());
									if (f.getCaller() == e.getCaller() &&
										 f.getCalled() == e.getCalled()) {
										System.out.println("Internal Error: prefix contains an old match");
				//************
										validVariant = false; break; //System.exit(1);
				//***********
									}
								} // while
							} // exercised != null
						}
						
if (!validVariant) {
	System.out.println("start over.");
	break;
}
						 
//7d					
						if (inhibited.size()>0) {
//7e						//System.out.println("inhibited.size() is " + inhibited.size());
							Iterator b = inhibited.iterator();
							boolean found = false;
							while (b.hasNext()) {
								srEvent f = (srEvent) b.next();
//7e							//System.out.println("check f, f-caller is " + f.getCaller() + 
								//	", found caller is " +  e.getCaller() +
								// ", f-called is " + f.getCalled() +
								//	", found called is " +  e.getCalled());
								if (f.getCaller() == e.getCaller() &&
									 f.getCalled() == e.getCalled()) {
									System.out.println("Internal Error: prefix contains an inhibited match");
				//*************
									System.exit(1);
				//*************
								}
							}
//7g						//System.out.println("prefix match is not inhibited");
						}
						//else System.out.println("inhibited is empty");
											
									
						// Add (currentState,e) to stateToEvents;				

						if (!(statesToEvents.containsKey(currentState))) { // put state in map
							statesToEvents.put(currentState,new LinkedList());
						}
						//else System.out.println("no new linked list for " + ID);
						LinkedList events = (LinkedList)statesToEvents.get(currentState);
						events.addLast(e); // add event to end of list
						e.setSourceGlobalState((stateVector) currentState.clone());;
						globalState.setLocal(e.getCaller(),e.getDestinationStateSender());
						globalState.setLocal(e.getCalled(),e.getDestinationStateReceiver());
						//System.out.println("called is " + e.getCalled());
						//System.out.println("DestinationStateReceiver is " + e.getDestinationStateReceiver());
						added = globalStates.add((stateVector)globalState.clone());
	
						currentState = (stateVector)globalState.clone();
						//System.out.println("current state:" + currentState);
						e.setDestinationGlobalState((stateVector) currentState.clone());
						
						// Always use e to remove events from inhibited
						//for each event f in inhibited, remove f from inhibited if !(e||f)
						String readOrWrite = e.getChannelName().substring(1,4);
						
						ListIterator b = inhibited.listIterator();
						vectorTimeStamp eVectorTimestamp = e.getVectorTS();
						while (b.hasNext()) {
							srEvent f = (srEvent) b.next();
							vectorTimeStamp fVectorTimestamp = f.getVectorTS();
							// f => e and e is a write (as opposed to read, ext events or send/receive
/* assuming read/write for now*/
							if (readOrWrite.equals("SVW") && fVectorTimestamp.lessThan(eVectorTimestamp)) {
								// || eVectorTimestamp.lessThan(fVectorTimestamp)) {
							//if ((e.getCaller() == f.getCaller() || e.getCaller() == f.getCalled()) ||
							//	(e.getCalled() == f.getCaller() || e.getCalled() == f.getCalled())) {
								// e and f are involved with each other
/*9d*/							 //System.out.println("removing from inhibited: " + f);
									 b.remove();
							}	
						}
						
						if (cycleState.get()) { // were in cycle state when E was selected
							//inhibited.clear();
							// add each old event e to inhibited if e || E
							if (savedExercisedTransitions != null) {
								Iterator b2 = savedExercisedTransitions.iterator();
								eVectorTimestamp = e.getVectorTS();
								while (b2.hasNext()) {
									srEvent f = (srEvent) b2.next();
									vectorTimeStamp fVectorTimestamp = f.getVectorTS();
									if (!fVectorTimestamp.lessThan(eVectorTimestamp) &&
										 !eVectorTimestamp.lessThan(fVectorTimestamp)) {									 
									//if ((E.getCaller() != e.getCaller() && E.getCaller() != e.getCalled()) &&
									//		(E.getCalled() != e.getCaller() && E.getCalled() != e.getCalled())) {
										 // e and f are uninvolved with each other
	//Q: Could add event already in inhibited?
										 inhibited.add(f);
/*9b*/ 								 //System.out.println("adding f to inhibited:  f-caller is " + f.getCaller() + 
										 //	", f-called is " + f.getCalled());
									}
								}
							}
						}

						if (!added) {
							//System.out.println("**************Foo**************");
							//if (i == variantsrSequence.size() - numRecollected - 1) {
								// last event completes a cycle
							  	cycleState.set(true);
								//inhibited.clear();
//2
  								//System.out.println("prefix has a cycle at state:"+currentState);
						  	//}
						}				
						else {
							//System.out.println("no Cycle detected");
/*
							if (cycleState.get()) {
								//inhibited.clear();
								// add each old event e to inhibited if e || E
								if (savedExercisedTransitions != null) {
									Iterator b = savedExercisedTransitions.iterator();
									vectorTimeStamp eVectorTimestamp = e.getVectorTS();
									while (b.hasNext()) {
										srEvent f = (srEvent) b.next();
										vectorTimeStamp fVectorTimestamp = f.getVectorTS();
										if (!fVectorTimestamp.lessThan(eVectorTimestamp) &&
											 !eVectorTimestamp.lessThan(fVectorTimestamp)) {
										//if ((e.getCaller() != f.getCaller() && e.getCaller() != f.getCalled()) &&
										//		(e.getCalled() != f.getCaller() && e.getCalled() != f.getCalled())) {
											 // e and f are uninvolved with each other
											 inhibited.add(f);
											//System.out.println("in prefix, adding f to inhibited:  f-caller is " + f.getCaller() + 
											//	", f-called is " + f.getCalled());
										}
										//else System.out.println("in prefix, don't add f since not concurrent");
									}
								}
							}
							else {
								//for each event f in inhibited, remove f from inhibited if !(f||e)
								ListIterator b = inhibited.listIterator();
								//vectorTimeStamp eVectorTimestamp = e.getVectorTS();
								while (b.hasNext()) {
									srEvent f = (srEvent) b.next();
									//vectorTimeStamp fVectorTimestamp = f.getVectorTS();
									//if (fVectorTimestamp.lessThan(eVectorTimestamp) ||
									//	 eVectorTimestamp.lessThan(fVectorTimestamp)) {
									if ((e.getCaller() == f.getCaller() || e.getCaller() == f.getCalled()) ||
										(e.getCalled() == f.getCaller() || e.getCalled() == f.getCalled())) {
										// e and f are involved with each other
//9d										//System.out.println("removing from inhibited: " + f);
											b.remove();
									}	
								}
							}
				*/
							cycleState.set(false);
						}						
					}  // CycleDetection
				} // SR_SYNCHRONIZATION
				else if ( !e.getEventType().equals(SYNCH_SEND)) { 
					System.out.println("Internal error: Prefix event not unaccepted_send "
				        + "or sr_synchronization (or recollected receive)");
					System.out.println("variant is:");
				   for(int k=0; k< variantsrSequence.size(); k++)
				   	System.out.println((srEvent)variantsrSequence.get(k));				   
				   System.exit(1);
				}
			} // else not an unaccepted send
	    } // end else non-mapped
		} // end for
		
} while (!validVariant);
// Q: Terminates?
	
			//System.out.println("variant is:");
		   //for(int i=0; i< variantsrSequence.size(); i++)
		   //	System.out.println((srEvent)variantsrSequence.get(i));
		   	
     	if (recollectedThreads.size() == 0) {
     		//System.out.println("starting trace mode: No recollected events found");
     		RTAllRecollected = true; // otherwise there are some recollected events
     	}
		//System.out.println("after adds:"); 
		//for(int i=0; i< tracedsrSequence.size(); i++)
		//   	System.out.println((srEvent)tracedsrSequence.get(i));
      // if (++count % 1 == 0)
      // 	System.out.println(count+"/"+collected);
      //if (count == 15) {
      //	try {Thread.sleep(5000);} catch(InterruptedException e){};
      //	System.exit(1);
     	//}
       if (count % 200000 == 0) {
         out.println("time:" + java.util.Calendar.getInstance().getTime());
         //System.exit(0);
       }
       
      //for (int i = 0; i<numLTS+1; i++) {
		//		System.out.println("startState graph " + i + " is " + ((Graph) LTS.get(i)).getCurrentState());
		//}

		//System.out.println("LTS.size() is " + LTS.size());		
		nonTerminatedLTS.clear();
		Iterator j = LTS.iterator();
		j.next();
		Graph gr;
		while (j.hasNext()) {
			if (!((gr=((Graph)j.next())).isTerminated())) {
				//System.out.println("adding graph " + gr.ID);
				nonTerminatedLTS.add(gr);
			}
			//else System.out.println("not adding graph " + gr.ID);
		}

		//for (int i = 0; i<numLTS+1; i++) {
		//		System.out.println("LTS " +  i + " starting in state: "+((Graph) LTS.get(i)).getCurrentState());		
		//}
		//System.out.println("after while-fill, non-terminatedLTS.size() is  :"+nonTerminatedLTS.size());
		if (nonTerminatedLTS.size()==0) 
			out.println("Internal Error: all LTSs in variant start states are terminated");
      //if (variantsrSequence.size() != 0)
      //	System.exit(1);
		//else System.out.println("nonTerminatedLTS.size() is " + nonTerminatedLTS.size());
		//System.out.println("starting with currentState = "+currentState);
		//System.out.println("starting with globalState = "+globalState);
		boolean found = false;
		boolean recollected = false;
		ArrayList enabled = null;
		boolean progress = true;
		atLeastOne.set(false);
		//System.out.println("starting with globalStates.size is " + globalStates.size());
		//System.out.println("starting with cycle state " + cycleState.get());
		//System.out.println("starting with statesToEvents.size is " + statesToEvents.size());
		// RHC: Cycle Detection uses" while (progress)
		//while (progress) {
		while ((nonTerminatedLTS.size()-(numNonTerminating-1)) > 0) {
		// Note: nonTerminatedLTS does not include null Graph in position 0
		//       but numNonTerminating does, hence the -1
//Q: What if all are non-terminating? 
//Q: Don't count nullGraph in numNonTerminating then don't need -1? I so
//     implement this by setting flag when construct it, as in it starts in terminal state
			//System.out.println("in while-more :"+nonTerminatedLTS.size()+","+(nonTerminatedLTS.size()-(numNonTerminating-1)));
		// Note: LTSs with terminal state are expected to terminate, e.g., port objects are non-terminating
			Iterator i = nonTerminatedLTS.iterator();
			found = false;
			progress = false;
			potentialMatch.set(false);
			while (i.hasNext()) {
				Graph g = (Graph) i.next();
				if (g.isMedium() ) {
					out.println("Internal error: g is a medium and we shouldn't ever get to mediums on this list");
					System.exit(1);
				}
				
				enabled = g.getEnabledEvents(); // alist of current state of g
				boolean result = findMatch(g,enabled,tracedsrSequence,LTS,nonTerminatedLTS,
									recollectedThreads, recollectedNoOldOrNewSends,
									recollectedOldSenders,recollectedChannels,
									recollectedWithOldPending,RTAllRecollected,
									versionNumbers,vectorTimeStamps,
									sbrtMode, globalState, globalStates,
									statesToEvents, currentState, cycleState, inhibited,potentialMatch,
									messages);
				//System.out.println("After FindMatch, currentState is " + currentState);
				//System.out.println("After FindMatch, globalState is " + globalState);
				//System.out.println("After FindMatch, inhibited.size() is " + inhibited.size());
				if (result) {
					found = true;
					atLeastOne.set(true);
					progress = true;
					break;	// hasNext
				}
/* Q: Here, remember if there was a potentialMatch? Or since we don't reset it to
      false for each findMatch, we will remember if it was set?
   Q: Do we need to track the potential matches so we can make sure not to add
      them to the partial sequence?
*/
			}
			// if (found) then keep on going. Note: the progress loop will terminate
			//    when all LTSs are terminated so !found may never be true.
			if (!found) {	// non-terminated LTSs but no match or match but not allowed
				//System.out.println("no match found"); // print tracedsrSequence?
				//System.out.println("variantSize is " + variantSize);
				if (variantSize == 0) { // first execution, implies there are no recollected receives
					if (!atLeastOne.get()) { // no events collected on first execution
						out.println("Deadlock: No events exercised and no sequence collected.");
						if (potentialMatch.get()) {
							out.println("Internal Error: match on first event of first trace not allowed");
						}
						System.exit(1);
					}
					else { // first execution collected at least one event
						if (!potentialMatch.get()) {	//there were no rendezvous that were possible but not allowed
																// for example, for an old send and a recollected receive
							out.println("Deadlock: no possible rendezvous for non-terminated processes.");
							System.exit(1);						
						}								
					}
				}
				else { // not first execution so variantSize > 0
					if (!potentialMatch.get()) {	//there were no rendezvous that were possible but not allowed
															// for example, for an old send and a recollected receive
						if (!atLeastOne.get()) { // no rendezvous events collected during trace
					   	                      // and "found" must be false
							out.println("Deadlock: no possible rendezvous for non-terminated processes "
								+ "and no trace events collected for a non-empty variant.");
							System.exit(1);					
						}
						else {
							out.println("Deadlock: no possible rendezvous for non-terminated processes.");
							System.exit(1);						
						}
						// else there was at least one rendezvous possible but it was not allowed
					}	
					// else at least one new rendezvous event was collected in the trace
				}
				// Here: No match was found but there was a possible match that was not allowed	
				//System.out.println("cycleState is " + cycleState.get());
				// If in a cycle state, then must be a possible match? by def. of cycle
/*Q*/			// but could there have been a recollected receive in this case?

			  	//if (!cycleState.get())    // Note: no { here

/* Q: if matches prevented because they are old or inhibited, but there are 
      recollected receives, then what will happen when we collect partial?
      That is, will unaccepted send/receives that are possible but not allowed
      be collected too?
*/
					if (recollectedThreads.size() > 0) {
						// recollected receive(s) with no new send
						if (recollectedWithOldPending.size()>0) {

							// there is a recollected event with a pending old send,
							// so it's not an application deadlock. Collect the partial sequence;
							// then let the execution finish and ignore the complete sequence
							//System.out.println("recollectedWithOldPending size is " + 	recollectedWithOldPending.size());
							//System.out.println("recollectedEvents not collected but old pending => collect partial sequence");
							//System.out.println("recollectedThreads size is " + 	recollectedThreads.size());
							//System.out.println("recollectedNoOldOrNewSends size is " + 	recollectedNoOldOrNewSends.size());
											
							// collect partial
							//checkForUnacceptedSendReceive();
							//TestAndReplayCollection.outputObjectBasedTestAndReplaySequences();
							//ArrayList tracedsrSequence = TestAndReplayCollection.getSequence();
						
							// add the non-recollected events
							// Note: no more matches are possible
/* Q: No, may be possible? */
							Iterator f = nonTerminatedLTS.iterator();
							while (f.hasNext()) {
								Graph g = (Graph) f.next();
								enabled = g.getEnabledEvents(); // alist of current state of g
								Iterator h = enabled.iterator();
           /* but there may be more than one enabled event if current state is for selective wait;
              so do we choose any one?
           */
								if (mappedSequence) {
								   arec a = (arec) h.next();
								   // don't need to search for the destination thread since this info is in U
								   Trans thisTransition = (Trans) a.getTransitionList().get(0); // only one trans for this label
									if (thisTransition.getEventType().equals("ASYNCH_SEND")) {
										// no reason why an asynch_send is not executed, even old sends are allowed to occur
										// (but can't be recieved until after new send received by recollected receive
										out.println("internal error: collecting partial sequence found asynch_send");
										System.exit(1);
										srEvent sendEvent = new srEvent(); // eventPool.acquire();	
										sendEvent.setSourceStateSender(g.getCurrentState());
										sendEvent.setDestinationStateSender(thisTransition.getDestinationState());
										sendEvent.setSourceStateReceiver(-1);
										sendEvent.setDestinationStateReceiver(-1);
										sendEvent.setCaller(g.ID); sendEvent.setCalled(thisTransition.getU()); 
										if (g.ID != thisTransition.getC()) {
											out.println("internal error: collecting partial g.ID != C");
											System.exit(1);
										}
										sendEvent.setCallerVersionNumber(versionNumbers[g.ID]); 
										versionNumbers[g.ID]++;  // to be consistent
										sendEvent.setCalledVersionNumber(-1);
										sendEvent.setChannelName(thisTransition.getChannelName());
										sendEvent.setChannelVersionNumber(-1); // not used
										sendEvent.setEventType(eventTypeParameters.UNACCEPTED_ASYNCH_SEND);
										sendEvent.setEventType2(eventTypeParameters.UNACCEPTED_ASYNCH_SEND);
										sendEvent.setLabel(thisTransition.getLabel());sendEvent.setIsEntry(false);
										sendEvent.setOpenList(null);
										tracedsrSequence.add(sendEvent);									
									}
									else { // ASYNCH_RECEIVE
										if (!recollectedThreads.containsKey(new Integer(g.ID))) {
											srEvent E = new srEvent(); // eventPool.acquire();
											E.setCaller(-1); E.setCalled(g.ID); // caller is C, by the way
											if (g.ID != thisTransition.getU()) {
												out.println("internal error: collecting partial g.ID != U");
												System.exit(1);
											}
											E.setCalledVersionNumber(versionNumbers[g.ID]); 
											versionNumbers[g.ID]++;  // to be consistent
											E.setCallerVersionNumber(-1);
											E.setSourceStateSender(-1);
											E.setDestinationStateSender(-1);
											E.setSourceStateReceiver(g.getCurrentState());
											E.setDestinationStateReceiver(thisTransition.getDestinationState());
											E.setChannelName(thisTransition.getChannelName());
											E.setChannelVersionNumber(-1); // not used
											E.setEventType(eventTypeParameters.UNACCEPTED_RECEIVE);
											E.setEventType2(eventTypeParameters.UNACCEPTED_RECEIVE);
											E.setLabel(thisTransition.getLabel());E.setIsEntry(false);
											ArrayList receivingThreadsOpenList = g.getCurrentStatesOpenList();
                                 /* OpenList 	could have many events; see note above */
											E.setOpenList(receivingThreadsOpenList);
											tracedsrSequence.add(E);
										}									
									}
						   	}
						   	else { // not mapped
									arec a = (arec) h.next();
									String label = a.getLabel();
									Integer receivingThread=null, sendingThread=null;
									//ArrayList receivingThreadsOpenList=null;
									if (label.charAt(0)=='\'') {
										//receivingThread = intSpace.get(firstID);
										//receivingThreadsOpenList = g.getCurrentStatesOpenList();
										sendingThread = intSpace.get(g.ID);
									}
									else {
										receivingThread = intSpace.get(g.ID);
										//sendingThread = intSpace.get(firstID);
										//receivingThreadsOpenList = otherGraph.getCurrentStatesOpenList();
									}
									if (sendingThread != null) {
										// create an unaccepted_send event
										String aBar = complement(a.getLabel());
										LinkedList aBarEvents = (LinkedList) Graph.eventsToThreads.get(aBar);
										if (aBarEvents == null) {
											out.println("Error: no threads execute event " + aBar);
											System.exit(1);
										}
												
									/* No: one thread could execute multiple aBar events. But could
									   check input and ensure for receive event E only one thread executes E
										if (aBarEvents.size() != 1) {
											out.println("Error: More than one thread executes receive event " + aBar);
											System.exit(1);
										}
									*/
												
										// aBarEvents is a list of potentially matching events of a.getLabel()
									 
										Iterator b = aBarEvents.iterator();
										//while (b.hasNext()) { // asume only one thread executes aBar events
											eventInfo otherEvent = (eventInfo)b.next(); // just need ID of other thread
										//}
										srEvent sendEvent = new srEvent(); // eventPool.acquire();	
										sendEvent.setSourceStateSender(g.getCurrentState());
										sendEvent.setDestinationStateSender(((Trans)a.getTransitionList().get(0)).getDestinationState());
										sendEvent.setSourceStateReceiver(-1);
										sendEvent.setDestinationStateReceiver(-1);
										sendEvent.setCaller(g.ID); sendEvent.setCalled(otherEvent.ID); 
										sendEvent.setCallerVersionNumber(versionNumbers[sendingThread.intValue()]); 
										versionNumbers[sendingThread.intValue()]++;  // to be consistent
										sendEvent.setCalledVersionNumber(-1);
										sendEvent.setChannelName(aBar);
										sendEvent.setChannelVersionNumber(-1); // not used
										sendEvent.setEventType(eventTypeParameters.UNACCEPTED_SYNCH_SEND);
										sendEvent.setEventType2(eventTypeParameters.UNACCEPTED_SYNCH_SEND);
										sendEvent.setLabel(aBar);sendEvent.setIsEntry(false);
										sendEvent.setOpenList(null);
										tracedsrSequence.add(sendEvent);
	
									}
									else { // it's a receive; make sure receive is not a recollected receive
								       // since they go at end
										if (!recollectedThreads.containsKey(receivingThread)) {
											srEvent E = new srEvent(); // eventPool.acquire();
											E.setCaller(-1); E.setCalled(receivingThread.intValue()); 
											E.setCalledVersionNumber(versionNumbers[receivingThread.intValue()]);
											versionNumbers[receivingThread.intValue()]++;  // to be consistent
											E.setCallerVersionNumber(-1); // rhc added
											E.setSourceStateSender(-1);
											E.setDestinationStateSender(-1);
											E.setSourceStateReceiver(g.getCurrentState());
											E.setDestinationStateReceiver(((Trans)a.getTransitionList().get(0)).getDestinationState());
											E.setChannelName(a.getLabel());
											E.setChannelVersionNumber(-1); // not used
											E.setEventType(eventTypeParameters.UNACCEPTED_RECEIVE);
											E.setEventType2(eventTypeParameters.UNACCEPTED_RECEIVE);
											E.setLabel(a.getLabel());E.setIsEntry(false);
											ArrayList receivingThreadsOpenList = g.getCurrentStatesOpenList();
/* OpenList 	could have many events; see note above */
											E.setOpenList(receivingThreadsOpenList);
											tracedsrSequence.add(E);
										}
									}
								} // not mapped
							}
										
							// add the recollected events
							Iterator k = recollectedWithOldPending.iterator();
							while (k.hasNext()) {
								Integer ID = (Integer) k.next();
								//System.out.println("ID in k-loop is " + ID);
								srEvent e = null;
								if (recollectedThreads.containsKey(ID))
									e = (srEvent) recollectedThreads.get(ID);
								else {
									out.println("internal error: recollected receive not found");
									System.exit(1);
								}
								tracedsrSequence.add(e);
							}

							// for debugging, to print the sequence instead of the variants
							//if (sequence.size() != 0 && traceVariants == TRACEON) {
								//	traceTheVariant(sequence);
							//}		
						     	
							//System.out.println("trace size is " + tracedsrSequence.size());
							//System.out.println("variant size is " + variantsrSequence.size());
							//if (sequence.size() <= variantsrSequence.size()) {
							//	System.out.println("size error");
							//}
							if (modularTesting == propertyParameters.MTON)
								collector.collectUnique(tracedsrSequence);				
							variant.setEvents(tracedsrSequence);
/**/
							generator.depositSeq (variant);
							
							//System.out.println("deposited PARTIAL sequence:"); 
							//System.exit(1);
//3
							//for(int m=0; m<tracedsrSequence.size(); m++)
			  				//	System.out.println((srEvent)tracedsrSequence.get(m));
			  				//System.out.println("deposited sequence size is " + tracedsrSequence.size());
										
							recollected = true;
							break; // while !terminated
							//recollectedChannels.clear(); recollectedThreads.clear();
							//recollectedNoOldOrNewSends.clear();
							//recollectedWithOldPending.clear();
							//recollectedOldSenders.clear();
						}
						else { // no old pending
/* Q: No, there is a potential match so no deadlock? */
/* But should we collect the sequence?
*/
   						out.println("recollected receive with no old pending");

							// a recollected receive with no new sends and no old sends pending
							// so this is an application deadlock
							
							//System.out.println("recollected event with no new or old sends ==> application deadlock");
							
							out.println("recollectedThreads size is " + 	recollectedThreads.size());
							//System.out.println("recollectedPermitted size is " + 	recollectedPermitted.size());
							out.println("recollectedNoOldOrNewSends size is " 
								+ 	recollectedNoOldOrNewSends.size());

							//System.out.println("Error: Timeout waiting for application completion - probable deadlock/livelock");
										
							//ObjectOutputStream outputSequence = new 
							//		ObjectOutputStream(new FileOutputStream("infeasibleSequence.dat"));
							//outputSequence.writeObject(variantsrSequence);
							//System.out.println("wrote tracedsrSequence");
/* Q: set this so the sequence is not collected? */
/* Or collect partial sequence but not the sends/receives for the disallowed matches?
   Also, should we collect the recollected receive?
*/
							recollected = true;
							//System.exit(1);	
						}
					}
					else {  // no recollected receives
/* Q: No, there is a potential match so no deadlock? */ 
						// no recollected receives so this is an application deadlock
						
						//System.out.println("no recollected receives ==> application deadlock");
						
						//System.out.println("recollectedThreads size is " + 	recollectedThreads.size());
						//System.out.println("recollectedPermitted size is " + 	recollectedPermitted.size());
						//System.out.println("recollectedMonitors size is " + 	recollectedMonitors.size());
						//System.out.println("recollectedMonitorCallers size is " + 	recollectedMonitorCallers.size());
						//System.out.println("recollectedNoOldOrNewSends size is " + 	recollectedNoOldOrNewSends.size());
						//System.out.println("Error: Timeout waiting for application completion - probable deadlock/livelock");
						
						//System.exit(1);	
					}
					//System.exit(1);
				} // !found
		} // while not terminated

/* q: delete these releases?
		for (int i=0; i<tracedsrSequence.size(); i++)
			eventPool.release((srEvent)tracedsrSequence.get(i));
*/
		if (!recollected) { //  || cycleState.get()) {
			collected++; // no partial sequence collected

			if (modularTesting == propertyParameters.MTON)
				collector.collectUnique(tracedsrSequence);

			variant.setEvents(tracedsrSequence);
			generator.depositSeq (variant);
//4
			//System.out.println("deposited sequence:");
			//for(int i=0; i< tracedsrSequence.size(); i++)
			//  System.out.println((srEvent)tracedsrSequence.get(i));
			//System.out.println("deposited sequence size is " + tracedsrSequence.size());

			//srSeq variantWithRaces = generator.computeRaceSet(variant);
			//ArrayList sequenceWithRaces = variantWithRaces.getEvents();
			//System.out.println("deposited sequence with races:");
			//for(int i=0; i< sequenceWithRaces.size(); i++)
			//  System.out.println((srEvent)sequenceWithRaces.get(i));
			//System.out.println("deposited sequence size is " + sequenceWithRaces.size());
			//System.exit(1);

			
/* check */
			if (sbrtMode == CycleDetection) {
				globalState.reset();
				globalStates.clear();
				statesToEvents.clear();
			}

			nameFound = false;
			if (sbrtMode == CycleDetection) {
				cycleState.set(false);
				inhibited.clear();
				currentState = (stateVector)globalState.clone();
				System.out.println("starting trace check, current state is " + currentState);
			}
			// note/Q: cycleDetection check only done if not recollected, why not do check on partial seq?
			if (mappedSequence) {
				for (int i=0; i < tracedsrSequence.size(); i++) {
					srEvent e = (srEvent) tracedsrSequence.get(i);
					//System.out.println("e being checked is " + e);
					if (e.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
						out.println("traced non-partial sequence has unaccepted_asynch_send event");
						System.exit(1);
					} // if UNACCEPTED_SYNCH_SEND	
					else {
						if (e.getEventType().equals(ASYNCH_SEND) || e.getEventType().equals(ASYNCH_RECEIVE)) {
							// skipping CycleDetection here
					   }
						else { 
							out.println("Internal error: trace event not asynch_send or asynch_receive");
							out.println("trace is:");
						   for(int k=0; k< variantsrSequence.size(); k++)
						   	out.println((srEvent)variantsrSequence.get(k));				   
						}
					} // else not an unaccepted send
				} // for
			}	
			else { // !mappedSequence
		
				for (int i=0; i < tracedsrSequence.size(); i++) {
					srEvent e = (srEvent) tracedsrSequence.get(i);
					//System.out.println("e being checked is " + e);
					if (e.getEventType().equals(UNACCEPTED_SYNCH_SEND)) {
						out.println("traced sequence has unaccepted_synch_send event");
						System.exit(1);
					} // if UNACCEPTED_SYNCH_SEND	
					else {
						/* Assume all receives are completed except for recollected receives */
						LinkedList savedExercisedTransitions = null;
						if ( e.getEventType().equals(SR_SYNCHRONIZATION)) {
							if (sbrtMode == CycleDetection) {
								if (!cycleState.get()) {
									System.out.println("traced event is not from cycle state " + currentState);
//5	
								}
								else {
									System.out.println("traced event is from cycle state " + currentState);
									LinkedList exercisedTransitions = (LinkedList) (statesToEvents.get(currentState));
									if (exercisedTransitions != null)
										savedExercisedTransitions = new LinkedList(exercisedTransitions);
									if (exercisedTransitions != null) {
//6	
										System.out.println("checking trace, exercisedTransitions.size() is " + exercisedTransitions.size());
										Iterator b = exercisedTransitions.iterator();
										found = false;
										while (b.hasNext()) {
											srEvent f = (srEvent) b.next();
//7
											System.out.println("check f, e-caller is " + f.getCaller() + 
												", found caller is " +  e.getCaller() +
												", f-called is " + f.getCalled() +
												", found called is " +  e.getCalled());
											if (f.getCaller() == e.getCaller() &&
												 f.getCalled() == e.getCalled()) {
												System.out.println("Internal Error: trace contains an old match");
												if (i>variantSize-1) { // past variant
													System.out.println("Internal Error: trace contains an inhibited match past variant, where i is "
													  + i + " and variant size -1 is " + (variantSize-1));											
													System.exit(1);
												}
											}
										} // while
									} // exercised != null
								}
		
//7d							//System.out.println("trace match not in cycle state");
								if (inhibited.size()>0) {
//7e						
									System.out.println("inhibited.size() is " + inhibited.size());
									Iterator b = inhibited.iterator();
									found = false;
									while (b.hasNext()) {
										srEvent f = (srEvent) b.next();
//7e								
										System.out.println("check f, f-caller is " + f.getCaller() + 
											", found caller is " +  e.getCaller() +
										 ", f-called is " + f.getCalled() +
											", found called is " +  e.getCalled());
										if (f.getCaller() == e.getCaller() &&
											 f.getCalled() == e.getCalled()) {
											System.out.println("Internal Error: trace contains an inhibited match");
											if (i>variantSize-1) { // past variant
													System.out.println("Internal Error: trace contains an inhibited match past variant, where i is "
													  + i + " and variant size -1 is " + (variantSize-1));
													System.exit(1);
											}	
										}
									}
//7g								System.out.println("trace match is not inhibited");
								}
								else System.out.println("inhibited is empty");					
									
								// Add (currentState,e) to stateToEvents;				
		
								if (!(statesToEvents.containsKey(currentState))) { // put state in map
									statesToEvents.put(currentState,new LinkedList());
								}
								//else System.out.println("no new linked list for " + ID);
								LinkedList events = (LinkedList)statesToEvents.get(currentState);
								events.addLast(e); // add event to end of list
								e.setSourceGlobalState((stateVector) currentState.clone());;
								globalState.setLocal(e.getCaller(),e.getDestinationStateSender());
								globalState.setLocal(e.getCalled(),e.getDestinationStateReceiver());
								//System.out.println("called is " + e.getCalled());
								//System.out.println("DestinationStateReceiver is " + e.getDestinationStateReceiver());
								added = globalStates.add((stateVector)globalState.clone());
			
								currentState = (stateVector)globalState.clone();
								//System.out.println("current state:" + currentState);
								e.setDestinationGlobalState((stateVector) currentState.clone());
								
								String readOrWrite = e.getChannelName().substring(1,4);
								
								//for each event f in inhibited, remove f from inhibited if !(f||e)
								ListIterator b = inhibited.listIterator();
								vectorTimeStamp eVectorTimestamp = e.getVectorTS();
								while (b.hasNext()) {
									srEvent f = (srEvent) b.next();
									vectorTimeStamp fVectorTimestamp = f.getVectorTS();
									if (readOrWrite.equals("SVW") && fVectorTimestamp.lessThan(eVectorTimestamp)) {
										//|| eVectorTimestamp.lessThan(fVectorTimestamp)) {
					//Q and check for read?
									//if ((e.getCaller() == f.getCaller() || e.getCaller() == f.getCalled()) ||
									//	(e.getCalled() == f.getCaller() || e.getCalled() == f.getCalled())) {
										// e and f are involved with each other
//9d											System.out.println("removing from inhibited: " + f);
										b.remove();
									}	
								}
						
								if (cycleState.get()) {
									//inhibited.clear();
									// add each old event e to inhibited if e || E
									if (savedExercisedTransitions != null) {
										Iterator b2 = savedExercisedTransitions.iterator();
										eVectorTimestamp = e.getVectorTS();
										while (b2.hasNext()) {
											srEvent f = (srEvent) b2.next();
											vectorTimeStamp fVectorTimestamp = f.getVectorTS();
											if (!fVectorTimestamp.lessThan(eVectorTimestamp) &&
												 !eVectorTimestamp.lessThan(fVectorTimestamp)) {
											//if ((e.getCaller() != f.getCaller() && e.getCaller() != f.getCalled()) &&
											//		(e.getCalled() != f.getCaller() && e.getCalled() != f.getCalled())) {
												 // e and f are uninvolved with each other
												 inhibited.add(f);
												System.out.println("in check trace, adding f to inhibited:  f-caller is " + f.getCaller() + 
													", f-called is " + f.getCalled());
											}
											//else System.out.println("in trace, don't add f since not concurrent");
										}
									}
								}
		
								if (!added) {
									//System.out.println("**************Foo**************");
									//if (i == variantsrSequence.size() - numRecollected - 1) {
										// last event completes a cycle
									  	cycleState.set(true);
										//inhibited.clear();
//2		
	  									System.out.println("trace has a cycle at state:"+currentState);
								  	//}
								}				
								else {
									System.out.println("no Cycle detected");
			/*	
									if (cycleState.get()) {
										inhibited.clear();
										// add each old event e to inhibited if e || E
										if (savedExercisedTransitions != null) {
											Iterator b = savedExercisedTransitions.iterator();
											//vectorTimeStamp eVectorTimestamp = e.getVectorTS();
											while (b.hasNext()) {
												srEvent f = (srEvent) b.next();
												//vectorTimeStamp fVectorTimestamp = f.getVectorTS();
												//if (!fVectorTimestamp.lessThan(eVectorTimestamp) &&
												//	 !eVectorTimestamp.lessThan(fVectorTimestamp)) {
												if ((e.getCaller() != f.getCaller() && e.getCaller() != f.getCalled()) &&
														(e.getCalled() != f.getCaller() && e.getCalled() != f.getCalled())) {
													 // e and f are uninvolved with each other
													 inhibited.add(f);
													System.out.println("in check trace, adding f to inhibited:  f-caller is " + f.getCaller() + 
														", f-called is " + f.getCalled());
												}
												else System.out.println("in trace, don't add f since not concurrent");
											}
										}
									}
									else {
						
										//for each event f in inhibited, remove f from inhibited if !(f||e)
										ListIterator b = inhibited.listIterator();
										//vectorTimeStamp eVectorTimestamp = e.getVectorTS();
										while (b.hasNext()) {
											srEvent f = (srEvent) b.next();
											//vectorTimeStamp fVectorTimestamp = f.getVectorTS();
											//if (fVectorTimestamp.lessThan(eVectorTimestamp) ||
											//	 eVectorTimestamp.lessThan(fVectorTimestamp)) {
											if ((e.getCaller() == f.getCaller() || e.getCaller() == f.getCalled()) ||
												(e.getCalled() == f.getCaller() || e.getCalled() == f.getCalled())) {
												// e and f are involved with each other
//9d											System.out.println("removing from inhibited: " + f);
													b.remove();
											}	
										}
									}
					*/
									cycleState.set(false);
								}						
							}  // CycleDetection
						} // SR_SYNCHRONIZATION
						else if ( !e.getEventType().equals(SYNCH_SEND)) { //Q: Do we use SYNCH_SEND events?
							out.println("Internal error: trace event not unaccepted_send "
						        + "or sr_synchronization (or recollected receive)");
							out.println("trace is:");
						   for(int k=0; k< variantsrSequence.size(); k++)
						   	out.println((srEvent)variantsrSequence.get(k));				   
						}
					} // else not an unaccepted send
				} // end for			
			} // !mapped
/* end check */
		}
		RTAllRecollected = false;
		for (int i=0; i<numLTS+1; i++)  {
			versionNumbers[i]=1;
			vectorTimeStamps[i].reset(i);
		}
		if (sbrtMode == CycleDetection) {
			globalState.reset();
			globalStates.clear();
			statesToEvents.clear();
		}
		
		messages.clear();
		recollectedChannels.clear(); 
		recollectedThreads.clear();
		recollectedNoOldOrNewSends.clear();
		recollectedWithOldPending.clear();
		recollectedOldSenders.clear();
		
      if (++count % hash == 0) {
       	out.println(count+"/"+collected);
         if (modularTesting == propertyParameters.MTON) {
         	collector.showSizes();
         }
      }
       	
      try {
         if (sendEmail && count % 500000 == 0)
           t.sendIT("SBRT count is " + count);
      } catch (Exception e) {}

	}
	
 }
	
	static final String complement(String s) {
	  StringBuffer comp = new StringBuffer(s);
	  if (comp.charAt(0)=='\'') {
	  	comp.deleteCharAt(0);
	    //comp.append(s.substring(1,s.length()));
  		comp.insert(0,'?');
	  }
	  else {
  	  	comp.deleteCharAt(0);
  		comp.insert(0,'\'');
	    //comp.append("'"); comp.append(s);
	  }
	  return comp.toString();
	}
	
	static final boolean findMatch(Graph g, ArrayList enabled, ArrayList tracedsrSequence, 
			ArrayList LTS, ArrayList nonTerminatedLTS,
			HashMap recollectedThreads, HashSet recollectedNoOldOrNewSends,
			HashMap recollectedOldSenders, HashMap recollectedChannels,
			HashSet recollectedWithOldPending, boolean RTAllRecollected,
			int[] versionNumbers, vectorTimeStamp[] vectorTimeStamps,
			propertyParameters.SBRTMode sbrtMode, stateVector globalState,
			HashSet globalStates, HashMap statesToEvents, stateVector currentState,
			mutableBoolean cycleState, LinkedList inhibited,
			mutableBoolean potentialMatch,
			HashMap messages) {
	// enabled is alist of current state of g, nonTerminatedLTSs are candidates for match
		srEvent E = null;
		srEvent sendEvent = null;
	   int firstID = g.ID;
	   
	   //System.out.println("enabled of: " + g.ID);
		Iterator i = enabled.iterator();
		if (!i.hasNext()) {
			out.println("Internal Error: non-terminated LTS has no enabled events");
			System.exit(1);
		}
		while (i.hasNext()) {
			arec a = (arec) i.next();
			Trans thisTransition = (Trans) a.getTransitionList().get(0);
			String aBar;
			if (mappedSequence)
				aBar = a.getLabel(); // mapped sequences from Lotos, 
			else
				aBar = complement(a.getLabel());
			//System.out.println("arec label is " + a.getLabel());
			//System.out.println("aBar is " + aBar);
			if (mappedSequence) {
				if (thisTransition.getEventType().equals("USER_EVENT")) {
					g.setCurrentState(thisTransition.getDestinationState());
					vectorTimeStamps[thisTransition.getC()].updateIntegerTS(thisTransition.getC());
					//System.out.println("Created User event: " + a.getLabel() + " " + thisTransition.getC() + " " + (vectorTimeStamp)(vectorTimeStamps[thisTransition.getC()]).getVectorTS());
					
					/* Turned off for now */
					//ApplicationEvents.exerciseEvent(a.getLabel(),thisTransition.getC(),(vectorTimeStamp)(vectorTimeStamps[thisTransition.getC()]).getVectorTS());
					
					
					if (sbrtMode == CycleDetection)
						/* skipped for now */ {
					}
					nonTerminatedLTS.clear();
					//System.out.println("clear:"+nonTerminatedLTS.size());
					Iterator l = LTS.iterator();
					l.next();
					Graph gr;
					while (l.hasNext()) {
						if (!((gr=((Graph)l.next())).isTerminated())) {
							nonTerminatedLTS.add(gr);
						}
					}
					return true;
				}
			}
			
			//LinkedList aBarEvents = (LinkedList) Graph.eventsToThreads.get(aBar);
			//if (aBarEvents == null) {
			//	System.out.println("Error: no threads execute event " + aBar);
			//	System.exit(1);
			//}
			//			
			// if (a is for a send) then aBarEvents can have only one thread
			//			
			// aBarEvents is a list of potentially matching events of a.getLabel()
			//Iterator j = aBarEvents.iterator();

			HashMap threadMap = (HashMap) Graph.eventsToTransitions.get(aBar);	
			if (threadMap == null) {
				out.println("Error: no threads execute event " + aBar);
				System.exit(1);
			}
			if (thisTransition.getEventType().equals(ASYNCH_SEND) || thisTransition.getEventType().equals(ASYNCH_RECEIVE) && threadMap.size() != 2) {
				out.println("Error: more than 2 threads synchronize on event " + aBar);
				System.exit(1);
			}	
			Set eSet = threadMap.entrySet();
			Iterator iMap = eSet.iterator();
			boolean foundMatch = false;
			eventInfo otherEvent = null;
			Graph otherGraph = null;
			while (iMap.hasNext()) {
				Map.Entry entry = (Map.Entry) (iMap.next());
				Integer ID = (Integer) entry.getKey();
				//System.out.println("ID is " + ID);
				if (!mappedSequence && firstID == ID.intValue()) {
					out.println("Internal Error: caller equals called for send-receive match");
					System.exit(1);
				}
				otherGraph = (Graph) LTS.get(ID.intValue());
				if ((mappedSequence && otherGraph.isMedium()) || (!mappedSequence && (g.ID != otherGraph.ID))) {
				   // for mapped Sequence, g is always a non-medium
				   // Q: for non-mapped sequence, do we need the check: g.ID != otherGraph.ID?
					HashMap stateMap = (HashMap) entry.getValue();
					//System.out.println("otherGraph.getCurrentState() " + otherGraph.getCurrentState());
					otherEvent = (eventInfo) stateMap.get(new Integer(otherGraph.getCurrentState()));
					//System.out.println(otherEvent);
					if (otherEvent != null) {
						foundMatch = true;
						break;
					}
				}
			}	
			if (foundMatch) {
		
			//while (j.hasNext()) {
				//eventInfo otherEvent = (eventInfo)j.next();
				//if (!mappedSequence && firstID == otherEvent.ID) {
				//	System.out.println("Internal Error: caller equals called for send-receive match");
				//	System.exit(1);
				//}
				//System.out.println("otherEvent: "+otherEvent);
				//thisTransition = (Trans) a.getTransitionList().get(0);
				//if (mappedSequence)
					//System.out.println("this event: " + "ID:" + firstID + " source:" + g.getCurrentState() 
					//  + " destination:" + thisTransition.getDestinationState()
		         //  + " label:" + thisTransition.getLabel() + " C:" + thisTransition.getC() 
	   	      //  + " U:" + thisTransition.getU() + " channelName:"+thisTransition.getChannelName() 
	      	   //    + " eventType:" + thisTransition.getEventType());


// Q:check here otherEvent.ID != firstID, i.e., don;t send msg to yourself 
				//Graph otherGraph = (Graph) /*nonTerminated*/LTS.get(otherEvent.ID);
				//System.out.println("check for match with " + otherEvent.ID+ " in state " + otherEvent.source);
				//System.out.println(otherEvent.ID + " currently in state " + otherGraph.getCurrentState());	
				//if (g.ID != otherGraph.ID && otherGraph.getCurrentState() == otherEvent.source) {
				// otherGraph is in a state where it can do a matching event
					potentialMatch.set(true);
					/* Test here for recollected receive events:
					- if receive is recollected, make sure send is new. 
						(Note that recollectedOldSenders is a map of
				     		threads that are doing old sends
					  - if it is new, then do book keeping 
					  		(recollectedNoOldOrNewSends/Channels.remove(ID/channelName);
				       else don't accept this match
					*/
					Integer receivingThread=null, sendingThread=null;
					Integer C = null;  Integer U = null;
					ArrayList receivingThreadsOpenList=null;
					//if (a.getTransitionList().size() != 1) { // state has one trans for given label (i.e., determ.)
					  // No: Example: Reply from either helper (non-deferred) or coordinator (deferred)
					  //(9, "REPLY_3_0R !0 !1 !11 !1 !REPLYCHANNEL0 !ASYNCH_RECEIVE", 10)
					  //(9, "REPLY_3_0R !0 !1 !12 !1 !REPLYCHANNEL0 !ASYNCH_RECEIVE", 10)
					//  System.out.println("internal error: transitionlist.size > 0");
					//  System.exit(1);
					//}
					// Note: requiring that only 2 processes match on an event: sender/receiver and medium.
					// could allow more but make sure one of matches is a medium? (for asynch message passing)
				   //Trans thisTransition = (Trans) a.getTransitionList().get(0); // only one trans for this label
				   if (mappedSequence)
					   if (thisTransition.getC() != otherEvent.C || thisTransition.getU() != otherEvent.U ||
					       !thisTransition.getChannelName().equals(otherEvent.channelName) ||
					       !thisTransition.getEventType().equals(otherEvent.eventType)) {
					    out.println("Note: mapping information for transitions do not match, skipping other transition");
					    out.println("thisTransition:"+thisTransition);
					    out.println(" other transition:" + " C:"+otherEvent.C + " U:"+otherEvent.U
					       + " otherEvent.channelName:" + otherEvent.channelName + " otherEvent.eventType:"
					       + otherEvent.eventType);
					    System.exit(1);
					    //continue;
				   	}
				   
					if (mappedSequence) {
						if (otherEvent.eventType.equals("ASYNCH_SEND")||
						   otherEvent.eventType.equals("ASYNCH_RECEIVE")) { // assume graph IDs match mapping IDs
								U = intSpace.get(otherEvent.U); // same for both transitions (asserted above)
								C = intSpace.get(otherEvent.C);								
								if (otherEvent.eventType.equals("ASYNCH_RECEIVE")) { // no openlist for asynch_sends
		  						  	// could also look at mapping of this event, the mapping info is the same
								   if (g.isMedium()) { // get openlist of current state of non-medium
								     receivingThreadsOpenList = otherGraph.getCurrentStatesOpenList();
								     receivingThread = intSpace.get(otherEvent.ID);
									  sendingThread = intSpace.get(firstID);
									  if (otherGraph.isMedium()) {
								     		out.println("internal error: two mediums synched");
								     		System.exit(1);
								     }
								   }
								   else { // othergraph is the medium 
								     receivingThreadsOpenList = g.getCurrentStatesOpenList();
								     receivingThread = intSpace.get(firstID);
									  sendingThread = intSpace.get(otherEvent.ID);								     
								     if (g.isMedium()) {
								     		out.println("internal error: two mediums synched");
								     		System.exit(1);
								     }
								   }
								}
								else { // this is an ASYNCH_SEND event so no openlist
								   if (g.isMedium()) { // get openlist of current state of non-medium
								     receivingThread = intSpace.get(firstID);
									  sendingThread = intSpace.get(otherEvent.ID);
  									  if (otherGraph.isMedium()) {
								     		out.println("internal error: two mediums synched");
								     		System.exit(1);
								     }
								   }
								   else { // othergraph is medium
								     receivingThread = intSpace.get(otherEvent.ID);
									  sendingThread = intSpace.get(firstID);
									  if (g.isMedium()) {
								     		out.println("internal error: two mediums synched");
								     		System.exit(1);
								     }
									}	
								}
						}
						//else ... for mapped synchronous events need to figure which is send and which
						// is receive and grab openlist of receiver's current state
						// Q: how know which is sender/receiver? !senderID ?receiverID on sender and reverse on receiver
						//   so look at IDs and see if senderID matches ID of graph? where graph IDs
						// match mapping IDs, i.e., graphs listed in FileList in mapped order
						//receivingThreadsOpenList = g.getCurrentStatesOpenList();
						//receivingThreadsOpenList = otherGraph.getCurrentStatesOpenList();				
					} else {
						if (aBar.charAt(0)=='\'') {
							receivingThread = intSpace.get(firstID);
							receivingThreadsOpenList = g.getCurrentStatesOpenList();
							sendingThread = intSpace.get(otherEvent.ID);
						}
						else {
							receivingThread = intSpace.get(otherEvent.ID);
							sendingThread = intSpace.get(firstID);
							receivingThreadsOpenList = otherGraph.getCurrentStatesOpenList();
						}
					}
					
					//System.out.println("sending thread: "+sendingThread+" , "+"receivingThread: " + receivingThread);
					//if (mappedSequence)
					//	System.out.println("C: "+C+" , "+"U: " + U);

					if (mappedSequence) {
						if (otherEvent.eventType.equals("ASYNCH_SEND")||
						   otherEvent.eventType.equals("ASYNCH_RECEIVE")) { // assume graph IDs match mapping IDs
						   if (otherEvent.eventType.equals("ASYNCH_RECEIVE")) {
								if (!RTAllRecollected) {
									if (recollectedThreads.containsKey(U)) {
										if (U != receivingThread) {
											out.println("internal error: U != receiving thread on asynch_receive");
											System.exit(1);
											// note: sendingThread != C since sending thread is medium
										}
										// this is a recollected thread, so make sure not being matched to oll send
										LinkedList oldSenders = (LinkedList)recollectedOldSenders.get(U);
										//if (recollectedOldSenders.contains(sendingThread)) {
										if (oldSenders != null && oldSenders.contains(C)) {
											//System.out.println(C + " is an old sender");
											// old send so no match
											continue;

										}
										else {
											srEvent e = (srEvent) recollectedThreads.get(U);
											recollectedChannels.remove(e.getChannelName());
											recollectedThreads.remove(U);
											recollectedNoOldOrNewSends.remove(U);								
											if (oldSenders != null)
												oldSenders.clear();
											/* wrong: need to remove other senders so they can now synch: This was fixed */
											recollectedOldSenders.remove(U);
											if (recollectedThreads.size() == 0)
												RTAllRecollected = true;
										}
									}
								}					   
						   }
						}
					}
					else { // not mapped
						if (!RTAllRecollected) {
							if (recollectedThreads.containsKey(receivingThread)) {
								// this is a recollected thread, so make sure not being matched to oll send
								LinkedList oldSenders = (LinkedList)recollectedOldSenders.get(receivingThread);
								//if (recollectedOldSenders.contains(sendingThread)) {
								if (oldSenders != null && oldSenders.contains(sendingThread)) {
									//System.out.println(sendingThread + " is an old sender");
								// old send so no match
									continue;
								}
								else {
									srEvent e = (srEvent) recollectedThreads.get(receivingThread);
									recollectedChannels.remove(e.getChannelName());
									recollectedThreads.remove(receivingThread);
									recollectedNoOldOrNewSends.remove(receivingThread);								
									if (oldSenders != null)
										oldSenders.clear();
									/* wrong: need to remove other senders so they can now synch: This was fixed */
									recollectedOldSenders.remove(receivingThread);
									if (recollectedThreads.size() == 0)
										RTAllRecollected = true;
								}
							}
						}
					}	
					
					LinkedList savedExercisedTransitions = null; // used below to set inhibited
					ArrayList inhibitedCallers = null;
					if (sbrtMode == CycleDetection) {
					// check BOTH old transitions and inhibited transitions, since both could contain
					// transitions. (Used to do one or the other depending on cycleState.get().)
					   if (!cycleState.get()) {
/*7d*/  				  System.out.println("find match not in cycle state");
						}
						else {
/*5*/					   System.out.println("find match in cycle state check for old match, current state is: "+currentState);

							LinkedList exercisedTransitions = (LinkedList) (statesToEvents.get(currentState));
							if (exercisedTransitions != null)
								savedExercisedTransitions = new LinkedList(exercisedTransitions);
							if (exercisedTransitions != null) {
/*6*/							System.out.println("exercisedTransitions.size() is " + exercisedTransitions.size());
								Iterator b = exercisedTransitions.iterator();
								boolean found = false;
								while (b.hasNext()) {
									srEvent e = (srEvent) b.next();
/*7*/								System.out.println("check e, e-caller is " + e.getCaller() + 
										", found caller is " +  sendingThread.intValue() +
										", e-called is " + e.getCalled() +
										", found called is " +  receivingThread.intValue());
									if (e.getCaller() == sendingThread.intValue() &&
										 e.getCalled() == receivingThread.intValue()) {
										found = true;
//7b									System.out.println("match is old");
										break;
									}
								}
								if (found) continue;
								else {
//7c								System.out.println("match is not old");
									// set inhibitedCallers
									b = exercisedTransitions.iterator();
									while (b.hasNext()) {
										srEvent e = (srEvent) b.next();
/*7*/									System.out.println("check e for adding to inhibitedCallers, e-caller is " 
										+ e.getCaller() + 
										", found caller is " +  sendingThread.intValue() +
										", e-called is " + e.getCalled() +
										", found called is " +  receivingThread.intValue());
										if (e.getCalled() == receivingThread.intValue()) {
											if (inhibitedCallers == null) inhibitedCallers = new ArrayList();
											inhibitedCallers.add(new Integer(e.getCaller()));
/*7b*/									System.out.println("to inhibitedCallers add old event caller "+ e.getCaller() + " to inhibitedCallers");
										}
									}				
								}
							}
						}
						if (inhibited.size()>0) {
/*7e*/					System.out.println("inhibited.size() is " + inhibited.size());
							Iterator b = inhibited.iterator();
							boolean found = false;
							while (b.hasNext()) {
								srEvent e = (srEvent) b.next();
/*7e*/						System.out.println("check e, e-caller is " + e.getCaller() + 
									", found caller is " +  sendingThread.intValue() +
								 ", e-called is " + e.getCalled() +
									", found called is " +  receivingThread.intValue());
								if (e.getCaller() == sendingThread.intValue() &&
									 e.getCalled() == receivingThread.intValue()) {
									found = true;
/*7f*/							System.out.println("match is inhibited");
									break;
								}
							}
							if (found) continue;
							else {
//7g							System.out.println("match is not inhibited");
								// set inhibitedCallers
								b = inhibited.iterator();
								while (b.hasNext()) {
									srEvent e = (srEvent) b.next();
/*7*/								System.out.println("check e for adding to inhibitedCallers, e-caller is " 
										+ e.getCaller() + 
										", found caller is " +  sendingThread.intValue() +
										", e-called is " + e.getCalled() +
										", found called is " +  receivingThread.intValue());
									// prevent races between caller of this selected event and 
									// caller of inhibited event or any event that caller
									// executes after inhibited event
									//if (e.getCalled() == receivingThread.intValue()) {
										if (inhibitedCallers == null) inhibitedCallers = new ArrayList();
										inhibitedCallers.add(new Integer(e.getCaller()));
//7b									System.out.println("to inhibited Callers add inhibited caller "+ e.getCaller() + " to inhibitedCallers");
									//}
								}
							}
						}
//7h					
						else System.out.println("inhibited is empty");
					}

					
					Trans t = otherEvent.t;
					// generate srEvent E from firstID, otherID, arec a and trans t
					// update current state of g and other
/*8*/				//System.out.println("Event:"+firstID+","+otherEvent.ID+","+a.getLabel()+"<-->"+aBar);
					int gCurrentState = g.getCurrentState();
					int otherCurrentState = otherEvent.source;

					g.setCurrentState(((Trans)a.getTransitionList().get(0)).getDestinationState()); 
					otherGraph.setCurrentState(otherEvent.destination);
					
					int otherDestinationState = otherEvent.destination;
					int gDestinationState = g.getCurrentState(); // having changed current state to dest. already

					srEvent sendEvent2 = null; // filled by asynch_receive with sending event
					if (mappedSequence) {
						if (otherEvent.eventType.equals("ASYNCH_SEND") ||
						   otherEvent.eventType.equals("ASYNCH_RECEIVE")) { // assume graph IDs match mapping IDs
								if (otherEvent.eventType.equals("ASYNCH_SEND")) {
									sendEvent = new srEvent(); // eventPool.acquire();
									sendEvent.setCaller(C.intValue()); 
									sendEvent.setCalled(U.intValue()); 
									sendEvent.setCallerVersionNumber(versionNumbers[C.intValue()]);
/* RHC */
									sendEvent.setCalledVersionNumber(1); 
									if (!(messages.containsKey(C))) { // put sender in map
										//System.out.println("new messages linked list for " + ID);
										messages.put(C,new LinkedList());
									}
									//else System.out.println("no new messages linked list for " + ID);
									LinkedList sends = (LinkedList)messages.get(C);
									//sends.addLast(intSpace.get(versionNumbers[C.intValue()]));
									sends.addLast(sendEvent);
									//
								}
								else { // ASYNCH_RECEIVE
									E = new srEvent(); // eventPool.acquire();
									E.setCaller(C.intValue()); 
									E.setCalled(U.intValue());
									
									if (!(messages.containsKey(C))) {
										out.println("internal error: on match, no message linked list for " + C);
										System.exit(1);
									}
									LinkedList sends = (LinkedList)messages.get(C);
									if (sends.size() == 0) {
										out.println("internal error: empty version number list for caller " + C);
										System.exit(1);
									}
									//Integer versionNumber = (Integer) sends.removeFirst(); 
									ListIterator L = sends.listIterator();
									boolean found = false;
									while (L.hasNext()) {
										srEvent s = (srEvent) L.next();
										if (s.getCalled() == E.getCalled()) {
											found = true;
											sendEvent2 = s;
											L.remove();
											break;									
										}
									}
									if (!found) {
										out.println("internal error: during tracing for asynch_receive, no send event found for caller  " + E.getCaller() + " and called " + E.getCalled());
										System.exit(1);					
									}
									if (sendEvent2.getCalled() != U.intValue()) {
										out.println("internal error: during tracing for asynch_receive, called ID  " + U + " does not match called ID in sender" + sendEvent2.getCalled());
										System.exit(1);								
									}
/* Need to compute timestamps, etc */
									//sendEvent2 = (srEvent) sends.removeFirst();
									// sendEvent2 also used below when updating timestamp of receive
									//E.setCallerVersionNumber(versionNumber.intValue()); 
									E.setCallerVersionNumber(sendEvent2.getCallerVersionNumber()); 
									sendEvent2.setCalledVersionNumber(versionNumbers[U.intValue()]);
									if (!sendEvent2.getEventType().equals(eventTypeParameters.UNACCEPTED_ASYNCH_SEND)) {
										out.println("internal error: new send was not created as UNACCEPTED_ASYNCH_SEND.");
										System.exit(1);						
									}
									sendEvent2.setEventType(eventTypeParameters.ASYNCH_SEND);
									sendEvent2.setEventType2(eventTypeParameters.ASYNCH_SEND);
									E.setCalledVersionNumber(versionNumbers[U.intValue()]); 					
									E.setInhibitedCallers(inhibitedCallers);
								}
						}
					}
					else {
						E = new srEvent(); // eventPool.acquire();
						sendEvent = new srEvent(); // eventPool.acquire();
						E.setCaller(sendingThread.intValue()); E.setCalled(receivingThread.intValue()); 
						E.setCallerVersionNumber(versionNumbers[sendingThread.intValue()]); 
						E.setCalledVersionNumber(versionNumbers[receivingThread.intValue()]); 
						E.setInhibitedCallers(inhibitedCallers);
					}
				
					if (sbrtMode == CycleDetection) {
						// same as previous destination, so no need to add
						E.setSourceGlobalState((stateVector) globalState.clone());
						sendEvent.setSourceGlobalState((stateVector) globalState.clone());
						if (!(statesToEvents.containsKey(currentState))) { // put state in map
								statesToEvents.put(currentState,new LinkedList());
						}
						//else System.out.println("no new linked list for " + ID);
						LinkedList events = (LinkedList)statesToEvents.get(currentState);
						events.addLast(E); // add event to end of list
						// we will be completing E below
					}

					if (mappedSequence) {
						if (otherEvent.eventType.equals("ASYNCH_SEND") ||
						   otherEvent.eventType.equals("ASYNCH_RECEIVE")) { // assume graph IDs match mapping IDs
								if (otherEvent.eventType.equals("ASYNCH_SEND")) {
									if (g.isMedium()) { 
										sendEvent.setSourceStateSender(otherCurrentState);
										sendEvent.setDestinationStateSender(otherDestinationState);
										sendEvent.setSourceStateReceiver(gCurrentState);
										sendEvent.setDestinationStateReceiver(gDestinationState);
									}
									else {
										sendEvent.setSourceStateSender(gCurrentState);
										sendEvent.setDestinationStateSender(gDestinationState);
										sendEvent.setSourceStateReceiver(otherCurrentState);
										sendEvent.setDestinationStateReceiver(otherDestinationState);
									}
								}
								else { //ASYNCH_RECEIVE
									if (g.isMedium()) { 
										E.setSourceStateReceiver(otherCurrentState);
										E.setDestinationStateReceiver(otherDestinationState);	
										E.setSourceStateSender(gCurrentState);
										E.setDestinationStateSender(gDestinationState);
									}
									else {
										E.setSourceStateReceiver(gCurrentState);
										E.setDestinationStateReceiver(gDestinationState);
										E.setSourceStateSender(otherCurrentState);
										E.setDestinationStateSender(otherDestinationState);
									}							
								}
						}
					}
					else {
						if (aBar.charAt(0)=='\'') {
							E.setSourceStateSender(otherCurrentState);
							E.setDestinationStateSender(otherDestinationState);
							E.setSourceStateReceiver(gCurrentState);
							E.setDestinationStateReceiver(gDestinationState);
							sendEvent.setSourceStateSender(otherCurrentState);
							sendEvent.setDestinationStateSender(otherDestinationState);
							if (sbrtMode == CycleDetection) {
								//System.out.println("set Global State");
								globalState.setLocal(sendingThread.intValue(),otherDestinationState);
								globalState.setLocal(receivingThread.intValue(),gDestinationState);
								currentState.setLocal(sendingThread.intValue(),otherDestinationState);
								currentState.setLocal(receivingThread.intValue(),gDestinationState);
								//System.out.println("FindMatch: set globalState to " + globalState);
								//System.out.println("FindMatch: set currentState to " + currentState);
							}
						}
						else {
							E.setSourceStateSender(gCurrentState);
							E.setDestinationStateSender(gDestinationState);
							E.setSourceStateReceiver(otherCurrentState);
							E.setDestinationStateReceiver(otherDestinationState);
							sendEvent.setSourceStateSender(gCurrentState);
							sendEvent.setDestinationStateSender(gDestinationState);
							if (sbrtMode == CycleDetection) {
								globalState.setLocal(sendingThread.intValue(),gDestinationState);
								globalState.setLocal(receivingThread.intValue(),otherDestinationState);
								currentState.setLocal(sendingThread.intValue(),gDestinationState);
								currentState.setLocal(receivingThread.intValue(),otherDestinationState);
								//System.out.println("FindMatch: set globalState to " + globalState);
								//System.out.println("FindMatch: set currentState to " + currentState);
							}
						}
					}
					if (mappedSequence) {
						if (otherEvent.eventType.equals("ASYNCH_SEND") ||
						   otherEvent.eventType.equals("ASYNCH_RECEIVE")) { // assume graph IDs match mapping IDs
								if (otherEvent.eventType.equals("ASYNCH_SEND")) {
									vectorTimeStamps[C.intValue()].updateIntegerTS(C.intValue());
									sendEvent.setVectorTimestamp((vectorTimeStamp)(vectorTimeStamps[C.intValue()]).getVectorTS());
								}
								else { // sendEvent2
									vectorTimeStamps[U.intValue()].updateIntegerTS(U.intValue());
									//vectorTimeStamps[U.intValue()].updateVectorTS((vectorTimeStamp)vectorTimeStamps[C.intValue()]);	
									vectorTimeStamps[U.intValue()].updateVectorTS(sendEvent2.getVectorTS());		
									//vectorTimeStamps[C.intValue()].updateVectorTS((vectorTimeStamp)vectorTimeStamps[U.intValue()]); // synch only							
									E.setVectorTimestamp((vectorTimeStamp)(vectorTimeStamps[U.intValue()]).getVectorTS());
								}
						}
					}
					else {
						vectorTimeStamps[sendingThread.intValue()].updateIntegerTS(sendingThread.intValue());
						sendEvent.setVectorTimestamp((vectorTimeStamp)(vectorTimeStamps[sendingThread.intValue()]).getVectorTS());
						vectorTimeStamps[receivingThread.intValue()].updateIntegerTS(receivingThread.intValue());
						vectorTimeStamps[receivingThread.intValue()].updateVectorTS((vectorTimeStamp)vectorTimeStamps[sendingThread.intValue()]);		
						vectorTimeStamps[sendingThread.intValue()].updateVectorTS((vectorTimeStamp)vectorTimeStamps[receivingThread.intValue()]);							
						E.setVectorTimestamp((vectorTimeStamp)(vectorTimeStamps[receivingThread.intValue()]).getVectorTS());
					}

					if (sbrtMode == CycleDetection) {
						E.setDestinationGlobalState((stateVector)globalState.clone());	
						//System.out.println("E globalDestState is " + E.getDestinationGlobalState());
						sendEvent.setDestinationGlobalState((stateVector)globalState.clone());
						String readOrWrite = a.getLabel().substring(1,4);
						// Always use E to remove events from inhibited
						//for each event e in inhibited, remove e from inhibited if !(e||E)
						ListIterator b = inhibited.listIterator();
						vectorTimeStamp EVectorTimestamp = E.getVectorTS();
						while (b.hasNext()) {
							srEvent e = (srEvent) b.next();
							vectorTimeStamp eVectorTimestamp = e.getVectorTS();
							if (readOrWrite.equals("SVW") && eVectorTimestamp.lessThan(EVectorTimestamp)) { 
							// || EVectorTimestamp.lessThan(eVectorTimestamp)) {
			//Q: and E is not a read?
							//if ((E.getCaller() == e.getCaller() || E.getCaller() == e.getCalled()) ||
							//	(E.getCalled() == e.getCaller() || E.getCalled() == e.getCalled())) {
								// E and e are involved with each other
/*9d*/							 //System.out.println("removing from inhibited: " + e);
									 b.remove();
							}	
						}
						
						if (cycleState.get()) { // were in cycle state when E was selected
							//inhibited.clear();
							// add each old event e to inhibited if e || E
							if (savedExercisedTransitions != null) {
								Iterator b2 = savedExercisedTransitions.iterator();
								EVectorTimestamp = E.getVectorTS();
								String readOrWrite2 = a.getLabel().substring(1,4);
								while (b2.hasNext()) {
									srEvent e = (srEvent) b2.next();
									vectorTimeStamp eVectorTimestamp = e.getVectorTS();
									//if (!eVectorTimestamp.lessThan(EVectorTimestamp) &&
									//	 !EVectorTimestamp.lessThan(eVectorTimestamp)) {									 
	//Q: check for read
									if (!readOrWrite2.equals("SVW")||
									    (!eVectorTimestamp.lessThan(EVectorTimestamp) &&
									     !EVectorTimestamp.lessThan(eVectorTimestamp))
										) {
									//if ((E.getCaller() != e.getCaller() && E.getCaller() != e.getCalled()) &&
									//		(E.getCalled() != e.getCaller() && E.getCalled() != e.getCalled())) {
										 // e and f are uninvolved with each other
	//Q: Could add event already in inhibited?
										 inhibited.add(e);
/*9b*/ 								 System.out.println("adding e to inhibited:  e-caller is " + e.getCaller() + 
										 	", e-called is " + e.getCalled());
									}
								}
							}
						}
						
						boolean added = globalStates.add((stateVector)globalState.clone());
						if (!added) {
/*8*/						//System.out.println("Cycle detected for selected event");
							cycleState.set(true);
							//inhibited.clear();
							// no, some events from "old cycle state" may still need to be inhibited
							// since it's possible that none of the events executed since then are
							// related, i.e., we have two cycles without between them being any events related
							// to first cycle
							// NO. but may have changed loop condition parts of which might
							// not be the first transition from cycle state
							// No, don't clear, use => and !read to remove events from inhibited
							//   this was done above
						}
						else { // added
/*9*/						//System.out.println("No Cycle detected for selected event");
			/*
							if (cycleState.get()) { // were in cycle state when E was selected
								//inhibited.clear();
								// add each old event e to inhibited if e || E
								if (savedExercisedTransitions != null) {
									Iterator b2 = savedExercisedTransitions.iterator();
									//vectorTimeStamp EVectorTimestamp = E.getVectorTS();
									while (b2.hasNext()) {
										srEvent e = (srEvent) b2.next();
										//vectorTimeStamp eVectorTimestamp = e.getVectorTS();
										//if (!eVectorTimestamp.lessThan(EVectorTimestamp) &&
										//	 !EVectorTimestamp.lessThan(eVectorTimestamp)) {
										if ((E.getCaller() != e.getCaller() && E.getCaller() != e.getCalled()) &&
												(E.getCalled() != e.getCaller() && E.getCalled() != e.getCalled())) {
											 // e and f are uninvolved with each other
											 inhibited.add(e);
//9b
											//System.out.println("adding e to inhibited:  e-caller is " + e.getCaller() + 
											//	", e-called is " + e.getCalled());
										}
									}
								}
							}
							else { // ! cycleState.get()
			
								//for each event e in inhibited, remove e from inhibited if !(e||E)
								ListIterator b = inhibited.listIterator();
								//vectorTimeStamp EVectorTimestamp = E.getVectorTS();
								while (b.hasNext()) {
									srEvent e = (srEvent) b.next();
									//vectorTimeStamp eVectorTimestamp = e.getVectorTS();
									//if (eVectorTimestamp.lessThan(EVectorTimestamp) ||
									//	 EVectorTimestamp.lessThan(eVectorTimestamp)) {
									if ((E.getCaller() == e.getCaller() || E.getCaller() == e.getCalled()) ||
										(E.getCalled() == e.getCaller() || E.getCalled() == e.getCalled())) {
										// E and e are involved with each other
//9d										 System.out.println("removing from inhibited: " + e);
											 b.remove();
									}	
								}
			
							} // !cycleState.get()
			*/
							cycleState.set(false);
						} // added
						// currentState is parameter so this will be forgotten; set above
						//currentState = (stateVector)globalState.clone();
					} // mode == cycleDetection

					if (mappedSequence) {
						if (otherEvent.eventType.equals("ASYNCH_SEND") ||
						   otherEvent.eventType.equals("ASYNCH_RECEIVE")) { // assume graph IDs match mapping IDs
								if (otherEvent.eventType.equals("ASYNCH_SEND")) {
									versionNumbers[C.intValue()]++;
								}
								else {
									versionNumbers[U.intValue()]++;
								}
						}
					}
					else {
						versionNumbers[sendingThread.intValue()]++;
						versionNumbers[receivingThread.intValue()]++;
					}

					if (mappedSequence) {
						if (otherEvent.eventType.equals("ASYNCH_SEND") ||
						   otherEvent.eventType.equals("ASYNCH_RECEIVE")) { // assume graph IDs match mapping IDs
								if (otherEvent.eventType.equals("ASYNCH_SEND")) {
							  		String label = otherEvent.label;
							  		//System.out.println("label is " + label);
							  		//System.out.println("label.indexOf(!)-1 is " + (label.indexOf("!")-1));
/*relabel 4*/
							  		String newLabel = label.substring(0,label.indexOf("!")-1)+"R"+label.substring(label.indexOf("!"),label.length()); // .replaceAll("_S","_R");
							  		//System.out.println("new label is " + newLabel);
					  				sendEvent.setLabel(newLabel);
					  				sendEvent.setChannelName(newLabel);
									//sendEvent.setChannelName(otherEvent.channelName);
									//sendEvent.setChannelName(otherEvent.label);
									sendEvent.setChannelVersionNumber(-1); // not used
									//sendEvent.setEventType(eventTypeParameters.ASYNCH_SEND);
									sendEvent.setEventType(eventTypeParameters.UNACCEPTED_ASYNCH_SEND);
									//sendEvent.setEventType2(eventTypeParameters.ASYNCH_SEND);
									sendEvent.setEventType2(eventTypeParameters.UNACCEPTED_ASYNCH_SEND);
									//sendEvent.setLabel(otherEvent.label);
									//sendEvent.setIsEntry(false);
									sendEvent.setOpenList(null);	
									tracedsrSequence.add(sendEvent);
									//System.out.println("after all sets " + sendEvent);
								}
								else {
									//E.setChannelName(otherEvent.channelName);
									E.setChannelName(otherEvent.label);
									E.setChannelVersionNumber(-1); // not used
									E.setEventType(eventTypeParameters.ASYNCH_RECEIVE);
									E.setEventType2(eventTypeParameters.ASYNCH_RECEIVE);
									E.setLabel(otherEvent.label);
									E.setIsEntry(false);
									E.setOpenList(receivingThreadsOpenList);
									tracedsrSequence.add(E);
									//System.out.println("after all sets " + E);
								}
						}
					}
					else {					
						if (aBar.charAt(0)=='\'')
							E.setChannelName(a.getLabel());
						else
							E.setChannelName(aBar);
						E.setChannelVersionNumber(-1); // not used
						E.setEventType(eventTypeParameters.SR_SYNCHRONIZATION);
						E.setEventType2(eventTypeParameters.SYNCH_RECEIVE);
						if (aBar.charAt(0)=='\'')
							E.setLabel(a.getLabel());
						else
							E.setLabel(aBar);
						E.setIsEntry(false);
						E.setOpenList(receivingThreadsOpenList);
					
						sendEvent.setCaller(sendingThread.intValue()); sendEvent.setCalled(receivingThread.intValue()); 
						sendEvent.setCallerVersionNumber(E.getCallerVersionNumber()); 
						sendEvent.setCalledVersionNumber(E.getCalledVersionNumber()/*-1*/); 
						sendEvent.setChannelName(E.getChannelName());
						sendEvent.setChannelVersionNumber(-1); // not used
						sendEvent.setEventType(eventTypeParameters.SYNCH_SEND);
						sendEvent.setEventType2(eventTypeParameters.SYNCH_SEND);
						sendEvent.setLabel(E.getLabel());sendEvent.setIsEntry(false);
						sendEvent.setOpenList(null);
						tracedsrSequence.add(sendEvent);
						tracedsrSequence.add(E);
					}
	
					//System.out.println("after all sets " + sendEvent);
//10
					//System.out.println("after all sets " + E);
					//tracedsrSequence.add(sendEvent);
					//tracedsrSequence.add(E);
					//int caller = E.getCaller();
					//if (((Graph)LTS.get(caller)).isTerminated())
					//	nonTerminatedLTS.remove((Graph)LTS.get(caller));
					//int called = E.getCalled();
					//if (((Graph)LTS.get(called)).isTerminated())
					//	nonTerminatedLTS.remove((Graph)LTS.get(called));
					nonTerminatedLTS.clear();
					//System.out.println("clear:"+nonTerminatedLTS.size());
					Iterator l = LTS.iterator();
					l.next();
					Graph gr;
					while (l.hasNext()) {
						if (!((gr=((Graph)l.next())).isTerminated())) {
							nonTerminatedLTS.add(gr);
						}
					}
					//E = new srEvent(1,0, -1, -1, aBar, -1, 
					//		eventTypeParameters.SR_SYNCHRONIZATION, new vectorTimeStamp(),
					//	a.getLabel(), false, null, eventTypeParameters.SR_SYNCHRONIZATION);
//12				
					//System.out.println("findMatch return true");
					return true;
				}
			//} // while j hasNext
		}
//12b	//System.out.println("findMatch return false");
		return false;
	}

	public static void traceTheSequence(ArrayList srSequence) {
		try {
			ObjectOutputStream outputSequence = new ObjectOutputStream(new FileOutputStream("sequence.dat"));
			outputSequence.writeObject(srSequence);
     	} 	
     	catch (IOException e) {
	     		System.err.println("Error while writing trace file: " + e.toString());
   	  		System.exit(1); 
		}
	}
	
	final static class SBRTWatchDog extends Thread {
		public SBRTWatchDog() {super("RTWatchDog");}
		private boolean resultsDisplayed = false;
		private int displayedIndex = -1;
		public void run() {
			int delayTime = 8000; 
			if (clientServer == propertyParameters.Client || clientServer == propertyParameters.Server)
			  delayTime = 180000; 
		   RTStopWatch sw = new RTStopWatch(delayTime);
	      sw.start();  // capture start time
			while (true) {
				int saveIndex = count;
				try {
					Thread.sleep(delayTime);
				}
				catch (InterruptedException e) {}
				boolean showResults = false;	
				if (clientServer != propertyParameters.Client) { // server, or neither client nor server
					showResults = (!resultsDisplayed && saveIndex == count && VariantGenerator.getInstance().appAndGeneratorThreadWaiting())
				                 || (resultsDisplayed && saveIndex != displayedIndex && saveIndex == count
				                 && VariantGenerator.getInstance().appAndGeneratorThreadWaiting());
				}
				else // client
					showResults = (!resultsDisplayed && saveIndex == count 
										&& VariantGenerator.getInstance().appAndGeneratorThreadWaiting()
										&& VariantGenerator.getInstance().clientIsIdle(new RTResult(sw,count,collected)))
				                 	|| (resultsDisplayed && saveIndex != displayedIndex && saveIndex == count
				                 	&& VariantGenerator.getInstance().appAndGeneratorThreadWaiting()
				                 	&& VariantGenerator.getInstance().clientIsIdle(new RTResult(sw,count,collected)));

				//System.out.println("Show results:" + showResults);
//				if ((!resultsDisplayed && saveIndex == count && VariantGenerator.getInstance().appAndGeneratorThreadWaiting())
//					 || resultsDisplayed && saveIndex != displayedIndex && saveIndex == count && VariantGenerator.getInstance().appAndGeneratorThreadWaiting() ) {
				if (showResults) {
	    			sw.end();  // capture end time
	    			out.println();
	    			out.println("Reachability Testing completed at "+ Calendar.getInstance().getTime()+ ".");
					out.println("  Executions:"+count+" / Sequences Collected:"+collected); // +"/"+transitionCount+"/"+eventCount);
	     			out.println("  Elapsed time in minutes: " + sw.elapsedMinutes());
	     			out.println("  Elapsed time in seconds: " + sw.elapsedSeconds());
	     			out.println("  Elapsed time in milliseconds: " + sw.elapsedMillis());
					out.flush();
	    			System.out.println();
	    			System.out.println("Reachability Testing completed at "+ Calendar.getInstance().getTime()+ ".");
					System.out.println("  Executions:"+count+" / Sequences Collected:"+collected); // +"/"+transitionCount+"/"+eventCount);
	     			System.out.println("  Elapsed time in minutes: " + sw.elapsedMinutes());
	     			System.out.println("  Elapsed time in seconds: " + sw.elapsedSeconds());
	     			System.out.println("  Elapsed time in milliseconds: " + sw.elapsedMillis());
					System.out.flush();
					resultsDisplayed = true;
					displayedIndex = saveIndex;
					if (clientServer == propertyParameters.Server) {
						VariantGenerator.getInstance().saveRTResult(0, new RTResult(sw,count,collected));
		   			out.println();
  		   			out.println("(sub)Total Executions:"+ VariantGenerator.getInstance().getTotalSequences() + 
  	   					" (sub)Total Sequences Collected:"+VariantGenerator.getInstance().getTotalCollected());
						out.flush();  
		   			System.out.println();
  		   			System.out.println("(sub)Total Executions:"+ VariantGenerator.getInstance().getTotalSequences() + 
  	   					" (sub)Total Sequences Collected:"+VariantGenerator.getInstance().getTotalCollected());
						System.out.flush();  
					}
					//if (detectDeadlock == DETECTIONON) {
					//	deadlockWatchdog.stopWatchdog();
					//}
					if (modularTesting == propertyParameters.MTON) {
						collector.showSizes();
						if (clientServer != propertyParameters.Client && clientServer != propertyParameters.Server)
							collector.saveUniqueSequences(myID); // saves sequences to local disk; myID =""
					}
					if (sendEmail) {
	 					try {
   	               t.sendIT("  Executions:"+count+" / Sequences Collected:"+collected
      	                     + "\n" + "  Elapsed time in minutes: " + sw.elapsedMinutes());
         	      } catch (Exception e) {}
         	   }
					if (clientServer != propertyParameters.Server && clientServer != propertyParameters.Client) {
                   System.exit(0);
               }
				}
           	if (clientServer == propertyParameters.Server && resultsDisplayed 
                && VariantGenerator.getInstance().clientsRunning()==0 
                && VariantGenerator.getInstance().appAndGeneratorThreadWaiting()) {
                  out.println();
   	     			out.println("Reachability Testing Total Executions:"+ VariantGenerator.getInstance().getTotalSequences());
   	     			out.println("Reachability Testing Total Sequences Collected:"+VariantGenerator.getInstance().getTotalCollected());
 						out.flush();

                  System.out.println();
   	     			System.out.println("Reachability Testing Total Executions:"+ VariantGenerator.getInstance().getTotalSequences());
   	     			System.out.println("Reachability Testing Total Sequences Collected:"+VariantGenerator.getInstance().getTotalCollected());
 						System.out.flush();
						if (modularTesting == propertyParameters.MTON) {
 							out.println();
 							out.println("Modular Testing Results:");
							// save sequences of manager
							saveWorkerSequences(collector.getUniqueSequences(),Integer.parseInt(myID));
							// count unique sequences among all workers and manager
							collector.countModularSequences(); // dumpAllWorkerSequences();
						}
 						out.close();
   	           	System.exit(0);
   	    	}
			}
		}
	}
	
	
	final static class modularEvent implements Serializable {
		private int caller;
		private int called;
		private int callerVersionNumber;
		private int calledVersionNumber;
		private String channelName;
		private String label;
		private eventTypeParameters.eventType eType2;
		
		public modularEvent(int caller, int called, String channelName,
			String label, eventTypeParameters.eventType eType2) {
			this.caller = caller;
			this.called = called;
			this.channelName = channelName;
			this.label = label;
			this.eType2 = eType2;
		}
		public void setCaller(int caller) {
			this.caller = caller;
		}
		public void setCalled(int called) {
			this.called = called;
		}
		public void setChannelName(String channelName) {
			this.channelName = channelName;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public void setEventType2(eventTypeParameters.eventType eType2) {
			this.eType2 = eType2;
		}
		public int getCaller() {
			return caller;
		}
		public int getCalled() {
			return called;
		}
		public String getChannelName() {
			return channelName;
		}
		public String getLabel() {
			return label;
		}
		public eventTypeParameters.eventType getEventType2() {
			return eType2;
		}
	}
 	static Vector[] getUniqueSequences() { 
 		// method of RTDriverSBRT, returns uniqueSeqs to VariantGenerator doClient()
 		// so they can be sent to the manager
		return collector.getUniqueSequences(); 
	}
	
	static void saveWorkerSequences(Vector[] uniqueSequences, int ID) {
		//method of RTDriverSBRT, saves uniqueSequences of worker
		// when they are received by manager
		collector.saveWorkerSequences(uniqueSequences, ID);
	}

	static void dumpAllWorkerSequences() {
		//method of RTDriverSBRT, dumps all uniqueSequence objects of all
		// workers to disk
		collector.dumpAllWorkerSequences();
	}
	
	final static class sequenceCollector {
	   //private static int numThreads = 10; // number of threads plus 1
	   Vector uniqueSequences[] = new Vector[modularTestingThreads+1];
	   Vector[] workerSequences[];

  	   sequenceCollector() {
	   	for (int i=0; i<=modularTestingThreads; i++) 
	      	uniqueSequences[i] = new Vector();
	      if (modularTesting == propertyParameters.MTON) {
	      	workerSequences = new Vector[numWorkers+1][];
	      }
	   }
	   
		void collectUnique(ArrayList sequence) {
			// called for each sequence as it is collected during RT
			//System.out.println("size of sequence is " + sequence.size());
			for (int i=1; i<=modularTestingThreads; i++)
			  check(i,uniqueSequences[i],sequence);
		}
		
		public void collectUnique(Vector sequences, int j) {
			//called by count methods to compute unique sequences over all workers
			//System.out.println("size of sequence is " + sequence.size());
				for (int i=0; i<sequences.size(); i++)
			  checkMerge(j,uniqueSequences[j],(ArrayList)sequences.get(i));
		}
		
		private void check(int ID, Vector v, ArrayList sequence) {
			// called to check uniqueness of sequence against those collected so far
			ArrayList pruned = new ArrayList();
			for (int i=0; i<sequence.size(); i++) {
				srEvent e = (srEvent) sequence.get(i);
				if ((e.getCaller()==ID && e.getEventType()==ASYNCH_SEND) ||
				     (e.getCalled()==ID && e.getEventType()==ASYNCH_RECEIVE)) {
					//pruned.add(e);
					//pruned.add(new modularEvent(e.getCaller(),e.getCalled(),e.getChannelName()));
					pruned.add(new modularEvent(e.getCaller(),e.getCalled(),
                                                e.getChannelName(),
						e.getLabel(),e.getEventType2()));
				}
			}
			//System.out.println("size of pruned is " + pruned.size());
			
			if (pruned.size() == 0) return;
			boolean unique = true;
			outer:
			for (int j=0; j<v.size(); j++) {
				ArrayList seq = (ArrayList) v.get(j);
				//System.out.println("check sequence " + j);
				if (pruned.size() != seq.size())
					continue;
				else {
				   boolean match = true;
				   inner: 
					for (int k=0; k<seq.size(); k++) { // sizes are equal
						//srEvent e1 = (srEvent) pruned.get(k);
						modularEvent e1 = (modularEvent) pruned.get(k);
						//System.out.println(e1);
						//srEvent e2 = (srEvent) seq.get(k);
						modularEvent e2 = (modularEvent) seq.get(k);
						//System.out.println(e2);
						if ((e1.getCaller() != e2.getCaller()) ||
						    (e1.getCalled() != e2.getCalled()) ||
						    (!(e1.getChannelName().equals(e2.getChannelName())))) {
						  match = false;
						  //System.out.println("no match");
						  break inner;
						}
					}
					//System.out.println("match is " + match);
					if (match) {
						//System.out.println("not unique");
						unique = false;
						break outer ;
					}
					//else System.out.println("there was a mismatch");
				}
			}
			if (unique) {
				v.add(pruned);
				//System.out.println("unique sequences" + ID + ": " + v.size());
				//for (int l=0; l<pruned.size(); l++) {
				//	srEvent e = (srEvent) pruned.get(l);
				//	System.out.println(e);
				//}
			}
		}

		private void checkMerge(int ID, Vector v, ArrayList sequence) {
			// called by collectUnique(Vector sequences, int j) to merge
			// unique sequences of workers
			ArrayList pruned = new ArrayList();
			for (int i=0; i<sequence.size(); i++) {
				modularEvent e = (modularEvent) sequence.get(i);
				if ((e.getCaller()==ID /*&& e.getEventType()==ASYNCH_SEND*/) ||
				     (e.getCalled()==ID /*&& e.getEventType()==ASYNCH_RECEIVE*/)) {
					//pruned.add(e);
					//pruned.add(new modularEvent(e.getCaller(),e.getCalled(),e.getChannelName()));
					pruned.add(new modularEvent(e.getCaller(),e.getCalled(),
						e.getChannelName(),
						e.getLabel(),e.getEventType2()));
				}
			}
			//System.out.println("size of pruned is " + pruned.size());

			if (pruned.size() == 0) return;	
			boolean unique = true;
			outer:
			for (int j=0; j<v.size(); j++) {
				ArrayList seq = (ArrayList) v.get(j);
				//System.out.println("check sequence " + j);
				if (pruned.size() != seq.size())
					continue;
				else {
				   boolean match = true;
				   inner: 
					for (int k=0; k<seq.size(); k++) { // sizes are equal
						//srEvent e1 = (srEvent) pruned.get(k);
						modularEvent e1 = (modularEvent) pruned.get(k);
						//System.out.println(e1);
						//srEvent e2 = (srEvent) seq.get(k);
						modularEvent e2 = (modularEvent) seq.get(k);
						//System.out.println(e2);
						if ((e1.getCaller() != e2.getCaller()) ||
						    (e1.getCalled() != e2.getCalled()) ||
						    (!(e1.getChannelName().equals(e2.getChannelName())))) {
						  match = false;
						  //System.out.println("no match");
						  break inner;
						}
					}
					//System.out.println("match is " + match);
					if (match) {
						//System.out.println("not unique");
						unique = false;
						break outer ;
					}
					//else System.out.println("there was a mismatch");
				}
			}
			if (unique) {
				v.add(pruned);
				//System.out.println("unique sequences" + ID + ": " + v.size());
				//for (int l=0; l<pruned.size(); l++) {
				//	srEvent e = (srEvent) pruned.get(l);
				//	System.out.println(e);
				//}
			}
		}
		
		Vector[] getUniqueSequences() {
			return uniqueSequences;
		}
		
		void saveWorkerSequences(Vector[] uniqueSequences, int ID) {
		// saves uniqueSequences of worker when they are received by manager
			workerSequences[ID] = uniqueSequences;
		}
	
		void dumpAllWorkerSequences() {
			//dumps uniqueSequences of workers to disk
			for (int i =0; i<numWorkers+1; i++) {
		   	try {
				  ObjectOutputStream uniqueSequencesFile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("uniqueSequences"+i+".dat")));
			 	  uniqueSequencesFile.writeObject(workerSequences[i]);
				}
				catch (IOException e) {
	     			System.out.println("Error while writing uniqueSequences file: " + e.toString());
		   	}				
			}
		}
		
		void countModularSequences() {
		// Called during distributed RT with modular testing.
		// Assumes uniqueSequences already contains sequences for node 0
		// Show node 1 sizes
			out.println("uniqueSequences0:");
			showSizes();
			//for (int j=1; j<=modularTestingThreads; j++) {
			//  out.println("uniqueSequences["+j+"].size() is " + uniqueSequences[j].size());       
  			//}
				
	  		for (int i = 1; i <= numWorkers; i++) {
	   		try {
		   		System.out.println();
  					out.println("uniqueSequences"+i+":");
					//ObjectInputStream inputInfeasibleSequence = 
					//	new ObjectInputStream(new FileInputStream("uniqueSequences"+i+".dat"));
					// Vector or Vector of ArrayList
					//Vector uniqueSequences[]  = (Vector[])inputInfeasibleSequence.readObject();
					int sum=0;
					for (int j=1; j<=modularTestingThreads; j++) {
				   	out.println("uniqueSequences"+j+":" + workerSequences[i][j].size());       
					sum += workerSequences[i][j].size();
					collectUnique(workerSequences[i][j],j); // unique sequences of thread j
					}
					out.println("-----------------------");
					out.println("                   "+sum);
					out.println("Cumulative:");
					showSizes();
				} catch (Exception e) {e.printStackTrace();}
			}
			saveUniqueSequences("");
	}
		
		void saveUniqueSequences(String client_server_ID) {
		   try {
			  ObjectOutputStream uniqueSequencesFile = new ObjectOutputStream(new FileOutputStream("uniqueSequences"+client_server_ID+".dat"));
			  uniqueSequencesFile.writeObject(uniqueSequences);
			}
			catch (IOException e) {
	     		System.out.println("Error while writing uniqueSequences file: " + e.toString()); 
		   }		
		}
		
		void inputUniqueSequences(String client_server_ID) {
		   try {
			  ObjectInputStream uniqueSequencesFile = new ObjectInputStream(new FileInputStream("uniqueSequences"+client_server_ID+".dat"));
			  uniqueSequences = (Vector []) uniqueSequencesFile.readObject();
			  showSizes();
			}
			catch (IOException e) {
	     		System.out.println("Error while reading uniqueSequences file: " + e.toString());
   	  		System.exit(1); 
		   }
		   catch (ClassNotFoundException e) {
	     		System.out.println("Error while reading uniqueSequences file: " + e.toString());
   	  		System.exit(1); 		   
		   }
		}
		
		void showSizes() {
			//out.println("");
			int sum=0;
			for (int i=1; i<=modularTestingThreads; i++) {
				out.println("unique sequences" + i + ": " + uniqueSequences[i].size());
				sum += uniqueSequences[i].size();
			}
			out.println("-----------------------");
			out.println("                   "+sum);
		}
	
	}

	final static class sendMail {
		private Socket smtpSocket = null;
		private DataOutputStream os = null;
		//private DataInputStream is = null;
		private BufferedReader is = null;
		public void sendIT(String s) {

			Date dDate = new Date();
			DateFormat dFormat =
			    DateFormat.getDateInstance(DateFormat.FULL,Locale.US);

			try { // Open port to server
			  smtpSocket = new Socket("osf1.gmu.edu", 25);
			  os = new DataOutputStream(smtpSocket.getOutputStream());
  			  is = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
			  if(smtpSocket != null && os != null && is != null) {
				 // Connection was made.  Socket is ready for use.
				 os.writeBytes("HELO\r\n");
			    // You will add the email address that the server
			    // you are using know you as.
			    os.writeBytes("MAIL From: <rcarver@gmu.edu>\r\n");

			    // Who the email is going to.
			    os.writeBytes("RCPT To: <rcarver@gmu.edu>\r\n");
			    //IF you want to send a CC then you will have to add this
			    //os.writeBytes("RCPT Cc: <rcarver@gmu.edu>\r\n");


			    // Now we are ready to add the message and the
		   	 // header of the email to be sent out.
			    os.writeBytes("DATA\r\n");

			    os.writeBytes("X-Mailer: Via Java\r\n");
			    os.writeBytes("DATE: " + dFormat.format(dDate) + "\r\n");
			    os.writeBytes("From: rcarver <rcarver@gmu.edu>\r\n");
			    os.writeBytes("To:  rcarver <rcarver@gmu.edu>\r\n");

			    //Again if you want to send a CC then add this.
			    //os.writeBytes("Cc: rcarver <CCPerson@theircompany.com>\r\n");

				 //Here you can now add a BCC to the message as well
			    //os.writeBytes("RCPT Bcc: rcarver <BCC@invisiblecompany.com>\r\n");
 
		   	 os.writeBytes("Subject: SBRT Results\r\n");
				 os.writeBytes(s + "\r\n");
		    	 os.writeBytes("\r\n.\r\n");
			    os.writeBytes(s + "\r\n");
			    os.writeBytes("\r\n.\r\n");
			    os.writeBytes("QUIT\r\n");
			    // Now send the email off and check the server reply.
			    // If an OK is reached you are complete.
			    String responseline;
			    while((responseline = is.readLine())!=null) {
			     // System.out.println(responseline);
		        if(responseline.indexOf("Ok") != -1)
      			 break;
	     		 }
	  	  	  }
	   	}
			catch(Exception e) { 
		 	  e.printStackTrace();
			}
		}
	}

}


	
	// OpenLists: if state has multiple events they are all receives;
	//          : need all labels of such states in OpenList, and the dest. states
	
	//Note1:   'x = send; x = receive
	//Note2:   enter1, exit1, etc. do not synch
	//Note3:   in RT-generated aut files we use (1,terminate,1); so don't
	//         add these transitions but do set terminael = true; (see problem
	//Problem: Some of LTSs, (ports, mediums) do not terminate, but not a deadlock
	//           and not an unaccepted receive either.
