package reachability;
import java.util.*;

public class selectableSynchChannelC extends channelC 
implements selectable,  propertyParameters{

   private Object message = null;
   private final Object sending = new Object();
   private final Object receiving = new Object();
   private final binarySemaphore sent = new binarySemaphore(0,NONE);
   private final binarySemaphore received = new binarySemaphore(0,NONE);
   private int ID;

	private boolean open = false;
	private int ready = 0;
	private IselectiveWait inchoice = null;
	private boolean guard_ = true;
	private static long timeStamp = 0;
	private Vector arrivals = new Vector();
	private String channelName;
	private static synchronized long getTimeStamp() {
		return(++timeStamp);
	}

   public selectableSynchChannelC() {super();this.channelName="null";}
	public selectableSynchChannelC(String channelName) {
		super();
		this.channelName=channelName;
	}
   public final void send(Object sentMsg) {
      if (sentMsg == null) {
         throw new NullPointerException("Null message passed to send()");
      }
      synchronized (sending) {
         message = sentMsg;
			signal();
         received.P();
      }
   }

   public final void send() {
      synchronized (sending) {
			signal();
         received.P();
      }
   }

   public final Object receive() {
      Object receivedMessage = null;
      synchronized (receiving) {
         sent.P();
			clearReady();
         receivedMessage = message;
			removeArrival();
         received.V();
      }
      return receivedMessage;
   }

	public final synchronized void setSelect(IselectiveWait s) {
		inchoice = s;
	}
	public final synchronized void setID(int ID) {this.ID = ID;}
	public final synchronized int getID() {return ID;}
	
	public final synchronized IselectiveWait getAndCheckSelect() {
	   if (inchoice == null)
	   	incReady(); // so ready will be set when controller checks
		return inchoice;
	}

	public final /*synchronized*/ void signal() {
		IselectiveWait inchoice = getAndCheckSelect();
		if (inchoice == null) {
			//if (channelName.equals("traceMsg"))
			//	traceit.getInstance().trace(((innerThread)Thread.currentThread()).getID()+" inchoice is null");
			//incReady();
			sent.V(); // if (open) notifyAll();
		} 
		else {
			incReady();
			synchronized (inchoice) {
				//if (channelName.equals("traceMsg"))
				//	traceit.getInstance().trace(((innerThread)Thread.currentThread()).getID()+" did incReady, incReady = "+ready);
				if (getOpen()) {  // is this is an entry that choice is waiting for?
					inchoice.notify(); 
					//if (channelName.equals("traceMsg"))
					//	traceit.getInstance().trace(((innerThread)Thread.currentThread()).getID()+" did notify");
				}
				//else 
				//	if (channelName.equals("traceMsg"))
				//		traceit.getInstance().trace(((innerThread)Thread.currentThread()).getID()+" no notify, open was false");
				sent.V();
			}
		}
	}

	 public synchronized long getOldestArrival() {
	 // only called from choose() when ready>0, i.e., arrivals.size()>0
		return (((Long)arrivals.firstElement()).longValue());
	 }

	 public final synchronized int count() { return ready; }

    public final synchronized boolean testReady() {
        return ready>0;
    }

    public final synchronized void clearReady() {
        --ready;
    }
    
    public final synchronized void incReady() {
        ++ready;
		  arrivals.addElement(new Long(getTimeStamp()));
    }
    
    public final synchronized void removeArrival() {
    		arrivals.removeElementAt(0);
    }

    public final synchronized void setOpen() {
        open=true;
    }
    
    public final synchronized boolean getOpen() {
        return open;
    }

    public final synchronized void clearOpen() {
         open=false;
    }

    public final synchronized void guard(boolean g) {
        guard_=g;
    }

    public final synchronized boolean testGuard(){
        return guard_;
    }
    
  	 public synchronized String getChannelName() { 
  	 	if (channelName != null)
	  	 	return channelName;
	  	else
	  		return "null";
	  }
}

