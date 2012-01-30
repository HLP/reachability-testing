package reachability;

import java.util.*;
import java.io.*;

public final class threadBasedMsgTracingAndReplay extends Controller
	implements eventTypeParameters, traceMessageTypeParameters,
	propertyParameters {
// Provides Trace and Replay for Channels. srSequence is a simple 
// send-receive sequence. Trace or Replay is selected by the user. 
// Single or Multiple is selected by user.

   private Vector simplesrSequence = new Vector();
	private Vector srSequence = new Vector();
	private int numberOfThreads; // number of threads in ThreadID file: 1..numberOfThreads
	private static final Object classLock = threadBasedMsgTracingAndReplay.class;
	private static boolean traceSequencesHaveBeenOutput = false;

	private propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	private propertyParameters.Controllers numControllers = SINGLE;
	//private propertyParameters.Strategy strategy = OBJECT;
	//private String traceFileName = null;

	private selectableEntryC requestSendPermit[]; 
	private selectableEntryC requestSelectPermit[]; 
	private selectableEntryC requestReceivePermit[];
	private selectableEntryC requestReceivePermitX[];
	private selectableEntryC requestElseDelayPermit[]; 
	private selectableEntryC requestSendExceptionPermit[]; 
	private selectableEntryC requestReceiveExceptionPermit[];
	private selectableEntryC msgReceived = new selectableEntryC();
	private selectableEntryC msgReceivedX = new selectableEntryC();
	private selectableEntryC traceMsg = new selectableEntryC();
	private selectableEntryC traceSendReceive = new selectableEntryC();

	private HashMap unacceptedSR = new HashMap();
	//private HashMap objectBasedSequences = new HashMap();

	private ThreadBasedTestAndReplayCollection TestAndReplayCollection; 

	//private DataOutputStream outputTrace = null; // output sequence of int IDs
	//private DataInputStream inputTrace = null; // input sequence of int IDs
	private ObjectOutputStream outputReplay = null; // output sequence of int IDs
	//private ObjectInputStream inputReplay = null; // input sequence of int IDs
	private PrintWriter outputReplayText = null;
	private BufferedReader inputReplayText = null;
	private ObjectOutputStream outputTest = null; // output sequence of int IDs
	private ObjectInputStream inputTest = null; // input sequence of int IDs
	private PrintWriter outputTestText = null;
	private BufferedReader inputTestText = null;

	public  void requestSynchSendPermit(int ID, String channelName, int callerVersionNumber) {}
	public boolean requestSendPermitX(int ID, String channelName, int callerVersionNumber) {return false;}
	public void sendArrivedRT(int ID, String channelName, int callerVersionNumber) {}
	public void monitorEnteredRT(int ID, String channelName, int callerVersionNumber) {}

	public threadBasedMsgTracingAndReplay(propertyParameters.Mode mode, 
			propertyParameters.Controllers numControllers,
			propertyParameters.Strategy strategy, String traceFileName) { 
		this.mode = mode;
		this.numControllers = numControllers;
		//this.strategy = strategy;
		//this.traceFileName = traceFileName;

    	if (mode == TRACE) { // trace
      		try {
        			//outputReplay = new ObjectOutputStream(new FileOutputStream(traceFileName+"-replay.dat"));
	     			outputReplayText = new PrintWriter(new FileOutputStream(traceFileName+"-replay.txt"));
        			//outputTest = new ObjectOutputStream(new FileOutputStream(traceFileName+"-test.dat"));
	     			outputTestText = new PrintWriter(new FileOutputStream(traceFileName+"-test.txt"));
					TestAndReplayCollection	= new ThreadBasedTestAndReplayCollection(outputTest,outputTestText,
							outputReplay,outputReplayText,numControllers);
				} 
				catch (IOException e) {
        				System.err.println("File not opened: " + e.toString());
        				System.exit(1);
      		}
				catch (Exception e) {
       			System.err.println("Error: " + e.toString());
        			System.exit(1);
      		}
    }
	 else if (mode == REPLAY) { // replay
		System.out.println("Reading Simple SR-sequence file.");
      try {
			//inputReplay = new ObjectInputStream(new FileInputStream(traceFileName+"-replay.dat"));
  			inputReplayText = new BufferedReader(new FileReader(traceFileName+"-replay.txt"));
      }
      catch (IOException e) {
        System.err.println("Trace file not opened: " + e.toString());
        System.exit(1);
      }
      int lineNo = 1;
      String line = null;
      try {
         while ((line = inputReplayText.readLine()) != null) {
          if (line.length()>1) { // skip lines with returns
           // line must be >=5, with a ',' with a ')' after comma
           if (!(line.length()>=5) || !(line.indexOf(',')>0) ||
               !(line.indexOf(')')>line.indexOf(','))) {
             throw new IOException("Format Error in trace file line "+lineNo+":\n"+line);
           }
           String caller = line.substring(1,line.indexOf(','));
           //String called = line.substring(line.indexOf(',')+1,line.indexOf(')'));
           line = line.substring(line.indexOf(',')+1,line.indexOf(')'));
			  String called = line.substring(0,line.indexOf(','));
           String eType = line.substring(line.indexOf(',')+1);
           simplesrEvent textEvent = new simplesrEvent(Integer.parseInt(caller),Integer.parseInt(called));
           if (eType.equals("sr_synchronization"))
           	textEvent.setEventType(eventTypeParameters.SR_SYNCHRONIZATION);
           else if (eType.equals("asynch_send")) 
           	textEvent.setEventType(eventTypeParameters.ASYNCH_SEND);           
           else if (eType.equals("asynch_receive"))
           	textEvent.setEventType(eventTypeParameters.ASYNCH_RECEIVE);           
           else if (eType.equals("send_exception"))
           	textEvent.setEventType(eventTypeParameters.SEND_EXCEPTION);           
           else if (eType.equals("receive_exception"))
           	textEvent.setEventType(eventTypeParameters.RECEIVE_EXCEPTION);
           else if (eType.equals("unaccepted_synch_send"))
           	textEvent.setEventType(eventTypeParameters.UNACCEPTED_SYNCH_SEND);
           else if (eType.equals("unaccepted_asynch_send")) 
           	textEvent.setEventType(eventTypeParameters.UNACCEPTED_ASYNCH_SEND);           
           else if (eType.equals("unaccepted_receive"))
           	textEvent.setEventType(eventTypeParameters.UNACCEPTED_RECEIVE);           
           else if (eType.equals("synch_send")) 
           	textEvent.setEventType(eventTypeParameters.SYNCH_SEND);           
           else if (eType.equals("synch_receive")) 
           	textEvent.setEventType(eventTypeParameters.SYNCH_RECEIVE);           
           else {
             	System.out.println("Internal Error: unknown event type " + eType + " in trace file");
             	System.exit(1);
           }
           
           simplesrSequence.addElement(textEvent);
           ++lineNo;
          }
         }
      }
      catch (IOException e) {
        System.err.println("Error while reading trace file:\n" + e.toString());
        System.exit(1);
      }
      catch (Exception e) {
        System.err.println("Error while reading trace file line " + lineNo + ": " + line + "\n"+ e.toString());
        System.exit(1);
      }
    }
	 else if (mode == TEST) { // replay
		System.out.println("Reading SR-sequence file.");
      try {
			//inputTest = new ObjectInputStream(new FileInputStream(traceFileName+"-test.dat"));
         inputTestText = new BufferedReader(new FileReader(traceFileName+"-test.txt"));
      }
      catch (IOException e) {
        System.err.println("Trace file not opened: " + e.toString());
        System.exit(1);
      }

   	try {   
			int lineNo = 1;
			String line = null;
      	while ((line = inputTestText.readLine()) != null) {
		     StringTokenizer t = new StringTokenizer(line," (,)");
		     if (t.countTokens() != 7) {
		     		System.out.println("Format Error in trace file line "+lineNo+":\n"+line
	      			+ ": Expecting an event with 4 fields, read an event with " 
	      			+ t.countTokens() + " fields");
	      		System.exit(1);
	   		}
	   		/*
					outputTestText.println("("+threadEvent.getOtherThread()+","+threadEvent.getOtherThreadVersionNumber()+","+
						threadEvent.getThisThread()+","+threadEvent.getThisThreadVersionNumber()+","+threadEvent.getChannelName()+
						","+threadEvent.getChannelVersionNumber()+","+threadEvent.getEventType()+")");
				*/
		      String otherThread_S = t.nextToken();
		      String otherThreadVersionNumber_S = t.nextToken();
  		      String thisThread_S = t.nextToken();
		      String thisThreadVersionNumber_S = t.nextToken();
		      String channelName_S = t.nextToken();
		      String channelVersionNumber_S = t.nextToken();
		      String eType_S = t.nextToken();
		      eventTypeParameters.eventType eType = null;
	         if (eType_S.equals("sr_synchronization"))
	           	eType = eventTypeParameters.SR_SYNCHRONIZATION;
          	else if (eType_S.equals("asynch_send")) 
	           	eType = eventTypeParameters.ASYNCH_SEND;           
	         else if (eType_S.equals("asynch_receive"))
	           	eType = eventTypeParameters.ASYNCH_RECEIVE;           
	         else if (eType_S.equals("send_exception"))
	           	eType = eventTypeParameters.SEND_EXCEPTION;           
	         else if (eType_S.equals("receive_exception"))
	           	eType = eventTypeParameters.RECEIVE_EXCEPTION;
	         else if (eType_S.equals("unaccepted_synch_send"))
	           	eType = eventTypeParameters.UNACCEPTED_SYNCH_SEND;
	         else if (eType_S.equals("unaccepted_asynch_send")) 
	           	eType = eventTypeParameters.UNACCEPTED_ASYNCH_SEND;           
	         else if (eType_S.equals("unaccepted_receive"))
	           	eType = eventTypeParameters.UNACCEPTED_RECEIVE;           
	         else if (eType_S.equals("synch_send")) 
	           	eType = eventTypeParameters.SYNCH_SEND;           
	         else if (eType_S.equals("synch_receive")) 
	           	eType = eventTypeParameters.SYNCH_RECEIVE;           
	         else {
	           	System.out.println("Internal Error: unknown event type " + eType + " in trace file");
	           	System.exit(1);
	         }
		      int otherThread=-1;
		      try {
			      	otherThread = Integer.parseInt(otherThread_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + otherThread_S); 
				      System.exit(1);
			   }
		      int thisThread=-1;
		      try {
			      	thisThread = Integer.parseInt(thisThread_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + thisThread_S); 
				      System.exit(1);
			   }
		      int otherThreadVersionNumber=-1;
		      try {
			      	otherThreadVersionNumber = Integer.parseInt(otherThreadVersionNumber_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + otherThreadVersionNumber_S); 
				      System.exit(1);
			   }
		      int thisThreadVersionNumber=-1;
		      try {
			      	thisThreadVersionNumber = Integer.parseInt(thisThreadVersionNumber_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + thisThreadVersionNumber_S); 
				      System.exit(1);
			   }
			  	int channelVersionNumber=-1;
		      try {
			      	channelVersionNumber = Integer.parseInt(channelVersionNumber_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + channelVersionNumber_S); 
				      System.exit(1);
			   }
			   
	      	srThreadEvent event = new srThreadEvent(thisThread,thisThreadVersionNumber,otherThread,otherThreadVersionNumber,channelName_S,channelVersionNumber,eType,null);
				srSequence.add(event);      	
				lineNo++;
			}      	
		}
		
//      srThreadEvent event;
//      try {
//			while (true) {
//				event = (srThreadEvent)inputTest.readObject();
//          	srSequence.addElement(event); 
//         }
//      }
//      catch (ClassNotFoundException e) { }	


      catch (EOFException eof) {
			try {
		  		inputTestText.close();
		  		//inputTest.close();
			}
			catch (Exception e) {
				System.err.println("Error closing trace file.");
			}
      }
      catch (IOException e) {
        System.err.println("Error while reading trace file: " + e.toString());
        System.exit(1);
      }
    }
	 if (mode == REPLAY || mode == TEST) { 
		numberOfThreads = ThreadIDGenerator.getInstance().getNumberOfThreads();
		requestSendPermit = new selectableEntryC[numberOfThreads+1];
		requestSelectPermit = new selectableEntryC[numberOfThreads+1];
		requestReceivePermit = new selectableEntryC[numberOfThreads+1];
		requestReceivePermitX = new selectableEntryC[numberOfThreads+1];
		requestElseDelayPermit = new selectableEntryC[numberOfThreads+1];
		requestSendExceptionPermit = new selectableEntryC[numberOfThreads+1];
		requestReceiveExceptionPermit = new selectableEntryC[numberOfThreads+1];
		for (int i=0; i<(numberOfThreads+1);i++) {
			requestSendPermit[i] = new selectableEntryC();
			requestSelectPermit[i] = new selectableEntryC();
			requestReceivePermit[i] = new selectableEntryC();
			requestReceivePermitX[i] = new selectableEntryC();
			requestElseDelayPermit[i] = new selectableEntryC();
			requestSendExceptionPermit[i] = new selectableEntryC();
			requestReceiveExceptionPermit[i] = new selectableEntryC();
		}
	}

}

	public void traceMsg(traceMessage m) {
		try {
			traceMsg.call(m);
		} catch(InterruptedException e) {}
	}

	public void traceSendReceive(traceMessage m) {
		try {
			traceSendReceive.call(m);
		} catch(InterruptedException e) {}
	}

	public void requestSendPermit(int ID, String channelName, int callerVersionNumber) {
		try {
			requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber);
			requestSendPermit[ID].call(m);
		} catch(InterruptedException e) {}
	}

	public void requestReceivePermit(int ID, String channelName, int calledVersionNumber) {
		try {
			requestPermitMessage  m = new requestPermitMessage(channelName,calledVersionNumber);
			requestReceivePermit[ID].call(m);
		} catch(InterruptedException e) {}
	}
	
	public int requestReceivePermitX(int ID, String channelName, int calledVersionNumber) {
		try {
			requestPermitMessage  m = new requestPermitMessage(channelName,calledVersionNumber);;
			Integer I = (Integer) requestReceivePermitX[ID].call(m);
			return I.intValue();

		} catch(InterruptedException e) {System.exit(1);}
		  //catch(ArrayIndexOutOfBoundsException e) {
		  //	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 15.  ");
		  //	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  //	System.exit(1);
		  //}
		return -1;
	}
	
	public boolean requestSelectPermit(int ID) {
		boolean oneArrival = false;
		try {
			oneArrival = ((Boolean)requestSelectPermit[ID].call()).booleanValue();
			return oneArrival;
		} catch(InterruptedException e) {
			return oneArrival;
			}
	}

	public void requestElseDelayPermit(int ID, int calledVersionNumber) {
		try {
			requestPermitMessage  m = new requestPermitMessage("",calledVersionNumber);
			requestElseDelayPermit[ID].call(m);
		} catch(InterruptedException e) {}
	}

	public void requestSendExceptionPermit(int ID, String channelName, 
					int callerVersionNumber, int calledVersionNumber) {
		// no need to check calledVersionNumber so we ignore it
		try {
			requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber);
			requestSendExceptionPermit[ID].call(m);
		} catch(InterruptedException e) {}
	}

	public void requestReceiveExceptionPermit(int ID, String channelName, int calledVersionNumber) {
		try {
			requestPermitMessage  m = new requestPermitMessage(channelName,calledVersionNumber);
			requestReceiveExceptionPermit[ID].call(m);
		} catch(InterruptedException e) {}
	}


	public void msgReceived() {
		try {
     		msgReceived.call();
		} catch(InterruptedException e) {}
	}
	
	public void msgReceived(int caller, int callerVersionNumber) {
		try {
			msgReceivedMessage m = new msgReceivedMessage(caller, callerVersionNumber);
	  		msgReceivedX.call(m);
		} catch(InterruptedException e) {}
	}

	private void outputTrace() throws IOException {

			try {
				traceMessage traceEvent = (traceMessage)traceMsg.accept();
				srEvent event = (srEvent)traceEvent.getMsg();

				// compute threadEvent
				srThreadEvent threadEvent;
				if (traceEvent.getMType() == SR_LINK_EVENT_TYPE ||
					 traceEvent.getMType() == SR_PORT_EVENT_TYPE ||
					 traceEvent.getMType() == SR_MAILBOX_EVENT_TYPE ||
					 traceEvent.getMType() == SR_ENTRY_EVENT_TYPE ||
					 traceEvent.getMType() == END_ENTRY_TYPE ||
					 traceEvent.getMType() == ASYNCH_RECEIVE_PORT_EVENT_TYPE ||
					 traceEvent.getMType() == ASYNCH_RECEIVE_LINK_EVENT_TYPE ||
					 traceEvent.getMType() == ASYNCH_RECEIVE_MAILBOX_EVENT_TYPE) {
					 threadEvent = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
					   event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());	
				}
				else if (traceEvent.getMType() == ELSE_DELAY_TYPE ||
    					 traceEvent.getMType() == RECEIVE_EXCEPTION_TYPE) {
					threadEvent = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
						event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());	
				}
  				else { //  traceEvent.getMType() == SEND_EXCEPTION_TYPE || ASYNCH_SEND_PORT/link/mailbox_EVENT_TYPE
					threadEvent = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
						event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());	
				}

				int testAndReplayIndex = TestAndReplayCollection.updateSequence(threadEvent);

				srEvent objectSequenceEvent = null;
				srThreadEvent threadSequenceEvent = null;


				if (numControllers == MULTIPLE) {

					// add to object-based trace
					if (!(event.getEventType().equals(ELSE_DELAY))) { // no channel associated with ELSE_DELAY events
						Integer key = new Integer(channelIDGenerator.getInstance().getChannelID(event.getChannelName()));
						objectSequenceEvent = event;
						objectBasedSequenceCollection.getInstance().updateSequence(event,key);
					}

					/* add to thread-based trace sequence : trace file different from replay/test files:
					   trace is per thread and events are send or receive - useful for visualization format */
					srThreadEvent threadEvent2;
					Integer key2;
					if (traceEvent.getMType() == SR_LINK_EVENT_TYPE ||
						 traceEvent.getMType() == SR_PORT_EVENT_TYPE ||
						 traceEvent.getMType() == SR_MAILBOX_EVENT_TYPE ||
						 traceEvent.getMType() == SR_ENTRY_EVENT_TYPE ||
						 traceEvent.getMType() == END_ENTRY_TYPE ||
						 traceEvent.getMType() == ASYNCH_RECEIVE_PORT_EVENT_TYPE ||
						 traceEvent.getMType() == ASYNCH_RECEIVE_LINK_EVENT_TYPE ||
						 traceEvent.getMType() == ASYNCH_RECEIVE_MAILBOX_EVENT_TYPE) {
						threadEvent2 = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
							event.getChannelName(),event.getChannelVersionNumber(),THREAD_BASED_RECEIVE,event.getVectorTS());	
						key2 = new Integer(event.getCalled());
					}
					else if (traceEvent.getMType() == ELSE_DELAY_TYPE ||
    						 traceEvent.getMType() == RECEIVE_EXCEPTION_TYPE) {
						threadEvent2 = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
							event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());	
						key2 = new Integer(event.getCalled());
					}
  					else { //  traceEvent.getMType() == SEND_EXCEPTION_TYPE || ASYNCH_SEND_PORT/LINK/MAIBOX_EVENT_TYPE
						threadEvent2 = new srThreadEvent(event.getCaller(),event.getCallerVersionNumber(),event.getCalled(),event.getCalledVersionNumber(),
							event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());	
						key2 = new Integer(event.getCaller());
					}
					threadSequenceEvent = threadEvent2;
					ThreadBasedSequenceCollection.getInstance().updateSequence(threadEvent2,key2);

					// create thread-based event for the caller for synchronous synchronizations
					srThreadEvent threadEvent3;
					Integer key3;
					if (traceEvent.getMType() == SR_LINK_EVENT_TYPE ||
						 traceEvent.getMType() == SR_PORT_EVENT_TYPE ||
						 traceEvent.getMType() == SR_MAILBOX_EVENT_TYPE ||
						 traceEvent.getMType() == SR_ENTRY_EVENT_TYPE ||
						 traceEvent.getMType() == END_ENTRY_TYPE) {
					    threadEvent3 = new srThreadEvent(event.getCaller(),event.getCallerVersionNumber(),event.getCalled(),event.getCalledVersionNumber(),
						event.getChannelName(),event.getChannelVersionNumber(),THREAD_BASED_SEND,event.getVectorTS());	
						key3 = new Integer(event.getCaller());

						ThreadBasedSequenceCollection.getInstance().updateSequence(threadEvent3,key3);
					}
				}

				if (event.getEventType().equals(UNACCEPTED_ASYNCH_SEND))
					if (numControllers == MULTIPLE) {
						addUnacceptedAsynchSend(event,testAndReplayIndex,objectSequenceEvent,threadSequenceEvent);
					}
					else
						addUnacceptedAsynchSend(event,testAndReplayIndex);

				traceMsg.reply();			// make sure trace gets written
			}
			catch (InterruptedException r) {}
			catch (ClassCastException e) {System.out.println("Error: " + e);}

	}


	private void processSendReceive() {
		try {
			traceMessage traceEvent = (traceMessage)traceSendReceive.acceptAndReply();
			if (traceEvent.getMType() == ADD_UNACCEPTED_SEND_TYPE ||
				traceEvent.getMType() == ADD_UNACCEPTED_RECEIVE_TYPE) {
					addSendReceive(traceEvent);
			}
			else {
					removeSendReceive(traceEvent);
			}
		}	catch (InterruptedException r) {}
	}

	private void addSendReceive(traceMessage traceEvent) {
			srEvent event = (srEvent)traceEvent.getMsg();
			unacceptedSREvent unaccepted = new unacceptedSREvent(event);
			if (event.getEventType() == UNACCEPTED_SYNCH_SEND) {
				ArrayList sequence;
				Integer key = new Integer(event.getCaller());
				if (unacceptedSR.containsKey(key)) 
					sequence = (ArrayList)unacceptedSR.get(key);
				else
					sequence = new ArrayList();
				sequence.add(unaccepted);
				unacceptedSR.put(key,sequence);
				//System.out.println("Added unaccepted event " + unaccepted);

			}
         else if (event.getEventType() == UNACCEPTED_RECEIVE) {
				ArrayList sequence;
				Integer key = new Integer(event.getCalled());
				if (unacceptedSR.containsKey(key)) 
					sequence = (ArrayList)unacceptedSR.get(key);
				else
					sequence = new ArrayList();
				sequence.add(unaccepted);
				unacceptedSR.put(key,sequence);
			}
			else {
				System.out.println("Internal Error in addSendReceive().");
				System.exit(1);
			}
	}

	private void removeSendReceive(traceMessage traceEvent) {
			srEvent event = (srEvent)traceEvent.getMsg();
			unacceptedSREvent unaccepted = new unacceptedSREvent(event);

			if (traceEvent.getMType() == REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE ||
					traceEvent.getMType() == REMOVE_UNACCEPTED_SEND_TYPE) {
				int actualCalledThread = 0; // thread that received the asynch_Send

				int actualCalledVersionNumber = 0;
				if (event.getEventType().equals(ASYNCH_RECEIVE)) {
					actualCalledThread = event.getCalled();
					actualCalledVersionNumber = event.getCalledVersionNumber();
					unaccepted.setEventType(UNACCEPTED_ASYNCH_SEND);
					unaccepted.setCaller(event.getCaller());
				}
				else
					unaccepted.setEventType(UNACCEPTED_SYNCH_SEND);
				Integer key = new Integer(event.getCaller());
				ArrayList sequence;
				sequence = (ArrayList) unacceptedSR.get(key);
				unacceptedSREvent removedEvent = (unacceptedSREvent) sequence.remove(sequence.indexOf(unaccepted));
				if (removedEvent == null)
					System.out.println("Internal Error: Failure removing unaccepted send");
				unacceptedSR.put(key,sequence);
				
				if (removedEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
					int testAndReplayIndex = removedEvent.getTestAndReplayIndex();
					TestAndReplayCollection.changeToAccepted(testAndReplayIndex,actualCalledThread);
					if (numControllers == MULTIPLE) {
						srEvent objectSequenceEvent = removedEvent.getObjectSequenceEvent();
						srThreadEvent threadSequenceEvent = removedEvent.getThreadSequenceEvent();
						ThreadBasedSequenceCollection.getInstance().changeToAccepted(threadSequenceEvent,
						actualCalledThread,actualCalledVersionNumber);
						objectBasedSequenceCollection.getInstance().changeToAccepted(objectSequenceEvent,
							actualCalledThread,actualCalledVersionNumber);
					}
				}
		
			}
			if (traceEvent.getMType() == REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE ||
					traceEvent.getMType() == REMOVE_UNACCEPTED_RECEIVE_TYPE) {		
				unaccepted.setEventType(UNACCEPTED_RECEIVE);
				unaccepted.setCaller(-2);
				Integer key = new Integer(event.getCalled());
				ArrayList sequence;
				sequence = (ArrayList) unacceptedSR.get(key);
				unacceptedSREvent removedEvent = (unacceptedSREvent) sequence.remove(sequence.indexOf(unaccepted));
				if (removedEvent == null)
					System.out.println("Internal Error: Failure removing unaccepted receive");
				unacceptedSR.put(key,sequence);
			}
	
	}
		
	private void checkForUnacceptedSendReceive() throws IOException {
			Set s = unacceptedSR.entrySet();
			Iterator i = s.iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry)(i.next());
				ArrayList l = (ArrayList)e.getValue();
			if (l.size() > 0) {
				unacceptedSREvent unaccepted = (unacceptedSREvent)l.get(0);
				srEvent event = (srEvent)unaccepted;

				// don't add events for unaccepted_Asynch_send since they are already in the sequence
				if (!(event.getEventType().equals(UNACCEPTED_ASYNCH_SEND))) {

					// write test file events
					srThreadEvent threadEvent;

					if (event.getEventType().equals(UNACCEPTED_SYNCH_SEND)) {
						threadEvent = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
							event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());		
					}
					else { // UNACCEPTED_RECEIVE
						threadEvent = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
							event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());	
					}


					//System.out.println("****Adding event****");
					TestAndReplayCollection.updateSequence(threadEvent);


					// create object- and thread-based trace events; thread-based sequences written after all unaccepted events created
					if (numControllers == MULTIPLE) {

						Integer key1 = new Integer(channelIDGenerator.getInstance().getChannelID(event.getChannelName()));

						objectBasedSequenceCollection.getInstance().updateSequence(event,key1);


						srThreadEvent threadEvent2;
						Integer key2;
						if (event.getEventType().equals(UNACCEPTED_SYNCH_SEND)) {
							threadEvent2 = new srThreadEvent(event.getCaller(),event.getCallerVersionNumber(),event.getCalled(),event.getCalledVersionNumber(),
								event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());	
							key2 = new Integer(event.getCaller());
						}
						else { // UNACCEPTED_RECEIVE
							threadEvent2 = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
								event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),event.getVectorTS());	
							key2 = new Integer(event.getCalled());
						}
						System.out.flush();
						ThreadBasedSequenceCollection.getInstance().updateSequence(threadEvent2,key2);

					}
				} // not an unaccepted_asynch_send
		} // if size>0
	}
	}

	private void addUnacceptedAsynchSend(srEvent event, int index) {
	// event is an unaccepted_asych_send
		unacceptedSREvent unaccepted = new unacceptedSREvent(event);
		unaccepted.setTestAndReplayIndex(index);
		ArrayList sequence;
		Integer key = new Integer(event.getCaller());
		if (unacceptedSR.containsKey(key)) 
			sequence = (ArrayList)unacceptedSR.get(key);
		else
			sequence = new ArrayList();
		sequence.add(unaccepted);
		unacceptedSR.put(key,sequence);

	}


	private void addUnacceptedAsynchSend(srEvent event, int testAndReplayIndex, srEvent objectSequenceEvent,
			srThreadEvent threadSequenceEvent) {
	// event is an unaccepted_asych_send
		unacceptedSREvent unaccepted = new unacceptedSREvent(event);
		unaccepted.setTestAndReplayIndex(testAndReplayIndex);
		unaccepted.setObjectSequenceEvent(objectSequenceEvent);
		unaccepted.setThreadSequenceEvent(threadSequenceEvent);
		ArrayList sequence;
		Integer key = new Integer(event.getCaller());
		if (unacceptedSR.containsKey(key)) 
			sequence = (ArrayList)unacceptedSR.get(key);
		else
			sequence = new ArrayList();
		sequence.add(unaccepted);
		unacceptedSR.put(key,sequence);

	}

	private void outputThreadBasedSequences() throws IOException {
		ThreadBasedSequenceCollection.getInstance().outputThreadBasedSequences();
	}

	private void outputObjectBasedSequences() throws IOException {
		objectBasedSequenceCollection.getInstance().outputObjectBasedSequences();
	}

	private void outputTraceSequences() throws IOException {
		synchronized (classLock) { // ensure sequences are output only one time
			if (!traceSequencesHaveBeenOutput) {
				outputThreadBasedSequences(); // only one controller will be able to do this
				outputObjectBasedSequences(); // only one controller will be able to do this
				traceSequencesHaveBeenOutput = true;
			}
		}
	}



	public void run() {
		int index= 0;
		try {
			if (mode == TRACE) {

				while (true) {
	    			try {
						selectiveWaitC select = new selectiveWaitC();
						// choose spins 100 times so 100 * 40 = delay of 4000 
						selectiveWaitC.delayAlternative timeout = select.new delayAlternative(40);
						select.add(traceMsg,1);		// alternative 1
						select.add(traceSendReceive,2);
						select.add(timeout);
						traceMsg.guard(true);
						traceSendReceive.guard(true);
						timeout.guard(true);
						int choice = 0;
						while (true) {
							choice = select.choose(select.getList());
							//System.out.println("Choice is "+ choice);
							if (choice == 1) {
								outputTrace();
							}
							else if (choice == 2) {
								processSendReceive();
							}
							else { // timeout
								timeout.accept();
								checkForUnacceptedSendReceive();
								TestAndReplayCollection.outputThreadBasedTestAndReplaySequences();
								if (numControllers == MULTIPLE) { // wait for all controllers to create their unaccepted events
									//System.out.println("MULTIPLE");
									Thread.sleep(4000);
									outputTraceSequences();
								}
								if (numControllers == SINGLE) // don't exit program if other controllers exist
									System.exit(0); // break; //  inner while
								else {
									Thread.sleep(5000);
									System.exit(0);
								}
							}
						}
					}
					catch (Exception e) {
        				System.err.println("Error while writing trace file: " + e.toString());
        				//System.exit(1); 
					}
				break; // outer while
				} // while
			} // trace
			else if (mode == REPLAY) { // replay

				Vector alwaysOpenAlternatives = new Vector();
				alwaysOpenAlternatives.add(msgReceived);

				Vector openAlternatives = new Vector(); // contains open alternatives for current event

				selectiveWaitC select = new selectiveWaitC();
				// choose spins 100 times so 100 * 20 = delay of 2000
				selectiveWaitC.delayAlternative timeout = select.new delayAlternative(20);
				int j = 0;
				int group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSendPermit[i],(i+1)+group);		// alternative 1 - (numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceivePermit[i],(i+1)+group);	// alternative (numberOfThreads+1)+1 - 2*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceivePermitX[i],(i+1)+group);	// alternative (numberOfThreads+1)+1 - 3*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSelectPermit[i],(i+1)+group);		// alternative (2*(numberOfThreads+1))+1 - 4*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSendExceptionPermit[i],(i+1)+group);	// alternative (3*(numberOfThreads+1))+1 - 5*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceiveExceptionPermit[i],(i+1)+group);	// alternative (4*(numberOfThreads+1))+1 - 6*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				select.add(msgReceived,1+group);						// alternative (6*(numberOfThreads+1)) + 1
				msgReceived.guard(true); 			
				select.add(timeout);
				timeout.guard(true); 
				int choice = 0;
				simplesrEvent nextEvent = null;
				while(index < simplesrSequence.size()) {
					nextEvent = (simplesrEvent)simplesrSequence.elementAt(index);
					//System.out.println("Next caller is: " + ((simplesrEvent)simplesrSequence.elementAt(index)).getCaller());
					//System.out.println("Next called is: " + ((simplesrEvent)simplesrSequence.elementAt(index)).getCalled());
					openAlternatives.clear();
					openAlternatives.addAll(alwaysOpenAlternatives);
					boolean g = false;
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCaller()
						     && (nextEvent.getEventType().equals(ASYNCH_SEND) || nextEvent.getEventType().equals(SR_SYNCHRONIZATION)));
						requestSendPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestSendPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCalled());
						requestReceivePermit[i].guard(g);
						if (g) openAlternatives.addElement(requestReceivePermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCalled());
						requestReceivePermitX[i].guard(g);
						if (g) openAlternatives.addElement(requestReceivePermitX[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCalled()); 
						requestSelectPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestSelectPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCaller() && nextEvent.getCalled() == -1);
						requestSendExceptionPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestSendExceptionPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCalled()  && nextEvent.getCaller() == -1);
						requestReceiveExceptionPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestReceiveExceptionPermit[i]);						
					}
					choice = select.choose(openAlternatives);
		 			//System.out.println("Choice is alternative " + choice);
					if (choice <= (numberOfThreads+1)) {
						requestSendPermit[choice-1].acceptAndReply();
						if (nextEvent.getCalled() == -2 ) 
							index++;
					}
					else if (choice <= 2*(numberOfThreads+1)) {
						requestReceivePermit[choice-(numberOfThreads+1)-1].acceptAndReply();
						if (nextEvent.getCaller() == -2 ) {
							index++;
						}
 					}
					else if (choice <= 3*(numberOfThreads+1)) {
						requestReceivePermitX[choice-(2*(numberOfThreads+1))-1].accept();
						requestReceivePermitX[choice-(2*(numberOfThreads+1))-1].reply(new Integer(nextEvent.getCaller()));	
						if (nextEvent.getCaller() == -2)
							index++;
						//System.out.println("ReceivePermitX granted for " + (choice-(2*(numberOfThreads+1))-1));	
					}
					else if (choice <= 4*(numberOfThreads+1)) {
						requestSelectPermit[choice-(3*(numberOfThreads+1))-1].accept();
						boolean oneArrival = true;
						int caller = nextEvent.getCaller();
						if (caller != -1) {
							oneArrival = true;
						}
						else
							oneArrival = false;
						requestSelectPermit[choice-(3*(numberOfThreads+1))-1].reply(new Boolean(oneArrival));
 
					}
					else if (choice <= 5*(numberOfThreads+1)) {
						requestSendExceptionPermit[choice-(4*(numberOfThreads+1))-1].acceptAndReply();
					}
					else if (choice <= 6*(numberOfThreads+1)) {
						requestReceiveExceptionPermit[choice-(5*(numberOfThreads+1))-1].acceptAndReply();
					}
					else if (choice == 6*(numberOfThreads+1)+1) {
						msgReceived.acceptAndReply();
   				   ++index;
					}
               else {
						timeout.accept();
						System.out.println("Infeasible Sequence - timeout waiting for event " + (index+1) + ": ("+nextEvent.getCaller()
                    +","+nextEvent.getCalled()+")");
						System.out.flush();
						System.exit(1);
					}
				}
				if (numControllers == SINGLE) { // don't exit program if other controllers exist
					System.out.println("***Sequence Completed***");
					System.out.flush();
					System.exit(0);
            }
				else {
					Thread.sleep(2000);
					System.out.println("***Sequence Completed***");
					System.out.flush();
					System.exit(0);
				}
			}
			else if (mode == TEST) { // test

				Vector alwaysOpenAlternatives = new Vector();
				alwaysOpenAlternatives.add(msgReceived);
				alwaysOpenAlternatives.add(msgReceivedX);

				Vector openAlternatives = new Vector(); // contains open alternatives for current event

				selectiveWaitC select = new selectiveWaitC();
				// choose spins 100 times so 100 * 20 = delay of 2000
				selectiveWaitC.delayAlternative timeout = select.new delayAlternative(20);
				int j = 0;
				int group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSendPermit[i],(i+1)+group);		// alternative 1 - (numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceivePermit[i],(i+1)+group);	// alternative (numberOfThreads+1)+1 - 2*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceivePermitX[i],(i+1)+group);	// alternative (numberOfThreads+1)+1 - 3*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSelectPermit[i],(i+1)+group);		// alternative (2*(numberOfThreads+1))+1 - 4*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestElseDelayPermit[i],(i+1)+group);	// alternative (3*(numberOfThreads+1))+1 - 5*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSendExceptionPermit[i],(i+1)+group);	// alternative (4*(numberOfThreads+1))+1 - 6*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceiveExceptionPermit[i],(i+1)+group);	// alternative (5*(numberOfThreads+1))+1 - 7*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				select.add(msgReceived,1+group);						// alternative (7*(numberOfThreads+1)) + 1
				select.add(msgReceivedX,2+group);					// alternative (7*(numberOfThreads+1)) + 2
				select.add(timeout);						   	// alternative (7*(numberOfThreads+1)) + 3
				msgReceived.guard(true);
				msgReceivedX.guard(true); 
				timeout.guard(true);
				int choice = 0;
				//String channelName = null;
				srThreadEvent nextEvent = null;
				while(index < srSequence.size()) {
					nextEvent = (srThreadEvent)srSequence.elementAt(index);
					//System.out.println("Next caller is: " + ((srThreadEvent)srSequence.elementAt(index)).getOtherThread());
					//System.out.println("Next called is: " + ((srThreadEvent)srSequence.elementAt(index)).getThisThread());
					
					openAlternatives.clear();
					openAlternatives.addAll(alwaysOpenAlternatives);
					
					boolean g = false;
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getOtherThread() && 
							(nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
							nextEvent.getEventType().equals(ASYNCH_SEND) ||
							nextEvent.getEventType().equals(START_ENTRY) ||
							nextEvent.getEventType().equals(FINISH_ENTRY) ||
							nextEvent.getEventType().equals(SEND_EXCEPTION) ||
							nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND) ||
							nextEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND)));
						requestSendPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestSendPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getThisThread() && 
							(nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
							nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
							nextEvent.getEventType().equals(START_ENTRY) ||
							nextEvent.getEventType().equals(FINISH_ENTRY) ||
							nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
							nextEvent.getEventType().equals(UNACCEPTED_RECEIVE)));
						requestReceivePermit[i].guard(g);
						if (g) openAlternatives.addElement(requestReceivePermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getThisThread() && 
							(nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
							nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
							nextEvent.getEventType().equals(START_ENTRY) ||
							nextEvent.getEventType().equals(FINISH_ENTRY) ||
							nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
							nextEvent.getEventType().equals(UNACCEPTED_RECEIVE)));
						requestReceivePermitX[i].guard(g);
						if (g) openAlternatives.addElement(requestReceivePermitX[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getThisThread() && 
							(nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
							nextEvent.getEventType().equals(START_ENTRY) ||
							nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
							nextEvent.getEventType().equals(UNACCEPTED_RECEIVE) ||
							nextEvent.getEventType().equals(ELSE_DELAY)));
						requestSelectPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestSelectPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getThisThread() && 
							nextEvent.getEventType().equals(ELSE_DELAY));
						requestElseDelayPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestElseDelayPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getOtherThread() && 
							nextEvent.getEventType().equals(SEND_EXCEPTION));
						requestSendExceptionPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestSendExceptionPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getThisThread() && 
							nextEvent.getEventType().equals(RECEIVE_EXCEPTION));
						requestReceiveExceptionPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestReceiveExceptionPermit[i]);
					}
					choice = select.choose(openAlternatives);
					requestPermitMessage msg = null;
					if (choice <= (numberOfThreads+1)) {
						msg = (requestPermitMessage)requestSendPermit[choice-1].accept();
						if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
							System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on send()");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						if ((msg.getVersionNumber() != (nextEvent.getOtherThreadVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on send()");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						requestSendPermit[choice-1].reply();
						if (nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND))
							index++;
					}
					else if (choice <= 2*(numberOfThreads+1)) {
						msg = (requestPermitMessage)requestReceivePermit[choice-(numberOfThreads+1)-1].accept();
						if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
							System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						if ((msg.getVersionNumber() != (nextEvent.getThisThreadVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						requestReceivePermit[choice-(numberOfThreads+1)-1].reply();
						if (nextEvent.getEventType().equals(UNACCEPTED_RECEIVE))
							index++;
 					}
					else if (choice <= 3*(numberOfThreads+1)) {
						msg = (requestPermitMessage)requestReceivePermitX[choice-(2*(numberOfThreads+1))-1].accept();
						if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
							System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						if ((msg.getVersionNumber() != (nextEvent.getThisThreadVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}						
						requestReceivePermitX[choice-(2*(numberOfThreads+1))-1].reply(new Integer(nextEvent.getOtherThread()));
						if (nextEvent.getEventType().equals(UNACCEPTED_RECEIVE))
							index++;
						//System.out.println("ReceivePermitX granted for " + (choice-(2*(numberOfThreads+1))-1));
					}
					else if (choice <= 4*(numberOfThreads+1)) {
						requestSelectPermit[choice-(3*(numberOfThreads+1))-1].accept();
						boolean oneArrival = true;
						int caller = nextEvent.getOtherThread();
						if (caller != -1) {
							oneArrival = true;
						}
						else
							oneArrival = false;
						requestSelectPermit[choice-(3*(numberOfThreads+1))-1].reply(new Boolean(oneArrival));
 
					}
					else if (choice <= 5*(numberOfThreads+1)) {
						msg = (requestPermitMessage)requestElseDelayPermit[choice-(4*(numberOfThreads+1))-1].accept();
						if ((msg.getVersionNumber() != (nextEvent.getThisThreadVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						requestElseDelayPermit[choice-(4*(numberOfThreads+1))-1].reply();
					}
					else if (choice <= 6*(numberOfThreads+1)) {
						msg = (requestPermitMessage)requestSendExceptionPermit[choice-(5*(numberOfThreads+1))-1].accept();
						if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
							System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on send exception");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						if ((msg.getVersionNumber() != (nextEvent.getOtherThreadVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on send exception");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						requestSendExceptionPermit[choice-(5*(numberOfThreads+1))-1].reply();
					}
					else if (choice <= 7*(numberOfThreads+1)) {
						msg = (requestPermitMessage)requestReceiveExceptionPermit[choice-(6*(numberOfThreads+1))-1].accept();
						if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
							System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive exception");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						if ((msg.getVersionNumber() != (nextEvent.getThisThreadVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive exception");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						requestReceiveExceptionPermit[choice-(6*(numberOfThreads+1))-1].reply();
					}
					else if (choice == (7*(numberOfThreads+1))+1){
						msgReceived.acceptAndReply();
   				   ++index;
					}
					else if (choice == (7*(numberOfThreads+1))+2){
						msgReceivedMessage Rmsg = (msgReceivedMessage) msgReceivedX.accept();
						if ((Rmsg.getCaller() != nextEvent.getOtherThread())) {
							System.out.println("Infeasible Sequence, unexpected caller " + Rmsg.getCaller() + " on receive");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						if ((Rmsg.getCallerVersionNumber() != (nextEvent.getOtherThreadVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected caller version number " + Rmsg.getCallerVersionNumber() + " on receive");
							System.out.println("Expected event was: ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}							
						
   				   ++index;
   				   msgReceivedX.reply();
					}
					else { 
						timeout.accept();
						System.out.println("Infeasible Sequence - timeout waiting for event " + (index+1) + ": ("+
							nextEvent.getOtherThread()+","+nextEvent.getThisThread()+
							","+nextEvent.getOtherThreadVersionNumber()+","+nextEvent.getThisThreadVersionNumber()+
							","+nextEvent.getChannelName()+")");
						System.out.flush();
						System.exit(1);
					}
				}
				if (numControllers == SINGLE) { // don't exit program if other controllers exist
					System.out.println("***Sequence Completed***");
					System.out.flush();
					System.exit(0);
            }
				else {
					Thread.sleep(4000);
					System.out.println("***Sequence Completed***");
					System.out.flush();
					System.exit(0);
				}
			}
		} catch (InterruptedException r) {}
	}

}
