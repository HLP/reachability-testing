package reachability;
import java.util.ArrayList;

/**
 * This class provides some utility methods.
 */
public class Util {
	/**
	 * This method prints out an error message in standard error and
	 * then exits.
	 * <p>
	 * @param errMsg the error message to be printed out.
	 */
	public static void abort (String errMsg) {
		System.err.println(errMsg);
		System.exit(1);
	}

	// a simplied method that determines the maximum prime number that
	// is smaller than or equal to a given value
	public static int getMaxPrime (int value) {
		int rval = 0;
		int[] primes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31};
		for (int i = primes.length - 1; i > 0; i --) {
			if (value >= primes[i]) {
				rval = primes[i];
				break;
			}
		}
		return rval;
	}

	public static boolean isPrime (int value) {
		boolean rval = false;
		int[] primes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41,
				43, 47, 51};
		for (int i = 0; i < primes.length; i ++) {
			if (value == primes[i]) {
				rval = true;
				break;
			}
		}
		return rval;
	}

	public static void dump (ArrayList matrix) {
		for (int i = 0; i < matrix.size(); i ++) {
			int[] row = (int[]) matrix.get(i);
			for (int j = 0; j < row.length; j ++) {
				System.out.print(row[j] + " ");
			}
			System.out.println();
		}
	}

	public static void dump (int[] test) {
		for (int j = 0; j < test.length; j ++) {
			System.out.print(test[j] + " ");
		}
		System.out.println();
	}
}

