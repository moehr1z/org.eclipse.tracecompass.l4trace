package org.eclipse.tracecompass.l4trace.ipc;

public class IpcTooltip {
	private int quark;
	private String text;
	
	public IpcTooltip(int quark, String text) {
		super();
		this.quark = quark;
		this.text = text;
	}
	public int getQuark() {
		return quark;
	}
	public void setQuark(int quark) {
		this.quark = quark;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

}