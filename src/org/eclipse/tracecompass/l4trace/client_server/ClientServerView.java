// ClientServerView.java
package org.eclipse.tracecompass.l4trace.client_server;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.tracecompass.tmf.core.event.*;
import org.eclipse.tracecompass.tmf.core.request.*;
import org.eclipse.tracecompass.tmf.core.signal.*;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

public class ClientServerView extends TmfView {
    private static final String VIEW_ID = "org.eclipse.tracecompass.l4trace.client_server.view";
    private TableViewer viewer;
    private ITmfTrace currentTrace;
    private final ThreadProvider threads = new ThreadProvider();
    private Long lastEventNumber = 0L;

    public ClientServerView() {
        super(VIEW_ID);
    }

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        
        viewer.setComparator(new ViewerComparator() {
            public int compare(Viewer viewer, Object e1, Object e2) {
                Thread t1 = (Thread) e1;
                Thread t2 = (Thread) e2;
                return t1.getImposedTime() < t2.getImposedTime() ? 1 : -1;
            };
        });

        String[] titles = { "Thread ID", "Thread Name", "Total Time", "Self Time", "Client Time", "Client Time Details" };
        int[] widths   = { 50,         100, 			100,          100,         100,           300 };
        for (int i = 0; i < titles.length; i++) {
            final int index = i;  // must be effectively final for the label provider
            TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
            col.getColumn().setText(titles[index]);
            col.getColumn().setWidth(widths[index]);
            col.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    Thread t = (Thread) element;
                    switch (titles[index]) {
                        case "Thread ID":             return t.getId();
                        case "Thread Name":           return t.getName();
                        case "Total Time":            return t.getTotalTimeFormatted();
                        case "Self Time":             return t.getSelfTimeFormatted();
                        case "Client Time":           return t.getImposedTimeFormatted();
                        case "Client Time Details":   return t.getImposedTimeDetailsFormatted();
                        default:                      return "";
                    }
                }
            });
        }
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    /** Dump all key fields of a thread to stderr with a custom tag. */
    private void dumpThread(Thread t, String tag) {
        long total = t.getTotalTime();
        long imposed = t.getImposedTime();
        System.err.printf(
            "[%s] Thread %s → total=%d, imposedSum=%d, start=%d, potImposed=%d, details=%s%n",
            tag, t.getId(), total, imposed,
            t.getPotentialImposedStart(), t.getPotentialImposedTime(),
            t.getImposedTimeDetailsFormatted()
        );
    }

    @TmfSignalHandler
    public void traceSelected(TmfTraceSelectedSignal signal) {
        if (currentTrace == signal.getTrace()) {
            return;
        }
        currentTrace = signal.getTrace();

        TmfEventRequest req = new TmfEventRequest(TmfEvent.class, 0,
                ITmfEventRequest.ALL_DATA, ITmfEventRequest.ExecutionType.BACKGROUND) {

            @Override
            public void handleData(ITmfEvent event) {
                super.handleData(event);

                // 1) Handle dropped events
                long evNum = Long.parseLong(event.getContent().getFieldValue(String.class, "context._event_count"));
                
                if (evNum - lastEventNumber > 1) {
                    System.err.println("[RESET] Dropped events: " + (evNum - lastEventNumber));
                    for (Thread t : threads.getThreads()) {
                        t.clearPotentialImposedTime();
                        t.setPotentialImposed(false);
                    }
                    lastEventNumber = evNum;
                    return;
                }
                lastEventNumber = evNum;

                // 2) Identify thread & timestamp
                String tid = event.getContent().getFieldValue(String.class, "context._dbg_id");
                String name = event.getContent().getFieldValue(String.class, "context._name");
                Thread th = threads.get(tid);
                if (th == null) {
                    th = new Thread(tid);
                    threads.put(tid, th);
                    System.err.println("[NEW THREAD] " + tid);
                }
                th.setName(name);
                long ts = event.getTimestamp().getValue();

                // 3) Dispatch by event type
                switch (event.getName()) {
                case "IPC": {
                	if (th.getIsScheduled()) {
						String op = event.getContent().getFieldValue(String.class, "type_");
						if (op.equals("Reply") || op.equals("ReplyAndWait")) {
							if (th.getPotentialImposed()) {
								String client = event.getContent().getFieldValue(String.class, "dbg_id");
								long delta = ts - th.getPotentialImposedStart();
								th.addPotentialImposedTime(delta);
								th.commitPotentialImposed(client);
							}
						} 
                	}
                    break;
                }
                case "IPCRES": {
                	if (th.getIsScheduled()) {
						String op2 = event.getContent().getFieldValue(String.class, "type_");
						if (op2.equals("Recv") || op2.equals("OpenWait")
								|| op2.equals("Wait") || op2.equals("ReplyAndWait")) {
							th.setPotentialImposed(true);
							th.setPotentialImposedStart(ts);
							th.clearPotentialImposedTime();
						}
						break;
                	}
                }
                case "sched_switch": {
                    // scheduled-out
                    String prevId = event.getContent().getFieldValue(String.class, "prev_tid");
                    Thread prev = threads.get(prevId);
                    if (prev == null) {
                        prev = new Thread(prevId);
                        threads.put(prevId, prev);
                        System.err.println("[NEW prevThread] " + prevId);
                    }
                    prev.setIsScheduled(false);
                    long lastTs = prev.getLastSchedTime();
                    if (lastTs != 0) {
                        prev.addTotalTime(ts - lastTs);
                    }
                    if (prev.getPotentialImposed()) {
                        prev.addPotentialImposedTime(ts - prev.getPotentialImposedStart());
                    }

                    // scheduled-in
                    String nextId = event.getContent().getFieldValue(String.class, "next_tid");
                    Thread next = threads.get(nextId);
                    if (next == null) {
                        next = new Thread(nextId);
                        threads.put(nextId, next);
                        System.err.println("[NEW nextThread] " + nextId);
                    }
                    next.setIsScheduled(true);
                    next.setLastSchedTime(ts);
                    if (next.getPotentialImposed()) {
                        next.setPotentialImposedStart(ts);
                    }
                    break;
                }
                } // switch
            }

            @Override
            public void handleSuccess() {
                super.handleSuccess();
                Display.getDefault().asyncExec(() -> viewer.setInput(threads.getThreads()));
            }
        };

        signal.getTrace().sendRequest(req);
    }
}
