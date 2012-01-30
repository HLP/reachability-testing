package reachability;
final class msgReceivedMessage {
		private int caller;
		private int callerVersionNumber;
		protected msgReceivedMessage(int caller, int callerVersionNumber) {
			this.caller = caller;
			this.callerVersionNumber = callerVersionNumber;
		}
		public int getCaller() {
			return caller;
		}
		public void setcaller(int caller) {
			this.caller = caller;
		}
		public int getCallerVersionNumber() {
			return callerVersionNumber;
		}
		public void setCallerVersionNumber(int callerVersionNumber) {
			this.callerVersionNumber = callerVersionNumber;
		}
	}
