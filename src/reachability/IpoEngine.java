package reachability;
import java.util.ArrayList;
import java.util.Random;

/**
 * This class implements the In-Parameter-Order algorithm for
 * generating covering arrays of n-way coverage, where n is referred
 * to the Degree Of Interaction or DOI.  In theory, DOI can be any
 * arbitrary number greater than or equal to 2 in theory; in practice,
 * DOI is limited by the resources available.
 *
 * @author <a href="mailto:ylei@cse.uta.edu">
 * @version 1.0
 */
public class IpoEngine {
  // array of parameters
  private ArrayList params;
  
  // the resulting covering array
  private TestSet ts;

  // degree of interaction
  private int doi;

  // a starter array that may be given
  private TestSet starter;

  // two test sets that need to be merged.
  private TestSet merger;
  private TestSet mergent;

  private Random random;

  boolean extend;

  private final static int FASTSWITCH = 10000;
  private final static int HISTORYSIZE = 2;

  private PairwiseRaceTableGenerator generator;
    
  /**
   * Creates a new <code>IpoEngine</code> instance.
   *
   * @param sut the configuration of the System Under Test (SUT). 
   */
  public IpoEngine (ArrayList params,
		    PairwiseRaceTableGenerator generator) {
    this.params = params;
    this.generator = generator;

    this.ts = new TestSet (params);
    doi = TestGenProfile.instance().getDOI ();

    // assign parameter IDs
    for (int i = 0; i < params.size (); i ++) {
      ((Parameter) params.get(i)).setID (i);
    }

    random = new Random ();
    extend = false;
  }

  /**
   * Get the resultant test set.
   *
   * @return the resultant <code>TestSet</code> object.
   */
  public TestSet getTestSet () {
    return ts;
  }

  // set a starter array
  public void setStarter (TestSet starter) {
    this.starter = starter;
  }

  public TestSet getStarter () {
    return starter;
  }

  public void setExistingTestSet (ArrayList matrix) {
    extend = true;
    ts.addMatrix (matrix);
  }

  /**
   * Build the test set for the System Under Test.
   *
   */
  public void build () {
    boolean progressOn = TestGenProfile.instance().isProgressOn ();

    int numOfCoveredParams = 0;

    int numOfTotalParams = ts.getNumOfParams ();

    // 1. construct initial test set
    if (extend) {
      numOfCoveredParams = 
	params.size () < doi - 1? params.size () : doi - 1;
    }
    ArrayList initMatrix = buildInitialMatrix ();
    //Util.dump(initMatrix);
    ts.addMatrix (initMatrix);
    numOfCoveredParams = 
      params.size () < doi ? params.size () : doi;
      
    if (TestGenProfile.instance().debug ()) {
      for (int i = 0; i < numOfCoveredParams; i ++) {
	System.out.println("Covering parameter: " +
			   ts.getParam(i).getName ()); 
      }
    }

    float expandTime = 0;
    float currExpandTime = 0;
    float growTime = 0;
    float currGrowTime = 0;
    float buildTime = 0;
    float currBuildTime = 0;

    // 2. cover the remaining parameters one by one
    for (int column = numOfCoveredParams; 
	 column < ts.getNumOfParams (); column ++) {
      if (TestGenProfile.instance().debug ()) {
	System.out.println("Covering parameter: " 
			   + ts.getParam (column).getName());
      }

      // 2.a: Construct missing tuple trees
      TupleTreeGroup trees = null;
      trees = new TupleTreeGroup (ts.getParams(), column, generator);

      if (TestGenProfile.instance().debug ()) {
	// start a timer
	TimeHelper.instance().countDown ();
      }

      trees.build ();

      if (TestGenProfile.instance().debug ()) {
	currBuildTime = TimeHelper.instance().getDuration ();
	buildTime += currBuildTime;
      }

      if (TestGenProfile.instance().debug ()) {
	// start a timer
	TimeHelper.instance().countDown ();
      }

      // 2.b: horizontal expansion
      expand (column, trees);

      if (TestGenProfile.instance().debug ()) {
	// get the duration
	currExpandTime = TimeHelper.instance().getDuration ();
	expandTime += currExpandTime;
      }
	
      if (TestGenProfile.instance().debug ()) {
	TimeHelper.instance().countDown ();
      }

      int numOfTests = 0; 
      if (TestGenProfile.instance().debug ()) {
	numOfTests = ts.getNumOfTests ();
      }

      // 2.c: vertical growth
      grow (column, trees);

      if (TestGenProfile.instance().debug ()) {
	currGrowTime = TimeHelper.instance().getDuration ();
	growTime += currGrowTime;
      }

      if (progressOn) {
	System.out.println();
      }

      if (TestGenProfile.instance().debug ()) {
	System.out.println("Tests Added: " +
			   (ts.getNumOfTests() - numOfTests));
      }
    }

    // remove DONT_CARE values
    carefy ();

    if (TestGenProfile.instance().debug ()) {
      System.out.println("\nTime for build tuple tree: " + buildTime);
      System.out.println("Time for horizontal expansion: " +
			 expandTime);
      System.out.println("Time for vertical growth: " + growTime);
    }
  }

  // build the initial matrix for the first doi parameters
  private ArrayList buildInitialMatrix () {
    ArrayList rval = new ArrayList ();

    int numOfParams = params.size () < doi ? params.size () : doi;
    ArrayList initialParams = new ArrayList (numOfParams);
    for (int i = 0; i < numOfParams; i ++) {
      initialParams.add(ts.getParam(i));
    }

    // remove invalid combinations
    ArrayList matrix = 
      Combinatorics.getValueCombos (initialParams);
    for (int j = 0; j < matrix.size (); j ++) {
      int[] row = (int[]) matrix.get(j);
      if (numOfParams == ts.getNumOfParams ()) {
	if (generator.check(row)) {
	  rval.add(row);
	}
      }
      else if (generator.localCheck(row, numOfParams)) {
	rval.add (row);
      }
    }

    return rval;
  }

  // expand the existing matrix to cover a new parameter
  private void expand (int column, TupleTreeGroup trees) {
    boolean progressOn = TestGenProfile.instance().isProgressOn();
    short hunit = TestGenProfile.instance().getHUnit ();

    Parameter param = ts.getParam(column);
    int domainSize = param.getDomainSize ();

    // count the number of appearances of each value
    // this count is used to break a tie between two values with the
    // same weight
    int[] appearances = new int [domainSize];
    
    // keep the indices for the last few rows for each value
    CyclicArray[] history = new CyclicArray [domainSize];

    // initialization
    for (int i = 0; i < domainSize; i ++) {
      appearances[i] = 0;
      history[i] = new CyclicArray (HISTORYSIZE);
    }

    // a condition to trigger the fast mode
    boolean fastSwitch = 
      Combinatorics.nOutOfM (column, doi - 1) > FASTSWITCH; 

    //int startIndex = column % param.getDomainSize ();

    // add one more value into each existing test
    for (int row = 0; row < ts.getNumOfTests (); row ++) {
      if (progressOn && (row % hunit == 0)) {
	System.out.print(".");
      }

      // check if there exists DONT_CARE values in the current row
      boolean hasDontCares = hasDontCares (row, column);
      
      // the chosen value
      int choice = TestSet.DONT_CARE;
      int maxWeight = -1;
      ArrayList maxTuples = null;
      
      // if all the tuples are already covered, we can just exit the
      // loop quickly
      if (trees.getNextMissingTuple () == null) {
	break;
      }

      int[] test = ts.getTest (row);
	    
      // keep track of the maximum weight
      for (int value = 0; value < param.getDomainSize (); value ++) {
	if (trees.getCountOfMissingTuples(value) > 0) {
	  // create a deep copy
	  int[] copy = new int [test.length];
	  for (int i = 0; i < test.length; i ++) {
	    copy[i] = test[i];
	  }
	  copy[column] = value;

	  // check constraints
	  if (generator.check(copy, column)) {
	    int currWeight;
	    ArrayList currTuples = null; 
		
	    // count the number of missing tuples covered by value
	    currTuples = getCoveredTuples (row, column, value,
					   trees, history[value], hasDontCares); 
	    currWeight = currTuples.size (); 

	    // the tie-break rule seems to work better when doi <= 4
	    if ((currWeight > maxWeight)
		|| (doi <= 4 
		    && choice != TestSet.DONT_CARE 
		    && currWeight == maxWeight
		    && appearances[value] < appearances[choice])) {
	      choice = value;
	      maxWeight = currWeight;
	      maxTuples = currTuples;
	    }
	  }
	}
      }
	
      if (choice != TestSet.DONT_CARE) {
	// set the chosen value in the i-th test for the new
	// parameter
	ts.setValue(row, column, choice);
	
	// mark tuples covered by this choice
	setCovered (row, maxTuples, trees, hasDontCares);
	
	// book keeping
	appearances[choice] ++;
	history[choice].add (row);
      }

      /*
      if (!generator.localCheck(ts.getTest(row), column)) {
	System.out.println("Invalid test in expansion: ");
	System.out.println("row: " + row + "column: " + column);
	System.out.println("choice: " + choice);
	Util.dump(ts.getTest(row));
      }
      */
    }
  }

  /**
   * Describe <code>getCoveredTuples</code> method here.
   *
   * @param row an <code>int</code> value
   * @param column an <code>int</code> value
   * @param value an <code>int</code> value
   * @param trees a <code>TupleTreeGroup</code> value
   * @return an <code>int</code> value
   */
  private ArrayList getCoveredTuples (int row, int column,
					     int value, 
					     TupleTreeGroup trees,
					     CyclicArray history,
					     boolean hasDontCares) {

    ArrayList rval = new ArrayList ();

    if (doi == 1) {
      Tuple tuple = new Tuple ();

      // add the current parameter
      tuple.addPair (new PVPair(ts.getParam(column), value)); 
	
      // check if this tuple is covered
      if (!trees.isCovered (tuple)) {
	// add the tuple to the return set
	rval.add(tuple);
      }
      return rval;
    }


    // "order" keeps the position of a parameter after being reordered
    int[] order = new int [column];
    ArrayList paramCombos = null;

    // find a history row that is most similar to the current row
    int minDiff = Integer.MAX_VALUE;
    int alike = -1;
    int size = history.size ();
    for (int i = 0; i < size; i ++) {
      int numOfDiffs = 
	ts.getNumOfDifferences (row, history.get(i), column); 
	if (numOfDiffs < minDiff) {
	  minDiff = numOfDiffs;
	  alike = history.get(i);
	}
    }
    
    // the parameters need to be reordered before we generate
    // parameter combinations
    int diffIndex = 0;
    int sameIndex = minDiff;
    
    if (alike != -1 && (column - minDiff) > doi - 1) {
      // reorder the parameters so that parameters with the same value
      // are placed in the end
	for (int i = 0; i < column; i ++) {
	  if (ts.getValue(row, i) == ts.getValue(alike, i)) {
	    order[i] = sameIndex ++;
	  }
	  else {
	    order[i] = diffIndex ++;
	  }
	}
	
	// Generate combinations - Combinations only involving
	// parameters which have the same number 
	paramCombos = Combinatorics.getParamCombos (column, doi - 1,
						    minDiff - 1); 
    }
    else {
      for (int i = 0; i < column; i ++) {
	order[i] = i;
      }
      
      paramCombos = Combinatorics.getParamCombos (column, doi - 1);
    }

    // create a copy of the row. this is needed as DONT_CARE values
    // need to be changed.
    int[] currTest = ts.clone (row);
    
    // 2. Count the number of new tuples which will be covered by
    // the combination
    for (int j = 0; j < paramCombos.size(); j ++) {
      int[] combo = (int[]) paramCombos.get(j);
      Tuple tuple = new Tuple ();

      boolean dontCares = false;
      // construct a tuple out of the combination
      for (int i = 0; i < column; i ++) {
	if (combo[order[i]] == 1) {
	  if (currTest[i] == TestSet.DONT_CARE) {
	    dontCares = true;
	    break;
	  }
	  else {
	    tuple.addPair (new PVPair(ts.getParam(i), currTest[i])); 
	  }
	}
      }

      if (!dontCares) {
	// add the current parameter
	tuple.addPair (new PVPair(ts.getParam(column), value)); 
	
	// check if this tuple is covered
	if (!trees.isCovered (tuple)) {
	  // add the tuple to the return set
	  rval.add(tuple);
	}
      }
    }

    return rval;
  }

  private int getMinDiff (int row, int column, int value, CyclicArray history) {
    int rval = Integer.MAX_VALUE;
    int size = history.size ();
    for (int i = 0; i < size; i ++) {
      int numOfDiffs = 
	ts.getNumOfDifferences (row, history.get(i), column); 
      if (numOfDiffs < rval) {
	rval = numOfDiffs;
      }
    }
    return rval;
  }

  private void setCovered (int row, ArrayList tuples,
			   TupleTreeGroup trees, boolean hasDontCares)
  {
    for (int i = 0; i < tuples.size(); i ++) {
      Tuple tuple = (Tuple) tuples.get(i);
      trees.setCovered (tuple);
      /*
      if (hasDontCares) {
	int numOfPairs = tuple.getNumOfPairs ();
	for (int i = 0; i < numOfPairs - 1; i ++) {
	  int column = tuple.getPair (i).param.getID ();
	  if (ts.getValue (row, column) == TestSet.DONT_CARE) {
	    ts.setValue (row, column, tuple.getPair(i).value);
	  }
	}
      }
      */
    }
  }

  private boolean hasDontCares (int row, int column) {
    boolean rval = false;
    for (int i = 0; i < column; i ++) {
      if (ts.getValue (row, i) == TestSet.DONT_CARE) {
	rval = true;
	break;
      }
    }
    return rval;
  }

  private void grow (int column, TupleTreeGroup trees) {
    //int startRow = ts.getNumOfTests ();
    int startRow = 0;
    Tuple tuple = null;
    
    // show progress
    int progress = 0;
    boolean progressOn = TestGenProfile.instance().isProgressOn ();
    int vunit = TestGenProfile.instance().getVUnit();

    while ((tuple = trees.getNextMissingTuple ()) != null) {
      boolean covered = false;
      for (int row = startRow; row < ts.getNumOfTests(); row ++) {
	//for (int row = ts.getNumOfTests() - 1; 
	// row >= startRow; row --) {
	if (ts.isCompatible (row, tuple)) {
	  // check if all the constraints are respected
	  int[] test = ts.getTest (row);
	  if (isCoverable (test, tuple)) {
	    ts.cover (row, tuple);
	    covered = true;
	    break;
	  }
	}
      }
      if (!covered) {
	int[] newTest = ts.createNewTest (tuple);

	if (!generator.check (newTest)) {
	  // complement the test if necessary to ensure it is a valid test
	  if (!generator.complement (newTest)) {
	    // if the test cannot be complemented, then the tuple cannot
	    // be covered
	    System.out.println("Impossible Tuple: " + tuple);
	  }
	}
	ts.add (newTest);
      }
      
      // advance the pointer to the next missing tuple
      trees.coverNextMissingTuple ();

      if (progressOn) {
	// show progress
	progress ++;
	
	if (progress % vunit == 0) {
	  System.out.print(".");
	}
      }
    }
  }

  private void carefy () {
    for (int i = 0; i < ts.getNumOfTests (); i ++) {
      int[] test = ts.getTest (i);
      if (!generator.check(test)) {
	System.out.println("Invalid test:");
	Util.dump (test);
      }
      else {
	for (int j = 0; j < test.length; j ++) {
	  if (test[j] == TestSet.DONT_CARE) {
	    Parameter param = ts.getParam(j);
	    boolean flag = false;
	    for (int k = 0; k < param.getDomainSize(); k ++) {
	      ts.setValue (i, j, k);
	      if (generator.check (test)) {
		flag = true;
		break;
	      }
	    }
	    if (!flag) {
	      System.out.println("Failure to carefy. " 
				 + "This should never happen.");
	      Util.dump (test);
	      System.exit(1);
	    }
	  }
	}
      }
    }
  }
 
  private boolean isCoverable (int[] test, Tuple tuple) {
    boolean rval = true;

    // make a deep copy
    int[] clone = new int [test.length];
    for (int i = 0; i < test.length; i ++) {
      clone[i] = test[i];
    }

    // try to cover the tuple on the clone
    int numOfPairs = tuple.getNumOfPairs ();
    for (int i = 0; i < numOfPairs; i ++) {
      PVPair pair = tuple.getPair (i);
      int column = pair.param.getID ();
      clone[column] = pair.value;
    }

    if (!generator.check (clone)) {
      rval = false;
    }

    return rval;
  }
}
