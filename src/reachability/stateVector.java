package reachability;
final class stateVector implements Cloneable {
  private int[] v = null;
  stateVector(int size) {
 		v = new int[size];
		for (int i=0; i<size; i++) {
			v[i]=0;
		} 
  }
  public void reset() {
 		for (int i=0; i<v.length; i++) {
			v[i]=0;
		}  
  }
  public void setV(int[] v) {this.v = v;}
  public int[] getV() {return v;}
  public void setLocal(int i, int state) { v[i] = state;}
  public int getLocal(int i, int state) { return v[i];}
  public boolean equals(Object o) {
  		if (o instanceof stateVector) {
			stateVector s = (stateVector) o;
			if (s.v.length != v.length) {
				System.out.println("Internal Error: state vectors of unequal length");
				System.exit(1);
				return false;
			}
			for (int i=0; i<v.length; i++)
				if (s.v[i] != v[i]) 
					return false;
			return true;
  		}
  		else {
  			System.out.println("Internal error: Comparing state vector and non state vector objects");
  			System.exit(1);
  			return false;
  		}
  }
  public Object clone() {
  		try {
  			stateVector s = (stateVector) super.clone();
  			s.v = (int[]) v.clone();
  			return s;
  		}
  		catch (CloneNotSupportedException e) {
  			// can't happen
  			throw new InternalError(e.toString());
  		}
  }
  
  public int hashCode() {
  	int code=0;
  	int ten = 1;
	if (v[0]>0) {
		System.out.println("Internal Error: local state of LTS 0 is not 0");  
		System.exit(1);
	}
  	for (int i=v.length-1; i>0; i--) {
  	// v[0] is always 0
  		code = code + (v[i]*ten);
  		ten = ten*10;
  	}
  	return code;
  }
  public String toString() {
  
		StringBuffer vString = new StringBuffer();
	   boolean first = true;
	   vString.append("[");
	   if (vString!=null) {
	   	for (int i=0; i<v.length; i++) {
	   	  if (!first) {
	   	    vString.append(",");
	   	  }
	   	  else 
  	   	    first = false;
	   	  vString.append(v[i]);
	   	}
	   }
	   vString.append("]");
	   return vString.toString();
	}
}
  
