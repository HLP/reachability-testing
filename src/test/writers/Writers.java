package test.writers;

import lattice.ConsistentStates;
import lattice.Evaluable;
import reachability.TDThread;
import reachability.countingSemaphore;
import reachability.sharedInteger;

public final class Writers implements Evaluable {
	@Override
	public boolean evaluate(ConsistentStates cs) {
		int x = (Integer) cs.getState(0);
		// int x = (Integer) cs.getState("x");
		// User can get variable using a tag name in the future
		int y = (Integer) cs.getState(1);
		
		if ((x == 0) && (y == 1)) {
			System.out.println("CGS" + cs + " State[" + x + "," + y + "]");
		}
		return false;
	}
	
	public static void main (String args[]) {
		sharedInteger x = new sharedInteger(0,"x");
		sharedInteger y = new sharedInteger(0,"y");
		countingSemaphore mutex1 = new countingSemaphore(1, "mutex1");
		countingSemaphore mutex2 = new countingSemaphore(1, "mutex2");

		Writer w1 = new Writer (x,y,1,mutex1,mutex2);
		Writer w2 = new Writer (x,y,2,mutex1,mutex2);
		try {
			w1.start(); w2.start();
			w1.join(); w2.join();
		} catch (InterruptedException e) {}
	}
}

final class Writer extends TDThread {
	sharedInteger s1;
	sharedInteger s2;
	private int ID;
	private countingSemaphore mutex1;
	private countingSemaphore mutex2;

	Writer (sharedInteger s1, sharedInteger s2, int ID, countingSemaphore mutex1, countingSemaphore mutex2) {
		super("Writer"+ID);
		this.s1 = s1; this.s2 = s2; this.mutex1 = mutex1; this.ID = ID; this.mutex2 = mutex2;
	}

	public void run () {
		int val = 0;
		if (ID == 1) {
			mutex1.P();
			val = (s1.Read() + 1) % 2;
			System.out.println("Writer" + ID + " is writing " + val + " to "+s1.getName());
			s1.Write(val);
			mutex1.V();

			mutex2.P();
			val = (s2.Read() + 1) % 2;
			System.out.println("Writer" + ID + " is writing " + val + " to "+s2.getName());
			s2.Write(val);
			mutex2.V();
		} else {
			mutex2.P();
			val = (s2.Read() + 1) % 2;
			System.out.println("Writer" + ID + " is writing " + val + " to "+s2.getName());
			s2.Write(val);
			mutex2.V();

			mutex1.P();
			val = (s1.Read() + 1) % 2;
			System.out.println("Writer" + ID + " is writing " + val + " to "+s1.getName());
			s1.Write(val);
			mutex1.V();
		}
	}
}
