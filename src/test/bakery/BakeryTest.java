
package test.bakery;

import reachability.TDThread;
import reachability.sharedInteger;

/**
 * @author Yen-Jung
 *
 */
public class BakeryTest {
	static final int THREAD = 7;
	public static void main(String[] args) {
		sharedInteger CS = new sharedInteger(-1, "CS"); // CS
		Lock b = new Bakery(THREAD); // false warnings
		//Lock b = new SharedBakery(THREAD); // never terminate
		
		// threads
		TestThread[] t = new TestThread[THREAD];
		for (int i = 0; i < THREAD; ++i)
			t[i] = new TestThread(b, i, CS);
		// start
		for (int i = 0; i < THREAD; ++i) t[i].start();
		// join
		try { for (int i = 0; i < THREAD; ++i) t[i].join();
		} catch (InterruptedException e) {}
		System.out.println("Program ends.");
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

