package reachability;
import java.util.LinkedList;


public class RollingAverage {

	private int size = 10;
	private LinkedList groupList;
	private long avg = -1;
	private long total = 0;
	
	public RollingAverage( ) { groupList = new LinkedList(); }
	
	public RollingAverage(int size) { this.size = size; groupList = new LinkedList(); }
	
	public void add(Object num)
	{
		groupList.addFirst(num);  //add number to the LinkedList so that it can be subtracted during the roll

		if (num instanceof java.lang.Long) {
			total = total + ((Long)num).longValue();
		} else if (num instanceof java.lang.Integer) {
			total = total + ((Integer)num).longValue();		
		}

		if (groupList.size() == size)
		{
			avg = total / size;
			if (num instanceof java.lang.Long) {
				total = total - ((Long)(groupList.removeLast())).longValue();
			} else if (num instanceof java.lang.Integer) {
				total = total - ((Integer)(groupList.removeLast())).longValue();
			}
		}
	}	
	
	public synchronized long getAvg() { return avg; }
}