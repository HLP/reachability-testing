package reachability;
import java.io.*;
	interface eventTypeParameters {
		eventType SR_SYNCHRONIZATION = new eventType("sr_synchronization");
		eventType ASYNCH_SEND = new eventType("asynch_send");
		eventType ASYNCH_RECEIVE = new eventType("asynch_receive");
		eventType START_ENTRY = new eventType("start_entry");
		eventType FINISH_ENTRY = new eventType("finish_entry");
		eventType ELSE_DELAY = new eventType("else_delay");
		eventType SEND_EXCEPTION = new eventType("send_exception");
		eventType RECEIVE_EXCEPTION = new eventType("receive_exception");
		eventType UNACCEPTED_SYNCH_SEND = new eventType("unaccepted_synch_send");
		eventType UNACCEPTED_ASYNCH_SEND = new eventType("unaccepted_asynch_send");
		eventType UNACCEPTED_RECEIVE = new eventType("unaccepted_receive");
		eventType THREAD_BASED_SEND = new eventType("send"); //for thread-based send events
		eventType THREAD_BASED_RECEIVE = new eventType("receive"); //for thread-based send events
      eventType MONITOR_CALL = new eventType("monitor_call");
      eventType MONITOR_ENTRY = new eventType("monitor_entry");
      eventType SEMAPHORE_CALL = new eventType("semaphore_call");
      eventType SEMAPHORE_COMPLETION = new eventType("semaphore_completion");
      eventType LOCK_CALL = new eventType("lock_call");
      eventType LOCK_COMPLETION = new eventType("lock_completion");
      eventType SYNCH_SEND = new eventType("synch_send");
      eventType SYNCH_RECEIVE = new eventType("synch_receive");
      eventType SHAREDVARIABLE_CALL = new eventType("sharedvariable_call");
      eventType SHAREDVARIABLE_COMPLETION = new eventType("sharedvariable_completion");
		
		final class eventType implements Serializable {
			private String name;
   		private eventType(String name) {
				this.name = name;
			}
			public String toString() {return name;}
			public boolean equals(Object e) {
				eventType that = (eventType)e;
				return (this.name).equals(that.name);
			}
		}
		
	}
