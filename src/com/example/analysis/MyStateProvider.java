package com.example.analysis;

import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public class MyStateProvider extends AbstractTmfStateProvider {

    public MyStateProvider(ITmfTrace trace) {
        super(trace, "MyStateProvider");
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        long timestamp = event.getTimestamp().toNanos();

        // Example: Add a dummy attribute with a constant value
        int quark = ss.getQuarkAbsoluteAndAdd("Dummy");
        ss.modifyAttribute(timestamp, 42, quark);
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new MyStateProvider(getTrace());
    }
}

