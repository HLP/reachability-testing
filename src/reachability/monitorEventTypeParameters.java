package reachability;
import java.io.*;

	interface monitorEventTypeParameters {
		eventType ENTRY = new eventType("entry");
		eventType CALL = new eventType("call");
		eventType SYNCHSEND = new eventType("synchsend");
		eventType SYNCHRECEIVE = new eventType("synchreceive");
		eventType EXIT = new eventType("exit");
		eventType WAIT = new eventType("wait");
		eventType SIGNAL = new eventType("signal");
		eventType SIGNALANDEXIT = new eventType("signalandexit");
		eventType REENTRY = new eventType("reentry");
		eventType COMM = new eventType("comm");

		final class eventType implements Serializable {
			String name;
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
