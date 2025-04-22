package com.example.analysis;

import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

public class MyStateSystemAnalysisModule extends TmfStateSystemAnalysisModule {

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new MyStateProvider(getTrace());
    }

    @Override
    public String getId() {
        return "com.example.analysis.mystateanalysis";
    }
}
