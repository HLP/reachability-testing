package reachability;
public class synchChannelC extends channelC
// *** Assumes there is only one sending thread and one receiving thread ***
// Used by receiver to send a reply on an entry call
implements propertyParameters {

   private Object message = null;
   //private final Object sending = new Object();
   //private final Object receiving = new Object();
   //private final binarySemaphore sent = new binarySemaphore(0,NONE);
   //private final binarySemaphore received = new binarySemaphore(0,NONE);
	private boolean sent = false;
	private boolean received = false;
	
   public synchChannelC() {super();}

   public final synchronized void send(Object sentMsg) {
      if (sentMsg == null) {
         throw new NullPointerException("Null message passed to send()");
      }
      message = sentMsg;
      sent = true;
      notify();
      while(!received) {
      	try {wait();} catch (InterruptedException e) {}
      }
      received = false;
      //synchronized (sending) {
      //   message = sentMsg;
      //   sent.V();
      //   received.P();
      //}
   }

   public final synchronized void send() {
      message = new Object();
      sent = true;
      notify();
      while(!received) {
      	try {wait();} catch (InterruptedException e) {}
      }
      received = false;
      //synchronized (sending) {
		//	message = new Object();
      //   sent.V();
      //   received.P();
      //}
   }

   public final synchronized Object receive() {
      Object receivedMessage = null;
      while(!sent) {
      	try {wait();} catch (InterruptedException e) {}
      }
      receivedMessage = message;
      sent = false;
      received = true;
      notify();
      //synchronized (receiving) {
      //   sent.P();
      //   receivedMessage = message;
       //  received.V();
      //}
      return receivedMessage;
   }
}

