package test.writers;

import reachability.TDThread;
import reachability.countingSemaphore;
import reachability.sharedInteger;

public final class Writers {

	public static void main (String args[]) {
		sharedInteger s1 = new sharedInteger(0,"s1");
		sharedInteger s2 = new sharedInteger(0,"s2");
		countingSemaphore mutex1 = new countingSemaphore(1, "mutex1");
		countingSemaphore mutex2 = new countingSemaphore(1, "mutex2");

		Writer w1 = new Writer (s1,s2,1,mutex1,mutex2);
		Writer w2 = new Writer (s1,s2,2,mutex1,mutex2);
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
