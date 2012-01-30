package reachability;
import java.util.*;

final class srEventPool {
	private int poolSize;
	//private final int increment = 50;
	private LinkedList pool;
	public srEventPool(int initialSize) {
	// space must hold at least 1 Integer
		if (initialSize < 1)
			initialSize = 1;
		poolSize = initialSize;
		pool = new LinkedList();
		// pool holds srEvents
		for (int i=0; i<poolSize; i++)
			pool.addLast(new srEvent());
	}
	
	public /*synchronized*/ final srEvent acquire() {
   	srEvent s = null;
		if (!(pool.size()==0)) {
			s = (srEvent) pool.getFirst();
			pool.removeFirst();
		}
		else {
			s = new srEvent();
			poolSize++;
		}	 
		return s;
	}

	public /*synchronized*/ final void release (srEvent s) {
		pool.addLast(s);
	}
}
