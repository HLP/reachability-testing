package reachability;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class consists of a set of static methods for combinatorial
 * computation. 
 *
 * @author <a href="mailto:ylei@cse.uta.edu"></a>
 * @version 1.0
 */
public class Combinatorics {

  /**
   * Compute the number of combinations for n out of m, i.e., C(n, m).
   *
   * @param m an <code>int</code> value
   * @param n an <code>int</code> value
   * @return C(n, m), i.e., n out of m.
   */
  public static int nOutOfM (int m, int n) {
    int rval = 1;

    if (m == n) {
      rval = 1;
    } 
    else {
      for (int i = 0; i < n; i ++) {
	rval = rval * (m - i);
      }
      for (int i = 1; i < n; i ++) {
	rval = rval / (i + 1);
      }
    }

    return rval;
  }

  /**
   * This method returns a hash-key-like index for a parameter
   * combination (out of <code>m</code> parameters). The index is
   * used to quickly find a parameter combination out of a set of
   * combinations stored in an array, without having to compare and
   * search. 
   * <p>
   * The proper working of this method assumes that the
   * parameters are placed in the increasing order of their
   * IDs. That is, given a parameter combination <code>(P1, P2, ...,
   * Pn)</code>, <code>id(P1) < id(P2) < ... <
   * id(Pn)</code>. 
   *
   * @param params the parameter combination
   * @param m the total number of parameters
   * @return an <code>int</code> value
   */
  public static int getIndex (ArrayList params, int m) { 
    int rval = 0;

    int n = params.size ();
    // ID of the previous parameter
    int ppid = -1;
    for (int i = 0; i < n; i ++) {
      // ID of the current parameter
      int pid = ((Parameter) params.get(i)).getID ();
      if (pid > ppid + 1) {
	if (i == n - 1) {
	  rval += pid - ppid - 1;
	}
	else {
	  for (int j = m - pid; j <=  m - ppid - 2; j ++) {
	    rval += nOutOfM (j, n - i - 1);
	  }
	}
      }
      // pid becomes ppid
      ppid = pid;
    }

    return rval;
	
  }

  public static ArrayList getParamCombos (int m, int n) {
    return getParamCombos(m, n, -1);
  }

  public static ArrayList 
    getParamCombos (int m, int n, int first) {
    
    ArrayList rval = new ArrayList ();

    int[] index = new int [m];
    if (first != -1 && first <= m - n) {
      for (int i = 0; i < index.length; i ++) {
	if (i < first || (i > first && i < m - n + 1)) {
	  index[i] = 0;
	}
	else {
	  index[i] = 1;
	}
      }
    } else {
      for (int i = 0; i < index.length; i ++) {
	if (i < m - n) {
	  index[i] = 0;
	}
	else {
	  index[i] = 1;
	}
      }
    }

    // termination flag
    boolean exhausted = false;

    while (!exhausted) {
      // create a deep copy of index
      addCombo (rval, index);

      // find the next combo
      // 1. find the last zero followed by one and
      // count the ones before such a zero
      int pos = -1;
      int ones = 0;
      int count = 0;
      for (int i = 0; i < m - 1; i ++) {
	if (index[i] == 1) {
	  ones ++;
	}
	else if (index[i] == 0 && index[i + 1] == 1) {
	  pos = i;
	  count = ones;
	}
      }

      // stop if no zero is followed by one
      if (pos == -1) {
	exhausted = true;
      }
      else {
	// change the found zero to one
	index[pos] = 1;
	if (index[m - 1] == 1) {
	  // e.g. 001011 -> 001101
	  index[pos + 1] = 0;
	}
	else {
	  // e.g. 001100 -> 010001
	  for (int i = pos + 1; i < m; i ++) {
	    if (i <= m - n + count) {
	      index[i] = 0;
	    }
	    else {
	      index[i] = 1;
	    }
	  }		    
	}
      }
    }

    return rval;
  } 



  public static ArrayList getValueCombos (ArrayList params) {
    ArrayList rval = new ArrayList ();

    int[] index = new int [params.size()];
    Arrays.fill(index, 0);

    // termination flag
    boolean exhausted = false;

    while (!exhausted) {
      // create a deep copy of index
      addCombo (rval, index);

      // add 1 to index
      int i = index.length - 1;
      for (; i > 0; i --) {
	Parameter param = (Parameter) params.get(i);
	if (index[i] == param.getDomainSize () - 1) {
	  index[i] = 0;
	}
	else {
	  break;
	}
      }

      if (i == 0 && 
	  index[0] == ((Parameter) params.get(0)).getDomainSize () - 1) {
	exhausted = true;
      }
      else {
	index[i] ++;
      }
    }

    return rval;
  }

  private static void addCombo (ArrayList combos, int[] index) {
    int[] combo = new int [index.length];
    for (int i = 0; i < index.length; i ++) {
      combo[i] = index[i];
    }
    combos.add(combo);
  } 
}
