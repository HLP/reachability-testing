package test;

import reachability.TDThread;
import reachability.binarySemaphore;
import reachability.sharedInteger;


public final class Writers {

	public static void main (String args[]) {
		sharedInteger s1 = new sharedInteger(0,"s1");
		sharedInteger s2 = new sharedInteger(0,"s2");
		binarySemaphore mutex1 = new binarySemaphore(1);
		binarySemaphore mutex2 = new binarySemaphore(1);
		Writer w1 = new Writer (s1,s2,1,mutex1,mutex2);
		Writer w2 = new Writer (s1,s2,2,mutex1,mutex2);
		w1.start(); 
		w2.start(); 
		try {
		    w1.join(); w2.join();
		}
 		catch (InterruptedException e) {}
	}
}

final class Writer extends TDThread {

	sharedInteger s1;
	sharedInteger s2;
	private int ID;
	private binarySemaphore mutex1;
	private binarySemaphore mutex2;

	Writer (sharedInteger s1, sharedInteger s2, int ID, binarySemaphore mutex1, binarySemaphore mutex2) {
 		this.s1 = s1; this.s2 = s2; this.mutex1 = mutex1; this.ID = ID; this.mutex2 = mutex2;
 	}

	public void run () {
		//System.out.println ("Writer # " + ID + "  Running");

        //for (int i = 0; i < 3; ++i) {
            int val = 0;
            if (ID == 1) {
                mutex1.P();
                val = (s1.Read() + 1) % 2;
                s1.Write(val);
                System.out.println("Writer # " + ID + " wrote " + val + " to "+s1.getName());
                mutex1.V();

                mutex2.P();
                val = (s2.Read() + 1) % 2;
                s2.Write(val);
                System.out.println("Writer # " + ID + " wrote " + val + " to "+s2.getName());
                mutex2.V();

                mutex1.P();
                val = (s1.Read() + 1) % 2;
                s1.Write(val);
                System.out.println("Writer # " + ID + " wrote " + val + " to "+s1.getName());
                mutex1.V();
            } else {
                mutex2.P();
                val = (s2.Read() + 1) % 2;
                s2.Write(val);
                System.out.println("Writer # " + ID + " wrote " + val + " to "+s2.getName());
                mutex2.V();

                mutex1.P();
                val = (s1.Read() + 1) % 2;
                s1.Write(val);
                System.out.println("Writer # " + ID + " wrote " + val + " to "+s1.getName());
                mutex1.V();

                mutex2.P();
                val = (s2.Read() + 1) % 2;
                s2.Write(val);
                System.out.println("Writer # " + ID + " wrote " + val + " to "+s2.getName());
                mutex2.V();
            }
        //}
	}
}

