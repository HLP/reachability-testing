package reachability;
public abstract class semaphore {
	protected abstract void P();
	protected abstract void V();
	protected abstract void VP(semaphore vSem);	
	protected semaphore(int initialPermits) {permits = initialPermits; semaphoreID = getSemID();}
	  // constructor calls getSemID() to get an ID for the semaphore. IDs are used in VP().
	protected static synchronized int getSemID() {return ID++;}
	  // each semaphore gets an ID when it is instantiated. IDs start with 1.
	protected abstract int getSemaphoreID();	// access method that returns the semaphore's ID
	protected int permits;
	protected static int ID=1;		// IDs start with 1
	protected int semaphoreID;		// each semaphore has an integer ID
}

