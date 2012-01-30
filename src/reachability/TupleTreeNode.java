package reachability;
import java.util.ArrayList;

/**
 * This class represents a node in the tuple tree.  Each node contains
 * a parameter value and a number of pointers.
 *
 * @author <a href="mailto:ylei@cse.uta.edu"> Yu Lei </a>
 * @version 1.0
 */
public class TupleTreeNode {
  private int value;
  private TupleTreeNode dad;
  private TupleTreeNode[] kids;

    
  /**
   * Creates a new <code>TupleTreeNode</code> instance.
   * <p>
   * NOTE: We may not need to store "value"
   *
   * @param value the parameter value
   * @param numOfKids the number of kids
   */
  public TupleTreeNode (int value, int numOfKids) {
    this.value = value;
    kids = new TupleTreeNode [numOfKids];
  }

  public TupleTreeNode (int value) {
    this.value = value;
    kids = null;
  }

  /**
   * Get the parameter value of the node.
   *
   * @return the value.
   */
  public int getValue () {
    return value;
  }

  /**
   * Set the parent.
   *
   * @param dad the parent.
   */
  public void setDad (TupleTreeNode dad) {
    this.dad = dad;
  }

  /**
   * Get the parent.
   *
   * @return the parent.
   */
  public TupleTreeNode getDad () {
    return dad;
  }

  /**
   * Set a child of the node.
   *
   * @param index the index of the child
   * @param kid the child node
   */
  public void setKid (int index, TupleTreeNode kid) {
    kids[index] = kid;
  } 

  /**
   * Get a child. 
   *
   * @param index the index of the child
   * @return the child
   */
  public TupleTreeNode getKid (int index) {
    return kids[index];
  }

  /**
   * Get the number of children.
   *
   * @return the number of children
   */
  public int getNumOfKids () {
    return kids.length;
  }

  public String toString () {
    return "Node (" + value + ")";  
  }
}
