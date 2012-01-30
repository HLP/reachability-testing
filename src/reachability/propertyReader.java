package reachability;
class propertyReader implements propertyParameters {
// java -Dmode=trace -startID=1 -DprocessName=process1
	static final int defaultMaxThreads = 20;
	private static final String ModePropertyName = "mode";
	private static final String ControllersPropertyName = "controllers";
	private static final String StrategyPropertyName = "strategy";
	private static final String StartIDPropertyName = "startID";
	//private static final String ProcessNamePropertyName = "processName";
	private static final String WorkerNumberPropertyName = "workerNumber";
	private static final String ControllerPortPropertyName = "controllerPort";
	private static final String AckPortPropertyName = "ackPort";
	private static final String RandomDelayPropertyName = "randomDelay";
	private static final String TraceVariantsPropertyName = "traceVariants";
	private static final String GenerateLTSPropertyName = "generateLTS";
	private static final String ClientServerPropertyName = "clientServer";
	private static final String ManagerWorkerPropertyName = "managerWorker";
	private static final String ServerIPPropertyName = "serverIP";
	private static final String ServerPortPropertyName = "serverPort";
	private static final String SBRTModePropertyName = "sbrtMode";
	private static final String DetectDeadlockPropertyName = "deadlockDetection";
	private static final String PVReductionPropertyName = "PVReduction";
	private static final String SymmetryReductionPropertyName = "symmetryReduction";
	private static final String MaxThreadsPropertyName = "maxThreads";
	private static final String CheckTracePropertyName = "checkTrace";
	private static final String ModularTestingPropertyName = "modularTesting";
	private static final String ModularTestingThreadsPropertyName = "modularTestingThreads";
	private static final String HashPropertyName = "hash";
	private static final String StatefulRTPropertyName = "statefulRT";

/* Add yours here */

    private static final String BufferTypePropertyName = "bufferType";
    private static final String DuplicateCheckPropertyName = "duplicateCheck";
    private static final String LimitOfVariantsPropertyName = "limitOfVariants";
    private static final String RunModePropertyName = "runMode";
    private static final String numOfHostsPropertyName = "numOfHosts";
    private static final String numWorkersPropertyName = "numWorkers";
    private static final String CoreAlgorithmPropertyName = "coreAlgorithm";
    private static final String DisplayTracePropertyName = "displayTrace";
    private static final String InteractionCoveragePropertyName = "tWay";
    private static final String PrintGenePropertyName = "printGene";
    private static final String DataRacePropertyName = "detectDataRace";

   
	private propertyParameters.Mode mode;
	private propertyParameters.Controllers controllers;
	private propertyParameters.Strategy strategy;
	private int startID;
	//private String processName;
	private String workerNumber;
	private int controllerPort;
	private int ackPort;
	private propertyParameters.RandomDelay randomDelay;
	private propertyParameters.TraceVariants traceVariants;
	private propertyParameters.GenerateLTS generateLTS;
	private propertyParameters.ClientServer clientServer;
	private propertyParameters.SBRTMode sbrtMode;
	private propertyParameters.DetectDeadlock detectDeadlock;
	private propertyParameters.PVReduction PVReduce;
	private propertyParameters.SymmetryReduction SymmetryReduce;
   private int maxThreads;
	private propertyParameters.CheckTrace checkTrace;
	private String serverIP;
	private int serverPort;
	private propertyParameters.ModularTesting modularTesting;
	private int modularTestingThreads;
	private int hash;
	private propertyParameters.StatefulRT statefulRT;
/* Add yours here */

    private propertyParameters.BufferType bufferType;
    private propertyParameters.DuplicateCheck duplicateCheck;
    private propertyParameters.RunMode runMode;
    private int limitOfVariants;
    private int numOfHosts;
    private propertyParameters.CoreAlgorithm coreAlgorithm;
    private propertyParameters.DisplayTrace displayTrace;
    private propertyParameters.InteractionCoverage interactionCoverage;
    private propertyParameters.PrintGene printGene;
    private propertyParameters.DataRace dataRace;

	private static final Object classLock = propertyReader.class;

	private static propertyReader instance = null;

	private propertyReader() {
		mode = determineModeProperty();
		controllers = determineControllersProperty();
		strategy = determineStrategyProperty();
		startID = determineStartIDProperty();
		//processName = determineProcessNameProperty(); // catches "-DprocessName=..."
		workerNumber = determineWorkerNumberProperty(); // catches "-DworkerNumber=..."
		controllerPort = determineControllerPortProperty();
		ackPort = determineAckPortProperty();
		randomDelay = determineRandomDelayProperty();
	   traceVariants = determineTraceVariantsProperty();
	   generateLTS = determineGenerateLTSProperty();
  	   clientServer = determineClientServerProperty();	 // catches "-DclientServer=..."
  	   clientServer = determineManagerWorkerProperty(); // catches "-DmanagerWorker=..."
  	   serverIP = determineServerIPProperty();
	   serverPort = determineServerPortProperty();
	   sbrtMode = determineSBRTModeProperty();
	   detectDeadlock = determineDeadlockDetectionProperty();
	   PVReduce = determinePVReductionProperty();
	   SymmetryReduce = determineSymmetryReductionProperty();
	   maxThreads = determineMaxThreadsProperty();
	   checkTrace = determineCheckTraceProperty();
	   modularTesting = determineModularTestingProperty();
	   modularTestingThreads = determineModularTestingThreadsProperty();
	   hash = determineHashProperty();
	   statefulRT = determineStatefulRTProperty();


/* Add yours here */

		bufferType = determineBufferTypeProperty ();
		duplicateCheck = determineDuplicateCheckProperty ();
		limitOfVariants = determineLimitOfVariantsProperty ();
		runMode = determineRunModeProperty ();
		numOfHosts = determineNumOfHostsProperty (); // catches "-DnumOfHosts=..."
		numOfHosts = determineNumWorkersProperty (); // catches "-DnumWorkers=..."
		coreAlgorithm = determineCoreAlgorithmProperty ();
		displayTrace = determineDisplayTraceProperty ();
  		interactionCoverage = determineInteractionCoverage ();
		printGene = determinePrintGeneProperty ();
		dataRace = determineDataRaceProperty ();
	}


	public static propertyReader getInstance() { 
		if (instance == null) {
			synchronized(classLock) {
      		if (instance == null)
        			instance = new propertyReader();
			}
		}
      return instance;
    }

/* Add your "determineXProperty() {...} here */

	private propertyParameters.Mode determineModeProperty() {
   	String property = System.getProperty(ModePropertyName);
		if (property == null)
	  		return NONE;
		else if (property.equals("trace"))
	  		return TRACE;
		else if (property.equals("replay"))
	  		return REPLAY;
		else if (property.equals("test"))
	  		return TEST;
		else if (property.equals("spectest") || property.equals("specTest") ||
		         property.equals("commTest") || property.equals("commtest"))
	  		return SPECTEST;
		else if (property.equals("rt"))
	  		return RT;
		else if (property.equals("none"))
			return NONE;
		else {
			System.out.println();
			System.out.println("Error: Invalid value for mode property: " + property);
			System.out.println("Valid values are: trace, replay, test, spectest, and rt.");
			System.exit(1);
			return NONE;
	  	}
	}  

	private propertyParameters.Controllers determineControllersProperty() {
   	String property = System.getProperty(ControllersPropertyName);
		if (property == null)
	  		return SINGLE;
		else if (property.equals("single"))
	  		return SINGLE;
		else if (property.equals("multiple"))
	 		 return MULTIPLE;
		else {
			System.out.println();
			System.out.println("Error: Invalid value for controllers property: " + property);
			System.out.println("Valid values are: single and multiple.");
			System.exit(1);
	  		return SINGLE;
		}
	}  

	private propertyParameters.Strategy determineStrategyProperty() {
   	String property = System.getProperty(StrategyPropertyName);
		if (property == null)
	  		return OBJECT;
		else if (property.equals("object"))
	  		return OBJECT;
		else if (property.equals("thread"))
	 		 return THREAD;
		else {
			System.out.println();
			System.out.println("Error: Invalid value for strategy property: " + property);
			System.out.println("Valid values are: object and thread.");
			System.exit(1);
	  		return THREAD;
		}

	}  
	
	private int determineStartIDProperty() {
   	String property = System.getProperty(StartIDPropertyName);
		if (property != null) {
			try {
				int i = Integer.parseInt(property);
				if (i<1) throw new NumberFormatException();
				return i;

			}
			catch (NumberFormatException e) {
				System.out.println("startID property must be an integer > 0.");
				System.exit(1);
			}
		}
	  	return 1; // default start
	} 
	
	private int determineControllerPortProperty() {
   	String property = System.getProperty(ControllerPortPropertyName);
		if (property != null) {
			try {
				int i = Integer.parseInt(property);
				return i;
			}
			catch (NumberFormatException e) {
				System.out.println("controllerPort property must be an integer.");
				System.exit(1);
			}
		}
	  	return 0;
	}  
	
	private int determineAckPortProperty() {
   	String property = System.getProperty(AckPortPropertyName);
		if (property != null) {
			try {
				int i = Integer.parseInt(property);
				return i;
			}
			catch (NumberFormatException e) {
				System.out.println("ackPort property must be an integer.");
				System.exit(1);
			}
		}
	  	return 0;
	}  
	
	//private String determineProcessNameProperty() {
   //	String property = System.getProperty(ProcessNamePropertyName);
	// 	return property;
	//}
	
	private String determineWorkerNumberProperty() {
   	String property = System.getProperty(WorkerNumberPropertyName);
	  	return property;
	}
	
	private propertyParameters.RandomDelay determineRandomDelayProperty() {
   	String property = System.getProperty(RandomDelayPropertyName);
		if (property == null)
	  		return OFF;
		else if (property.equals("on"))
	  		return ON;
		else if (property.equals("off"))
	 		 return OFF;
		else {
			System.out.println();
			System.out.println("Error: Invalid value for randomDelay property: " + property);
			System.out.println("Valid values are: on and off.");
			System.exit(1);
	  		return OFF;
		}
	} 
	
	private propertyParameters.TraceVariants determineTraceVariantsProperty() {
  		String property = System.getProperty(TraceVariantsPropertyName);
		if (property == null)
  			return TRACEOFF;
		else if (property.equals("on"))
  			return TRACEON;
		else if (property.equals("off"))
 		 	return TRACEOFF;
		else {
			System.out.println();
			System.out.println("Error: Invalid value for traceVariants property: " + property);
			System.out.println("Valid values are: on and off.");
			System.exit(1);
  			return TRACEOFF;
		}
	} 
	
	private propertyParameters.GenerateLTS determineGenerateLTSProperty() {
  		String property = System.getProperty(GenerateLTSPropertyName);
		if (property == null)
  			return LTSOFF;
		else if (property.equals("on"))
  			return LTSON;
		else if (property.equals("off"))
 		 	return LTSOFF;
		else {
			System.out.println();
			System.out.println("Error: Invalid value for generateLTS property: " + property);
			System.out.println("Valid values are: on and off.");
			System.exit(1);
  			return LTSOFF;
		}
	}
	
	private propertyParameters.ClientServer determineClientServerProperty() {
  		String property = System.getProperty(ClientServerPropertyName);
		if (property == null)
  			return StandAlone;
		else if (property.equals("client"))
  			return Client;
		else if (property.equals("server"))
 		 	return Server;
		else
  			return StandAlone;
	}
	
	private propertyParameters.ClientServer determineManagerWorkerProperty() {
  		String property = System.getProperty(ManagerWorkerPropertyName);
		if (property == null)
  			return clientServer;  // determineClientServerProperty() already called, use it
		else if (property.equals("worker"))
  			return Client;
		else if (property.equals("manager"))
 		 	return Server;
		else
  			return StandAlone;
	}
	
	private String determineServerIPProperty() {
   	String property = System.getProperty(ServerIPPropertyName);
	  	return property;
	}
	
	private int determineServerPortProperty() {
   	String property = System.getProperty(ServerPortPropertyName);
		if (property != null) {
			try {
				int i = Integer.parseInt(property);
				if (i<1025) throw new NumberFormatException();
				return i;

			}
			catch (NumberFormatException e) {
				System.out.println("server port property must be an integer > 1024.");
				System.exit(1);
			}
		}
	  	return 2020; // default start
	} 
	
		private propertyParameters.DetectDeadlock determineDeadlockDetectionProperty() {
	  		String property = System.getProperty(DetectDeadlockPropertyName);
			if (property == null)
  				return DETECTIONOFF;
			else if (property.equals("on"))
  				return DETECTIONON;
			else if (property.equals("off"))
	 		 	return DETECTIONOFF;
			else {
				System.out.println();
				System.out.println("Error: Invalid value for deadlockDetection property: " + property);
				System.out.println("Valid values are: on and off.");
				System.exit(1);
	  			return DETECTIONOFF;
			} 
		}
	
		private propertyParameters.PVReduction determinePVReductionProperty() {
  		String property = System.getProperty(PVReductionPropertyName);
		if (property == null)
  			return PVREDUCTIONOFF;
		else if (property.equals("on"))
  			return PVREDUCTIONON;
		else if (property.equals("off"))
 		 	return PVREDUCTIONOFF;
		else {
				System.out.println();
				System.out.println("Error: Invalid value for PVReduction property: " + property);
				System.out.println("Valid values are: on and off.");
				System.exit(1);
	  			return PVREDUCTIONOFF;
		}
	}
	
	private propertyParameters.SymmetryReduction determineSymmetryReductionProperty() {
		String property = System.getProperty(SymmetryReductionPropertyName);
		if (property == null)
  			return SYMMETRYREDUCTIONOFF;
		else if (property.equals("on"))
  			return SYMMETRYREDUCTIONON;
		else if (property.equals("off"))
 		 	return SYMMETRYREDUCTIONOFF;
		else {
				System.out.println();
				System.out.println("Error: Invalid value for symmetryReduction property: " + property);
				System.out.println("Valid values are: on and off.");
				System.exit(1);
	  			return SYMMETRYREDUCTIONOFF;
		}
	}
	
	private int determineMaxThreadsProperty() {
   	String property = System.getProperty(MaxThreadsPropertyName);
		if (property != null) {
			try {
				int i = Integer.parseInt(property);
				if (i < 2) i = 2;
				return i;
			}
			catch (NumberFormatException e) {
				System.out.println("maxThreads must be an integer.");
				System.exit(1);
			}
		}
	  	return defaultMaxThreads;
	} 
	
	private propertyParameters.CheckTrace determineCheckTraceProperty() {
		String property = System.getProperty(CheckTracePropertyName);
		if (property == null)
  			return CHECKTRACEOFF;
		else if (property.equals("on"))
  			return CHECKTRACEON;
		else if (property.equals("off"))
 		 	return CHECKTRACEOFF;
		else {
				System.out.println();
				System.out.println("Error: Invalid value for checkTrace property: " + property);
				System.out.println("Valid values are: on and off.");
				System.exit(1);
	  			return CHECKTRACEOFF;
		}
	}

    private propertyParameters.BufferType determineBufferTypeProperty () {
		String property = System.getProperty(BufferTypePropertyName);
		if (property == null) {
		    return MEMBUFFER;
		}
		else if (property.equals("queue")) {
		    return DISKQUE;
		}
		else if (property.equals("stack")) {
		    return DISKSTACK;
		}
		else if (property.equals("memory")) {
		    return MEMBUFFER;
		}
		else {
				System.out.println();
				System.out.println("Error: Invalid value for bufferType property: " + property);
				System.out.println("Valid values are: queue, stack, and memory.");
				System.exit(1);
				return DISKSTACK;
		}
    }

    private propertyParameters.DuplicateCheck determineDuplicateCheckProperty () {
		String property = System.getProperty (DuplicateCheckPropertyName);
		if (property == null) {
		    return CHECKOFF;
		}
		else if (property.equals("on")) {
		    return CHECKON;
		}
		else if (property.equals("off")) {
		    return CHECKOFF;
		}
		else if (property.equals("random")) {
		    return RANDCHECK;
		}
		else if (property.equals("custom")) {
		    return CUSTCHECK;
		}
		else {
				System.out.println();
				System.out.println("Error: Invalid value for duplicateCheck property: " + property);
				System.out.println("Valid values are: one, off, random, and custom.");
				System.exit(1);
				return CHECKOFF;
		}
    }
	
    private propertyParameters.RunMode determineRunModeProperty () {
		String property = System.getProperty (RunModePropertyName);
		if (property == null) {
		    return NORMAL;
		}
		else if (property.equals("restart")) {
		    return RESTART;
		}
		else if (property.equals("split")) {
		    return SPLIT;
		}
		else if (property.equals("random")) {
		    return RANDOM;
		}
		else {
				System.out.println();
				System.out.println("Error: Invalid value for runMode property: " + property);
				System.out.println("Valid values are: restart, split, and random.");
				System.exit(1);
				return NORMAL;
		}
    }

    private propertyParameters.CoreAlgorithm determineCoreAlgorithmProperty () {
		String property = System.getProperty (CoreAlgorithmPropertyName);
		if (property == null) {
		    return FULL;
		}
		else if (property.equals("prune")) {
		    return PRUNE;
		}
		else if (property.equals("base")) {
		    return BASE;
		}
		else if (property.equals("full")) {
		    return FULL;
		}
		else {
				System.out.println();
				System.out.println("Error: Invalid value for coreAlgorithm property: " + property);
				System.out.println("Valid values are: prune, base, and full.");
				System.exit(1);
				return FULL;
		}
    }
	
    private int determineLimitOfVariantsProperty() {
   	String property = System.getProperty(LimitOfVariantsPropertyName);
		if (property != null) {
		   try {
				int i = Integer.parseInt(property);
				return i;
		   }
			catch (NumberFormatException e) {
				System.out.println("limitOfVariants property must be an integer.");
				System.exit(1);
			}
		}
		return 0;
    }  

    private int determineNumOfHostsProperty() {
		String property = System.getProperty(numOfHostsPropertyName);
		if (property != null) {
	   	try {
				int i = Integer.parseInt(property);
				return i;
	    	}
	    	catch (NumberFormatException e) {
				System.out.println("numOfHosts property must be an integer.");
				System.exit(1);
	    	}
		}
		return 0;
    } 
    
    private int determineNumWorkersProperty() {
		String property = System.getProperty(numWorkersPropertyName);
		if (property != null) {
	   	try {
				int i = Integer.parseInt(property);
				return i;
	    	}
	    	catch (NumberFormatException e) {
				System.out.println("numWorkers property must be an integer.");
				System.exit(1);
	    	}
		}
		return 0;
    } 
    
	private propertyParameters.SBRTMode determineSBRTModeProperty() {
   	String property = System.getProperty(SBRTModePropertyName);
		if (property == null)
	  		return PBRT;
		else if (property.equals("pbrt"))
	  		return PBRT;
		else if (property.equals("cycleDetection"))
	  		return CycleDetection;
		else {
	  		System.out.println("Error: Invalid value for sbrtMode property: " + property);
			System.out.println("Valid values are: pbrt and cycleDetection.");
	  		System.exit(1);
		  	return PBRT;
	  	}
	}  

	private propertyParameters.DisplayTrace determineDisplayTraceProperty() {
   	String property = System.getProperty(DisplayTracePropertyName);
		if (property == null)
	  		return DISOFF;
		else if (property.equals("on"))
	  		return DISON;
		else if (property.equals("off"))
	 		 return DISOFF;
		else {
	  		System.out.println("Error: Invalid value for displayTrace property: " + property);
			System.out.println("Valid values are: on and off.");
	  		System.exit(1);
		  	return DISOFF;
	  	}
	}
	
	private propertyParameters.PrintGene determinePrintGeneProperty() {
   	String property = System.getProperty(PrintGenePropertyName);
		if (property == null)
	  		return GENEOFF;
		else if (property.equals("on"))
	  		return GENEON;
		else if (property.equals("off"))
	 		 return GENEOFF;
		else {
	  		System.out.println("Error: Invalid value for printGene property: " + property);
			System.out.println("Valid values are: on and off.");
	  		System.exit(1);
		  	return GENEOFF;
	  	}
	}
	
	private propertyParameters.DataRace determineDataRaceProperty() {
   	String property = System.getProperty(DataRacePropertyName);
		if (property == null)
	  		return DATARACEOFF;
		else if (property.equals("on"))
	  		return DATARACEON;
		else if (property.equals("off"))
	 		 return DATARACEOFF;
		else {
	  		System.out.println("Error: Invalid value for detectDataRace property: " + property);
			System.out.println("Valid values are: on and off.");
	  		System.exit(1);
		  	return DATARACEON;
	  	}
	}
	
         private propertyParameters.InteractionCoverage determineInteractionCoverage () {
			String property = System.getProperty (InteractionCoveragePropertyName);
			if (property == null) {
				return INTERACTIONOFF;
      	}
      	else if (property.equals("on")) {
				return INTERACTIONON;
      	}
      	else if (property.equals("off")) {
			return INTERACTIONOFF;
      	}
      	else {
				System.out.println();
				System.out.println("Error: Invalid value for InteractionCoverage property: " + property);
				System.out.println("Valid values are: on and off.");
				System.exit(1);
      	}
     		return INTERACTIONOFF;
    }
 
	private propertyParameters.ModularTesting determineModularTestingProperty() {
   	String property = System.getProperty(ModularTestingPropertyName);
		if (property == null)
	  		return MTOFF;
		else if (property.equals("on"))
	  		return MTON;
		else if (property.equals("off"))
	 		 return MTOFF;
		else {
	  		System.out.println("Error: Invalid value for modular testing property: " + property);
			System.out.println("Valid values are: on and off.");
	  		System.exit(1);
		  	return MTOFF;
	  	}
	}
	
    private int determineModularTestingThreadsProperty() {
		String property = System.getProperty(ModularTestingThreadsPropertyName);
		if (property != null) {
	   	try {
				int i = Integer.parseInt(property);
				return i;
	    	}
	    	catch (NumberFormatException e) {
				System.out.println("modularTestingThreads property must be an integer.");
				System.exit(1);
	    	}
		}
		return 0;
    } 

    private int determineHashProperty() {
		String property = System.getProperty(HashPropertyName);
		if (property != null) {
	   	try {
				int i = Integer.parseInt(property);
				if (i>0)
					return i;
	    	}
	    	catch (NumberFormatException e) {
				System.out.println("hash property must be an integer.");
				System.exit(1);
	    	}
		}
		return 25;
    } 
    
	private propertyParameters.StatefulRT determineStatefulRTProperty() {
   	String property = System.getProperty(StatefulRTPropertyName);
		if (property == null)
	  		return STATEFULOFF;
		else if (property.equals("on"))
	  		return STATEFULON;
		else if (property.equals("off"))
	 		 return STATEFULOFF;
		else {
	  		System.out.println("Error: Invalid value for the statefulRT property: " + property);
			System.out.println("Valid values are: on and off.");
	  		System.exit(1);
		  	return STATEFULOFF;
	  	}
	}
	
    /* Add yours here */
	public synchronized propertyParameters.Mode getModeProperty() {return mode;}
	public synchronized propertyParameters.Controllers getControllersProperty() {return controllers;}
	public synchronized propertyParameters.Strategy getStrategyProperty() {return strategy;}
	public synchronized int getStartIDProperty() {return startID;}
	//public synchronized String getProcessNameProperty() {return processName;}
	public synchronized String getWorkerNumberProperty() {return workerNumber;}
	public synchronized int getControllerPortProperty() {return controllerPort;}
	public synchronized int getAckPortProperty() {return ackPort;}
	public synchronized propertyParameters.RandomDelay getRandomDelayProperty() {return randomDelay;}
	public synchronized propertyParameters.TraceVariants getTraceVariantsProperty() {return traceVariants;}
	public synchronized propertyParameters.GenerateLTS getGenerateLTSProperty() {return generateLTS;}
	public synchronized propertyParameters.ClientServer getClientServerProperty() {return clientServer;}
	public synchronized propertyParameters.ClientServer getManagerWorkerProperty() {return clientServer;}
	public synchronized String getServerIPProperty() {return serverIP;}
	public synchronized int getServerPortProperty() {return serverPort;}
	public synchronized propertyParameters.SBRTMode getSBRTModeProperty() {return sbrtMode;}
	public synchronized propertyParameters.DetectDeadlock getDetectDeadlockProperty() {return detectDeadlock;}
	public synchronized propertyParameters.PVReduction getPVReductionProperty() {return PVReduce;}
	public synchronized propertyParameters.SymmetryReduction getSymmetryReductionProperty() {return SymmetryReduce;}
	public synchronized int getMaxThreadsProperty() {return maxThreads;}
	public synchronized propertyParameters.CheckTrace getCheckTraceProperty() {return checkTrace;}
	public synchronized propertyParameters.ModularTesting getModularTestingProperty() {return modularTesting;}
	public synchronized int getModularTestingThreadsProperty() {return modularTestingThreads;}
	public synchronized int getHashProperty() {return hash;}
	public synchronized propertyParameters.StatefulRT getStatefulRTProperty() {return statefulRT;}


    public synchronized propertyParameters.BufferType getBufferTypeProperty() {return bufferType;}
    public synchronized propertyParameters.DuplicateCheck getDuplicateCheckProperty() {return duplicateCheck;}
    public synchronized int getLimitOfVariantsProperty () {return limitOfVariants; }
    public synchronized propertyParameters.RunMode getRunModeProperty () {return runMode;}
    public synchronized int getNumOfHostsProperty () {return numOfHosts; }
    public synchronized int getNumWorkersProperty () {return numOfHosts; }
    public synchronized propertyParameters.CoreAlgorithm
    getCoreAlgorithmProperty () {return coreAlgorithm; }
  	 public synchronized propertyParameters.DisplayTrace
    getDisplayTraceProperty () {return displayTrace; }
    public synchronized propertyParameters.InteractionCoverage
    getInteractionCoverageProperty () {return interactionCoverage; }
    public synchronized propertyParameters.PrintGene
    getPrintGeneProperty () {return printGene; }
    public synchronized propertyParameters.DataRace
	 getDataRaceProperty () { return dataRace; }
 }
