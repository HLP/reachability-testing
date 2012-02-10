package test.read_write_lock;

import java.util.HashSet;


public class MySet {
	HashSet<Integer> h;
	ReadWriteLock lock;
	
	public MySet() {
		h = new HashSet<Integer>();
		lock = new ReadWriteLock();
	}
	
	public boolean contains(int x) {
		lock.beginRead();
		System.out.println("(Read) contains:" + x);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try { return h.contains(x); }
		finally { lock.endRead(); }
	}
	
	public int size() {
		lock.beginRead();
		System.out.println("(Read) size");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try { return h.size(); }
		finally { lock.endRead();}
	}
	
	public boolean isEmpty() {
		lock.beginRead();
		System.out.println("(Read) isEmpty");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try { return h.isEmpty(); }
		finally { lock.endRead();}
	}
	
	public boolean add(int x) {
		lock.beginWrite();
		System.out.println("(Write) add:" + x);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try { return h.add(x); }
		finally { lock.endWrite(); }
	}
	
	public boolean remove(int x) {
		lock.beginWrite();
		System.out.println("(Write) remove:" + x);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try { return h.remove(x); }
		finally { lock.endWrite(); }
	}
	
	public void clear() {
		lock.beginWrite();
		System.out.println("(Write) clear");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try { h.clear(); }
		finally { lock.endWrite(); }
	}
}
