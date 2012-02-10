package test.read_write_lock;


import java.util.Random;

import reachability.TDThread;


public class ReadWriteLockTest {
	static public void main(String args[]) {
		StringBuffer sharedBuffer = new StringBuffer();
		ReadWriteLock lock = new ReadWriteLock();
		Reader reader = new Reader(lock, sharedBuffer);
		Writer writer = new Writer(lock, sharedBuffer);
		
		try {
			reader.start(); writer.start();
		    reader.join(); writer.join();
		} catch (InterruptedException e) { e.printStackTrace(); }
		
		System.out.println(sharedBuffer);
	}
}

abstract class ReaderWriter extends TDThread {
	static Random ran = new Random();
	static final int SLEEP_RANGE = 500;
	
	public ReaderWriter(ReadWriteLock lock, StringBuffer sb, String s) {
		super(s);
		this.lock = lock;
		this.sb = sb;
	}
	
	@Override
	public void run () {
		int session = lock.getTicket();
		this.sleep();
		beginSession();
		inSession(session);
		endSession();
	}
	
	void sleep() {
		try {
			Thread.sleep(1 + ran.nextInt(SLEEP_RANGE));
		} catch (InterruptedException e) { e.printStackTrace();	}
	}
	
	protected final ReadWriteLock lock;
	protected final StringBuffer sb;	
	protected abstract void beginSession();
	protected abstract void inSession(int session);
	protected abstract void endSession();
}

final class Reader extends ReaderWriter {
	public Reader(ReadWriteLock lock, StringBuffer sb) { super(lock, sb, "Reader"); }
	public void beginSession () { lock.beginRead(); }
	protected void inSession(int session) {
		sb.append(session + " Reader reads\n");
	}
	protected void endSession() { lock.endRead(); }
}

final class Writer extends ReaderWriter {
	public Writer(ReadWriteLock lock, StringBuffer sb) { super(lock, sb, "Writer"); }
	protected void beginSession() { lock.beginWrite(); }
	protected void inSession(int session) {
		sb.append(session + " Writer writes\n");
	}
	protected void endSession() { lock.endWrite(); }
}

