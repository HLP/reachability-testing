package test.bakery;

import reachability.sharedBoolean;
import reachability.sharedInteger;

class SharedBakery implements Lock {
	final int N; // number of processes using this object
	sharedBoolean[] choosing;
	sharedInteger[] number;

	public SharedBakery(int n) {
		this.N = n;
		choosing = new sharedBoolean[N];
		number = new sharedInteger[N];
		for (int i = 0; i < N; ++i) {
			choosing[i]	= new sharedBoolean(false, "choosing" + i);
			number[i] = new sharedInteger(0, "number" + i);
		}
	}
	
	@Override
	public void requestCS(int id) {
		// step1
		choosing[id].Write(true);
		for (int j = 0; j < N; ++j) {
			if (number[j].Read() > number[id].Read()) {
				number[id].Write(number[j].Read());
			}
		}
		number[id].Write(number[id].Read() + 1);
		choosing[id].Write(false);
		
		// step2
		for (int j = 0; j < N; ++j) {
			while (choosing[j].Read()) {System.out.println("");} // process j in doorway
			while ((number[j].Read() != 0) &&
					((number[j].Read() < number[id].Read()) ||
					((number[j].Read() == number[id].Read()) && j < id)))
			{
				System.out.println(""); // busy waiting
			}
		}
	}
	
	@Override
	public void releaseCS(int id) {
		number[id].Write(0);
	}
}


