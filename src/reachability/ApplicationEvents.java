package reachability;
import java.util.*;

class ApplicationEvents implements propertyParameters {
	private static propertyParameters.Mode mode = (propertyReader.getInstance().getModeProperty());;  // user chooses trace or replay or none
	private static propertyParameters.GenerateLTS generateLTS = (propertyReader.getInstance().getGenerateLTSProperty());
	private static propertyParameters.CheckTrace checkTrace = (propertyReader.getInstance().getCheckTraceProperty());
	private static ArrayList appEvents = new ArrayList();
	public static ArrayList getAppEvents() {return appEvents;}
	public static void resetAppEvents() {appEvents.clear();}

	public static void exerciseEvent(String label) {
		if ((checkTrace==CHECKTRACEON) && (mode != NONE)) {
			innerThread inner = (innerThread) Thread.currentThread();
			inner.updateIntegerTS();
			AppEvent e = new AppEvent(inner.getID(),label,null,inner.getVectorTS());
			synchronized(appEvents) {
				appEvents.add(e);
			}
	   }
		//if ((generateLTS==LTSON) && (mode != NONE)) {
	      //String state = getState();
      	//programTransition t = new programTransition(getThreadName(),PC+":"+state,label);
	      //LTSGenerator.getInstance().depositTransition(t);	
	   //}
	}
	public static void exerciseEvent(Object obj) {
		if ((checkTrace==CHECKTRACEON) && (mode != NONE)) {
			innerThread inner = (innerThread) Thread.currentThread();
			inner.updateIntegerTS();
			AppEvent e = new AppEvent(inner.getID(),obj.toString(),obj,inner.getVectorTS());
			synchronized(appEvents) {
				appEvents.add(e);
			}
	   }
		//if ((generateLTS==LTSON) && (mode != RT)) {
	      //String state = getState();
      	//programTransition t = new programTransition(getThreadName(),PC+":"+state,label);
	      //LTSGenerator.getInstance().depositTransition(t);	
	   //}
	}
	public static void exerciseTransitionEvent(String label) {
	// generate a user transition in an LTS 
		if ((checkTrace==CHECKTRACEON) && (mode != NONE)) {
			innerThread inner = (innerThread) Thread.currentThread();
			inner.updateIntegerTS();
			AppEvent e = new AppEvent(inner.getID(),label,null,inner.getVectorTS());
			synchronized(appEvents) {
				appEvents.add(e);
			}
	   }
		if ((generateLTS==LTSON) && (mode != NONE)) {

		   String state = ((innerThread)Thread.currentThread()).getThreadState();
		      
			StringBuffer B = new StringBuffer();
			Throwable ex = new Throwable();
			StackTraceElement[] stackElements = ex.getStackTrace();
			for (int i=stackElements.length-1; i>=0; i--)
				B.append(stackElements[i]);
			String PC = B.toString();
				
		   programTransition t = new programTransition(((innerThread)Thread.currentThread()).getName(),PC+":"+state,label);
   	   LTSGenerator.getInstance().depositTransition(t);
		}
	}
	public static void exerciseEvent(String label, int ID, vectorTimeStamp VTStamp) {
	// called by RTDriverSBRT
		//if ((checkTrace==CHECKTRACEON) && (mode != NONE)) {

			AppEvent e = new AppEvent(ID,label,null,VTStamp);
			synchronized(appEvents) {
				appEvents.add(e);
			}
	   //}
		//if ((generateLTS==LTSON) && (mode != NONE)) {
	      //String state = getState();
      	//programTransition t = new programTransition(getThreadName(),PC+":"+state,label);
	      //LTSGenerator.getInstance().depositTransition(t);	
	   //}
	}
}
