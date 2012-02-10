package test.read_write_lock;

import java.util.concurrent.Semaphore;

/**
 * A ReadWrite Lock that writers have first priority
 * @author yenjung
 *
 */
public class ReadWriteLock {
	final Semaphore mutexR1 = new Semaphore(1); // make new readers wait for waiting writers
	final Semaphore mutexR2 = new Semaphore(1); // mutex to readers
	final Semaphore mutexW = new Semaphore(1);  // mutex to writers
	
	final Semaphore readLock = new Semaphore(1);
	final Semaphore writeLock = new Semaphore(1);
	
	int readers = 0;
	int writers = 0;
	
	public void beginRead() {
		try {
			mutexR1.acquire();
			readLock.acquire();
			mutexR2.acquire();
				if (readers == 0) writeLock.acquire();
				++readers;
			mutexR2.release();
			readLock.release();
			mutexR1.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void endRead() {
		try {
			mutexR2.acquire();
			--readers;
			if (readers == 0) writeLock.release();
			mutexR2.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void beginWrite() {
		try {
			mutexW.acquire();
			if (writers == 0) readLock.acquire();
			++writers;
			mutexW.release();
			writeLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void endWrite() {
		try {
			writeLock.release();
			mutexW.acquire();
			--writers;
			if (writers == 0) readLock.release();
			mutexW.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
