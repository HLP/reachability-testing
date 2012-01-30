package reachability;
final class boundedBuffer implements Buffer {
    private int fullSlots=0;
    private int capacity = 0;
    private Object[] buffer = null;
    private int in = 0, out = 0;
    private boolean appWaiting = false;

    public boundedBuffer(int _capacity) {
		capacity = _capacity;
		buffer = new Object [capacity];
    }

    public synchronized int size () {
		 return fullSlots;
    }
    
    public synchronized boolean consumerIsWaiting () {
		 return appWaiting;
    }   

    public synchronized Object withdrawN() {
    	return new Object();
    }
    
    public synchronized Object withdraw () {
		Object rval = null;
		while (fullSlots == 0)
	    try { appWaiting=true; wait(); appWaiting=false;} catch (InterruptedException ex) {}
		rval = buffer[out];	

		// help GC
		buffer[out] = null;

		out = (out + 1) % capacity;
		if (fullSlots-- == capacity)	
	    	notifyAll();				
			return rval;
    	}

    public synchronized void deposit (Object value) {
		while (fullSlots == capacity)
	    try { wait(); } catch (InterruptedException ex) {}
		buffer[in] = value;
		in = (in + 1) % capacity;
		if (fullSlots++ == 0)
	    notifyAll();
    }
}
