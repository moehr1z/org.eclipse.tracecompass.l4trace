package org.eclipse.tracecompass.l4trace.ipc;

import java.util.HashMap;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;


public class IpcStateProvider extends AbstractTmfStateProvider {
	private static final @NonNull String PROVIDER_ID = "org.eclipse.tracecompass.l4trace.ipc.provider"; //$NON-NLS-1$
    private static final int VERSION = 0;
    // maps send event number to corresponding sender context and timestamp
    private HashMap<Long, IpcSendContext> eventSndRcvMap = new HashMap<Long, IpcSendContext>();
    
    public IpcStateProvider(@NonNull ITmfTrace trace) {
        super(trace, PROVIDER_ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new IpcStateProvider(getTrace());
    }
    
    @Override
    protected void eventHandle(ITmfEvent event) {
		final long ts = event.getTimestamp().getValue();
		final long number = event.getContent().getFieldValue(Long.class, "context._event_count"); //$NON-NLS-1$
		ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

		// Initialize state for sender
        if (event.getName().equals("IPC")) { //$NON-NLS-1$
            final long sender = event.getContent().getFieldValue(Long.class, "context._ctx"); //$NON-NLS-1$
            
            final IpcSendContext ipcContext = new IpcSendContext(sender, ts);
            eventSndRcvMap.put(number, ipcContext);

            int senderQuark = ss.getQuarkAbsoluteAndAdd("IPC", String.valueOf(sender)); //$NON-NLS-1$
            ss.modifyAttribute(ts, "Waiting", senderQuark);

        // Correlate response to send + finish sender state and handle receiver state
        } else if (event.getName().equals("IPCRES")) { //$NON-NLS-1$
        	final long replier = event.getContent().getFieldValue(Long.class, "context._ctx"); //$NON-NLS-1$
        	final long pairEvent = event.getContent().getFieldValue(Long.class, "pair_event"); //$NON-NLS-1$
        	
        	IpcSendContext ipcContext = eventSndRcvMap.get(pairEvent);
        	if (ipcContext != null) {
        		long sender = ipcContext.getSendCtx();
        		// TODO check if it exists
				int senderQuark = ss.getQuarkAbsoluteAndAdd("IPC", String.valueOf(sender)); //$NON-NLS-1$
				ss.removeAttribute(ts, senderQuark);
				
				int replierQuark = ss.getQuarkAbsoluteAndAdd("IPC", String.valueOf(replier)); //$NON-NLS-1$
				ss.modifyAttribute(ipcContext.getSendTimestamp(), "Processing", replierQuark);
				ss.removeAttribute(ts, replierQuark);
				
				eventSndRcvMap.remove(pairEvent);
        	} else {
        		System.out.println("Found no matching send for the reply...");
        	}
        }
    }

}