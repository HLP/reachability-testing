package reachability;
// this distributes seqs and closes sockets after use (i.e., sockets used by clients
// to request more variants, and sockets used by server to ask clients for more
// variants 

import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Serializable;
import java.util.Collections;

import java.util.Calendar;
import java.util.Random;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* if any nodes are down: adjust line 1252, and make an adjustment in ServerSplitter */

public class VariantGenerator {

    private static final String STATISTICS_FN = "stats.txt";
    private static final String GENE_FN = "gene.txt";
    private static final String RESULTSFILE_FN = "RTResults.txt";
    private static final int CHECKOFF = 0;
    private static final int NORMCHECK = 1;
    private static final int RANDCHECK = 2;
    private static final int CUSTCHECK = 3;
    private static final int CHECKLIMIT = 2000;
    private static final int CHECKINTERVAL = 12;
    private static final int MAXWAIT = 10000;
    private static final int SOCKETTIMEOUT = 30000;
    private int hash = 25; // hash property is read below in constructor;
    private static final int groupMax = 500;
    
    private static final boolean debug = false;

    // keep all the variants generated (for debugging purpose)
    private Vector vars;
    
    // keep all the sequences collected (for debugging purpose)
    private Vector seqs;

    private Buffer in;
    private Buffer out;

    private int treeMarker = 1;

    private int numOfClients = 0;
    
    private static final Object classLock = VariantGenerator.class;

    private static VariantGenerator instance = null;

    private VariantGroup variantGroup;

    private boolean firstTime;

    private int clientAttempts = 0;
    private int serverAttempts = 0;

    private boolean waitingGroups = false;
    
    private long groupStartTime = 0;
    private long groupEndTime = 0;
    private RollingAverage avgGroupTime;
    
    private long requestStartTime = 0;
    private long requestEndTime = 0;
    // not currently in use: request time is time to service a client's request to
    // the server for more groups
    private RollingAverage avgRequestTime;
    
    private long variantStartTime = 0;
    private long variantEndTime = 0;
    private RollingAverage avgVariantTime;
    
    private int variantStartCount = 0;
    private int variantGroupCount = 0;
    private RollingAverage avgVariantGroup;
    
    private long startReceived = 0;
    private long sizeAtRequest = 0;
    private long sizeOfReceived = 0;

    private PrintWriter output;
    private PrintWriter resultsFile;
    private PrintWriter treeOut;
    private PrintWriter outputGene;

    private boolean isRestart;
    private Random generator;	

    // In SPLIT mode, we only generate the top level of the RT tree,
    // and then split the variants into different groups
    private boolean isSplit;

    // Used in split mode to indicate how many groups should be split
    private int numOfHosts;
    private int numberRunningClients;
    private long totalSequences; // total num sequences exercised by RT clients
    private long totalCollected; // total num sequences collected by RT clients
    private long numGroupsSent; // number of groups sent to clients by server
    private long numGroupsReceived; // number of groups received by server from clients
    
/**/
	 private ArrayList[] groups; // Arraylist of ArrayLists, each a list of groups for clients
	 private propertyParameters.ClientServer clientServer = propertyParameters.StandAlone;
	 private boolean groupsGenerated = false; // set to true in Server mode after groups
	                                          // are generated for clients
	 private boolean firstTimeServer = true;  // indicates generator thread is generating
	                                          // its first variant group
	 private String serverIP; // IP address of server (assuming host name: www.cs.gmu.edu)
 	 private int serverPort;  // port number for server
 	 
 	 private static propertyParameters.ModularTesting modularTesting = propertyParameters.MTOFF;


/**/

    private int numOfVars;
    private int limitOfVars;

    private int dupCheckMode;
    // perform random RT
    private boolean randomRT;
    private Random rand;


    private boolean displayTrace;
    
    private Termination terminator;
    
    private int previousServerResultsSequences = 0;
    private int previousServerResultsCollected = 0;
    
    private boolean aggressive = true;

	 public static VariantGenerator getInstance() { 
		synchronized(classLock) {
        	if (instance == null)
				instance = new VariantGenerator();
	   }
		return instance;
    }

    private VariantGenerator () {
  		clientServer = propertyReader.getInstance().getClientServerProperty();
  		hash = propertyReader.getInstance().getHashProperty();
  		modularTesting = propertyReader.getInstance().getModularTestingProperty();	
		writeLine ("New Test Run at " +  Calendar.getInstance().getTime());
		avgGroupTime = new RollingAverage(50);
		avgRequestTime = new RollingAverage(2);
		avgVariantTime = new RollingAverage(10);
		avgVariantGroup = new RollingAverage(50);
		generator = new Random();
		// read the start mode property
		isRestart = 
	    propertyReader.getInstance().getRunModeProperty () == propertyParameters.RESTART;
	    
		randomRT =  propertyReader.getInstance().getRunModeProperty () == propertyParameters.RANDOM;

		// check if it is split mode
		isSplit =
	    	propertyReader.getInstance().getRunModeProperty () == propertyParameters.SPLIT;
		if (isSplit) {
	    	numOfHosts = propertyReader.getInstance().getNumOfHostsProperty ();
	    	numOfClients = numOfHosts;
	    	numberRunningClients = numOfHosts;
	    	if (numOfHosts == 0) {
				System.out.println("In the split mode, numOfHosts property must be specified!");
				System.exit (1);
	    	}
		}

/**/
		if (clientServer == propertyParameters.Server) {
      		numOfHosts = propertyReader.getInstance().getNumWorkersProperty();
     			numOfClients = numOfHosts;
     			numberRunningClients = numOfHosts;
      		serverPort = propertyReader.getInstance().getServerPortProperty();
    		if (numOfHosts == 0) {
				System.out.println("In client/server mode, numOfHosts property must be specified!");	
				System.exit (1); 
			}
    		groups = new ArrayList[numOfHosts+1];
			for (int i=0; i<numOfHosts+1; i++)	// client lists start at position 1, not 0.
				groups[i]=new ArrayList();			// e.g., 4 clients => groups[1] .. groups[4].
		}												  	// so client IDs start with 1, not 0, 
		                                       // and groups[0] currently not use
		else if (clientServer == propertyParameters.Client) {
      		serverIP = propertyReader.getInstance().getServerIPProperty();
      		serverPort = propertyReader.getInstance().getServerPortProperty();
      		// use "LocalHost" for LocalHost, else use, e.g., www.cs.gmu.edu
      		// For example: -DserverIP=LocalHost -DserverPort=2021
      		//              -DserverIP=www.cs.gmu.edu -DserverPort=2021
		}
		                                 
/**/
      // used to stop RT (temporarily for later restart)
		limitOfVars = propertyReader.getInstance().getLimitOfVariantsProperty ();

		in = new boundedBuffer (500);

		if (propertyReader.getInstance().getCoreAlgorithmProperty () ==
	    	propertyParameters.PRUNE) {
	    	//writeLine("Core Algorithm: PRUNE");
		}

		if (propertyReader.getInstance().getBufferTypeProperty () == 
	    	propertyParameters.MEMBUFFER) {
	    	out = new unboundedBuffer ("Out Buffer: ");
	    	//writeLine("Buffer Type: MEMORY");
	    	((unboundedBuffer) out).init (isRestart);
		}
		else if (propertyReader.getInstance().getBufferTypeProperty () == 
		 	propertyParameters.DISKQUE) {
	    	out = new DiskBuffer ("Out Buffer: ");
	    	//writeLine("Buffer Type: DISK QUE");
	    	((DiskBuffer) out).init (isRestart);
		}
		else {
	    	out = new DiskStack ("Out Buffer (stack): ");
	    	//writeLine("Buffer Type: DISK STACK");
	    	((DiskStack) out).init (isRestart);
		}
		
		// determine duplicate check mode
		dupCheckMode = CHECKOFF;
		if (propertyReader.getInstance().getDuplicateCheckProperty () ==
	    	propertyParameters.CHECKON) {
	    	dupCheckMode = NORMCHECK;
	    	writeLine("Duplicate Check: NORMAL");
		}
		else if (propertyReader.getInstance().getDuplicateCheckProperty () ==
			propertyParameters.RANDCHECK) {
	    	dupCheckMode = RANDCHECK;
	    	writeLine("Duplicate Check: RANDOM");
		}
		else if (propertyReader.getInstance().getDuplicateCheckProperty () ==
		 	propertyParameters.CUSTCHECK) {
	    	dupCheckMode = CUSTCHECK;
	    	writeLine("Duplicate Check: CUSTOM");
		}

		// determine whether to display trace/variants

		displayTrace = false;

		if (propertyReader.getInstance().getDisplayTraceProperty () ==
	    		propertyParameters.DISON) {
	 		displayTrace = true;
	  		writeLine("Display Trace: ON");
		}

		firstTime = true;
		variantGroup = null;

		generatorThread t = new generatorThread(); // generates variants from execution traces
	
/**/
		if (clientServer == propertyParameters.Client || clientServer == propertyParameters.Server) {
   	 	  String stringID = propertyReader.getInstance().getWorkerNumberProperty();
           int ID = 0;
   	 	  if (stringID == null) 
              ID = 0;
           else
	  	 	     ID = Integer.parseInt(stringID);
			if (clientServer == propertyParameters.Client)
				terminator = new Termination();
			clientServerThread cs = new clientServerThread(clientServer,ID);
			cs.setDaemon(true);
			cs.start();
		}
/**/
		seqs = new Vector ();
		rand = new Random ();

		numOfVars = 0;

		t.setDaemon(true);
		t.start ();

    }

    boolean appAndGeneratorThreadWaiting() { 
    // RT stops when no variants are being executed and no more are being generated
		return in.consumerIsWaiting() && out.consumerIsWaiting();
    }
    
    boolean clientIsIdle(RTResult result) {return terminator.doClientIsIdle(result);}
    int clientsRunning() {return numberRunningClients;}
    
	 synchronized void decNumberRunningClients() {numberRunningClients--;}
	 synchronized void addToTotalSequences(long sequences, long collected) {
	 	totalSequences+=sequences; totalCollected += collected;
	 }
	 synchronized long getTotalSequences() { return totalSequences;}
	 synchronized long getTotalCollected() { return totalCollected;}
 	 synchronized void addNumGroupsSent(long sent ) { numGroupsSent+=sent;}
	 synchronized void addNumGroupsReceived(long received ) { numGroupsReceived+=received;}
	 synchronized void addNumGroupsSentReceived(long sent) { 
	 	numGroupsSent+=sent; numGroupsReceived+=sent; // received groups and sent them
	 }
 	 synchronized long getNumGroupsSent() { return numGroupsSent;}
 	 synchronized long getNumGroupsReceived() { return numGroupsReceived;}
 	 synchronized void resetGroupsSentReceived() { numGroupsSent = numGroupsReceived = 0;}

    public srSeq computeRaceSet (srSeq seq) {
		RaceAnalyzer analyzer = RaceAnalyzer.getInstance();
		// rhc: FIFO is now the default
		//analyzer.setModel(RaceAnalyzer.FIFO);
		SrSeqPO po = SrSeqTranslator.convert (seq);
		analyzer.analyze (po);
		srSeq rval = SrSeqTranslator.convert (po, true);
		return rval;
    }

    public void depositSeq (srSeq seq) {
      // RT controller process calls this method to deposit execution traces,
      // which are processed by the variant generator thread
		if (seq != null) { 
			//System.out.println("####");
			//System.out.println(seq);
	    	in.deposit(seq);	    
		}
    }

    public srSeq getVariant () {
    // RT controller process calls this method to get the next variant to execute
		srSeq rval = null;
	
		/* DISABLED
		//process every 100 variants to get avg variants per second
		if ((variantStartTime != 0) && (numOfVars%100 == 0))
		{
			variantEndTime = Calendar.getInstance().getTime().getTime();
			variantEndTime = variantEndTime - variantStartTime;
			variantEndTime = 100000/variantEndTime;
			int variantAvgCnt = (int) variantEndTime;
			avgVariantTime.add(new Integer(variantAvgCnt));
		}
		*/

/**/
		if ((clientServer == propertyParameters.StandAlone || 
	    	clientServer == propertyParameters.Server ) 
	     	&& firstTime && !isRestart) {
	    	// for the first time, return an empty srSeq to start
	    	/* This is true in Server mode, and also in StandAlone mode)
	       	But if in client mode, then the first variant group will be obtained
	       	from the server and deposited in the out buffer so grab one from out buffer
  		 	*/
	    	rval = SrSeqTranslator.convert(SrSeqPO.EMPTY_SEQ);
			/* Code added for generating tree. this needs to be controlled by a property. Disable for now.
			rval.setOrigNode(0);
	    	rval.setTreeId("0");
			*/
	    	
	    	firstTime = false;
		}
		else { 
	    	if ((limitOfVars > 0) && (numOfVars > limitOfVars) 
				&& variantGroup != null && !variantGroup.hasNext()) {
				// stop RT temporarily for later restart; save remaining variant groups on disk
				writeLine ("Exceeding the maximum number of variants. Stopping RT for later restart.");
				writeLine ("Test Run Ends at " +  Calendar.getInstance().getTime());

				// wait for the in buffer to be empty before exit
				try {
			    	while (in.size() > 0) {
						Thread.sleep (500);
			    	}
				}
				catch (InterruptedException ex) { }

				// serialize all the variantGroups in the out buffer
				// so that we can restart the RT process
				if (propertyReader.getInstance().getBufferTypeProperty () == 
			    	propertyParameters.MEMBUFFER) {
			    	((unboundedBuffer) out).serialize ();
			    	//((unboundedBuffer) in).serialize ();
				}
				System.out.println("Reachability testing stopped after " + (numOfVars+1) + " executions.");
				System.exit (1);
	    	}
         // Group variantGroup is the current group of variants being processed
         // If we don't have a current group (because we just started RT) or
         // we've processed all the variants in the current group, get the next group
	    	if (variantGroup == null || ! variantGroup.hasNext ()) {
		    	//process to gather avg time to complete a group
				/* DISABLED
				if (groupStartTime != 0)
				{
					groupEndTime = Calendar.getInstance().getTime().getTime();
					groupEndTime = groupEndTime - groupStartTime;
					avgGroupTime.add(new Long(groupEndTime));
					//System.out.println("Average group processing is " + avgGroupTime.getAvg());
				}
			
				//process to gather avg number of variants per group
				if (numOfVars != 0) {
					variantGroupCount = numOfVars - variantStartCount;
					avgVariantGroup.add(new Integer(variantGroupCount));
					variantStartCount = numOfVars;
				}
				*/

	    			//output.println("get group from out buffer");
	    		//synchronized(out) { // get the next group
					variantGroup = (VariantGroup) out.withdraw ();
					/* DISABLE
					//checks to see if we are an initial group
					if(variantGroup.getOrigSeq() == null && variantGroup.getTab() == null)
					{
						//System.out.println("Here is the delimiter for an initial variantGroup");
						variantGroup = (VariantGroup) out.withdraw();
					}
					*/
				//}

            /* DISABLED  this needs to be controlled by a property! Disable for now
				//writes a string to be used for generating a RT treee
				writeTreeId(variantGroup.getTreeId());
				//System.out.println("This is the TreeId of a new variantGroup " + variantGroup.getTreeId());
				treeMarker = 1;
				groupStartTime = Calendar.getInstance().getTime().getTime();
				*/
	    	}

	    	// get the next variant from the current group of variants
	    	SrSeqPO var_po = variantGroup.next ();
	    	rval = SrSeqTranslator.convert(var_po);
      /* DISABLED this needs to be controlled by a property. Disable for now
	    	rval.setOrigNode(variantGroup.getOrigNode());
	    	rval.setTreeId(variantGroup.getTreeId() + "," + treeMarker);
	    	//System.out.println("Grabbing " + rval.getTreeId());
			++treeMarker;
			if (variantStartTime == 0 || (numOfVars%100 == 0)) {
	    		variantStartTime = Calendar.getInstance().getTime().getTime();
	    		//System.out.println("Start time is " + variantStartTime);
    		}
		*/
	    
		// print out gene string
	    	if (propertyReader.getInstance().getPrintGeneProperty () ==
			propertyParameters.GENEON) {
				writeGene (var_po);
	   	}

	    	// print out the variant

	    	if (displayTrace) {
	       	System.out.println("variant " + (variantGroup.getCurrentVarID() + 1) +
	   	       " of " + variantGroup.getNumOfVariants ()
	    	       + " for sequence " + (variantGroup.getSeqID () + 1));
				System.out.println(rval);
	    	}

	   	numOfVars ++;
	    
		} // end else

		return rval; // return variant to RT controller so it can be replayed
    }
    
    //predicts in milliseconds the amount of time left to process leftover groups
    public long getWorkPrediction()
    {
		return out.size() * avgGroupTime.getAvg();
    }
    
    //predicts average time for a request
    public long getRequestPrediction()
    {
	    return avgRequestTime.getAvg();
    }
    
    //returns the nodes avg variants executed per second
    public int getVariantsPerSec()
    {
		return (int) avgVariantTime.getAvg();
    }   
    
    //returns the nodes avg variant count per variant group
    public int getAvgVariantCount()
    {
	  	return (int) avgVariantGroup.getAvg();  
    }

    private class generatorThread extends Thread {
    // This thread reads execution traces and generates a group of variants for the trace
    	//private boolean firstTime = true; 

     RaceAnalyzer analyzer;
     public generatorThread () {
			super("generatorThread");
			analyzer = RaceAnalyzer.getInstance();
			//rhc: FIFO is now the default
			//analyzer.setModel(RaceAnalyzer.FIFO);
      }
      
	public void run () {
	    int num_of_seqs = 0;
	    int num_of_groups = 0;
	    int num_of_vars = 0;
	    int num_of_dups = 0;
	    int nodeVal = 0;

	    Random randCheckGen = new Random (); 

	    Random randRTGen = new Random ();
		
	    //Random rand = new Random ();
	    
/**/	 int numberOfClientGroups =0; // number of variants in the first group. If this is
                                    // n, then the next n groups will be split among
                                    // the server and the clients
       int initialNumberOfServerGroups=0; //number of variants in first group given
                                          // to server. Usually numberOfClientGroups * .5

	    writeLine ("1:NumOfSeqs (executed) " + "2:Remaining Groups " + "3:Variants Generated");
	    int tmpNodeVal = 0;
	    String tmpTreeVal = "";
	    for (;;) {
			// withdraw a srSeq object (i.e., execution trace) from the in buffer
			srSeq sr_seq = (srSeq) in.withdraw ();

			boolean discard = randRTGen.nextBoolean ();
		
			if (num_of_seqs > 0 && randomRT && discard) {
			// During random reachability testing, execution traces are randomly discarded
			// so that RT can finish in a reasonable amount of time
		    		continue;
			}

			SrSeqPO seq = SrSeqTranslator.convert (sr_seq);

			if (num_of_seqs == 0) {
		    	seq.setGene ("0");
			}

			// set sequence id
			seq.setID (num_of_seqs++);

			if(displayTrace) {
		  		System.out.println("Sequence " + (seq.getID () + 1) + ":");
		  		System.out.println(SrSeqTranslator.convert(seq));
			}

			// FOR DEBUGGING
			if (dupCheckMode == NORMCHECK) {
		   		if(!addSeq (seqs, seq, true)) {
					num_of_dups ++;
		    	};
			}
			else if (dupCheckMode == RANDCHECK) {
		    	boolean keep = randCheckGen .nextBoolean ();
		    	if (!addSeq (seqs, seq, keep)) {
					num_of_dups ++;
		    	}
			}
			else if (dupCheckMode == CUSTCHECK) {
		    	boolean keep = (num_of_seqs % CHECKINTERVAL == 0);
		    	if (!addSeq (seqs, seq, keep)) {
					num_of_dups ++;
		    	}
			}

			analyzer.analyze(seq);

			VariantCalculator calculator = new VariantCalculator (seq);

			// deposit all the variants of this sequence into the buffer
			VariantGroup variantGroup = calculator.getVariantGroup ();
			/* DISCARD
			//helps build the string we need to build a tree
			if (numberOfClientGroups > 0 && variantGroup != null) {
				variantGroup.setOrigNode(numberOfClientGroups);
				variantGroup.setTreeId("0," + numberOfClientGroups);
				//System.out.println("Pushing " + variantGroup.getTreeId());
				tmpNodeVal = numberOfClientGroups;
			} else if (variantGroup != null) {
				tmpNodeVal = sr_seq.getOrigNode();
				tmpTreeVal = sr_seq.getTreeId();
				variantGroup.setOrigNode(tmpNodeVal);
				variantGroup.setTreeId(tmpTreeVal);
				//System.out.println("Pushing " + variantGroup.getTreeId());
			}
			*/

			if (clientServer == propertyParameters.Server && firstTimeServer) {
			// If we are doing distributed reachability testing, we need to give
			// each client some of the first batch of variants
				numberOfClientGroups = variantGroup.getNumOfVariants(); // number of variants of first exec. trace
				if (debug)
					writeLine("number of groups in first batch is " + numberOfClientGroups);
				float half = numberOfClientGroups / (numOfHosts+1); // server keeps 1/nth; n = num clients + 1
				// the server keeps some of the first batch of variants also
				initialNumberOfServerGroups = Math.round(half);
				if (debug)				
					writeLine("number of groups in first batch given to server " + initialNumberOfServerGroups);

				if (numberOfClientGroups - initialNumberOfServerGroups < numOfHosts) {
					// first group goes to server as does every 1/(numOfHosts+1) group.
					// So we could need 6 groups to service 4 clients (6th and 5th group go to server).
					// If not enough we continue and just grab groups from server's buffer
					writeLine("Server: not enough initial groups for " + numOfHosts + " clients.");
				}
			}
			
			if (variantGroup != null) {
		
				if (clientServer == propertyParameters.Server) {
					if (firstTimeServer || numberOfClientGroups == 0 || numberOfClientGroups<=initialNumberOfServerGroups) {
						// Deposit if this is the group for the first execution trace (firstTimeServer =true,
						//  or all groups for clients have been set aside.
						// First variant group (generated from first execution trace) is used to generate groups for the clients
						//output.println("do deposit: num_of_groups= " +num_of_groups+" numberOfClientGroups= " + numberOfClientGroups);
						out.deposit (variantGroup);
						if (!firstTimeServer && numberOfClientGroups>0) { // once numberOfClientGroups is zero is stays zero
						   // note that we gave a group to a client
							if (debug)						   
								writeLine("do deposit: numberOfClientGroups= " + numberOfClientGroups);
							numberOfClientGroups--; // stop giving groups to clients when this reaches zero
							/*
							VariantGroup delim = new VariantGroup(null,null);
							out.deposit(delim); // deposit a delimiter on the buffer so 
										  // we know when we are about to process an initial group
							*/
						}
						else if (firstTimeServer) {
							if (debug)
								writeLine("do deposit: first group");
						}
						firstTimeServer = false;
		    		}
		    		else { // not the first group and havn't given the clients their groups
		    			if (numberOfClientGroups>0) { // give group to a client
  							if (debug)
								writeLine("split: " +"numberOfClientGroups= " + numberOfClientGroups);
							// give group to the clients, in turn
							ServerSplitter.instance(numOfHosts).write(groups,variantGroup);
							numberOfClientGroups--;
						}
						if (numberOfClientGroups <= initialNumberOfServerGroups) {
							synchronized(groups) {
								if (debug)
									writeLine("notify server thread ");
								groupsGenerated = true;
								groups.notify(); // notify clientServerThread that clients can be given their groups
							}
		    			}
					}
		    		// bookkeeping
		    		num_of_groups ++;
		    		num_of_vars = variantGroup.getNumOfVariants();			
				}
				else { // if not doing dist. reachability testing, deposit group
					if (!isSplit || num_of_groups == 0) {
						out.deposit (variantGroup);
		    		}
		    		else { // split groups into n groups so 1 computer can do them in batches
						Splitter.instance(numOfHosts).write(variantGroup);
		    		}

		    		// bookkeeping
		    		num_of_groups ++;
		    		num_of_vars = variantGroup.getNumOfVariants();
		   		}
			}
			else num_of_vars = 0;

			// Added for DME 4 due to large number of sequences
			if (num_of_seqs % hash == 0) {
				   int groupsLeft=0;
					if (VariantGenerator.getInstance().variantGroup != null && VariantGenerator.getInstance().variantGroup.hasNext())
						groupsLeft = out.size()+1;
						//if distributing srSeqs
						//groupsLeft = out.size()+1+in.size();
					else 
						groupsLeft = out.size();
						//if distributing srSeqs
						//groupsLeft = out.size()+in.size();
					 
		       	writeLine(num_of_seqs + " "  +  (groupsLeft) + " " + num_of_vars); /* DISABLE + " " + tmpNodeVal); */

				}
			}
		}
    }

   private boolean addSeq (Vector seqs, SrSeqPO seq, boolean keep) {
	// check duplicates
		boolean rval = true;
		for (int i = 0; i < seqs.size (); i ++) {
		 	SrSeqPO it = (SrSeqPO) seqs.get(i);
	    	if (seq.equals(it)) {
				writeLine("Duplicate Seq: ");
				writeLine("Seq A:");
				// translate to TO
				srSeq seqTO = SrSeqTranslator.convert (seq);
				writeLine(seqTO.toString());
				System.out.println("Seq A:");
				//seq.dump ();
				System.out.println(seq);
				writeLine("Seq B:");
				// translate to TO
				srSeq itTO = SrSeqTranslator.convert (it);
				writeLine(itTO.toString());
				System.out.println("Seq B:");
				//it.dump ();
				System.out.println(itTO);
				rval = false;
				break;
	    	}
		}

		if (rval) {
	 		if (keep) {
				if (dupCheckMode == RANDCHECK || dupCheckMode == CUSTCHECK ) {
			 	// For debugging only.
		    	// if limit is reached, then randomly pick up a
		    	// sequence to remove
		    	if (seqs.size () == CHECKLIMIT) {
					int index = rand.nextInt (seqs.size());
					seqs.remove (index);
		    	}
			}
			seqs.add (seq);
	 	}
	}
	else {
	    output.close ();
	    System.out.println("Duplicates found, and system exits!!!");
	    System.exit (1);
	}	

	return rval;
 }

    private void writeGene (SrSeqPO seq) {
		if (outputGene == null) {
	  		try {
				FileOutputStream ostream = new FileOutputStream (GENE_FN);
				outputGene = new PrintWriter (ostream);
	    	}
	    	catch (Exception ex) {
				ex.printStackTrace (System.out);
	    	}
		}
		outputGene.println(seq.getGene());
		outputGene.flush ();
    }

    private void writeLine (String line) {
		if (output == null) {
	  		try {
				if (clientServer != propertyParameters.Client) {
					FileOutputStream ostream = new FileOutputStream (STATISTICS_FN);
					output = new PrintWriter (ostream);
				}
				else {
	   			String stringID = propertyReader.getInstance().getWorkerNumberProperty();
					FileOutputStream ostream = new FileOutputStream ("stats"+stringID+".txt");
					output = new PrintWriter (ostream);		
				}
	    	}
	    	catch (Exception ex) {
				ex.printStackTrace (System.out);
	    	}
		}
		output.println(line);
		output.flush ();
    }
    
   synchronized void saveRTResult(int ID, RTResult result) {
		if (resultsFile == null) {
			try {
				FileOutputStream ostream = new FileOutputStream (RESULTSFILE_FN);
				resultsFile = new PrintWriter (ostream);
			}
			catch (Exception ex) {
				ex.printStackTrace ();
			}
		}
		resultsFile.println();
		if (ID != 0)
			resultsFile.println("Reachability Testing completed for Worker "+ ID);
		else
			resultsFile.println("Reachability Testing completed for Manager");
		resultsFile.println("  Executions:"+result.getNumSequences()+" / Sequences Collected:"+result.getCollected()); // +"/"+transitionCount+"/"+eventCount);
	   resultsFile.println("  Elapsed time in minutes: " + result.getRTStopWatch().elapsedMinutes());
	   resultsFile.println("  Elapsed time in seconds: " + result.getRTStopWatch().elapsedSeconds());
	   resultsFile.println("  Elapsed time in milliseconds: " + result.getRTStopWatch().elapsedMillis());
		resultsFile.flush();
	   resultsFile.println();
  		if (ID==0) {
			if (previousServerResultsSequences !=0) { // results reported previously, so only add the difference
				addToTotalSequences((result.getNumSequences()-previousServerResultsSequences),(result.getCollected()-previousServerResultsCollected));
				previousServerResultsSequences = result.getNumSequences();
				previousServerResultsCollected = result.getCollected();
			}
			else {
				addToTotalSequences(result.getNumSequences(),result.getCollected());
				previousServerResultsSequences = result.getNumSequences();
				previousServerResultsCollected = result.getCollected();
			}
		}
		else { // only ever called once in client (at termination)
		   addToTotalSequences(result.getNumSequences(),result.getCollected());
		}
   	resultsFile.println("(sub)Total Executions:"+ getTotalSequences() + " (sub)Total Sequences Collected:"+getTotalCollected());
   	resultsFile.flush(); 
	
	}
    
    
    //writes out a string of comma-separated values that denote the nodes corresponding to variant groups (ie: 0,5,15,3)
    private void writeTreeId (String line) {
	    if (treeOut == null) {
		    try {
			    FileOutputStream ostream = null;
			    if (clientServer != propertyParameters.Client) {
				    ostream = new FileOutputStream ("trees.txt");
			    } else {
				    String stringID = propertyReader.getInstance().getWorkerNumberProperty();
				    ostream = new FileOutputStream ("trees" + stringID + ".txt");
			   	}
			   	treeOut = new PrintWriter(ostream);
		   	} catch (Exception ex) {
			   	ex.printStackTrace (System.out);
		   	}
	   	}
	   	treeOut.println(line);
	   	treeOut.flush();
   	}

    private class clientServerThread extends Thread {
    // Clients creat this thread with clientServer == propertyParameters.Client,
    // while servers create it as clientServer == propertyParameters.Server
	    private propertyParameters.ClientServer clientServer;
       private int ID = 0;
       private int consecutiveTimesEmpty=0;
	    private clientServerThread(propertyParameters.ClientServer clientServer, int ID) {
                super("rx2clientServerThread"+ID);
                this.ID = ID;
   	 			 this.clientServer = clientServer;
   	 }
   	 public void run() {
			if (debug)
	   	 	writeLine("max memory is " +  Runtime.getRuntime().maxMemory());
   	 	if (clientServer == propertyParameters.Server) 
   	 		doServer();
   	 	else
   	 		doClient();
   	 }
   	 
	  	 private class clientHandler /*extends Thread*/ implements Runnable {
	  	 // When clients try to steal groups from the server, this thread handles
	  	 // the client's request. 
  		 	private Socket socket;
	  	 	private ObjectOutputStream toClient;
	  	 	private ObjectInputStream fromClient;
	  	 	private clientRequest request;
	  	 	private HashMap InetAddresses;
	  	 	private HashSet thieves;
	  	 	private HashSet victims;
	  	 	private ArrayList clientHandlers;
	  	 	private ArrayList serversForClients;
	  	 	private ArrayList clientLocks;
	  	 	private boolean stopped = false;
	  	 	clientHandler(Socket socket, ObjectOutputStream toClient, ObjectInputStream fromClient,
	  	 	              clientRequest request, HashMap InetAddresses, HashSet thieves, HashSet victims,
	  	 	              ArrayList clientHandlers, ArrayList serversForClients,
	  	 	              ArrayList clientLocks) {

	  	 	   //super("dr3clientHandler"+request.getID());
	  	 	   this.socket = socket;
		 	   this.toClient = toClient;    // streams are already open
	  	 	   this.fromClient = fromClient;
	  	 	   this.request = request;
  		 	   this.InetAddresses = InetAddresses; // already contains client
	  	 	   this.thieves = thieves;
	  	 	   this.victims = victims;
	  	 	   this.clientHandlers = clientHandlers;
	  	 	   this.serversForClients = serversForClients;
	  	 	   this.clientLocks = clientLocks;
	      }
	      public void halt() {stopped = true;}
	      public void run() {
	        try {
				if (debug)
		       	writeLine("clientHandler for Client " + request.getID() + " running");
   	     	do {
   	     	  if (request.getRTResult() == null) {
      	  		try {
  						if (debug)
	      	  		   writeLine("clientHandler: got request from " + request.getID()
   	                 + " at " + Calendar.getInstance().getTime());
      	  		   // try to steal some of server's groups
      	  		   boolean stealFromServer = false;
  		 				ArrayList clientGroups = (ArrayList)out.withdrawN(); // returns list of groups
      	  		   // if distributing srSeqs
						//ArrayList clientGroups = (ArrayList)in.withdrawN(); // returns list of groups      	  		   
						if (clientGroups.size()==0) {
						   // The server has no groups, or only a few groups, and is not willing to
						   // give any to client, so 
							// get groups for client request.getID() from some other client
							if (debug)
								writeLine("clientHandler: calling getGroupsFromClient");
							clientGroups = getGroupsFromClient(request.getID(),InetAddresses,thieves,
							    victims,serversForClients,clientLocks);
							if (debug)
								writeLine("clientHandler: back from calling getGroupsFromClient");
						}
						else { // server is willing to give up some of its groups
							stealFromServer = true;
							synchronized(InetAddresses) {
							    // In the future, we won't steal from this client since it has
							    // already to to steal some for itself, i.e., it is a thief
			   			 	thieves.add(new Integer(request.getID()));
			   			 	victims.remove(new Integer(request.getID()));
			   			 	if (InetAddresses.size() == thieves.size()) {
			   			 		resetGroupsSentReceived(); // reset to 0 for new round of stealing
	 		               	thieves.clear();	// all clients are thieves so reset
	 		               	victims.clear();  // victims should already be empty
	 		               	thieves.add(new Integer(request.getID()));	// this client is a thief
	 		               }
							}
						}
 	   	 			writeLine("clientHandler: sending " + clientGroups.size() + " more groups to Worker "+request.getID()+ " at " 
	 	   	 		          + Calendar.getInstance().getTime());   	 			
 		   	 		boolean success = false;
 	   	 			try { // send groups to client
  	 						toClient.writeObject(clientGroups);
   	 					toClient.flush();
		 	   	 		success = true;
		 	   	 		if (stealFromServer)
		 	   	 			addNumGroupsSent(clientGroups.size());
		 	   	 		else 
		 	   	 			addNumGroupsSentReceived(clientGroups.size());

							try {Thread.sleep(1000);}catch(InterruptedException e) {}
							try {
								toClient.reset();
								toClient.writeObject(Boolean.valueOf(true));
								toClient.flush();
							} catch (Exception e) {
								if (debug)
		 	   		 			writeLine("Exception in clientHandler doing reset() after sending more groups to Client " + request.getID() + "at " + Calendar.getInstance().getTime() + ": " + e);
								e.printStackTrace();
	 	   	 				try {
	 	   	 					socket.close();fromClient.close();toClient.close();
		 	   	 			}	
		 	   	 			catch (Exception e3) {}	
			   	 			synchronized(clientHandlers) {
			   	 				clientHandlers.set(request.getID(),null);
			   	 			}
		 		   	 		return;
							}
		 	   	 	}
	 		   	 	catch (Exception e) {
	 						if (debug)
		 	   		 		writeLine("Exception in clientHandler sending more groups to Client " + request.getID() + "at " + Calendar.getInstance().getTime() + ": " + e);
							e.printStackTrace();
	 	   	 			try {
	 	   	 				socket.close();fromClient.close();toClient.close();
		 	   	 		}	
		 	   	 		catch (Exception e3) {}
		 	   		}
		 	   	 	if (!success) { // connection failed while sending groups
   	 					if (debug)
			 	   	 		writeLine("clientHandler:  Redeposit groups.");
		 	   	 		try {
			 	   	 		if (clientGroups.size()>0) { // deposit variants back in out buffer
		  		 					Iterator i = clientGroups.iterator();
					  					while (i.hasNext()) {
					  						// if distributing srSeqs
 	 										//srSeq variantGroup = (srSeq) i.next();
 	 										VariantGroup variantGroup = (VariantGroup) i.next();
 	 										if (variantGroup == null) {
												if (debug)
		 					 						writeLine("Error: doServer: manager received empty variant group!");	
												//System.exit (1); 	
			 									}
		   		 						else { 
											out.deposit(variantGroup); // make available to application
					  						// if distributing srSeqs
											//in.deposit(variantGroup); // make available to application

										}
									}
			   	 			}	
	 							if (debug)
				   	 			writeLine("Redeposited groups." + " ClientHandler returning.");
			   	 		}
         	         catch (Exception e4) {
  	         				if (debug)
	         	            writeLine("clientHandler: *****Exception while redepositing groups*****" + e4);
			  		 			e4.printStackTrace();
         	         }
		   	 			synchronized(clientHandlers) {
		   	 				clientHandlers.set(request.getID(),null);
		   	 			}
		 	   	 		return;
			   	 	}
						clientGroups = null;

						//try {
							//toClient.close();
	   			 		//ObjectOutputStream toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	   			 		//toClient.flush();
	   			 		//fromClient.close();
	   		 			//ObjectInputStream fromClient = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
   		 			//} catch (Exception e3) {
  		           	// 	e3.printStackTrace();
		            //	writeLine("Exception in clientHandler thread creating Object Streams: " + e3); 		 			
   		 			//}

   	 				try {
   	 					socket.close();fromClient.close();toClient.close();
   	 					socket = null; fromClient = null; toClient = null;
 	   	 			}	
 	   	 			catch (Exception eClose) {socket = null; fromClient = null; toClient = null;}	
         			//request = (clientRequest) fromClient.readObject();
         			stopped = true;
         			
         			// clients do not reset() this stream; they send one object possibly many times
	     	  		}
	     			catch(Exception e) {
	   	 			synchronized(clientHandlers) {
	   	 				clientHandlers.set(request.getID(),null);
	   	 			}
		            e.printStackTrace();
        				if (debug)
			            writeLine("Exception in clientHandler thread: " + e); 
 	   	 			try {
 	   	 				socket.close();fromClient.close();toClient.close();
	 	   	 		}	
	 	   	 		catch (Exception e3) {}
	               return;
	     	      }
	     	    }
	     	    else {
	     	     try {
	     	     	RTResult result = request.getRTResult();
	     	     	if (debug) 
		     	     	writeLine("ClientHandler"+request.getID()+" received RTResult at "+Calendar.getInstance().getTime()
		     	     	          +" from worker " + request.getID());
	     	      result.getRTStopWatch().end();  // capture end time (may also be captured on server too)

	    			System.out.println();
	    			System.out.println("Reachability Testing completed for Worker "+ request.getID());
					System.out.println("  Executions:"+result.getNumSequences()+" / Sequences Collected:"+result.getCollected()); // +"/"+transitionCount+"/"+eventCount);
	     			System.out.println("  Elapsed time in minutes: " + result.getRTStopWatch().elapsedMinutes());
	     			System.out.println("  Elapsed time in seconds: " + result.getRTStopWatch().elapsedSeconds());
	     			System.out.println("  Elapsed time in milliseconds: " + result.getRTStopWatch().elapsedMillis());
					System.out.flush();
	     	      decNumberRunningClients();	     			
	     	      synchronized(InetAddresses) {
	     	      	InetAddresses.remove(request.getID());
	     	      }
	     	      saveRTResult(request.getID(),result);
	     	      if (modularTesting == propertyParameters.MTON) {
	     	      	if (result.getUniqueSequences() != null) {
							synchronized(classLock) {
								try {
									  RTDriverSBRT.saveWorkerSequences(result.getUniqueSequences(),request.getID());
									  //ObjectOutputStream uniqueSequencesFile = new ObjectOutputStream(new FileOutputStream("uniqueSequences"+request.getID()+".dat"));
									  //uniqueSequencesFile.writeObject(result.getUniqueSequences());
								}
								catch (Exception e) {
						     		writeLine("Error while writing uniqueSequences"+request.getID()+".dat file: " + e.toString()); 
						   	}		
						   }
	     	      	}	
	     	      }
	   			System.out.println();
  	   			System.out.println("(sub)Total Executions:"+ getTotalSequences() + " (sub)Total Sequences Collected:"+getTotalCollected());
					System.out.flush();  
					
					stopped = true;
	     	     }
	     	     catch (Exception e) {}
	     	    
	     	    }
	   		} while (!stopped); // if connection fails, a new clientHandler will be created
	   		                    // and this clientHandler will be stopped
	 			try {
 					socket.close(); toClient.close(); fromClient.close();
 					socket = null; fromClient = null; toClient = null;
 				} catch (Exception e) {socket = null; fromClient = null; toClient = null;}
			} catch(Exception e) {
				e.printStackTrace();
     	     	if (debug) 
					writeLine("Exception in clientHandler thread run()." + e); 
  	 			try {
   	 			socket.close();fromClient.close();toClient.close();
   	 			socket = null; fromClient = null; toClient = null;
   	 		}	
   	 		catch (Exception e3) {}
			}
		  } // run
		}   	 
   	 
   	 private void doServer() {
   	 // clientServerThreads that are playing the role of server execute this code:
   	 // 1. wait for the generation of the initial batch of variant groups.
   	 //    These groups will be distributed to the clients when they request them
   	 // 2. Service requests from clients for more groups
   	 	// wait for clients to request list of groups
   	 	ServerSocket listen;
   	 	HashMap InetAddresses = new HashMap();
   	 	HashSet thieves = new HashSet(); // clients who have stolen groups from others
   	 	HashSet victims = new HashSet(); // clients who've had groups stolen from them
   	 	ArrayList clientHandlers = new ArrayList(numOfClients+1);
   	 	ArrayList serversForClients = new ArrayList(numOfClients+1);
   	 	ArrayList clientLocks = new ArrayList(numOfClients+1);
   	 	final int maxPoolThreads = 20;
   	 	ExecutorService exec = Executors.newFixedThreadPool(numOfHosts<maxPoolThreads?numOfHosts:maxPoolThreads);
   	 	
			//System.out.println("client-server, listen for clients");
   	 	try {
   	 		//writeLine("pool size"+(numOfHosts<maxPoolThreads?numOfHosts:maxPoolThreads));
            for (int i=0; i<numOfClients+1; i++)
              clientHandlers.add(null);
            for (int i=0; i<numOfClients+1; i++)
              serversForClients.add(null);
            for (int i=0; i<numOfClients+1; i++)
              clientLocks.add(new Object());
   	 		listen = new ServerSocket(serverPort);
   	 		// wait for initial batch of groups to be generated
	   	 	synchronized (groups) {
   		 		if (!groupsGenerated) { // wait for splitting to complete
   		 			try {
			     	     	if (debug) 
	   	 					System.out.println("Server: wait for groups");
   	 					groups.wait();
			     	     	if (debug) 
	    	 					System.out.println("Server: notified");
	   	 			} catch (InterruptedException e) {}
					}   	 			
   		 	}  
   		 	int clientsServiced = 0; // number of clients that have received groups
   		 	boolean serverWatchdogStarted = false; //set to true when serverWatchdog started
   	 		while (true) {
             try {
   	 		// clients get their initial groups on their first call; they may make more
   	 		// calls to get more groups after that.
   	 		
				// Note: There may not have been enough groups in first batch to give one group
 				// to all the clients. (Need to increase the size of the first batch.)
	   	 		Socket socket = listen.accept();
   		 		ObjectOutputStream toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
   		 		toClient.flush();
   		 		ObjectInputStream fromClient = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
 	   	 		//System.out.println("Server: waiting for client request");
   	 			clientRequest request = (clientRequest) fromClient.readObject();
   	 			

   		 		//ObjectOutputStream toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
   		 		//toClient.flush();

   	 			// clients do not reset this stream; only one small message is passed
 	   	 		//System.out.println("Server: got request from client "+ request.getID());

					// put host InetAddress in map with client's ID, this enables us
					// to request groups from the client, if necessary
					synchronized(InetAddresses) {
						// old value is replaced
						InetAddresses.put(new Integer(request.getID()),socket.getInetAddress());
					}
		
 	   	 		if (request.getID() > 0 && request.getID() <= numOfHosts) {
	  		 			ArrayList clientGroups = (ArrayList)groups[request.getID()];
						// If this is the client's first request, clientGroups.size() > 0 will
						// be true. 
	   	 			if (clientGroups.size() > 0) {
		 	   	 		writeLine("Server: sending " + clientGroups.size() + " initial groups to worker "+request.getID()+ " at " + Calendar.getInstance().getTime()); 
		 	   	 		try {
	   			 			toClient.writeObject(clientGroups);
	   			 			toClient.flush();
	   			 			// no reset as this socket used to send only one message
                        //try {Thread.sleep(1000);}catch(InterruptedException e) {}
                        //try {
	                     //   toClient.reset();
	  							//	toClient.writeObject(Boolean.valueOf(true));
								//	toClient.flush();
		                  //} catch (Exception resetException) {
	          				//	writeLine("Exception doing reset() after sending client's initial request; initial groups still available: " + resetException);           
	          				//	resetException.printStackTrace();
	                     //}
 		   			 		//System.out.println("Server: sent initial groups to client "+request.getID()); 
 	   				 		// clear ID's list of groups so subsequent calls are viewed as requests
 	   			 			// for more groups, not the request for the initial groups.
 	   			 			((ArrayList)groups[request.getID()]).clear();
				   	 		if (clientsServiced < numOfHosts)
								  clientsServiced++;
 	   			 		} catch (Exception e) {
 	   			 			writeLine("Exception sending client's initial request; initial groups still available: " + e);
 	   			 		}
 				   	 	try {
		   	            socket.close();fromClient.close();toClient.close();
		   	            // value in InetAddresses will be replaced when client calls again
			            } catch (Exception e) {}
		   	 		}
		   	 		else { // this is a client request for more variant groups 
		   	 			synchronized(clientHandlers) {
		   	 			   // There is one clientHandler for each client. If a clientHandler
		   	 			   // already exists, then a previous connection was broken so we
		   	 			   // are going to create a new clientHandler to service the new connection
		   	 			   // clientHandlers are stored in a vector indexed by client ID
		   	 			   
				   	 		//clientHandler t = (clientHandler) clientHandlers.get(request.getID());

				   	 		//if (t != null) {
	                     //   writeLine("doServer() stopping clientHandler for Client " + request.getID());
				   	 		//	t.halt();	// stop old t from running since client openened new socket
				   	 		//	t = null;
				   	 		//	clientHandlers.set(request.getID(),t); // save clientHandler
				   	 		//}

				   	 		//socket.setKeepAlive(true);
				   	 		
				   	 		// clientHandler will try to steal groups and send them to the client
				   	 		clientHandler t = new clientHandler(socket,toClient,fromClient,request,InetAddresses,thieves,
				   	 				victims, clientHandlers, serversForClients,clientLocks);
				   	 		//t.setPriority(Thread.MAX_PRIORITY);
				   	 		//t.start();
				   	 		//clientHandlers.set(request.getID(),t);
				   	 		exec.execute(t);
 	 			     	     	if (debug) 
	                        writeLine("doServer() starting clientHandler for Client " + request.getID() +
   	                       " at " + Calendar.getInstance().getTime());
				   		 }
		   	 		}
		   	 	}
		   	 	else { // new client joined
	   	 			synchronized(clientHandlers) {
			   	 		//clientHandler t = (clientHandler) clientHandlers.get(request.getID());
			   	 		//if (t != null) {
	                  //   writeLine("doServer() stopping clientHandler for Client " + request.getID());
			   	 		//	t.halt();	// stop old t from running since client openened new socket
			   	 		//	t = null;
			   	 		//	clientHandlers.set(request.getID(),t);
			   	 		//}
			   	 		//socket.setKeepAlive(true);
			   	 		clientHandler t = new clientHandler(socket,toClient,fromClient,request,InetAddresses,thieves,
			   	 				victims, clientHandlers, serversForClients,clientLocks);
			   	 		//t.setPriority(Thread.MAX_PRIORITY);
			   	 		//t.start();
			   	 		//clientHandlers.set(request.getID(),t);
			   	 		exec.execute(t);
 			     	     	if (debug) 
	                     writeLine("doServer() starting clientHandler for Client " + request.getID());
			   		 }
		   	 	}
		   	 	//try {
	            //   socket.close();fromClient.close();toClient.close();
	            //} catch (Exception e) {}

					  if (!(serverWatchdogStarted) && clientsServiced == numOfHosts) { // clients have started
               	/*change due to node 21 being down */
						//if (!(serverWatchdogStarted) && clientsServiced == (numOfHosts-1)) { // clients have started
               	/*changed due to dual mode and node 21 being down */
						//if (!(serverWatchdogStarted) && clientsServiced == (numOfHosts-2)) { // clients have started
		     	     	if (debug) 
							writeLine("server starting watchdog, clientsServiced is " + clientsServiced);
		   	 		serverWatchdog s = new serverWatchdog(InetAddresses,thieves,victims,serversForClients,clientLocks);
			   	 	s.start();
		   		 	serverWatchdogStarted = true;
			   	}
             }
             catch (Exception e) {
  	     	     	if (debug) 
	               writeLine("Exception in doServer():" + e);
               e.printStackTrace();
             }
  	   	 	} // end while
	   	 } catch (IOException e) {
               e.printStackTrace();
	     	     	if (debug) 
   	            writeLine("Exception in doServer():" + e); 
            }
	     	   catch (Exception e) {e.printStackTrace();
    	     	   if (debug) 
                 writeLine("Exception in doServer():" + e);
            }	 	
   	 }
   	 private ArrayList getGroupsFromClient(int clientID, HashMap InetAddresses, HashSet thieves,
   	 	HashSet victims, ArrayList serversForClients, ArrayList clientLocks) {
   	   // ID is the ID of client we are servicing, InetAddresses maps clients to their addresses:
   	   // client ID ->InetAddress 
   	 	ArrayList variantGroups = new ArrayList();
   	 	Iterator i;
   	 	ArrayList entries;
  	     	if (debug) 
	   	 	writeLine("getGroupsFromClient: get groups for server/client "+clientID+", there are " 
   	 	 + InetAddresses.size() + " clients to query");
         /* Q: synch whole thing? */
   	 	try {
    	     	if (debug) 
	   	 		writeLine("InetAddresses.size()="+InetAddresses.size()+"thieves.size()="+thieves.size()
   			 		+"victims.size()="+victims.size());
   	 	   // Thieves are clients who have already stolen groups. We don't want to steal
   	 	   // back some of the groups they have stolen, if we can avoid it
	   	 	synchronized(InetAddresses) {
	   	 		if (clientID != 0) { // don't add server to thieves
			   	 	thieves.add(new Integer(clientID));
			   	 	victims.remove(new Integer(clientID));	
			   	}
	   	 		HashMap InetAddressesClone = (HashMap) InetAddresses.clone();
	   	 		// If some clients are not yet thieves, remove all the thieves
	   	 		// from the client list
	   	 		if (InetAddressesClone.size() != thieves.size()) {
						Iterator i2 = thieves.iterator();
						while (i2.hasNext()) {
							Integer ID = (Integer) i2.next();
							InetAddressesClone.remove(ID);
						}
						if (InetAddressesClone.size() != victims.size()) {
							Iterator i3 = victims.iterator();
							while (i3.hasNext()) {
								Integer ID = (Integer) i3.next();
								InetAddressesClone.remove(ID);
							}							
						}
					}
               else { // all the clients are thieves; we have no choice but to 
                      // start another round ..
     	     	     	if (debug) 
	                  writeLine("getGroupsFromClients: all the clients are thieves");
               	if (thieves.size() > InetAddressesClone.size()) {
     		     	     	if (debug) 
	               		writeLine("Error: more thieves than entries");
               	}
               	thieves.clear();	// all clients are thieves so reset
               	victims.clear();  // reset victims too
               	resetGroupsSentReceived();
               	
               	if (clientID != 0) { // don't add server to thieves
			   	 		thieves.add(new Integer(clientID));	// this client is a thief
	               	//entries.removeAll(thieves); // don't steal from a thief
	            		Iterator i2 = thieves.iterator();
							while (i2.hasNext()) {
								Integer ID = (Integer) i2.next();
								InetAddressesClone.remove(ID);
							}
	               }
               }
               // entries contains the clients we can steal from 
               entries = new ArrayList(InetAddressesClone.entrySet());
               // consider rotate instead
               // shuffle the client list so we will query clients in a random order
	   	 		Collections.shuffle(entries);
	   	 		//Collections.rotate(entries,-1);
	   			//i = InetAddressesClone.entrySet().iterator();
	     	     	if (debug) 
		   			writeLine("After removing thieves, there are " + entries.size() + " clients to query for "+clientID);
	   			i = entries.iterator();
	   		}
	   		//ArrayList deleteList = new ArrayList(); // if client returns 0 groups, don't call it again
	   		while (i.hasNext()) {
		   		Integer IntegerID = null;
	   			try {
			  			Map.Entry e = (Map.Entry) i.next();
   					IntegerID = (Integer) e.getKey();
   					// Don't send a request to the client who is requesting more groups
   					if (IntegerID.intValue() != clientID) {
			     	     	if (debug) 
								writeLine("getGroupsFromClients: Try " + IntegerID.intValue() + " for " + clientID);
			   			InetAddress host = (InetAddress) e.getValue();
			   			synchronized(InetAddresses) {
			   				// other clientHandler may have already selected this one

								// must allow victims to be stolen from, but not workers that have just become a thief

			   				if (thieves.contains(IntegerID) /* || victims.contains(IntegerID)*/) { // might have become a thief prior to this
  					     	     	if (debug) 
				   					writeLine("getGroupsFromClients: continue");
			   					continue; // may still steal from a thief as there is a race - okay
			   				}
			   				else victims.add(IntegerID);
			   			}
			   			Socket socket = null;
	   		   	 	ObjectOutputStream toClient = null;
                                         
   					 	ObjectInputStream fromClient = null;
			   			try {	

			   				//Object l = clientLocks.get(IntegerID.intValue());
								//writeLine("getGroupsFromClients: try to get lock for " + IntegerID.intValue());
			   				//synchronized(l) {
			   					try {
										// writeLine("getGroupsFromClients: got lock for " + IntegerID.intValue());
				   				   // use a socket opened previously if one exists
				   					socketInfo s = (socketInfo) serversForClients.get(IntegerID.intValue());
				   					if (s == null) { // create (first) socket to client
   						     	     	if (debug) 
												writeLine("getGroupsFromClients: create first socket for client "+ IntegerID);
						 	 				socket = new Socket();
				 		 					socket.bind(null);
	 						     	     	if (debug) 
											 	writeLine("getGroupsFromClient: connect() to server for client "+IntegerID);
				  	 						socket.connect(new InetSocketAddress(host,serverPort+IntegerID.intValue()),5000);
			 		 						socket.setSoTimeout(SOCKETTIMEOUT);	   			
											socket.setKeepAlive(true);
					 			 			//Socket socket = new Socket(host,serverPort+IntegerID.intValue());
				   						toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
					   					toClient.flush();
						   		 		clientRequest request = new clientRequest(clientID);
	   		 			     	     	if (debug) 
						   	 				writeLine("getGroupsFromClient: send request to Client " + IntegerID+" for a batch of variants for "+clientID);  	 		
						   		 		toClient.writeObject(request); toClient.flush();
	         	                  try {Thread.sleep(1000);}catch(InterruptedException e3) {}
	            	               toClient.reset();
           								toClient.writeObject(Boolean.valueOf(true));
											toClient.flush();
							     	     	if (debug) 
												writeLine("getGroupsFromClient: get input stream to read groups from Client " + IntegerID + " for client " + clientID); 			   		 		
				  				 			fromClient = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			 				     	     	if (debug) 
				   		 					writeLine("getGroupsFromClient: starting read for getting groups from Client " + IntegerID + " for client " + clientID);  
							  		 		variantGroups = (ArrayList) fromClient.readObject();
							  		 		try {
							  		 			// read Boolean so that reset() occurs
							  		 			Boolean readReset = (Boolean) fromClient.readObject(); 
							  		 			// groups were apparently read correctly
							  		 		}
							  		 		catch (Exception eBoolean) {
			  		 			     	     	if (debug) 
								  		 			writeLine("getGroupsFromClient: exception reading resetBoolean when getting groups from Client "+IntegerID + " for client " + clientID);
							  		 		}
							  		 		if (clientID==0)
							   		 		writeLine("Manager: stole "+variantGroups.size()+" groups from Worker "+IntegerID);
							   		 	else
							   		 		writeLine("Manager: stole "+variantGroups.size()+" groups from Worker "+ IntegerID
							   		 		 + " for Worker " + clientID);
						   		 		//writeLine("getGroupsFromClient"+IntegerID+" total memory is " + Runtime.getRuntime().totalMemory());
						   		 		//toClient.close(); 
						   		 		//toClient = null;
						   		 		//fromClient.close(); 
					   			 		//fromClient = null;
 						  		 		try {
						  		 			if (socket != null) {
								  		 		socket.close(); toClient.close(); fromClient.close();
								  		 	}
				  					 	} catch(Exception e4) { }
										
					   			 		//s = new socketInfo(socket,toClient,fromClient);
					   			 		//serversForClients.set(IntegerID.intValue(),s);
					   	 			}
						   	 		else {  // use existing socket
                                                                //writeLine("getGroupsFromClients: use existing socket");
							   			socket = s.socket;
						   			
					   		   	 	toClient = s.toClient;
				   		   	 	
					   					//toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
					   					//toClient.flush();

				   					 	fromClient = s.fromClient;
			   					 	
						   		 		clientRequest request = new clientRequest(clientID);
   		 			     	     		if (debug) 
					   	 					writeLine("getGroupsFromClient: send request to Client " + IntegerID+" for a batch of variants for client " + clientID);  	 		
						   		 		toClient.writeObject(request); toClient.flush();
	   	                        try {Thread.sleep(1000);}catch(InterruptedException e3) {} 
	      	                     toClient.reset(); // reset object cache
           								toClient.writeObject(Boolean.valueOf(true));
											toClient.flush();
							     	     	if (debug) 
												writeLine("getGroupsFromClient: get input stream to read groups from Client " + IntegerID + " for client " + clientID); 			   		 		
							     	     	if (debug) 
				   			 				writeLine("getGroupsFromClient: starting read for getting groups from Client " + IntegerID + " for client " + clientID);  
				  				 			//fromClient = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
						  			 		variantGroups = (ArrayList) fromClient.readObject();
							  		 		try {
							  		 			// read Boolean so that reset() occurs
							  		 			Boolean readReset = (Boolean) fromClient.readObject();
							  		 			// groups were apparently read correctly
							  		 		}
							  		 		catch (Exception eBoolean) {
			  		 			     	     	if (debug) 
								  		 			writeLine("getGroupsFromClient: exception reading resetBoolean when getting groups from Client "+IntegerID + " for client " + clientID);
							  		 		}
					   			 		writeLine("getGroupsFromClient: got "+variantGroups.size()+" groups from Client "+IntegerID + " for client " + clientID);
						   		 		//writeLine("getGroupsFromClient"+IntegerID+" total memory is " + Runtime.getRuntime().totalMemory());	   					 	
						   		 		//toClient.close(); 
						   		 		//toClient = null;
						   		 		//fromClient.close(); 
						   		 		//fromClient = null;
						   	 		}
						   	 	}
						   	 	catch (SocketTimeoutException e3) {
		   	 		     	     	if (debug) 
				 		   	 			writeLine("getGroupsFromClient:timeout reading groups from worker " +IntegerID+" server for client " + clientID + " at "+ Calendar.getInstance().getTime() + ":" + e3);
 						  		 		try {
						  		 			if (socket != null) {
								  		 		socket.close(); toClient.close(); fromClient.close();
								  		 	}
				  					 	} catch(Exception e4) { }
				  					 	serversForClients.set(IntegerID.intValue(),null);
	 		   	 				}
	 		   	 				catch (Exception e7) {
  	 					     	     	if (debug) 
						  					writeLine("getGroupsFromClient: Exception communicating with client "+IntegerID+" for client " + clientID + " : "+e7);
   		                     e7.printStackTrace();
					   	 			//synchronized(InetAddresses) {
						  				//	InetAddresses.remove(IntegerID);	 		
						  				//}	 			  		 		
				  						try {
				  				 			if (socket != null) {
						  				 		socket.close(); toClient.close(); fromClient.close();
						  		 			}
							  		 	} catch(Exception e8) { }
						  			 	serversForClients.set(IntegerID.intValue(),null);
						  		 	}	

			   		 		//} // end synchronized (l)
								// writeLine("getGroupsFromClients: release lock for " + IntegerID);		
				   	 	}
							catch (OutOfMemoryError out1) {
					   		out1.printStackTrace();
   			     	     	if (debug) 
						   		writeLine("OutOfMemory Exception in getGroupsFromClient for client: " +IntegerID + "." + out1);			
							}

				  	 		if (variantGroups != null && variantGroups.size() > 0)
					  	 		break;
					  	 		
			  		 	}
			  		} 
					catch (OutOfMemoryError out2) {
			   		out2.printStackTrace();
		     	     	if (debug) 
				   		writeLine("OutOfMemory Exception in getGroupsFromClient:" + out2);			
					}
			  		catch (Exception e) {
			   		e.printStackTrace();
		     	     	if (debug) 
				  			writeLine("getGroupsFromClient: Exception dealing with Client"+IntegerID+ " for client " + clientID);
			   	 	//synchronized(InetAddresses) {
				  		//	InetAddresses.remove(IntegerID);	 		
				  		//}
			  		}
   			} // end while (i.hasNext())
				if (variantGroups != null && variantGroups.size() == 0) {
					synchronized(InetAddresses) {
						thieves.clear(); // may need to steal from a thief or victim
						victims.clear();
					} // put this in synchronized?
					//System.out.println("consecutiveTimesEmpty:"+ (++consecutiveTimesEmpty));
				}
				else {
					consecutiveTimesEmpty = 0;
				}
					
			}
			catch (OutOfMemoryError out3) {
	   		out3.printStackTrace();
	  	     	if (debug) 
		   		writeLine("OutOfMemory Exception in getGroupsFromClient:" + out3);			
			}
	   	catch (Exception e) {
	   		e.printStackTrace();
    	     	if (debug) 
		   		writeLine("Exception in getGroupsFromClient:" + e);
	   	}
	   	
  			return variantGroups;
   	 }
   	 
   	 private void doClient() {
   	 // clientServerThreads that are playing the role of client execute this code:
   	 // 1. get intial groups from server
   	 // 2. If the client is running out of variant groups, try to steal some from
   	 //    server
 
   	 	InetAddress host = null;
   	 	//int serverPort = 2020;
   	 	Socket socket = null;
   	 	ObjectOutputStream toServer = null;
   	 	ObjectInputStream fromServer = null;
   	 	int groupMin = 5;
   	 	String stringID = propertyReader.getInstance().getWorkerNumberProperty();
   	 	Random delayResult = new Random();
   	 	if (stringID == null) {
  				System.out.println("In the clientServer mode, processName(ID) property must be specified!");	
				System.exit (1); 
	  	 	}
	  	 	int ID = Integer.parseInt(stringID);
  	 		serverThreadForClients s = new serverThreadForClients(ID);
   	 	s.start();
  	 		clientRequest request = new clientRequest(ID);
   	 	try {
     	     	if (debug) 
	   	 		writeLine("request connection to server");
   	 		if (!serverIP.equals("LocalHost"))
   	 			host = InetAddress.getByName(serverIP);
   	 		else
	   	 		host = InetAddress.getLocalHost();
	   	 	// Get initial groups from server
  	 			socket = new Socket(host,serverPort);
   	 		toServer = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
   	 		toServer.flush();
   	 		//clientRequest request = new clientRequest(ID);
    	 		//System.out.println("Client: send request for initial batch of variants");  	 		
   	 		toServer.writeObject(request); 
   	 		toServer.flush();
   	 		// no reset(); only one message sent over this socket
            //try {Thread.sleep(1000);} catch(InterruptedException e3) {} 
            //toServer.reset();
  		 		fromServer = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
     	     	if (debug) 
	   	 		writeLine("doClient"+ID+": waiting to read initial groups");
  		 		ArrayList variantGroups = (ArrayList) fromServer.readObject();
    	     	if (debug) 
	   	 		writeLine("doClient"+ID+": read initial groups");
  		 		try {
	  		 		socket.close(); toServer.close(); fromServer.close();
	  		 	} catch (Exception e) {}
   	 		writeLine("Worker"+ID+": got " + variantGroups.size()+ " initial groups");
   	 		// Deposit groups in the out (bounded) buffer so that variants can be
   	 		// withdrawn and used for reachability testing
  		 		if (variantGroups.size() != 0) { // deposit variants in out buffer
  		 			Iterator i = variantGroups.iterator();
  		 			while (i.hasNext()) {
  		 				VariantGroup variantGroup = (VariantGroup) i.next();
  		 				if (variantGroup == null) {
  		 				  	//System.out.println("client received empty variant group!");	
							System.exit (1); 	
  		 				}
	 	   	 		//System.out.println("Client: deposit group"); 
	 	   	 		
	 	   	 		// If distributing srSeq, grab it from variantGroup
	 	   	 		//srSeq seq = SrSeqTranslator.convert(variantGroup.getOrigSeq());
	 	   	 		
						out.deposit(variantGroup); // make available to application
		 				// if distributing srSeqs
						//in.deposit(seq); // make available to application
					}
				}
				else {
	  				writeLine("No initial variant groups returned to worker"+ID+" -  at "+Calendar.getInstance().getTime()+" -> terminating");	
					System.exit (1); 
		  	 	}
  		 		
  		 	}
  	 		catch (IOException e) {
  	 			e.printStackTrace();
     	     	if (debug) 
	  	 			writeLine("Exception in doClient"+ID+":" + e 
  		 				+ " at "+Calendar.getInstance().getTime()+" -> no initial groups so terminating");
  	 			System.exit(1);
  	 		}	
  	 		catch (ClassNotFoundException e) {
  	 			e.printStackTrace();
     	     	if (debug) 
					writeLine("Exception in doClient"+ID+":" + e 
  		 				+ " at "+Calendar.getInstance().getTime()+" -> no initial groups so terminating");
  	 			System.exit(1);  	 			
 			}	
  	 		int attempts=0;
  	 		socket=null; toServer=null;fromServer=null;
  	 		// sleep a while to let the client start producing variants.
  	 		try {Thread.sleep(10000);}catch(InterruptedException e) {}
  	 		while (true) {
				try {
	  	 			try {Thread.sleep(2000);}catch(InterruptedException e) {}
  		 			// if distributing srSeqs
  		 			//int inBufferSize = in.size();
  		 			int outBufferSize = out.size();
	  		      ArrayList variantGroups = null;
	  		      //clientRequest request = new clientRequest(ID);
	  		      //if distributing srSeqs
	  		      //if (inBufferSize <=groupMin) { // check for near-empty buffer every 2 seconds
  	 				if (outBufferSize <=groupMin) { // check for near-empty buffer every 2 seconds
  	 					//requestMoreVariants
						if (terminator.sendRequest()) { // RT Driver has not halted			  			
			  				try {
			   				if (socket == null) {
		 	 						socket = new Socket();
 			 						socket.bind(null);
					     	     	if (debug) 
	 				   		 		writeLine("doClient"+ID+": connect() to server");
  	 								socket.connect(new InetSocketAddress(host,serverPort),20000);
 	 								socket.setSoTimeout(SOCKETTIMEOUT);
 	 								socket.setKeepAlive(true);
 	 							}
 	 						
		 						if (toServer == null) {
				   	 			toServer = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream())); 
				   	 			toServer.flush();
				   	 		}
	 		   	 			//request = new clientRequest(ID);
	 		   	 			if (debug)
	   		 					writeLine("Worker"+ID+": sending request for more variants at " + Calendar.getInstance().getTime());
			   	 			toServer.writeObject(request); toServer.flush();
         	            try {Thread.sleep(1000);}catch(InterruptedException e3) {}
         	            // No reset(); sending identical message, which is cached
			   	 		   //toServer.reset();	
 								//toServer.writeObject(Boolean.valueOf(true));
								//toServer.flush();
			   	 			if (fromServer == null)
					  				fromServer = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				     	     	if (debug) 
			   			 		writeLine("doClient"+ID+": waiting to read groups from manager");
			  			 		variantGroups = (ArrayList) fromServer.readObject();
	 			     	     	if (debug) 
	 			   	 			writeLine("doClient"+ID+": read groups from manager");
 							  	try {
							  		// read Boolean so that reset() occurs
							  		Boolean readReset = (Boolean) fromServer.readObject(); 
							  		// groups were apparently read correctly
							  	}
				  		 		catch (Exception eBoolean) {
  		 			     	     	if (debug) 
					  		 			writeLine("doClient"+ID+": exception reading resetBoolean when getting groups from manager");
				  		 		}

 				  		 		try {
				  		 			if (socket != null) {
						  				socket.close(); toServer.close(); fromServer.close();
						  		 	}
					  		 	} catch(Exception eClose) { }
					  		 	socket = null; toServer = null; fromServer = null;
					  		 	
 			   	 			//toServer.close(); 
 			   	 			//toServer = null;
 			   	 			//fromServer.close(); 
 			   	 			//fromServer = null;
 		   		 		} 
	 		  				catch (SocketTimeoutException e6) {
				     	     	if (debug) 
	 			   	 			writeLine("doClient"+ID+ ":timeout reading groups from manager at "+ Calendar.getInstance().getTime() + ":" + e6);
 				  		 		try {
				  		 			if (socket != null) {
						  		 		socket.close(); toServer.close(); fromServer.close();
						  		 	}
					  		 	} catch(Exception e) { }
					  		 	socket = null; toServer=null; fromServer=null;
	 		   	 		}
	 		  				catch (Exception e7) {
				     	     	if (debug) 
					  		 		writeLine("doClient"+ID+ ":Exception reading groups from manager at "+ Calendar.getInstance().getTime() + ":" + e7);
 				  		 		try {
				  		 			if (socket != null) {
						  		 		socket.close(); toServer.close(); fromServer.close();
						  		 	}
					  		 	} catch(Exception e) { }
					  		 	socket = null; toServer=null; fromServer=null;
				  		 	}
				  		 	finally {
				  		 		if (variantGroups != null && variantGroups.size() != 0) {
				  		 			terminator.reportRequest(true);	// more groups received	
				  		 		}
				  		 		else if (attempts > 3) terminator.reportRequest(false); // no more groups received	
				  		 	}

			  		 		if (variantGroups != null && variantGroups.size() != 0) {
	 				  			if (out.size() == 0 && groupMin < 100) {
					  				groupMin++; // request earlier next time
 					     	     	if (debug) 
			  							writeLine("out.size() is " + out.size() + ", groupMin is " + groupMin);
		  						}					  				
		  						else if (out.size() >= 20 && groupMin > 1) {
		  							groupMin--;	// wait a little longer before requesting more work
					     	     	if (debug) 
			  							writeLine("out.size() is " + out.size() + ", groupMin is " + groupMin);
		  						}
				  		 		attempts=0;
				   	 		writeLine("Worker: got " + variantGroups.size()+ " groups from Manager at "+Calendar.getInstance().getTime());
			  		 			Iterator i = variantGroups.iterator();
			  		 			while (i.hasNext()) {
	  				 				VariantGroup variantGroup = (VariantGroup) i.next();
	  				 				// if distributing srSeqs
 	  				 				//srSeq seq = (srSeq) i.next();
 	  				 				
			  		 				if (variantGroup == null) {
	  				 				// if distributing srSeqs
			  		 				//if (seq == null) {
	 					     	     	if (debug) 
		  				 				  	writeLine("Error: doClient: client received empty variant group!");	
										//System.exit (1); 	
			  		 				}
			  		 				else {
			 			   	 		//System.out.println("Client: deposit group"); 
										out.deposit(variantGroup); // make available to application
		  				 				// if distributing srSeqs
										//in.deposit(seq); // make available to application										
									}
								}
								
							}
							else {
								attempts++; // break; // System.exit (1); 
		  						writeLine("No variant groups returned to worker (attempts:"+attempts+") - at "+Calendar.getInstance().getTime());							
		  						try {
		  						 Thread.sleep(attempts*6000); 
		  						} catch (InterruptedException e) {}
			  	 			} 	
			  	 			if (attempts==10) {
				  	 			attempts = 0;
		  						try {
		  						 Thread.sleep(5000); 
		  						} catch (InterruptedException e) {}
			  	 				writeLine("10 attempts, sleep and try again.");
			  	 				//break;
			  	 			}
		  	 			} // terminator.sendRequest()
		  	 			else { // send RT results to server
		  	 				attempts = 0;
		  	 				try {
		  	 					RTResult result = terminator.getRTResult();
		  	 					if (modularTesting == propertyParameters.MTON) {
	 		   	 				result.setUniqueSequences(RTDriverSBRT.getUniqueSequences());
	 		   	 			}
		  	 					try {
		   						stringID = propertyReader.getInstance().getWorkerNumberProperty();
									FileOutputStream ostream = new FileOutputStream ("workerResults"+stringID+".txt");
									PrintWriter outputResults = new 	PrintWriter (ostream);
	    							outputResults.println();
					    			outputResults.println("Reachability Testing completed.");
									outputResults.println("  Executions:"+result.getNumSequences()+" / Sequences Collected:"+result.getCollected()); // +"/"+transitionCount+"/"+eventCount);
					     			outputResults.println("  Elapsed time in minutes: " + result.getRTStopWatch().elapsedMinutes());
					     			outputResults.println("  Elapsed time in seconds: " + result.getRTStopWatch().elapsedSeconds());
					     			outputResults.println("  Elapsed time in milliseconds: " + result.getRTStopWatch().elapsedMillis());
									outputResults.flush();
								} catch (Exception e) {
										System.out.println("Exception in doClient() writing results to results file" + e);
						    			System.out.println();
						    			System.out.println("Reachability Testing completed.");
										System.out.println("  Executions:"+result.getNumSequences()+" / Sequences Collected:"+result.getCollected()); // +"/"+transitionCount+"/"+eventCount);
						     			System.out.println("  Elapsed time in minutes: " + result.getRTStopWatch().elapsedMinutes());
						     			System.out.println("  Elapsed time in seconds: " + result.getRTStopWatch().elapsedSeconds());
						     			System.out.println("  Elapsed time in milliseconds: " + result.getRTStopWatch().elapsedMillis());
										System.out.flush();
								}
								int delayTime = delayResult.nextInt(60); 
								// don't want all the workers sending results at the same time
								writeLine("delay sending results by " + delayTime + " seconds");
								try {Thread.sleep(delayTime);} catch (InterruptedException e) {}
								
			   				if (socket == null) {
		 	 						socket = new Socket();
 			 						socket.bind(null);
					     	     	if (debug) 
	 				   		 		writeLine("doClient"+ID+": connect() to server");
  	 								socket.connect(new InetSocketAddress(host,serverPort),20000);
 	 								socket.setSoTimeout(20000);
 	 								socket.setKeepAlive(true);
 	 							}
 	 						
		 						if (toServer == null) {
				   	 			toServer = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				   	 			toServer.flush();
				   	 		}
	
	 		   	 			request = new clientRequest(ID,result);
   		 					writeLine("Worker "+ID+": sending RTResult at " + Calendar.getInstance().getTime());
			   	 			toServer.writeObject(request); toServer.flush();
 				     	     	if (debug) 
	     		 					writeLine("Worker "+ID+": sent RTResult at " + Calendar.getInstance().getTime());
         	            try {Thread.sleep(5000);}catch(InterruptedException e3) {}
         	            // no reset; party over
			   	 		   //toServer.reset();
			   	 		   System.exit(0);	
							}
	 		  				catch (Exception e8) {
				     	     	if (debug) 
					  		 		writeLine("doClient"+ID+ ":Exception sending RTResult to manager at "+ Calendar.getInstance().getTime() + ":" + e8);
 				  		 		try {
				  		 			if (socket != null) {
						  		 		socket.close(); toServer.close(); fromServer.close();
						  		 	}
					  		 	} catch(Exception e) { }
					  		 	socket = null; toServer=null; fromServer=null;
				  		 	}
				  		 	attempts++;
				  		 	if (attempts == 20) {
 		 		     	     	if (debug) 
					  		 		writeLine("doClient"+ID+ ": tried 20 times to send RTResult, shutting down at "+ Calendar.getInstance().getTime());
				  		 		System.exit(1);
				  		 	}
        	            else try {Thread.sleep(attempts*6000);}catch(InterruptedException e3) {}
		
		  	 			} // send RT results to server
		  	 		} // outBufferSize <=7
		  	 	}
		 		catch (Exception e) {
	     	     	if (debug) 
			  	 		writeLine("Exception in doClient inner loop " + ID + " at "
			  	 		  + Calendar.getInstance().getTime() + ":" + e);
		  	 		e.printStackTrace();
	  		 		try {
	  		 			if (socket != null) {
			  		 		socket.close(); toServer.close(); fromServer.close();
			  		 	}
		  		 	} catch(Exception e8) { }
		  		 	socket = null; toServer=null; fromServer=null;
		  	 	}
		  	 } // end while
   	 }
   	 
    	private class serverWatchdog extends Thread {
    	 // If the server runs out of variants, this thread tries to steal
    	 // groups from the clients
	    int ID = 0;
	    HashMap InetAddresses;
	    HashSet thieves;
	    HashSet victims;
	    ArrayList serversForClients;
	    ArrayList clientLocks;
       //boolean aggressive = true;
	    private serverWatchdog(HashMap InetAddresses,HashSet thieves,HashSet victims,
	    				ArrayList serversForClients, ArrayList clientLocks) {
			super("st1serverWatchdog");
	    	this.InetAddresses = InetAddresses;
	    	this.thieves = thieves;
	    	this.victims = victims;
	    	this.serversForClients = serversForClients;
	    	this.clientLocks = clientLocks;
	    }
   	 public void run() {
 			int attempts = 0;
 			int groupMin = 5; // if less than groupMin groups remain, request some more
 			// give server time to start producing variants
 			try {Thread.sleep(10000);} catch(InterruptedException e) {}
 	 		while (true) {
  	 			try {
	  	 			try {Thread.sleep(500);}catch(InterruptedException e) {}
  	 				
               //if (thieves.size() > (InetAddresses.size() / 2))
               //   aggressive = false;
               			
  	 	  	 		// if distributing srSeqs
					//if ((aggressive && getNumGroupsSent() > getNumGroupsReceived()) && in.size() < groupMax || in.size() <= 5) {
  	 				if ((aggressive && getNumGroupsSent() > getNumGroupsReceived()) && out.size() < groupMax || out.size() <= groupMin) {
		     	     	if (debug) 
	  	 					writeLine("ServerWatchdog: getting groups from client");
		  		 		ArrayList variantGroups = getGroupsFromClient(0,InetAddresses,thieves,victims,
		  		 		   serversForClients,clientLocks);
		  		 		if (variantGroups.size() != 0) {
	  			  			if (out.size() == 0 && groupMin < 100) {
				  				groupMin++; // request earlier next time
				     	     	if (debug) 
			  						writeLine("out.size() is " + out.size() + ", groupMin is " + groupMin);
		  					}
				  			//else if (out.size() >= 20 && groupMin > 1) {
				  			// out.size() >= 20 may often be true when using aggressive stealing
				  			// so don't adjust groupMin
				  			//	groupMin--;	// wait a little longer before requesting more work
				  			// if (debug) 
		  					//		writeLine("out.size() is " + out.size() + ", groupMin is " + groupMin);
		  					//}
		  		 			addNumGroupsReceived(variantGroups.size());
		  		 			if (debug)
				   	 		writeLine("Manager: stole " + variantGroups.size()+ " more groups");
							attempts=0;
		  		 			Iterator i = variantGroups.iterator();
		  		 			while (i.hasNext()) {
  				 				VariantGroup variantGroup = (VariantGroup) i.next();
  				 				// if distributing srSeqs
  				 				//srSeq seq = (srSeq) i.next();
		  		 				if (variantGroup == null) {
  				 				// if distributing srSeqs
		  		 				//if (seq == null) {
					     	     	if (debug) 
	  				 				  	writeLine("Error: server watchdog received empty variant group!");	
									//System.exit (1); 	
		  		 				}
		  		 				else {
		 			   	 		//System.out.println("ServerWatchdog: deposit group"); 
									out.deposit(variantGroup); // make available to application
	  				 				// if distributing srSeqs
									//in.deposit(seq); // make available to application
                           variantGroup = null;
  	  				 				// if distributing srSeqs
                           //seq = null;
								}
							}
						}
						else { // no groups were available 
	  						attempts++;
			     	     	if (debug) 
		  						writeLine("Manager: No variant groups returned to manager (attempts:"+attempts+") - at "+Calendar.getInstance().getTime());	
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {}
		  	 			} 
		  	 			if (attempts==10) { // try clients n times
		  	 				attempts = 0;
			     	     	if (debug) 
			  	 				writeLine("ServerWatchdog: 10 attempts; try again after sleeping at "+Calendar.getInstance().getTime());
							try {
								Thread.sleep(30000);
							} catch (InterruptedException e) {}
		  	 				//break;
		  	 			}
		  	 		}
		  	 	}
		  	 	catch (Exception e) {writeLine("Exception in ServerWatchdog:" + e);}
		  	 }
		} // end run
  	  } // end serverWatchdog
   } // end clientServerThread
   
   	private class serverThread /*extends Thread*/ implements Runnable {
   	// handles socket communication for serverThreadForClient, i.e., when the
   	// server connects with the serverThreadForClient, this thread is created
   	// to service the server's request, and any future requests
   		private boolean stopped = false;
   		private Socket socket;
   		private int ID;
   		public serverThread(Socket socket, int ID) {
   			//super("ar2serverThread"+ID);
   			this.socket=socket;this.ID=ID;
   		}
   		public void halt() {stopped = true;} // stop this thread
   		public void run() {
  			 	ObjectOutputStream toClient = null;
	 			ObjectInputStream fromClient = null;
   			try {
	     	     	if (debug) 
   					writeLine("serverThread for Client " + ID + " running.");
   			 	toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
   			 	toClient.flush();
  		 			fromClient = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
  		 			clientRequest request = null;
   				do {	
	    			 	//toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
   				 	//toClient.flush();
  			 			//fromClient = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
  		 				//clientRequest request = null;

	 	  	 			request = (clientRequest) fromClient.readObject();
 						try {
							// read Boolean so that reset() occurs
							Boolean readReset = (Boolean) fromClient.readObject(); 
							// groups were apparently read correctly
						}
				  		catch (Exception eBoolean) {
				  			writeLine("serverThread for Client " + ID + ": exception reading resetBoolean when getting request from manager");
				  		}
		     	     	if (debug) 
			   	 		writeLine("Client " + ID + "'s serverThread" + Thread.currentThread().getName()+ ": got request from server for client " + request.getID() + " at " + Calendar.getInstance().getTime());
						ArrayList clientGroups = (ArrayList)out.withdrawN(); // returns list of groups to be sent to server
		 				// if distributing srSeqs
						//ArrayList clientGroups = (ArrayList)in.withdrawN(); // returns list of groups to be sent to server
		     	     	if (debug) 
   				 		writeLine("Client " + ID + "'s serverThread: sending " + clientGroups.size() + " groups to Manager for client " + request.getID());   	 			

	 	   	 		boolean success = false;
 		   	 		try {
  	 						toClient.writeObject(clientGroups);
							toClient.flush();
 		   	 			success = true;
							try {Thread.sleep(1000);}catch(InterruptedException e3) {}
                     try {
							 	toClient.reset(); // flush the object stream cache
								toClient.writeObject(Boolean.valueOf(true));
								toClient.flush();
                     } catch (Exception e5) {
           	     	     	  if (debug) 
 		   	 			     		writeLine("serverHandler: Exception on reset() sending more groups to manager: " + e5);
                          //stopped = true;
 	   		 			     e5.printStackTrace();
 	   		 			     //try {
 	   	 					  //	socket.close(); toClient.close(); fromClient.close();
 	   	 				     //} catch (Exception e) {}
                     }
	  		 				writeLine("Worker " + ID + ": sent "+clientGroups.size()+" groups to Manager at "+Calendar.getInstance().getTime());
 	   		 		}
	 	   	 		catch (Exception e1) {
	 	   	 		   if (request != null) {
		   	     	     	if (debug) 
		 		   	 			writeLine("serverHandler: Exception sending more groups to Manager for client " + request.getID() + ":" + e1);
		 		   	 		}
	 		   	 		else { 
  	 			     	     	if (debug) 
		 		   	 			writeLine("serverHandler: Exception sending more groups to Manager:" + e1);	 		   	 		
		 		   	 	}
 	   		 			e1.printStackTrace();
 	   		 			try {
 	   	 					socket.close(); toClient.close(); fromClient.close();
 	   	 				} catch (Exception e) {}
	 	   	 		}
 		   	 		if (!success) { // groups were not successfully sent to the manager, so redeposit groups
                  	stopped = true;
     		     	     	if (debug) 
	 		   	 			writeLine("serverHandler: redeposit groups:");
							//ObjectOutputStream outputSequence = new ObjectOutputStream(new FileOutputStream("BrokenGroup-"+ID+".dat"));
							//outputSequence.writeObject(clientGroups);
 		   	 			try {
		 	   	 			if (clientGroups.size()>0) { // deposit variants back in out buffer
			 						Iterator i = clientGroups.iterator();
				  		 			while (i.hasNext()) {
 										VariantGroup variantGroup = (VariantGroup) i.next();
		  				 				// if distributing srSeqs
  										//srSeq seq = (srSeq) i.next();
 										if (variantGroup == null) {
		  				 				// if distributing srSeqs 										
				  		 				//if (seq == null) {
		 					     	     	if (debug) 
 					  							writeLine("Error: doServer: manager received empty variant group!");	
											//System.exit (1); 	
  										}
		  	 							else { 
											out.deposit(variantGroup); // make available to application
			  				 				// if distributing srSeqs
											//in.deposit(seq); // make available to application

										}
									}
			  	 				}
				     	     	if (debug) 
	         	         	writeLine("Redeposited groups.");
         	         }
         	         catch (Exception e3) {
         	     	     	if (debug) 
	         	            writeLine("serverHandler: *****Exception while redepositing groups*****" + e3);
			  		 			e3.printStackTrace();
         	         }
			   	 	} // !success 								
                  clientGroups = null;
                  //toClient.close(); 
                  //toClient=null;
                  //fromClient.close(); 
                  //fromClient=null;


		 				stopped = true; 
   				} while(!stopped);
   				// if serverThreadForClients gets request i+1 from the 
   				// server, it means the connection created by the ith 
   				// request was broken. In that case, serverThread i is
   				// interrupted and stopped before serverThread i+1 is 
   				// created/started.
	     	     	if (debug) 
   	            writeLine("serverThread stopped.");
  		 			try {
  	 					socket.close(); toClient.close(); fromClient.close();
  	 				} catch (Exception e) {}
   			} catch (Exception e2) {
	     	     	if (debug) 
	   	 			writeLine("serverHandler stopping:"+e2);
  		 			//e2.printStackTrace();
  		 			try {
  	 					if (socket != null) socket.close(); 
  	 					if (toClient != null) toClient.close(); 
  	 					if (fromClient != null) fromClient.close();
  	 				} catch (Exception e) {}   			
   			}	
   		}
   	}
   	
    	private class serverThreadForClients extends Thread {
    	// Each client has a server thread to service requests from the server. (When 
    	// the server's supply of variant groups gets low, the server tries to steal
    	// groups from the clients
	    private propertyParameters.ClientServer clientServer;
	    int ID;
	    
	    private serverThreadForClients(int ID) {
	    	super("ku3serverThreadForClient"+ID);this.ID = ID;
	    }
   	 public void run() {
   	 	ServerSocket listen;
   	 	serverThread t = null;

   	 	ExecutorService exec = Executors.newCachedThreadPool();

   	 	while (true) {
	   	 	try {
	 	  	 		listen = new ServerSocket(serverPort+ID);
		  	 		while (true) {
	  		 			// server may request groups from client when server runs out
		     	     	if (debug) 
			   	 		writeLine("serverThreadForClients: Worker " + ID + "'s server: waiting for request from manager");  		 			
	   		 		Socket socket = listen.accept();
	    				socket.setKeepAlive(true);   
		     	     	if (debug) 
			   	 		writeLine("serverThreadForClients: Worker " + ID + "'s server: got request from manager");  		 			
	   		 		
						//if (false) {
		  	 			//if (t != null) {
		  	 			// if a connection with the server is broken, the server will try to reconnect
		  	 			// with this serverThreadForClients. When it reconnects, we stop
		  	 			// the serverThread (t) that was created previously before cresting a new serverThread.
		  	 			// If no connections are broken, only one server thread will be created.
		   	 		//   writeLine("serverThreadForClients: Client " + ID + "'s server:  Halting serverThread.");
		   	 		//  t.interrupt();
		  	 			//	t.halt(); // stop the old serverThread ...
		  	 			//	t = null;
		  	 			//}
		  	 			t = new serverThread(socket,ID); // ... before creating the new one, which will service
	  	 			                                 // the new socket
     	     	     	if (debug) 
			   	 		writeLine("serverThreadForClients: Worker " + ID + "'s server: start serverThread");  		 			
		  	 			//t.start();
		   	 		exec.execute(t);
		  	 		} // end while
		  	 	} 	
		  	 	catch (IOException e) {
	  	 			e.printStackTrace();
	     	     	if (debug) 
		  	 			writeLine("serverThreadForClients: Exception " + e);
		  	 	}
				catch (OutOfMemoryError out1) {
					  out1.printStackTrace();
	 	     	     if (debug) 
					  	writeLine("OutOfMemory Exception in serverThreadForClients for client: " +ID + "." + out1);			
				}
		 	  	catch (Exception e) {
	  	 	  		e.printStackTrace();
	     	     	if (debug) 
		  	 			writeLine("serverThreadForClients: Exception " + e);
		  	 	}
		  	 }
		} // end run
  	  } // end serverThreadForClients	 
  	  
  	  class Termination {
  	  	boolean requesting = false; // true if doClient thread is requesting more groups
  	  	boolean moreSequences = false; // true if doClient received more sequences
  	  	boolean RTDriverWaiting = false; // true if RTDriver is waiting for request to finish
  	  	RTResult result;	// results of RT, to be sent to manager
  	  	boolean RTDone = false; // true when RTDriver has finished
  	  	boolean sendRequest() {
  	  	// return true if doClient should send its request
  	  		if (!RTDone) {
  	  			requesting = true;
  	  			return true;
  	  		}
	  		else { 	  		
  		  		return false;
  	  		}
  	  	}
  	  	RTResult getRTResult() {
  	  		return result; // doClient sends RT results to manager
  	  	}
  	   synchronized void  reportRequest(boolean moreSequences) {
  	  	// indicate whether any groups were received by doClient()
  	  		requesting = false;
  	  		if (RTDriverWaiting) {
  	  			this.moreSequences = moreSequences;
  	  			notify();
  	  		}
  	   }
  	   synchronized boolean doClientIsIdle(RTResult result) {
  	   // return true is doClient is idle and no more sequences to be processed
  		   this.result = result;
  		   if (requesting) {
  		   	RTDriverWaiting = true;
  		   	try {wait(300000);} catch (InterruptedException e) {}
  		   	// let doClient make several attempts to get more variants
  		   	RTDriverWaiting = false;
  		   	if (moreSequences) {
					moreSequences = false;
					return false;
  		   	}
  		   	else {
  		   		RTDone = true;
  		   		return true;
  		   	}
  		   }
  		   else {
  		   	RTDone = true;
  		   	return true;
  		  	}
  	   }
	}
} // end VariantGenerator

class clientRequest implements Serializable {
// A client requests more variant groups by sending its ID
	clientRequest(int ID) {this.ID = ID;}
	clientRequest(int ID, RTResult result) {this.ID = ID; this.result=result;}
	int getID() {return ID;}
	RTResult getRTResult() {return result;}
	private int ID;
	private RTResult result = null;
}

class socketInfo {
// Information about the socket being used by the server to communicate with a client
	Socket socket;
 	ObjectOutputStream toClient;
 	ObjectInputStream fromClient;
	public socketInfo(Socket socket,	ObjectOutputStream toClient, ObjectInputStream fromClient) {
		this.socket = socket; this.toClient = toClient; this.fromClient = fromClient;
	}
}

class RTResult implements Serializable {
  private RTStopWatch sw;
  private int numSequences;
  private int collected;
  private Vector uniqueSequences[] = null;
  RTResult(RTStopWatch sw, int numSequences, int collected) {
  	this.sw = sw; this.numSequences = numSequences; this.collected = collected;
  }
  RTStopWatch getRTStopWatch() {return sw;}
  int getNumSequences() {return numSequences;}
  int getCollected() {return collected;}
  void setUniqueSequences(Vector uniqueSequences[]) {this.uniqueSequences = uniqueSequences;}
  Vector [] getUniqueSequences() {return uniqueSequences;}

}

/* code for making in an unbounded buffer and out a bounded buffer
		// initialization
		if (propertyReader.getInstance().getBufferTypeProperty () == 
	    	propertyParameters.MEMBUFFER) {
			in = new unboundedBuffer ("In Buffer:  ");
   		((unboundedBuffer) in).init (isRestart);
	    	//writeLine("Buffer Type: MEMORY");
		}
		else if (propertyReader.getInstance().getBufferTypeProperty () == 
		 	propertyParameters.DISKQUE) {
	    	in = new DiskBuffer ("Out Buffer: ");
	    	//writeLine("Buffer Type: DISK QUE");
	    	((DiskBuffer) in).init (isRestart);
		}
		else {
	    	in = new DiskStack ("Out Buffer (stack): ");
	    	//writeLine("Buffer Type: DISK STACK");
	    	((DiskStack) in).init (isRestart);
		}
		//   	Q: DiskBuffer and DiskStack options for in?
		
		if (propertyReader.getInstance().getBufferTypeProperty () == 
	    	propertyParameters.MEMBUFFER) {
	    	//out = new unboundedBuffer ("Out Buffer: ");
	    	out = new boundedBuffer(5);
	    	//writeLine("Buffer Type: MEMORY");
	    	//((unboundedBuffer) out).init (isRestart);
		}
		else if (propertyReader.getInstance().getBufferTypeProperty () == 
		 	propertyParameters.DISKQUE) {
	    	out = new DiskBuffer ("Out Buffer: ");
	    	//writeLine("Buffer Type: DISK QUE");
	    	((DiskBuffer) out).init (isRestart);
		}
		else {
	    	out = new DiskStack ("Out Buffer (stack): ");
	    	//writeLine("Buffer Type: DISK STACK");
	    	((DiskStack) out).init (isRestart);
		}
*/

//Runtime rt2 = Runtime.getRuntime();
//writeLine("hash: Total memory allocated to VM: " + rt2.totalMemory());
//writeLine("hash:   Memory currently available: " + rt2.freeMemory());
