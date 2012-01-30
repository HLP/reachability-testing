package reachability;
final class requestPermitMessage {
		private String channelName;
		private int versionNumber;
		private int calledID; // this is monitor ID during RT (so know the monitor for recollected events)
		protected requestPermitMessage(String channelName, int versionNumber) {
			this.channelName = channelName;
			this.versionNumber = versionNumber;
			this.calledID = -1;
		}
		protected requestPermitMessage(String channelName, int versionNumber, int calledID) {
			this.channelName = channelName;
			this.versionNumber = versionNumber;
			this.calledID = calledID;
		}
		public String getChannelName() {
			return channelName;
		}
		public void setChannelName(String channelName) {
			this.channelName = channelName;
		}
		public int getVersionNumber() {
			return versionNumber;
		}
		public void setVersionNumber(int versionNumber) {
			this.versionNumber = versionNumber;
		}
		public int getCalledID() {
			return calledID;
		}
		public void setCalledID(int calledID) {
			this.calledID = calledID;
		}
	}
