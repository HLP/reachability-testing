package reachability;

public class TDThread implements propertyParameters {
	protected propertyParameters.Mode mode = NONE;  // user chooses trace or replay or none
	public TDThread() {
		inner = new innerThread();
		mode = (propertyReader.getInstance().getModeProperty());
	}
	public TDThread(String threadName) {
		inner = new innerThread(threadName);
		mode = (propertyReader.getInstance().getModeProperty());
	}
	public TDThread(Runnable runnable) {
		inner = new innerThread(runnable); this.runnable = runnable;
		mode = (propertyReader.getInstance().getModeProperty());
	}
	public TDThread(Runnable runnable, String threadName) {
		inner = new innerThread(runnable,threadName); this.runnable = runnable;
		mode = (propertyReader.getInstance().getModeProperty());
	}
   public TDThread(ThreadGroup group, Runnable runnable) {
  		inner = new innerThread(group,runnable); this.runnable = runnable;
  		mode = (propertyReader.getInstance().getModeProperty());
	}
	public TDThread(ThreadGroup group, String threadName) {
 		inner = new innerThread(group,threadName);
 		mode = (propertyReader.getInstance().getModeProperty());
	}	
	public TDThread(ThreadGroup group, Runnable runnable, String threadName) {
	 	inner = new innerThread(group, runnable,threadName);this.runnable = runnable;
		mode = (propertyReader.getInstance().getModeProperty());
	}

	final public int getID() {return inner.getID();}
	final public Controller getController() {return inner.getController();}
	final public int getVersionNumber() { return inner.getVersionNumber();}
	final public void join() throws InterruptedException {inner.join();}
	final public void start() {
		if (runnable == null) inner.startT(this);
		else inner.startRunnable(this);
	}
	final public void setDaemon(boolean on) {inner.setDaemon(on);}
	final public int getResult() {return inner.getResult();}
	final public boolean isAlive() {return inner.isAlive();}
	final public void addToState(Object o) {inner.addToState(o);}
	public String getThreadName() { return inner.getThreadName(); }
	public static TDThread currentThread()	{return ((innerThread)Thread.currentThread()).myT;}
	public /*int*/ void run() {/*return 0;*/} 
	protected Runnable runnable = null;
	private innerThread inner;
}
