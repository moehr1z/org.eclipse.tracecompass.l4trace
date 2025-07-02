package org.eclipse.tracecompass.l4trace.client_server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.ui.part.ViewPart;


public class ClientServerView extends TmfView {
	private static final String VIEW_ID = "org.eclipse.tracecompass.l4trace.client_server.view";
	private TableViewer viewer;
    private ITmfTrace currentTrace;
    private ThreadProvider threads; 
    private Long lastEventNumber = 0l;

    public ClientServerView() {
        super(VIEW_ID);
		threads = new ThreadProvider();
    }

    @Override
    public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn colThreadId = new TableViewerColumn(viewer, SWT.NONE);
		colThreadId.getColumn().setWidth(200);
		colThreadId.getColumn().setText("Thread ID");
		colThreadId.setLabelProvider(new ColumnLabelProvider() {
		    @Override
		    public String getText(Object element) {
		    	Thread t = (Thread) element;
		    	System.out.println(t.getId());
		    	return t.getId();
		    }
		});

		TableViewerColumn colTotal = new TableViewerColumn(viewer, SWT.NONE);
		colTotal.getColumn().setWidth(200);
		colTotal.getColumn().setText("Total Time");
		colTotal.setLabelProvider(new ColumnLabelProvider() {
		    @Override
		    public String getText(Object element) {
		    	Thread t = (Thread) element;
		    	return t.getTotalTime();
		    }
		});

		TableViewerColumn colSelf = new TableViewerColumn(viewer, SWT.NONE);
		colSelf.getColumn().setWidth(200);
		colSelf.getColumn().setText("Self Time");
		colSelf.setLabelProvider(new ColumnLabelProvider() {
		    @Override
		    public String getText(Object element) {
		    	Thread t = (Thread) element;
		    	return t.getSelfTime();
		    }
		});

		TableViewerColumn colImposed = new TableViewerColumn(viewer, SWT.NONE);
		colImposed.getColumn().setWidth(200);
		colImposed.getColumn().setText("Client Time");
		colImposed.setLabelProvider(new ColumnLabelProvider() {
		    @Override
		    public String getText(Object element) {
		    	Thread t = (Thread) element;
		    	return t.getImposedTime();
		    }
		});

		TableViewerColumn colImposedDetails = new TableViewerColumn(viewer, SWT.NONE);
		colImposedDetails.getColumn().setWidth(500);
		colImposedDetails.getColumn().setText("Client Time Details");
		colImposedDetails.setLabelProvider(new ColumnLabelProvider() {
		    @Override
		    public String getText(Object element) {
		    	Thread t = (Thread) element;
		    	return t.getImposedTimeDetails();
		    }
		});
		
    }

    @Override
    public void setFocus() {
    	viewer.getControl().setFocus();

    }
    
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        // Don't populate the view again if we're already showing this trace
        if (currentTrace == signal.getTrace()) {
            return;
        }
        currentTrace = signal.getTrace();


        // Create the request to get data from the trace
        TmfEventRequest req = new TmfEventRequest(TmfEvent.class, 0, ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.BACKGROUND) {

            @Override
            public void handleData(ITmfEvent event) {
                // Called for each event
                super.handleData(event);
                
                Long eventNumber = Long.parseLong(event.getContent().getFieldValue(String.class, "context._event_count")); //$NON-NLS-1$
                // Events were dropped
                if (eventNumber - lastEventNumber > 1) {		
                	System.out.println("Dropped events." + eventNumber + "\t" + lastEventNumber);
                	for (Thread thread: threads.getThreads()) {
                		thread.clearPotentialImposedTime();
                		thread.setPotentialImposed(false);
                	}
                	lastEventNumber = eventNumber;
                	return;
                }
                
				lastEventNumber = eventNumber;
                
				String threadId = event.getContent().getFieldValue(String.class, "context._dbg_id"); //$NON-NLS-1$
				Thread thread = threads.get(threadId);
				if (thread == null) {
					thread = new Thread(threadId);
					threads.put(threadId, thread);
				} 
				Long timestamp = event.getTimestamp().getValue();
                
                if (event.getName().equals("IPC")) {
					String operation = event.getContent().getFieldValue(String.class, "type_");

					// Thread replies, we assume it to be some server
					if (operation.equals("Reply") || operation.equals("ReplyAndWait")) {
						String clientThread = event.getContent().getFieldValue(String.class, "dbg_id"); //$NON-NLS-1$
						thread.addPotentialImposedTime(timestamp - thread.getPotentialImposedStart());
						thread.commitPotentialImposed(clientThread);	
					}
					
					if (operation.equals("Send") || operation.equals("Call")) { 
						thread.setPotentialImposed(false);
						thread.clearPotentialImposedTime();
					}
                } else if (event.getName().equals("IpcRes")) { 
					String operation = event.getContent().getFieldValue(String.class, "type_");

					// Thread waited for some IPC, so possibly a server
					if (operation.equals("Recv") || operation.equals("OpenWait") || operation.equals("Wait") || operation.equals("ReplyAndWait")) {
						thread.setPotentialImposed(true);
						thread.setPotentialImposedStart(timestamp);
					} 
                } else if (event.getName().equals("sched_switch")) {
                	// handle scheduled out thread
                	String prevThreadId = event.getContent().getFieldValue(String.class, "prev_tid"); //$NON-NLS-1$
                	Thread prevThread = threads.get(prevThreadId);
                	if (prevThread == null) {
						prevThread = new Thread(prevThreadId);
						threads.put(prevThreadId, prevThread);
                	}
                	Long lastSchedTime = prevThread.getLastSchedTime();
                	if (lastSchedTime != 0) {
						prevThread.addTotalTime(timestamp - lastSchedTime);
                	}
                	if (prevThread.getPotentialImposed()) {
                		prevThread.addPotentialImposedTime(timestamp - prevThread.getPotentialImposedStart()); 
                	}

                	// handle scheduled in thread
                	String nextThreadId = event.getContent().getFieldValue(String.class, "next_tid"); //$NON-NLS-1$
                	Thread nextThread = threads.get(nextThreadId);
                	if (nextThread == null) {
						nextThread = new Thread(nextThreadId);
						threads.put(nextThreadId, nextThread);
                	}
                	nextThread.setLastSchedTime(timestamp);
                	if (nextThread.getPotentialImposed()) {
                		nextThread.setPotentialImposedStart(timestamp);
                	}
                }
                
            }

            @Override
            public void handleSuccess() {
                // Request successful, not more data available
                super.handleSuccess();

                // This part needs to run on the UI thread since it updates the chart SWT control
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                    }

                });
            }
        };
        ITmfTrace trace = signal.getTrace();
        trace.sendRequest(req);
        
		viewer.setInput(threads.getThreads());
    }


}