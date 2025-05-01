package org.eclipse.tracecompass.l4trace.ipc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    private List<IpcArrow> arrows = new ArrayList<IpcArrow>();
    private List<IpcTooltip> tooltips = new ArrayList<IpcTooltip>();
    
    public IpcStateProvider(@NonNull ITmfTrace trace) {
        super(trace, PROVIDER_ID);
    }
    
    public List<IpcArrow> getArrows() {
		return arrows;
    }
    
    public List<IpcTooltip> getTooltips() {
    	return tooltips;
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
		final String operation = event.getContent().getFieldValue(String.class, "type_");

		ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

		final String dbg_id = event.getContent().getFieldValue(String.class, "context._dbg_id"); //$NON-NLS-1$
		int quark = ss.getQuarkAbsoluteAndAdd("IPC", dbg_id); //$NON-NLS-1$

        if (event.getName().equals("IPC")) { //$NON-NLS-1$
			ss.modifyAttribute(ts, operation, quark);
        	
        	// for operations where we don't send anything, we don't have to draw an arrow
        	if (!(operation.equals("Recv") | operation.equals("OpenWait") | operation.equals("Wait"))) {
            	final String receiver_id = event.getContent().getFieldValue(String.class, "dbg_id"); //$NON-NLS-1$
            	
    			int receiverQuark = ss.getQuarkAbsoluteAndAdd("IPC", receiver_id); //$NON-NLS-1$
    			
                IpcArrow corr = new IpcArrow(quark, receiverQuark, ts, 0);
                arrows.add(corr);
        	}
        	
    		final long tag = event.getContent().getFieldValue(Long.class, "tag"); 
    		final long label = event.getContent().getFieldValue(Long.class, "label"); 
    		final String name = event.getContent().getFieldValue(String.class, "context._name"); //$NON-NLS-1$

    		String tooltipText = String.format("Dbg ID: %s \n Name: %s \n tag: %d \n label: %d", dbg_id, name, tag, label);
    		tooltips.add(new IpcTooltip(quark, tooltipText));
        } else if (event.getName().equals("IPCRES")) { //$NON-NLS-1$
    		final long pairEvent = event.getContent().getFieldValue(Long.class, "pair_event");
    		// Sometimes we get res events from IPC events which happened before we started the stream server
    		if (pairEvent < number) {
    			ss.removeAttribute(ts, quark);
        	}
        }
    }

}