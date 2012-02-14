package reachability;
import java.io.*;
import java.util.*;

final class IntegerSpace {
// All Integers with value m share the same reference. Since Timestamps all start with
// 0 and are incremented, the space of values should be small.
	private int size;
	private final int increment = 50;
	private ArrayList space;
	public IntegerSpace(int initialSize) {
	// space must hold at least 1 Integer
		if (initialSize < 1)
			initialSize = 1;
		size = initialSize;
		space = new ArrayList(initialSize);
		// Thread IDs start at 0; space holds Integers in the range 0..size-1
		for (int i=0; i<size; i++)
			space.add(new Integer(i));
	}
	public Integer get(int i) {
	// return the Integer at position i, which holds the intValue() i.
	// assume i>=0
	    if (i<size) // space holds Integers in the range 0..size-1.
		return (Integer) space.get(i);
	    else { // expand the space in fixed size increments
		int oldSize = size;
		do {
		    size = size + increment;
		} while (i >= size); // the new range is 0..size-1

		for (int j=oldSize; j<size; j++)
		    space.add(new Integer(j)); // fill the expanded space
	
		return (Integer) space.get(i);
	    }
	}
}

final class TimestampRCSpace {
    private int size;
    private final int increment = 50;
    private ArrayList space;
    public TimestampRCSpace(int initialSize) {
	// space must hold at least 1 TimestampRCSpace
	if (initialSize < 1)
	    initialSize = 1;
	size = initialSize;
	space = new ArrayList(initialSize);
	// a timestampRC holds an integer clock value; 
	// space holds timestampRCs in the range 0..size-1
	for (int i=0; i<size; i++)
	    space.add(new timestampRC(i));
    }
    public timestampRC get(int i) {
	// return the timestampRC at position i, which holds the value i.
	// assume i>=0
	if (i<size) // space holds timestampRCs in the range 0..size-1.
	    return (timestampRC) space.get(i);
	else { // expand the space in fixed size increments
	    int oldSize = size;
	    do {
		size = size + increment;
	    } while (i >= size); // the new range is 0..size-1
	    for (int j=oldSize; j<size; j++)
		space.add(new timestampRC(j)); // fill the expanded space
	    return (timestampRC) space.get(i);
	}
    }
} 
		
final class timestampRC implements Serializable {
	public int clockValue;
	public timestampRC(int clockValue) {
		this.clockValue = clockValue;
	}

/* we shouldn't be cloning elements of the TimestampRCSpace */
	//public Object clone() {
	//	try {
	//		timestampRC t = (timestampRC) super.clone();
	//		t.clockValue = clockValue;
	//		return t;
	//	}
	//	catch (CloneNotSupportedException e) // can't happpen
	//		{return null; }
   //
   //}
		
}

public class vectorTimeStamp implements Serializable, Cloneable { 
    private HashMap vectorTS; // thread name and JKEventID pairs
    // space for 50 Integers that represent Thread IDS
    private static IntegerSpace integerSpace = new IntegerSpace(50);
    //space for 100 integer clock values
    private static TimestampRCSpace timestampRCSpace = new TimestampRCSpace(100);
 
     /*
     * The timestamps are stored in a hash table. Thread IDs are used as 
	  * keys and timeStamps are the associated timestamp values.
     */
    public void makeVectorTimeStamp(Timestamp t) { 
    // Timestamp is Jeff's RT class, not one above
      int dimension = t.getDimension();
     	for (int i = 0; i < dimension; i ++) {
     	  //vectorTS.put(new Integer(i),new timestampRC(t.getValue(i)));
     	  vectorTS.put(integerSpace.get(i),timestampRCSpace.get(t.getValue(i)));
     	}
     	//return v;
    }
    
    public boolean lessThan (vectorTimeStamp v) {
		//boolean rval = true;
		//if(dimension != stamp.getDimension ()) {
		//  //System.out.println ("The two timestamps are not comparable.");
		//    rval = false;
		//}
		/*else*/ if (isSame (v)) {
		    return false;
		}
		else {
		/*
			for (int i = 0; i < dimension; i ++) {
				if (values[i] > stamp.getValue(i)) {
		    		rval = false;
	    			break;
				}
			}
		*/	
			if ( v == null || v.getSize() == 0) {
			    return false;
			}

			if (getSize () > v.getSize ()) {
			    // this means that this timestamp has more knowledge than v
			    return false;
			} 

			/*
			Set s = v.getVectorTS().vectorTS.entrySet();
			Iterator i = s.iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry)(i.next());
				Integer ID = (Integer)e.getKey();
					int otherClockValue = ((timestampRC)e.getValue()).clockValue;
					timestampRC thisTimeStamp = (timestampRC)vectorTS.get(ID);
					//if (thisTimeStamp == null) {
					//return false; // no clockValue yet for this thread
					//}
					//else if (otherClockValue > thisTimeStamp.clockValue) {
					//	return false;
					//}
					if (thisTimeStamp != null && 
					  thisTimeStamp.clockValue > otherClockValue) {
					    return false;
					}
			}
			*/
			Set s = vectorTS.entrySet ();
			Iterator it = s.iterator ();
			while (it.hasNext ()) {
			    Map.Entry entry = (Map.Entry) (it.next());
			    Integer ID = (Integer) entry.getKey ();
			    int myClockValue = ((timestampRC) entry.getValue()).clockValue;
			    timestampRC otherTimeStamp = (timestampRC) v.vectorTS.get(ID);
			    if (otherTimeStamp == null) {
				return false;
			    }
			    else {
				if (myClockValue > otherTimeStamp.clockValue) {
				    return false;
				}
			    }
			}
		}
		return true;
    } 

    public vectorTimeStamp() { 
		// called by clone when copying contents of HashMap
		vectorTS = new HashMap(); 
    }

	public vectorTimeStamp(int ID) { 
	// called by TDthread to initialize vector TimeStamp for thread ID
		vectorTS = new HashMap(); 
		//vectorTS.put(new Integer(ID), new timestampRC(0));
		vectorTS.put(integerSpace.get(ID), timestampRCSpace.get(0));
	}
	
	public vectorTimeStamp(String stamp) {
	// used to create timestamps for debugging. 
	// Example: "1/1,2/1" : means T1's integer timestamp is 1 and T2's integer timestamp is 1
	vectorTS = new HashMap(); 
	StringTokenizer stampTokens = new StringTokenizer(stamp,",");
	while (stampTokens.hasMoreTokens()) {
		String token = stampTokens.nextToken();
		StringTokenizer tokenValues =  new StringTokenizer(token,"/");
		while (tokenValues.hasMoreTokens()) {
			int ID = Integer.parseInt(tokenValues.nextToken());
			int value = Integer.parseInt(tokenValues.nextToken());
			vectorTS.put(integerSpace.get(ID), timestampRCSpace.get(value));
		}
	}
	
	
	
	}
	
	public void reset() {
		vectorTS.clear();
	}
	// added by rhc for SBRT	
	public void reset(int ID) {
		vectorTS.clear();
		vectorTS.put(integerSpace.get(ID), timestampRCSpace.get(0));
	}

    // added by lei
    public int getSize () {
		return vectorTS.size ();
    }

	public Object clone(){
	// We can't simply clone HashMap because entries are not cloned. 
	// So create new table with new entries.
		vectorTimeStamp v = new vectorTimeStamp();
		Set s = vectorTS.entrySet();
		Iterator i = s.iterator();
		while (i.hasNext()) {
			Map.Entry e = (Map.Entry)(i.next());
			int ID = ((Integer)e.getKey()).intValue();
			int clockValue = ((timestampRC)e.getValue()).clockValue;
			//v.vectorTS.put(new Integer(ID),new timestampRC(clockValue));
			v.vectorTS.put(integerSpace.get(ID),timestampRCSpace.get(clockValue));
		}
		return v;
	}
	
   // compares the current vector timestamp with the passed in vector timestamp
	public boolean isSame(vectorTimeStamp v) {

		if (v == null || getSize () != v.getSize()) {
	    	return false;
		}
		Set s = v.getVectorTS().vectorTS.entrySet();
		Iterator i = s.iterator();
		while (i.hasNext()) {
			Map.Entry e = (Map.Entry)(i.next());
			Integer ID = (Integer)e.getKey();
			int otherClockValue = ((timestampRC)e.getValue()).clockValue;
			timestampRC thisTimeStamp = (timestampRC)vectorTS.get(ID);
			if (thisTimeStamp == null)
				return false; // no clockValue yet for this thread
			else if (otherClockValue != thisTimeStamp.clockValue)
				return false;
		}
		return true;
	}

    public boolean equals (vectorTimeStamp v) {
		return isSame (v);
    }
					
 
	// Returns a clone of internal hash table
		vectorTimeStamp getVectorTS() {
		return (vectorTimeStamp) this.clone();
	}


     // Returns the integer clock associated with the given thread ID
	int getIntegerTS(int ID) { 
		//timestampRC ts = ((timestampRC)vectorTS.get(new Integer(ID)));
		timestampRC ts = ((timestampRC)vectorTS.get(integerSpace.get(ID)));
		if (ts == null)
			//return -1;
			// Yenjung
			return 0;
		else
			return ts.clockValue;	
	}

   // Increments integer clock associated with given thread
   void updateIntegerTS(int ID) {
		//timestampRC ts = ((timestampRC)vectorTS.get(new Integer(ID)));
		timestampRC ts = ((timestampRC)vectorTS.get(integerSpace.get(ID)));
		if (ts != null) {
			//++ts.clockValue;
			int newClockValue = ts.clockValue;
			newClockValue ++;
			vectorTS.put(integerSpace.get(ID),timestampRCSpace.get(newClockValue)); 
			// overwrites old value
		}
	}


   // Updates integer clock of the given thread with the given clock value
   void setIntegerTS(int ID, int clockValue) {
		//timestampRC ts = (timestampRC)vectorTS.get(new Integer(ID));
		timestampRC ts = (timestampRC)vectorTS.get(integerSpace.get(ID));
		if (ts != null)
			//ts.clockValue = clockValue;
			vectorTS.put(integerSpace.get(ID),timestampRCSpace.get(clockValue)); 
			// overwrites old value
	}


   /*
    * Updates the current vector time stamp with the given timestamp.
    * If an entry is not present then adds to the current and
    * updates the entries with given vectorTS entries
	*/
    void updateVectorTS(vectorTimeStamp v) {
		Set s = v.getVectorTS().vectorTS.entrySet();
		Iterator i = s.iterator();
		while (i.hasNext()) {
			Map.Entry e = (Map.Entry)(i.next());
			Integer ID = (Integer)e.getKey();
	    	// If the clock value of event > than that of the event in
	    	// vectorTS with the same ID, replace the old event
	    	// with the new event.
			int otherClockValue = ((timestampRC)e.getValue()).clockValue;
			timestampRC thisTimeStamp = (timestampRC)vectorTS.get(ID);
			if (thisTimeStamp == null) {
				//vectorTS.put(ID,new timestampRC(otherClockValue));
				vectorTS.put(ID,timestampRCSpace.get(otherClockValue));
			}
			else if (otherClockValue > thisTimeStamp.clockValue) {
				// overwrites old value
				//vectorTS.put(ID,new timestampRC(otherClockValue)); 
				vectorTS.put(ID,timestampRCSpace.get(otherClockValue)); 
			}

		}
	}
	
	public String toString() {
		StringBuffer stringVectorTS = new StringBuffer("[");
		int clockValue;
		Set s = vectorTS.keySet();
		Integer m = (Integer) Collections.max(s);
		for (int i = 1; i<=m.intValue(); i++) {
			clockValue = getIntegerTS(i);
			if (clockValue == -1)
				stringVectorTS.append("0");
			else
				stringVectorTS.append(clockValue);
			stringVectorTS.append(",");
		}
		stringVectorTS.setCharAt(stringVectorTS.length()-1,']');
		return stringVectorTS.toString();
	}


public static void main (String args[]) {
	vectorTimeStamp v = new vectorTimeStamp("1/1,2/2");
	System.out.println(v); // displays [1,2]
	vectorTimeStamp vt = new vectorTimeStamp("2/1");
	System.out.println(vt); // displays [0,1]	
}

}



