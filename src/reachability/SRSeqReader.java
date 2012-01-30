package reachability;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;

import java.io.IOException;
import java.io.FileNotFoundException;

public class SRSeqReader {
    // constants in the data file
    private static final String PREFIX_INDEX = "PREFIX_INDEX:";
    
    private static final int NUM_OF_FIELDS = 7; 

    private static final int SND = 0;
    private static final int RCV = 1;

    // the name of the data file
    private String fn;

    public SRSeqReader (String fn) {
	this.fn = fn;
    }

    public srSeq read () {
	srSeq rval = new srSeq ();
	try {
	    // prefix index
	    int [] prefix_index;
	    FileReader freader = new FileReader (fn);
	    BufferedReader breader = new BufferedReader (freader);

	    String line;
	    while ((line = breader.readLine()) != null) {
		if (line.startsWith (PREFIX_INDEX)) {
		    String data = line.substring (PREFIX_INDEX.length());
		    StringTokenizer tokens = new StringTokenizer (data, ",[] ");
		    prefix_index = new int [tokens.countTokens()];
		    int i = 0;
		    while (tokens.hasMoreTokens ()) {
			prefix_index [i] = Integer.parseInt (tokens.nextToken ());
			i ++;
		    }
		    // set prefix index
		    rval.setPrefixIndex (prefix_index);
		}
		else {
		    StringTokenizer tokens = new StringTokenizer (line, ",() ");
		    if (tokens.countTokens () != NUM_OF_FIELDS) {
			System.out.println("INCORRECT data file format!");
			System.exit (1);
		    }
		    else {
			int caller = Integer.parseInt (tokens.nextToken ());
			int called = Integer.parseInt (tokens.nextToken ());
			int callerVersionNumber = Integer.parseInt (tokens.nextToken ());
			int calledVersionNumber = Integer.parseInt (tokens.nextToken ());
			String channel_name = tokens.nextToken ();

			// FOR NOW, let us ignore how to maintain channel version number
			int channelVersionNumber = Integer.parseInt(tokens.nextToken ());
			
			String type_name = tokens.nextToken ();
			eventTypeParameters.eventType event_type = getEventType(type_name);
			
			srEvent event = new srEvent (caller, called, callerVersionNumber, 
						     calledVersionNumber, channel_name, channelVersionNumber, 
						     event_type, null);
			
			rval.addEvent (event);
		    }
		}
	    }
	}
	catch (FileNotFoundException ex) {
	    ex.printStackTrace ();
	    System.exit (1);
	}
	catch (IOException ex) {
	    ex.printStackTrace ();
	    System.exit (1);
	}

	return rval;
    }

    private eventTypeParameters.eventType getEventType (String type_name) { 
	eventTypeParameters.eventType rval = null;
	if (type_name.equals("asynch_receive")) {
	    rval = eventTypeParameters.ASYNCH_RECEIVE;
	}
	else if (type_name.equals("asynch_send")) {
	    rval = eventTypeParameters.ASYNCH_SEND;
	}
	else if (type_name.equals("unaccepted_receive")) {
	    rval = eventTypeParameters.UNACCEPTED_RECEIVE;
	}
	else if (type_name.equals("unaccepted_asynch_send")) {
	    rval = eventTypeParameters.UNACCEPTED_ASYNCH_SEND;
	}
	return rval;
    }
}
