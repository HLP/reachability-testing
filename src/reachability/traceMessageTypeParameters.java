package reachability;
	interface traceMessageTypeParameters {
		traceMessageType SR_LINK_EVENT_TYPE = new traceMessageType();
		traceMessageType SR_PORT_EVENT_TYPE = new traceMessageType();
		traceMessageType SR_MAILBOX_EVENT_TYPE = new traceMessageType();
		traceMessageType SR_ENTRY_EVENT_TYPE = new traceMessageType();
		traceMessageType END_ENTRY_TYPE = new traceMessageType();
		traceMessageType ASYNCH_SEND_PORT_EVENT_TYPE = new traceMessageType();
		traceMessageType ASYNCH_RECEIVE_PORT_EVENT_TYPE = new traceMessageType();
		traceMessageType ASYNCH_SEND_LINK_EVENT_TYPE = new traceMessageType();
		traceMessageType ASYNCH_RECEIVE_LINK_EVENT_TYPE = new traceMessageType();
		traceMessageType ASYNCH_SEND_MAILBOX_EVENT_TYPE = new traceMessageType();
		traceMessageType ASYNCH_RECEIVE_MAILBOX_EVENT_TYPE = new traceMessageType();
		traceMessageType ELSE_DELAY_TYPE = new traceMessageType();
		traceMessageType SEND_EXCEPTION_TYPE = new traceMessageType();
		traceMessageType RECEIVE_EXCEPTION_TYPE = new traceMessageType();
		traceMessageType ADD_UNACCEPTED_SEND_TYPE = new traceMessageType();
		traceMessageType ADD_UNACCEPTED_RECEIVE_TYPE = new traceMessageType();
		traceMessageType REMOVE_UNACCEPTED_SEND_TYPE = new traceMessageType();
		traceMessageType REMOVE_UNACCEPTED_RECEIVE_TYPE = new traceMessageType();
		traceMessageType REMOVE_UNACCEPTED_SEND_AND_RECEIVE_TYPE = new traceMessageType();

		final class traceMessageType { 
			private traceMessageType(){} 
		}
	}
