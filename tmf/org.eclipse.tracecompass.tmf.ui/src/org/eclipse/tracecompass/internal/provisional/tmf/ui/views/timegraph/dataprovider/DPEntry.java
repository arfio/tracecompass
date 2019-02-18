/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph.dataprovider;

import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * Data Provider entry in the timegraph
 *
 * @author Matthew Khouzam
 */
final class DPEntry extends TimeGraphEntry {

    private final ITmfTreeDataProvider<?> fProvider;

    public DPEntry(ITmfTreeDataProvider<?> dataProvider, String label) {
        super(new TimeGraphEntryModel(DataProviderBaseView.getId(), -1, label, Long.MIN_VALUE, Long.MAX_VALUE, false));
        fProvider = dataProvider;
    }

    @Override
    public boolean hasTimeEvents() {
        return false;
    }

    public ITmfTreeDataProvider<?> getProvider() {
        return fProvider;
    }
}