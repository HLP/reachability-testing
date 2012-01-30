package reachability;
public class CyclicArray {
  private int[] data;
  private int size;
  private int next;
  private int capacity;

  public CyclicArray (int capacity) {
    data = new int [capacity];
    this.capacity = capacity;
    size = 0; next = 0;
  }

  public void add (int item) {
    data[next] = item;
    next = (next + 1) % capacity;
    if (size < capacity) {
      size ++;
    }
  }

  public int size () {
    return size;
  }

  public int get (int index) {
    return data[index];
  }
}
