package reachability;
import java.util.ArrayList;

public class ServerSplitter {
    private int numOfHosts;
    private static ServerSplitter splitter;
    private int numOfGroups;
    private int clientIndex = 1;

    
    public static ServerSplitter instance (int numOfHosts) {
		if (splitter == null) {
	   	 splitter = new ServerSplitter (numOfHosts);
		}
		return splitter;
    }

    private ServerSplitter (int numOfHosts) {
		this.numOfHosts = numOfHosts;
		numOfGroups = 1;
    }

    public void write (ArrayList[] groups, VariantGroup group) {
		try {
			groups[clientIndex].add(group);
			System.out.println("give group to client " + clientIndex);
	   	clientIndex++;
           /* change due to node 21 being down */
           //if (clientIndex == 21) clientIndex++;
           /* change due to dual node and node 21 being down (clients 41 and 42 in dual mode) */
           //if (clientIndex == 41) {clientIndex++; clientIndex++;} // skip to 43
  	   	if (clientIndex>numOfHosts) clientIndex=1;
		}
		catch (Exception e) {e.printStackTrace();System.exit (1);}
    }
}
