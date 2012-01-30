package reachability;
import java.io.*;

final class simplesrThreadEvent implements Serializable {
	private int otherThread;

	public simplesrThreadEvent(int otherThread) {
		this.otherThread = otherThread;
	}

	public void setOtherThread(int otherThread) {
		this.otherThread = otherThread;
	}

	public int getOtherThread() {
		return otherThread;
	}

}
