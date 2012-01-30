package reachability;
import java.util.*;
import java.io.*;

class transition {
	public transition(String label, int sourceState, int destinationState) {
		this.label = label;
		this.sourceState = sourceState;
		this.destinationState = destinationState;
	}
	public String label;
	public int sourceState;
	public int destinationState;
	public String toString() {
		return ("("+sourceState+", "+label+", "+destinationState+")");
	}
	public boolean equals ( Object other ) { 
	// assume: if source state and label are same then destination state will be the same too
	//         since we assume LTS is deterministic
		if ( other == null ) { return false; } 
		else if (other instanceof transition ) { 
			return	this.sourceState == ((transition)other).sourceState &&
					//can't compare sine initially destination is -1 and this is changed later,
					// so in run 2, destination state will be -1 but in run 1 was changed to n.
					//this.destinationState == ((transition)other).destinationState &&
					(this.label).equals(((transition)other).label);
		} 
		else return false; 
	} 

}

class stateNumber {
	public stateNumber(int value) {this.value = value;}
	public int value;
}

class LTSGraph {
	public String ID;	// name, e.g., distributedProcess0 or requestPort0
	public boolean mediumOrPort = false; // true if medium or port
	public int numberOfStates=0;
	public int numberOfTransitions=0;
	public int numberOfDestinationStates=0;
	private HashMap stateTable = new HashMap(); // stores states for this LTS
	private TreeMap transitionTable = new TreeMap();
	private transition previousTransition = null;
	public LTSGraph(String ID) {this.ID = ID;}
	public HashMap getStateTable() {return stateTable;}
	public TreeMap getTransitionTable() {return transitionTable;}
	public transition getPreviousTransition() {return previousTransition;}
	public void setPreviousTransition(transition previous) {previousTransition = previous;}
}
 
public class LTSGenerator {

   private static final int IN_CAPACITY = 500;
   private boundedBuffer in;
	private static final Object classLock = LTSGenerator.class;
	private static LTSGenerator instance = null;
	private HashMap graphTable = new HashMap();

	public LTSGenerator () {
		in = new boundedBuffer (IN_CAPACITY);
		ltsGeneratorThread t = new ltsGeneratorThread();
		t.setDaemon(true);
		t.start ();
	}

	public static LTSGenerator getInstance() { 
		if (instance == null) {
			synchronized(classLock) {
      		if (instance == null)
        			instance = new LTSGenerator();
			}
		}
      return instance;
    }

	public void reset() {
	   // called by driver; let's remaining transitions get processed before restart
		programTransition pt = new programTransition("RESET","RESET","RESET");
		depositTransition(pt);
	}
	
    
	public void depositTransition (programTransition transition) {
		in.deposit(transition);
	}
	
   public programTransition getTransition () {
		programTransition t = (programTransition) in.withdraw();
		while ((t.ID).equals("RESET")) {
			Iterator p = graphTable.entrySet().iterator();
			while (p.hasNext()) {
				Map.Entry e = (Map.Entry)p.next();	
				LTSGraph G = (LTSGraph)e.getValue();
				G.setPreviousTransition(null);	
			}
			t = (programTransition) in.withdraw();
		}
		return t;
	}
	
	public void generateLTSs() {
			Iterator p1 = graphTable.entrySet().iterator();
			while (p1.hasNext()) {
				try {
					Map.Entry e1 = (Map.Entry)p1.next();	
					String ID = (String)e1.getKey();
					PrintWriter outputLTS = null; 
					outputLTS = new PrintWriter(new FileOutputStream(ID+".aut"));
					LTSGraph G = (LTSGraph)e1.getValue();
					if (!G.mediumOrPort) G.numberOfTransitions++; // add "terminate" transition
					outputLTS.println("des (0, "+G.numberOfTransitions+", "+(G.numberOfStates+1)+")");
					TreeMap transitionTable = G.getTransitionTable();
					Iterator p2 = transitionTable.entrySet().iterator();
					while (p2.hasNext()) {
						Map.Entry e2 = (Map.Entry)p2.next();	
						//Integer sourceState = (Integer)e2.getKey();
						LinkedList L = (LinkedList)e2.getValue();
						Iterator p3 = L.iterator();
						while (p3.hasNext()) {
							transition t = (transition) p3.next();
							if (t.destinationState == -1)
							// if no destination state it's -1, meaning it was last transition to be executed
								t.destinationState = G.numberOfStates;
							outputLTS.println(t);
						}
					}
					if (!G.mediumOrPort) {
						transition t = new transition("terminate",G.numberOfStates,G.numberOfStates);
						outputLTS.println(t);
					}
					outputLTS.close();
				}
				catch (IOException e) {
        			System.err.println("File not opened: " + e.toString());
        			System.exit(1);
      		}
				catch (Exception e) {
       			System.err.println("Error: " + e.toString());
        			System.exit(1);
      		}
			}
	}

	private class ltsGeneratorThread extends Thread {
		ltsGeneratorThread() {super("ltsGeneratorThread");}
		public void run () {
	   	for (;;) {
				programTransition t = (programTransition) getTransition (); 

					// put state in stateTable with Integer representing next State
					if (!(graphTable.containsKey(t.ID))) { // put sender in message map
						LTSGraph G = new LTSGraph(t.ID);
						if (t.destinationState.equals("M") || t.destinationState.equals("P")) 
							G.mediumOrPort = true;
						else G.mediumOrPort = false;
						graphTable.put(t.ID, G);
					}
					LTSGraph G = (LTSGraph) graphTable.get(t.ID);
					if (t.destinationState.equals("M")) {
						addMediumTransition(G.ID,t.state,t.destinationState,t.label,G);
					}
					else if (t.destinationState.equals("P")) {
						addPortTransition(G.ID,t.state,t.destinationState,t.label,G);
					}
					else {
						HashMap s = G.getStateTable();
						int stateNumber = -1;
						if (s.containsKey(t.state)) {
							stateNumber = ((Integer) s.get(t.state)).intValue();
						}
						//if (!(s.containsKey(t.state))) { // put sender in message map
						else {
							stateNumber = ((G.numberOfStates)++);
							s.put(t.state, new Integer(stateNumber));
						}
						//System.out.println(G.ID+":"+stateNumber+","+t.label);
						//System.out.println(t.state);
						addTransition(G.ID,stateNumber,t.label, G);
						// add stateNumber --t.label-->-1 to G.lts, which is map: [stateNumber,LinkedList of t's]
					}
						
				}
		}
		
		private void addMediumTransition(String ID, String source, String destination, String label, LTSGraph G) {
			Integer sourceState = new Integer(0);
			Integer destinationState = new Integer(++(G.numberOfDestinationStates));
			TreeMap transitionTable = G.getTransitionTable();
			
			LinkedList L = (LinkedList) transitionTable.get(sourceState);
			if (L==null) {
				L = new LinkedList();
				transitionTable.put(sourceState,L);
			}
			transition t = new transition(label+"[S]",0,destinationState.intValue());
			if (!L.contains(t)) {
				L.addLast(t);
				G.numberOfTransitions++;
				t = new transition("'"+label+"[A]",destinationState.intValue(),0);
				L.addLast(t);
				G.numberOfTransitions++;
				G.numberOfStates++; // e.g., add 0->1 and 1->0 so added a state
			}

		}

		private void addPortTransition(String ID, String source, String destination, String label, LTSGraph G) {
			Integer sourceState = new Integer(0);
			Integer destinationState = new Integer(++(G.numberOfDestinationStates));
			TreeMap transitionTable = G.getTransitionTable();
			
			LinkedList L = (LinkedList) transitionTable.get(sourceState);
			if (L==null) {
				L = new LinkedList();
				transitionTable.put(sourceState,L);
			}
			transition t = new transition(label+"[A]",0,destinationState.intValue());

			if (!L.contains(t)) {
				L.addLast(t);
				G.numberOfTransitions++;
				t = new transition("'"+label+"[R]",destinationState.intValue(),0);
				L.addLast(t);
				G.numberOfTransitions++;
				G.numberOfStates++; // e.g., add 0->1 and 1->0 so added a state	
			}

		}
		
		private void addTransition(String ID, int stateNumber, String label, LTSGraph G) {
			Integer sourceState = new Integer(stateNumber);
			TreeMap transitionTable = G.getTransitionTable();
			
			LinkedList L = (LinkedList) transitionTable.get(sourceState);
			if (L==null) {
				L = new LinkedList();

				transitionTable.put(sourceState,L);
			}
			transition t = new transition(label,sourceState.intValue(),-1);

			if (!L.contains(t)) {
				L.addLast(t);
				G.numberOfTransitions++;
				transition previousTransition = G.getPreviousTransition();
				if (previousTransition == null)
					G.setPreviousTransition(t);
				else {
					previousTransition.destinationState = sourceState.intValue();
					G.setPreviousTransition(t);
				}			
			}
			else {
				transition previousTransition = G.getPreviousTransition();
				if (previousTransition != null) {
					previousTransition.destinationState = sourceState.intValue();
					G.setPreviousTransition(null);
				}							
			}
		}
	}
}
