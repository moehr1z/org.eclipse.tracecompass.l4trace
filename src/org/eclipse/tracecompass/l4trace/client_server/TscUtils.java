package org.eclipse.tracecompass.l4trace.client_server;

public class TscUtils {
	public static String tscToFormattedTime(long tscNanos) {
	    if (tscNanos / 1_000_000_000L >= 1) {
	        return String.format("%.3f s", tscNanos / 1_000_000_000.0);
	    } else if (tscNanos / 1_000_000L >= 1) {
	        return String.format("%.3f ms", tscNanos / 1_000_000.0);
	    } else if (tscNanos / 1_000L >= 1) {
	        return String.format("%.3f μs", tscNanos / 1_000.0);
	    } else {
	        return tscNanos + " ns";
	    }
	}
}