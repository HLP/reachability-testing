package reachability;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class encodes the test set being generated.  The test set
 * contains a matrix in which each row represents a test, and each
 * column represents a parameter.  
 * <p> 
 * Note that the parameters may not appear in the same order as in the
 * input file in order to optimize the resultant test set.  
 *
 * @author <a href="mailto:ylei@cse.uta.edu"> Yu Lei </a>
 * @version 1.0
 */
public class TestSet {
  // uninitialized values or values that do not affect the test
  // coverage 
  public static final int DONT_CARE = -2;

  private ArrayList params;
  private ArrayList matrix;

  /**
   * Creates a new <code>TestSet</code> instance.
   *
   */
  public TestSet () {
    matrix = new ArrayList ();
  }

  public TestSet (ArrayList params) {
    this.params = params;
    matrix = new ArrayList ();
  }

  /**
   * Set the parameters.
   */
  public void setParams (ArrayList params) {
    this.params = params;
  }
  
  public ArrayList getParams () {
    return params;
  }

  /**
   * Get the number of parameters in the test set.
   *
   * @return the number of parameters.
   */
  public int getNumOfParams () {
    return params.size ();
  }

  /**
   * Get the parameter with a given index.
   *
   * @param index the parameter index
   * @return the parameter
   */
  public Parameter getParam (int index) {
    return (Parameter) params.get(index);
  }

  /**
   * Add an existing matrix into the test set.
   *
   * @param matrix an existing test matrix
   */
  public void addMatrix (ArrayList matrix) {
    for (int j = 0; j < matrix.size(); j ++) {
      int[] row = (int[]) matrix.get(j);
      int numOfColumns = 
	row.length < getNumOfParams() 
	? row.length : getNumOfParams (); 
      int[] test = new int [getNumOfParams()];
      Arrays.fill (test, DONT_CARE);
      for (int i = 0; i < numOfColumns; i ++) {
	test[i] = row[i];
      }
      this.matrix.add(test);
    }
  }

  public ArrayList getMatrix () {
    return matrix;
  }

  /**
   * Get the number of tests in the test set.
   *
   * @return the number of tests.
   */
  public int getNumOfTests () {
    return matrix.size ();
  }

  public int[] getTest (int index) {
    return (int[]) matrix.get(index);
  }

  /**
   * Get a value in the test set.
   *
   * @param row the row index
   * @param column the column index
   * @return the value
   */
  public int getValue (int row, int column) {
    return ((int[]) matrix.get(row))[column];
  }

  /**
   * Set a value in the test set.
   *
   * @param row the row index
   * @param column the column index
   * @param value the value to be set
   */
  public void setValue (int row, int column, int value) {
    ((int[]) matrix.get(row))[column] = value;
  }

  /**
   * Check whether a tuple can be covered by an existing test.
   *
   * @param row the row index
   * @param tuple the tuple
   * @return <code>true</code> if the tuple can be covered
   */
  public boolean isCompatible (int row, Tuple tuple) {
    boolean rval = true;
    int numOfPairs = tuple.getNumOfPairs ();
    for (int i = 0; i < numOfPairs; i ++) {
      PVPair pair = tuple.getPair (i);
      int column = pair.param.getID ();
      if  (((int[]) matrix.get(row))[column] != DONT_CARE
	   && ((int[]) matrix.get(row))[column] != pair.value) {
	rval = false;
	break;
      }
    }
    return rval;
  }

  /**
   * Change an existing test to cover a tuple.
   *
   * @param row the row index of the existing test
   * @param tuple the tuple to be covered
   */
  public void cover (int row, Tuple tuple) {
    int numOfPairs = tuple.getNumOfPairs ();
    for (int i = 0; i < numOfPairs; i ++) {
      PVPair pair = tuple.getPair (i);
      int column = pair.param.getID ();
      ((int[]) matrix.get(row))[column] = pair.value;
    }
  }

  /**
   * Add a new test to cover a given tuple.  All the remaining values
   * are set to DONT_CARE values.
   *
   * @param tuple the tuple to be covered.
   */
  public void addNewTest (Tuple tuple) {
    int [] test = new int [getNumOfParams()];
    Arrays.fill(test, DONT_CARE);
    matrix.add (test);
    cover (matrix.size () - 1, tuple); 
  }

  public int[] createNewTest (Tuple tuple) {
    int [] rval = new int [getNumOfParams()];
    Arrays.fill(rval, DONT_CARE);
    int numOfPairs = tuple.getNumOfPairs ();
    for (int i = 0; i < numOfPairs; i ++) {
      PVPair pair = tuple.getPair (i);
      int column = pair.param.getID ();
      rval[column] = pair.value;
    }
    return rval;
  }

  public void add (int[] test) {
    matrix.add(test);
  }

  public int getNumOfDifferences (int row1, int row2, int column) { 
    int rval = 0;
    for (int i = 0; i < column; i ++) {
      if (((int[]) matrix.get(row1))[i] != 
	  ((int[]) matrix.get(row2))[i]) {
	rval ++;
      }
    }
    return rval;
  }

  public int[] clone (int row) {
    int[] rval = new int [getNumOfParams()];
    for (int i = 0; i < rval.length; i ++) {
      rval[i] = getValue (row, i);
    }
    return rval;
  }

  public String toString () {
    StringBuffer rval = new StringBuffer ();
    // print out the matrix
    for (int i = 0; i < matrix.size(); i ++) {
      for (int j = 0; j < ((int[]) matrix.get(i)).length; j ++) {
	if (j > 0) {
	  rval.append(" ");
	}
	rval.append(((int[]) matrix.get(i))[j]);
      } 
      rval.append("\n");
    }

    return rval.toString ();
  }
}
