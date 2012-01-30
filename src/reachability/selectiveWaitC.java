package reachability;

import java.util.*;


/*********************Select*****************************/
// implements choice
public final class selectiveWaitC implements IselectiveWait{
   Vector list = new Vector();
	delayAlternative delay = null;
	elseAlternative else_ = null;
	boolean hasElse = false;
	boolean hasDelay = false;

	public final Vector getList() {return list;}

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

		public void accept() { }
	}

	public final class elseAlternative {
				public void accept() { }
	}

    public void add(selectable s, int ID) {
        list.addElement(s);
        s.setSelect(this);
        s.setID(ID);
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

    private void clearAll(Vector openAlternatives) {
        for (Enumeration e = openAlternatives.elements(); e.hasMoreElements();){
           ((selectable)e.nextElement()).clearOpen();
        }
    }

    private void openAll(Vector openAlternatives) {
        for (Enumeration e = openAlternatives.elements(); e.hasMoreElements();){
            selectable s = (selectable)e.nextElement();
            if (s.testGuard()) s.setOpen();
            //if (s.getChannelName().equals("traceMsg"))
       		//	traceit.getInstance().trace("traceMsg setOpen()");
        }
    }

    private int testAll(Vector openAlternatives) {
			long oldest = Long.MAX_VALUE;
			int i = 0;
			int j = 1; // for debugging
			boolean AtLeastOneTrueGuard = false;
			for (Enumeration e = openAlternatives.elements(); e.hasMoreElements(); ++j) {
        		selectable s = (selectable)e.nextElement();
        		//System.out.println(s.getID());
        		if (!s.testGuard()) {
        			System.out.println("Internal error: selectiveWaitC guard not open in "+s.getID());
        			System.exit(1);
        		}
        		//else System.out.println("open in "+s.getID());
       		AtLeastOneTrueGuard = AtLeastOneTrueGuard || s.testGuard(); // all guards should be true
       		//if (s.getChannelName().equals("traceMsg")) {
       			//traceit.getInstance().trace("for traceMsg: j is " + j + "  s.testReady() is " + s.testReady() + " s.testGuard is " + s.testGuard());
	           	//if (s.testReady() && s.testGuard())
  	       			//traceit.getInstance().trace("s.getOldestArrival() is " + s.getOldestArrival());
       		//}
           	if (s.testReady() && s.testGuard()) {
					long oldestSArrival = s.getOldestArrival();
					if (oldestSArrival < oldest) {
						i = s.getID(); // j;
						//System.out.println(j+", "+s.getID()); // should match
						oldest = oldestSArrival;
						/* Optimization for RT? no need to find earliest */
						break;
					}
				}
        }
        if (i == 0 && !AtLeastOneTrueGuard)
          return -1; // all guards are false
        else 
        	 return i;
    }

    private int testAllPrint(Vector openAlternatives) {
			long oldest = Long.MAX_VALUE;
			int i = 0;
			int j = 1;
			boolean AtLeastOneTrueGuard = false;
			for (Enumeration e = openAlternatives.elements(); e.hasMoreElements(); ++j) {
        		selectable s = (selectable)e.nextElement();
       		AtLeastOneTrueGuard = AtLeastOneTrueGuard || s.testGuard();
				//System.out.println("j is " + j + "  s.testReady() is " + s.testReady() + " s.testGuard is " + s.testGuard());
           	if (s.testReady() && s.testGuard()) {
					long oldestSArrival = s.getOldestArrival();
					//System.out.println("oldestSArrival is " + oldestSArrival);
					if (oldestSArrival < oldest) {
						i = j;
						oldest = oldestSArrival;
						/* Optimization for RT? no need to find earliest */
						break;
					}
				}
        }
        System.out.println("i is " + i);
        if (i == 0 && !AtLeastOneTrueGuard)
          return -1; // all guards are false
        else 
        	 return i;
    }

	public synchronized int choose(Vector openAlternatives) throws InterruptedException {

		int readyIndex = 0;
		int rep=0;
		do {
	      readyIndex=testAll(openAlternatives);
	      rep++;
	   } while (readyIndex<=0 && rep<100);
      if (readyIndex<=0) { // not ready
			if (hasElse)
				return list.size()+1;
        	openAll(openAlternatives);
			if (hasDelay && delay.testGuard()) { // allow timeout
        		//long startTime = System.currentTimeMillis();
   	     	long waitTime = delay.getMsecDelay();
     			//traceit.getInstance().trace("choose waiting");
     			rep=0;
     			do {
     			//synchronized(this) {
	        		wait(waitTime);
	        	//}
					readyIndex=testAll(openAlternatives);
					rep++;
				} while (readyIndex<=0 && rep<100);
        		if (readyIndex <= 0)  {
        		   readyIndex = testAll(openAlternatives);
					/* must be timeout here, since no unrelated notifications. */
	     			//traceit.getInstance().trace("choose timeout");
	     			if (readyIndex <=0)
	              	readyIndex = list.size()+1;
            } 
        		//else traceit.getInstance().trace("choose returning " + readyIndex);
			} // allow timeout
			else { // no else or open delay
			/* Controller does have a delay so this should never be executed */
				if (readyIndex == -1)            // all accept alternatives have false guards
					throw new SelectException();  // && the delay alternative is closed or no else
    			synchronized(this) {
					wait();
				}
				readyIndex=testAll(openAlternatives);
			} // no else or delay
		} // not ready
		//else traceit.getInstance().trace("choose returning " + readyIndex);

		clearAll(openAlternatives);
		return readyIndex;
	} // choose
	
	
	public String getChannelName() {return "foo";}
	// openList refers to list of open alternatives used in srEvents for reachabilty testing
   public void resetOpenList() {}
   public ArrayList getOpenList() {return new ArrayList();}

}


