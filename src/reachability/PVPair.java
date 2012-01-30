package reachability;
/**
 * This class represents a parameter-value pair.
 *
 * @author <a href="mailto:ylei@cse.uta.edu">Yu Lei</a>
 * @version 1.0
 */
public class PVPair {
  // parameter
  public Parameter param;

  // the index of the value
  public int value;

  /**
   * Creates a new <code>PVPair</code> instance.
   *
   * @param param a <code>Parameter</code> object.
   * @param value the value of the parameter
   */
  public PVPair (Parameter param, int value) {
    this.param = param;
    this.value = value;
  }

  /**
   * Check whether two pairs are equal.
   *
   * @param other a <code>PVPair</code>.
   * @return <code>true</code> if the two pairs equal.
   */
  public boolean equals (PVPair other) {
    return param.getID () == other.param.getID () 
      && value == other.value; 
  }

  /**
   * Get a string representation.
   *
   * @return the string representation.
   */
  public String toString () {
    return "(" + param.getID () + ", " + value + ")";
  }
}
