package reachability;
import java.util.*;
import java.io.*;

public final class Graph {
	ArrayList states;
	static HashMap eventsToThreads;
	static HashMap eventsToTransitions;
	static IntegerSpace Ispace;
	static HashMap labelSpace;
	HashSet events;
	int stateCount, transitionCount;
	int startState, currentState = 0;
	int numStates, numTransitions;
	int ID;
	boolean isMedium = false;
	int transitionNum;
	String inputFileName;
	boolean terminating = false; // true if graph has a terminal state
	boolean mappedSequence = true;
	boolean visited[]; // used during DFS
	int pathCount = 0; // used to count paths in generatePaths();
	public Graph() {
		states = new ArrayList();
		events = new HashSet();					// Q; need this if have labelSpace?
		if (eventsToThreads == null)
			eventsToThreads = new HashMap();
		if (eventsToTransitions == null)
			eventsToTransitions = new HashMap();
		if (Ispace == null)
			Ispace = new IntegerSpace(20);
		if (labelSpace == null)
			labelSpace = new HashMap();
		stateCount = 0; transitionCount = 0;
		startState = 0; transitionNum=0;
		ID = -1;
	}
	public final void setCurrentState(int currentState) {this.currentState=currentState;}
	public final int getCurrentState() {return currentState;}
	public final ArrayList getCurrentStatesOpenList() {return ((State)states.get(currentState)).getOpenList();}
	public final boolean isTerminated() {return ((State)states.get(currentState)).getTerminal();}
	public final boolean isTerminating() {return terminating;}
	public final void setTerminating() {terminating = true;}	
	public final boolean isMedium() {return isMedium;}
	
	public final void input() {
		try {
			//FileInputStream fstream = new FileInputStream(inputFileName);
			//DataInputStream in = new DataInputStream(fstream);
			FileReader fstream = new FileReader(inputFileName);
			BufferedReader in = new BufferedReader(fstream);
			String header = in.readLine();
			StringTokenizer t = new StringTokenizer(header," (,)");
			String des = t.nextToken();
			String startStateStr = t.nextToken();
			String transitionCountStr = t.nextToken();
			String stateCountStr = t.nextToken();
			startState = Integer.parseInt(startStateStr);
			currentState = startState;
			transitionCount = Integer.parseInt(transitionCountStr);
			stateCount = Integer.parseInt(stateCountStr);
			//System.out.println("startState:"+startState+
			//	" transitionCount:"+transitionCount+" stateCount:"+stateCount);
			for (int i=0; i<stateCount; i++) 
				states.add(new State(i));
	      for (int i=0; i<transitionCount; i++) {
	      	int C = -1; int U = -1; String channelName = null; String eventType = null;
	      	String line = in.readLine();
	      	StringTokenizer t2 = new StringTokenizer(line,"(,)");
	      	String sourceStr = t2.nextToken().trim();
	      	int source = Integer.parseInt(sourceStr);
	      	String label = t2.nextToken();
	      	String unmappedLabel = null;
	      	//System.out.println(label);
	      	if (mappedSequence) {
		      	String[] result = label.split(" ");
		      	StringBuffer actualLabel = new StringBuffer();
		      	for (int x=0; x<(result.length)-4; x++) { 
		      	  actualLabel.append(result[x]);
	  	      	}
   	         unmappedLabel = actualLabel.toString(); // trim off starting double quote
   	         if (unmappedLabel.charAt(0) == '\"') unmappedLabel = unmappedLabel.substring(1);
					C = Integer.parseInt((result[result.length-4]).substring(1)); 
					U = Integer.parseInt((result[result.length-3]).substring(1)); 
					channelName = result[result.length-2].substring(1); 
					eventType = result[result.length-1].substring(1); 
					if (eventType.charAt(eventType.length()-1) == '\"') eventType = eventType.substring(0,eventType.length()-1);
					//System.out.println(C+" "+U+" "+channelName+" " + eventType);
					label = unmappedLabel + "!"+C+"!"+U+"!"+channelName; // +"!"+eventType;
					//System.out.println(label);
				} 
				else label = label.trim();
	  	      String destinationStr = t2.nextToken().trim();
	      	int destination = Integer.parseInt(destinationStr);    	
	      	transitionNum++;
  				//System.out.println("source:"+source+" destination:"+destination+" label:"+label);
	     		if (mappedSequence) {
		      	eventInfo f = addTransition(source,destination,label,C,U,channelName,eventType,unmappedLabel);
	   	   	f.ID = ID;
  		      	addEventToThread(label,f);
      		}
      		else {
	         	eventInfo f = addTransition(source,destination,label);
	   	   	f.ID = ID;		
  		      	addEventToThread(label,f);
      		}
	      	events.add(label);
	      	//Q put(I);put(I) keeps same I?
			}
			for (int i=0; i<stateCount; i++) {
			// determine if this LTS can terminate or not; terminating LTSs are
			// expected to terminate
				if (((State)states.get(i)).getTerminal() == true) {
					terminating = true;
					break;
				}
			}
		}
		catch (Exception e) {e.printStackTrace();}
	}
	private final eventInfo addTransition(int source, int destination, String label) {
		Trans t = new Trans(destination,transitionNum,label);
		State s = (State) states.get(source);
		eventInfo f = s.insert(t);
		f.source = source; f.destination = destination; f.label = label;
/* Q: Don't set terminal to false if label is "terminate" */
		s.setTerminal(false);
		return f;
	}
	private final eventInfo addTransition(int source, int destination, String label,
	                                      int C, int U, String channelName, String eventType,
	                                      String unmappedLabel) {
		Trans t = new Trans(destination,transitionNum,label,C,U,channelName,eventType,unmappedLabel);
		State s = (State) states.get(source);
		eventInfo f = s.insertMapped(t,eventType,isMedium);
		f.source = source; f.destination = destination; f.label = label;
		f.C = C; f.U = U; f.channelName = channelName; f.eventType = eventType;
		f.unmappedLabel = unmappedLabel;
/* Q: Don't set terminal to false if label is "terminate" */
		s.setTerminal(false);
		return f;
	}
	public final ArrayList getOpenList() {
		return ((State)states.get(currentState)).getOpenList();
	}
	public final ArrayList getEnabledEvents() {
	  // return arecs of currentState
		return (((State)states.get(currentState)).getAlist().agroups);
	}
	private final void addEventToThread(String label, eventInfo f) {
		if (!eventsToThreads.containsKey(label)) {
			eventsToThreads.put(label,new LinkedList());
		}
		LinkedList threads = (LinkedList)eventsToThreads.get(label);
		//System.out.println("ID is " + ID);
		threads.addFirst(f);
		
		if (!eventsToTransitions.containsKey(label))
			eventsToTransitions.put(label, new HashMap());
		HashMap threadMap = (HashMap) eventsToTransitions.get(label);
		Integer f_ID = new Integer(f.ID);
		if (!threadMap.containsKey(f_ID))
			threadMap.put(f_ID,new HashMap(stateCount));
		HashMap stateMap = (HashMap) threadMap.get(f_ID);
		Integer f_source = new Integer(f.source);
		if (stateMap.containsKey(f_source)) {
			System.out.println("Internal Error: state has 2 transitions for the same event");
			System.exit(1);
		}
		stateMap.put(f_source,f);	
	}
	public final void output() {
		System.out.println("***LTS "+ID+" ("+inputFileName+") ***");
		Iterator i = states.iterator();
		while (i.hasNext()) {
			System.out.println(i.next());
		}
	}
	public final void generatePaths() {
	// Assumes that the graph contains no cycles. Prints paths from the
	// startState to each terminal state, i.e., a state with no 
	// transitions. Also counts the paths.
		LinkedList path = new LinkedList();
		visited = new boolean[stateCount];
		genPathsDFS(startState,path);
		
	}
	private final void genPathsDFS(int stateNum, LinkedList path) {
	// prints the paths using depth-first-search
		if (((State)states.get(stateNum)).getTerminal()) {
			System.out.println();
			pathCount++;
			System.out.print(pathCount+": ");
			Iterator p = path.iterator();
			while (p.hasNext()) {
				Trans t = (Trans) p.next();
				System.out.print(t.getLabel()+".");
			}
			return;
		}
		Iterator arecItr = ((State)states.get(stateNum)).a_list.agroups.iterator();
		while (arecItr.hasNext()) {
			Iterator transitionItr = ((arec)arecItr.next()).transitionList.iterator();
			while (transitionItr.hasNext()) {
				Trans t = (Trans) transitionItr.next();
				if (!visited[t.getDestinationState()]) {
					path.addLast(t);
					genPathsDFS(t.getDestinationState(),path);
					path.removeLast();
				}
				else {
					System.out.println("Error: cycle in graph");
					System.exit(1);
				}
			}
			
		}
	}
}

