package org.eclipse.tracecompass.l4trace.ipc;


import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;


@SuppressWarnings("restriction")
public class IpcTimeGraphView extends BaseDataProviderTimeGraphView {

    public static final String ID = "org.eclipse.tracecompass.l4trace.timegraph.view"; //$NON-NLS-1$

    public IpcTimeGraphView() {
        super(ID, new BaseDataProviderTimeGraphPresentationProvider(), IpcTimeGraphDataProvider.ID);
    }
}
