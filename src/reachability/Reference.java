package reachability;
public final class Reference {
	public static int delay = 0;
	public static void main (String args[]) {
		delay++;
		int nodes = 20; // must be an even number
		int iterations = 12;
		asynchPort p1 = new asynchPort("p1");
		asynchPort p2 = new asynchPort("p2");
		asynchPort reply1 = new asynchPort("reply1");
		asynchPort reply2 = new asynchPort("reply2");
		Sender s1 = new Sender (1,nodes,iterations,p1,p2,reply1);
		Sender s2 = new Sender (2,nodes,iterations,p1,p2,reply2);
		Receiver r1 = new Receiver (1,nodes,iterations,p1,p2,reply1,reply2);;
		s1.start();
		s2.start(); 
		r1.start();
		try {
		    s1.join(); s2.join(); r1.join();
		}
 		catch (InterruptedException e) {}
	}
}

final class Sender extends TDThread {

	private int ID, nodes, iterations;
	private asynchPort p1, p2, reply;

	Sender (int ID, int nodes, int iterations, asynchPort p1, asynchPort p2, asynchPort reply) {
		super("Sender"+ID);
 		this.ID = ID; this.nodes = nodes; this.iterations = iterations; 
 		this.p1 = p1; this.p2 = p2; this.reply = reply;
 	}

	public void run () {
		//System.out.println ("Sender" + ID + "  Running, delay is " + Reference.delay);
		for (int i=0; i<nodes/2; i++) {
		   if ((Reference.delay>=1 && Reference.delay<=5) && ID == 2) {try {Thread.sleep(100);} catch(InterruptedException e) {}}
			p1.send(new Integer(ID));
			Integer raceResult1 = (Integer) reply.receive();
			if (raceResult1.intValue() == 2) {
				//System.out.println("sender" + ID + " 2 won");
			   if ((Reference.delay>=2 && Reference.delay<=5)  && ID == 2) {try {Thread.sleep(100);} catch(InterruptedException e) {}}
				p1.send(new Integer(ID));
				Integer raceResult1n = (Integer) reply.receive();
				if (raceResult1n.intValue() == 2) {
					//System.out.println("sender" + ID + " 2 won");
				   for (int j = 0; j < iterations; j++) {
						p1.send(new Integer(0));
			   	}
			   	return;
			   }
			   else return;// 	{System.out.println("sender" + ID + " 1 won"); return;}
			}
			//else System.out.println("sender" + ID + " 1 won");
		   if ((Reference.delay>=1 && Reference.delay<=5) && ID == 2) {try {Thread.sleep(100);} catch(InterruptedException e) {}}
			p2.send(new Integer(ID));
			Integer raceResult2 = (Integer) reply.receive();
			if (raceResult2.intValue() == 2) {
				//System.out.println("sender" + ID + " 2 won");
			   if ((Reference.delay>=2 && Reference.delay<=5) && ID == 2) {try {Thread.sleep(100);} catch(InterruptedException e) {}}
				p2.send(new Integer(ID));
				Integer raceResult2n = (Integer) reply.receive();
				if (raceResult2n.intValue() == 2) {
					//System.out.println("sender" + ID + " 2 won");
			  		for (int j = 0; j < iterations; j++) {
						p2.send(new Integer(0));
		   		}
		   		return;
		   	}
			   else return; //	{System.out.println("sender" + ID + " 1 won"); ;return;}
			}
			//else System.out.println("sender" + ID + " 1 won");
		}

	}
}

final class Receiver extends TDThread {

	private int ID, nodes, iterations;
	private asynchPort p1, p2, reply1, reply2;

	Receiver (int ID, int nodes, int iterations, asynchPort p1, asynchPort p2, asynchPort reply1, asynchPort reply2) {
		super("Receiver"+ID);
 		this.ID = ID; this.nodes = nodes; this.iterations = iterations; 
 		this.p1 = p1; this.p2 = p2; this.reply1 = reply1; this.reply2 = reply2;
 	}

	public void run () {
		//System.out.println ("Receiver" + ID + "  Running");
		for (int i = 0; i < nodes/2; i++) {
			Integer m1 = (Integer) p1.receive();
			Integer m2 = (Integer) p1.receive();
			reply1.send(m1); 
			reply2.send(m1);
			if (m1.intValue() == 2) {
			   //System.out.println("receiver: s2 won");
				Integer m1n = (Integer) p1.receive();
				Integer m2n = (Integer) p1.receive();
				reply1.send(m1n); 
				reply2.send(m1n);
				if (m1n.intValue() == 2) {
				   //System.out.println("receiver: s2 won");
				   for (int j = 0; j < iterations; j++) {
				   	Integer x = (Integer) p1.receive();
				   	Integer y = (Integer) p1.receive();			   	
				   }
			   	return;
				}
				else return;
			}
			//else System.out.println("receiver: s1 won");
			Integer m3 = (Integer) p2.receive();
			Integer m4 = (Integer) p2.receive();
			reply1.send(m3); 
			reply2.send(m3);
			if (m3.intValue() == 2 ) {
			   //System.out.println("receiver: s2 won");
				Integer m3n = (Integer) p2.receive();
				Integer m4n = (Integer) p2.receive();
				reply1.send(m3n); 
				reply2.send(m3n);
				if (m3n.intValue() == 2 ) {
				   //System.out.println("receiver: s2 won");
				   for (int j = 0; j < iterations; j++) {
				   	Integer x = (Integer) p2.receive();
				   	Integer y = (Integer) p2.receive();
				   }
			   	return;
			   }
			   else return;
			}
			//else System.out.println("receiver: s1 won");			
		}
	}
}

