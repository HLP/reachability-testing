package reachability;
import java.util.*;
    public final class deadlockWatchdog extends Thread implements propertyParameters {
    
   	private static final Object classLock = deadlockWatchdog.class;
   	private static final Object o = new Object();
		private static deadlockWatchdog instance = null;
		private boolean stopped = false;
		static private HashMap status = new HashMap();
		static private ArrayList trace = new ArrayList();
		static private propertyParameters.Mode mode = (propertyReader.getInstance().getModeProperty());
		static private propertyParameters.DetectDeadlock detectDeadlock = 
			(propertyReader.getInstance().getDetectDeadlockProperty());

		static void changeStatus(innerThread t, String statusMessage) {
      	synchronized(classLock) {
				status.put(t,statusMessage);
			}
		}
		static void removeThread(innerThread t) {
      	synchronized(classLock) {
				status.remove(t);
			}
		}
		static void deadlockTrace(String traceMsg) {
      	synchronized(classLock) {
				trace.add(traceMsg);
			}			
		}
		static void resetDeadlockTrace() {
      	synchronized(classLock) {
				trace.clear();
			}			
		}
		static {
			if (detectDeadlock == DETECTIONON) {
				System.out.println("Monitoring for Deadlock.");
				System.out.flush();
			}
		}
		static deadlockWatchdog startInstance() { 
      	synchronized(classLock) {
         	if (instance == null) {
            	instance = new deadlockWatchdog();
					instance.start();
            }
			}
			return instance;
		}
		public deadlockWatchdog() {super("deadlockWatchdog");}
		public static void notifyDeadlock() {
			// if RT controller detects a deadlock when -DdeadlockDetection=on, the RT
			// controller calls this method so that the watchdog will detect the
			// deadlock. This is necessary since, in one case, a thread can become
			// blocked on a call to the RT controller, and this block will not be
			// visible to this watchdog thread, i.e., this watchdog will consider
			// the blocked thread to still be running
			innerThread.setNumBlockedThreads(innerThread.getNumRunningThreads());		
		}
		
		public static void stopWatchdog() {
		// Watchdog started only if mode == TRACE || mode == RT
			innerThread.resetNumBlockedRunningThreads(); 
			if (instance != null) {	// in case user specifies deadlockDetection=on but mode=none
				instance.stopped = true;
				instance.interrupt(); // wake up thread if it is sleeping
				/* instance.stop(); stop is deprecated */
			}
			instance = null;    // old WatchDog may wakeup and do one last check and
		}                      // detect same deadlock as new watchDog. Below,
									  // we lock detection code so only one of them can do it
									  // that is, first to detect deadlock executes System.exit()
		
		public void run() {
			boolean deadlockDetected = false;
			// deadlock must be detected twice, same for no threads running in trace mode,
			// before deadlock is announced
			boolean firstDeadlockDetected = true;
			boolean firstNoThreadsRunning = true;
			//System.out.println("Monitoring for Deadlock ....");
			while (!deadlockDetected && !stopped) {
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {}
				if (deadlockDetected || stopped) return;
				//System.out.println("running:"+innerThread.getNumRunningThreads());
				//System.out.println("blocked:"+innerThread.getNumBlockedThreads());				
				if (innerThread.getNumRunningThreads() == innerThread.getNumBlockedThreads() &&
					innerThread.getNumRunningThreads()>0 ) {
					if (firstDeadlockDetected) {
						firstDeadlockDetected = false;
					}
					else {
						System.out.println("Deadlock detected:");
						deadlockDetected = true;
						Set s = status.entrySet();
						Iterator i = s.iterator();
						while (i.hasNext()) {
							Map.Entry e = (Map.Entry)(i.next());
							innerThread t = (innerThread) e.getKey();
							String statusMessage = (String) e.getValue();
							System.out.println("- " + t.getThreadName() + " " + statusMessage);
						}
						if (trace.size() != 0) {
							System.out.println();
							System.out.println("Execution trace:");
							Iterator j = trace.iterator();
							while (j.hasNext()) {
								System.out.println((String)j.next());
							}	
						}
						System.exit(1);
						/*
						// This code stops the deadlocked threads, instead of exiting the program.
						// If a deadlock is found during reachability testing, and the threads
						//   are stopped, the reachability testing can continue.
						innerThread.resetNumBlockedRunningThreads();
						innerThread deadThreads[] = new innerThread[s.size()];
						int j = 0;
						i = s.iterator();
						while (i.hasNext()) {
							Map.Entry e = (Map.Entry)(i.next());
							deadThreads[j] = (innerThread) e.getKey();
							j++;
						}
						for (int k = 0; k<j; k++) {
							String name = deadThreads[k].getThreadName();
							deadThreads[k].stop();
							System.out.println(name + " stopped");
						}
						System.out.flush();
						stopWatchdog();
						instance = null;
						*/
					}
				}
				else {
					firstDeadlockDetected = true;
					if (mode == TRACE && innerThread.getNumRunningThreads()==0) {
						if (firstNoThreadsRunning)
							firstNoThreadsRunning = false;
						else
							stopWatchdog();
					}
					else {
						firstNoThreadsRunning = true;
					}
				}
			}
		}
	}

