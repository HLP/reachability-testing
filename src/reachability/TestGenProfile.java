package reachability;
/**
 * This class contains the profile of the test generation process.
 * The profile includes parameters such as degree of interaction, test
 * generation mode (starting from scratch or from an existing test
 * set) and test generation algorithms (IPO, or some other new
 * algorithms we may develop later.
 * <p>
 * These parameters are specified on the command line in the following
 * format: <code>-D<property name>=<property value></code>.  For
 * example, <code>-DDOI=3</code> specifies that degree of interaction
 * is 3. 
 *
 * @author <a href="mailto:ylei@cse.uta.edu">Yu Lei</a>
 * @version 1.0
 */
public class TestGenProfile {
  // available test generation modes
  //public static enum Mode {scratch, extend};
  public static final int SCRATCH = 0;
  public static final int EXTEND = 1;
  public static final int IPO = 2;
  public static final int IAC = 3;
  public static final int BUSH = 4;
  public static final int REC = 5;
  public static final int NUMERIC = 6;
  public static final int NIST = 7;
  

  // available core algorithms
  //  public static enum Algorithm {ipo, iac, bush, rec};

  //public static enum OutputFormat {numeric, nist};

  // constant strings used for property names and values
  public static final String PN_DOI = "t";

  // the following properties control the display
  public static final String PN_PROGRESS = "progress";
  public static final String ON = "on";
  public static final String OFF = "off";

  // the number of tests extended during horizontal growth per
  // progress dot 
  public static final String PN_HUNIT = "hunit";
  public static final short DEFAULT_HUNIT = 50;
  // the number of tuples covered during vertical growth per progress
  // dot 
  public static final String PN_VUNIT = "vunit";
  public static final short DEFAULT_VUNIT = 1000;

  // verify coverage after test generation
  public static final String PN_CHECK = "check";

  // debug info
  public static final String PN_DEBUG = "debug";

  // output format
  public static final String PN_OUTPUTFORMAT = "output";
  public static final String PV_NUMERIC = "numeric";
  public static final String PV_NIST = "nist";

  // random generation
  public static final String PN_RANDOM = "random";

  // degree of interaction (2 up to 6)
  private int doi;

  // output format
  private int output;

  // boolean flag indicating whether progress info should be displayed
  private boolean progress;
  private short hunit;
  private short vunit;

  // flag indicating whether coverage should be verified
  private boolean check;

  // debug flag
  private boolean debug;

  // random flag
  private boolean random;

  // keep a single instance of this class
  private static TestGenProfile profile = null; 

  /**
   * Creates a new <code>TestGenProfile</code> instance.
   * <p>
   * The constructor is private to implement the Singleton pattern. 
   */
  private TestGenProfile () {
  }

  /**
   * Get the single instance of <code>TestGenProfile</code>.
   *
   * @return the instance.
   */
  public static TestGenProfile instance () {
    if (profile == null) {
      profile = new TestGenProfile ();

      // set attributes in the profile
      profile.setDOI ();
      profile.setProgress ();
      profile.setHUnit ();
      profile.setVUnit ();
      profile.setCheckCoverage ();
      profile.setDebugMode ();
      profile.setRandomMode ();
      profile.setOutputFormat ();
    }
	    
    return profile;
  }

  /**
   * Get the degree of interaction.
   *
   * @return the degree of interaction.
   */
  public int getDOI () {
    return doi;
  }

  private void setDOI () {
    String prop = System.getProperty (PN_DOI);
    if (prop == null) {
      doi = 1;
    }
    else {
      try {
	doi = Integer.parseInt (prop);
	if (doi > 6 || doi < 1) {
	  System.out.println("Strength of coverage t must be in the range [1 .. 6].");
	  System.exit (1);
	}
      }
      catch (NumberFormatException ex) {
	System.out.println("Strength of coverage t must be an integer!");
	System.exit (1);
      }
    }
  }

  /**
   * Check if progress info should be displayed.
   *
   * @return true if progress info should be displayed.
   */
  public boolean isProgressOn () {
    return progress;
  }

  /**
   * Check if coverage should be verified after test generation.
   *
   * @return true if coverage should be verified.
   */
  public boolean checkCoverage () {
    return check;
  }

  public boolean debug () {
    return debug;
  }

  public boolean random () {
    return random;
  }

  public int getOutputFormat () {
    return output;
  }

  /**
   * Get the progress unit (in terms of the number of tests covered)
   * during horizontal growth.
   *
   * @return the horizontal progress unit.
   */
  public short getHUnit () {
    return hunit;
  }

  /**
   * Get the progress unit (in terms of the number of pairs covered)
   * during vertical growth.
   *
   * @return the vertical progress unit.
   */
  public short getVUnit () {
    return vunit;
  }

  private void setProgress () {
    String prop = System.getProperty (PN_PROGRESS);
    if (prop == null) {
      progress = false;
    }
    else if (prop.equals(ON)) {
      progress = true;
    }
    else if (prop.equals(OFF)) {
      progress = false;
    }
    else {
      Util.abort ("Invalid progress option!");
    }
  }

  private void setDebugMode () {
    String prop = System.getProperty (PN_DEBUG);
    if (prop == null) {
      debug = false;
    }
    else if (prop.equals(ON)) {
      debug = true;
    }
    else if (prop.equals(OFF)) {
      debug = false;
    }
    else {
      Util.abort ("Invalid debug mode option");
    }
  }

  private void setRandomMode () {
    String prop = System.getProperty (PN_RANDOM);
    if (prop == null) {
      random = false;
    }
    else if (prop.equals(ON)) {
      random = true;
    }
    else if (prop.equals(OFF)) {
      random = false;
    }
    else {
      Util.abort ("Invalid random mode option");
    }
  }

  private void setCheckCoverage () {
    String prop = System.getProperty (PN_CHECK);
    if (prop == null) {
      check = false;
    }
    else if (prop.equals(ON)) {
      check = true;
    }
    else if (prop.equals(OFF)) {
      check = false;
    }
    else {
      Util.abort ("Invalid check option!");
    }
  }

  private void setHUnit () {
    String prop = System.getProperty (PN_HUNIT);
    if (prop == null) {
      hunit = DEFAULT_HUNIT;
    }
    else {
      try {
	hunit = Short.parseShort (prop);
      }
      catch (NumberFormatException ex) {
	Util.abort ("hunit property must be an integer > 0.");
      }
    }
  }

  private void setVUnit () {
    String prop = System.getProperty (PN_VUNIT);
    if (prop == null) {
      vunit = DEFAULT_VUNIT;
    }
    else {
      try {
	vunit = Short.parseShort (prop);
      }
      catch (NumberFormatException ex) {
	Util.abort ("hunit property must be an integer > 0.");
      }
    }
  }

  private void setOutputFormat () {
    String prop = System.getProperty (PN_OUTPUTFORMAT);
    if (prop == null) {
      output = NIST;
    }
    else if (prop.equals(PV_NIST)) {
      output = NIST;
    }
    else if (prop.equals(PV_NUMERIC)) {
      output = NUMERIC;
    }
    else {
      Util.abort("Invalid output format!");
    }
  }

  public String toString () {
    StringBuffer rval = new StringBuffer ();
    rval.append("\nTest Generation Profile: \n");
    rval.append("===========================================\n");
    rval.append("Strength of Coverage: ").append(doi).append("\n");
    rval.append("Progress Info: ");
    if (progress) {
      rval.append("on\n");
      rval.append("Progress HUnit: ").append(hunit).append("\n");
      rval.append("Progress VUnit: ").append(vunit).append("\n");
    }
    else {
      rval.append("off\n");
    }
    rval.append("Debug mode: ");
    if (debug) {
      rval.append("on\n");
    }
    else {
      rval.append("off\n");
    }
    rval.append("Random mode: ");
    if (random) {
      rval.append("on\n");
    }
    else {
      rval.append("off\n");
    }
    rval.append("Verify Coverage: ");
    if (check) {
      rval.append("on\n");
    }
    else {
      rval.append("off\n");
    }
    return rval.toString();
  }
}
