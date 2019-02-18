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
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph.dataprovider.DataProviderBaseView.DataProviderEntries;
import org.eclipse.tracecompass.internal.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.model.TimeGraphLineEntry;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * @author Matthew Khouzam
 * @author Geneviève Bastien
 */
public final class ContinuousDataProviderEntryProvider {
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    public static List<TimeGraphEntry> getEntriesFromDataProvider(ITmfTrace trace, ITmfTrace parentTrace, AbstractTreeCommonXDataProvider<TmfStateSystemAnalysisModule, ITmfTreeDataModel> dataProvider, DataProviderEntries dpEntries,
            DataProviderBaseView view, IProgressMonitor monitor) {
        boolean complete = false;
        List<TimeGraphEntry> newEntries = new ArrayList<>();
        while (!complete && !monitor.isCanceled()) {
            TmfModelResponse<@NonNull List<@NonNull ITmfTreeDataModel>> response = dataProvider.fetchTree(new TimeQueryFilter(0, Long.MAX_VALUE, 2), monitor);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                Activator.getDefault().logError(view.getClass().getSimpleName() + " Data Provider failed: " + response.getStatusMessage()); //$NON-NLS-1$
                return newEntries;
            } else if (response.getStatus() == ITmfResponse.Status.CANCELLED) {
                return newEntries;
            }
            complete = response.getStatus() == ITmfResponse.Status.COMPLETED;

            List<@NonNull ITmfTreeDataModel> model = response.getModel();
            if (model != null) {
                synchronized (view.getLock()) {
                    for (ITmfTreeDataModel entry : model) {
                        TimeGraphEntry uiEntry = dpEntries.get(entry.getId());
                        if (entry.getParentId() != -1) {
                            if (uiEntry == null) {
                                uiEntry = new TimeGraphLineEntry(convert(entry));
                                TimeGraphEntry parent = dpEntries.get(entry.getParentId());
                                if (parent != null) {
                                    parent.addChild(uiEntry);
                                }
                                dpEntries.put(entry.getId(), uiEntry);
                            } else {
                                if (ITimeGraphEntry.DisplayStyle.LINE.equals(uiEntry.getStyle())) {
                                    uiEntry.updateModel(convert(entry));
                                }
                            }
                        } else {
                            if (uiEntry == null) {
                                // Remove this entry and replace by the data provider entry
                                dpEntries.put(entry.getId(), dpEntries.getDpEntry());
                            }
                        }
                    }
                }
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

    private static @NonNull TimeGraphEntryModel convert(@NonNull ITmfTreeDataModel model) {
        return model instanceof TimeGraphEntryModel ? (TimeGraphEntryModel) model : new TimeGraphEntryModel(model.getId(), model.getParentId(), model.getName(), Long.MIN_VALUE, Long.MAX_VALUE, true);
    }
}
