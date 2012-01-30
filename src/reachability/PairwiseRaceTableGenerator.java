package reachability;

import java.util.ArrayList;

public class PairwiseRaceTableGenerator {
  private RaceTable table;

  public PairwiseRaceTableGenerator (RaceTable table) {
    this.table = table;
  }

  public RaceTable getRaceTable () {
    return table;
  }

  public void build () {
    // creat the input parameter model
    ArrayList params = buildIPM ();

    if (params.size() > 0) {
      // create IPO engine
      IpoEngine engine = new IpoEngine (params, this);
      
      // build a pairwise test set
      engine.build ();
      
      // add the test set into the race table
      TestSet ts = engine.getTestSet ();
      ArrayList matrix = ts.getMatrix();
      int count = 0;
      for (int i = 0; i < matrix.size(); i ++) {
	int[] row = (int[]) matrix.get (i);
	//if (check (row)) {
	  for (int j = 0; j < row.length; j ++) {
	    if (row[j] >= 0) {
	      row[j] -= 1;
	    }
	  }
	  table.addIndex (row);
	  //}
	  //else {
	  //count ++;
	  //}
      }
    }
  }

  // build the input parameter model
  private ArrayList buildIPM () {
    ArrayList rval = new ArrayList (table.getNumOfHeadingEvents ());
    for (int i = 0; i < table.getNumOfHeadingEvents (); i ++) {
      Parameter param = new Parameter ("P" + i);
      param.setID (i);
      rval.add(param);
      Event recv = table.getHeadingEvent (i);
      int domainSize = recv.getSizeOfRaceSet ();
      for (int j = 0; j <= domainSize + 1; j ++) {
	param.addValue (Integer.toString (j - 1));
      }
    }
    return rval;
  }

  // convenience method
  public boolean check (int[] index, int pos) {
    return check (index, pos, pos);
  }

  // check constraints
  public boolean check (int[] index, int pos, int range) {
    boolean rval = true;
    Event recv = table.getHeadingEvent (pos);

    if (index[pos] == TestSet.DONT_CARE) {
      // we do not check DONT_CARE values
      return true;
    }

    if (index[pos] - 1 == -1) {
      if (isExistable (recv, index, pos)) {
	rval = false;
	return rval;
      }
    }

    if (index[pos] - 1 >= 0) {
      if (!isExistable (recv, index, pos)) {
	rval = false;
      }
      else {
	if (index[pos] - 1 > 0) {
	  Event send = recv.getRaceEvent (index[pos] - 2);
	  if (!isExistable (send, index, range)) {
	    rval = false;
	  }
	  else {
	    for (int i = 0; i < range; i ++) {
	      if (index[i] - 1 >= 0) {
		Event other_recv = table.getHeadingEvent (i);
		if (recv.isInPrimeStruct (other_recv)) {
		  rval = false;
		  break;
		}
		else if (index[i] - 1 > 0) { 
		  Event other_send = 
		    other_recv.getRaceEvent(index[i] - 2);
		  if (recv.isInPrimeStruct (other_send)) {
		    rval = false;
		    break;
		  }
		}
	      }
	    }
	  }
	}
      }
    }
      
    return rval;
  }

  public boolean localCheck (int[] index, int length) {
    boolean rval = true;
    for (int j = 0; j < length; j ++) {
      if (!check (index, j, length)) {
	rval = false;
	break;
      }
    }
    return rval;
  }

  public boolean check (int[] index) {
    boolean rval = false;
    for (int i = 0; i < index.length; i ++) {
      if (index[i] - 1 > 0) {
	rval = true;
	break;
      }
    }
    if (rval) {
      for (int j = 0; j < index.length; j ++) {
	if (!check (index, j, index.length)) {
	  rval = false;
	  break;
	}
      }
    }

    return rval;
  }

  public boolean check (Tuple tuple) {
    boolean rval = true;
    boolean changed = false;
    for (int i = 0; i < tuple.getNumOfPairs (); i ++) {
      PVPair pair = tuple.getPair (i);
      Event recv = table.getHeadingEvent (pair.param.getID());
      if (pair.value - 1 == -1) {
	//if (firstRecv (recv)) {
	  rval = false;
	  break;
	  //}
      }
      else {
	if (pair.value - 1 >= 0) {
	    if (!isExistable (recv, tuple)) {
	      rval = false;
	      break;
	    }
	    else {
	      if (pair.value - 1 > 0) {
		changed = true;
		Event send = recv.getRaceEvent(pair.value - 2);
		if (!isExistable (send, tuple)) {
		  rval = false;
		  break;
		}
	      }
	    }
	}
      }
    }
    
    return changed && rval;
  }

  public boolean complement (int[] test) {
    boolean rval = true;
    for (int i = 0; i < test.length; i ++) {
      Event recv = table.getHeadingEvent (i);
      if (test[i] - 1 >= 0) {
	// then all the receives in the prime structure should be zero
	for (int j = 0; j < i; j ++) {
	  Event other_recv = table.getHeadingEvent (j);
	  if (other_recv.isInPrimeStruct (recv)) {
	    if (test[j] == TestSet.DONT_CARE) {
	      test[j] = 1;
	    }
	    else {
	      if (test[j] != 1) {
		rval = false;
		break;
	      }
	    }
	  }
	}
	if (rval) {
	  if (test[i] - 1 > 0) {
	    Event send = recv.getRaceEvent (test[i] - 2);
	    for (int j = 0; j < test.length; j ++) {
	      Event other_recv = table.getHeadingEvent (j);
	      if (other_recv.isInPrimeStruct (send)) {
		if (test[j] == TestSet.DONT_CARE) {
		  test[j] = 1;
		}
		else {
		  if (test[j] != 1) {
		    rval = false;
		    break;
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    
    return rval;
  }

  // for receive events, we only need to check up to the current pos,
  // as all the heading events are ordered
  private boolean isExistable (Event event, int[] index, int range) {
    boolean rval = true;
    for (int i = 0; i < range; i ++) {
      if (index [i] - 1 == -1 || index[i] - 1 > 0) {
	Event it = table.getHeadingEvent (i);
	if (it.isInPrimeStruct(event)) {
	  rval = false;
	  break;
	}
      }
    }
    return rval;
  }

  private boolean isExistable (Event event, Tuple tuple) {
    boolean rval = true;
    for (int i = 0; i < tuple.getNumOfPairs (); i ++) {
      PVPair pair = tuple.getPair (i);
      if (pair.value - 1 > 0) {
	Event recv = table.getHeadingEvent (pair.param.getID());
	if (recv.isInPrimeStruct(event)) {
	  rval = false;
	  break;
	}
      }
    }
    return rval;
  }

  private boolean firstRecv (Event recv) {
    boolean rval = true;
    for (int i = 0; i < table.getNumOfHeadingEvents(); i ++) {
      Event other = table.getHeadingEvent (i);
      if (other.isInPrimeStruct(recv)) {
	rval = false;
	break;
      }
    }
    return rval;
  }
}
