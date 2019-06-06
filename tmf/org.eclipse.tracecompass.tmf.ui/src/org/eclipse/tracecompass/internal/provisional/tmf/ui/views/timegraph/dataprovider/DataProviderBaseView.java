/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph.dataprovider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.model.TimeLineEvent;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.IFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry.Sampling;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeLinkEvent;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * A data provider time graph view that can display data coming from many
 * different data providers, which can also be of different types, ie either XY,
 * or time graph data providers.
 *
 * @author Matthew Khouzam
 * @author Geneviève Bastien
 */
public class DataProviderBaseView extends AbstractTimeGraphView {

    private final Map<ITmfTrace, Table<TraceEntry, DPEntry, DataProviderEntries>> fEntriesMap = new HashMap<>();
    private static final AtomicLong fIds = new AtomicLong(-100000000);
    private final Object fLock = new Object();
    private final Collection<String> fProviderIds;

    private @Nullable Table<TraceEntry, DPEntry, DataProviderEntries> fCurrentEntries = HashBasedTable.create();

    class DataProviderEntries {
        private final Map<Long, TimeGraphEntry> fEntries = new HashMap<>();
        private final DPEntry fDpEntry;

        public DataProviderEntries(DPEntry dpEntry) {
            fDpEntry = dpEntry;
        }

        public DPEntry getDpEntry() {
            return fDpEntry;
        }

        public @Nullable TimeGraphEntry get(long id) {
            return fEntries.get(id);
        }

        public void put(long id, TimeGraphEntry uiEntry) {
            fEntries.put(id, uiEntry);
        }

        public Map<Long, TimeGraphEntry> getEntries() {
            return fEntries;
        }
    }

    /**
     * Constructs a time graph view that contains a time graph viewer.
     *
     * By default, the view uses a single default column in the name space that
     * shows the time graph entry name. To use multiple columns and/or
     * customized label texts, the subclass constructor must call
     * {@link #setTreeColumns(String[])} and/or
     * {@link #setTreeLabelProvider(TreeLabelProvider)}.
     *
     * @param id
     *            The id of the view
     * @param pres
     *            The presentation provider
     * @param providerIds
     *            the IDs for the data providers to use to populate this view
     */
    public DataProviderBaseView(String id, TimeGraphPresentationProvider pres, Collection<String> providerIds) {
        super(id, pres);
        fProviderIds = providerIds;
    }

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries = getCurrentEntriesForTrace(parentTrace);

        // TODO: store and sort
        DataProviderManager dpm = DataProviderManager.getInstance();
        Collection<String> ids = fProviderIds;
        TraceEntry traceEntry = getTraceEntry(trace, parentTrace, currentEntries);
        for (String id : ids) {
            ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> tDataProvider = dpm.getDataProvider(trace, id, ITimeGraphDataProvider.class);
            if (tDataProvider != null) {
                // Get the entries for this data provider
                DataProviderEntries dpEntries = getDpEntries(traceEntry, tDataProvider, currentEntries);

                List<TimeGraphEntry> entries = DiscreteDataProviderEntryProvider.getEntriesFromDataProvider(trace, parentTrace, tDataProvider, dpEntries, this, monitor);
                for (TimeGraphEntry entry : entries) {
                    traceEntry.addChild(entry);
                }
            }
            AbstractTreeCommonXDataProvider<@NonNull TmfStateSystemAnalysisModule, @NonNull ITmfTreeDataModel> xyDataProvider = dpm.getDataProvider(trace, id, AbstractTreeCommonXDataProvider.class);
            if (xyDataProvider != null) {
                // Get the entries for this data provider
                DataProviderEntries dpEntries = getDpEntries(traceEntry, xyDataProvider, currentEntries);

                List<TimeGraphEntry> entries = ContinuousDataProviderEntryProvider.getEntriesFromDataProvider(trace, parentTrace, xyDataProvider, dpEntries, this, monitor);
                for (TimeGraphEntry entry : entries) {
                    traceEntry.addChild(entry);
                }
            }
        }
        long start = getStartTime();
        long end = getEndTime();
        final long resolution = Long.max(1, (end - start) / getDisplayWidth());
        zoomEntries(getAllEntries(currentEntries), start, end, resolution, monitor);
        super.putEntryList(trace, Collections.singletonList(traceEntry));
        if (parentTrace.equals(getTrace())) {
            synchingToTime(getTimeGraphViewer().getSelectionBegin());
            refresh();
        }
    }

    private static Iterable<TimeGraphEntry> getAllEntries(Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries) {
        List<TimeGraphEntry> list = new ArrayList<>();
        for (DataProviderEntries entries : currentEntries.values()) {
            list.addAll(entries.getEntries().values());
        }
        return list;
    }

    private DataProviderEntries getDpEntries(TraceEntry traceEntry, ITmfTreeDataProvider<?> dataProvider, Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries) {
        // See if entries already exist for this data provider
        for (DPEntry dpEntry : currentEntries.row(traceEntry).keySet()) {
            if (dpEntry.getProvider().equals(dataProvider)) {
                DataProviderEntries dpEntries = currentEntries.get(traceEntry, dpEntry);
                if (dpEntries == null) {
                    dpEntries = new DataProviderEntries(dpEntry);
                    currentEntries.put(traceEntry, dpEntry, dpEntries);
                }
                return dpEntries;
            }
        }
        // Create one
        DPEntry dpEntry = new DPEntry(dataProvider);
        traceEntry.addChild(dpEntry);
        DataProviderEntries dpEntries = new DataProviderEntries(dpEntry);
        currentEntries.put(traceEntry, dpEntry, dpEntries);
        return dpEntries;
    }

    private Table<TraceEntry, DPEntry, DataProviderEntries> getCurrentEntriesForTrace(ITmfTrace parentTrace) {
        Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries = fCurrentEntries;
        if (currentEntries == null) {
            currentEntries = HashBasedTable.create();
            fCurrentEntries = currentEntries;
            fEntriesMap.put(parentTrace, currentEntries);
        }
        return currentEntries;
    }

    /**
     * Class to encapsulate a {@link TimeGraphEntryModel} for the trace level
     * and the relevant data provider
     *
     * @author Loic Prieur-Drevon
     * @since 3.3
     */
    protected static class TraceEntry extends TimeGraphEntry {
        private final @NonNull ITmfTrace fTrace;

        /**
         * Constructor
         *
         * @param trace
         *            The trace corresponding to this trace entry.
         */
        public TraceEntry(@NonNull ITmfTrace trace) {
            super(trace.getName(), trace.getStartTime().toNanos(), trace.getEndTime().toNanos());
            fTrace = trace;
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }

        /**
         * Getter for this trace entry's trace
         *
         * @return the trace for this trace entry and its children
         */
        public @NonNull ITmfTrace getTrace() {
            return fTrace;
        }

        @Override
        public String getName() {
            return fTrace.getName();
        }

    }

    /**
     * Get the {@link ITmfTrace} from a {@link TimeGraphEntry}'s parent.
     *
     * @param entry
     *            queried {@link TimeGraphEntry}.
     * @return the {@link ITmfTrace}
     */
    public static @NonNull ITmfTrace getTrace(TimeGraphEntry entry) {
        return getTraceEntry(entry).getTrace();
    }

    /**
     * Get the {@link ITimeGraphDataProvider} from a {@link TimeGraphEntry}'s
     * parent.
     *
     * @param entry
     *            queried {@link TimeGraphEntry}.
     * @return the {@link ITimeGraphDataProvider}
     * @since 3.3
     */
    public static ITmfTreeDataProvider<?> getProvider(TimeGraphEntry entry) {
        ITimeGraphEntry parent = entry;
        while (parent != null) {
            if (parent instanceof DPEntry) {
                return ((DPEntry) parent).getProvider();
            }
            parent = parent.getParent();
        }
        throw new IllegalStateException(entry + " should have a TraceEntry parent"); //$NON-NLS-1$
    }

    private static DataProviderEntries getProviderEntry(TimeGraphEntry entry, Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries) {
        ITimeGraphEntry parent = entry;
        while (parent != null) {
            if (parent instanceof DPEntry) {
                TraceEntry traceEntry = getTraceEntry(parent);
                return Objects.requireNonNull(currentEntries.get(traceEntry, parent));
            }
            parent = parent.getParent();
        }
        throw new IllegalStateException(entry + " should have a TraceEntry parent"); //$NON-NLS-1$
    }


    private static TraceEntry getTraceEntry(ITimeGraphEntry entry) {
        ITimeGraphEntry parent = entry;
        while (parent != null) {
            if (parent instanceof TraceEntry) {
                return ((TraceEntry) parent);
            }
            parent = parent.getParent();
        }
        throw new IllegalStateException(entry + " should have a TraceEntry parent"); //$NON-NLS-1$
    }

    private TraceEntry getTraceEntry(ITmfTrace trace, ITmfTrace parentTrace, Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries) {
        for (TraceEntry entry : currentEntries.rowKeySet()) {
            if (entry.getTrace().equals(trace)) {
                return entry;
            }
        }
        TraceEntry uiEntry = new TraceEntry(trace);
        addToEntryList(parentTrace, Collections.singletonList(uiEntry));
        return uiEntry;
    }

    @Override
    protected void zoomEntries(@NonNull Iterable<@NonNull TimeGraphEntry> entries, long zoomStartTime, long zoomEndTime, long resolution, @NonNull IProgressMonitor monitor) {
        super.zoomEntries(entries, zoomStartTime, zoomEndTime, resolution, monitor);
        Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries = fCurrentEntries;
        if (currentEntries == null) {
            // View not initialized yet, exit
            return;
        }
        if (resolution < 0) {
            // StateSystemUtils.getTimes would throw an illegal argument
            // exception.
            return;
        }

        long start = Long.min(zoomStartTime, zoomEndTime);
        long end = Long.max(zoomStartTime, zoomEndTime);
        List<@NonNull Long> times = StateSystemUtils.getTimes(start, end, resolution);
        Sampling sampling = new Sampling(start, end, resolution);
        Multimap<DataProviderEntries, Long> providersToModelIds = filterGroupEntries(entries, zoomStartTime, zoomEndTime);
        SubMonitor subMonitor = SubMonitor.convert(monitor, getClass().getSimpleName() + "#zoomEntries(IO)", providersToModelIds.size()); //$NON-NLS-1$

        for (Entry<DataProviderEntries, Collection<Long>> entry : providersToModelIds.asMap().entrySet()) {
            // Blind cast
            DataProviderEntries key = entry.getKey();
            ITmfTreeDataProvider<?> provider = key.getDpEntry().getProvider();
            if (provider instanceof AbstractTreeCommonXDataProvider) {
                AbstractTreeCommonXDataProvider<@NonNull TmfStateSystemAnalysisModule, @NonNull ITmfTreeDataModel> dataProvider = (AbstractTreeCommonXDataProvider<@NonNull TmfStateSystemAnalysisModule, @NonNull ITmfTreeDataModel>) provider;
                SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(times, entry.getValue());
                TmfModelResponse<@NonNull ITmfXyModel> response = dataProvider.fetchXY(filter, monitor);

                ITmfXyModel model = response.getModel();
                if (model != null) {
                    zoomEntries(key.getEntries(), model, response.getStatus() == ITmfResponse.Status.COMPLETED, sampling);
                }
                subMonitor.worked(1);
            } else if (provider instanceof ITimeGraphDataProvider) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider = (ITimeGraphDataProvider<? extends TimeGraphEntryModel>) provider;
                SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(times, entry.getValue());
                TmfModelResponse<@NonNull List<ITimeGraphRowModel>> response = dataProvider.fetchRowModel(filter, monitor);

                List<ITimeGraphRowModel> model = response.getModel();
                if (model != null) {
                    zoomEntries(key.getEntries(), model, response.getStatus() == ITmfResponse.Status.COMPLETED, sampling);
                }
                subMonitor.worked(1);

            }
        }
    }

    private void zoomEntries(Map<Long, TimeGraphEntry> map, List<ITimeGraphRowModel> model, boolean completed, Sampling sampling) {
        boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
        for (ITimeGraphRowModel rowModel : model) {
            TimeGraphEntry entry = map.get(rowModel.getEntryID());

            if (entry != null) {
                List<ITimeEvent> events = createTimeEvents(entry, rowModel.getStates());
                if (isZoomThread) {
                    applyResults(() -> {
                        entry.setZoomedEventList(events);
                        if (completed) {
                            entry.setSampling(sampling);
                        }
                    });
                } else {
                    entry.setEventList(events);
                }
            }
        }
    }

    private void zoomEntries(Map<Long, TimeGraphEntry> map, ITmfXyModel model, boolean completed, Sampling sampling) {
        boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
        for (TimeGraphEntry entry : map.values()) {
            String uniqueName = getUniqueName(entry);
            ISeriesModel rowModel = model.getData().get(uniqueName);
            if (rowModel == null) {
                continue;
            }
            List<ITimeEvent> events = createTimeEvents(entry, rowModel);
            if (isZoomThread) {
                applyResults(() -> {
                    entry.setZoomedEventList(events);
                    if (completed) {
                        entry.setSampling(sampling);
                    }
                });
            } else {
                entry.setEventList(events);
            }
        }
    }

    private static String getUniqueName(TimeGraphEntry entry) {
        List<String> names = new ArrayList<>();
        TimeGraphEntry current = entry;
        names.add(current.getName());
        while (current.getParent() != null) {
            current = current.getParent();
            if (!(current instanceof DPEntry)) {
                names.add(current.getName());
            }
        }
        StringJoiner stringJoiner = new StringJoiner(File.separator);
        for (int i = names.size() - 1; i >= 0; i--) {
            stringJoiner.add(names.get(i));
        }
        return stringJoiner.toString();
    }

    private static List<ITimeEvent> createTimeEvents(TimeGraphEntry entry, ISeriesModel rowModel) {
        List<ITimeEvent> timeEvents = new ArrayList<>();
        long[] xAxis = rowModel.getXAxis();
        for (int index = 0; index < xAxis.length; index++) {
            Long yValue = (long) rowModel.getData()[index];
            timeEvents.add(new TimeLineEvent(entry, xAxis[index], Collections.singletonList(yValue)));
        }
        return timeEvents;
    }

    private List<ITimeEvent> createTimeEvents(TimeGraphEntry entry, List<ITimeGraphState> values) {
        List<ITimeEvent> events = new ArrayList<>(values.size());
        ITimeEvent prev = null;
        for (ITimeGraphState state : values) {
            ITimeEvent event = createTimeEvent(entry, state);
            if (prev != null) {
                long prevEnd = prev.getTime() + prev.getDuration();
                if (prevEnd < event.getTime() && (getTimeEventFilterDialog() == null || !getTimeEventFilterDialog().hasActiveSavedFilters())) {
                    // fill in the gap.
                    TimeEvent timeEvent = new TimeEvent(entry, prevEnd, event.getTime() - prevEnd);
                    if (getTimeEventFilterDialog() != null && getTimeEventFilterDialog().isFilterActive()) {
                        timeEvent.setProperty(IFilterProperty.DIMMED, true);
                    }
                    events.add(timeEvent);
                }
            }
            prev = event;
            events.add(event);
        }
        return events;
    }

    /**
     * Create a {@link TimeEvent} for a {@link TimeGraphEntry} and a
     * {@link TimeGraphState}
     *
     * @param entry
     *            {@link TimeGraphEntry} for which we create a state
     * @param state
     *            {@link ITimeGraphState} from the data provider
     * @return a new {@link TimeEvent} for these arguments
     *
     * @since 3.3
     */
    protected TimeEvent createTimeEvent(TimeGraphEntry entry, ITimeGraphState state) {
        if (state.getValue() == Integer.MIN_VALUE) {
            return new NullTimeEvent(entry, state.getStartTime(), state.getDuration());
        }
        String label = state.getLabel();
        if (label != null) {
            return new NamedTimeEvent(entry, state.getStartTime(), state.getDuration(), state.getValue(), label, state.getActiveProperties());
        }
        return new TimeEvent(entry, state.getStartTime(), state.getDuration(), state.getValue(), state.getActiveProperties());
    }

    private Multimap<DataProviderEntries, Long> filterGroupEntries(Iterable<TimeGraphEntry> visible,
            long zoomStartTime, long zoomEndTime) {
        Multimap<DataProviderEntries, Long> providersToModelIds = HashMultimap.create();
        Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries = fCurrentEntries;
        if (currentEntries == null) {
            // View not initialized yet, exit
            return providersToModelIds;
        }
        for (TimeGraphEntry entry : visible) {
            if (zoomStartTime <= entry.getEndTime() && zoomEndTime >= entry.getStartTime() && entry.hasTimeEvents()) {
                long id = entry.getModel().getId();
                DataProviderEntries providerEntry = getProviderEntry(entry, currentEntries);
                providersToModelIds.put(providerEntry, id);
            }
        }
        return providersToModelIds;
    }

    @Override
    protected @NonNull List<@NonNull ILinkEvent> getLinkList(long zoomStartTime, long zoomEndTime, long resolution,
            @NonNull IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        List<@NonNull TimeGraphEntry> traceEntries = getEntryList(trace);
        if (trace == null || traceEntries == null) {
            return Collections.emptyList();
        }
        List<@NonNull ILinkEvent> linkList = new ArrayList<>();
        List<@NonNull Long> times = StateSystemUtils.getTimes(zoomStartTime, zoomEndTime, resolution);
        TimeQueryFilter queryFilter = new TimeQueryFilter(times);

        Table<TraceEntry, DPEntry, DataProviderEntries> currentEntries = getCurrentEntriesForTrace(trace);
        for (DataProviderEntries key : currentEntries.values()) {
            ITmfTreeDataProvider<?> provider = key.getDpEntry().getProvider();
            if (provider instanceof ITimeGraphDataProvider) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider = (ITimeGraphDataProvider<? extends TimeGraphEntryModel>) provider;
                TmfModelResponse<List<ITimeGraphArrow>> response = dataProvider.fetchArrows(queryFilter, monitor);
                List<ITimeGraphArrow> model = response.getModel();

                if (model != null) {
                    for (ITimeGraphArrow arrow : model) {
                        ITimeGraphEntry prevEntry = key.get(arrow.getSourceId());
                        ITimeGraphEntry nextEntry = key.get(arrow.getDestinationId());
                        if (prevEntry != null && nextEntry != null) {
                            linkList.add(new TimeLinkEvent(prevEntry, nextEntry, arrow.getStartTime(), arrow.getDuration(), arrow.getValue()));
                        }
                    }
                }
            }
        }
        return linkList;
    }

    /*
     * Helpers
     */

    /**
     * Central method to get IDs. Should be package-private since it is used by
     * other classes from this package
     *
     * @return A new ID for an entry
     */
    static long getId() {
        return fIds.incrementAndGet();
    }

    public void clampTimes(long startTime, long endTime) {
        setStartTime(Long.min(getStartTime(), startTime));
        setEndTime(Long.max(getEndTime(), endTime));
    }

    public void alignTime() {
        synchingToTime(getTimeGraphViewer().getSelectionBegin());
        refresh();
    }

    /*
     * Make these visible
     */
    @Override
    public long getStartTime() {
        return super.getStartTime();
    }

    @Override
    public long getEndTime() {
        return super.getEndTime();
    }

    @Override
    public int getDisplayWidth() {
        return super.getDisplayWidth();
    }

    public Object getLock() {
        return fLock;
    }

    @Override
    protected void loadingTrace(ITmfTrace trace) {
        System.out.println("Loading trace " + trace.getName());
        super.loadingTrace(trace);
        /*
         * Set the current entries to the one for the trace if available. Do not
         * initialize at this point, the build entry will do it
         */
        fCurrentEntries = fEntriesMap.get(trace);
    }

    @Override
    protected void resetView(@Nullable ITmfTrace viewTrace) {
        super.resetView(viewTrace);
        // Remove the trace's entry table and set the current entries to null
        if (viewTrace != null) {
            fEntriesMap.remove(viewTrace);
        }
    }

}
