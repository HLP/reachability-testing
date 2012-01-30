package reachability;
class Mutable
 {
     public static class Integer { 
     	public int    value; 
     	public java.lang.String toString() {
     		return java.lang.Integer.toString(value);
     	}
     	public Integer(int x) {value = x;}
     }
     
     public static class Boolean { 
     	public boolean    value; 
     	public String toString() {
     		if (value)
     			return "true";
     		else
     			return "false";
     	}
     	public Boolean(boolean x) {value = x;}
     }
 }
