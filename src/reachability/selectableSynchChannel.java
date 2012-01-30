package reachability;
import java.util.*;

public class selectableSynchChannel extends channel implements selectable{

   private Object message = null;
   private final Object sending = new Object();
   private final Object receiving = new Object();
   private final binarySemaphore sent = new binarySemaphore(0);
   private final binarySemaphore received = new binarySemaphore(0);

	private boolean open = false;
	private int ready = 0;
	private IselectiveWait inchoice = null;
	private boolean guard_ = true;
	private static long timeStamp = 0;
	private Vector arrivals = new Vector();
	
	private static synchronized long getTimeStamp() {
		return(++timeStamp);
	}

   public selectableSynchChannel() {super();}

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
			arrivals.removeElementAt(0);
         received.V();
      }
      return receivedMessage;
   }

	public synchronized void setSelect(IselectiveWait s) {
		inchoice = s;
	}

	synchronized void signal() {
		if (inchoice == null) {
			++ready; 
			arrivals.addElement(new Long(getTimeStamp()));
			sent.V(); 
		} 
		else {
			synchronized (inchoice) {
				++ready;
				arrivals.addElement(new Long(getTimeStamp()));
				sent.V();	
				if (open)    // is this is an entry that choice is waiting for?
					inchoice.notify(); 
			}
		}
	}

	public long getOldestArrival() {
	// only called from choose() when ready>0, i.e., arrivals.size()>0
		return (((Long)arrivals.firstElement()).longValue());
	}

	public final int count() { return ready; }

    public boolean testReady() {
        return ready>0;
    }

    public synchronized void clearReady() {
        --ready;
    }

    public void setOpen() {
        open=true;
    }

    public void clearOpen() {
         open=false;
    }

    public void guard(boolean g) {
        guard_=g;
    }

    public boolean testGuard(){
        return guard_;
    }
    
   // used by selectableSynchChannelC
  	public void setID(int ID) {}
	public int getID() {return -1;}
}

