package test.read_write_lock;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ReadWriteLockTest implements Runnable {
	MySet s;
	
	public ReadWriteLockTest(MySet s) {
		this.s = s;
	}
	
	public void run() {
		Random generator = new Random();
		
		int act = generator.nextInt(100);
		if (act < 70) {
			s.contains(generator.nextInt(100));
		} else if (act < 80) {
			s.size();
		} else if (act < 85) {
			s.isEmpty();
		} else{ 
			if (act < 96) {
				s.add(generator.nextInt(100));
			} else if (act < 99) {
				s.remove(generator.nextInt(100));
			} else {
				s.clear();
			}
		}
	}
	
	static public void main(String args[]) {
		ExecutorService pool = Executors.newCachedThreadPool();
		MySet s = new MySet();
		
		for (int i = 0; i < 100; ++i) {
			pool.submit(new ReadWriteLockTest(s));
		}
		pool.shutdown();
	}
}
