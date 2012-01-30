package reachability;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class encodes all the tuples involving a new parameter being
 * extended.  The tuples are organized in a two-level hierarchy.  The
 * first level is all the possible parameter combinations.  The second
 * level is a set of <code>TupleTree</code>s, one for each
 * combination.  In other words, all the tuples involving the same
 * parameter combination are stored in a <code>TupleTree</code>.
 *
 * @author <a href="mailto:ylei@cse.uta.edu"> Yu Lei </a>
 * @version 1.0
 */
public class TupleTreeGroup {
  private ArrayList params;
  private TupleTree[] trees;
    
  private int doi;

  private int exclude;
    
  // the first tree which has missing tuples
  private int firstMissingTree;

  // the count of missing tuples per value
  private int[] missingCounts;

  PairwiseRaceTableGenerator generator;

  private int currTree;

  /**
   * Creates a new <code>TupleTreeGroup</code> instance.
   *
   * @param params a parameter group
   * @param index the index of the new parameter
   */
  public TupleTreeGroup (ArrayList params, int index, int exclude,
			 PairwiseRaceTableGenerator generator) { 
    this.params = new ArrayList ();
    for (int i = 0; i <= index; i ++) {
      this.params.add(params.get(i));
    }

    this.exclude = exclude;
    this.generator = generator;

    init ();
  }

  public TupleTreeGroup (ArrayList params, int index,
			 PairwiseRaceTableGenerator generator) {
    this (params, index, -1, generator);
  }

  /**
   * Get the number of trees.
   *
   * @return the number of trees.
   */
  public int getNumOfTrees () {
    return trees.length;
  }

  private void init () {
    doi = TestGenProfile.instance().getDOI ();
    int numOfTrees = Combinatorics.nOutOfM (params.size() - 1, 
					    doi - 1);

    trees = new TupleTree [numOfTrees];

    firstMissingTree = 0;
    currTree = 0;

    // initialize the array of missing counts
    Parameter param = (Parameter) params.get(params.size() - 1);
    missingCounts = new int [param.getDomainSize()]; 
  }

  /**
   * Build the trees.
   *
   */
  public void build () {
    // 1. Generate all the possible parameter combinations
    ArrayList paramCombos = null;
    if (exclude == -1) {
      paramCombos = Combinatorics.getParamCombos (params.size() - 1,
						  doi - 1); 
    }
    else {
      paramCombos = Combinatorics.getParamCombos (params.size() - 1,
						  doi - 1, exclude); 
    }

    // count tuples
    int totalCountOfTuples = 0;

    for (int j = 0; j < paramCombos.size(); j ++) {
      int[] paramCombo = (int[]) paramCombos.get(j);
      ArrayList group = new ArrayList (doi);
      for (int i = 0; i < paramCombo.length; i ++) {
	if (paramCombo[i] == 1) {
	  group.add(this.params.get(i));
	}
      }

      // get the position where the parameter group should be
      // stored
      int index = Combinatorics.getIndex (group,
					  params.size() - 1);

      // add the last parameter
      group.add(params.get(params.size() - 1));

      // count the missing tuples
      totalCountOfTuples += getTotalCountOfTuples (group);

      TupleTree tree = new TupleTree(group);
      trees[index] = tree;
    }

    removeInvalidTuples ();

    //dump ();
    
    // initialize the missingCounts array
    for (int i = 0; i < missingCounts.length; i ++) {
      missingCounts[i] = totalCountOfTuples;
    }
  }


  /**
   * Check whether a tuple is covered or not.
   *
   * @param tuple the tuple
   * @return true if covered 
   */
  public boolean isCovered (Tuple tuple) {
    int index = getIndex (tuple);
    return trees[index].lookup (tuple) == null;
  }

  /**
   * Set a tuple as covered.
   *
   * @param tuple the tuple
   * @return true if the tuple was not already covered
   */
  public boolean setCovered (Tuple tuple) {
    boolean rval = false;
    int index = getIndex (tuple);
    if (trees[index].setCovered (tuple)) {
      // update the missingCounts array
      PVPair pair = tuple.getPair (tuple.getNumOfPairs() - 1);
      missingCounts[pair.value] --;
      rval = true;
    }
    return rval;
  }

  /**
   * Get the next tuple that is yet to be covered.
   *
   * @return the next tuple to be covered
   */
  public Tuple getNextMissingTuple () {
    Tuple rval = null;
    while (firstMissingTree < trees.length) {
      if (trees[firstMissingTree] != null) {
	rval = trees[firstMissingTree].getNextMissingTuple();
      }
      if (rval == null) {
	firstMissingTree ++;
      }
      else {
	break;
      }
    }
    return rval;
  }

  /**
   * Cover the next tuple.
   *
   */
  public void coverNextMissingTuple () {
    while (trees[firstMissingTree] == null) {
      firstMissingTree ++;
    }
    trees[firstMissingTree].coverNextMissingTuple ();
    if (trees[firstMissingTree].getNextMissingTuple() == null) {
      firstMissingTree ++;
    }
  }

  public void initializeIterator () {
    currTree = 0;
    for (int i = 0; i < trees.length; i ++) {
      trees[i].initializeIterator ();
    }
  }

  public Tuple nextTuple () {
    Tuple rval = null;
    while (currTree < trees.length
	    && (rval = trees[currTree].nextTuple()) == null) {
      currTree ++;
    }
    return rval;
  }

  /**
   * Check if all the tuples are covered or not.
   *
   * @return null if all the tuples are covered or an uncovered tuple
   * otherwise 
   */
  public Tuple isCovered () {
    Tuple rval = null;
    for (int i = 0; i < trees.length; i ++) {
      if (trees[i] != null) {
	rval = trees[i].isCovered ();
      }
      if (rval != null) {
	break;
      }
    }
    return rval;
  }

  private int getIndex (Tuple tuple) {
    ArrayList group = 
      new ArrayList (tuple.getNumOfPairs());
    for (int i = 0; i < tuple.getNumOfPairs () - 1; i ++) {
      group.add (tuple.getPair(i).param);
    }

    return Combinatorics.getIndex(group, params.size () - 1);
  }

  // this function may not be correct for merging
  private int getTotalCountOfTuples (ArrayList params) {
    int rval = 1;
    for (int i = 0; i < params.size(); i ++) {
      Parameter param = (Parameter) params.get(i);
      rval *= param.getDomainSize ();
    }
    return rval;
  }

  public int getCountOfMissingTuples (int value) {
    return missingCounts[value];
  }

  public int getValueWithMostMissingTuples () {
    int rval = TestSet.DONT_CARE;
    int max = 0;
    for (int i = 0; i < missingCounts.length; i ++) {
      if (max < missingCounts[i]) {
	max = missingCounts[i];
	rval = i;
      }
    }

    return rval;
  }

  public void removeInvalidTuples () {
    Tuple tuple = null;
    while ((tuple = nextTuple ()) != null) {
      if (!generator.check (tuple)) {
	setCovered (tuple);
      }
    }
  }

  public void dump () {
    Tuple tuple = null;
    initializeIterator ();
    while ((tuple = nextTuple ()) != null) {
      System.out.print (tuple);
    }
    System.out.println();
    initializeIterator ();
  }

  public String toString () {
    StringBuffer rval = new StringBuffer ();
    for (int i = 0; i < trees.length; i ++) {
      rval.append(trees[i]).append("\n");
    }
    return rval.toString();
  }
}
