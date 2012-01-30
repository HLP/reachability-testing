package reachability;
public class SelectException extends RuntimeException {
// thrown when a selectiveWait has no accept alternative with a true guard
// and no open dleay alternative and no else alternative.
	SelectException() { }
	SelectException(String msg) {super(msg);}
}
