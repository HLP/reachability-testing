package reachability;
/*
@author  j.n.magee 20/11/96
*/

import java.util.*;


/*********************Select*****************************/
// implements choice
public final class selectiveWait 
	implements eventTypeParameters, traceMessageTypeParameters,
	propertyParameters, IselectiveWait {
   private Vector list = new Vector();
	private delayAlternative delay = null;
	private elseAlternative else_ = null;
	private boolean hasElse = false;
	private boolean hasDelay = false;
	
	protected ArrayList openList = null;

	protected propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	protected propertyParameters.Controllers numControllers = SINGLE;
	protected propertyParameters.Strategy strategy = OBJECT;

	private threadBasedMsgTracingAndReplay control = null;

   public ArrayList getOpenList() {return (ArrayList) openList.clone();}
   public void resetOpenList() {openList = new ArrayList();}
   
	selectiveWait() {
		strategy = (propertyReader.getInstance().getStrategyProperty());
		numControllers = (propertyReader.getInstance().getControllersProperty());
		mode = (propertyReader.getInstance().getModeProperty());
		openList = new ArrayList();
		if (mode!=NONE && strategy == OBJECT) {
			System.out.println("User Error: You must use a thread-based control strategy when the program has a selective wait statement.");
			System.exit(1);
		}
	}


	public final class delayAlternative {
		private long msecDelay = 0;
		boolean guard = true;

		delayAlternative(long msecDelay) {
			this.msecDelay = msecDelay;
		}

		public long getMsecDelay() {
			return msecDelay;
		}

    	public void guard(boolean g) {
        guard = g;
    	}

    	boolean testGuard(){
        return guard;
    	}

		void accept() {
      	((innerThread)Thread.currentThread()).updateIntegerTS();
			if (mode == TRACE) {
				control = (threadBasedMsgTracingAndReplay)(((channel)list.firstElement()).getController());
				srEvent m = new srEvent(-1,((innerThread)Thread.currentThread()).getID(),-1,
								((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
								"N/A",-1,ELSE_DELAY,
				      		((innerThread)Thread.currentThread()).getVectorTS(),"else_delay",openList,ELSE_DELAY);
				traceMessage t = new traceMessage(ELSE_DELAY_TYPE,m);
				control.traceMsg(t);	
			}
			else if (mode == TEST) {
				if (control == null) { // control is set in choose(), which should be called before accept()
					System.out.println("You must call select.choose() on a selective wait before");
					System.out.println("  you call accept() on a delayAlternative.");
					System.exit(1);			
				}
				control.requestElseDelayPermit(((innerThread)Thread.currentThread()).getID(),((innerThread)Thread.currentThread()).getAndIncVersionNumber());
			}
			
			if (mode == REPLAY || mode == TEST) { // control is set in choose(), which should be called before accept()
				if (control == null) {
					System.out.println("You must call select.choose() on a selective wait before");
					System.out.println("  you call accept() on a delayAlternative.");
					System.exit(1);			
				}
				control.msgReceived();
			}
		}
	}

	public final class elseAlternative {
		void accept() {
      	((innerThread)Thread.currentThread()).updateIntegerTS();
			if (mode == TRACE) {
				control = (threadBasedMsgTracingAndReplay)(((channel)list.firstElement()).getController());
				srEvent m = new srEvent(-1,((innerThread)Thread.currentThread()).getID(),-1,
								((innerThread)Thread.currentThread()).getAndIncVersionNumber(),
								"N/A",-1,ELSE_DELAY,
				      		((innerThread)Thread.currentThread()).getVectorTS(),"else_delay",openList,ELSE_DELAY);

				traceMessage t = new traceMessage(ELSE_DELAY_TYPE,m);
				control.traceMsg(t);	
			}
			else if (mode == TEST) { // control is set in choose(), which should be called before accept()
				if (control == null) {
					System.out.println("You must call select.choose() on a selective wait before");
					System.out.println("  you call accept() on an else alternative.");
					System.exit(1);			
				}
				control.requestElseDelayPermit(((innerThread)Thread.currentThread()).getID(),((innerThread)Thread.currentThread()).getAndIncVersionNumber());
			}

			if (mode == REPLAY || mode == TEST) { // control is set in choose(), which should be called before accept()
				if (control == null) {
					System.out.println("You must call select.choose() on a selective wait before");
					System.out.println("  you call accept() on an else alternative.");
					System.exit(1);			
				}
				control.msgReceived();
			}
		}
	}

    public void add(selectable s) {
        list.addElement(s);
        s.setSelect(this);
    }

    public void add(delayAlternative d) {
       this.delay = d;
       
		 if (this.hasDelay) {
		 	System.out.println("Warning: A selectiveWait cannot have more than one delay alternative.");
		 	System.out.println("Warning: Only one of the delay alternatives can be selected.");
		 }
		 
		 this.hasDelay = true;
		 
		 if (this.hasElse) {
		 	System.out.println("Warning: A selectiveWait cannot have an else alternative and a delay alternative.");
		 	System.out.println("Warning: Only one of the else/delay alternatives can be selected.");
		 }
    }
    
    public void add(elseAlternative e) {
       this.else_ = e;
       
		 if (this.hasElse) {
		 	System.out.println("Warning: A selectiveWait cannot have more than one else alternative.");
		 	System.out.println("Warning: Only one of the else alternatives can be selected.");
		 }
		 
		 this.hasElse = true;
		 
		 if (this.hasDelay) {
		 	System.out.println("Warning: A selectiveWait cannot have a delay alternative and an else alternative.");
		 	System.out.println("Warning: Only one of the else/delay alternatives can be selected.");
		 }
    }

    public void removeElse() {
			hasElse = false;
			else_ = null;
    }

    public void removeDelay() {
			hasDelay = false;
			delay = null;
    }

    private void clearAll() {
        for (Enumeration e = list.elements(); e.hasMoreElements();){
           ((selectable)e.nextElement()).clearOpen();
        }
    }

    private void openAll() {
        for (Enumeration e = list.elements(); e.hasMoreElements();){
            selectable s = (selectable)e.nextElement();
            if (s.testGuard()) s.setOpen();
        }
    }
    
    private void computeOpenList() {
    // called during RT to get list of open alternatives
      openList = new ArrayList();
		for (Enumeration e = list.elements(); e.hasMoreElements(); ) {
      	selectable s = (selectable)e.nextElement();
	     	if (s.testGuard()) {
  				OpenEvent event = new OpenEvent(s.getChannelName(),-1);
	     		openList.add(event); // list of open alternatives for RT
	     	}
      }
    }
    

    private int testAll() {
    
			long oldest = Long.MAX_VALUE;
			int i = 0;
			int j = 1;
			boolean AtLeastOneTrueGuard = false;
			for (Enumeration e = list.elements(); e.hasMoreElements(); ++j) {
        		selectable s = (selectable)e.nextElement();
       		AtLeastOneTrueGuard = AtLeastOneTrueGuard || s.testGuard();
           	if (s.testReady() && s.testGuard()) {
					long oldestSArrival = s.getOldestArrival();
					if (oldestSArrival < oldest) {
						i = j;
						oldest = oldestSArrival;
					}
				}
        }
        if (i == 0 && !AtLeastOneTrueGuard)
          return -1; // all guards are false
        else 
        	 return i;
    }

	public synchronized int choose() throws InterruptedException {
		Controller RTcontrol = null;
		boolean oneArrival;
		if (mode == REPLAY || mode == TEST || mode == RT) {
/* Assumes list is not empty!! */
			if (mode == REPLAY || mode == TEST) {
				control = (threadBasedMsgTracingAndReplay)(((channel)list.firstElement()).getController());
				oneArrival = control.requestSelectPermit(((innerThread)Thread.currentThread()).getID());
			}
			else {
				//RTcontrol = ((innerThread)Thread.currentThread()).control; //msgTracingAndReplay.getInstance();
				RTcontrol = msgTracingAndReplay.getInstance(mode,numControllers,strategy,"channel");
				//System.out.println(((innerThread)Thread.currentThread()).getID()+ " Requesting select permit");
				oneArrival = RTcontrol.requestSelectPermit(((innerThread)Thread.currentThread()).getID());
			}
			if (oneArrival) {
				//System.out.println("wait for oneArrival");
				int currentArrivals=0;
				for (Enumeration e = list.elements(); e.hasMoreElements();) {
        			selectable s = (selectable)e.nextElement();
					currentArrivals += s.count();
				}


				if (currentArrivals < 1) {

        			openAll();
					wait(); // Thread.sleep(1000);
/*
					for (Enumeration e = list.elements(); e.hasMoreElements();) {
        				selectable s = (selectable)e.nextElement();
						currentArrivals += s.count();
					}
*/
					clearAll();
				}

			}	
		}

		int readyIndex = 0;
      readyIndex=testAll();
      if (readyIndex<=0) { // not ready
			if (hasElse) {
				return list.size()+1;
			}
        	openAll();
			if (hasDelay && delay.testGuard()) { // allow timeout
        		//long startTime = System.currentTimeMillis();
   	     	long waitTime = delay.getMsecDelay();
        		wait(waitTime);
				readyIndex=testAll();
        		if (readyIndex <= 0) 
					/* must be timeout here, since no unrelated notifications. */
              	readyIndex = list.size()+1;
			} // allow timeout
			else { // no else or open delay
				if (readyIndex == -1)            // all accept alternatives have false guards
					throw new SelectException();  // && the delay alternative is closed or no else
				//System.out.println("selective wait starts to wait().");
				wait();
				//System.out.println("selective wait awakens.");
				readyIndex=testAll();
			} // no else or delay
		} // not ready
		if (mode == RT) {
			computeOpenList();
		}
		clearAll();
		return readyIndex;
	} // choose

}


