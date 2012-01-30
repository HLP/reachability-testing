package reachability;
public class AppEvent {

  private int tid; // thread id

  private String label; // event label

  private vectorTimeStamp stamp; // event timestamp
  
  private Object obj;



  public AppEvent (int tid, String label, Object obj, vectorTimeStamp stamp) {

    this.tid = tid;

    this.label = label;

    this.stamp = stamp;
    
    this.obj = obj;

  }



  public int getThreadID () {

    return tid;

  }



  public String getLabel () {

    return label;

  }



  public vectorTimeStamp getTimeStamp () {

    return stamp;

  }
  
  public Object getObject () {

    return obj;

  }



  public boolean happenBefore (AppEvent another) {

    return stamp.lessThan (another.getTimeStamp ());

  }



  public boolean isConcurrent (AppEvent another) {

    return !happenBefore(another) && !another.happenBefore(this);

  }



  public String toString () {

    StringBuffer rval = new StringBuffer ();

    rval.append("AppEvent [");

    rval.append(tid);

    rval.append(", ");

    rval.append(label + ", ");

    rval.append(obj + ", ");

    rval.append(stamp);
    
    rval.append("]");

    return rval.toString ();

  }
}

