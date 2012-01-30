package reachability;
final class traceMessage implements traceMessageTypeParameters {

		private traceMessageTypeParameters.traceMessageType mType;
		private Object msg;
		protected traceMessage(traceMessageTypeParameters.traceMessageType mType, Object msg) {
			this.mType = mType;
			this.msg = msg;
		}
		public void setMsg(Object msg) {
			this.msg = msg;
		}
		public void setMType(traceMessageTypeParameters.traceMessageType mType) {
			this.mType = mType;
		}
		public Object getMsg() {
			return msg;
		}
		public traceMessageTypeParameters.traceMessageType getMType() {
			return mType;
		}
}
