package org.eclipse.tracecompass.l4trace.ipc;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

public class IpcAnalysisModule extends TmfStateSystemAnalysisModule {

    public static final String ID = "org.eclipse.tracecompass.l4trace.state.system.module"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new IpcStateProvider(Objects.requireNonNull(getTrace()));
    }

}