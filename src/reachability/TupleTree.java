package reachability;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class represents all the missing tuples with respect to a
 * group of parameters in a tree structure.  This structure allows
 * membership check in a constant time.
 *
 * @author <a href="mailto:ylei@cse.uta.edu"> Yu Lei </a>
 * @version 1.0
 */
public class TupleTree {
  // the group of parameters
  ArrayList params;

  // the root of the tree
  TupleTreeNode root;

  // the head of the chain of uncovered nodes
  TupleTreeNode head;
    
  // the first value of the last parameter that has missing tuples
  int firstMissingValue;
  
  // the following two variables are used to iterate through all the
  // missing tuples
  int currValue;
  TupleTreeNode currNode;

  /**
   * Creates a new <code>TupleTree</code> instance.
   *
   * @param params the parameter group
   */
  public TupleTree (ArrayList params) {
    this.params = params;

    // construct the tuple tree
    init ();
  }

  private void init () {
    if (TestGenProfile.instance().getDOI () == 1) {
      Parameter param = (Parameter) params.get(0);
      head = new TupleTreeNode (-1, param.getDomainSize());

      TupleTreeNode pseudo = new TupleTreeNode (-1);

      for (int i = 2; i < param.getDomainSize (); i ++) {
	// forward link
	head.setKid (i, pseudo);
      }

      root = head;
    }
    else { 
      // create the root node
      root = new TupleTreeNode (-1, 
				((Parameter) params.get(0)).getDomainSize()); 

      // bootstrap
      ArrayList currLevel = new ArrayList (1);
      currLevel.add(root);
    
      for (int level = 0; level < params.size(); level ++) {
	Parameter param = (Parameter) params.get(level);
	if (level == params.size () - 1) {
	  // create the head node for missing tuples
	  head = new TupleTreeNode (-1, param.getDomainSize());
	  
	  // keep the left sibling node, which is used to establish the
	  // chain of uncovered nodes 
	  TupleTreeNode sibling = null;
	
	  for (int j = 0; j < currLevel.size(); j ++) {
	    TupleTreeNode it = (TupleTreeNode) currLevel.get(j);
	    for (int i = 1; i < param.getDomainSize (); 
		 i ++) {
	      if (sibling == null) {
		// forward link
		head.setKid (i, it);
		// backward linke
		it.setKid (2 * i + 1, head);
	      }
	      else {
		// forward link
		sibling.setKid (2 * i, it);
		// backward link
		it.setKid (2 * i + 1, sibling);
	      } 
	    }
	    sibling = it;
	  }
	}
	else {
	  int numOfKids = 
	    ((Parameter) params.get(level + 1)).getDomainSize();
	  // keep the nodes in the current level
	  ArrayList nextLevel = 
	    new ArrayList (currLevel.size() *
			   param.getDomainSize());  

	  for (int j = 0; j < currLevel.size (); j ++) {
	    TupleTreeNode it = (TupleTreeNode) currLevel.get(j);
	    for (int i = 0; i < param.getDomainSize(); i ++) {
	      TupleTreeNode node = null;
	      if (level < params.size () - 2) {
		node = new TupleTreeNode (i, numOfKids);
	      }
	      else {
		// the lowest level maintains a
		// double link list for uncovered nodes 
		node = new TupleTreeNode (i, 2 * numOfKids);
	      }
	      // establish the child/parent link
	      it.setKid (i, node);
	      node.setDad (it);
	      
	      // add node into the current level
	      nextLevel.add(node);
	    }
	  }

	  // nextLevel becomes currLevel
	  currLevel = nextLevel;
	}
      }
    }
   
    firstMissingValue = 0;
    currValue = 0;
    currNode = head.getKid(currValue);
  }


  /**
   * Look up a tuple in the tree. 
   * <p>
   * Note that the tuple must have the same parameters as the tree and
   * must appear in the same order.
   *
   * @param tuple the <code>Tuple</code> to be looked up
   * @return if found, the leaf node in the tree that represents the
   * tuple; otherwise, null.
   */
  public TupleTreeNode lookup (Tuple tuple) {
    TupleTreeNode rval = null;

    if (tuple.getNumOfPairs() == 1) {
      rval = root.getKid(tuple.getPair (0).value);
    }
    else {
      Iterator it = tuple.getIterator ();
      LinkedList nodes = new LinkedList ();
      nodes.add(root);
      int numOfPairs = tuple.getNumOfPairs ();
      // the last pair does not really count
      for (int i = 0; i < numOfPairs - 1; i ++) {
	int value = tuple.getPair(i).value;

	// the number of nodes in the current level
	int numOfNodes = nodes.size ();
	for (int j = 0; j < numOfNodes; j ++) {
	  TupleTreeNode node = (TupleTreeNode) nodes.removeFirst();
	  if (value >= 0) {
	    nodes.add(node.getKid (value));
	  }
	  //else {
	  // add all the kids into the next level
	  //int numOfKids = node.getNumOfKids ();
	  //for(int k = 0; k < numOfKids; k ++) {
	  //nodes.add(node.getKid (k)); 
	  //}
	}
      }
  
      int lastIndex = tuple.getPair(tuple.getNumOfPairs() - 1).value;
    
      // check if the leaf node is marked as covered or not
      for (int i = 0; i < nodes.size(); i ++) {
	TupleTreeNode node = (TupleTreeNode) nodes.get(i);
	if (node.getKid (2 * lastIndex) != null
	    || node.getKid (2 * lastIndex + 1) != null) {
	  rval = node;
	  break;
	} 
      }
      /*
	if (rval != null) {
	TupleTreeNode node = rval;
      
	// change don't care values if necessary
	if (tuple.hasDontCares ()) {
	for (int i = numOfPairs - 2; i >= 0; i --) {
	PVPair pair = tuple.getPair (i);
	if (pair.value == TestSet.DONT_CARE) {
	pair.value = node.getValue ();
	}
	node = node.getDad ();
	}
	}
	}
      */
    }
    
    return rval;
  }

  /**
   * Mark a tuple as covered.
   *
   * @param tuple the tuple
   * @return true if the tuple was not already covered
   */
  public boolean setCovered (Tuple tuple) {
    boolean rval = false;

    if (tuple.getNumOfPairs () == 1) {
      head.setKid (tuple.getPair(0).value, null);
      return true;
    }

    TupleTreeNode node = lookup (tuple);
    if (node != null) {
      int lastIndex = 
	tuple.getPair(tuple.getNumOfPairs() - 1).value;
      TupleTreeNode succ = node.getKid(2 * lastIndex);
      TupleTreeNode prev = node.getKid(2 * lastIndex + 1);
      if (prev == head) {
	prev.setKid (lastIndex, succ);
      }
      else {
	prev.setKid(2 * lastIndex, succ);
      }
      if (succ != null) {
	succ.setKid(2 * lastIndex + 1, prev);
      }
      node.setKid(2 * lastIndex, null);
      node.setKid(2 * lastIndex + 1, null);

      // change don't care values if necessary
      //      if (tuple.hasDontCares ()) {
      //	int numOfPairs = tuple.getNumOfPairs ();
      //	for (int i = numOfPairs - 2; i >= 0; i --) {
      //	  PVPair pair = tuple.getPair (i);
      //	  if (pair.value == TestSet.DONT_CARE) {
      //	    pair.value = node.getValue ();
      //	  }
      //	  node = node.getDad ();
      //	}
      //}

      // set the return flag
      rval = true;
    }
    return rval;
  }

  /**
   * Get the next tuple that has not been covered yet.
   *
   * @return the next missing tuple
   */
  public Tuple getNextMissingTuple () {
    Tuple rval = null;
    Parameter lastParam = (Parameter) params.get (params.size() - 1);
    while (firstMissingValue < lastParam.getDomainSize ()) {
      TupleTreeNode node = head.getKid(firstMissingValue);
      if (node == null) {
	firstMissingValue ++;
      }
      else {
	rval = new Tuple ();
	if (root != head) {
	  for (int i = params.size () - 2; i >= 0; i --) {
	    Parameter param = (Parameter) params.get(i);
	    PVPair pair = new PVPair (param, node.getValue());
	    rval.addPair(pair);
		    
	    // move up the tree
	    node = node.getDad ();
	  }
	}
	// add the tuple for the last parameter
	rval.addPair(new PVPair (lastParam,
				 firstMissingValue));
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
    // break the links as necessary
    TupleTreeNode curr = head.getKid (firstMissingValue);
    if (curr.getValue () == -1) {
      // 1-wise testing
      head.setKid (firstMissingValue, null);
      firstMissingValue ++;
    }
    else {
      TupleTreeNode next = curr.getKid (2 * firstMissingValue);
      head.setKid (firstMissingValue, next);
      curr.setKid (2 * firstMissingValue, null);
      curr.setKid (2 * firstMissingValue + 1, null);
      if (next != null) {
	next.setKid (2 * firstMissingValue + 1, head);
      }

      // check if we covered all the missing tuples for this missing
      // value 
      if (head.getKid (firstMissingValue) == null) {
	firstMissingValue ++;
      }
    }
  }

  /**
   * Check if the entire tree has been covered or not.
   *
   * @return null if the tree has been covered or the next missing
   * tuple otherwise.
   */
  public Tuple isCovered () {
    Tuple rval = null;
    if (head != null) {
      rval = getNextMissingTuple ();
    }
    return rval;
  }

  public void initializeIterator () {
    currValue = 0;
    currNode = head.getKid(currValue);
  }

  public Tuple nextTuple () {
    Tuple rval = null;
    Parameter lastParam = (Parameter) params.get (params.size() - 1);
    while (currNode == null && 
	   currValue < lastParam.getDomainSize () - 1) { 
	currValue ++;
	currNode = head.getKid (currValue);
    }

    if (currNode != null) {
      TupleTreeNode node = currNode;
      rval = new Tuple ();
      if (root != head) {
	for (int i = params.size () - 2; i >= 0; i --) {
	  Parameter param = (Parameter) params.get(i);
	  PVPair pair = new PVPair (param, node.getValue());
	  rval.addPair(pair);
		    
	  // move up the tree
	  node = node.getDad ();
	}
      }

      // add the tuple for the last parameter
      rval.addPair(new PVPair (lastParam, currValue));

      if (currNode.getValue () == -1) {
	// 1-wise testing
	currValue ++;
	if (currValue < lastParam.getDomainSize()) {
	  currNode = head.getKid (currValue);
	}
	else {
	  currNode = null;
	}
      }
      else {
	// advance the currNode
	currNode = currNode.getKid (2 * currValue);
      }
    }

    return rval;
  }

  // print out the tree in a breadth-first manner
  public String toString () {
    StringBuffer rval = new StringBuffer ();

    for (int i = 0; i < params.size(); i ++) {
      Parameter param = (Parameter) params.get(i);
      rval.append(param);
    }
    rval.append("\n");

    LinkedList que = new LinkedList ();
    que.add(root);

    /*
      while (!que.isEmpty()) {
      TupleTreeNode node = que.getFirst();
      if (node != null) {
      rval.append(node.getValue()).append(" ");
      for (int i = 0; i < node.getNumOfKids (); i ++) {
      que.add(node.getKid(i));
      }
      }
		
      // remove the current node
      que.removeFirst();
      }
    */
    // print out missing nodes
    Parameter lastParam = (Parameter) params.get(params.size() - 1);
    for (int i = 0; i < lastParam.getDomainSize (); i ++) {
      rval.append ("Missing tuples with (" + i + "):");
      if (root == head) {
	// 1-wise testing
	if (head.getKid(i) != null) {
	  rval.append(i + " ");
	}
      }
      else {
	TupleTreeNode node = head;
	while (node != null) {
	  rval.append (node.getValue ()).append(" ");
	  if (node == head) {
	    node = node.getKid(i);
	  }
	  else {
	    node = node.getKid(2 * i);
	  }
	}
	rval.append ("\n");
      }
    }

    return rval.toString ();
  }
}
