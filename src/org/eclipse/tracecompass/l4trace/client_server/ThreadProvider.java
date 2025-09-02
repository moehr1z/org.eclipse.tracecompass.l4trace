package org.eclipse.tracecompass.l4trace.client_server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadProvider {
	private Map<String, Thread> threads;
	
	public ThreadProvider() {
		threads = new HashMap<String, Thread>();
	}
	
	public List<Thread> getThreads() {
		ArrayList<Thread> threadList = new ArrayList<Thread>();
		for (Thread t: this.threads.values()) {
			if (t.getImposedTime() > 0) {
				threadList.add(t);
			}
		}
		return threadList;
	}
	
	public void put(String id, Thread thread) {
		this.threads.put(id, thread);
	}
	
	public Thread get(String id) {
		return this.threads.get(id);
	}
	
}