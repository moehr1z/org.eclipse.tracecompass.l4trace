package org.eclipse.tracecompass.l4trace.client_server;

import java.util.HashMap;
import java.util.Map;

public class Thread {
    private final String id;
    private Long totalTime = 0L;
    private Boolean potentialImposed = false;
    private Long potentialImposedTime = 0L;
    private final Map<String, Long> imposedTimeDetails = new HashMap<>();
    private Long lastSchedTime = 0L;
    private Long potentialImposedStart = 0L;
    private Boolean isScheduled = false;
    private String name = "";


	public Thread(String id) {
        this.id = id;
        this.name = "";
        debug("NEW", "");
    }


    private boolean isDebug() {
        return "12".equals(id);
    }

    private void debug(String field, String msg) {
        if (isDebug()) {
            System.err.printf("[Thread %s] %s → %s%n", id, field, msg);
        }
    }

    public Boolean getIsScheduled() { return isScheduled; }
	public void setIsScheduled(Boolean isScheduled) { this.isScheduled = isScheduled; }

    public String getId() {
        return id;
    }

    public void setName(String name) {
    	this.name = name;
    }
    public String getName() {
        return name;
    }

    public Long getTotalTime() {
        return totalTime;
    }
    public String getTotalTimeFormatted() {
    	return TscUtils.tscToFormattedTime(getTotalTime());
    }

    public Long getImposedTime() {
        long sum = 0L;
        for (long v : imposedTimeDetails.values()) sum += v;
        
        return sum;
    }
    public String getImposedTimeFormatted() {
    	return TscUtils.tscToFormattedTime(getImposedTime());
    }
    

    public String getImposedTimeDetailsFormatted() {
        if (imposedTimeDetails.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        imposedTimeDetails.forEach((k,v) -> sb.append(k).append("=").append(TscUtils.tscToFormattedTime(v)).append(", "));
        sb.setLength(sb.length() - 2);
        return sb.append("}").toString();
    }

    public Long getSelfTime() {
        long self = totalTime - getImposedTime();
        return self;
    }
    public String getSelfTimeFormatted() {
    	return TscUtils.tscToFormattedTime(getSelfTime());
    }

    public Long getLastSchedTime() {
        return lastSchedTime;
    }
    public void setLastSchedTime(Long t) {
//        debug("setLastSchedTime", "old=" + lastSchedTime + " new=" + t);
        lastSchedTime = t;
    }

    public Boolean getPotentialImposed() {
        return potentialImposed;
    }
    public void setPotentialImposed(Boolean b) {
        debug("setPotentialImposed", "old=" + potentialImposed + " new=" + b);
        potentialImposed = b;
    }

    public Long getPotentialImposedStart() {
        return potentialImposedStart;
    }
    public void setPotentialImposedStart(Long t) {
        debug("setPotentialImposedStart", "old=" + potentialImposedStart + " new=" + t);
        potentialImposedStart = t;
    }

    public void clearPotentialImposedTime() {
        debug("clearPotentialImposedTime", "old=" + potentialImposedTime + " new=0");
        potentialImposedTime = 0L;
    }

    public void addPotentialImposedTime(Long t) {
        debug("addPotentialImposedTime", "delta=" + t + " → " + potentialImposedTime + "+" + t);
        potentialImposedTime += t;
    }

    public Long getPotentialImposedTime() {
        return potentialImposedTime;
    }

    public void addTotalTime(Long delta) {
//        debug("addTotalTime", "delta=" + delta + " → " + totalTime + "+" + delta);
        totalTime += delta;
    }

    public void commitPotentialImposed(String clientThread) {
        Long prev = imposedTimeDetails.get(clientThread);
        long sum = (prev == null ? 0L : prev) + potentialImposedTime;
        debug("commitPotentialImposed",
              "client=" + clientThread +
              " prev=" + prev +
              " added=" + potentialImposedTime +
              " → new=" + sum);
        imposedTimeDetails.put(clientThread, sum);
        potentialImposedTime = 0L;
        potentialImposed = false;
    }
}

