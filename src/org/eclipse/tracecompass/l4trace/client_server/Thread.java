package org.eclipse.tracecompass.l4trace.client_server;

import java.util.HashMap;
import java.util.Map;

public class Thread {
	private String id;
	private Long totalTime;
	private Long imposedTime;
	private Boolean potentialImposed;
	private Long potentialImposedTime;
	private Map<String, Long> imposedTimeDetails;
	private Long lastSchedTime;
	private Long potentialImposedStart;
	

	public Thread(String id) {
		super();
		this.potentialImposed = false;
		this.potentialImposedTime = 0l;
		this.id = id;
		this.totalTime = 0l;
		this.imposedTime = 0l;
		this.imposedTimeDetails = new HashMap<String, Long>();
		this.lastSchedTime = 0l;
		this.potentialImposedStart = 0l;
	}
	
	public Long getPotentialImposedStart() {
		return potentialImposedStart;
	}

	public void setPotentialImposedStart(Long potentialImposedStart) {
		this.potentialImposedStart = potentialImposedStart;
	}

	public Long getLastSchedTime() {
		return lastSchedTime;
	}

	public void setLastSchedTime(Long lastSchedTime) {
		this.lastSchedTime = lastSchedTime;
	}

	public Boolean getPotentialImposed() {
		return potentialImposed;
	}

	public void setPotentialImposed(Boolean potentialImposed) {
		this.potentialImposed = potentialImposed;
	}

	public void addPotentialImposedTime(Long time) {
		this.potentialImposedTime += time;
	}

	public void clearPotentialImposedTime() {
		this.potentialImposedTime = 0l;
	}
	
	public void commitPotentialImposed(String clientThread) {
		Long clientTime = this.imposedTimeDetails.get(clientThread);
		
		if (clientTime == null) {
			this.imposedTimeDetails.put(clientThread, this.potentialImposedTime);
		} else {
			this.imposedTimeDetails.replace(clientThread, clientTime + this.potentialImposedTime);
		}
		this.potentialImposedTime = 0l;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTotalTime() {
		// TODO convert
		return totalTime.toString();
	}
	public void addTotalTime(Long totalTime) {
		this.totalTime += totalTime;
	}
	public String getImposedTime() {
		Long sum = 0l;
		// TODO do this in commit imposed time and only return here
		for (Long v: imposedTimeDetails.values()) {
			sum += v;
		}
		
		// TODO convert

		return sum.toString();
	}
	public void addImposedTime(String imposerId, Long imposedTime) {
		Long i = this.imposedTimeDetails.get(imposerId);
		if (i == null) {
			this.imposedTimeDetails.put(imposerId, imposedTime);
		} else {
			i += imposedTime;
		}
		
		this.imposedTime += imposedTime;
	}
	public String getImposedTimeDetails() {
		// TODO convert
	    StringBuilder mapAsString = new StringBuilder("{");
	    for (String key : this.imposedTimeDetails.keySet()) {
	        mapAsString.append(key + "=" + this.imposedTimeDetails.get(key).toString() + ", ");
	    }
	    mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
	    return mapAsString.toString();
	}
	public String getSelfTime() {
		// TODO convert
		Long selfTime = this.totalTime - this.imposedTime;
		return selfTime.toString();
	}
}
