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

		final String sender_id = event.getContent().getFieldValue(String.class, "context._dbg_id"); //$NON-NLS-1$
		final String sender_name = event.getContent().getFieldValue(String.class, "context._name"); //$NON-NLS-1$

		// Initialize state for sender
        if (event.getName().equals("IPC")) { //$NON-NLS-1$
        	final String receiver_id = event.getContent().getFieldValue(String.class, "dbg_id"); //$NON-NLS-1$
        	final String receiver_name = event.getContent().getFieldValue(String.class, "rcv_name"); //$NON-NLS-1$
            
            final IpcSendContext ipcContext = new IpcSendContext(receiver_id, receiver_name, ts);		
            eventSndRcvMap.put(number, ipcContext);

            int senderQuark;
            if (sender_name == "") {
				senderQuark = ss.getQuarkAbsoluteAndAdd("IPC", sender_id); //$NON-NLS-1$
            } else {
				senderQuark = ss.getQuarkAbsoluteAndAdd("IPC", sender_name); //$NON-NLS-1$
            }
			ss.modifyAttribute(ts, "Waiting", senderQuark);

            int receiverQuark = ss.getQuarkAbsoluteAndAdd("IPC", receiver_id); //$NON-NLS-1$
            ss.modifyAttribute(ts, "Processing", receiverQuark);

        // Correlate response to send + finish sender state and handle receiver state
        } else if (event.getName().equals("IPCRES")) { //$NON-NLS-1$
        	final long pairEvent = event.getContent().getFieldValue(Long.class, "pair_event"); //$NON-NLS-1$
        	
        	IpcSendContext ipcContext = eventSndRcvMap.get(pairEvent);
        	if (ipcContext != null) {
				int senderQuark;
				if (sender_name == "") {
					senderQuark = ss.getQuarkAbsoluteAndAdd("IPC", sender_id); //$NON-NLS-1$
				} else {
					senderQuark = ss.getQuarkAbsoluteAndAdd("IPC", sender_name); //$NON-NLS-1$
				}
				ss.removeAttribute(ts, senderQuark);
				
				String receiver_id = ipcContext.getRcvId();	
				String receiver_name = ipcContext.getRcvName();
				int receiverQuark;
				if (receiver_name == "") {
					receiverQuark = ss.getQuarkAbsoluteAndAdd("IPC", receiver_id); //$NON-NLS-1$
				} else {
					receiverQuark = ss.getQuarkAbsoluteAndAdd("IPC", receiver_name); //$NON-NLS-1$
				}
				ss.removeAttribute(ts, receiverQuark);
				
				eventSndRcvMap.remove(pairEvent);
        	} else {
        		System.out.println("Found no matching send for the reply...");
        	}
        }
    }

}