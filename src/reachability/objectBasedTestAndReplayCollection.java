package reachability;
import java.util.*;
import java.io.*;

class objectBasedTestAndReplayCollection implements eventTypeParameters,
		propertyParameters {


	private ArrayList objectBasedEventSequence = null;

	private ObjectOutputStream outputReplay = null; // output sequence of int IDs
	private PrintWriter outputReplayText = null;
	private ObjectOutputStream outputTest = null; // output sequence of int IDs
	private PrintWriter outputTestText = null;
	private ObjectOutputStream outputRT = null; // output sequence of int IDs
	private PrintWriter outputRTText = null;

	propertyParameters.Controllers numControllers;

	public objectBasedTestAndReplayCollection (ObjectOutputStream outputTest,PrintWriter outputTestText,
					ObjectOutputStream outputReplay, PrintWriter outputReplayText,
					ObjectOutputStream outputRT,PrintWriter outputRTText,
					propertyParameters.Controllers numControllers) {
		this.outputTest = outputTest;
		this.outputTestText = outputTestText;
		this.outputRT = outputRT;
		this.outputRTText = outputRTText;
		this.outputReplay = outputReplay;
		this.outputReplayText = outputReplayText;
		this.numControllers = numControllers;
		
		objectBasedEventSequence = new ArrayList();
	}


	public synchronized int updateSequence(srEvent e) {
		int nextIndex = objectBasedEventSequence.size();
		objectBasedEventSequence.add(e);
		return nextIndex;
	}

	public synchronized void changeToAccepted(int index, int actualCalledThread,
	      int actualCalledVersionNumber) {
		srEvent unaccepted = (srEvent) objectBasedEventSequence.get(index); // get it 
		unaccepted.setEventType(ASYNCH_SEND); //change type to accepted
		//unaccepted.setEventType2(ASYNCH_SEND); //change type to accepted
		unaccepted.setCalled(actualCalledThread);
		unaccepted.setCalledVersionNumber(actualCalledVersionNumber);
		objectBasedEventSequence.set(index,unaccepted); // put it
	}

	public synchronized ArrayList getSequence() {
		return objectBasedEventSequence;
	}
	
	public synchronized void clearSequence() {
		objectBasedEventSequence = new ArrayList();	
	}

	public synchronized void outputObjectBasedTestAndReplaySequences() throws Exception {
		System.out.println("Output object-based test and replay sequences");
		try {
			Iterator i = objectBasedEventSequence.iterator();
			while (i.hasNext()) {
				srEvent event = (srEvent) i.next();

				// write test event
				//outputTest.writeObject(event);
				//outputTest.flush();
				// numControllers could be SINGLE

				if (numControllers == MULTIPLE)
					outputTestText.println("("+event.getCaller()+","+event.getCalled()+
						","+event.getCallerVersionNumber()+","+event.getCalledVersionNumber()+
						","+event.getChannelName()+","+event.getEventType()+")");			
				else
					outputTestText.println("("+event.getCaller()+","+event.getCalled()+
						","+event.getCallerVersionNumber()+","+event.getCalledVersionNumber()+
						","+event.getChannelName()+","+event.getChannelVersionNumber()+","+event.getEventType()+")");
					//outputTestText.println("("+event.getCaller()+","+event.getCalled()+
					//	","+event.getChannelName()+","+event.getEventType()+")");
				outputTestText.flush();
						
						
				// write RT event
				//outputRT.writeObject(event);
				//outputRT.flush();
				// numControllers could be SINGLE

				// always output version numbers for RT
				//	outputRTText.println("("+event.getCaller()+","+event.getCalled()+
				//		","+event.getCallerVersionNumber()+","+event.getCalledVersionNumber()+
				//		","+event.getChannelName()+","+event.getEventType()+")");			

				// write replay event

				simplesrEvent simpleEvent;
				// Note: object-based uses -1 on send_exception called and receive_excpetion caller,
		      // so this SEND_EXCEPTION check is not  needed, just the ASYNCH_SEND check is needed
				if (event.getEventType().equals(ASYNCH_SEND) || event.getEventType().equals(SEND_EXCEPTION))
					// can't use getCalled() since it's not -1 on asynch_sends, it's owner!
					// but replay uses called == -1 to skip to next event
					simpleEvent = new simplesrEvent(event.getCaller(),-1,event.getEventType());
				else if (event.getEventType().equals(ASYNCH_RECEIVE))
					simpleEvent = new simplesrEvent(event.getCaller(),event.getCalled(),event.getEventType());
				else
					simpleEvent = new simplesrEvent(event.getCaller(),event.getCalled(),event.getEventType());
				//outputReplay.writeObject(simpleEvent);
				//outputReplay.flush();
				if (event.getEventType().equals(ASYNCH_SEND) || event.getEventType().equals(SEND_EXCEPTION))
					// can't use getCalled() since it's not -1 on asynch_sends, it's owner!
					// but replay uses called == -1 to skip to next event
					outputReplayText.println("("+event.getCaller()+",-1,"+event.getEventType()+")");
				else if (event.getEventType().equals(ASYNCH_RECEIVE))
					outputReplayText.println("("+event.getCaller()+","+event.getCalled()+","+event.getEventType()+")");
				else
					outputReplayText.println("("+event.getCaller()+","+event.getCalled()+","+event.getEventType()+")");			
				outputReplayText.flush();			

			}
			//outputTest.close();
			outputTestText.close();
			//outputRT.close();
			//outputRTText.close();
			//outputReplay.close();
			outputReplayText.close();
		} 	catch (Exception e) {
				System.out.println("IOExcpetion in output object-based test and replay Sequences");
				System.out.flush();
				if (outputTest != null)
					outputTest.close();
				if (outputTestText != null)
					outputTestText.close();
				if (outputRT != null)
					outputRT.close();
				if (outputRTText != null)
					outputRTText.close();
				//if	(outputReplay != null)
				//	outputReplay.close();
				if (outputReplayText != null)
					outputReplayText.close();
				throw e;
  			}
		System.out.println("\n\n***Test and replay sequences generated***");
		System.out.flush();
	}

}
