/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph.dataprovider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph.dataprovider.DataProviderBaseView.DataProviderEntries;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * @author Matthew Khouzam
 * @author Geneviève Bastien
 */
public class DiscreteDataProviderEntryProvider {
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    /**
     * @param trace
     * @param parentTrace
     * @param dataProvider
     * @param dpEntries
     * @param view
     * @param monitor
     * @return
     */
    public static List<TimeGraphEntry> getEntriesFromDataProvider(ITmfTrace trace, ITmfTrace parentTrace, ITimeGraphDataProvider<TimeGraphEntryModel> dataProvider, DataProviderEntries dpEntries, DataProviderBaseView view,
            IProgressMonitor monitor) {
        boolean complete = false;
        List<TimeGraphEntry> newEntries = new ArrayList<>();
        while (!complete && !monitor.isCanceled()) {
            // Get the tree for the data provider
            TmfModelResponse<List<TimeGraphEntryModel>> response = dataProvider.fetchTree(new TimeQueryFilter(0, Long.MAX_VALUE, 2), monitor);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                Activator.getDefault().logError(view.getClass().getSimpleName() + " Data Provider failed: " + response.getStatusMessage()); //$NON-NLS-1$
                return newEntries;
            } else if (response.getStatus() == ITmfResponse.Status.CANCELLED) {
                return newEntries;
            }
            complete = response.getStatus() == ITmfResponse.Status.COMPLETED;

            List<TimeGraphEntryModel> model = response.getModel();
            if (model != null) {
                synchronized (view.getLock()) {
                    for (TimeGraphEntryModel entry : model) {
                        TimeGraphEntry uiEntry = dpEntries.get(entry.getId());
                        if (entry.getParentId() != -1) {
                            if (uiEntry == null) {
                                uiEntry = new TimeGraphEntry(entry);
                                TimeGraphEntry parent = dpEntries.get(entry.getParentId());
                                if (parent != null) {
                                    parent.addChild(uiEntry);
                                }
                                dpEntries.put(entry.getId(), uiEntry);
                            } else {
                                uiEntry.updateModel(entry);
                            }
                        } else {
                            view.clampTimes(entry.getStartTime(), entry.getEndTime() + 1);
                            if (uiEntry == null) {
                                // Use the data provider entry
                                dpEntries.put(entry.getId(), dpEntries.getDpEntry());
                            }
                        }
                    }
                }
                long start = view.getStartTime();
                long end = view.getEndTime();
                final long resolution = Long.max(1, (end - start) / view.getDisplayWidth());
                view.zoomEntries(dpEntries.getEntries().values(), start, end, resolution, monitor);
            }

            if (monitor.isCanceled()) {
                return newEntries;
            }

            if (parentTrace.equals(trace)) {
                view.alignTime();
            }
            monitor.worked(1);

            if (!complete && !monitor.isCanceled()) {
                try {
                    Thread.sleep(BUILD_UPDATE_TIMEOUT);
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Failed to wait for data provider", e); //$NON-NLS-1$
                    Thread.currentThread().interrupt();
                }
            }
        }
        return newEntries;
    }
}
