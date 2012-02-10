package test.bakery;

import java.util.Random;

class Bakery implements Lock {
	final int N; // number of processes using this object
	final Random ran = new Random();
	final boolean[] choosing ;
	final int[] number;
	final boolean[] inCS;

	public Bakery(int n) {
		this.N = n;
		choosing = new boolean[N];
		number = new int[N];
		inCS = new boolean[N];
		for (int i = 0; i < N; ++i) {
			choosing[i]	= false;
			number[i] = 0;
			inCS[i]	= false;
		}
	}
	
	@Override
	public void requestCS(int id) {
		// step1
		choosing[id] = true;
		for (int j = 0; j < N; ++j) {
			if (number[j] > number[id]) number[id] = number[j];
		}
		randomSleep(300);
		number[id] = number[id] + 1;
		choosing[id]= false;
		
		// step2
		for (int j = 0; j < N; ++j) {
			while (choosing[j]) {System.out.print("");}; // comment out to induce data racing
			while ((number[j] != 0) &&
					((number[j] < number[id]) ||
					((number[j] == number[id]) && j < id)))
			{
				System.out.print(""); // busy waiting
			}
		}
		
		// enter CS
		inCS[id] = true;
	}
	
	@Override
	public void releaseCS(int id) {
		for (int i = 0; i < N; ++i) {
			if (i != id && inCS[i]) {
				System.out.println("DATA RACING DETECTED!");
				System.exit(0);
			}
		}
		randomSleep(100);
		inCS[id] = false;
		number[id] = 0;
	}
	
	private void randomSleep(int time) {
		try { Thread.sleep(1 + ran.nextInt(time));
		} catch (InterruptedException e) {}
	}
}


