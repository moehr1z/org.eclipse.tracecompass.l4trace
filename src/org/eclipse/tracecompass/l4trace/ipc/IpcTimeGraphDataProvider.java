package org.eclipse.tracecompass.l4trace.ipc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

public class IpcTimeGraphDataProvider
extends AbstractTmfTraceDataProvider
implements ITimeGraphDataProvider<ITimeGraphEntryModel>, IOutputStyleProvider {

	public static final String ID = "org.eclipse.dataprovider.timegraph.ipc"; //$NON-NLS-1$

	private final IpcAnalysisModule fModule;
    private static final AtomicLong sfAtomicId = new AtomicLong();
    private final BiMap<Long, Integer> fIDToDisplayQuark = HashBiMap.create();
    private static final String STYLE0_NAME = "styleCall"; //$NON-NLS-1$
    private static final String STYLE1_NAME = "styleSend"; //$NON-NLS-1$
    private static final String STYLE2_NAME = "styleRecv"; //$NON-NLS-1$
    private static final String STYLE3_NAME = "styleOpenWait"; //$NON-NLS-1$
    private static final String STYLE4_NAME = "styleReply"; //$NON-NLS-1$
    private static final String STYLE5_NAME = "styleWait"; //$NON-NLS-1$
    private static final String STYLE6_NAME = "styleSendAndWait"; //$NON-NLS-1$
    private static final String STYLE7_NAME = "styleReplyAndWait"; //$NON-NLS-1$
    private static final String STYLE8_NAME = "styleCallIpc"; //$NON-NLS-1$



    /* The map of basic styles */
    private static final Map<String, OutputElementStyle> STATE_MAP;
    /*
     * A map of styles names to a style that has the basic style as parent, to
     * avoid returning complete styles for each state
     */
    private static final Map<String, OutputElementStyle> STYLE_MAP;
    
    static {
        /* Build three different styles to use as examples */
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();

        builder.put(STYLE0_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE0_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("blue")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 0.5f,
                StyleProperties.OPACITY, 0.75f)));
        builder.put(STYLE1_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE1_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("yellow")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));
        builder.put(STYLE2_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE2_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("green")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 0.75f,
                StyleProperties.OPACITY, 0.5f)));
        builder.put(STYLE3_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE3_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("red")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));
        builder.put(STYLE4_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE4_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("brown")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));
        builder.put(STYLE5_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE5_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("pink")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));
        builder.put(STYLE6_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE6_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("black")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));
        builder.put(STYLE7_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE7_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("orange")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));
        builder.put(STYLE8_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE8_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("purple")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));

        STATE_MAP = builder.build();

        /* build the style map too */
        builder = new ImmutableMap.Builder<>();
        builder.put(STYLE0_NAME, new OutputElementStyle(STYLE0_NAME));
        builder.put(STYLE1_NAME, new OutputElementStyle(STYLE1_NAME));
        builder.put(STYLE2_NAME, new OutputElementStyle(STYLE2_NAME));
        builder.put(STYLE3_NAME, new OutputElementStyle(STYLE3_NAME));
        builder.put(STYLE4_NAME, new OutputElementStyle(STYLE4_NAME));
        builder.put(STYLE5_NAME, new OutputElementStyle(STYLE5_NAME));
        builder.put(STYLE6_NAME, new OutputElementStyle(STYLE6_NAME));
        builder.put(STYLE7_NAME, new OutputElementStyle(STYLE7_NAME));
        builder.put(STYLE8_NAME, new OutputElementStyle(STYLE8_NAME));

        STYLE_MAP = builder.build();
    }

    /**
     * Constructor
     *
     * @param trace
     *            The trace this analysis is for
     * @param module
     *            The scripted analysis for this data provider
     */
	public IpcTimeGraphDataProvider(ITmfTrace trace, IpcAnalysisModule module) {
		super(trace);
		fModule = module;
	}

    /**
     * Create the time graph data provider
     *
     * @param trace
     *            The trace for which is the data provider
     * @return The data provider
     */
	public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace) {
		IpcAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace,
				IpcAnalysisModule.class, IpcAnalysisModule.ID);
		return module != null ? new IpcTimeGraphDataProvider(trace, module) : null;
	}
	
	@Override
    public TmfModelResponse<TmfTreeModel<@NonNull ITimeGraphEntryModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        boolean isComplete = ss.waitUntilBuilt(0);
        long endTime = ss.getCurrentEndTime();
        
        // Make an entry for each base quark
        List<ITimeGraphEntryModel> entryList = new ArrayList<>();
        for (Integer quark : ss.getQuarks("IPC", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            Long id = (long) quark;
            entryList.add(new TimeGraphEntryModel(id, -1, ss.getAttributeName(quark), ss.getStartTime(), endTime));
        }


        List<String> headerList = new ArrayList<>();
        headerList.add("ID");
        Status status = isComplete ? Status.COMPLETED : Status.RUNNING;
        String msg = isComplete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING;
        return new TmfModelResponse<>(new TmfTreeModel<>(headerList, entryList), status, msg);
    }
	
    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public @NonNull TmfModelResponse<TimeGraphModel> fetchRowModel(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        try {
            List<@NonNull ITimeGraphRowModel> rowModels = getDefaultRowModels(fetchParameters, ss, monitor);
            if (rowModels == null) {
                rowModels = Collections.emptyList();
            }
            return new TmfModelResponse<>(new TimeGraphModel(rowModels), Status.COMPLETED, CommonStatusMessage.COMPLETED);
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }
    }
    
    private @Nullable List<ITimeGraphRowModel> getDefaultRowModels(Map<String, Object> fetchParameters, ITmfStateSystem ss, @Nullable IProgressMonitor monitor) throws IndexOutOfBoundsException, TimeRangeException, StateSystemDisposedException {
        Map<Integer, ITimeGraphRowModel> quarkToRow = new HashMap<>();
        
        // This regex map automatically filters or highlights the entry
        // according to the global filter entered by the user
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        // Prepare the quarks to display
        Collection<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        for (Integer quark : ss.getQuarks("IPC", "*")) {
				long id =  (long) quark;

                if (selectedItems == null || selectedItems.isEmpty() || selectedItems.contains(id)) {
                    quarkToRow.put(quark, new TimeGraphRowModel(id, new ArrayList<>()));
                }
        }


        // Query the state system to fill the states
        long currentEndTime = ss.getCurrentEndTime();
        for (ITmfStateInterval interval : ss.query2D(quarkToRow.keySet(), getTimes(ss, DataProviderParameterUtils.extractTimeRequested(fetchParameters)))) {
        	
            if (monitor != null && monitor.isCanceled()) {
                return Collections.emptyList();
            }
            ITimeGraphRowModel row = quarkToRow.get(interval.getAttribute());
            if (row != null) {
                List<@NonNull ITimeGraphState> states = row.getStates();
                ITimeGraphState timeGraphState = getStateFromInterval(interval, currentEndTime);
                applyFilterAndAddState(states, timeGraphState, row.getEntryID(), predicates, monitor);
            }
        }
        for (ITimeGraphRowModel model : quarkToRow.values()) {
            model.getStates().sort(Comparator.comparingLong(ITimeGraphState::getStartTime));
        }

        return new ArrayList<>(quarkToRow.values());
    }
    
    private static TimeGraphState getStateFromInterval(ITmfStateInterval statusInterval, long currentEndTime) {
        long time = statusInterval.getStartTime();
        long duration = Math.min(currentEndTime, statusInterval.getEndTime() + 1) - time;
        
        Object o = statusInterval.getValue();        
        
        if (!(o instanceof String)) {
            // Add a null state
            return new TimeGraphState(time, duration, Integer.MIN_VALUE);
        }
        
        String stateName = o.toString();
        String styleName = "style" + stateName; //$NON-NLS-1$ 
        
        return new TimeGraphState(time, duration, String.valueOf(o), STYLE_MAP.get(styleName));
    }

    private static Set<Long> getTimes(ITmfStateSystem key, @Nullable List<Long> list) {
        if (list == null) {
            return Collections.emptySet();
        }
        Set<@NonNull Long> times = new HashSet<>();
        for (long t : list) {
            if (key.getStartTime() <= t && t <= key.getCurrentEndTime()) {
                times.add(t);
            }
        }
        return times;
    }

    // FIX: currently you loose the arrows when reopening trace compass. A proper fix would be to have the arrows as quarks in the state system and draw them from there
    @Override
    public @NonNull TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
    	fModule.waitForCompletion();
        
        List<IpcArrow> analysisArrows = fModule.getArrows();
        List<ITimeGraphArrow> arrows = new ArrayList<>(analysisArrows.size());

        for (IpcArrow arr : analysisArrows) {
        	long srcId = (long) arr.getSrcId();
            long dstId = (long) arr.getDstId();
        	long ts = arr.getTs();
        	long dur = arr.getDur();
        	
        	arrows.add(new TimeGraphArrow(srcId, dstId, ts, dur));
        }
    	
        return new TmfModelResponse<>(arrows, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }


    @Override
    public @NonNull TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
    	fModule.waitForCompletion();
    	
        return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }
}
