package reachability;
// The definition of entry assumes that there can be many clients but only one server

class selectableEntryC extends selectableSynchChannelC implements selectable {
 	private callMsg cm; 
 	private String channelName;
 	public Object call(Object request) throws InterruptedException {
		synchChannelC replyChannel = new synchChannelC();
		send(new callMsg(request,replyChannel));
		return replyChannel.receive();
	}
	selectableEntryC(String channelName) {
		super(channelName);
		this.channelName = channelName;
	}
	selectableEntryC() {this.channelName = "null";}
 	public Object call() throws InterruptedException {
		synchChannelC replyChannel = new synchChannelC();
		send(new callMsg(replyChannel));
		return replyChannel.receive();
	}
	
	public Object accept() throws InterruptedException { cm = (callMsg) receive(); return cm.request;}

	public void reply(Object response) throws InterruptedException { cm.replyChannel.send(response); }

	public void reply() throws InterruptedException { cm.replyChannel.send(); }

	public Object acceptAndReply() throws InterruptedException { 
		cm = (callMsg) receive(); cm.replyChannel.send(); return cm.request;
	}

	private class callMsg {
		Object  request;
		synchChannelC replyChannel;
		callMsg(Object m, synchChannelC c) {request=m; replyChannel=c;}
		callMsg(synchChannelC c) {request = new Object(); replyChannel=c;}
  }

	public String getChannelName() {return channelName;}

}

