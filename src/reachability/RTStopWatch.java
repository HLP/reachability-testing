package reachability;
import java.util.*;
import java.io.Serializable;

public class RTStopWatch implements Serializable {
    Calendar startCal;
    Calendar endCal;
    TimeZone tz = TimeZone.getTimeZone("CST");
    long delayTime; // delay time used by RTWatchDog thread
    
    /** Creates a new instance of StopWatch */
    public RTStopWatch(long delayTime) {
    	this.delayTime = delayTime;
    }

    public RTStopWatch(long delayTime,String tzoneStr) {
     this.delayTime = delayTime;
	  tz = TimeZone.getTimeZone(tzoneStr);
    }
    
    // Start the stopwatch
    public void start() {
        startCal = Calendar.getInstance(tz);
    }
    
    // Stop the stopwatch
    public void end() {
        endCal = Calendar.getInstance(tz);
    }
    
    // adjust times by subtracting delayTime + half of delayTime
    // Measure the elapsed time in different units
    public double elapsedSeconds() {
        return (endCal.getTimeInMillis() - startCal.getTimeInMillis() - (delayTime+(delayTime/2)))/1000.0;
    }
    
    public long elapsedMillis() {
        return endCal.getTimeInMillis() - startCal.getTimeInMillis() - (delayTime+(delayTime/2));
    }
    
    public double elapsedMinutes() {
        return (endCal.getTimeInMillis() - startCal.getTimeInMillis() - (delayTime+(delayTime/2)))/(1000.0 * 60.0);
    }
    
    public static void main (String [] args) {
        RTStopWatch sw = new RTStopWatch(8000);
        sw.start();  // capture start time
        
        try {
        Thread.sleep(5000);   // sleep for 5 seconds
        }catch (Exception e) {
				System.out.println(e);
            System.exit(1);
        }
        
        sw.end();  // capture end time
        
        System.out.println("Elapsed time in minutes: " + sw.elapsedMinutes());
        System.out.println("Elapsed time in seconds: " + sw.elapsedSeconds());
        System.out.println("Elapsed time in milliseconds: " + sw.elapsedMillis());
    }
}  // end of StopWatch class
