package reachability;
import java.util.*;

public class selectablePort extends channel 
	implements eventTypeParameters, traceMessageTypeParameters,
		propertyParameters, selectable {

	private debugMessage message; //  = new debugMessage();
	private Object arrivalLock = new Object();
	private final Object sending = new Object();
   private final Object receiving = new Object();
	private final binarySemaphore sent = new binarySemaphore(0,NONE);
	private final binarySemaphore received = new binarySemaphore(0,NONE);
	private Thread receiver = null;
	private final Object receiverLock = new Object();
	private final Object ownerLock = new Object();

	private boolean open = false;
	private int ready = 0;
	private IselectiveWait inchoice = null;
	private boolean guard_ = true;
	private static long timeStamp = 0;
	private Vector arrivals = new Vector();

   private TDThread owner = null;
	private int exceptionOwnerID = -1;
	private int unacceptedOwnerID = -2; 					// values used if object-based control strategy used
	private final int exceptionOwnerVersionNumber = -1;
	private final int unacceptedOwnerVersionNumber = -2;
	
	//private Labeler sendLabeler = null; 
	//private Labeler receiveLabeler = null;
 	LTSGenerator LTSgen;
 	
	private ArrayList defaultOpenList; // for send events

 	private final int	delayAmount = 750;
 	
   public selectablePort() {
  		if (mode == RT) {
			OpenEvent e = new OpenEvent(channelName,-1);
			defaultOpenList = new ArrayList(); defaultOpenList.add(e);
		}
     	if (generateLTS==LTSON)
	   	LTSgen = LTSGenerator.getInstance();
	}
   public selectablePort(String portName) {
   	super(portName);
		if (mode == RT) {
			OpenEvent e = new OpenEvent(channelName,-1);
			defaultOpenList = new ArrayList(); defaultOpenList.add(e);
		}
   	if (generateLTS==LTSON)
	   	LTSgen = LTSGenerator.getInstance();
  	}
   	
	public void setOwner(TDThread owner) { 
		if (strategy.equals(OBJECT)) {// ignore if object-based control strategy selected
			return;
		}
		synchronized (ownerLock) {
			this.owner = owner;
			this.control = owner.getController();	
			exceptionOwnerID = owner.getID();
			unacceptedOwnerID = owner.getID();		
			ownerLock.notifyAll();
		}
	}

	private static synchronized long getTimeStamp() {
		return(++timeStamp);
	}


	public final void send(Object sentMsg) {
		synchronized (ownerLock) {
			if (mode != NONE && strategy.equals(THREAD) && owner==null) {
				try {ownerLock.wait();} catch (InterruptedException ex) {}
			}
		}
		boolean isOld = false;
		if (randomDelay == ON && mode == TRACE) {
			try {
				int r = (int)(Math.random()*delayAmount);
				Thread.sleep(r); // (int)(Math.random()*delayAmount));	// default delayAmount is 750
			} catch (InterruptedException e) {}
		}
		if (mode == TRACE) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),unacceptedOwnerID,((innerThread)Thread.currentThread()).getVersionNumber(),
					strategy.equals(THREAD) ? owner.getVersionNumber() : unacceptedOwnerVersionNumber,channelName,getVersionNumber(),UNACCEPTED_SYNCH_SEND,
					((innerThread)Thread.currentThread()).getVectorTS());
			traceMessage t = new traceMessage(ADD_UNACCEPTED_SEND_TYPE,m);
			control.traceSendReceive(t);
      }
		else if (mode == REPLAY || mode == TEST) {
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" requests sendPermit");
			control.requestSendPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		else if (mode == RT) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
	      //System.out.println(((innerThread)Thread.currentThread()).getID()+" calling requestSendPermitX");
			isOld = control.requestSendPermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
	      //System.out.println(((innerThread)Thread.currentThread()).getID()+" got requestSendPermitX");

			control.msgReceived();
			
		   //int callerForReceiveEvent = -1;
		   // callerForReceiveEvent is not used here
			// Note: request made by caller/current thread; version number ignored since
			// really can't say what version number of "monitor thread" is here.
			// The send-->receive receive<--send is just a model for race analysis, not
			// how replay is implemented.
			/* sender will request too. */

/* RHC: If this is expected to throw an exception, need to let it throw before */
/* making this request, else the exception won't be allowed to occur? */
/* Or stop RT when this exception is thrown? */

			//System.out.println("calling requestSynchSendPermit");
			control.requestSynchSendPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());  					
			//System.out.println("got requestSynchSendPermit");
	   }
		if (sentMsg != null) 
			setArrival();	// note arrival
		synchronized (sending) {
			if (sentMsg == null) {
				if (mode == TRACE) {
					// if thread-based  ownerversion = getAndInc else ownerVersion = -1
					srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),exceptionOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber,channelName,getAndIncVersionNumber(),SEND_EXCEPTION,
							((innerThread)Thread.currentThread()).getVectorTS());
					traceMessage t = new traceMessage(SEND_EXCEPTION_TYPE,m);
					control.traceMsg(t);
					t = new traceMessage(REMOVE_UNACCEPTED_SEND_TYPE,m);
					control.traceSendReceive(t);
      		}
				if (mode == TEST) {
					// inc owner version number 'cause we inced it in trace
					control.requestSendExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,
						((innerThread)Thread.currentThread()).getAndIncVersionNumber(),strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber);
				}
				if	 (mode == REPLAY || mode == TEST) {
					control.msgReceived();
				}
				if (mode == RT) {
					control.requestSendExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,
						((innerThread)Thread.currentThread()).getAndIncVersionNumber(),strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber);				
					// don't inc owner version number 'cause we jst inced it in trace and doing both TEST and TRACE in RT
					srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),exceptionOwnerID,((innerThread)Thread.currentThread()).getVersionNumber(),
							strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber,channelName,getAndIncVersionNumber(),SEND_EXCEPTION,
							((innerThread)Thread.currentThread()).getVectorTS(),"exception",defaultOpenList,SEND_EXCEPTION);
					traceMessage t = new traceMessage(SEND_EXCEPTION_TYPE,m);
					control.traceMsg(t);
					t = new traceMessage(REMOVE_UNACCEPTED_SEND_TYPE,m);
					control.traceSendReceive(t);
					
					control.msgReceived();
				}
				throw new NullPointerException("Null message passed to send()");
			}
			message = new debugMessage();
         message.setMsg(sentMsg);
         if (mode == TRACE || mode == TEST || mode == RT) {
				message.setCaller(((innerThread)Thread.currentThread()).getID());
				message.setVersionNumber(((innerThread)Thread.currentThread()).getAndIncVersionNumber());
				message.setVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
			}
			if (mode == RT) {
				String label = "unaccepted_send";
			
				if (SymmetryReduce == SYMMETRYREDUCTIONON) {
					StringBuffer B = new StringBuffer();
					Throwable ex = new Throwable();
					StackTraceElement[] stackElements = ex.getStackTrace();
					for (int i=stackElements.length-1; i>=0; i--)
						B.append(stackElements[i]);
					label = B.toString();
				}
			
				srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),unacceptedOwnerID,((innerThread)Thread.currentThread()).getVersionNumber()-1,
								//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getVersionNumber(),UNACCEPTED_SYNCH_SEND,
								// use -1 for receiver's version number
								strategy.equals(THREAD) ? owner.getVersionNumber() : unacceptedOwnerVersionNumber,channelName,getVersionNumber(),UNACCEPTED_ASYNCH_SEND,
								((innerThread)Thread.currentThread()).getVectorTS(),label,defaultOpenList,SYNCH_SEND);
				traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);
		      //System.out.println(((innerThread)Thread.currentThread()).getID()+" calling traceMessage");

				control.traceMsg(t);

				//control.msgReceived();			

				if (!isOld) {
					//System.out.println(((innerThread)Thread.currentThread()).getID()+" calling sendArrivedRT");
					control.sendArrivedRT(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber()-1);
				}
				//else System.out.println(((innerThread)Thread.currentThread()).getID()+" not calling sendArrivedRT");
			}
			signal();
			received.P();
			if (mode == TRACE || mode == RT) {
			 	((innerThread)Thread.currentThread()).updateVectorTS(message.getVectorTS());
			}
		}
	}

	public final void send() {
		synchronized (ownerLock) {
			if (mode != NONE && strategy.equals(THREAD) && owner==null) {
				try {ownerLock.wait();} catch (InterruptedException ex) {}
			}
		}
		boolean isOld = false;
		if (randomDelay == ON && mode == TRACE) {
			try {
				int r = (int)(Math.random()*delayAmount);
				Thread.sleep(r); // (int)(Math.random()*delayAmount));	// default delayAmount is 750
			} catch (InterruptedException e) {}
		}
		if (mode == TRACE) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),unacceptedOwnerID,((innerThread)Thread.currentThread()).getVersionNumber(),
						strategy.equals(THREAD) ? owner.getVersionNumber() : unacceptedOwnerVersionNumber,channelName,getVersionNumber(),UNACCEPTED_SYNCH_SEND,
						((innerThread)Thread.currentThread()).getVectorTS());
			traceMessage t = new traceMessage(ADD_UNACCEPTED_SEND_TYPE,m);
			control.traceSendReceive(t);
      }
		else 	if (mode == REPLAY || mode == TEST)
			control.requestSendPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		else if (mode == RT) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
  	      //System.out.println(((innerThread)Thread.currentThread()).getID()+" calling requestSendPermitX");
			isOld = control.requestSendPermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
	      //System.out.println(((innerThread)Thread.currentThread()).getID()+" got requestSendPermitX");

			control.msgReceived();
				
		   //int callerForReceiveEvent = -1;
		   // callerForReceiveEvent is not used here
			// Note: request made by caller/current thread; version number ignored since
			// really can't say what version number of "monitor thread" is here.
			// The send-->receive receive<--send is just a model for race analysis, not
			// how replay is implemented.
			/* sender will request too. */
				
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" calling requestSynchSendPermit");
			control.requestSynchSendPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" got requestSynchSendPermit");
	   }
		setArrival();
		synchronized (sending) {
			message = new debugMessage();
         message.setMsg(new Object());
         if (mode == TRACE || mode == TEST || mode == RT) {
				message.setCaller(((innerThread)Thread.currentThread()).getID());
				message.setVersionNumber(((innerThread)Thread.currentThread()).getAndIncVersionNumber());
				message.setVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
			}
			if (mode == RT) {
				String label = "unaccepted_send";
			
				if (SymmetryReduce == SYMMETRYREDUCTIONON) {
					StringBuffer B = new StringBuffer();
					Throwable ex = new Throwable();
					StackTraceElement[] stackElements = ex.getStackTrace();
					for (int i=stackElements.length-1; i>=0; i--)
						B.append(stackElements[i]);
					label = B.toString();
				}
				srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),unacceptedOwnerID,((innerThread)Thread.currentThread()).getVersionNumber()-1,
								//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getVersionNumber(),UNACCEPTED_SYNCH_SEND,
								// use -1 for receiver's version number
								strategy.equals(THREAD) ? owner.getVersionNumber() : unacceptedOwnerVersionNumber,channelName,getVersionNumber(),UNACCEPTED_ASYNCH_SEND,
								((innerThread)Thread.currentThread()).getVectorTS(),label,defaultOpenList,SYNCH_SEND);
				traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);
				control.traceMsg(t);
				
				//control.msgReceived();
			
				if (!isOld) {
					//System.out.println(((innerThread)Thread.currentThread()).getID()+" calling sendArrivedRT");
					control.sendArrivedRT(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber()-1);
				}
				//else System.out.println(((innerThread)Thread.currentThread()).getID()+" not calling sendArrivedRT");
			}
			signal();
			received.P();
			if (mode == TRACE || mode == RT) {
			 	((innerThread)Thread.currentThread()).updateVectorTS(message.getVectorTS());
			}
		}
	}

	synchronized void signal() {
		if (inchoice == null) {
			sent.V(); // if (open) notifyAll();
		} 
		else {
			synchronized (inchoice) {
				sent.V();	
				if (open)    // is this is an entry that choice is waiting for?
					inchoice.notify(); 
			}
		}
	}


	public final Object receive() {
		synchronized (ownerLock) {
			if (mode != NONE && strategy.equals(THREAD) && owner==null) {
				try {ownerLock.wait();} catch (InterruptedException ex) {}
			}
		}
		if (randomDelay == ON && mode == TRACE) {
			try {
				int r = (int)(Math.random()*delayAmount);
				Thread.sleep(r); // (int)(Math.random()*delayAmount));	// default delayAmount is 750
			} catch (InterruptedException e) {}
		}
		if (mode == TRACE) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
			srEvent m = new srEvent(-2,((innerThread)Thread.currentThread()).getID(),-2,((innerThread)Thread.currentThread()).getVersionNumber(),
								channelName,getVersionNumber(),UNACCEPTED_RECEIVE,
								((innerThread)Thread.currentThread()).getVectorTS());
			traceMessage t = new traceMessage(ADD_UNACCEPTED_RECEIVE_TYPE,m);
			control.traceSendReceive(t);
      } 
      else if (mode == REPLAY || mode == TEST) {
			control.requestReceivePermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		else if (mode == RT) { 
			//System.out.println("Thread " + ((innerThread)Thread.currentThread()).getID() + " calling requestReceivePermitX");
			control.requestReceivePermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
			//System.out.println("Thread " + ((innerThread)Thread.currentThread()).getID() + " got requestReceivePermitX");

	      ((innerThread)Thread.currentThread()).updateIntegerTS();
			srEvent m = new srEvent(-2,((innerThread)Thread.currentThread()).getID(),-2,((innerThread)Thread.currentThread()).getVersionNumber(),
								channelName,getVersionNumber(),UNACCEPTED_RECEIVE,
								((innerThread)Thread.currentThread()).getVectorTS());
			traceMessage t = new traceMessage(ADD_UNACCEPTED_RECEIVE_TYPE,m);
			control.traceSendReceive(t);
		}

      synchronized (receiving) {
		// Note: This synchronized block has been added so we
		// can replay possible exceptions. There is supposed to be
		// only one sender on a link so synchronized is not required
		// when not replaying or testing
			if (receiver == null)					// save the first Thread to call receive
				synchronized(receiverLock) {
					// guard against a context switch after if, allowing two threads to see (receiver == null)
					if (receiver == null)						
						receiver = Thread.currentThread();
				}
			if (Thread.currentThread() != receiver) {
				if (mode == TRACE) {
					srEvent m = new srEvent(-1,((innerThread)Thread.currentThread()).getID(),-1,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
										channelName,getAndIncVersionNumber(),RECEIVE_EXCEPTION,
									  	((innerThread)Thread.currentThread()).getVectorTS());
					traceMessage t = new traceMessage(RECEIVE_EXCEPTION_TYPE,m);
					control.traceMsg(t);
					t = new traceMessage(REMOVE_UNACCEPTED_RECEIVE_TYPE,m);
					control.traceSendReceive(t);
  			  	}
				else if (mode == TEST) {
					//System.out.println("calling control.requestexceptionpermit");
					control.requestReceiveExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getAndIncVersionNumber());
				}
				if (mode == REPLAY || mode == TEST)
					control.msgReceived();
				if (mode == RT) {
					control.requestReceiveExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getAndIncVersionNumber());
					// don't inc owner version number 'cause we jst inced it in trace and doing both TEST and TRACE in RT
					srEvent m = new srEvent(-1,((innerThread)Thread.currentThread()).getID(),-1,((innerThread)Thread.currentThread()).getVersionNumber(),
										channelName,getAndIncVersionNumber(),RECEIVE_EXCEPTION,
									  	((innerThread)Thread.currentThread()).getVectorTS());
					traceMessage t = new traceMessage(RECEIVE_EXCEPTION_TYPE,m);
					control.traceMsg(t);
					t = new traceMessage(REMOVE_UNACCEPTED_RECEIVE_TYPE,m);
					control.traceSendReceive(t);				
					control.msgReceived();				
				}
				throw new InvalidPortUsage("Attempted to use port with multiple receivers");
			}

			debugMessage receivedMessage = null;
			sent.P();
			receivedMessage = message;
			clearArrival();

			if (mode == TRACE) {
				((innerThread)Thread.currentThread()).updateVectorTS(receivedMessage.getVectorTS());
				srEvent m = new srEvent(receivedMessage.getCaller(),((innerThread)Thread.currentThread()).getID(),receivedMessage.getVersionNumber(),
									((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
									channelName,getAndIncVersionNumber(),SR_SYNCHRONIZATION,
							     	((innerThread)Thread.currentThread()).getVectorTS());
				traceMessage t = new traceMessage(SR_PORT_EVENT_TYPE,m);
				control.traceMsg(t);
				t = new traceMessage(REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE,m);
				control.traceSendReceive(t);
     		}
			else if (mode == REPLAY || mode == TEST) {
				((innerThread)Thread.currentThread()).getAndIncVersionNumber();
				control.msgReceived();
			}
			else if (mode == RT) {
				ArrayList openList;
				if (inchoice == null) {
					openList = defaultOpenList;
				}
				else {
					openList = inchoice.getOpenList();  // returns clone of selective waits open list
					inchoice.resetOpenList(); 
				}
				((innerThread)Thread.currentThread()).updateVectorTS(receivedMessage.getVectorTS());
				srEvent m = new srEvent(receivedMessage.getCaller(),((innerThread)Thread.currentThread()).getID(),receivedMessage.getVersionNumber(),
									((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
									channelName,getAndIncVersionNumber(),ASYNCH_RECEIVE,
							     	((innerThread)Thread.currentThread()).getVectorTS(),
							     	"receive",openList,SYNCH_RECEIVE);
				traceMessage t = new traceMessage(SR_PORT_EVENT_TYPE,m);
				control.traceMsg(t);
				t = new traceMessage(REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE,m);
				control.traceSendReceive(t);	
				
				control.msgReceived();
			}
			if (mode == TRACE || mode == RT) {
				message.setVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
			}
			received.V();
			return receivedMessage.getMsg();
		}
	}


	public void setSelect(IselectiveWait s) {
		inchoice = s;
	}

	public long getOldestArrival() {
	// only called from choose() when ready>0, i.e., arrivals.size()>0
		synchronized(arrivalLock) {
			return (((Long)arrivals.firstElement()).longValue());
		}
	}

	public int count() { return ready; }

    public boolean testReady() {
        return ready>0;
    }
    
    public void setArrival() {
    		synchronized(arrivalLock) {
    			++ready;
    			arrivals.addElement(new Long(getTimeStamp()));	
    		}
    }
    
    public void clearArrival() {
    		synchronized(arrivalLock) {
    			--ready;
    			arrivals.removeElementAt(0);	
    		}
    }  

    public void setOpen() {
        open=true;
    }

    public void clearOpen() {
         open=false;
    }

    public void guard(boolean g) {
        guard_=g;
    }

    public boolean testGuard(){
        return guard_;
    }
    
   // used by selectableSynchChannelC
  	public void setID(int ID) {}
	public int getID() {return -1;}
}
