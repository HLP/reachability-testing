package reachability;
import java.util.*;

public final class asynchPort extends channel 
 implements eventTypeParameters, traceMessageTypeParameters,
 propertyParameters  {


	private final int capacity = 100;
	TreeMap messagesX = new TreeMap();
	private countingSemaphore messageAvailable = new countingSemaphore(0,true);
	private countingSemaphore slotAvailable = new countingSemaphore(capacity,true);
	private binarySemaphore senderMutex = new binarySemaphore(1,NONE);
	private binarySemaphore receiverMutex = new binarySemaphore(1,NONE);
	//private Thread sender = null;
	private Thread receiver = null;
	//private final Object senderLock = new Object();
	private final Object receiverLock = new Object();
	private final Object ownerLock = new Object();

   private TDThread owner = null;
	private int sendOwnerID = -1;
	private int exceptionOwnerID = -1;
	//private int unacceptedOwnerID = -2; 					// values used if object-based control strategy used
	private final int sendOwnerVersionNumber = -1;
	private final int exceptionOwnerVersionNumber = -1;
	//private final int unacceptedOwnerVersionNumber = -2;
	
	private sendLabeler sendLabeler = null; 
	private receiveLabeler receiveLabeler = null;
 	LTSGenerator LTSgen;
 
 	private final int	delayAmount = 750;
 	
	private ArrayList openList = null; // port name (representing always open port)

   public asynchPort(sendLabeler s, receiveLabeler r) {
   	this.sendLabeler = s;
   	this.receiveLabeler = r;
		if (mode == RT) {
			OpenEvent e = new OpenEvent(channelName,-1);
			openList = new ArrayList(); openList.add(e);  // (channelName);
		}
   }
   
   public asynchPort(String portName, sendLabeler s, receiveLabeler r) {
   	super(portName);
   	this.sendLabeler = s;
   	this.receiveLabeler = r;  
   	if (generateLTS==LTSON)
	   	LTSgen = LTSGenerator.getInstance();
		if (mode == RT) {
			OpenEvent e = new OpenEvent(channelName,-1);
			openList = new ArrayList(); openList.add(e);  // (channelName);
		}
   }
 	
/*
 	private String getState() {
 		String state = new String("");
		Iterator p = buffer.iterator();
		while (p.hasNext()) {
			debugMessage receivedMessage = (debugMessage)p.next();
			String label = new String(receiveLabeler.getLabel(receivedMessage.getMsg()));
			state = state + label;
		}
		return state;
	}
*/
 	
   public asynchPort() {
   	if (mode == RT) {
			OpenEvent e = new OpenEvent(channelName,-1);
			openList = new ArrayList(); openList.add(e);  // (channelName);
		}
   }
   public asynchPort(String portName) {
   	super(portName);
   	if (mode == RT) {
			OpenEvent e = new OpenEvent(channelName,-1);
			openList = new ArrayList(); openList.add(e);  // (channelName);
		}
   }

	public void setOwner(TDThread owner) { 
		if (mode != RT && strategy.equals(OBJECT)) // ignore if object-based control strategy selected
			return;	
		if (mode == RT) {
			synchronized (ownerLock) {
				this.owner = owner;
				if (strategy.equals(THREAD))
					this.control = owner.getController();	
				sendOwnerID = owner.getID();
				exceptionOwnerID = owner.getID();
				//unacceptedOwnerID = owner.getID();
				ownerLock.notifyAll();
				return;
			}
		}
		synchronized (ownerLock) {
			this.owner = owner;
			if (!(mode==NONE)) {
				this.control = owner.getController();	
				sendOwnerID = owner.getID();
				exceptionOwnerID = owner.getID();
				//unacceptedOwnerID = owner.getID();
			}
			ownerLock.notifyAll();
		}	
	}
	

		public final void send(Object sentMsg) {
		
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
		if (mode == TRACE || mode == RT) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
	   }
	
		boolean isOld = false;
		if (mode == REPLAY || mode == TEST) {
			control.requestSendPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		else if (mode == RT) {
			isOld = control.requestSendPermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		slotAvailable.P();
		senderMutex.P();
		if (sentMsg == null) {
			if (mode == TRACE) {
				srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),exceptionOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber,channelName,getAndIncVersionNumber(),SEND_EXCEPTION,
							((innerThread)Thread.currentThread()).getVectorTS(),"exception",openList,SEND_EXCEPTION);
				traceMessage t = new traceMessage(SEND_EXCEPTION_TYPE,m);
				control.traceMsg(t);
     		}
			if (mode == TEST)
				// inc owner version number 'cause we inced it in trace
				control.requestSendExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,
					((innerThread)Thread.currentThread()).getAndIncVersionNumber(),strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber);
			if	 (mode == REPLAY || mode == TEST)
				control.msgReceived();
			if (mode == RT) { // TEST (req permit) then TRACE then TEST (release permit)
				control.requestSendExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,
					((innerThread)Thread.currentThread()).getAndIncVersionNumber(),strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber);
				// don't inc owner version number 'cause we jst inced it in trace and doing both TEST and TRACE in RT
				srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),exceptionOwnerID,((innerThread)Thread.currentThread()).getVersionNumber(),
							strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber,channelName,getAndIncVersionNumber(),SEND_EXCEPTION,
							((innerThread)Thread.currentThread()).getVectorTS(),"exception",openList,SEND_EXCEPTION);
				traceMessage t = new traceMessage(SEND_EXCEPTION_TYPE,m);
				control.traceMsg(t);
				control.msgReceived();
			}
			
			slotAvailable.V();
			senderMutex.V();
			throw new NullPointerException("Null message passed to send()");
		}

		debugMessage message = new debugMessage();
      message.setMsg(sentMsg);

      String label = new String("");
      if (generateLTS==LTSON)
         label = new String("'"+sendLabeler.getLabel(sentMsg)+"[S]");  
         
		//label = new String("'"+sendLabeler.getLabel(sentMsg)+"[S]"); 

      if (mode == TRACE || mode == TEST || mode == RT) {
			message.setCaller(((innerThread)Thread.currentThread()).getID());
			message.setVersionNumber(((innerThread)Thread.currentThread()).getVersionNumber());
			message.setVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
			message.setLabel(label);
			message.setIsOld(isOld); // true if message sent by old send during RT
		}
		
	synchronized(messagesX) {
		{
      	if (generateLTS==LTSON) {

				//label sender's send event
	   	   String state = ((innerThread)Thread.currentThread()).getThreadState();
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
					// Note: for ( int i=0; i<stackElements.length; i++ ) {
					//	StackTraceElement e = stackElements[i];
				   //	PC.append(e.getClassName());PC.append(":");PC.append(e.getFileName());
				   //	PC.append(":");PC.append(e.getMethodName());PC.append(":");
				   //	PC.append(e.getLineNumber()); PC.append(";");
				   //	//System.out.println(PC);
			   	//}
				String PC = B.toString();
	      	programTransition t = new programTransition(((innerThread)Thread.currentThread()).getName(),PC+":"+state,label);
			   LTSgen.depositTransition(t);
		   
				//label Medium's events: Medium = x.'x.Medium
				String portLabel = new String(sendLabeler.getLabel(sentMsg));
	   		//state = getState();
   			t = new programTransition((((innerThread)Thread.currentThread()).getName()+"-"+channelName+"-Medium"),"0","M",portLabel);
		   	LTSgen.depositTransition(t); 

		  	}
		}

		
		Integer caller = new Integer(((innerThread)Thread.currentThread()).getID());
		if (!(messagesX.containsKey(caller))) { // put sender in message map
		   //System.out.println("new linked list for " + caller);
			messagesX.put(caller,new LinkedList());
		}

		LinkedList callerMessages = (LinkedList)messagesX.get(caller);
		callerMessages.addLast(message); // add sender's message to end of list (FCFS)
	
		
		if (mode == TRACE) {
			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),sendOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							// use -1 for receiver's version number
							sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							((innerThread)Thread.currentThread()).getVectorTS(),label,openList,UNACCEPTED_ASYNCH_SEND);
			traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);
			control.traceMsg(t);
  		}
		else if (mode == REPLAY || mode == TEST) {
			((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			control.msgReceived();
		}
		else if (mode == RT) { /// do TEST (release permit) and TRACE
			//((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			
			if (SymmetryReduce == SYMMETRYREDUCTIONON) {
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				label = B.toString();
			}

			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),sendOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							// use -1 for receiver's version number
							sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							((innerThread)Thread.currentThread()).getVectorTS(),label,openList,UNACCEPTED_ASYNCH_SEND);
							
			// these are accepted immed. after getting the sendPermitX so send arrives before
			// recollected receive, (if it is a recollected receive that received this. 
			traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);

			control.traceMsg(t);

			control.msgReceived();
			if (!isOld) {
				control.sendArrivedRT(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
			}
		}
	}

		senderMutex.V();
		messageAvailable.V();

	}

/***********/

		public final void send(Object sentMsg, int PC) {
				
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
		if (mode == TRACE || mode == RT) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
	   }
	
		boolean isOld = false;
		if (mode == REPLAY || mode == TEST) {
			control.requestSendPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		else if (mode == RT) {
			isOld = control.requestSendPermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		slotAvailable.P();
		senderMutex.P();
		if (sentMsg == null) {
			if (mode == TRACE) {
				srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),exceptionOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber,channelName,getAndIncVersionNumber(),SEND_EXCEPTION,
							((innerThread)Thread.currentThread()).getVectorTS(),"exception",openList,SEND_EXCEPTION);
				traceMessage t = new traceMessage(SEND_EXCEPTION_TYPE,m);
				control.traceMsg(t);
     		}
			if (mode == TEST)
				// inc owner version number 'cause we inced it in trace
				control.requestSendExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,
					((innerThread)Thread.currentThread()).getAndIncVersionNumber(),strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber);
			if	 (mode == REPLAY || mode == TEST)
				control.msgReceived();
			if (mode == RT) { // TEST (req permit) then TRACE then TEST (release permit)
				control.requestSendExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,
					((innerThread)Thread.currentThread()).getAndIncVersionNumber(),strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber);
				// don't inc owner version number 'cause we jst inced it in trace and doing both TEST and TRACE in RT
				srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),exceptionOwnerID,((innerThread)Thread.currentThread()).getVersionNumber(),
							strategy.equals(THREAD) ? owner.getVersionNumber() : exceptionOwnerVersionNumber,channelName,getAndIncVersionNumber(),SEND_EXCEPTION,
							((innerThread)Thread.currentThread()).getVectorTS(),"exception",openList,SEND_EXCEPTION);
				traceMessage t = new traceMessage(SEND_EXCEPTION_TYPE,m);
				control.traceMsg(t);
				control.msgReceived();
			}
			
			slotAvailable.V();
			senderMutex.V();
			throw new NullPointerException("Null message passed to send()");
		}

		debugMessage message = new debugMessage();
      message.setMsg(sentMsg);

      String label = new String("");
      if (generateLTS==LTSON)
         label = new String("'"+sendLabeler.getLabel(sentMsg)+"[S]");  
         
		//label = new String("'"+sendLabeler.getLabel(sentMsg)+"[S]"); 

      if (mode == TRACE || mode == TEST || mode == RT) {
			message.setCaller(((innerThread)Thread.currentThread()).getID());
			message.setVersionNumber(((innerThread)Thread.currentThread()).getVersionNumber());
			message.setVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
			message.setLabel(label);
			message.setIsOld(isOld); // true if message sent by old send during RT
		}
		
	synchronized(messagesX) {
		{
      	if (generateLTS==LTSON) {

				//label sender's send event
	   	   String state = ((innerThread)Thread.currentThread()).getThreadState();
	      	programTransition t = new programTransition(((innerThread)Thread.currentThread()).getName(),PC+":"+state,label);
			   LTSgen.depositTransition(t);
		   
				//label Medium's events: Medium = x.'x.Medium
				String portLabel = new String(sendLabeler.getLabel(sentMsg));
	   		//state = getState();
   			t = new programTransition((((innerThread)Thread.currentThread()).getName()+"-"+channelName+"-Medium"),"0","M",portLabel);
		   	LTSgen.depositTransition(t); 

		  	}
		}

		Integer caller = new Integer(((innerThread)Thread.currentThread()).getID());
		if (!(messagesX.containsKey(caller))) { // put sender in message map
			messagesX.put(caller,new LinkedList());
		}
		LinkedList callerMessages = (LinkedList)messagesX.get(caller);
		callerMessages.addLast(message); // add sender's message to end of list (FCFS)	
		
		if (mode == TRACE) {
			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),sendOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							// use -1 for receiver's version number
							sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							((innerThread)Thread.currentThread()).getVectorTS(),label,openList,UNACCEPTED_ASYNCH_SEND);
			traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);
			control.traceMsg(t);
  		}
		else if (mode == REPLAY || mode == TEST) {
			((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			control.msgReceived();
		}
		else if (mode == RT) { /// do TEST (release permit) and TRACE
		
			if (SymmetryReduce == SYMMETRYREDUCTIONON) {
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				label = B.toString();
			}

			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),sendOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							// use -1 for receiver's version number
							sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							((innerThread)Thread.currentThread()).getVectorTS(),label,openList,UNACCEPTED_ASYNCH_SEND);
							
			// these are accepted immed. after getting the sendPermitX so send arrives before
			// recollected receive, (if it is a recollected receive that received this. 
			traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);

			control.traceMsg(t);

			control.msgReceived();
			if (!isOld) {
				//System.out.println(((innerThread)Thread.currentThread()).getID()+" calling sendArrivedRT");
				control.sendArrivedRT(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
			}

		}
	}

		senderMutex.V();
		messageAvailable.V();

	}

		public final void send() {
				
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
		if (mode == TRACE || mode == RT) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
	   }
	
		boolean isOld = false;
		if (mode == REPLAY || mode == TEST) {
			control.requestSendPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		else if (mode == RT) {
			isOld = control.requestSendPermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		slotAvailable.P();
		senderMutex.P();

		debugMessage message = new debugMessage();
      message.setMsg(new Object());

      String label = new String("");
      if (generateLTS==LTSON)
         label = new String("'"+sendLabeler.getLabel()+"[S]");  
         
		//label = new String("'"+sendLabeler.getLabel()+"[S]"); 

      if (mode == TRACE || mode == TEST || mode == RT) {
			message.setCaller(((innerThread)Thread.currentThread()).getID());
			message.setVersionNumber(((innerThread)Thread.currentThread()).getVersionNumber());
			message.setVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
			message.setLabel(label);
			message.setIsOld(isOld); // true if message sent by old send during RT
		}
		
		synchronized(messagesX) {
		{
      	if (generateLTS==LTSON) {
 
				//label sender's send event
	   	   String state = ((innerThread)Thread.currentThread()).getThreadState();

				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				String PC = B.toString();
	      	programTransition t = new programTransition(((innerThread)Thread.currentThread()).getName(),PC+":"+state,label);
			   LTSgen.depositTransition(t);
		   
				//label Medium's events: Medium = x.'x.Medium
				String portLabel = new String(sendLabeler.getLabel());
	   		//state = getState();
   			t = new programTransition((((innerThread)Thread.currentThread()).getName()+"-"+channelName+"-Medium"),"0","M",portLabel);
		   	LTSgen.depositTransition(t); 

		  	}
		}
		
		Integer caller = new Integer(((innerThread)Thread.currentThread()).getID());
		if (!(messagesX.containsKey(caller))) { // put sender in message map
			messagesX.put(caller,new LinkedList());
		}
		LinkedList callerMessages = (LinkedList)messagesX.get(caller);
		callerMessages.addLast(message); // add sender's message to end of list (FCFS)	
		
		if (mode == TRACE) {
			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),sendOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							// use -1 for receiver's version number
							sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							((innerThread)Thread.currentThread()).getVectorTS(),label,openList,UNACCEPTED_ASYNCH_SEND);
			traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);
			control.traceMsg(t);
  		}
		else if (mode == REPLAY || mode == TEST) {
			((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			control.msgReceived();
		}
		else if (mode == RT) { /// do TEST (release permit) and TRACE
			//((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			
			
			if (SymmetryReduce == SYMMETRYREDUCTIONON) {
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				label = B.toString();
			}

			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),sendOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							// use -1 for receiver's version number
							sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							((innerThread)Thread.currentThread()).getVectorTS(),label,openList,UNACCEPTED_ASYNCH_SEND);
							
			// these are accepted immed. after getting the sendPermitX so send arrives before
			// recollected receive, (if it is a recollected receive that received this. 
			traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);

			control.traceMsg(t);

			control.msgReceived();
			if (!isOld) 
				control.sendArrivedRT(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
	}

		senderMutex.V();
		messageAvailable.V();
	}
	
	
		public final void send(int PC) {
				
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
		if (mode == TRACE || mode == RT) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
	   }
	
		boolean isOld = false;
		if (mode == REPLAY || mode == TEST) {
			control.requestSendPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		else if (mode == RT) {
			isOld = control.requestSendPermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
		slotAvailable.P();
		senderMutex.P();

		debugMessage message = new debugMessage();
      message.setMsg(new Object());

      String label = new String("");
      if (generateLTS==LTSON)
         label = new String("'"+sendLabeler.getLabel()+"[S]");  
         
		//label = new String("'"+sendLabeler.getLabel()+"[S]"); 

      if (mode == TRACE || mode == TEST || mode == RT) {
			message.setCaller(((innerThread)Thread.currentThread()).getID());
			message.setVersionNumber(((innerThread)Thread.currentThread()).getVersionNumber());
			message.setVectorTS(((innerThread)Thread.currentThread()).getVectorTS());
			message.setLabel(label);
			message.setIsOld(isOld); // true if message sent by old send during RT
		}
		
		synchronized(messagesX) {
		{
      	if (generateLTS==LTSON) {
 
				//label sender's send event
	   	   String state = ((innerThread)Thread.currentThread()).getThreadState();
	      	programTransition t = new programTransition(((innerThread)Thread.currentThread()).getName(),PC+":"+state,label);
			   LTSgen.depositTransition(t);
		   
				//label Medium's events: Medium = x.'x.Medium
				String portLabel = new String(sendLabeler.getLabel());
	   		//state = getState();
   			t = new programTransition((((innerThread)Thread.currentThread()).getName()+"-"+channelName+"-Medium"),"0","M",portLabel);
		   	LTSgen.depositTransition(t); 

		  	}
		}
		
		Integer caller = new Integer(((innerThread)Thread.currentThread()).getID());
		if (!(messagesX.containsKey(caller))) { // put sender in message map
			messagesX.put(caller,new LinkedList());
		}
		LinkedList callerMessages = (LinkedList)messagesX.get(caller);
		callerMessages.addLast(message); // add sender's message to end of list (FCFS)	
		
		if (mode == TRACE) {
			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),sendOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							// use -1 for receiver's version number
							sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							((innerThread)Thread.currentThread()).getVectorTS(),label,openList,UNACCEPTED_ASYNCH_SEND);
			traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);
			control.traceMsg(t);
  		}
		else if (mode == REPLAY || mode == TEST) {
			((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			control.msgReceived();
		}
		else if (mode == RT) { /// do TEST (release permit) and TRACE
			//((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			
			if (SymmetryReduce == SYMMETRYREDUCTIONON) {
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				label = B.toString();
			}

			srEvent m = new srEvent(((innerThread)Thread.currentThread()).getID(),sendOwnerID,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
							//strategy.equals(THREAD) ? owner.getVersionNumber() : sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							// use -1 for receiver's version number
							sendOwnerVersionNumber,channelName,getAndIncVersionNumber(),UNACCEPTED_ASYNCH_SEND,
							((innerThread)Thread.currentThread()).getVectorTS(),label,openList,UNACCEPTED_ASYNCH_SEND);
							
			// these are accepted immed. after getting the sendPermitX so send arrives before
			// recollected receive, (if it is a recollected receive that received this. 
			traceMessage t = new traceMessage(ASYNCH_SEND_PORT_EVENT_TYPE,m);

			control.traceMsg(t);

			control.msgReceived();
			if (!isOld) 
				control.sendArrivedRT(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
		}
	}

		senderMutex.V();
		messageAvailable.V();

	}

	public final Object receive(int PC) {
			
		synchronized (ownerLock) {
			if (mode != NONE && strategy.equals(THREAD) && owner==null) {
				try {ownerLock.wait();} catch (InterruptedException ex) {}
				//System.out.println("User Error: Each channel must have an owner when a thread-based control strategy is used. Use setOwner() for channel "
				//						 + channelName + "."); 
				//System.out.flush();
				//System.exit(1);
			}
		}

      int callerForReceiveEvent = -1;
		if (mode == TRACE) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
			srEvent m = new srEvent(-2,((innerThread)Thread.currentThread()).getID(),-2,((innerThread)Thread.currentThread()).getVersionNumber(),
								channelName,getVersionNumber(),UNACCEPTED_RECEIVE,
				      		((innerThread)Thread.currentThread()).getVectorTS(),"unacceptedReceive",openList,UNACCEPTED_RECEIVE);
			traceMessage t = new traceMessage(ADD_UNACCEPTED_RECEIVE_TYPE,m);
			control.traceSendReceive(t);
      } 
      else if (mode == REPLAY) {
  		  callerForReceiveEvent = control.requestReceivePermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
  		}
      else if (mode == TEST)
  		  callerForReceiveEvent = control.requestReceivePermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
   	else if (mode == RT) { // do TEST then TRACE
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" calling requestReceivePermitX");
  		   callerForReceiveEvent = control.requestReceivePermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());  		

	      ((innerThread)Thread.currentThread()).updateIntegerTS();
	      
			srEvent m = new srEvent(-2,((innerThread)Thread.currentThread()).getID(),-2,((innerThread)Thread.currentThread()).getVersionNumber(),
								channelName,getVersionNumber(),UNACCEPTED_RECEIVE,
				      		((innerThread)Thread.currentThread()).getVectorTS(),"unacceptedReceive",openList,UNACCEPTED_RECEIVE);
			traceMessage t = new traceMessage(ADD_UNACCEPTED_RECEIVE_TYPE,m);
			control.traceSendReceive(t);
 		}
		

		messageAvailable.P();
		receiverMutex.P();
		// save the first Thread to call receive
		synchronized(receiverLock) {
			// guard against a context switch after if, allowing two threads to see (receiver == null)
			if (receiver == null)						
				receiver = Thread.currentThread();
		}

		if (Thread.currentThread() != receiver) {
				if (mode == TRACE) {
					srEvent m = new srEvent(-1,((innerThread)Thread.currentThread()).getID(),-1,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
									channelName,getAndIncVersionNumber(),RECEIVE_EXCEPTION,
				      			((innerThread)Thread.currentThread()).getVectorTS(),"exception",openList,RECEIVE_EXCEPTION);
					traceMessage t = new traceMessage(RECEIVE_EXCEPTION_TYPE,m);
					control.traceMsg(t);
					t = new traceMessage(REMOVE_UNACCEPTED_RECEIVE_TYPE,m);
					control.traceSendReceive(t);
  			  	}
				else if (mode == TEST) {
					control.requestReceiveExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getAndIncVersionNumber());
				}
				if (mode == REPLAY || mode == TEST)
					control.msgReceived();
				if (mode == RT) {
					control.requestReceiveExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getAndIncVersionNumber());
					srEvent m = new srEvent(-1,((innerThread)Thread.currentThread()).getID(),-1,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
									channelName,getAndIncVersionNumber(),RECEIVE_EXCEPTION,
				      			((innerThread)Thread.currentThread()).getVectorTS(),"exception",openList,RECEIVE_EXCEPTION);
					traceMessage t = new traceMessage(RECEIVE_EXCEPTION_TYPE,m);
					control.traceMsg(t);
					t = new traceMessage(REMOVE_UNACCEPTED_RECEIVE_TYPE,m);
					control.traceSendReceive(t);
					control.msgReceived();
				}
				receiverMutex.V();
				throw new InvalidPortUsage("Attempted to use port with multiple receivers");

		}

		debugMessage receivedMessage = null; 
	synchronized(messagesX) {		
		if (mode == NONE || mode == TRACE) {

				int numCallers = messagesX.size(); // number of threads that have messages in buffer
				int r = (int)(Math.floor(Math.random()*numCallers)+1); // r in 1..numCallers
				Iterator p = messagesX.entrySet().iterator();
				Map.Entry e = null;
				for (int i=0; i<r; i++)
					e = (Map.Entry) p.next(); // select rth sender
				LinkedList l = (LinkedList)e.getValue(); // get messages sent by sender
				receivedMessage = (debugMessage) l.getFirst(); // get first message of sender
				l.removeFirst();
				if(l.size()==0) { // if no more messages from sender, delete sender from Map
					messagesX.remove(e.getKey());
				}

		}
		else if (mode == REPLAY) {
			boolean found = false;

				Iterator p = messagesX.entrySet().iterator();
				while (p.hasNext()) {
					Map.Entry e = (Map.Entry)p.next();
					int ID = ((Integer)e.getKey()).intValue();
					if (ID == callerForReceiveEvent) {
						LinkedList l = (LinkedList)e.getValue();
						receivedMessage = (debugMessage)l.getFirst();
						l.removeFirst();
						if(l.size()==0) { // if no more messages from sender, delete sender from Map
							messagesX.remove(e.getKey());
						}
						found = true;
						break;
					}
				}

			if (!found) {
				receivedMessage = new debugMessage();
				receivedMessage.setMsg(null);
				receivedMessage.setCaller(-1);
				receivedMessage.setVersionNumber(-1);
				receivedMessage.setVectorTS(null);
			}
		}
		else if (mode == TEST) {
			boolean found = false;

				Iterator p = messagesX.entrySet().iterator();
				while (p.hasNext()) {
					Map.Entry e = (Map.Entry)p.next();
					int ID = ((Integer)e.getKey()).intValue();
					if (ID == callerForReceiveEvent) {
						LinkedList l = (LinkedList)e.getValue();
						receivedMessage = (debugMessage)l.getFirst();
						l.removeFirst();
						if(l.size()==0) { // if no more messages from sender, delete sender from Map
							messagesX.remove(e.getKey());
						}
						found = true;
						break;
					}
				}

			if (!found) {
				receivedMessage = new debugMessage();
				receivedMessage.setMsg(null);
				receivedMessage.setCaller(-1);
				receivedMessage.setVersionNumber(-1);
				receivedMessage.setVectorTS(null);
			}
		}
		else if (mode == RT) {
			if (callerForReceiveEvent == -1) { // same as trace
					int numCallers = messagesX.size(); // number of threads that have messages in buffer
					int r = (int)(Math.floor(Math.random()*numCallers)+1); // r in 1..numCallers
					Iterator p = messagesX.entrySet().iterator();
					Map.Entry e = null;
					for (int i=0; i<r; i++)
						e = (Map.Entry) p.next(); // select rth sender
					LinkedList l = (LinkedList)e.getValue(); // get messages sent by sender
					receivedMessage = (debugMessage) l.getFirst(); // get first message of sender
					l.removeFirst();
					if(l.size()==0) { // if no more messages from sender, delete sender from Map
						messagesX.remove(e.getKey());
					}			

			}
			else if (callerForReceiveEvent == -2) { // ignore old sends and receive a new one
					boolean found = false;

/* debug code to see whether messages are old/new
					System.out.println("messagesX.size is "+messagesX.size());
					Iterator q = messagesX.entrySet().iterator();
					while (q.hasNext()) {
						Map.Entry e = (Map.Entry)q.next();
						int ID = ((Integer)e.getKey()).intValue();
						System.out.println("while looking for new send, check msg from " + ID);
						LinkedList l = (LinkedList)e.getValue();
						ListIterator i = (ListIterator) l.listIterator();
						while (i.hasNext()) {
							receivedMessage = (debugMessage)i.next();
							//if (receivedMessage.getIsOld() == false) 
							//	System.out.println("not old");
							//else
							//	System.out.println("old");
						}
					}					
*/

					Iterator p = messagesX.entrySet().iterator();
					while (p.hasNext()) {
						Map.Entry e = (Map.Entry)p.next();
						//int ID = ((Integer)e.getKey()).intValue();
						LinkedList l = (LinkedList)e.getValue();
						receivedMessage = (debugMessage) l.getFirst();
						if (receivedMessage.getIsOld() == false) {
							l.removeFirst();
							if(l.size()==0) { // if no more messages from sender, delete sender from Map
								messagesX.remove(e.getKey());
							}
							found = true;
							break;
						}
					 }

					if (!found) {
						receivedMessage = new debugMessage();
						receivedMessage.setMsg(null);
						receivedMessage.setCaller(-1);
						receivedMessage.setVersionNumber(-1);
						receivedMessage.setVectorTS(null);
					}
			}
			else {
					boolean found = false;

					Iterator p = messagesX.entrySet().iterator();
					while (p.hasNext()) {
						Map.Entry e = (Map.Entry)p.next();
						int ID = ((Integer)e.getKey()).intValue();
						if (ID == callerForReceiveEvent) {
							LinkedList l = (LinkedList)e.getValue();
							receivedMessage = (debugMessage)l.getFirst();
							l.removeFirst();
							if(l.size()==0) { // if no more messages from sender, delete sender from Map
								messagesX.remove(e.getKey());
							}
							found = true;
							break;
						}
					}

				if (!found) {
					receivedMessage = new debugMessage();
					receivedMessage.setMsg(null);
					receivedMessage.setCaller(-1);
					receivedMessage.setVersionNumber(-1);
					receivedMessage.setVectorTS(null);
				}
			}
	
			{
				if (generateLTS == LTSON) {

				// label thread's receive event
				String label = new String(receiveLabeler.getLabel(receivedMessage.getMsg())+"[R]");
		      String threadState = ((innerThread)Thread.currentThread()).getThreadState();
	      	programTransition t = new programTransition(((innerThread)Thread.currentThread()).getName(),PC+":"+threadState,label);
		      LTSgen.depositTransition(t);

				// label port's events: Port = x.'x.Port
				String portLabel = new String(receiveLabeler.getLabel(receivedMessage.getMsg()));
   			t = new programTransition(channelName,"0","P",portLabel);
		   	LTSgen.depositTransition(t); 

		   	}
			}
		}
		
		if (mode == TRACE) {
			String label;
			if (generateLTS == LTSON)
				label = new String(receiveLabeler.getLabel(receivedMessage.getMsg())+"[R]");
			else
				label = new String("");
				
			((innerThread)Thread.currentThread()).updateVectorTS(receivedMessage.getVectorTS());
			srEvent m = new srEvent(receivedMessage.getCaller(),((innerThread)Thread.currentThread()).getID(),receivedMessage.getVersionNumber(),
								((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
								channelName,getAndIncVersionNumber(),ASYNCH_RECEIVE,
			      			((innerThread)Thread.currentThread()).getVectorTS(),label,openList,ASYNCH_RECEIVE);
			traceMessage t = new traceMessage(ASYNCH_RECEIVE_PORT_EVENT_TYPE,m);
			control.traceMsg(t);
			t = new traceMessage(REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE,m);
			control.traceSendReceive(t);
  		}
		else if (mode == REPLAY) {
			((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			if (receivedMessage.getCaller() != -1) {// hang replay if not found
				control.msgReceived();
			}
		}
		else if (mode == TEST) {
			((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			control.msgReceived(receivedMessage.getCaller(),receivedMessage.getVersionNumber());
		}
		else if (mode == RT) {
			
			// don't inc receivers version number again, already did it in TEST part

			String label;
			if (generateLTS == LTSON)			
				label = new String(receiveLabeler.getLabel(receivedMessage.getMsg())+"[R]");
			else
				label = new String("");
				
			//label = new String(receiveLabeler.getLabel(receivedMessage.getMsg())+"[R]");

			((innerThread)Thread.currentThread()).updateVectorTS(receivedMessage.getVectorTS());
			srEvent m = new srEvent(receivedMessage.getCaller(),((innerThread)Thread.currentThread()).getID(),receivedMessage.getVersionNumber(),
								((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
								channelName,getAndIncVersionNumber(),ASYNCH_RECEIVE,
			      			((innerThread)Thread.currentThread()).getVectorTS(),label,openList,ASYNCH_RECEIVE);
			traceMessage t = new traceMessage(ASYNCH_RECEIVE_PORT_EVENT_TYPE,m);
			control.traceMsg(t);
			t = new traceMessage(REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE,m);
			control.traceSendReceive(t);			
			control.msgReceived(receivedMessage.getCaller(),receivedMessage.getVersionNumber());
		}
	}

		receiverMutex.V();
		slotAvailable.V();
		return receivedMessage.getMsg();
   }
   
	public final Object receive() {
		synchronized (ownerLock) {
			if (mode != NONE && strategy.equals(THREAD) && owner==null) {
				try {ownerLock.wait();} catch (InterruptedException ex) {}
			}
		}

      int callerForReceiveEvent = -1;
		if (mode == TRACE) {
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
			srEvent m = new srEvent(-2,((innerThread)Thread.currentThread()).getID(),-2,((innerThread)Thread.currentThread()).getVersionNumber(),
								channelName,getVersionNumber(),UNACCEPTED_RECEIVE,
				      		((innerThread)Thread.currentThread()).getVectorTS(),"unacceptedReceive",openList,UNACCEPTED_RECEIVE);
			traceMessage t = new traceMessage(ADD_UNACCEPTED_RECEIVE_TYPE,m);
			control.traceSendReceive(t);
      } 
      else if (mode == REPLAY) {
	     callerForReceiveEvent = control.requestReceivePermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
  		}
      else if (mode == TEST)
  		  callerForReceiveEvent = control.requestReceivePermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());
   	else if (mode == RT) { // do TEST then TRACE
   		//System.out.println(((innerThread)Thread.currentThread()).getID()+" calling requestReceivePermitX");
  		  	callerForReceiveEvent = control.requestReceivePermitX(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getVersionNumber());  		
			//System.out.println(((innerThread)Thread.currentThread()).getID()+" back from requestReceivePermitX");
	      ((innerThread)Thread.currentThread()).updateIntegerTS();
	      
			srEvent m = new srEvent(-2,((innerThread)Thread.currentThread()).getID(),-2,((innerThread)Thread.currentThread()).getVersionNumber(),
								channelName,getVersionNumber(),UNACCEPTED_RECEIVE,
				      		((innerThread)Thread.currentThread()).getVectorTS(),"unacceptedReceive",openList,UNACCEPTED_RECEIVE);
			traceMessage t = new traceMessage(ADD_UNACCEPTED_RECEIVE_TYPE,m);
			control.traceSendReceive(t);
  		}
		

		messageAvailable.P();
		receiverMutex.P();
		// save the first Thread to call receive
		synchronized(receiverLock) {
			// guard against a context switch after if, allowing two threads to see (receiver == null)
			if (receiver == null)						
				receiver = Thread.currentThread();
		}

		if (Thread.currentThread() != receiver) {
				if (mode == TRACE) {
					srEvent m = new srEvent(-1,((innerThread)Thread.currentThread()).getID(),-1,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
									channelName,getAndIncVersionNumber(),RECEIVE_EXCEPTION,
				      			((innerThread)Thread.currentThread()).getVectorTS(),"exception",openList,RECEIVE_EXCEPTION);
					traceMessage t = new traceMessage(RECEIVE_EXCEPTION_TYPE,m);
					control.traceMsg(t);
					t = new traceMessage(REMOVE_UNACCEPTED_RECEIVE_TYPE,m);
					control.traceSendReceive(t);
  			  	}
				else if (mode == TEST) {
					control.requestReceiveExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getAndIncVersionNumber());
				}
				if (mode == REPLAY || mode == TEST)
					control.msgReceived();
				if (mode == RT) {
					control.requestReceiveExceptionPermit(((innerThread)Thread.currentThread()).getID(),channelName,((innerThread)Thread.currentThread()).getAndIncVersionNumber());
					srEvent m = new srEvent(-1,((innerThread)Thread.currentThread()).getID(),-1,((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
									channelName,getAndIncVersionNumber(),RECEIVE_EXCEPTION,
				      			((innerThread)Thread.currentThread()).getVectorTS(),"exception",openList,RECEIVE_EXCEPTION);
					traceMessage t = new traceMessage(RECEIVE_EXCEPTION_TYPE,m);
					control.traceMsg(t);
					t = new traceMessage(REMOVE_UNACCEPTED_RECEIVE_TYPE,m);
					control.traceSendReceive(t);
					control.msgReceived();
				}
				receiverMutex.V();
				throw new InvalidPortUsage("Attempted to use port with multiple receivers");

		}

		debugMessage receivedMessage = null; //  = (debugMessage) messages[out]; 
	synchronized(messagesX) {		
		if (mode == NONE || mode == TRACE) {

				int numCallers = messagesX.size(); // number of threads that have messages in buffer
				int r = (int)(Math.floor(Math.random()*numCallers)+1); // r in 1..numCallers
				Iterator p = messagesX.entrySet().iterator();
				Map.Entry e = null;
				for (int i=0; i<r; i++)
					e = (Map.Entry) p.next(); // select rth sender
				LinkedList l = (LinkedList)e.getValue(); // get messages sent by sender
				receivedMessage = (debugMessage) l.getFirst(); // get first message of sender
				l.removeFirst();
				if(l.size()==0) { // if no more messages from sender, delete sender from Map
					messagesX.remove(e.getKey());
				}

		}
		else if (mode == REPLAY) {
			boolean found = false;

				Iterator p = messagesX.entrySet().iterator();
				while (p.hasNext()) {
					Map.Entry e = (Map.Entry)p.next();
					int ID = ((Integer)e.getKey()).intValue();
					if (ID == callerForReceiveEvent) {
						LinkedList l = (LinkedList)e.getValue();
						receivedMessage = (debugMessage)l.getFirst();
						l.removeFirst();
						if(l.size()==0) { // if no more messages from sender, delete sender from Map
							messagesX.remove(e.getKey());
						}
						found = true;
						break;
					}
				}

			if (!found) {
				receivedMessage = new debugMessage();
				receivedMessage.setMsg(null);
				receivedMessage.setCaller(-1);
				receivedMessage.setVersionNumber(-1);
				receivedMessage.setVectorTS(null);
			}
		}
		else if (mode == TEST) {
			boolean found = false;

				Iterator p = messagesX.entrySet().iterator();
				while (p.hasNext()) {
					Map.Entry e = (Map.Entry)p.next();
					int ID = ((Integer)e.getKey()).intValue();
					if (ID == callerForReceiveEvent) {
						LinkedList l = (LinkedList)e.getValue();
						receivedMessage = (debugMessage)l.getFirst();
						l.removeFirst();
						if(l.size()==0) { // if no more messages from sender, delete sender from Map
							messagesX.remove(e.getKey());
						}
						found = true;
						break;
					}
				}

			if (!found) {
				receivedMessage = new debugMessage();
				receivedMessage.setMsg(null);
				receivedMessage.setCaller(-1);
				receivedMessage.setVersionNumber(-1);
				receivedMessage.setVectorTS(null);
			}
		}
		else if (mode == RT) {
			//String portState = getState();
			if (callerForReceiveEvent == -1) { // same as trace
					int numCallers = messagesX.size(); // number of threads that have messages in buffer
					int r = (int)(Math.floor(Math.random()*numCallers)+1); // r in 1..numCallers
					Iterator p = messagesX.entrySet().iterator();
					Map.Entry e = null;
					for (int i=0; i<r; i++)
						e = (Map.Entry) p.next(); // select rth sender
					LinkedList l = (LinkedList)e.getValue(); // get messages sent by sender
					receivedMessage = (debugMessage) l.getFirst(); // get first message of sender
					l.removeFirst();
					if(l.size()==0) { // if no more messages from sender, delete sender from Map
						messagesX.remove(e.getKey());
					}			

			}
			else if (callerForReceiveEvent == -2) { // ignore old sends and receive a new one
					boolean found = false;

/* debug code to see whether messages are old/new
					Iterator q = messagesX.entrySet().iterator();
					//System.out.println("messagesX.size() is "+messagesX.size());
					while (q.hasNext()) {
						Map.Entry e = (Map.Entry)q.next();
						int ID = ((Integer)e.getKey()).intValue();
						System.out.println("while looking for new send, check msg from " + ID);
						LinkedList l = (LinkedList)e.getValue();
						ListIterator i = (ListIterator) l.listIterator();
						while (i.hasNext()) {
							receivedMessage = (debugMessage)i.next();
							if (receivedMessage.getIsOld() == false) 
								System.out.println("not old");
							else
								System.out.println("old");
						}
					}					
*/

					Iterator p = messagesX.entrySet().iterator();
					while (p.hasNext()) {
						Map.Entry e = (Map.Entry)p.next();
						//int ID = ((Integer)e.getKey()).intValue();
						LinkedList l = (LinkedList)e.getValue();
						receivedMessage = (debugMessage) l.getFirst();
						if (receivedMessage.getIsOld() == false) {
							l.removeFirst();
							if(l.size()==0) { // if no more messages from sender, delete sender from Map
								messagesX.remove(e.getKey());
							}
							found = true;
							break;
						}
					 }


					if (!found) {
						//System.out.println("not found: -2");
						//System.exit(1);
						receivedMessage = new debugMessage();
						receivedMessage.setMsg(null);
						receivedMessage.setCaller(-1);
						receivedMessage.setVersionNumber(-1);
						receivedMessage.setVectorTS(null);
					}
			}
			else {
					boolean found = false;

					Iterator p = messagesX.entrySet().iterator();
					while (p.hasNext()) {
						Map.Entry e = (Map.Entry)p.next();
						int ID = ((Integer)e.getKey()).intValue();
						if (ID == callerForReceiveEvent) {
							LinkedList l = (LinkedList)e.getValue();
							receivedMessage = (debugMessage)l.getFirst();
							l.removeFirst();
							if(l.size()==0) { // if no more messages from sender, delete sender from Map
								messagesX.remove(e.getKey());
							}
							found = true;
							break;
						}
					}

				if (!found) {
					receivedMessage = new debugMessage();
					receivedMessage.setMsg(null);
					receivedMessage.setCaller(-1);
					receivedMessage.setVersionNumber(-1);
					receivedMessage.setVectorTS(null);
				}
			}
			{
				if (generateLTS == LTSON) {
				// label thread's receive event
				String label = new String(receiveLabeler.getLabel(receivedMessage.getMsg())+"[R]");
		      String state = ((innerThread)Thread.currentThread()).getThreadState();
		      
				StringBuffer B = new StringBuffer();
				Throwable ex = new Throwable();
				StackTraceElement[] stackElements = ex.getStackTrace();
				for (int i=stackElements.length-1; i>=0; i--)
					B.append(stackElements[i]);
				String PC = B.toString();
				
	      	programTransition t = new programTransition(((innerThread)Thread.currentThread()).getName(),PC+":"+state,label);
		      LTSgen.depositTransition(t);

				// label port's events: Port = x.'x.Port
				String portLabel = new String(receiveLabeler.getLabel(receivedMessage.getMsg()));
   			t = new programTransition(channelName,"0","P",portLabel);
		   	LTSgen.depositTransition(t); 

		   	}
			}
		}
		
		if (mode == TRACE) {
			String label;
			if (generateLTS == LTSON)
				label = new String(receiveLabeler.getLabel(receivedMessage.getMsg())+"[R]");
			else
				label = new String("");
				
			((innerThread)Thread.currentThread()).updateVectorTS(receivedMessage.getVectorTS());
			srEvent m = new srEvent(receivedMessage.getCaller(),((innerThread)Thread.currentThread()).getID(),receivedMessage.getVersionNumber(),
								((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
								channelName,getAndIncVersionNumber(),ASYNCH_RECEIVE,
			      			((innerThread)Thread.currentThread()).getVectorTS(),label,openList,ASYNCH_RECEIVE);
			traceMessage t = new traceMessage(ASYNCH_RECEIVE_PORT_EVENT_TYPE,m);
			control.traceMsg(t);
			t = new traceMessage(REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE,m);
			control.traceSendReceive(t);
  		}
		else if (mode == REPLAY) {
			((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			if (receivedMessage.getCaller() != -1) {// hang replay if not found
				control.msgReceived();
			}
		}
		else if (mode == TEST) {
			((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			control.msgReceived(receivedMessage.getCaller(),receivedMessage.getVersionNumber());
		}
		else if (mode == RT) {
			//((innerThread)Thread.currentThread()).getAndIncVersionNumber();
			
			// don't inc receivers version number again, already did it in TEST part

			String label;
			if (generateLTS == LTSON)			
				label = new String(receiveLabeler.getLabel(receivedMessage.getMsg())+"[R]");
			else
				label = new String("");
				
			//label = new String(receiveLabeler.getLabel(receivedMessage.getMsg())+"[R]");
     
			((innerThread)Thread.currentThread()).updateVectorTS(receivedMessage.getVectorTS());
			srEvent m = new srEvent(receivedMessage.getCaller(),((innerThread)Thread.currentThread()).getID(),receivedMessage.getVersionNumber(),
								((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
								channelName,getAndIncVersionNumber(),ASYNCH_RECEIVE,
			      			((innerThread)Thread.currentThread()).getVectorTS(),label,openList,ASYNCH_RECEIVE);
			traceMessage t = new traceMessage(ASYNCH_RECEIVE_PORT_EVENT_TYPE,m);
			control.traceMsg(t);
			t = new traceMessage(REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE,m);
			control.traceSendReceive(t);			
			control.msgReceived(receivedMessage.getCaller(),receivedMessage.getVersionNumber());
		}
	}

		receiverMutex.V();
		slotAvailable.V();
		return receivedMessage.getMsg();
   }
}

final class InvalidPortUsage extends InvalidChannelUsage {
	InvalidPortUsage() { }
	InvalidPortUsage(String msg) {super(msg);}
}
