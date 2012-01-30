package reachability;
import java.util.*;
import java.io.*;

public final class callerList implements Serializable {
	void setCallerList(ArrayList callerList) {this.callerList = callerList;}
	ArrayList getCallerList() {return callerList;}
	private ArrayList callerList;
	boolean contains(int callerID) { 
		return callerList.contains(new Integer(callerID));
	}
	public String toString() {
		StringBuffer stringCallerList = new StringBuffer("[");
		if (callerList != null) {
			int callerID;
			for (int i = 0; i<callerList.size(); i++) {
				callerID = ((Integer)callerList.get(i)).intValue();
				stringCallerList.append(callerID);
				stringCallerList.append(",");
			}
		}
		if (stringCallerList.length()>1)
			stringCallerList.setCharAt(stringCallerList.length()-1,']');
		else
			stringCallerList.append("]");
		return stringCallerList.toString();
	}
	public static void main (String args[]) {
		callerList L = null;
		L = new callerList();
		ArrayList x = new ArrayList();
		x.add(new Integer(1));
		x.add(new Integer(2));
		L.setCallerList(x);
		if (L.contains(1))
			System.out.println("contains 1");
		if (L.contains(2))
			System.out.println("contains 2");
		if (L.contains(3))
			System.out.println("contains 3");
		System.out.println(L);
		
		srEvent e = new srEvent();
		e.setInhibitedCallers(x);
		int sendingThread = 3;
		if (!e.getInhibitedCallers().contains(sendingThread))
			System.out.println("Caller 3 is not inhibited");
			
		callerList c = e.getInhibitedCallers();
		if (!(c.contains(sendingThread)))
		  System.out.println("Caller " + sendingThread + " is not inhibited");
	}
}
