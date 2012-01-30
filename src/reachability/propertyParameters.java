package reachability;
interface propertyParameters {
		Mode TRACE = new Mode();
		Mode REPLAY = new Mode();
		Mode TEST = new Mode();
		Mode SPECTEST = new Mode();
		Mode RT = new Mode();
		Mode NONE = new Mode();
		Controllers SINGLE = new Controllers();
		Controllers MULTIPLE = new Controllers();
		Strategy OBJECT = new Strategy();
		Strategy THREAD = new Strategy();
		RandomDelay ON = new RandomDelay();
		RandomDelay OFF = new RandomDelay();
		TraceVariants TRACEON = new TraceVariants();
		TraceVariants TRACEOFF = new TraceVariants();
		GenerateLTS LTSON = new GenerateLTS();
		GenerateLTS LTSOFF = new GenerateLTS();
		ClientServer Client = new 	ClientServer();
		ClientServer Server = new 	ClientServer();
		ClientServer StandAlone = new ClientServer();
		SBRTMode PBRT = new SBRTMode();
		SBRTMode CycleDetection = new SBRTMode();
		DetectDeadlock DETECTIONON = new DetectDeadlock();
		DetectDeadlock DETECTIONOFF = new DetectDeadlock();
		PVReduction PVREDUCTIONON = new PVReduction();
		PVReduction PVREDUCTIONOFF = new PVReduction();
		SymmetryReduction SYMMETRYREDUCTIONON = new SymmetryReduction();
		SymmetryReduction SYMMETRYREDUCTIONOFF = new SymmetryReduction();
		CheckTrace CHECKTRACEON = new CheckTrace();
		CheckTrace CHECKTRACEOFF = new CheckTrace();
		ModularTesting MTON = new ModularTesting();
		ModularTesting MTOFF = new ModularTesting();
		StatefulRT STATEFULON = new StatefulRT();
		StatefulRT STATEFULOFF = new StatefulRT();
/* Add yours here */

    BufferType DISKQUE = new BufferType ();
    BufferType DISKSTACK = new BufferType ();
    BufferType MEMBUFFER = new BufferType ();
    DuplicateCheck CHECKON = new DuplicateCheck ();
    DuplicateCheck CHECKOFF = new DuplicateCheck ();
    DuplicateCheck RANDCHECK = new DuplicateCheck ();
    DuplicateCheck CUSTCHECK = new DuplicateCheck ();
    RunMode NORMAL = new RunMode ();
    RunMode SPLIT = new RunMode ();
    RunMode RESTART = new RunMode ();
    RunMode RANDOM = new RunMode ();

    CoreAlgorithm PRUNE = new CoreAlgorithm (); 
    CoreAlgorithm BASE = new CoreAlgorithm ();
    CoreAlgorithm FULL = new CoreAlgorithm ();
    
    InteractionCoverage INTERACTIONON = new InteractionCoverage ();
  	 InteractionCoverage INTERACTIONOFF = new InteractionCoverage ();

	 DisplayTrace DISON = new DisplayTrace ();
	 DisplayTrace DISOFF = new DisplayTrace ();

    PrintGene GENEON = new PrintGene ();
    PrintGene GENEOFF = new PrintGene (); 
    
    DataRace DATARACEON = new DataRace ();
    DataRace DATARACEOFF = new DataRace ();

		public final class Controllers { 
			private Controllers(){} 
		}

		public final class Mode { 
			private Mode(){} 
		}

		public final class Strategy { 
			private Strategy(){} 
		}
		
		public final class RandomDelay { 
			private RandomDelay(){} 
		}
		
		public final class TraceVariants { 
			private TraceVariants(){} 
		}
		
		public final class GenerateLTS { 
			private GenerateLTS(){} 
		}
		
		public final class ClientServer { 
			private ClientServer(){} 
		}
		
		public final class SBRTMode { 
			private SBRTMode(){} 
		}
		
		public final class DetectDeadlock { 
			private DetectDeadlock(){} 
		}
		
		public final class PVReduction { 
			private PVReduction(){} 
		}
		
		public final class SymmetryReduction { 
			private SymmetryReduction(){} 
		}
		
		public final class CheckTrace { 
			private CheckTrace(){} 
		}
		
		public final class ModularTesting { 
			private ModularTesting(){} 
		}

		public final class StatefulRT { 
			private StatefulRT(){} 
		}
		
/* Add yours here */
		
    public final class BufferType {
	private BufferType () {};
    }
    public final class DuplicateCheck {
	private DuplicateCheck () {}
    }
    public final class RandomDuplicateCheck {
	private RandomDuplicateCheck () {}
    }

    public final class RunMode {
	private RunMode () {}
    }

    public final class CoreAlgorithm {
	private CoreAlgorithm () {}
    }

  public final class DegreeOfInteraction {
    private DegreeOfInteraction () {}
  }

  public final class InteractionCoverage {
    private InteractionCoverage () {}
  }
  

  public final class DisplayTrace {
    private DisplayTrace () {}
  }

    public final class PrintGene {
	private PrintGene () {}
    }

    public final class DataRace {
	private DataRace () {};
    }

}
