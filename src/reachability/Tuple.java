package reachability;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class represents a tuple, which is a list of <code>PVPair<code>s.
 *
 * @author <a href="mailto:ylei@cse.uta.edu"> Yu Lei </a>
 * @version 1.0
 */
public class Tuple {
  private ArrayList pairs;
  private int dimension;
    
  /**
   * Creates a new <code>Tuple</code> instance.
   * <p>
   * by default, # of pairs in a tuple equals DOI
   */
  public Tuple () {
    this.dimension = TestGenProfile.instance().getDOI();
    pairs = new ArrayList (dimension);
  }

  /**
   * Creates a new <code>Tuple</code> instance with a given
   * dimension. 
   *
   * @param dimension the dimension
   */
  public Tuple (int dimension) {
    this.dimension = dimension;
    pairs = new ArrayList (dimension);
  }

  /**
   * Add a PVPair to the tuple.
   * <p>
   * Note that the pairs are ordered so that tuple equality can be
   * checked quickly.
   * @param pair a <code>PVPair</code> value
   */
  public void addPair (PVPair pair) {
    if (pairs.size () == dimension) {
      Util.abort("Tuple Overflows!");
    }
    int i = pairs.size ();
    for (; i > 0; i --) {
      PVPair tmp = (PVPair) pairs.get(i - 1);
      if(pair.param.getID() > tmp.param.getID() ) {
	break;
      }
    }
    pairs.add(i, pair);
  }

  /**
   * Check whether a tuple has DONT_CARE values.
   *
   * @return true if it contains one or more DONT_CARE values
   */
  public boolean hasDontCares () {
    boolean rval = false;
    for (int i = 0; i < pairs.size (); i ++) {
      PVPair pair = (PVPair) pairs.get(i);
      if (pair.value == TestSet.DONT_CARE) {
	rval = true;
	break;
      }
    }
    return rval;
  }

  /**
   * Get a pair in the tuple.
   *
   * @param index the pair index.
   * @return a <code>PVPair</code>
   */
  public PVPair getPair (int index) {
    return (PVPair) pairs.get(index);
  }

  /**
   * Get the number of pairs in the tuple
   *
   * @return the number of pairs
   */
  public int getNumOfPairs () {
    return pairs.size ();
  }

  /**
   * Get the iterator of pairs.
   *
   * @return the iterator.
   */
  public Iterator getIterator () {
    return pairs.iterator ();
  }

  /**
   * Check equality.
   *
   * @param other another <code>Tuple</code>
   * @return <code>true</code> if two pairs equal
   */
  public boolean equals (Tuple other) {
    boolean rval = true;

    Iterator selfIt = getIterator ();
    Iterator otherIt = other.getIterator ();
    while (selfIt.hasNext() && otherIt.hasNext()) {
      if (!((PVPair) selfIt.next()).equals(((PVPair) otherIt.next()))) {
	rval = false;
	break;
      }
    }

    return rval;
  } 

  public String toString () {
    StringBuffer rval = new StringBuffer ();
    rval.append("[");
    Iterator it = pairs.iterator ();
    boolean first = true;
    while (it.hasNext ()) {
      if (!first) {
	rval.append(", ");
      }
      else {
	first = false;
      }
      rval.append(it.next());
    }
    rval.append("]");

    return rval.toString ();
  }
}
