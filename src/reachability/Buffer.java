package reachability;
public interface Buffer {
    public int size ();
    public Object withdraw ();
    public boolean consumerIsWaiting(); // true if the thread calling withdraw is blocked
    public Object withdrawN();
    public void deposit (Object value);
}
