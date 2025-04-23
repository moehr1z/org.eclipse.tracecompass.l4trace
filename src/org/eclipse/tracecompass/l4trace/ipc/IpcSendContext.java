package org.eclipse.tracecompass.l4trace.ipc;

public class IpcSendContext {
	private String rcvId;
	private String rcvName;
	private long sendTimestamp;
	
	public IpcSendContext(String rcvId, String rcvName, long sendTimestamp) {
		this.rcvId = rcvId;
		this.rcvName = rcvName;
		this.sendTimestamp = sendTimestamp;
	}
	
	public String getRcvId() {
		return rcvId;
	}

	public void setRcvId(String rcvId) {
		this.rcvId = rcvId;
	}

	public String getRcvName() {
		return rcvName;
	}

	public void setRcvName(String rcvName) {
		this.rcvName = rcvName;
	}

	public long getSendTimestamp() {
		return sendTimestamp;
	}

	public void setSendTimestamp(long sendTimestamp) {
		this.sendTimestamp = sendTimestamp;
	}
}