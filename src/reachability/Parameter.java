package reachability;
import java.util.ArrayList;

/**
 * This class represents a <code>parameter</code> in the SUT.  Note
 * that each parameter has a list of <code>string</code> values.
 * Also, each parameter has two IDs:
 * <ul>
 * <li> <code>id</code>: ID as given in the SUT.
 * <li> <code>activeID</code>: ID used in the test generation
 * process. 
 * </ul>
 *
 * @author <a href="mailto:ylei@cse.uta.edu">Yu Lei</a>
 * @version 1.0
 */
public class Parameter {
  private int id;
  private String name;
  private ArrayList values;

  /**
   * Creates a new <code>Parameter</code> instance.
   *
   * @param name the name of the parameter
   */
  public Parameter (String name) {
    this.name = name;

    values = new ArrayList ();
  }

  /**
   * Get the parameter name.
   *
   * @return the parameter name.
   */
  public String getName () {
    return name;
  }
    
  /**
   * Set the active ID of the parameter to be used in the test
   * generation. 
   *
   * @return an <code>int</code> value
   */
  public void setID (int id) {
    this.id = id;
  }

  /**
   * Get the active ID of the parameter as used in the test
   * generation. 
   *
   * @param activeID the active parameter ID.
   */
  public int getID () {
    return id;
  }

  /**
   * Add a value into the list of parameter values.
   *
   * @param value the value to be added.
   */
  public void addValue (String value) {
    values.add(value);
  }

  /**
   * Get the parameter value with a given index.
   *
   * @param index the value index.
   * @return the parameter value.
   */
  public String getValue (int index) {
    return (String) values.get(index);
  }

  /**
   * Get the number of values of this parameter.
   *
   * @return the number of values of this parameter.
   */
  public int getDomainSize () {
    return values.size ();
  }
    
  public String toString () {
    StringBuffer rval = new StringBuffer ();
    rval.append(name + ": [");
    for (int i = 0; i < values.size (); i ++) {
      if (i > 0) {
	rval.append(", ");
      }
      rval.append(values.get(i));
    }
    rval.append("]\n");

    return rval.toString();
  }
}
 
