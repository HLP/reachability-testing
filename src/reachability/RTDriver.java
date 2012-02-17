package reachability;


import java.lang.reflect.*;

import test.PartialOrderReader;

import lattice.Computation;
import lattice.Evaluable;
import lattice.traveler.LatticeTraveler;

public final class RTDriver implements propertyParameters {
	// the driver for RT			
	private static int i = 1; // counts the number of executions
	private static int collected = 1;
	private static int numNonSystemThreads = 0;
	static RTWatchDog watchDog;
	static LTSGenerator LTSgen;
	static String clientServerID;
	static private propertyParameters.DetectDeadlock detectDeadlock = (propertyReader.getInstance().getDetectDeadlockProperty());
	static private propertyParameters.Mode mode = (propertyReader.getInstance().getModeProperty());
	static private propertyParameters.GenerateLTS generateLTS = (propertyReader.getInstance().getGenerateLTSProperty());
	static private propertyParameters.Controllers numControllers = (propertyReader.getInstance().getControllersProperty());
	static private propertyParameters.ClientServer clientServer = propertyReader.getInstance().getClientServerProperty();
	static private int hash = propertyReader.getInstance().getHashProperty(); // progress displayed every hash executions, user can set this value

	static Class unknown = null;
	
	public static void main (String args[]) {
		try {
			if (args.length == 0) {
				System.out.println("Error: The name of the program to be tested ");
				System.out.println("       must be specified as a command line parameter.");
				System.out.println("       For example: java -Dmode=rt RTDriver MyProgram");
				System.exit(1);		   
			}
			if (mode != RT) {
				System.out.println("Error: The RT Driver must be executed with mode=rt.");
				System.out.println("       For example: java -Dmode=rt RTDriver MyProgram");
				System.exit(1);
			}
			if (numControllers != SINGLE) {
				System.out.println("Error: The RT Driver must be executed with a single controller,");
				System.out.println("       which is the default for property -Dcontrollers.");
				System.out.println("       For example: java -Dmode=rt RTDriver MyProgram");
				System.exit(1);
			}
			if (clientServer == propertyParameters.Client)
				clientServerID = propertyReader.getInstance().getWorkerNumberProperty();
			else if (clientServer == propertyParameters.Server)
				clientServerID = new String("0");
			else 
				clientServerID = new String();

			msgTracingAndReplay.setUsingRTDriver();
			//Class unknown = null;
			try {
				unknown = Class.forName(args[0]);
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Error: The name of the program to be tested ");
				System.out.println("       must be specified as a command line parameter.");
				System.out.println("       For example: java -Dmode=rt RTDriver MyProgram");
				System.exit(1);
			}
			Class partypes[] = new Class[1];
			partypes[0] = String[].class;
			Method meth = null;
			try {
				meth = unknown.getDeclaredMethod("main", partypes);
			} catch(NoSuchMethodException e) {
				System.out.println(e);
				System.out.println("Error: Class " + args[0] + " does not have a main(String[] args) method.");
				System.exit(1);           
			}
			catch(SecurityException e) {
				System.out.println(e);
				System.out.println("Error: The Security Manager will not allow this RTDriver to work.");
				System.out.println("       See file RTDriverAlternate.java in the library and follow its instructions.");			  		
				System.exit(1);           
			}
			Object arglist[] = new Object[1];
			String parm[] = null;
			if (args.length > 1) {
				parm = new String[args.length - 1];
				for (int i=1; i<args.length; i++)
					parm[i-1] = args[i];
			}
			arglist[0] = parm;
			if (generateLTS==LTSON)
				LTSgen = LTSGenerator.getInstance();
			watchDog = new RTWatchDog(); watchDog.start();
			System.out.println("start:" + java.util.Calendar.getInstance().getTime());	   
			Object p = unknown.newInstance();
			// Yenjung: get partial order
			PartialOrderDumper pod = PartialOrderDumper.getInstance();
			pod.beginRun(0);
			Object retobj = meth.invoke(p, arglist);
			pod.endRun();
			// check whether any user-threads are still running. If so, stop RT
			checkRunningThreads(clientServerID); numNonSystemThreads=0;
			msgTracingAndReplay controller = channel.getObjectBasedController();
			boolean ignored = controller.appCompleted();
			if (generateLTS==LTSON)
				LTSgen.reset();
			if (detectDeadlock == DETECTIONON)
				deadlockWatchdog.resetDeadlockTrace();
			if (ignored) System.out.println("Internal Error: !ignored failed");
			System.out.println("1/1");

			while (true) {
				ThreadIDGenerator.getInstance().resetIDs(); // reset ID generator
				channelIDGenerator.getInstance().resetIDs(); // reset ID generator
				conditionVarIDGenerator.getInstance().resetIDs();
				p = unknown.newInstance();
				// Yenjung: get partial order
				pod.beginRun(i);
				retobj = meth.invoke(p, arglist);
				pod.endRun();
				// check whether any user-threads are still running. If so, stop RT
				checkRunningThreads(clientServerID); numNonSystemThreads=0;
				controller = channel.getObjectBasedController();
				ignored = controller.appCompleted();
				if (generateLTS==LTSON)
					LTSgen.reset();
				if (detectDeadlock == DETECTIONON)
					deadlockWatchdog.resetDeadlockTrace();
				if (!ignored)
					collected++;
				i++;
				//if (i%hash==0) System.out.println(i+"/"+collected);
				System.out.println(i+"/"+collected);
			}
			
		} catch (Exception e) {e.printStackTrace();}
	}

	final static class RTWatchDog extends Thread {
		public RTWatchDog() {super("RTWatchDog");}
		private boolean resultsDisplayed = false;
		private long displayedIndex = -1;
		public void run() {
			int delayTime = 8000; 
			if (clientServer == propertyParameters.Client || clientServer == propertyParameters.Server)
				delayTime = 60000;
			RTStopWatch sw = new RTStopWatch(delayTime);
			sw.start();  // capture start time
			while (true) {
				int saveIndex = i;
				try {
					Thread.sleep(delayTime);
				}
				catch (InterruptedException e) {}
				boolean showResults = false;
				if (clientServer != propertyParameters.Client) { // server, or neither client nor server
					showResults = (!resultsDisplayed && saveIndex == i && VariantGenerator.getInstance().appAndGeneratorThreadWaiting())
							|| (resultsDisplayed && saveIndex != displayedIndex && saveIndex == i
							&& VariantGenerator.getInstance().appAndGeneratorThreadWaiting());
				}
				else // client
					showResults = (!resultsDisplayed && saveIndex == i 
					&& VariantGenerator.getInstance().appAndGeneratorThreadWaiting()
					&& VariantGenerator.getInstance().clientIsIdle(new RTResult(sw,i,collected)))
					|| (resultsDisplayed && saveIndex != displayedIndex && saveIndex == i
					&& VariantGenerator.getInstance().appAndGeneratorThreadWaiting()
					&& VariantGenerator.getInstance().clientIsIdle(new RTResult(sw,i,collected)));

				//System.out.println("Show results is " + showResults + " i:"+i);

				if (showResults) {
					sw.end();  // capture end time (may also be captured on server too)
					System.out.println();
					if (clientServer == propertyParameters.Client)
						System.out.println("Reachability Testing completed for Worker "+clientServerID+".");
					else	if (clientServer == propertyParameters.Server)
						System.out.println("Reachability Testing completed for Manager.");
					else
						System.out.println("Reachability Testing completed.");
					System.out.println("  Executions:"+i+" / Sequences Collected:"+collected); // +"/"+transitionCount+"/"+eventCount);
					//System.out.println("  Elapsed time in minutes: " + sw.elapsedMinutes());
					System.out.println("  Elapsed time in seconds: " + sw.elapsedSeconds());
					//System.out.println("  Elapsed time in milliseconds: " + sw.elapsedMillis());
					System.out.flush();
					resultsDisplayed = true;
					displayedIndex = saveIndex;
					
					System.out.println("RichTest is done\n");
					System.out.println("Predicate detection starts");
					Computation[] comp = PartialOrderReader.parse();
					for (int i = 0; i < 3; ++i) {
						System.out.println("Evaluating partial order " + i);
						try {
							comp[i].setPredicate((Evaluable) unknown.newInstance());
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
						LatticeTraveler.lexTraverse(comp[i]);
						System.out.println();
					}
					
					if (clientServer == propertyParameters.Server) {
						VariantGenerator.getInstance().saveRTResult(0, new RTResult(sw,i,collected));
						System.out.println();
						System.out.println("(sub)Total Executions:"+ VariantGenerator.getInstance().getTotalSequences() + 
								" (sub)Total Sequences Collected:"+VariantGenerator.getInstance().getTotalCollected());
						System.out.flush();  
					}
					if (detectDeadlock == DETECTIONON) {
						deadlockWatchdog.stopWatchdog();
					}
					if (generateLTS==LTSON) {
						LTSGenerator.getInstance().generateLTSs();
					}
					if (clientServer != propertyParameters.Server && clientServer != propertyParameters.Client) {
						System.exit(0);
					}
				}
				if (clientServer == propertyParameters.Server && resultsDisplayed 
						&& VariantGenerator.getInstance().clientsRunning()==0 
						&& VariantGenerator.getInstance().appAndGeneratorThreadWaiting()) {
					System.out.println();
					System.out.println("Reachability Testing Total Executions:"+ VariantGenerator.getInstance().getTotalSequences());
					System.out.println("Reachability Testing Total Sequences Collected:"+VariantGenerator.getInstance().getTotalCollected());
					System.out.flush();
					System.exit(0);
				}
			}
		}
	}

	static void checkRunningThreads(String clientServerID) {
		ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
		while (root.getParent() != null) {
			root = root.getParent();
		}
		// Visit each thread group
		visit(root, 0, clientServerID);
	}

	public static void visit(ThreadGroup group, int level, String clientServerID) {
		// Get threads in `group'


		if (group != null) {
			int numThreads = group.activeCount();
			Thread[] threads = new Thread[numThreads*2];
			numThreads = group.enumerate(threads, false);
			for (int i=0; i<numThreads; i++) {
				// Get thread
				Thread thread = threads[i];
				if (thread != null) {
					ThreadGroup g = thread.getThreadGroup();
					if (g != null) {
						String groupName = g.getName();
						//System.out.println("groupName="+groupName+" thread name="+thread.getName());
						if (groupName.equals("system"))
							;
						else {
							if (!thread.getName().equals("RTWatchDog") && !thread.getName().equals("generatorThread")
									&& !thread.getName().equals("Controller") && !thread.getName().equals("deadlockWatchdog")
									&& !thread.getName().equals("ltsGeneratorThread") && !thread.getName().equals("rx2clientServerThread"+clientServerID)
									&& !thread.getName().equals("ku3serverThreadForClient"+clientServerID)
									&& !thread.getName().equals("st1serverWatchdog")
									&& !thread.getName().equals("ar2serverThread"+clientServerID)
									&& !(thread.getName().indexOf("dr3clientHandler")>=0)
									&& !(thread.getName().indexOf("Poller SunPKCS11-Darwin")>=0)
									&& (!(thread.getName().indexOf("pool")>=0) && !(thread.getName().indexOf("thread")>=0) ))
								numNonSystemThreads++;
							// The RTDriver has 4 associated threads that always run throughout RT
							// (main thread, variant generator thread, controller thread, and RTWatchdog thread).
							// If -DdeadlockDetection==on, then there is also a deadlock detection watchdog thread.
							// Other threads should not be running when main returns. Here we try to check
							// this and help the user.
							if (numNonSystemThreads>1)  {
								System.out.println("");
								//System.out.println("Non-terminated Thread: groupName="+groupName+" thread name="+thread.getName());
								System.out.print("Error: Some of the threads in your program had ");
								System.out.println("not terminated when the main ");
								System.out.println("method returned to RTDriver. Use a try-catch block with a call to join()");
								System.out.print("for each thread ");
								System.out.println("to ensure that all threads are completed when main() returns.");
								System.out.println("For example:");
								System.out.println("     myThread t1 = new myThread();");
								System.out.println("     myThread t2 = new myThread();");
								System.out.println("     t1.start(); t2.start();");
								System.out.println("     try {");
								System.out.println("       t1.join();t2.join();");
								System.out.println("     } catch(InterruptedException e) {}");
								System.exit(1);
							}
						}
					}
				}
			}    
			int numGroups = group.activeGroupCount();
			ThreadGroup[] groups = new ThreadGroup[numGroups*2];
			numGroups = group.enumerate(groups, false);
			// Recursively visit each subgroup
			for (int i=0; i<numGroups; i++) {
				visit(groups[i], level+1, clientServerID);
			}
		}
	}
}
