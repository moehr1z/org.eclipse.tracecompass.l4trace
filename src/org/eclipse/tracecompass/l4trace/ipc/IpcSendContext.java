package org.eclipse.tracecompass.l4trace.ipc;

public class IpcSendContext {
	private long sendCtx;
	private long sendTimestamp;
	
	public IpcSendContext(long sendCtx, long sendTimestamp) {
		this.sendCtx = sendCtx;
		this.sendTimestamp = sendTimestamp;
	}
	
	public long getSendCtx() {
		return sendCtx;
	}

	public void setSendCtx(long sendCtx) {
		this.sendCtx = sendCtx;
	}

	public long getSendTimestamp() {
		return sendTimestamp;
	}

	public void setSendTimestamp(long sendTimestamp) {
		this.sendTimestamp = sendTimestamp;
	}
}