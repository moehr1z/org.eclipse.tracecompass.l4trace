package org.eclipse.tracecompass.l4trace.ipc;

public class IpcArrow {
	private int srcId;
	private int dstId;
	private long ts;
	private long dur;

	public IpcArrow(int srcId, int dstId, long ts, long dur) {
		this.srcId = srcId;
		this.dstId = dstId;
		this.ts = ts;
		this.dur = dur;
	}
	
	public int getSrcId() {
		return srcId;
	}
	public void setSrcId(int srcId) {
		this.srcId = srcId;
	}
	public int getDstId() {
		return dstId;
	}
	public void setDstId(int dstId) {
		this.dstId = dstId;
	}
	public long getTs() {
		return ts;
	}
	public void setTs(long ts) {
		this.ts = ts;
	}
	public long getDur() {
		return dur;
	}
	public void setDur(long dur) {
		this.dur = dur;
	}
}