package reachability;
interface selectable {
	public void setSelect(IselectiveWait s);
	public void clearOpen();
	public boolean testGuard();
	public void setOpen();
	public boolean testReady();
	public long getOldestArrival();
	public int count();
	public String getChannelName();
	public void setID(int ID);
	public int getID();
}
