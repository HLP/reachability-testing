package reachability;
public class TimeHelper {
  private long start;

  private static TimeHelper helper = null;

  private TimeHelper () {
  }

  public static TimeHelper instance () {
    if (helper == null) {
      helper = new TimeHelper ();
    }
    return helper;
  }

  public void countDown () {
    start = System.currentTimeMillis ();
  }

  public float getDuration () {
    return ((float) (System.currentTimeMillis () - start)) / 1000;
  }
}
