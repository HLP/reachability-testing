package reachability;
import java.util.*;

// two methods used during reachability testing. Implemented
// by selectiveWait but not by selectiveWaitC; the latter is
// used by the reachability testing controller code.
interface IselectiveWait {
    public ArrayList getOpenList();
    public void resetOpenList();
}
