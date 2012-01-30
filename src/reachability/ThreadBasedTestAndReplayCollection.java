package reachability;
import java.util.*;
import java.io.*;

class ThreadBasedTestAndReplayCollection implements eventTypeParameters,
		propertyParameters {


	private ArrayList threadBasedEventSequence = null;

	private ObjectOutputStream outputReplay = null; // output sequence of int IDs
	private PrintWriter outputReplayText = null;
	private ObjectOutputStream outputTest = null; // output sequence of int IDs
	private PrintWriter outputTestText = null;

	propertyParameters.Controllers numControllers;

	public ThreadBasedTestAndReplayCollection (ObjectOutputStream outputTest,PrintWriter outputTestText,
					ObjectOutputStream outputReplay, PrintWriter outputReplayText,
					propertyParameters.Controllers numControllers) {
		this.outputTest = outputTest;
		this.outputTestText = outputTestText;
		this.outputReplay = outputReplay;
		this.outputReplayText = outputReplayText;
		this.numControllers = numControllers;
		
		threadBasedEventSequence = new ArrayList();
	}


	public int updateSequence(srThreadEvent e) {
		int nextIndex = threadBasedEventSequence.size();
		threadBasedEventSequence.add(e);
		return nextIndex;
	}

	public void changeToAccepted(int index, int actualCalledThread) {
		//system.out.println("OLD SEQUENCE: " + threadBasedEventSequence);
		srThreadEvent unaccepted = (srThreadEvent) threadBasedEventSequence.get(index); // get it 
		//System.out.println("unaccepted before: " + unaccepted);
		unaccepted.setEventType(ASYNCH_SEND); //change type to accepted
		unaccepted.setThisThread(actualCalledThread);
		//System.out.println("Changed this thread to " + actualCalledThread + " for " + unaccepted);
		threadBasedEventSequence.set(index,unaccepted); // put it
		//System.out.println("NEW SEQUENCE: " + threadBasedEventSequence);
	}

	public void outputThreadBasedTestAndReplaySequences() throws Exception {
		//System.out.println("Output thread-based test and replay sequences");
		try {
			//System.out.println("sequence size is " + threadBasedEventSequence.size());
			Iterator i = threadBasedEventSequence.iterator();
			while (i.hasNext()) {
				srThreadEvent threadEvent = (srThreadEvent) i.next();

				// write test event
				//outputTest.writeObject(threadEvent);
				//outputTest.flush();
				// numControllers could be SINGLE
				if (numControllers == MULTIPLE)
					outputTestText.println("("+threadEvent.getOtherThread()+","+threadEvent.getOtherThreadVersionNumber()+","+
						threadEvent.getThisThread()+","+threadEvent.getThisThreadVersionNumber()+","+threadEvent.getChannelName()+
						","+threadEvent.getChannelVersionNumber()+","+threadEvent.getEventType()+")");
				else
					outputTestText.println("("+threadEvent.getOtherThread()+","+threadEvent.getOtherThreadVersionNumber()+","+
						threadEvent.getThisThread()+","+threadEvent.getThisThreadVersionNumber()+","+threadEvent.getChannelName()+
						","+threadEvent.getChannelVersionNumber()+","+threadEvent.getEventType()+")");
				//	outputTestText.println("("+threadEvent.getOtherThread()+","+threadEvent.getThisThread()+
				//		","+threadEvent.getChannelName()+","+threadEvent.getEventType()+")");
				outputTestText.flush();
					// write replay event

				simplesrEvent simpleEvent;
				if (threadEvent.getEventType().equals(ASYNCH_SEND) || threadEvent.getEventType().equals(SEND_EXCEPTION))
					// can't use getCalled() since it's not -1 on asynch_sends, it's owner!
					// but replay uses called == -1 to skip to next event
					simpleEvent = new simplesrEvent(threadEvent.getOtherThread(),-1,threadEvent.getEventType());
				else if (threadEvent.getEventType().equals(ASYNCH_RECEIVE))
					simpleEvent = new simplesrEvent(threadEvent.getOtherThread(),threadEvent.getThisThread(),threadEvent.getEventType());
				else if (threadEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND))
						// can't use getCalled() since it's not -2 on unaccepted_sends, it's owner!
						// but replay uses called == -2 to skip to next event
						simpleEvent = new simplesrEvent(threadEvent.getOtherThread(),-2,threadEvent.getEventType());
				else if (threadEvent.getEventType().equals(UNACCEPTED_RECEIVE))
						simpleEvent = new simplesrEvent(threadEvent.getOtherThread(),threadEvent.getThisThread(),threadEvent.getEventType());
				else
					simpleEvent = new simplesrEvent(threadEvent.getOtherThread(),threadEvent.getThisThread(),threadEvent.getEventType());
				//outputReplay.writeObject(simpleEvent);
				//outputReplay.flush();

				if (threadEvent.getEventType().equals(ASYNCH_SEND) || threadEvent.getEventType().equals(SEND_EXCEPTION))
					// can't use getCalled() since it's not -1 on asynch_sends, it's owner!
					// but replay uses called == -1 to skip to next event
					outputReplayText.println("("+threadEvent.getOtherThread()+",-1,"+threadEvent.getEventType()+")");
				else if (threadEvent.getEventType().equals(ASYNCH_RECEIVE))
					outputReplayText.println("("+threadEvent.getOtherThread()+","+threadEvent.getThisThread()+","+threadEvent.getEventType()+")");
				else if (threadEvent.getEventType().equals(UNACCEPTED_SYNCH_SEND))
						// can't use getCalled() since it's not -2 on unaccepted_sends, it's owner!
						// but replay uses called == -2 to skip to next event
						outputReplayText.println("("+threadEvent.getOtherThread()+",-2,"+threadEvent.getEventType()+")");
				else if (threadEvent.getEventType().equals(UNACCEPTED_RECEIVE))
						outputReplayText.println("("+threadEvent.getOtherThread()+","+threadEvent.getThisThread()+","+threadEvent.getEventType()+")");
				else
					outputReplayText.println("("+threadEvent.getOtherThread()+","+threadEvent.getThisThread()+","+threadEvent.getEventType()+")");			
				outputReplayText.flush();

			}
			//outputTest.close();
			outputTestText.close();
			//outputReplay.close();
			outputReplayText.close();
		} 	catch (Exception e) {
				System.out.println("IOExcpetion in outputThreadBasedSequences");
				System.out.flush();
				//if (outputTest != null)
				//	outputTest.close();
				if (outputTestText != null)
					outputTestText.close();
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
