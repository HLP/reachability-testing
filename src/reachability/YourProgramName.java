package reachability;
public final class YourProgramName  {
	public static void main (String args[]) {
		Thread t1 = new Thread();
		Thread t2 = new Thread();
		t1.start(); t2.start();
		try {	// For reachability testing threads must complete before main() returns
			t1.join(); t2.join();
		} catch (InterruptedException e) {}
	}
}
