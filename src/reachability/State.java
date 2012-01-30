package reachability;
import java.util.*;

final class State {
	private int number;
	public int getNumber() {return number;}
	private boolean terminal = true; // true unless a transition is added (that is not "terminate")
	alist a_list;
	
	private ArrayList OpenList; // list of open receive transitions
	// a state can have only one send transition. If it has a send it cannot have a receive
	private boolean hasSendTransition;	// true if this state has a send transition
	public boolean getTerminal() {return terminal;}
	public void setTerminal(boolean terminal) {this.terminal = terminal;}
	public alist getAlist(){ return a_list;}
	public ArrayList getOpenList() {return OpenList;}
	public eventInfo insert(Trans t) {
		StringBuffer label = new StringBuffer(t.getLabel());
	   if (label.charAt(0)=='\'') {
			if (!hasSendTransition) {
				if (OpenList != null && OpenList.size()>0) {
					System.out.println("Error: State with receive and send transitions.");
					System.exit(1);		
				}
				hasSendTransition = true;
			}
			else { // a state can have only one send transition
				System.out.println("Error: State with two send transitions.");
				//System.exit(1);
			}
	   }
	   else {
	   	if (t.getDestinationState()<0) {
		   	System.out.println("abort: invalid destination state " + t.getDestinationState() + " for state number " + number);
		   	System.exit(1);
		   }
	   	OpenEvent e = new OpenEvent(t.getLabel(),t.getDestinationState());
	   	if (OpenList == null)
	   	 OpenList = new ArrayList();
	   	OpenList.add(e);
	   }
	   //System.out.println("insert: openlist: "+OpenList+" label: " + label);
		return (a_list.insert(t));
	}
	public eventInfo insertMapped(Trans t, String eventType, boolean isMedium) {
	   if (!isMedium) { // this is not an LTS for a medium
	
		   if (eventType.equals("ASYNCH_SEND") || eventType.equals("SYNCH_SEND")) {
				if (!hasSendTransition) {
					if (OpenList != null && OpenList.size()>0) {
						System.out.println("Error: State with receive and send transitions.");
						System.exit(1);		
					}
					hasSendTransition = true;
				}
				else { // a state can have only one send transition
					System.out.println("Error: State with two send transitions.");
					//System.exit(1);
				}
		   }
		   else if (eventType.equals("ASYNCH_RECEIVE")) { // it's a receive transition
	   		if (t.getDestinationState()<0) {
		   		System.out.println("abort: invalid destination state " + t.getDestinationState() + " for state number " + number);
			   	System.exit(1);
			   }
			   // if receiver executes x_R, then sender executes x_S, so change labels in OpenList
			   // Assumes format of recieve is: label...R!x!y, i.e, first fields end with "R"
				//String label = t.getLabel();
				//System.out.println("t.label is " + label);
				//System.out.println("label.indexOf(!)-1 is " + (label.indexOf("!")-1));
/*relabel 1*/
				//String newLabel = label.substring(0,label.indexOf("!")-1)+"S"+label.substring(label.indexOf("!"),label.length()); // .replaceAll("_S","_R");
				//System.out.println("new label is " + newLabel);
	   		OpenEvent e = new OpenEvent(/*newLabel*/t.getLabel()/*t.getChannelName()*/,t.getDestinationState());
	   		if (OpenList == null)
		   	   OpenList = new ArrayList();
		   	OpenList.add(e);
   		   //System.out.println("insert: openlist of state " + number + " : "+OpenList+" label: " + t.getLabel() /*getChannelName()*/);
	   	}
	   }

		return (a_list.insert(t));
	}
	public State(int number) {
		this.number = number;
		this.a_list = new alist();
	}
	public String toString() {
		return("state " + number + ":\n"+a_list);
	}
}

final class eventInfo {  // captured when a transition is created/inserted
	public ArrayList agroups;
	public arec ar;
	public Trans t;
	public int ID;
	public int source;
	public int destination;
	public String label;
	public String unmappedLabel;
	public int C = -1;
	public int U = -1;
	public String channelName;
	public String eventType;
	public String toString() {
	 return("ID:" + ID + " source:" + source + " destination:" + destination
	     + " label:" + label + " C:" + C + " U:" + U + " channelName:"+channelName 
	     + " eventType:" + eventType + " unmappedLabel:" + unmappedLabel);
	}
}


final class alist {
	ArrayList agroups;
	public alist() {
		agroups = new ArrayList();
	}
	public final eventInfo insert(Trans t) {
		boolean found = false;
		eventInfo f = new eventInfo();
		f.agroups = agroups;
		ListIterator i = agroups.listIterator();
		while (i.hasNext()) {
			arec ar = (arec) i.next();
			if (ar.getLabel().equals(t.getLabel())) {
				ar.getTransitionList().add(t);
				found = true;
				f.ar = ar;
				f.t = t;
				break;
			}
		}
		if (!found) {
			arec newa = new arec(t.getLabel());
			newa.getTransitionList().add(t);
			agroups.add(newa);
			f.ar = newa;
			f.t = t;
		}	
		return f;
	}
	public final String toString() {
		Iterator i = agroups.iterator();
		StringBuffer s = new StringBuffer();
		while (i.hasNext()) {
			arec ar = (arec) i.next();
			s.append(ar+"\n");
		}
		return s.toString();
	}
}

final class arec {
	ArrayList transitionList;
	public ArrayList getTransitionList() {return transitionList;}
	private String a;
	public String getLabel() {return a;}
	public arec(String a) {
		this.a = a;
		transitionList = new ArrayList();
	}
	public final String toString() {
		StringBuffer s = new StringBuffer();
		s.append(a+":\n");
		Iterator i = transitionList.iterator();
		while (i.hasNext()) {
			Trans t = (Trans) i.next();
			s.append(/*t.getLabel() + */ "   ---> " + t.getDestinationState());
		}
		return s.toString();
	}
}
