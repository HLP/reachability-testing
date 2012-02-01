
package test;

import reachability.TDThread;
import reachability.sharedBoolean;
import reachability.sharedInteger;

/**
 * @author Yen-Jung
 *
 */
public class BakeryTest {
	public static void main(String[] args) {
		sharedInteger CS = new sharedInteger(-1, "CS"); // CS
		Lock b = new Bakery(2); // lock
		//Lock b = new SharedBakery(2); // lock
		
		// threads
		TestThread[] t = new TestThread[2];
		t[0] = new TestThread(b, 0, CS);
		t[1] = new TestThread(b, 1, CS);
		t[1].start(); t[0].start(); 
		try {
		    t[0].join(); t[1].join();
		}
 		catch (InterruptedException e) {}
		//System.out.println("Program ends: " + CS.Read());
	}
}

interface Lock {
	public void requestCS(int id);
	public void releaseCS(int id);
}

class Bakery implements Lock {
	final int N; // number of processes using this object
	boolean[] choosing;
	int[] number;

	public Bakery(int n) {
		this.N = n;
		choosing = new boolean[N];
		number = new int[N];
		for (int i = 0; i < N; ++i) {
			choosing[i]	= false;
			number[i] = 0;
		}
	}
	
	@Override
	public void requestCS(int id) {
		// step1
		choosing[id] = true;
		for (int j = 0; j < N; ++j) {
			if (number[j] > number[id]) {
				number[id] = number[j];
			}
		}
		number[id] = number[id] + 1;
		choosing[id]= false;
		
		// step2
		for (int j = 0; j < N; ++j) {
			while (choosing[j]) {System.out.println("");}; // process j in doorway
			while ((number[j] != 0) &&
					((number[j] < number[id]) ||
					((number[j] == number[id]) && j < id)))
			{
				System.out.print(""); // busy waiting
			}
		}
	}
	
	@Override
	public void releaseCS(int id) {
		number[id] = 0;
	}	
}

class SharedBakery implements Lock {
	final int N; // number of processes using this object
	sharedBoolean[] choosing;
	sharedInteger[] number;

	public SharedBakery(int n) {
		this.N = n;
		choosing = new sharedBoolean[N];
		number = new sharedInteger[N];
		for (int i = 0; i < N; ++i) {
			choosing[i]	= new sharedBoolean(false, "choosing" + i);
			number[i] = new sharedInteger(0, "number" + i);
		}
	}
	
	@Override
	public void requestCS(int id) {
		// step1
		choosing[id].Write(true);
		for (int j = 0; j < N; ++j) {
			if (number[j].Read() > number[id].Read()) {
				number[id].Write(number[j].Read());
			}
		}
		number[id].Write(number[id].Read() + 1);
		choosing[id].Write(false);
		
		// step2
		for (int j = 0; j < N; ++j) {
			while (choosing[j].Read()) ; // process j in doorway
			while ((number[j].Read() != 0) &&
					((number[j].Read() < number[id].Read()) ||
					((number[j].Read() == number[id].Read()) && j < id)))
			{
				; // busy waiting
			}
		}
	}
	
	@Override
	public void releaseCS(int id) {
		number[id].Write(0);
	}
}

class TestThread extends TDThread {
	final Lock b;
	final int id;
	sharedInteger CS;
	
	public TestThread(Lock b, int id, sharedInteger CS) {
		this.b = b;
		this.id = id;
		this.CS = CS;
	}
	
	@Override
	public void run() {
		for (int i = 0; i < 100; ++i) {
			b.requestCS(id);
			CS.Write(id);
			System.out.println(id + " writes " + CS.Read());
			b.releaseCS(id);
		}
	}
	
}
