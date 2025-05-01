package org.eclipse.tracecompass.l4trace.ipc;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

public class IpcAnalysisModule extends TmfStateSystemAnalysisModule {

	private IpcStateProvider stateProvider = null;
	
    public static final String ID = "org.eclipse.tracecompass.l4trace.state.system.module"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
    	stateProvider = new IpcStateProvider(Objects.requireNonNull(getTrace()));
        return stateProvider;
    }
    
    public List<IpcArrow> getArrows() {
		return stateProvider.getArrows();
    }
    
    public List<IpcTooltip> getTooltips() {
    	return stateProvider.getTooltips();
    }

}