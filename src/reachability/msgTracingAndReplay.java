package reachability;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
public final class msgTracingAndReplay extends Controller 
	implements eventTypeParameters, traceMessageTypeParameters,
	propertyParameters, monitorEventTypeParameters, Control {
// Provides Trace and Replay for Channels. srSequence is a simple 
// send-receive sequence. Trace or Replay is selected by the user. 
// Single or Multiple is selected by user.


   private Vector simplesrSequence = new Vector();
	private ArrayList srSequence = new ArrayList();
	private int numberOfThreads; // number of threads in ThreadID file: 1..numberOfThreads
	private static final Object threadBasedMsgTracingAndReplayClassLock = threadBasedMsgTracingAndReplay.class;
	private static final Object classLock = msgTracingAndReplay.class;

	private static msgTracingAndReplay instance = null;
	private static boolean traceSequencesHaveBeenOutput = false;

	private propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	private propertyParameters.Controllers numControllers = SINGLE;
	//private propertyParameters.Strategy strategy = OBJECT;
	private propertyParameters.TraceVariants traceVariants = TRACEOFF;  
	protected propertyParameters.CheckTrace checkTrace = CHECKTRACEOFF;
	//private String traceFileName = null;
	
	Checker checker;
	
	private boolean RTTracing = false; // true if in RT mode and replay is over (so now tracing)
	private boolean RTAllRecollected = false; // true if in RT mode and replay is over (so now tracing)
	                                          // and all recollected events have been recollected with new sends
   private boolean ignoreSequence = false; // true if recollected receive could only receive
                                           // old sends but no new sends.

  	static long transitionCount=0;
  	static long eventCount=0;
  	
  	private static boolean usingRTDriver = false;
	
	private static int variantNumber = 1;
 	private static final String TRACEVARIANT_DIR = "variantFiles";
   private static final String FNAME_PREFIX = "variantX";
   private static final String FNAME_SUFFIX = ".txt";
   private File subdir;
   private static boolean firstExecution = true;

	private selectableEntryC requestSendPermit[]; 
	private selectableEntryC requestSendPermitX[];
	private selectableEntryC requestSendPermitMS[];
	private selectableEntryC requestSelectPermit[]; 
	private selectableEntryC requestReceivePermit[];
	private selectableEntryC requestReceivePermitX[];
	private selectableEntryC requestReceivePermitMS[];
	private selectableEntryC requestElseDelayPermit[]; 
	private selectableEntryC requestSendExceptionPermit[]; 
	private selectableEntryC requestReceiveExceptionPermit[];
	private selectableEntryC requestSynchSendPermit[];
	private selectableEntryC msgReceived = new selectableEntryC();
	private selectableEntryC msgReceivedX = new selectableEntryC();
	private selectableEntryC traceMsg = new selectableEntryC("traceMsg");
	private selectableEntryC traceSendReceive = new selectableEntryC();
	private selectableEntryC appCompleted = new selectableEntryC();
	private selectableEntryC sendArrivedRT = new selectableEntryC();
	private selectableEntryC monitorEnteredRT = new selectableEntryC();

	private HashMap unacceptedSR = new HashMap();
	//private HashMap threadBasedSequences = new HashMap();

	private objectBasedTestAndReplayCollection TestAndReplayCollection;
	VariantGenerator generator = null;

	//private DataOutputStream outputTrace = null; // output sequence of int IDs
	//private DataInputStream inputTrace = null; // input sequence of int IDs
	private ObjectOutputStream outputReplay = null; // output sequence of int IDs
	private ObjectInputStream inputReplay = null; // input sequence of int IDs
	private PrintWriter outputReplayText = null;
	private BufferedReader inputReplayText = null;
	private ObjectOutputStream outputTest = null; // output sequence of int IDs
	private ObjectInputStream inputTest = null; // input sequence of int IDs
	private PrintWriter outputTestText = null;
	private BufferedReader inputTestText = null;
	private ObjectOutputStream outputRT = null; // added for RT
	private PrintWriter outputRTText = null;  // added for RT
	//private BufferedReader inputRTText = null; // added for RT


	public msgTracingAndReplay(propertyParameters.Mode mode, 
			propertyParameters.Controllers numControllers,
			propertyParameters.Strategy strategy, String traceFileName) { 
		super("Controller");
		this.mode = mode;
		this.numControllers = numControllers;
		//this.strategy = strategy;
		//this.traceFileName = traceFileName;
		checkTrace = (propertyReader.getInstance().getCheckTraceProperty());
    if (mode == TRACE) { // trace
		//if ((numControllers == MULTIPLE) || (strategy == OBJECT)) {
      	try {
        		//outputReplay = new ObjectOutputStream(new FileOutputStream(traceFileName+"-replay.dat"));
	     		outputReplayText = new PrintWriter(new FileOutputStream(traceFileName+"-replay.txt"));
        		//outputTest = new ObjectOutputStream(new FileOutputStream(traceFileName+"-test.dat"));
	     		outputTestText = new PrintWriter(new FileOutputStream(traceFileName+"-test.txt"));
        		//outputRT = new ObjectOutputStream(new FileOutputStream(traceFileName+"-rt.dat"));
	     		//outputRTText = new PrintWriter(new FileOutputStream(traceFileName+"-rt.txt"));
				TestAndReplayCollection	= new objectBasedTestAndReplayCollection(outputTest,outputTestText,
							outputReplay,outputReplayText,outputRT,outputRTText,numControllers);
			} 
			catch (IOException e) {
        			System.err.println("File not opened: " + e.toString());
        			System.exit(1);
      	}
			catch (Exception e) {
       		System.err.println("Error: " + e.toString());
        		System.exit(1);
      	}
      //}
      if (checkTrace==CHECKTRACEON) {
      	checker = new MyChecker();
      }
    }
	 else if (mode == REPLAY) { // replay
		System.out.println("Reading Simple SR-sequence.");
      try {
        //inputTrace = new DataInputStream(new FileInputStream(traceFileName+"-replay.dat"));
		  //inputReplay = new ObjectInputStream(new FileInputStream(traceFileName+"-replay.dat"));
        inputReplayText = new BufferedReader(new FileReader(traceFileName+"-replay.txt"));
      }
      catch (IOException e) {
        System.err.println("Trace file not opened: " + e.toString());
        System.exit(1);
      }
   	try {   
			int lineNo = 1;
			String line = null;
      	while ((line = inputReplayText.readLine()) != null) {
		     StringTokenizer t = new StringTokenizer(line," (,)");
		     if (t.countTokens() != 3) {
		     		System.out.println("Format Error in trace file line "+lineNo+":\n"+line
	      			+ ": Expecting an event with 3 fields, read an event with " 
	      			+ t.countTokens() + " fields");
	      		System.exit(1);
	   		}
		      String caller_S = t.nextToken();
		      String called_S = t.nextToken();
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
		      int caller=-1;
		      try {
			      	caller = Integer.parseInt(caller_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + caller_S); 
				      System.exit(1);
			   }
		      int called=-1;
		      try {
			      	called = Integer.parseInt(called_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + called_S); 
				      System.exit(1);
			   }
	      	simplesrEvent event = new simplesrEvent(caller,called,eType);
				simplesrSequence.addElement(event);      	
				lineNo++;
			}      	
		}
			
	//      try {
	//			while (true) {
	//				event = (simplesrEvent)inputReplay.readObject();
	//          	simplesrSequence.addElement(event); 
	//         }
	//      }
	//      catch (ClassNotFoundException e) { }	
	
      catch (EOFException eof) {
			try {
		  		inputReplayText.close();
		  		//inputReplay.close();
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
	 else if (mode == TEST) { // test
 		System.out.println("Reading SR-sequence.");
      try {
        //inputTrace = new DataInputStream(new FileInputStream(traceFileName+"-test.dat"));
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
		      String caller_S = t.nextToken();
		      String called_S = t.nextToken();
		      String callerVersionNumber_S = t.nextToken();
		      String calledVersionNumber_S = t.nextToken();
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
		      int caller=-1;
		      try {
			      	caller = Integer.parseInt(caller_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + caller_S); 
				      System.exit(1);
			   }
		      int called=-1;
		      try {
			      	called = Integer.parseInt(called_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + called_S); 
				      System.exit(1);
			   }
		      int callerVersionNumber=-1;
		      try {
			      	callerVersionNumber = Integer.parseInt(callerVersionNumber_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + callerVersionNumber_S); 
				      System.exit(1);
			   }
		      int calledVersionNumber=-1;
		      try {
			      	calledVersionNumber = Integer.parseInt(calledVersionNumber_S);
			   }
			   catch(NumberFormatException e) {
				      System.out.println("NumberFormatException while reading trace file at line "+lineNo+":\n"+line
				      + ": integer ID expected, actual value was: " + callerVersionNumber_S); 
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
			   
	      	srEvent event = new srEvent(caller,called,callerVersionNumber,calledVersionNumber,channelName_S,channelVersionNumber,eType,null);
				srSequence.add(event);      	
				lineNo++;
			}      	
		}
      //srEvent event;
      //try {
		//	while (true) {
		//		event = (srEvent)inputTest.readObject();
      //   	srSequence.add(event); 
      //   }
      //}
      //catch (ClassCastException e) {
      // System.out.println("Error while reading SR-sequence file"+ e.toString());
      // System.exit(1);
      //}
      //catch (ClassNotFoundException e) {
      // System.out.println("Error while reading SR-sequence file"+ e.toString());
      // System.exit(1);
      //}	
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
    else if (mode == RT) {
      	try {
        		//outputReplay = new ObjectOutputStream(new FileOutputStream(traceFileName+"-replay.dat"));
	     		//outputReplayText = new PrintWriter(new FileOutputStream(traceFileName+"-replay.txt"));
        		//outputTest = new ObjectOutputStream(new FileOutputStream(traceFileName+"-test.dat"));
	     		//outputTestText = new PrintWriter(new FileOutputStream(traceFileName+"-test.txt"));
        		//outputRT = new ObjectOutputStream(new FileOutputStream(traceFileName+"-rt.dat"));
	     		//outputRTText = new PrintWriter(new FileOutputStream(traceFileName+"-rt.txt"));
	     		generator = VariantGenerator.getInstance();
				TestAndReplayCollection	= new objectBasedTestAndReplayCollection(outputTest,outputTestText,
							outputReplay,outputReplayText,outputRT,outputRTText,numControllers);
							
				traceVariants = (propertyReader.getInstance().getTraceVariantsProperty());
				if (traceVariants == TRACEON && firstExecution) {
					initTraceTheVariants();
					firstExecution = false;
				}
		      if (checkTrace==CHECKTRACEON) {
      			checker = new MyChecker();
		      }
			} 
			//catch (IOException e) {
        	//		System.out.println("File not opened: " + e.toString());
        	//		System.exit(1);
      	//}
			catch (Exception e) {
       		System.out.println("Error: " + e.toString());
        		System.exit(1);
      	}
    }
	 if (mode == REPLAY || mode == TEST || mode == RT) {
	 	numberOfThreads = (propertyReader.getInstance().getMaxThreadsProperty());
		//System.out.println("numberOFThreads: "+numberOfThreads);
		requestSendPermit = new selectableEntryC[numberOfThreads+1];
		requestSendPermitX = new selectableEntryC[numberOfThreads+1];
		requestSendPermitMS = new selectableEntryC[numberOfThreads+1];
		requestSelectPermit = new selectableEntryC[numberOfThreads+1];
		requestReceivePermit = new selectableEntryC[numberOfThreads+1];
		requestReceivePermitX = new selectableEntryC[numberOfThreads+1];
		requestReceivePermitMS = new selectableEntryC[numberOfThreads+1];
		requestElseDelayPermit = new selectableEntryC[numberOfThreads+1];
		requestSendExceptionPermit = new selectableEntryC[numberOfThreads+1];
		requestReceiveExceptionPermit = new selectableEntryC[numberOfThreads+1];
		requestSynchSendPermit = new selectableEntryC[numberOfThreads+1];
		for (int i=0; i<(numberOfThreads+1);i++) {
			// ensure all guards are false
			requestSendPermit[i] = new selectableEntryC();	requestSendPermit[i].guard(false);
			requestSendPermitX[i] = new selectableEntryC(); requestSendPermitX[i].guard(false);
			requestSendPermitMS[i] = new selectableEntryC(); requestSendPermitMS[i].guard(false);
			requestSelectPermit[i] = new selectableEntryC(); requestSelectPermit[i].guard(false);
			requestReceivePermit[i] = new selectableEntryC(); requestReceivePermit[i].guard(false);
			requestReceivePermitX[i] = new selectableEntryC(); requestReceivePermitX[i].guard(false);
			requestReceivePermitMS[i] = new selectableEntryC(); requestReceivePermitMS[i].guard(false);
			requestElseDelayPermit[i] = new selectableEntryC(); requestElseDelayPermit[i].guard(false);
			requestSendExceptionPermit[i] = new selectableEntryC(); requestSendExceptionPermit[i].guard(false);
			requestReceiveExceptionPermit[i] = new selectableEntryC(); requestReceiveExceptionPermit[i].guard(false);
			requestSynchSendPermit[i] = new selectableEntryC(); requestSynchSendPermit[i].guard(false);
		}
	}

}

	public static msgTracingAndReplay getInstance(propertyParameters.Mode mode, 
			propertyParameters.Controllers numControllers,
			propertyParameters.Strategy strategy, String traceFileName) { 
		synchronized(classLock) {
      	if (instance == null) {
        		instance = new msgTracingAndReplay(mode,numControllers,strategy,traceFileName);
        		instance.start();
        	}
		}
      return instance;
   }
   
  	public static msgTracingAndReplay getController() {
  		propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	   propertyParameters.Controllers numControllers = SINGLE;
	   propertyParameters.Strategy strategy = OBJECT;
  		mode = (propertyReader.getInstance().getModeProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		strategy = (propertyReader.getInstance().getStrategyProperty());
		return msgTracingAndReplay.getInstance(mode,numControllers,strategy,"channel");
  	}
  	
  public static void setUsingRTDriver() {usingRTDriver = true;}
   
   public static void resetController() {
   	instance = null;
   }

	public void initTraceTheVariants() {
   // create the subdir for the variant text files
		subdir = new File (TRACEVARIANT_DIR);
		if (subdir.exists()) {
   		if (subdir.isDirectory()) {
				File[] files = subdir.listFiles();
	    		for (int i = 0; i < files.length; i ++) {
					files[i].delete();
		    	}
			}
	    	else {
				subdir.delete ();
				subdir.mkdir ();
		   }
		}
		else {
	   	 subdir.mkdir ();
		}	
	}
	
	public void traceTheVariant(ArrayList srSequence) {
		try {
			PrintWriter outputVariant = null;
	  		outputVariant = new PrintWriter(new FileOutputStream(TRACEVARIANT_DIR+"\\"+FNAME_PREFIX+variantNumber+FNAME_SUFFIX));
	  		Iterator p = srSequence.iterator();
	  		while (p.hasNext()) {
	  			srEvent e = (srEvent) p.next();

				//StringBuffer stringVectorTS = new StringBuffer("[");
				//int clockValue;
				//for (int i = 1; i<=numberOfThreads; i++) {
				//for (int i = 0; i < e.getVectorTS().getSize (); i ++) {
				//	clockValue = e.getVectorTS().getIntegerTS(i);
				//	if (clockValue == -1)
				//		stringVectorTS.append("0");
				//	else
				//		stringVectorTS.append(clockValue);
				//	stringVectorTS.append(",");
				//}
				//stringVectorTS.setCharAt(stringVectorTS.length()-1,']');
									
  				outputVariant.println(e.getLabel()+"_"+e.getCaller()+"_"+e.getCalled()
	    				+ "_-1_-1_(-1,-1)"+e.getVectorTS()); // stringVectorTS);
	    	}
	     	outputVariant.close();
	     	variantNumber++;
	     	//if (variantNumber > 5)
	      //  System.exit(0);
     	} 	
     	catch (IOException e) {
	     		System.err.println("Error while writing trace file: " + e.toString());
   	  		System.exit(1); 
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
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber);
				requestSendPermit[ID].call(m);
			}
			//else System.out.println("shortcut requestSendPermit from "+ID);
		} catch(InterruptedException e) {}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
		  catch (Exception e) {e.printStackTrace();};
	}
	
	public boolean requestSendPermitX(int ID, String channelName, int callerVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber);
				Boolean isOld = (Boolean) requestSendPermitX[ID].call(m);
				return isOld.booleanValue(); // true if this is an old send during RT
			}
			else { 
 			   //System.out.println("shortcut requestSendPermitX from "+ID);
				return false;
 			}
		} catch(InterruptedException e) {e.printStackTrace();}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
		  catch (Exception e) {e.printStackTrace();}
	   return false;
	}

	public boolean requestSendPermitMS(int ID, String channelName, int callerVersionNumber, int calledID) {
	// calledID is ID of monitor
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber,calledID);
				Boolean isOld = (Boolean) requestSendPermitMS[ID].call(m);
				return isOld.booleanValue(); // true if this is an old send during RT
			}
			else { 
 			   //System.out.println("shortcut requestSendPermitMS from "+ID);
				return false;
 			}
		} catch(InterruptedException e) {e.printStackTrace();}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
		  catch (Exception e) {e.printStackTrace();}
	   return false;
	}

	public void requestReceivePermit(int ID, String channelName, int calledVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't botrher requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,calledVersionNumber);
				requestReceivePermit[ID].call(m);
			}
			//else System.out.println("shortcut requestReceivePermit from "+ID);
		} catch(InterruptedException e) {}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
	}
	
	public int requestReceivePermitX(int ID, String channelName, int calledVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't botrher requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,calledVersionNumber);;
				Integer I = (Integer) requestReceivePermitX[ID].call(m);
				return I.intValue();
			}
			else {
				//System.out.println("shortcut requestReceivePermitX from "+ID);
				return -1;
			}
		} catch(InterruptedException e) {System.exit(1);}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
		return -1;
	}
	
	public int requestReceivePermitMS(int ID, int calledID, String channelName, int calledVersionNumber) {
	// calledID is monitor ID
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't botrher requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,calledVersionNumber,calledID);
				Integer I = (Integer) requestReceivePermitMS[ID].call(m);
				return I.intValue();
			}
			else {
				//System.out.println("shortcut requestReceivePermitMS from "+ID);
				return -1;
			}
		} catch(InterruptedException e) {System.exit(1);}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
		return -1;
	}

	public boolean requestSelectPermit(int ID) {
		boolean oneArrival = false;
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother requesting if now in RT trace mode
				oneArrival = ((Boolean)requestSelectPermit[ID].call()).booleanValue();
				return oneArrival;
			}
			else {
				//System.out.println("shortcut requestSelectPermit from "+ID);
			   return false;
			}
		} catch(InterruptedException e) {
			  return oneArrival;
		  }
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  	return false;
		  }
	}

	public void requestElseDelayPermit(int ID, int calledVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't botrher requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage("",calledVersionNumber);
				requestElseDelayPermit[ID].call(m);
			}
		} catch(InterruptedException e) {}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
	}

	public void requestSendExceptionPermit(int ID, String channelName, 
			int callerVersionNumber, int calledVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't botrher requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber);
				requestSendExceptionPermit[ID].call(m);
			}
		} catch(InterruptedException e) {}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
	}

	public void requestReceiveExceptionPermit(int ID, String channelName, int calledVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,calledVersionNumber);
				requestReceiveExceptionPermit[ID].call(m);
			}
		} catch(InterruptedException e) {}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
	}
	public void requestSynchSendPermit(int ID, String channelName, int callerVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber);
				requestSynchSendPermit[ID].call(m);
			}
			//else System.out.println("shortcut requestSynchSendPermit from "+ID);
		} catch(InterruptedException e) {}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
		  catch (Exception e) {System.out.println("ID is " + ID);e.printStackTrace();};
	}


	public void msgReceived() {
		try {
		   //if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother releasing if now in RT trace mode
	     		msgReceived.call();
	     	//}
			//else System.out.println("shortcut msgReceived");
		} catch(InterruptedException e) {}
	}

	public void msgReceived(int caller, int callerVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother releasing if now in RT trace mode
		      msgReceivedMessage m = new msgReceivedMessage(caller, callerVersionNumber);
	     		msgReceivedX.call(m);
	     	}
	     	else transitionCount++;
			//else System.out.println("shortcut msgReceivedX");
		} catch(InterruptedException e) {}
	}
	
	public void sendArrivedRT(int ID, String channelName, int callerVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother requesting if now in RT trace mode
		   	// Here ID is the caller, not the called!
				requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber,ID);
				sendArrivedRT.call(m);
			}
			//else System.out.println("shortcut sendArrivedRT from "+ID);

		} catch(InterruptedException e) {e.printStackTrace();}
		  catch (Exception e) {System.out.println("ID is " + ID);e.printStackTrace();}
	}
	public void monitorEnteredRT(int ID, String channelName, int callerVersionNumber) {
		try {
		   if (!(mode == RT && RTTracing && RTAllRecollected))  { // don't bother requesting if now in RT trace mode
		   	// Here ID is the caller, not the called!
				requestPermitMessage  m = new requestPermitMessage(channelName,callerVersionNumber,ID);
				monitorEnteredRT.call(m);
			}
			//else System.out.println("shortcut monitorEnteredRT from "+ID);

		} catch(InterruptedException e) {e.printStackTrace();}
		  catch (Exception e) {e.printStackTrace();}
	}
	public boolean appCompleted() {
		try {
			Boolean ignored = (Boolean) appCompleted.call();
			return ignored.booleanValue();
		} catch(InterruptedException e) {e.printStackTrace();System.exit(1);}
		return false;
	}
	public void trace(monitorEvent m) {}
	public void trace(srEvent e) {
		try {
			//srEvent e = new srEvent(m.getCallerID(),m.getThreadID(),-1,
			//				-1,m.getMethodName(),-1,
			//				((m.getEventType()).equals(ENTRY)||(m.getEventType()).equals(SYNCHRECEIVE))  ? ASYNCH_RECEIVE : ASYNCH_SEND, 
			//				m.getVectorTS(),"",
			//				((m.getEventType()).equals(ENTRY))  ? true : false);
			if(e.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
				//System.out.println("trace unaccepted send: traceMsg.call");
			   traceMessage msg = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,e);
				traceMsg.call(msg);
			}
			else if(e.getEventType().equals(UNACCEPTED_RECEIVE)) {
				//System.out.println("trace unaccepted receive: traceSendReceive.call");
				traceMessage msg = new traceMessage(ADD_UNACCEPTED_RECEIVE_TYPE,e);
				traceSendReceive(msg); 			
			}
			else {
				//System.out.println("trace receive: traceMsg.call");
				traceMessage msg = new traceMessage(ASYNCH_RECEIVE_PORT_EVENT_TYPE,e);
				traceMsg.call(msg);
				//System.out.println("remove send/receive: start traceSendReceive.call");
				traceMessage msg2 = new traceMessage(REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE,e);
				traceSendReceive.call(msg2);
				//System.out.println("remove send/receive: did traceSendReceive.call");
			}
			
				
				
		} catch(InterruptedException ex) {}
	}


	public void requestEntryPermit(int ID) {
		try {
		   if (!(mode == RT && RTTracing))  { // don't bother requesting if now in RT trace mode
				requestPermitMessage  m = new requestPermitMessage("",-1);
				requestReceivePermit[ID].call(m);
			}
		} catch(InterruptedException e) {}
		  catch(ArrayIndexOutOfBoundsException e) {
		  	System.out.println("Error: Too many threads/synchronization objects. The default maximum is 20.  ");
		  	System.out.println("       Use -DmaxThreads=n to raise the limit.");
		  	System.exit(1);
		  }
		  catch (Exception e) {System.out.println("ID is " + ID);e.printStackTrace();};
	}

	public void releaseEntryPermit() {
		try {
		   if (!(mode == RT && RTTracing))  { // don't bother releasing if now in RT trace mode
	     		msgReceived.call();
	     	}
		} catch(InterruptedException e) {}
	}

	public void requestMPermit(monitorEventTypeParameters.eventType op, int ID, String methodName, 
			String conditionName) {}

	public void releaseMPermit() {}
	
	public void requestMPermitSpecTest(int ID) {}
	
	public void requestAndReleaseCommPermit(int ID, String eventName) {}
	
	public void exerciseEvent(int ID, String eventName) {}
	
	public void traceCommEvent(monitorSpecEvent m) {}
	
	public void requestPermit(int ID) {}
	
	public void releasePermit() {}
	
	public void trace(int ID) {}


	private void outputTrace() throws IOException {
		//if (traceEvent.getMType() == SR_LINK_EVENT_TYPE ||
		//	 traceEvent.getMType() == SR_PORT_EVENT_TYPE ||
		//	 traceEvent.getMType() == SR_MAILBOX_EVENT_TYPE ||
		//	 traceEvent.getMType() == SR_ENTRY_EVENT_TYPE ||
		//	 traceEvent.getMType() == ELSE_DELAY_TYPE ||
      //  traceEvent.getMType() == SEND_EXCEPTION_TYPE ||
      //	 traceEvent.getMType() == RECEIVE_EXCEPTION_TYPE ||
		//	 traceEvent.getMType() == END_ENTRY_TYPE) {
			try {
				//System.out.println("outputTrace: calling traceMsg.accept():"+traceMsg.count());
				traceMessage traceEvent = (traceMessage)traceMsg.accept();
				//System.out.println("outputTrace: called traceMsg.accept()");

						// output object-based

						srEvent event = (srEvent)traceEvent.getMsg();

						//System.out.println("****Adding event****: " + event);
						int testAndReplayIndex = TestAndReplayCollection.updateSequence(event);

						srEvent objectSequenceEvent = null;
						srThreadEvent threadSequenceEvent = null;

						srThreadEvent threadEvent;
						Integer key;
						if (numControllers == MULTIPLE) {
							// add to object-based trace
							if (!(event.getEventType().equals(ELSE_DELAY))) { // no channel associated with ELSE_DELAY events
								Integer key2 = new Integer(channelIDGenerator.getInstance().getChannelID(event.getChannelName()));
								objectSequenceEvent = event;
								objectBasedSequenceCollection.getInstance().updateSequence(event,key2);
							}

							// add to thread-based trace

							if (traceEvent.getMType() == SR_LINK_EVENT_TYPE ||
								 traceEvent.getMType() == SR_PORT_EVENT_TYPE ||
								 traceEvent.getMType() == SR_MAILBOX_EVENT_TYPE ||
								 traceEvent.getMType() == SR_ENTRY_EVENT_TYPE ||
								 traceEvent.getMType() == END_ENTRY_TYPE ||
						 		 traceEvent.getMType() == ASYNCH_RECEIVE_PORT_EVENT_TYPE ||
						 		 traceEvent.getMType() == ASYNCH_RECEIVE_LINK_EVENT_TYPE ||
						 		 traceEvent.getMType() == ASYNCH_RECEIVE_MAILBOX_EVENT_TYPE) {
								threadEvent = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
									event.getChannelName(),event.getChannelVersionNumber(),THREAD_BASED_RECEIVE,
									event.getVectorTS());	
								key = new Integer(event.getCalled());
							}
							else if (traceEvent.getMType() == ELSE_DELAY_TYPE ||
      						 traceEvent.getMType() == RECEIVE_EXCEPTION_TYPE) {
								threadEvent = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
									event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),	
									event.getVectorTS());	
								key = new Integer(event.getCalled());
							}
      					else { //  traceEvent.getMType() == SEND_EXCEPTION_TYPE || ASYNCH_SEND/link/mailbox_PORT_EVENT_TYPE
								threadEvent = new srThreadEvent(event.getCaller(),event.getCallerVersionNumber(),event.getCalled(),event.getCalledVersionNumber(),
									event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),
									event.getVectorTS());	
								key = new Integer(event.getCaller());
							}

							threadSequenceEvent = threadEvent;
							ThreadBasedSequenceCollection.getInstance().updateSequence(threadEvent,key);

							// create thread-based event for the caller for synchronous synchronizations
							srThreadEvent threadEvent2;
							Integer key2;
							//LinkedList sequence2;
							if (traceEvent.getMType() == SR_LINK_EVENT_TYPE ||
								 traceEvent.getMType() == SR_PORT_EVENT_TYPE ||
								 traceEvent.getMType() == SR_MAILBOX_EVENT_TYPE ||
								 traceEvent.getMType() == SR_ENTRY_EVENT_TYPE ||
								 traceEvent.getMType() == END_ENTRY_TYPE) {
								threadEvent2 = new srThreadEvent(event.getCalled(),event.getCallerVersionNumber(),event.getCalled(),event.getCalledVersionNumber(),
									event.getChannelName(),event.getChannelVersionNumber(),THREAD_BASED_SEND,
									event.getVectorTS());	
								key2 = new Integer(event.getCaller());

								ThreadBasedSequenceCollection.getInstance().updateSequence(threadEvent2,key2);
							}


						}

						//System.out.println("**Adding unaccepted asynch send event**");
						if (event.getEventType().equals(UNACCEPTED_ASYNCH_SEND))
							if (numControllers == MULTIPLE) {
								addUnacceptedAsynchSend(event,testAndReplayIndex,objectSequenceEvent,threadSequenceEvent);
							}
							else {
								addUnacceptedAsynchSend(event,testAndReplayIndex);
							}

				traceMsg.reply();			// make sure trace gets written
			}
			catch (InterruptedException r) {}
		//}
	}
	
	private void processSendReceive() {
		try {
			//System.out.println("Starting traceSendReceive.acceptAndReply()");
			traceMessage traceEvent = (traceMessage)traceSendReceive.acceptAndReply();
			//System.out.println("Did traceSendReceive.acceptAndReply()");
			if (traceEvent.getMType() == ADD_UNACCEPTED_SEND_TYPE ||
				traceEvent.getMType() == ADD_UNACCEPTED_RECEIVE_TYPE) 
					addSendReceive(traceEvent);
			else
					removeSendReceive(traceEvent);
		}	catch (InterruptedException r) {}
		   catch (Exception e) {
				System.out.println("Reachability testing failed. The failure is usually caused   ");
				System.out.println("   by a program that accesses a shared variable outside of   ");
				System.out.println("   a critical section. To verify that this is the cause, use ");
				System.out.println("   the Shared Variable classes in the library. These classes,");
				System.out.println("   as described in the User Manual, allow reachability       ");
				System.out.println("   testing to detect data races.                             ");
				
				System.exit(1);		   
		   }
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
				//System.out.println(key.intValue()+" UNACCEPTED_SYNCH_SEND before: "+sequence);
				//System.out.println("Putting new event for key " + key.intValue());
				sequence.add(unaccepted);
				unacceptedSR.put(key,sequence);
				//System.out.println(key.intValue()+" UNACCEPTED_SYNCH_SEND after: " + (ArrayList)unacceptedSR.get(key));

			}
         else if (event.getEventType() == UNACCEPTED_RECEIVE) {
				ArrayList sequence;
				Integer key = new Integer(event.getCalled());
				if (unacceptedSR.containsKey(key)) 
					sequence = (ArrayList)unacceptedSR.get(key);
				else
					sequence = new ArrayList();
				//System.out.println(key.intValue()+" UNACCEPTED_RECEIVE before: "+sequence);
				sequence.add(unaccepted);
				//System.out.println("Putting new event for key " + key.intValue());
				unacceptedSR.put(key,sequence);
				//System.out.println(key.intValue()+" UNACCEPTED_RECEIVE after: " + (ArrayList)unacceptedSR.get(key));

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
				int actualCalledVersionNumber = 0; // version number of receiving thread
				if (event.getEventType().equals(ASYNCH_RECEIVE)) {
					actualCalledThread = event.getCalled();
					actualCalledVersionNumber = event.getCalledVersionNumber();
					unaccepted.setEventType(UNACCEPTED_ASYNCH_SEND);
					unaccepted.setCaller(event.getCaller());
					//System.out.println("called:" +event.getCalled()+","+
					//	"calledversionNumber:"+event.getCalledVersionNumber()+","+
					//	"caller:"+event.getCaller());
				}
				else
					unaccepted.setEventType(UNACCEPTED_SYNCH_SEND);
				Integer key = new Integer(event.getCaller());
				ArrayList sequence;
				//System.out.println("Removing unaccepted send for "+ event.getCaller() + " " + event.getCalled());
				sequence = (ArrayList) unacceptedSR.get(key);
				//System.out.println("unaccepted:"+unaccepted);
				//System.out.println(key.intValue()+" UNACCEPTED_SEND before: "+sequence);			
				unacceptedSREvent removedEvent = (unacceptedSREvent) sequence.remove(sequence.indexOf(unaccepted));
				if (removedEvent == null)
					System.out.println("Internal Error: Failure removing unaccepted send");
				//System.out.println(key.intValue()+" UNACCEPTED_SYNCH_SEND after: " + sequence);
				unacceptedSR.put(key,sequence);
				
				if (removedEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
					int testAndReplayIndex = removedEvent.getTestAndReplayIndex();
					TestAndReplayCollection.changeToAccepted(testAndReplayIndex,actualCalledThread,
						actualCalledVersionNumber);
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
				//System.out.println("Removing unaccepted receive for "+ event.getCaller() + " " + event.getCalled());
				sequence = (ArrayList) unacceptedSR.get(key);
				//System.out.println(key.intValue()+" UNACCEPTED_RECEIVE before: "+sequence);			
				unacceptedSREvent removedEvent = (unacceptedSREvent) sequence.remove(sequence.indexOf(unaccepted));
				if (removedEvent == null)
					System.out.println("Internal Error: Failure removing unaccepted receive");
				//System.out.println(key.intValue()+" UNACCEPTED_RECEIVE after: " + sequence);
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
					//srEvent

					// don't add events for unaccepted_Asynch_send since they are already in the sequence
					if (!(event.getEventType().equals(UNACCEPTED_ASYNCH_SEND))) {

						//System.out.println("****Adding event****: " + event);
						TestAndReplayCollection.updateSequence(event);

	
						// create thread-based events; thread-based sequences written after all unaccepted events created
						if (numControllers == MULTIPLE) {

							Integer key1 = new Integer(channelIDGenerator.getInstance().getChannelID(event.getChannelName()));
							objectBasedSequenceCollection.getInstance().updateSequence(event,key1);

							srThreadEvent threadEvent;
							Integer key;
							if (event.getEventType() == UNACCEPTED_SYNCH_SEND) {
								threadEvent = new srThreadEvent(event.getCaller(),event.getCallerVersionNumber(),event.getCalled(),event.getCalledVersionNumber(),
									event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),
									event.getVectorTS());	
								key = new Integer(event.getCaller());
								//System.out.println("Adding unaccepted send for key " + key.intValue() + " and version " + event.getCallerVersionNumber());
							}
							else { // UNACCEPTED_RECEIVE
								threadEvent = new srThreadEvent(event.getCalled(),event.getCalledVersionNumber(),event.getCaller(),event.getCallerVersionNumber(),
									event.getChannelName(),event.getChannelVersionNumber(),event.getEventType(),
									event.getVectorTS());	
								key = new Integer(event.getCalled());
								//System.out.println("Adding unaccepted receive for key " + key.intValue() + " and version " + event.getCalledVersionNumber());
							}
	
							ThreadBasedSequenceCollection.getInstance().updateSequence(threadEvent,key);
						}
					} // not an unaccepted_asynch_send
				} // if size > 0
				else {
					//System.out.println("Empty list");
					//System.out.flush();
				}
			}

		//System.out.println("Exiting check");
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
		//System.out.println(key.intValue()+" UNACCEPTED_SEND sequence before: "+sequence);
		//System.out.println("Putting new unaccepted_asynch_send event for key " + key.intValue() + " index " + index);
		sequence.add(unaccepted);
		unacceptedSR.put(key,sequence);
		//System.out.println(key.intValue()+" UNACCEPTED_SEND sequence after: " + (ArrayList)unacceptedSR.get(key));

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
		//System.out.println(key.intValue()+" UNACCEPTED_ASYNCH_SEND sequence before: "+sequence);
		//System.out.println("Putting new unaccepted_asynch_send event for key " + key.intValue() + " index " + testAndReplayIndex);
		sequence.add(unaccepted);
		unacceptedSR.put(key,sequence);
		//System.out.println(key.intValue()+" UNACCEPTED_ASYNCH_SEND sequence after: " + (ArrayList)unacceptedSR.get(key));

	}

	private void outputThreadBasedSequences() throws IOException {
		ThreadBasedSequenceCollection.getInstance().outputThreadBasedSequences();
	}

	private void outputObjectBasedSequences() throws IOException {
		objectBasedSequenceCollection.getInstance().outputObjectBasedSequences();
	}

	private void outputTraceSequences() throws IOException {
		synchronized (threadBasedMsgTracingAndReplayClassLock) { // ensure sequences are output only one time
			if (!traceSequencesHaveBeenOutput) {
				outputThreadBasedSequences(); // only one controller will be able to do this
				outputObjectBasedSequences(); // only one controller will be able to do this
				traceSequencesHaveBeenOutput = true;
			}
		}
	}

	public void run() {
		int index = 0;
		try {
			if (mode == TRACE) {
				while (true) {
	    			try {
						selectiveWaitC select = new selectiveWaitC();
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
							if (choice == 1) {
								outputTrace();
							}
							else if (choice == 2) {
								processSendReceive();
							}
							else { // timeout
								timeout.accept();
								checkForUnacceptedSendReceive();
								TestAndReplayCollection.outputObjectBasedTestAndReplaySequences();
								if (numControllers == MULTIPLE) { // wait for all controllers to create their unaccepted events
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
        				System.exit(1); 
					}
				break; // outer while
				} // while
			} // trace
			else if (mode == REPLAY) { // replay
			
				Vector alwaysOpenAlternatives = new Vector();
				alwaysOpenAlternatives.add(msgReceived);

				Vector openAlternatives = new Vector(); // contains open alternatives for current event
						
				selectiveWaitC select = new selectiveWaitC();
				//selectiveWait.delayAlternative delayA = select.new delayAlternative(50);
				int j = 0;
				int group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSendPermit[i],(i+1)+group);		// alternative 1 - (numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceivePermit[i],(i+1)+group);	// alternative (numberOfThreads+1)+1 - 2*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceivePermitX[i],(i+1)+group);	// alternative (2*numberOfThreads+1)+1 - 3*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSelectPermit[i],(i+1)+group);		// alternative (3*(numberOfThreads+1))+1 - 4*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSendExceptionPermit[i],(i+1)+group);	// alternative (4*(numberOfThreads+1))+1 - 5*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;				
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceiveExceptionPermit[i],(i+1)+group);	// alternative (5*(numberOfThreads+1))+1 - 6*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				select.add(msgReceived,1+group);						// alternative (6*(numberOfThreads+1)) + 1
				msgReceived.guard(true); 			
				//select.add(delayA);
				int choice = 0;
				simplesrEvent nextEvent = null;
				while(index < simplesrSequence.size()) {
					nextEvent = (simplesrEvent)simplesrSequence.elementAt(index);
					/* asynch_send = (caller,-1,asynch_send), asynch_receive = (caller, called, asynch_receive */
					/* synchronous rendezvous = (caller,called,sr_synchronization)                             */
					//System.out.println("Next caller is: " + ((simplesrEvent)simplesrSequence.elementAt(index)).getCaller());
					//System.out.println("Next called is: " + ((simplesrEvent)simplesrSequence.elementAt(index)).getCalled());
					//System.out.println("index="+index+", "+simplesrSequence.size()); 
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
		 			//System.out.println("Choosing.");
					choice = select.choose(openAlternatives);
		 			//System.out.println("Choice is alternative " + choice);
					if (choice <= (numberOfThreads+1)) {
						requestSendPermit[choice-1].acceptAndReply();
						if (nextEvent.getCalled() == -2)
							index++;
						//System.out.println("SendPermit granted for " + (choice-1));
					}
					else if (choice <= 2*(numberOfThreads+1)) {
						requestReceivePermit[choice-(numberOfThreads+1)-1].acceptAndReply();
						if (nextEvent.getCaller() == -2)
							index++;
						//System.out.println("ReceivePermit granted for " + (choice-(numberOfThreads+1)-1));
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
							//requestSendPermit[caller-1].acceptAndReply();
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
					else {
						msgReceived.acceptAndReply();
						//System.out.println("msgReceived");
   				   ++index;
					}
				}
				if (numControllers == SINGLE) { // don't exit program if other controllers exist
					System.out.println("\n\n***Sequence Completed***\n");
					System.out.flush();
					System.exit(0);
            }
				else {
					Thread.sleep(2000);
					System.exit(0);
				}
			}
			else if (mode == TEST) { // test
				Vector alwaysOpenAlternatives = new Vector();
				alwaysOpenAlternatives.add(msgReceived);
				alwaysOpenAlternatives.add(msgReceivedX);

				Vector openAlternatives = new Vector(); // contains open alternatives for current event

				selectiveWaitC select = new selectiveWaitC();
				selectiveWaitC.delayAlternative timeout = select.new delayAlternative(4000);
				int j = 0;
				int group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSendPermit[i],(i+1)+group);		// alternative 1 - (numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceivePermit[i],(i+1)+group);	// alternative (numberOfThreads+1)+1 - 2*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceivePermitX[i],(i+1)+group);	// alternative (2*numberOfThreads+1)+1 - 3*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSelectPermit[i],(i+1)+group);		// alternative (3*(numberOfThreads+1))+1 - 4*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestElseDelayPermit[i],(i+1)+group);	// alternative (4*(numberOfThreads+1))+1 - 5*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestSendExceptionPermit[i],(i+1)+group);	// alternative (5*(numberOfThreads+1))+1 - 6*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++)
					select.add(requestReceiveExceptionPermit[i],(i+1)+group);	// alternative (6*(numberOfThreads+1))+1 - 7*(numberOfThreads+1)
				j++; group = (numberOfThreads+1) * j;
				select.add(msgReceived,1+group);						// alternative (7*(numberOfThreads+1))
				select.add(msgReceivedX,2+group);					// alternative (7*(numberOfThreads+2)) 
				select.add(timeout);						   			// alternative (7*(numberOfThreads+3))
				msgReceived.guard(true); 
				msgReceivedX.guard(true);
				timeout.guard(true);
				int choice = 0;
				//String channelName = null;
				srEvent nextEvent = null;
				while(index < srSequence.size()) {
					nextEvent = (srEvent)srSequence.get(index);
					//System.out.println("Next caller is: " + nextEvent.getCaller());
					//System.out.println("Next called is: " + nextEvent.getCalled());

					openAlternatives.clear();
					openAlternatives.addAll(alwaysOpenAlternatives);
					
					boolean g = false;
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCaller() && 
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
						g = (i==nextEvent.getCalled() && 
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
						g = (i==nextEvent.getCalled() && 
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
						g = (i==nextEvent.getCalled() && 
							(nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
							nextEvent.getEventType().equals(START_ENTRY) ||
							nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
							nextEvent.getEventType().equals(UNACCEPTED_RECEIVE) ||
							nextEvent.getEventType().equals(ELSE_DELAY)));
						requestSelectPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestSelectPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCalled()  && 
							nextEvent.getEventType().equals(SEND_EXCEPTION));
						requestElseDelayPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestElseDelayPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCaller()  && 
							nextEvent.getEventType().equals(SEND_EXCEPTION));
						requestSendExceptionPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestSendExceptionPermit[i]);
					}
					for (int i=0; i<(numberOfThreads+1);i++) {
						g = (i==nextEvent.getCalled() && 
							nextEvent.getEventType().equals(RECEIVE_EXCEPTION));
						requestReceiveExceptionPermit[i].guard(g);
						if (g) openAlternatives.addElement(requestReceiveExceptionPermit[i]);
					}
		 			//System.out.println("Choosing.");
					choice = select.choose(openAlternatives);
					requestPermitMessage msg = null;
		 			//System.out.println("Choice is alternative " + choice);
					if (choice <= (numberOfThreads+1)) {
						msg = (requestPermitMessage)requestSendPermit[choice-1].accept();
						if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
							System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on send()");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						if ((msg.getVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on send()");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						requestSendPermit[choice-1].reply();
						if (nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND))
							index++;
						//System.out.println("SendPermit granted for " + (choice-1));
					}
					else if (choice <= 2*(numberOfThreads+1)) {
						msg = (requestPermitMessage)requestReceivePermit[choice-(numberOfThreads+1)-1].accept();
						if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
							System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						requestReceivePermit[choice-(numberOfThreads+1)-1].reply();
						if (nextEvent.getEventType().equals(UNACCEPTED_RECEIVE))
							index++;
						//System.out.println("ReceivePermit granted for " + (choice-(numberOfThreads+1)-1));
 					}
					else if (choice <= 3*(numberOfThreads+1)) {
						msg = (requestPermitMessage)requestReceivePermitX[choice-(2*(numberOfThreads+1))-1].accept();
						if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
							System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						
						requestReceivePermitX[choice-(2*(numberOfThreads+1))-1].reply(new Integer(nextEvent.getCaller()));
						if (nextEvent.getEventType().equals(UNACCEPTED_RECEIVE))
							index++;
						//System.out.println("ReceivePermit granted for " + (choice-(2*(numberOfThreads+1))-1));
					}
					else if (choice <= 4*(numberOfThreads+1)) {
						requestSelectPermit[choice-(3*(numberOfThreads+1))-1].accept();
						boolean oneArrival = true;
						int caller = nextEvent.getCaller();
						if (caller != -1) {
							oneArrival = true;
							//requestSendPermit[caller-1].acceptAndReply();
						}
						else
							oneArrival = false;
						requestSelectPermit[choice-(3*(numberOfThreads+1))-1].reply(new Boolean(oneArrival));
					}
					else if (choice <= 5*(numberOfThreads+1)) {
						msg = (requestPermitMessage)requestElseDelayPermit[choice-(4*(numberOfThreads+1))-1].accept();
						if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
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
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						if ((msg.getVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on send exception");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
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
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive exception");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}	
						requestReceiveExceptionPermit[choice-(6*(numberOfThreads+1))-1].reply();
					}
					else if (choice == (7*(numberOfThreads+1))+1){
						msgReceived.acceptAndReply();
						//System.out.println("msgReceived");
   				   ++index;
					}
					else if (choice == (7*(numberOfThreads+1))+2){
						msgReceivedMessage Rmsg = (msgReceivedMessage) msgReceivedX.accept();
						if ((Rmsg.getCaller() != nextEvent.getCaller())) {
							System.out.println("Infeasible Sequence, unexpected caller " + Rmsg.getCaller() + " on receive");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}
						if ((Rmsg.getCallerVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
							System.out.println("Infeasible Sequence, unexpected caller version number " + Rmsg.getCallerVersionNumber() + " on receive");
							System.out.println("Expected event was: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+
							","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
							System.exit(1);
						}							
						
   				   ++index;
   				   msgReceivedX.reply();
					}
					else { 
						timeout.accept();
						System.out.println("Infeasible Sequence - timeout waiting for event: ("+
							nextEvent.getCaller()+","+nextEvent.getCalled()+","+
							nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
							","+nextEvent.getChannelName()+")");
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
					System.exit(0);
				}
			}
			else if (mode == RT) { // RT
			  try {
			   if (!usingRTDriver) {
			   	System.out.println();
			   	System.out.println("Error: Reachability testing must be performed using an RT Driver.");
			   	System.out.println("       See the UserManual for instructions and library file RTDriver.java");
			   	System.out.println("         for an example.");
			   	System.exit(1);
			   }

				Vector alwaysOpenAlternatives = new Vector();
				alwaysOpenAlternatives.add(traceMsg);
				alwaysOpenAlternatives.add(traceSendReceive);
				alwaysOpenAlternatives.add(msgReceived);
				alwaysOpenAlternatives.add(msgReceivedX);
				alwaysOpenAlternatives.add(sendArrivedRT);
				alwaysOpenAlternatives.add(monitorEnteredRT);

				Vector ItraceAlternatives = new Vector(); // open alternatives during tracing
				// used to init traceAlternatives during tracing part; 2 more alts set dynamically
				ItraceAlternatives.add(traceMsg); // make sure this is first alternative checked
				ItraceAlternatives.add(traceSendReceive);
				ItraceAlternatives.add(msgReceived);
				ItraceAlternatives.add(msgReceivedX);
				ItraceAlternatives.add(sendArrivedRT);
				ItraceAlternatives.add(monitorEnteredRT);
				ItraceAlternatives.add(appCompleted);
				
				for (int i=0; i<(numberOfThreads+1);i++) {
					//traceAlternatives.add(requestSendPermit[i]);
					ItraceAlternatives.add(requestSendPermitX[i]);
					ItraceAlternatives.add(requestSendPermitMS[i]);
					//traceAlternatives.add(requestReceivePermit[i]);
					// Next 2 are set dynamically
					//traceAlternatives.add(requestReceivePermitX[i]);
					//traceAlternatives.add(requestReceivePermitMS[i]);
					ItraceAlternatives.add(requestSelectPermit[i]);
					//traceAlternatives.add(requestElseDelayPermit[i]);
					//traceAlternatives.add(requestSendExceptionPermit[i]);
					//traceAlternatives.add(requestReceiveExceptionPermit[i]);
					ItraceAlternatives.add(requestSynchSendPermit[i]);
				}

				Vector openAlternatives = new Vector(); // contains open alternatives for current event


				selectiveWaitC select = new selectiveWaitC();
				selectiveWaitC.delayAlternative timeout = select.new delayAlternative(100);
				int j = 0;
				int group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestSendPermit[i],(i+1)+group);		// alternative 1 - (numberOfThreads+1)
					////traceAlternatives.addElement(requestSendPermit[i]); // not used for RT
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestSendPermitX[i],(i+1)+group);		// alternative (numberOfThreads+1)+1 - 2*(numberOfThreads+1)
					//traceAlternatives.addElement(requestSendPermitX[i]);
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestSendPermitMS[i],(i+1)+group);		// alternative (2*numberOfThreads+1)+1 - 3*(numberOfThreads+1)
					//traceAlternatives.addElement(requestSendPermitMS[i]);
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestReceivePermit[i],(i+1)+group);	// alternative (3*numberOfThreads+1)+1 - 4*(numberOfThreads+1)
					////traceAlternatives.addElement(requestReceivePermit[i]); // not used for rt
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestReceivePermitX[i],(i+1)+group);	// alternative (4*numberOfThreads+1))+1 - 5*(numberOfThreads+1)
					//traceAlternatives.addElement(requestReceivePermitX[i]);
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestReceivePermitMS[i],(i+1)+group);	// alternative (5*numberOfThreads+1)+1 - 6*(numberOfThreads+1)
					//traceAlternatives.addElement(requestReceivePermitMS[i]);
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestSelectPermit[i],(i+1)+group);		// alternative (6*numberOfThreads+1)+1 - 7*(numberOfThreads+1)
					//traceAlternatives.addElement(requestSelectPermit[i]);
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestElseDelayPermit[i],(i+1)+group);	// alternative (7*numberOfThreads+1)+1 - 8*(numberOfThreads+1)
					////traceAlternatives.addElement(requestElseDelayPermit[i]); // not implemented for RT
				}
				j++; group = (numberOfThreads+1) * j;					
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestSendExceptionPermit[i],(i+1)+group);	// alternative (8*numberOfThreads+1)+1 - 9*(numberOfThreads+1)
					////traceAlternatives.addElement(requestSendExceptionPermit[i]); // not used for rt
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestReceiveExceptionPermit[i],(i+1)+group);	// alternative (9*numberOfThreads+1)+1 - 10*(numberOfThreads+1)
					////traceAlternatives.addElement(requestReceiveExceptionPermit[i]); // not used for RT
				}
				j++; group = (numberOfThreads+1) * j;
				for (int i=0; i<(numberOfThreads+1);i++) {
					select.add(requestSynchSendPermit[i],(i+1)+group);	// alternative (10*numberOfThreads+1) + 1 - 11*(numberOfThreads+1)
					//traceAlternatives.addElement(requestSynchSendPermit[i]);
				}
				j++; group = (numberOfThreads+1) * j;
				// these added to traceAlternatives above
				select.add(msgReceived,1+group);						// alternative (11*(numberOfThreads+1)) + 1
				select.add(msgReceivedX,2+group);						// alternative (11*(numberOfThreads+1)) + 2
				select.add(sendArrivedRT,3+group);						// alternative (11*(numberOfThreads+1)) + 3
				select.add(monitorEnteredRT,4+group);					// alternative (11*(numberOfThreads+1)) + 4
				select.add(traceMsg,5+group);							// alternative (11*(numberOfThreads+1)) + 5
				select.add(traceSendReceive,6+group);					// alternative (11*(numberOfThreads+1)) + 6
				select.add(appCompleted,7+group);						// alternative (11*(numberOfThreads+1)) + 7
				select.add(timeout);						   	// alternative (11*(numberOfThreads+1)) + 8
				traceMsg.guard(true);
				traceSendReceive.guard(true);
				msgReceived.guard(true); 
				msgReceivedX.guard(true);
				sendArrivedRT.guard(true);
				monitorEnteredRT.guard(true);
				appCompleted.guard(false);
				timeout.guard(true);
				
				//timeout.guard(false);
				int choice = 0;
				//String channelName = null;
				srEvent nextEvent = null;
				RTTracing = false; // doing replay part
				RTAllRecollected = false;
				
				//srSeq variant = generator.getVariant ();
								
				//ObjectInputStream inputInfeasibleSequence = new ObjectInputStream(new FileInputStream("infeasibleSequence.dat"));
				//srSequence = (ArrayList)inputInfeasibleSequence.readObject();
				//srSeq variant = new srSeq();
				//variant.setEvents(srSequence);
				//System.out.println("got infeasible sequence");
				
				//if (variant==null) {
				// System.out.println("variant is null");
				// System.out.flush();
				//}

				//srSequence = variant.getEvents();
				

						   
				HashMap recollectedChannels = new HashMap(); // channelNames->receivingThread of recollected receives
				HashMap recollectedThreads = new HashMap(); // receiving threads of recollected receive -> event
				HashSet recollectedPermitted = new HashSet(); // recollected recieves permitted but not yet executed
				HashSet recollectedNoOldOrNewSends = new HashSet(); // recollected recieves with no old or new sends 
				HashMap recollectedMonitors = new HashMap(); // receiving threads of recollected receive -> event
				HashMap recollectedMonitorNames = new HashMap(); // monoitor names of recollected receives
				HashSet recollectedMonitorCallers = new HashSet(); // threads that have called recollected recieve for monitors
				HashSet recollectedMonitorCleared = new HashSet(); // monitor calls that are not to recollected receives
				                                                   // so they are allowed to get receivePermit
				HashMap recollectedOldMonitorSends = new HashMap(); // monitorID -> callerID of old send; for each monitor
				                                                    // the old sends that become availabe after a recollected event				                                                
				                                                    
				HashMap recollectedWithOldPending = new HashMap(); // thread IDs for threads with a recollected event that has an
				                                                   // old event pending. If no new sends and pending old then no deadlock
				                                                   // mapped to linkedlist of sending threads for the old events
				                                                   				                                                   
while(true) { // main while-loop of RT, never broken, i.e., use same controller for entire RT 
// for each sequence
				index = 0;
				traceMsg.guard(true);
				traceSendReceive.guard(true);
				msgReceived.guard(true); 
				msgReceivedX.guard(true);
				sendArrivedRT.guard(true);
				monitorEnteredRT.guard(true);
				appCompleted.guard(false);
				timeout.guard(true);
			
				for (int i=0; i<(numberOfThreads+1);i++) {
					requestSendPermit[i].guard(false); 
					requestSendPermitX[i].guard(false); 
					requestSendPermitMS[i].guard(false); 
					requestReceivePermit[i].guard(false);
					requestReceivePermitX[i].guard(false);
					requestReceivePermitMS[i].guard(false);
					requestSelectPermit[i].guard(false);
					requestElseDelayPermit[i].guard(false);
					requestSendExceptionPermit[i].guard(false);
					requestReceiveExceptionPermit[i].guard(false); 
					requestSynchSendPermit[i].guard(false);
				}
       
				RTTracing = false; // doing replay part
				ignoreSequence = false;
				RTAllRecollected = false;
				TestAndReplayCollection.clearSequence();
				unacceptedSR.clear();
				
				
				//ObjectInputStream inputSourceSequence = new ObjectInputStream(new FileInputStream("SourceSequenceSat1.dat"));
				//srSeq sourceX = (srSeq)inputSourceSequence.readObject();
				//System.out.println("Source:");
				//System.out.println(sourceX);
				//ObjectInputStream inputInfeasibleSequence = new ObjectInputStream(new FileInputStream("infeasibleSequenceSat1.dat"));
				//ArrayList var = (ArrayList)inputInfeasibleSequence.readObject();
				//srSeq temp = new srSeq();
				//temp.setEvents(var);	
				//System.out.println("Variant:");
				//System.out.println(temp);
				//System.exit(1);
				

// comment these 5 and uncomment the 2 below.				
				//ObjectInputStream inputInfeasibleSequence = new ObjectInputStream(new FileInputStream("infeasibleSequence.dat"));
				//srSequence = (ArrayList)inputInfeasibleSequence.readObject();
				//srSeq variant = new srSeq();
				//variant.setEvents(srSequence);	
				//System.out.println("got infeasible sequence");
				
            //rhc: need to keep variant declared since ref it when print infeas seq.
				//srSeq variant = new srSeq(); // generator.getVariant ();
				//System.out.println("get variant");


				srSeq variant = generator.getVariant ();
				//System.out.println("got variant");


				srSequence = variant.getEvents();
				
				//if (variant.getSource() != null) {
				//	System.out.println("NOT NULL1");
				//	System.out.println(variant.getSource());
				//	if (variant.getSource().getSource() != null) {
				//		System.out.println("NOT NULL2");
				//		System.out.println(variant.getSource());
				//	}
				//	else System.out.println("NULL2");
					//System.exit(0);
				//}
					
				//srSequence = previous;
				//variant.setEvents(srSequence);	
				
				for (int i = srSequence.size()-1; i >=0; i--) {
					srEvent e = (srEvent) srSequence.get(i);
					if (e.getIsRecollected()) { // true if e is recollected and has no send partner
						// note: a recollected event e that has its send partner changed
						// has e.getIsRecollected() == false, so we treat it as a regular receive
						// i.e., that is allowed to receive the send it is matched with
						if (e.getIsEntry() == false) {
							//***recollectedThreads.put(new Integer(e.getCalled()),e.getOpenList());
							recollectedThreads.put(new Integer(e.getCalled()),e);
 							recollectedNoOldOrNewSends.add(new Integer(e.getCalled()));
  							recollectedChannels.put(e.getChannelName(),new Integer(e.getCalled()));
  							//System.out.println("recollected thread event found: "+e);
  							//System.exit(1);
  						}
  						else { // monitor or semaphore/lock event
							//***recollectedMonitors.put(new Integer(e.getCalled()),e.getOpenList());
							recollectedMonitors.put(new Integer(e.getCalled()),e);							
 							recollectedNoOldOrNewSends.add(new Integer(e.getCalled()));
 							String monitorName = (e.getChannelName()).substring(0,e.getChannelName().indexOf(":"));
  							recollectedMonitorNames.put(monitorName,new Integer(e.getCalled()));
  							//System.out.println("recollected monitor event found: "+e);  						
  							//System.out.println("monitor is " + monitorName);
  						}
					}
					else {
						//System.out.println(((srSequence.size()-1)-i)+ " recollected event(s) found");
						break;
					}
				}

				//if (srSequence.size() != 0 && traceVariants == TRACEON) {
				//	traceTheVariant(srSequence);
		     	//}				
				
				//if (srSequence.size() == 0)
				//	System.out.println("Sequence size is " + srSequence.size());
				//else {
				//	System.out.println("variant is ("+srSequence.size()+")");
			   //	System.out.println(variant);
				//	//System.exit(0);
				//}
			
				while(index < srSequence.size()) {
					try {
						nextEvent = (srEvent)srSequence.get(index);
						if (nextEvent.getIsRecollected()) { // true if nextevent is recollected and has no send partner
	 						 //System.out.println("index at break: "+index);
							 break; // while for RT replay since we don't replay recollected events
						}
						//System.out.println("index:"+index);
						//System.out.println("Next caller is: " + nextEvent.getCaller());
						//System.out.println("Next called is: " + nextEvent.getCalled());
						//System.out.println("next event is " + nextEvent);
		 				//System.out.println("requestSendPermitMS[6].guard() is " + (requestSendPermitMS[6].testGuard()));
		 				//System.out.println("requestSendPermitX[4].guard() is " + (requestSendPermitX[4].testGuard()));
						
						int nextCaller = nextEvent.getCaller();
						int nextCalled = nextEvent.getCalled();
						
						openAlternatives.clear();
						openAlternatives.addAll(alwaysOpenAlternatives);
						
						if (nextCaller >= 0) {
/* RHC: This is not used for RT? */
							boolean g = false;
							g = (nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
								nextEvent.getEventType().equals(ASYNCH_SEND) ||
								nextEvent.getEventType().equals(SYNCH_SEND) ||
								nextEvent.getEventType().equals(START_ENTRY) ||
								nextEvent.getEventType().equals(FINISH_ENTRY) ||
								nextEvent.getEventType().equals(SEND_EXCEPTION) ||
								nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND) ||
								nextEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND));						
							requestSendPermit[nextCaller].guard(g);
							/* Not used for RT */
							//if (g) openAlternatives.addElement(requestSendPermit[nextCaller]);
							
							g = (nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
								nextEvent.getEventType().equals(ASYNCH_SEND) ||
/* RHC: Need this now. */
								nextEvent.getEventType().equals(SYNCH_SEND) ||
								nextEvent.getEventType().equals(START_ENTRY) ||
								nextEvent.getEventType().equals(FINISH_ENTRY) ||
								nextEvent.getEventType().equals(SEND_EXCEPTION) ||
								nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND) ||
								nextEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND));
							requestSendPermitX[nextCaller].guard(g);
							if (g) openAlternatives.addElement(requestSendPermitX[nextCaller]);
							
							g = (nextEvent.getEventType().equals(ASYNCH_SEND) ||
								nextEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND));
							requestSendPermitMS[nextCaller].guard(g);
							if (g) openAlternatives.addElement(requestSendPermitMS[nextCaller]);
							
							g = // receiver for entry and i is caller (since monitor is called)
								(nextEvent.getEventType().equals(ASYNCH_RECEIVE) && 
								nextEvent.getIsEntry()==true);
								//System.out.println(i+":"+
								//(nextEvent.getEventType().equals(ASYNCH_RECEIVE) && 
								//nextEvent.getIsEntry()==true && i==nextEvent.getCaller()));
								//||
								//(i==nextEvent.getCalled() && 
								//(nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
								//nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
								//nextEvent.getEventType().equals(START_ENTRY) ||
								//nextEvent.getEventType().equals(FINISH_ENTRY) ||
								//nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
								//nextEvent.getEventType().equals(UNACCEPTED_RECEIVE)))
							requestReceivePermitMS[nextCaller].guard(g);
							if (g) openAlternatives.addElement(requestReceivePermitMS[nextCaller]);
							
							g = nextEvent.getEventType().equals(SEND_EXCEPTION);
							requestSendExceptionPermit[nextCaller].guard(g);
							/* Not implemented by RT */
							//if (g) openAlternatives.addElement(requestSendExceptionPermit[nextCaller]);
							
							g = (nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
/* RHC: no synch send when next event is an asynch receive? */
								//nextEvent.getEventType().equals(SYNCH_SEND) ||
								//nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
								//nextEvent.getEventType().equals(START_ENTRY) ||
								//nextEvent.getEventType().equals(FINISH_ENTRY) ||
								nextEvent.getEventType().equals(SEND_EXCEPTION)); //  ||
/* RHC: No? this is handled by requestSendPermitX and msgReceived? */
								//nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND) ||
								//nextEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND)));							
							requestSynchSendPermit[nextCaller].guard(g);
							if (g) openAlternatives.addElement(requestSynchSendPermit[nextCaller]);
						}
						
						if (nextCalled >= 0) {
						 boolean g = false;
						 // nextCalled could be greater than maxThreads if nextCalled
						 // is the ID of a semaphore/monitor/lock object
						 g = (!(nextEvent.getEventType2().equals(SEMAPHORE_CALL) ||
                         nextEvent.getEventType2().equals(SEMAPHORE_COMPLETION) ||
                         nextEvent.getEventType2().equals(MONITOR_CALL) ||
                         nextEvent.getEventType2().equals(MONITOR_ENTRY) ||
                         nextEvent.getEventType2().equals(LOCK_CALL) ||
                         nextEvent.getEventType2().equals(LOCK_COMPLETION))) 
                         && (nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
								nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
								nextEvent.getEventType().equals(START_ENTRY) ||
								nextEvent.getEventType().equals(FINISH_ENTRY) ||
								nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
								nextEvent.getEventType().equals(UNACCEPTED_RECEIVE));
					 	 requestReceivePermit[nextCalled].guard(g);
						 /* Not used for RT */
						 //if (g) openAlternatives.addElement(requestReceivePermit[nextCalled]);
						
  					 	g = (!(nextEvent.getEventType2().equals(SEMAPHORE_CALL) ||
                         nextEvent.getEventType2().equals(SEMAPHORE_COMPLETION) ||
                         nextEvent.getEventType2().equals(MONITOR_CALL) ||
                         nextEvent.getEventType2().equals(MONITOR_ENTRY) ||
                         nextEvent.getEventType2().equals(LOCK_CALL) ||
                         nextEvent.getEventType2().equals(LOCK_COMPLETION)))
                         && 
/* RHC: */
								(nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
		  						//nextEvent.getEventType().equals(SYNCH_RECEIVE) ||
								nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
								nextEvent.getEventType().equals(START_ENTRY) ||
								nextEvent.getEventType().equals(FINISH_ENTRY) ||
								nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
								nextEvent.getEventType().equals(UNACCEPTED_RECEIVE));
							/*System.out.println("requestReceivePermitX["+1+ "] = " +
							((nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
								nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
								nextEvent.getEventType().equals(START_ENTRY) ||
								nextEvent.getEventType().equals(FINISH_ENTRY) ||
								nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
								nextEvent.getEventType().equals(UNACCEPTED_RECEIVE))
							));
							System.out.println("requestReceivePermitX["+nextCalled+ "] = " +
							((nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
								nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
								nextEvent.getEventType().equals(START_ENTRY) ||
								nextEvent.getEventType().equals(FINISH_ENTRY) ||
								nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
								nextEvent.getEventType().equals(UNACCEPTED_RECEIVE))
							));
							*/
						 requestReceivePermitX[nextCalled].guard(g);
						 if (g) openAlternatives.addElement(requestReceivePermitX[nextCalled]);
							
						 g = (!(nextEvent.getEventType2().equals(SEMAPHORE_CALL) ||
                         nextEvent.getEventType2().equals(SEMAPHORE_COMPLETION) ||
                         nextEvent.getEventType2().equals(MONITOR_CALL) ||
                         nextEvent.getEventType2().equals(MONITOR_ENTRY) ||
                         nextEvent.getEventType2().equals(LOCK_CALL) ||
                         nextEvent.getEventType2().equals(LOCK_COMPLETION)))
                         && 								
                        (nextEvent.getEventType().equals(SR_SYNCHRONIZATION) ||
 /* kludge*/				nextEvent.getEventType().equals(ASYNCH_RECEIVE) ||
								nextEvent.getEventType().equals(START_ENTRY) ||
								nextEvent.getEventType().equals(RECEIVE_EXCEPTION) ||
								nextEvent.getEventType().equals(UNACCEPTED_RECEIVE) ||
								nextEvent.getEventType().equals(ELSE_DELAY));
							requestSelectPermit[nextCalled].guard(g);
							if (g) openAlternatives.addElement(requestSelectPermit[nextCalled]);
							
						 g = (!(nextEvent.getEventType2().equals(SEMAPHORE_CALL) ||
                         nextEvent.getEventType2().equals(SEMAPHORE_COMPLETION) ||
                         nextEvent.getEventType2().equals(MONITOR_CALL) ||
                         nextEvent.getEventType2().equals(MONITOR_ENTRY) ||
                         nextEvent.getEventType2().equals(LOCK_CALL) ||
                         nextEvent.getEventType2().equals(LOCK_COMPLETION)))
                         && (nextEvent.getEventType().equals(SEND_EXCEPTION));
                   requestElseDelayPermit[nextCalled].guard(g);
						 /* Not implemented for RT */
						 //if (g) openAlternatives.addElement(requestElseDelayPermit[nextCalled]);
						
						 g = (!(nextEvent.getEventType2().equals(SEMAPHORE_CALL) ||
                         nextEvent.getEventType2().equals(SEMAPHORE_COMPLETION) ||
                         nextEvent.getEventType2().equals(MONITOR_CALL) ||
                         nextEvent.getEventType2().equals(MONITOR_ENTRY) ||
                         nextEvent.getEventType2().equals(LOCK_CALL) ||
                         nextEvent.getEventType2().equals(LOCK_COMPLETION)))
                         && (nextEvent.getEventType().equals(RECEIVE_EXCEPTION));
  						 requestReceiveExceptionPermit[nextCalled].guard(g);
						 /* Not used for RT */
						 //if (g) openAlternatives.addElement(requestReceiveExceptionPermit[nextCalled]);
						}

		 				//System.out.println("Choosing replay");
		 				//System.out.println("requestReceivePermitX[1].guard() is " + (requestReceivePermitX[1].testGuard()));
		 				//System.out.println("requestSendPermitX[3].guard() is " + (requestSendPermitX[3].testGuard()));
		 				//System.out.println("requestSendPermitX[4].guard() is " + (requestSendPermitX[4].testGuard()));

						choice = select.choose(openAlternatives);
						requestPermitMessage msg = null;
			 			//System.out.println("Choice is alternative " + choice);			
						if (choice <= (numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSendPermit[choice-1].accept();
							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on send()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}
/*
							if ((msg.getVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
								//System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on send()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
*/
							requestSendPermit[choice-1].reply();
							//requestSendPermit[choice-1].guard(false); requestSendPermitX[choice-1].guard(false); 
							//requestSendPermitMS[choice-1].guard(false);requestReceivePermitMS[choice-1].guard(false);
							//requestSendExceptionPermit[choice-1].guard(false);requestSynchSendPermit[choice-1].guard(false);
							
							if (nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND)) {
								//index++;
								requestSendPermit[choice-1].guard(false); requestSendPermitX[choice-1].guard(false); 
								requestSendPermitMS[choice-1].guard(false);requestReceivePermitMS[choice-1].guard(false);
								requestSendExceptionPermit[choice-1].guard(false);requestSynchSendPermit[choice-1].guard(false);
							}
							//System.out.println("SendPermit granted for " + (choice-1));
						}
						else if (choice <= 2*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSendPermitX[choice-(numberOfThreads+1)-1].accept();
							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on send()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}
/*
							if ((msg.getVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
								System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on send()");
								System.out.println("Expected event was: ("+
								nextEvent.getCaller()+","+nextEvent.getCalled()+
								","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								","+nextEvent.getChannelName()+")");
								System.exit(1);
							}	
*/
							//boolean old = variant.isOld(nextEvent); // always old since in variant
							//System.out.println("next event is old" );
							requestSendPermitX[choice-(numberOfThreads+1)-1].reply(new Boolean(true));
							if (recollectedChannels.containsKey(msg.getChannelName())) {
								Integer ID = (Integer) recollectedChannels.get(msg.getChannelName());
								// note that an old send can be synchronized with a recollected receive
								// by removing the receiving thread. (All threads that remain at end
								// have no old or new sends)
								if (nextEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
								// this is an old send; can it be synched with recollected receive?
								// yes if asynch or synch w/ no select. With select depends on OpenList?
									 srEvent e = (srEvent) recollectedThreads.get(ID);
									 ArrayList OpenList = e.getOpenList();
									 //if (OpenList == null) {
									 //	System.out.println("null openlist for " + e);
									 //}
									 boolean found = false;
									 for (int i=0; i<OpenList.size(); i++) {
										OpenEvent p = (OpenEvent) OpenList.get(i);
										if (p.getLabel().equals(msg.getChannelName())) {
											found = true;
											break;
										}
									 }
	/* RHC: if asynch, it will be found, and that is all we need as we are addressing the case
	   where T issues old send and then new send. this can't happen for synch channels.
	   But what if select with aynchPort?
   */
									 if (found) { // (OpenList.contains(msg.getChannelName())) {
								    	recollectedNoOldOrNewSends.remove(ID); // okay if 2nd remove => noop
   								 	//recollectedWithOldPending.add(ID); // old send pending for thread ID; okay if 2nd add (ignored)
										ArrayList sendingThreads;
										if (recollectedWithOldPending.containsKey(ID)) 
											sendingThreads = (ArrayList)recollectedWithOldPending.get(ID);
										else
											sendingThreads = new ArrayList();
										sendingThreads.add(new Integer(choice-(numberOfThreads+1)-1));
										recollectedWithOldPending.put(ID,sendingThreads);   								 	
										//System.out.println("recollectedNoOldOrNewSends size is " + 	recollectedNoOldOrNewSends.size());
									 }
								}
								//recollectedNoOldOrNewSends.remove(ID);
								//System.out.println("recollectedNoOldOrNewSends size is " + 	recollectedNoOldOrNewSends.size());
							   //System.out.println("recollectedWithOldPending size is " + 	recollectedWithOldPending.size());
							}			
							
							//requestSendPermit[choice-(numberOfThreads+1)-1].guard(false); requestSendPermitX[choice-(numberOfThreads+1)-1].guard(false); 
							//requestSendPermitMS[choice-(numberOfThreads+1)-1].guard(false);requestReceivePermitMS[choice-(numberOfThreads+1)-1].guard(false);
							//requestSendExceptionPermit[choice-(numberOfThreads+1)-1].guard(false);requestSynchSendPermit[choice-(numberOfThreads+1)-1].guard(false);

							if (nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND)) {
/* RHC: need to get past the unaccepted_sycnh_send event? */
/* No: SYNCH ports, call msgReceived() after get sendPermitX permission. This incs index,
   which moves past the to-be-accepted or unaccepted send. Senders then requestSynchSendPermit
   which will be accepted when they are the caller for an SRsynchronization. The receiver
   will call msgReceived to inc past the SRsynchronization event.
*/
								//index++;
								requestSendPermit[choice-(numberOfThreads+1)-1].guard(false); requestSendPermitX[choice-(numberOfThreads+1)-1].guard(false); 
								requestSendPermitMS[choice-(numberOfThreads+1)-1].guard(false);requestReceivePermitMS[choice-(numberOfThreads+1)-1].guard(false);
								requestSendExceptionPermit[choice-(numberOfThreads+1)-1].guard(false);requestSynchSendPermit[choice-(numberOfThreads+1)-1].guard(false);
							}
							//System.out.println("SendPermitX granted for " + (choice-(numberOfThreads+1)-1));
						}
						else if (choice <= 3*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSendPermitMS[choice-(2*(numberOfThreads+1))-1].accept();
							//System.out.println("SendPermitMS granted for " + (choice-(2*numberOfThreads+1)-1));
							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on send()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}
/*
							if ((msg.getVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
								System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on send()");
								System.out.println("Expected event was: ("+
								nextEvent.getCaller()+","+nextEvent.getCalled()+
								","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								","+nextEvent.getChannelName()+")");
								System.exit(1);
							}	
*/
							//boolean old = variant.isOld(nextEvent); // always old since in variant
							//System.out.println("next event is old" );
							requestSendPermitMS[choice-(2*(numberOfThreads+1))-1].reply(new Boolean(true));
 							String monitorName = (msg.getChannelName()).substring(0,msg.getChannelName().indexOf(":"));
							if (recollectedMonitorNames.containsKey(monitorName)) {
								Integer ID = (Integer) recollectedMonitorNames.get(monitorName);
								// idicate that an old send can be synchronized with a recollected receive
								// by removing the receiving thread. (All threads that remain at end
								// have no old or new sends)
								if (nextEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
									if (!(recollectedOldMonitorSends.containsKey(ID))) { // put sender in map
		   							//System.out.println("new linked list for " + ID);
										recollectedOldMonitorSends.put(ID,new LinkedList());
									}
									//else System.out.println("no new linked list for " + ID);
									LinkedList monitorMessages = (LinkedList)recollectedOldMonitorSends.get(ID);
									// for monitors, canonly be once unaccepted send per thread since synchronous?
									monitorMessages.addLast(new Integer(choice-(2*(numberOfThreads+1))-1)); // add sender's ID to end of list
									//System.out.println("For "+ID+" monitorMessages.size() is " + monitorMessages.size());
								
									// this is an old send; can it be synched with recollected receive?
									srEvent e = (srEvent) recollectedMonitors.get(ID);
									ArrayList OpenList = e.getOpenList();
									if (monitorName.equals("P") || monitorName.equals("V")) {
										System.out.println("A semaphore or monitor named P or V may cause problems! Rename monitor.");
										System.exit(1);
									}
									boolean found1 = false;
									for (int i=0; i<OpenList.size(); i++) {
										OpenEvent p = (OpenEvent) OpenList.get(i);
										if (p.getLabel().equals(monitorName)) {
											found1 = true;
											break;
										}
									}
									if (found1) { // (OpenList.contains(monitorName)) {
									   // it's a monitor so call is always open (only P/V in sem. OpenLists)
									   recollectedNoOldOrNewSends.remove(ID);
									   //recollectedWithOldPending.add(ID); // old send pending for thread ID
									   // Note: sending threads of old sends are not tracked for synchronous constructs
										recollectedWithOldPending.put(ID,new LinkedList());
									   //System.out.println("old send for " + ID);
									   //System.out.println("recollectedNoOldOrNewSends size is " + 	recollectedNoOldOrNewSends.size());
									   //System.out.println("recollectedWithOldPending size is " + 	recollectedWithOldPending.size());
									}
									else {
										String opName = (msg.getChannelName()).substring(msg.getChannelName().indexOf(":")+1);
										//System.out.println("opName is " + opName);
										if (!(opName.equals("P") || opName.equals("V") || opName.equals("Read") || opName.equals("Write"))) {
											System.out.println("internal error: operation P or V or Read or Write expected in OpenList");
											System.exit(1);
										}
										boolean found2 = false;
										for (int i=0; i<OpenList.size(); i++) {
											OpenEvent p = (OpenEvent) OpenList.get(i);
											if (p.getLabel().equals(opName)) {
												found2 = true;
												break;
											}
										}
										if (found2) { // (OpenList.contains(opName)) {
									   	recollectedNoOldOrNewSends.remove(ID);
										   //recollectedWithOldPending.add(ID); // old send pending for thread ID		
										   // Note: sending threads of old sends are not tracked for synchronous constructs
										   recollectedWithOldPending.put(ID,new LinkedList());
										   //System.out.println("pending old send for " + ID);
										   //System.out.println("recollectedNoOldOrNewSends size is " + 	recollectedNoOldOrNewSends.size());
										   //System.out.println("recollectedWithOldPending size is " + 	recollectedWithOldPending.size());
										}
									}
								}
								//recollectedNoOldOrNewSends.remove(ID);
								//System.out.println("recollectedNoOldOrNewSends size is " + 	recollectedNoOldOrNewSends.size());
							   //System.out.println("recollectedWithOldPending size is " + 	recollectedWithOldPending.size());
							}
							else if (nextEvent.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
							// This send is cleared to be received since it's not for a 
							// recollected receive
								recollectedMonitorCleared.add(new Integer(choice-(2*(numberOfThreads+1))-1));
								// Note: this canot be received until tracing mode starts (as it is "unaccepted" in the variant")
							}
							
							//requestSendPermit[choice-(2*(numberOfThreads+1))-1].guard(false); requestSendPermitX[choice-(2*(numberOfThreads+1))-1].guard(false); 
							//requestSendPermitMS[choice-(2*(numberOfThreads+1))-1].guard(false);requestReceivePermitMS[choice-(2*(numberOfThreads+1))-1].guard(false);
							//requestSendExceptionPermit[choice-(2*(numberOfThreads+1))-1].guard(false);requestSynchSendPermit[choice-(2*(numberOfThreads+1))-1].guard(false);

							if (nextEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND)) {
							// this can't happen for monitors during RT? Right, since monitor will eventually be entered
								index++;								
								requestSendPermit[choice-(2*(numberOfThreads+1))-1].guard(false); requestSendPermitX[choice-(2*(numberOfThreads+1))-1].guard(false); 
								requestSendPermitMS[choice-(2*(numberOfThreads+1))-1].guard(false);requestReceivePermitMS[choice-(2*(numberOfThreads+1))-1].guard(false);
								requestSendExceptionPermit[choice-(2*(numberOfThreads+1))-1].guard(false);requestSynchSendPermit[choice-(2*(numberOfThreads+1))-1].guard(false);
							}
							//System.out.println("SendPermitMS granted for " + (choice-(2*(numberOfThreads+1))-1));
						}
						else if (choice <= 4*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestReceivePermit[choice-(3*(numberOfThreads+1))-1].accept();
							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
							if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
								//System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
							requestReceivePermit[choice-(3*(numberOfThreads+1))-1].reply();
							
							//requestReceivePermit[choice-(3*(numberOfThreads+1))-1].guard(false);
							//requestReceivePermitX[choice-(3*(numberOfThreads+1))-1].guard(false);
							//requestSelectPermit[choice-(3*(numberOfThreads+1))-1].guard(false);
							//requestElseDelayPermit[choice-(3*(numberOfThreads+1))-1].guard(false);
							//requestReceiveExceptionPermit[choice-(3*(numberOfThreads+1))-1].guard(false);		
							
							if (nextEvent.getEventType().equals(UNACCEPTED_RECEIVE)) {
								index++;
								requestReceivePermit[choice-(3*(numberOfThreads+1))-1].guard(false);
								requestReceivePermitX[choice-(3*(numberOfThreads+1))-1].guard(false);
								requestSelectPermit[choice-(3*(numberOfThreads+1))-1].guard(false);
								requestElseDelayPermit[choice-(3*(numberOfThreads+1))-1].guard(false);
								requestReceiveExceptionPermit[choice-(3*(numberOfThreads+1))-1].guard(false);
							}
							//System.out.println("ReceivePermit granted for " + (choice-(3*(numberOfThreads+1))-1));
 						}
						else if (choice <= 5*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestReceivePermitX[choice-(4*(numberOfThreads+1))-1].accept();

							//System.out.println("ReceivePermitX granted for " + (choice-(4*(numberOfThreads+1))-1));
							//System.out.println("Expected caller is " + nextEvent.getCaller());

							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
/*
							if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
								System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
								System.out.println("Expected event was: ("+
								nextEvent.getCaller()+","+nextEvent.getCalled()+
								","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								","+nextEvent.getChannelName()+")");
								System.exit(1);
							}
*/
							requestReceivePermitX[choice-(4*(numberOfThreads+1))-1].reply(new Integer(nextEvent.getCaller()));
							
							//requestReceivePermit[choice-(4*(numberOfThreads+1))-1].guard(false);
							//requestReceivePermitX[choice-(4*(numberOfThreads+1))-1].guard(false);
							//requestSelectPermit[choice-(4*(numberOfThreads+1))-1].guard(false);
							//requestElseDelayPermit[choice-(4*(numberOfThreads+1))-1].guard(false);
							//requestReceiveExceptionPermit[choice-(4*(numberOfThreads+1))-1].guard(false);		
			 				//System.out.println("***After off, requestReceivePermitX[1].guard() is " + (requestReceivePermitX[1].testGuard()));

	
							if (nextEvent.getEventType().equals(UNACCEPTED_RECEIVE)) {
								index++;
								requestReceivePermit[choice-(4*(numberOfThreads+1))-1].guard(false);
								requestReceivePermitX[choice-(4*(numberOfThreads+1))-1].guard(false);
								requestSelectPermit[choice-(4*(numberOfThreads+1))-1].guard(false);
								requestElseDelayPermit[choice-(4*(numberOfThreads+1))-1].guard(false);
								requestReceiveExceptionPermit[choice-(4*(numberOfThreads+1))-1].guard(false);			
							}
							//System.out.println("ReceivePermitX granted for " + (choice-(4*(numberOfThreads+1))-1));
						}
						else if (choice <= 6*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestReceivePermitMS[choice-(5*(numberOfThreads+1))-1].accept();
							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
/*
							if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
								System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
								System.out.println("Expected event was: ("+
								nextEvent.getCaller()+","+nextEvent.getCalled()+
								","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								","+nextEvent.getChannelName()+")");
								System.exit(1);
							}
*/
							requestReceivePermitMS[choice-(5*(numberOfThreads+1))-1].reply(new Integer(nextEvent.getCaller()));

							//requestReceivePermit[choice-(5*(numberOfThreads+1))-1].guard(false);
							//requestReceivePermitX[choice-(5*(numberOfThreads+1))-1].guard(false);
							//requestSelectPermit[choice-(5*(numberOfThreads+1))-1].guard(false);
							//requestElseDelayPermit[choice-(5*(numberOfThreads+1))-1].guard(false);
							//requestReceiveExceptionPermit[choice-(5*(numberOfThreads+1))-1].guard(false);		

							if (nextEvent.getEventType().equals(UNACCEPTED_RECEIVE)) {
							// this can't happen for monitors during RT?
								index++;
								requestReceivePermit[choice-(5*(numberOfThreads+1))-1].guard(false);
								requestReceivePermitX[choice-(5*(numberOfThreads+1))-1].guard(false);
								requestSelectPermit[choice-(5*(numberOfThreads+1))-1].guard(false);
								requestElseDelayPermit[choice-(5*(numberOfThreads+1))-1].guard(false);
								requestReceiveExceptionPermit[choice-(5*(numberOfThreads+1))-1].guard(false);		
							}
							//System.out.println("ReceivePermitMS granted for " + (choice-(5*(numberOfThreads+1))-1));
						}
						else if (choice <= 7*(numberOfThreads+1)) {
							requestSelectPermit[choice-(6*(numberOfThreads+1))-1].accept();
							boolean oneArrival = true;
							int caller = nextEvent.getCaller();
							if (caller != -1) {
								oneArrival = true;
								//requestSendPermit[caller-1].acceptAndReply();
							}
							else
								oneArrival = false;
							requestSelectPermit[choice-(6*(numberOfThreads+1))-1].reply(new Boolean(oneArrival));
							
							//requestReceivePermit[choice-(6*(numberOfThreads+1))-1].guard(false);
							//requestReceivePermitX[choice-(6*(numberOfThreads+1))-1].guard(false);
							//requestSelectPermit[choice-(6*(numberOfThreads+1))-1].guard(false);
							//requestElseDelayPermit[choice-(6*(numberOfThreads+1))-1].guard(false);
							//requestReceiveExceptionPermit[choice-(6*(numberOfThreads+1))-1].guard(false);		

							//System.out.println("SelectPermit granted for " + (choice-(6*(numberOfThreads+1))-1));
						}
						else if (choice <= 8*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestElseDelayPermit[choice-(7*(numberOfThreads+1))-1].accept();
							if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
								//System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive()");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
							requestElseDelayPermit[choice-(7*(numberOfThreads+1))-1].reply();
							
							//requestReceivePermit[choice-(7*(numberOfThreads+1))-1].guard(false);
							//requestReceivePermitX[choice-(7*(numberOfThreads+1))-1].guard(false);
							//requestSelectPermit[choice-(7*(numberOfThreads+1))-1].guard(false);
							//requestElseDelayPermit[choice-(7*(numberOfThreads+1))-1].guard(false);
							//requestReceiveExceptionPermit[choice-(7*(numberOfThreads+1))-1].guard(false);
						}
						else if (choice <= 9*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSendExceptionPermit[choice-(8*(numberOfThreads+1))-1].accept();
							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on send exception");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}
							if ((msg.getVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
								//System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on send exception");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
							requestSendExceptionPermit[choice-(8*(numberOfThreads+1))-1].reply();

							//requestSendPermit[choice-(8*(numberOfThreads+1))-1].guard(false); requestSendPermitX[choice-(8*(numberOfThreads+1))-1].guard(false); 
							//requestSendPermitMS[choice-(8*(numberOfThreads+1))-1].guard(false);requestReceivePermitMS[choice-(8*(numberOfThreads+1))-1].guard(false);
							//requestSendExceptionPermit[choice-(8*(numberOfThreads+1))-1].guard(false);requestSynchSendPermit[choice-(8*(numberOfThreads+1))-1].guard(false);

						}
						else if (choice <= 10*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestReceiveExceptionPermit[choice-(9*(numberOfThreads+1))-1].accept();
							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on receive exception");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}
							if ((msg.getVersionNumber() != (nextEvent.getCalledVersionNumber()))) {
								//System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on receive exception");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
							requestReceiveExceptionPermit[choice-(9*(numberOfThreads+1))-1].reply();
		
							//requestReceivePermit[choice-(9*(numberOfThreads+1))-1].guard(false);
							//requestReceivePermitX[choice-(9*(numberOfThreads+1))-1].guard(false);
							//requestSelectPermit[choice-(9*(numberOfThreads+1))-1].guard(false);
							//requestElseDelayPermit[choice-(9*(numberOfThreads+1))-1].guard(false);
							//requestReceiveExceptionPermit[choice-(9*(numberOfThreads+1))-1].guard(false);
						}
						else if (choice <= 11*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSynchSendPermit[choice-(10*(numberOfThreads+1))-1].accept();
							if (!(msg.getChannelName().equals(nextEvent.getChannelName()))) {
								//System.out.println("Infeasible Sequence, unexpected channel name " + msg.getChannelName() + " on synchronous send");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}
							if ((msg.getVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
								//System.out.println("Infeasible Sequence, unexpected version number " + msg.getVersionNumber() + " on synchronous send");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}	
							requestSynchSendPermit[choice-(10*(numberOfThreads+1))-1].reply();
							
							//requestSendPermit[choice-(10*(numberOfThreads+1))-1].guard(false); requestSendPermitX[choice-(10*(numberOfThreads+1))-1].guard(false); 
							//requestSendPermitMS[choice-(10*(numberOfThreads+1))-1].guard(false);requestReceivePermitMS[choice-(10*(numberOfThreads+1))-1].guard(false);
							//requestSendExceptionPermit[choice-(10*(numberOfThreads+1))-1].guard(false);requestSynchSendPermit[choice-(10*(numberOfThreads+1))-1].guard(false);

							//System.out.println("SynchSendPermit granted for " + ((choice-(10*(numberOfThreads+1))-1)));
 
						}
						else if (choice == (11*(numberOfThreads+1))+1){
							//System.out.println("chose msgReceived");
							msgReceived.acceptAndReply();
							//System.out.println("did msgReceived");

   					   ++index;
   					   
							requestSendPermit[nextCaller].guard(false); requestSendPermitX[nextCaller].guard(false); 
							requestSendPermitMS[nextCaller].guard(false);requestReceivePermitMS[nextCaller].guard(false);
							requestSendExceptionPermit[nextCaller].guard(false);requestSynchSendPermit[nextCaller].guard(false);
  					      if (!(nextEvent.getEventType2().equals(SEMAPHORE_CALL) ||
                         nextEvent.getEventType2().equals(SEMAPHORE_COMPLETION) ||
                         nextEvent.getEventType2().equals(MONITOR_CALL) ||
                         nextEvent.getEventType2().equals(MONITOR_ENTRY) ||
                         nextEvent.getEventType2().equals(LOCK_CALL) ||
                         nextEvent.getEventType2().equals(LOCK_COMPLETION))) {
							requestReceivePermit[nextCalled].guard(false);
							requestReceivePermitX[nextCalled].guard(false);
							requestSelectPermit[nextCalled].guard(false);
							requestElseDelayPermit[nextCalled].guard(false);
							requestReceiveExceptionPermit[nextCalled].guard(false);
						 }
						}
						else if (choice == (11*(numberOfThreads+1))+2){
							//System.out.println("chose msgReceivedX");
							msgReceivedMessage Rmsg = (msgReceivedMessage) msgReceivedX.accept();
							if ((Rmsg.getCaller() != nextEvent.getCaller())) {
								//System.out.println("Infeasible Sequence, unexpected caller " + Rmsg.getCaller() + " on receive");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}
							if ((Rmsg.getCallerVersionNumber() != (nextEvent.getCallerVersionNumber()))) {
								//System.out.println("Infeasible Sequence, unexpected caller version number " + Rmsg.getCallerVersionNumber() + " on receive");
								//System.out.println("Expected event was: ("+
								//nextEvent.getCaller()+","+nextEvent.getCalled()+
								//","+nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								//","+nextEvent.getChannelName()+")");
								System.out.println("Reachability testing failed. The failure is usually caused   ");
								System.out.println("   by a program that accesses a shared variable outside of   ");
								System.out.println("   a critical section. To verify that this is the cause, use ");
								System.out.println("   the Shared Variable classes in the library. These classes,");
								System.out.println("   as described in the User Manual, allow reachability       ");
								System.out.println("   testing to detect data races.                             ");

								System.exit(1);
							}							
							
   					   ++index;
   					   msgReceivedX.reply();
   					   
							requestSendPermit[nextCaller].guard(false); requestSendPermitX[nextCaller].guard(false); 
							requestSendPermitMS[nextCaller].guard(false);requestReceivePermitMS[nextCaller].guard(false);
							requestSendExceptionPermit[nextCaller].guard(false);requestSynchSendPermit[nextCaller].guard(false);
  					      if (!(nextEvent.getEventType2().equals(SEMAPHORE_CALL) ||
                         nextEvent.getEventType2().equals(SEMAPHORE_COMPLETION) ||
                         nextEvent.getEventType2().equals(MONITOR_CALL) ||
                         nextEvent.getEventType2().equals(MONITOR_ENTRY) ||
                         nextEvent.getEventType2().equals(LOCK_CALL) ||
                         nextEvent.getEventType2().equals(LOCK_COMPLETION))) {
							requestReceivePermit[nextCalled].guard(false);
							requestReceivePermitX[nextCalled].guard(false);
							requestSelectPermit[nextCalled].guard(false);
							requestElseDelayPermit[nextCalled].guard(false);
							requestReceiveExceptionPermit[nextCalled].guard(false);	
						 }
							
  							//System.out.println("did msgReceivedX");
						}
						else if (choice == (11*(numberOfThreads+1))+3){
							//System.out.println("chose sendArrivedRT");
							msg = (requestPermitMessage)sendArrivedRT.acceptAndReply();
						}
						else if (choice == (11*(numberOfThreads+1))+4){
							//System.out.println("chose monitorEnteredRT");
							msg = (requestPermitMessage)monitorEnteredRT.acceptAndReply();
						}
						else if (choice == (11*(numberOfThreads+1))+5){
							//System.out.println("chose output trace");
							outputTrace();
							//System.out.println("did output trace");
						}
						else if (choice == (11*(numberOfThreads+1))+6){
							//System.out.println("chose processSendReceive");
							processSendReceive();					
							//System.out.println("did processSendReceive");
						}
						else if (choice == (11*(numberOfThreads+1))+7){
							appCompleted.accept();
							System.out.println("Error: Application completed before prefix-replayed");
							System.exit(1);
						}
						else { 
							timeout.accept();
							//System.out.println("traceMsg.count() = " + traceMsg.count());
							System.out.println("Infeasible Sequence - timeout waiting for event: ("+
								nextEvent.getCaller()+","+nextEvent.getCalled()+","+
								nextEvent.getCallerVersionNumber()+","+nextEvent.getCalledVersionNumber()+
								","+nextEvent.getChannelName()+","+nextEvent.getEventType()+")");
							System.out.println("index:"+index);
				     		ObjectOutputStream outputSequence = new ObjectOutputStream(new FileOutputStream("infeasibleSequence.dat"));
							outputSequence.writeObject(srSequence);
							System.out.println("Infeasible Sequence recorded in file infeasibleSequence.dat");
							System.out.println(variant);
							System.out.println("Reachability testing failed. The failure is usually caused   ");
							System.out.println("   by a program that accesses a shared variable outside of   ");
							System.out.println("   a critical section. To verify that this is the cause, use ");
							System.out.println("   the Shared Variable classes in the library. These classes,");
							System.out.println("   as described in the User Manual, allow reachability       ");
							System.out.println("   testing to detect data races.                             ");
							System.out.println();							
							System.out.println("source:");
							SrSeqPO source = variant.getSource();
							//SrSeqPO parent = source.getSource();
							//srSeq convertedSource = SrSeqTranslator.convert(parent,true);
							srSeq convertedSource = SrSeqTranslator.convert(source,true);
							System.out.println(convertedSource);
							//ArrayList sourceSequence = convertedSource.getEvents();
				     		ObjectOutputStream outputSequenceS = new ObjectOutputStream(new FileOutputStream("SourceSequence.dat"));
							outputSequenceS.writeObject(convertedSource);
							System.out.println("Source Sequence recorded in file SourceSequence.dat");
							
							System.exit(1);
						}
					}
					catch (IOException e) {
	     				System.err.println("Error while writing trace file: " + e.toString());
   	  				System.exit(1); 
					}
				} // end while
				// Trace mode
				//System.out.println("Do Tracing");
				//System.out.println("recollectedThreads size:"+recollectedThreads.size());
				//System.out.println("recollectedMonitors size:"+recollectedMonitors.size());
				//System.out.println("recollectedPermitted size:"+recollectedPermitted.size());
				//System.out.println("recollectedMonitorCallers size:"+recollectedMonitorCallers.size());
				//System.out.println("recollectedMonitorCleared size:"+recollectedMonitorCleared.size());
         	RTTracing = true;
         	if (recollectedThreads.size() == 0 && recollectedMonitors.size() == 0) {
   	     		//System.out.println("starting trace mode: No recollected events found");
	       		RTAllRecollected = true; // otherwise there are some recollected events
	       	}
				for (int i=0; i<(numberOfThreads+1);i++) {
					requestSendPermit[i].guard(true); 
					requestSendPermitX[i].guard(true); 
					requestSendPermitMS[i].guard(true); 
					requestReceivePermit[i].guard(true);
				// guard for requestReceivePermitX and requestReceivePermitMS set in loop below
					requestSelectPermit[i].guard(true);
					requestElseDelayPermit[i].guard(true);
					requestSendExceptionPermit[i].guard(true);
					requestReceiveExceptionPermit[i].guard(true); 
					requestSynchSendPermit[i].guard(true);
				}
				
				traceMsg.guard(true);
				traceSendReceive.guard(true);
				appCompleted.guard(true);
				timeout.guard(true);
				msgReceived.guard(true); 
				msgReceivedX.guard(true);
				sendArrivedRT.guard(true);
				monitorEnteredRT.guard(true);
				requestPermitMessage msg = null;
				while (true) {
				Vector traceAlternatives = new Vector(ItraceAlternatives); // open alternatives during tracing
					try {
						for (int i=0; i<(numberOfThreads+1);i++) {
							boolean g = true;
							if (recollectedThreads.size() > 0) {
								Integer ID = new Integer(i);
							   if (recollectedThreads.containsKey(ID))
							   	if (recollectedPermitted.contains(ID))
						   			g = true;
							   	else 
					   				g = false;
							   else
							       g = true;
							}
							//if (i==8)
								//System.out.println("guard for requestReceivePermitX for " + i + " is " + g);
							requestReceivePermitX[i].guard(g);
							if (g) traceAlternatives.addElement(requestReceivePermitX[i]);
						}
						for (int i=0; i<(numberOfThreads+1);i++) {
							boolean g = true;
							if (recollectedMonitors.size() > 0) {
								Integer ID = new Integer(i);
							   //if (recollectedThreads.containsKey(ID))
							   	if (recollectedMonitorCallers.contains(ID) ||
							   			recollectedMonitorCleared.contains(ID))
						   			g = true;
							   	else 
					   				g = false;
							   //else
							   //   g = true;
							}
							requestReceivePermitMS[i].guard(g);
							if (g) traceAlternatives.addElement(requestReceivePermitMS[i]);
							//System.out.println("guard for requestReceivePermitS for " + i + " is " + g);
						}
						//System.out.println("choosing trace");
						choice = select.choose(traceAlternatives /*select.getList()*/);
						//System.out.println("chose " + choice);
						if (choice <= (numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSendPermit[choice-1].acceptAndReply();	
							//System.out.println("Trace: SendPermit granted for " + (choice-1));
						}
						else if (choice <= 2*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSendPermitX[choice-(numberOfThreads+1)-1].accept();
							requestSendPermitX[choice-(numberOfThreads+1)-1].reply(new Boolean(false));	
							//System.out.println("Trace: SendPermitX granted for " + (choice-(numberOfThreads+1)-1));
							// check if this new send is for a recollected receive
							/* Note; check for new send moved to ack code */
						}
						else if (choice <= 3*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSendPermitMS[choice-(2*(numberOfThreads+1))-1].accept();
							requestSendPermitMS[choice-(2*(numberOfThreads+1))-1].reply(new Boolean(false));	
							//System.out.println("Trace: SendPermitMS granted for " + (choice-(2*(numberOfThreads+1))-1));
							// check if this new send is for a recollected receive
 							String monitorName = (msg.getChannelName()).substring(0,msg.getChannelName().indexOf(":"));
 							//System.out.println("monitor name is " + monitorName);
							if (recollectedMonitorNames.containsKey(monitorName)) {
								//Integer ID = (Integer) recollectedMonitorNames.get(monitorName);
								// can't remove thread from recollectedThreads since need it in there
								// to correctly set guard on next iteration (where receive can be accepted)
								recollectedMonitorCallers.add(new Integer(choice-(2*(numberOfThreads+1))-1));

								/*Q1: not sure it's a new send yet.
								//recollectedNoOldOrNewSends.remove(ID);
								//System.out.println("recollectedNoOldOrNewSends size is" + 	recollectedNoOldOrNewSends.size());
								/*Q2: might be a, say, P operation that cannot complete*/
								//recollectedMonitorNames.remove(monitorName);
								//System.out.println("add "+(choice-(2*(numberOfThreads+1))-1)+" to recollectedMonitorCallers"); // and remove "+monitorName+"("+ID+") from recollectedMonitorNames");
								// make sure send arrives before recollected receive gets permission
							}
							else // it's not to a recollected receive so it is allowed to get receivePermit
								recollectedMonitorCleared.add(new Integer(choice-(2*(numberOfThreads+1))-1));
						}
						else if (choice <= 4*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestReceivePermit[choice-(3*(numberOfThreads+1))-1].acceptAndReply();
							//System.out.println("Trace: ReceivePermit granted for " + (choice-(3*(numberOfThreads+1))-1));
	 					}
						else if (choice <= 5*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestReceivePermitX[choice-(4*(numberOfThreads+1))-1].accept();
							Integer ID = new Integer(choice-(4*(numberOfThreads+1))-1);
							if (!(recollectedPermitted.contains(ID))) {
								//System.out.println("Receive by "+(ID.intValue())+" is not a recollected receive");
								requestReceivePermitX[choice-(4*(numberOfThreads+1))-1].reply(new Integer(-1));
							}
							else {
								requestReceivePermitX[choice-(4*(numberOfThreads+1))-1].reply(new Integer(-2));
								//System.out.println("Receive by "+ID+" is a recollected receive");
								recollectedPermitted.remove(ID);
								recollectedThreads.remove(ID);
							   recollectedWithOldPending.remove(ID); // note: old may still be pending but doesn't matter anymore
								//System.out.println("remove " + ID + " from recollectedThreads");
								//System.out.println("recollectedThreads size is: " + recollectedThreads.size());

								if (recollectedThreads.size() == 0 && recollectedPermitted.size() == 0 &&
/*Q: check this for monitors */
 										recollectedMonitors.size() == 0 && recollectedMonitorCallers.size()==0) {
									//System.out.println("RTAllRecollected now true");
									RTAllRecollected = true;
								}
							}
							//System.out.println("Trace: ReceivePermitX granted for " + (choice-(4*(numberOfThreads+1))-1));
	 					}
						else if (choice <= 6*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestReceivePermitMS[choice-(5*(numberOfThreads+1))-1].accept();
							Integer ID = new Integer(choice-(5*(numberOfThreads+1))-1);
							if (!(recollectedMonitorCallers.contains(ID))) {
								//System.out.println("Receive by "+(ID.intValue())+" is not a recollected receive");
								requestReceivePermitMS[choice-(5*(numberOfThreads+1))-1].reply(new Integer(-1));
								recollectedMonitorCleared.remove(ID);
							}
							else {
								requestReceivePermitMS[choice-(5*(numberOfThreads+1))-1].reply(new Integer(-2));
								//System.out.println("Receive by "+ID+" is a recollected receive.");
/*Q3: move? yes, can't block since might not be completed */							
								//requestPermitMessage msg2 = (requestPermitMessage) monitorEnteredRT.acceptAndReply();
								//System.out.println("got ack");

								recollectedMonitorCallers.remove(ID);
/*Q4: move remaining: yes, wait until recollected receive completes */
//								recollectedMonitors.remove(new Integer(msg.getCalledID()));
//								System.out.println("remove " + msg.getCalledID() + " from recollectedMonitors");
//								System.out.println("recollectedMonitors size is: " + recollectedMonitors.size());
//								// move old sends for ID to cleared
//		 						///String monitorName = (msg.getChannelName()).substring(0,msg.getChannelName().indexOf(":"));
//								///if (recollectedMonitorNames.containsKey(monitorName)) {
//									///Integer monitorID = (Integer) recollectedMonitorNames.get(monitorName);
//									if (recollectedOldMonitorSends.containsKey(new Integer(msg.getCalledID()))) {
//										LinkedList monitorMessages = (LinkedList)recollectedOldMonitorSends.get(new Integer(msg.getCalledID()));
//										ListIterator i = (ListIterator) monitorMessages.listIterator();
//										//System.out.println("add old sends (if any) to cleared:");
//										while (i.hasNext()) {
//											Integer callerID = (Integer)i.next();
//											//System.out.println("add "+callerID+" to recollectedMonitorCleared");
//											recollectedMonitorCleared.add(callerID);
//										}
//										recollectedOldMonitorSends.remove(new Integer(msg.getCalledID()));
//									}
//								///}								
//									if (recollectedThreads.size() == 0 && recollectedPermitted.size() == 0 &&
//										recollectedMonitors.size() == 0 && recollectedMonitorCallers.size()==0) {
//									//System.out.println("RTAllRecollected now true");
//									RTAllRecollected = true;
/* Q4: end */
								//}
							}
							//System.out.println("Trace: ReceivePermitMS granted for " + (choice-(5*(numberOfThreads+1))-1));
	 					}
						else if (choice <= 7*(numberOfThreads+1)) {
							requestSelectPermit[choice-(6*(numberOfThreads+1))-1].accept();
							requestSelectPermit[choice-(6*(numberOfThreads+1))-1].reply(new Boolean(false));
							//System.out.println("Trace: SelectPermit granted for " + (choice-(6*(numberOfThreads+1))-1));
						}
						else if (choice <= 8*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestElseDelayPermit[choice-(7*(numberOfThreads+1))-1].acceptAndReply();
						}
						else if (choice <= 9*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSendExceptionPermit[choice-(8*(numberOfThreads+1))-1].acceptAndReply();
						}
						else if (choice <= 10*(numberOfThreads+1))  {
							msg = (requestPermitMessage)requestReceiveExceptionPermit[choice-(9*(numberOfThreads+1))-1].acceptAndReply();
						}
						else if (choice <= 11*(numberOfThreads+1)) {
							msg = (requestPermitMessage)requestSynchSendPermit[choice-(10*(numberOfThreads+1))-1].acceptAndReply();
							//System.out.println("Trace: SynchSendPermit granted for " + (choice-(8*(numberOfThreads+1))-1));
						}
						else if (choice == (11*(numberOfThreads+1))+1){
							msgReceived.acceptAndReply();
							//System.out.println("Trace: msgReceived");
						}
						else if (choice == (11*(numberOfThreads+1))+2){
							msgReceivedX.acceptAndReply();
							transitionCount++;
							//System.out.println("Trace: msgReceivedX");
						}
						else if (choice == (11*(numberOfThreads+1))+3){
						// make sure send arrives before recollected receive gets permission
							msg = (requestPermitMessage)sendArrivedRT.acceptAndReply();	
							// msg.getCalledID() is the caller, i.e., the sending thread for the new send
							//System.out.println("Trace: sendArrivedRT from " + msg.getCalledID());
							// check if this new send is for a recollected receive
							if (recollectedChannels.containsKey(msg.getChannelName())) {
								Integer ID = (Integer) recollectedChannels.get(msg.getChannelName());
								
			/* RHC: to do. get linked list and see if sender is in it. If so, then go ahead and add it.
			   Also, need to clean up the old Pending data structure? No, this is done by requestReceivePermitX
			*/
			
								ArrayList sendingThreads = (ArrayList)recollectedWithOldPending.get(ID);
								boolean found = false;
								if (sendingThreads != null) {
									int ret = sendingThreads.indexOf(new Integer(msg.getCalledID()));
									if (ret != -1)
										found = true;
									else
									   found = false;
								}
								else found = false;
								
								if (!found) {
									// can't remove thread from recollectedThreads since need it in there
									// to correctly set guard on next iteration (where send can be accepted)
									recollectedPermitted.add(ID);
									//System.out.println("Trace: sendArrivedRT from " + msg.getCalledID()+" is for "+ID.intValue() );
									// note that a new send was synchronized with a recollected receive
									// by removing the receiving thread. (All threads that remain at end
									// have no old or new sends)
									recollectedNoOldOrNewSends.remove(ID);
									//System.out.println("recollectedNoOldOrNewSends size is" + 	recollectedNoOldOrNewSends.size());
									recollectedChannels.remove(msg.getChannelName());
									// if two sends were allowed, only one will find channel name in recollectedChannels
									//System.out.println("add "+ID+" to recollectedPermitted and remove "+ID+" from recollectedChannels");								
								}
								else {
									//System.out.println(ID+" not added to recollectedPermitted since it has an old send pending");								
								}
							}
						}
						else if (choice == (11*(numberOfThreads+1))+4){
						// make sure send arrives before recollected receive gets permission
							msg = (requestPermitMessage)monitorEnteredRT.acceptAndReply();	
 							String monitorName = (msg.getChannelName()).substring(0,msg.getChannelName().indexOf(":"));
							// msg.getCalledID() is actually the caller, just for debugging
							//System.out.println("Trace: monitorEnteredRT from " + msg.getCalledID());

							// Note: old sends pending not checked for synchronous constructs
							if (recollectedMonitorNames.containsKey(monitorName)) {
								Integer ID = (Integer) recollectedMonitorNames.get(monitorName);
								//System.out.println("recollected event (monitor entry) for " + ID);
								recollectedNoOldOrNewSends.remove(ID);
								
								//System.out.println("recollectedNoOldOrNewSends size is" + 	recollectedNoOldOrNewSends.size());
								recollectedMonitorNames.remove(monitorName);
								recollectedMonitors.remove(ID);
							   recollectedWithOldPending.remove(ID); // note: old may still be pending but doesn't matter anymore
							   //System.out.println("recollectedWithOldPending size is" + 	recollectedWithOldPending.size());

								//System.out.println("remove " + ID + " from recollectedMonitors");
								//System.out.println("recollectedMonitors size is: " + recollectedMonitors.size());
								
								// move old sends for ID to cleared
								if (recollectedOldMonitorSends.containsKey(ID)) {
									LinkedList monitorMessages = (LinkedList)recollectedOldMonitorSends.get(ID);
									ListIterator i = (ListIterator) monitorMessages.listIterator();
									//System.out.println("add old sends (if any) to cleared:");
									while (i.hasNext()) {
										Integer callerID = (Integer)i.next();
										//System.out.println("add "+callerID+" to recollectedMonitorCleared");
										recollectedMonitorCleared.add(callerID);
									}
									recollectedOldMonitorSends.remove(ID);
								}
								
								if (recollectedThreads.size() == 0 && recollectedPermitted.size() == 0 &&
									recollectedMonitors.size() == 0 && recollectedMonitorCallers.size()==0) {
									//System.out.println("RTAllRecollected now true");
									RTAllRecollected = true;
								}
							}
							recollectedMonitorCallers.remove(new Integer(msg.getCalledID()));
						}
						else if (choice == (11*(numberOfThreads+1))+5){
							//System.out.println("outputTrace");
							outputTrace();
						}
						else if (choice == (11*(numberOfThreads+1))+6){
							//System.out.println("processSendReceive");
							processSendReceive();					
						}
						else if (choice == (11*(numberOfThreads+1))+7){
							appCompleted.accept();
							if (! ignoreSequence) {
								checkForUnacceptedSendReceive();
								//TestAndReplayCollection.outputObjectBasedTestAndReplaySequences();
								ArrayList sequence = TestAndReplayCollection.getSequence();

								if (checkTrace==CHECKTRACEON) {
									ArrayList appEvents = ApplicationEvents.getAppEvents();
									//System.out.println("*****");
									//for (int i=0; i<appEvents.size();i++)
									//	System.out.println((AppEvent)appEvents.get(i));
									//System.out.println("*****");
									boolean passed = checker.check(appEvents);
									if (!passed) {
										System.out.println("Trace check failed, terminating RT");
										System.exit(1);
									}
									if (checkTrace==CHECKTRACEON) {
						   			ApplicationEvents.resetAppEvents();
						   		}
								}

                        // rhc
								variant.setEvents(sequence);
								eventCount += sequence.size();

								generator.depositSeq (variant);
								
								//System.out.println(variant);
								//System.out.println("Did deposit:"+sequence.size());
// RHC: show races
			//srSeq variantWithRaces = generator.computeRaceSet(variant);
			//ArrayList sequenceWithRace = variantWithRaces.getEvents();
			//System.out.println("deposited sequence with races:");
			//for(int i=0; i< sequenceWithRace.size(); i++)
			//  System.out.println((srEvent)sequenceWithRace.get(i));
			//System.exit(1);
							}
							else {
								// System.out.println("appCompleted - ignoring sequence");
								// The partial sequence was not collected earlier since 
								// this execution may have deadlocked instead of completing. 
								// variant was set in the timeout alternative.
								
								if (checkTrace==CHECKTRACEON) {
									ArrayList appEvents = ApplicationEvents.getAppEvents();
									//System.out.println("*****");
									//for (int i=0; i<appEvents.size();i++)
									//	System.out.println((AppEvent)appEvents.get(i));
									//System.out.println("*****");
									boolean passed = checker.check(appEvents);
									if (!passed) {
										System.out.println("Trace check failed, terminating RT");
										System.exit(1);
									}
									if (checkTrace==CHECKTRACEON) {
						   			ApplicationEvents.resetAppEvents();
						   		}
								}
	
	                     // rhc
	                     //System.out.println("2********");
								//System.out.println(variant);

								generator.depositSeq (variant);
								
								//System.out.println(variant);
								//System.out.println("Did deposit");
							}

							RTTracing = false; // doing replay part
							RTAllRecollected = false;
							appCompleted.reply(new Boolean(ignoreSequence));
							ignoreSequence = false;

						   
  							recollectedChannels.clear();
							recollectedThreads.clear();
							recollectedPermitted.clear();
							recollectedNoOldOrNewSends.clear();
							recollectedMonitors.clear();
							recollectedMonitorNames.clear();
							recollectedMonitorCallers.clear();
							recollectedMonitorCleared.clear();
						   recollectedWithOldPending.clear();
						   recollectedOldMonitorSends.clear();
							//if (checkTrace==CHECKTRACEON) {
						   //	TDThread.resetAppEvents();
						   //}
							//return;
				 		   //traceit.getInstance().trace("end:" + java.util.Calendar.getInstance().getTime());
							//traceit.getInstance().close();
							break;
						
						}
						else { 
							timeout.accept();
							if (ignoreSequence) {
								// second timeout so deadlock while letting execution wind out, 
								// i.e., collecting duplicate sequence (see below)
								//System.out.println("second timeout");
								propertyParameters.DetectDeadlock detectDeadlock = 
									(propertyReader.getInstance().getDetectDeadlockProperty());
								if (detectDeadlock==DETECTIONON) {
									System.out.println("Error: Timeout waiting for application completion - probable deadlock/livelock.");
									deadlockWatchdog.notifyDeadlock();
								}
								else {
									System.out.println("Error: Timeout waiting for application completion - probable deadlock/livelock.");
									System.out.println("Run with -DdeadlockDetection=on to get more information about the deadlock.");
									System.exit(1);
								}	
							}
							else {
								if (recollectedThreads.size() > 0 || recollectedPermitted.size() > 0
								      || recollectedMonitors.size() > 0) {
									// recollected receive(s) with no new send
									if (recollectedWithOldPending.size()>0) {
										// there is a recollected event with a pending old send,
										// so it's not an application deadlock. Collect the partial sequence;
										// then let the execution finish and ignore the complete sequence
										
										// collect partial
										checkForUnacceptedSendReceive();
										//TestAndReplayCollection.outputObjectBasedTestAndReplaySequences();
										ArrayList sequence = TestAndReplayCollection.getSequence();

										// add the recollected events
										//Iterator i = recollectedWithOldPending.iterator();
/* RHC: changed Iterator */
										Set s = recollectedWithOldPending.keySet();
										Iterator i = s.iterator();
										while (i.hasNext()) {
											Integer ID = (Integer) i.next();
											srEvent e = null;
											if (recollectedMonitors.containsKey(ID))
												e = (srEvent) recollectedMonitors.get(ID);
											else if (recollectedThreads.containsKey(ID))
												e = (srEvent) recollectedThreads.get(ID);
											else {
												System.out.println("internal error: recollected receive not found");
												System.exit(1);
											}
											sequence.add(e);
										}
										// get copy of sequence as sequence will be changed as execution unwinds
										ArrayList partialSequence = new ArrayList();
										//System.out.println("1********");
										for (int k=0; k<sequence.size();k++) {
											//System.out.println((srEvent)sequence.get(k));
											srEvent e = (srEvent) sequence.get(k);
											if (e.getEventType().equals(UNACCEPTED_ASYNCH_SEND)) {
												srEvent c = (srEvent) e.clone();
												partialSequence.add(c);
											}
											else
												partialSequence.add(e);	
										}
										//System.out.println("1********");
										variant.setEvents(partialSequence);
										//System.out.println(variant);
										eventCount += partialSequence.size();

										//Don't deposit the partial sequence yet since we may yet detect a 
										// deadlock in this execution. The partial sequence is collected 
										// (above) when the application is completed.
										
										//generator.depositSeq (variant);
								
										//System.out.println(variant);
										//System.out.println("Did deposit");										
									
										ignoreSequence = true;
										recollectedChannels.clear(); recollectedThreads.clear();
										recollectedPermitted.clear(); recollectedNoOldOrNewSends.clear();
										recollectedMonitors.clear(); recollectedMonitorNames.clear();
										recollectedMonitorCallers.clear(); recollectedOldMonitorSends.clear();
										recollectedMonitorCleared.clear();
						   			recollectedWithOldPending.clear();
										//TDThread.stopAllTDThreads();
									}
									else { 
										// a recollected receive with no new sends and no old sends pending
										// so this is an application deadlock

										propertyParameters.DetectDeadlock detectDeadlock = 
												(propertyReader.getInstance().getDetectDeadlockProperty());
										if (detectDeadlock==DETECTIONON) {
											System.out.println("Error: Timeout waiting for application completion - probable deadlock/livelock.");
											deadlockWatchdog.notifyDeadlock();
										}
										else {
											//System.out.println("recollectedThreads size:"+recollectedThreads.size());
											//System.out.println("recollectedMonitors size:"+recollectedMonitors.size());
											//System.out.println("recollectedPermitted size:"+recollectedPermitted.size());
											//System.out.println("recollectedMonitorCallers size:"+recollectedMonitorCallers.size());
											//System.out.println("recollectedMonitorCleared size:"+recollectedMonitorCleared.size());
 											//System.out.println("recollectedWithOldPending size:"+recollectedWithOldPending.size());
											System.out.println("Error: Timeout waiting for application completion - probable deadlock/livelock.");
											System.out.println("Run with -DdeadlockDetection=on to get more information about the deadlock.");
											System.exit(1);
										}
																			
									}
								}
								else { 
									// no recollected receives so this is an application deadlock
									propertyParameters.DetectDeadlock detectDeadlock = 
											(propertyReader.getInstance().getDetectDeadlockProperty());
									if (detectDeadlock==DETECTIONON) {
										System.out.println("Error: Timeout waiting for application completion - probable deadlock/livelock.");
										deadlockWatchdog.notifyDeadlock();
									}
									else {
										System.out.println("Error: Timeout waiting for application completion - probable deadlock/livelock.");
										System.out.println("Run with -DdeadlockDetection=on to get more information about the deadlock.");
										System.exit(1);
									}
								}
							} // don't ignore
						} // do timeout
					} // try
					catch (IOException e) {
	     				System.err.println("Error while writing tracing: " + e.toString());
   	  				System.exit(1); 
					}
				} // inner-while of trace mode
			  } // while of RT mode (never broken); use same controller for entire RT
			 } // try of if mode is RT
			 catch (Exception e) {
			 	e.printStackTrace(); 
				System.out.println("Reachability testing failed. The failure is usually caused   ");
				System.out.println("   by a program that accesses a shared variable outside of   ");
				System.out.println("   a critical section. To verify that this is the cause, use ");
				System.out.println("   the Shared Variable classes in the library. These classes,");
				System.out.println("   as described in the User Manual, allow reachability       ");
				System.out.println("   testing to detect data races.                             ");

			 	System.exit(0);}
			} // end RT
		} catch (InterruptedException r) {}
	} // end RUN

}
