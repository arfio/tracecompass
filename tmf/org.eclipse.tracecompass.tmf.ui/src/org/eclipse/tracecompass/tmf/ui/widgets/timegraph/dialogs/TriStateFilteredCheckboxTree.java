/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A <code>FilteredTree</code> whose checkboxes support three visual states:
 * checked, grayed and empty.
 *
 * @author Mikael Ferland
 * @since 3.2
 */
public class TriStateFilteredCheckboxTree extends FilteredCheckboxTree {

    /**
     * Set containing only the tree items that are grayed
     */
    private Set<Object> fGrayedObjects = new HashSet<>();
    private List<IPreCheckStateListener> fPreCheckStateListeners = new ArrayList<>();

    /**
     * Create a new instance of the receiver.
     *
     * @param parent
     *            the parent <code>Composite</code>
     * @param treeStyle
     *            the style bits for the <code>Tree</code>
     * @param filter
     *            the filter to be used
     * @param useNewLook
     *            <code>true</code> if the new <code>FilteredTree</code> look should
     *            be used
     * @deprecated use {@link #TriStateFilteredCheckboxTree(Composite, int, PatternFilter, boolean, boolean)}
     * @since 3.1
     */
    @Deprecated
    public TriStateFilteredCheckboxTree(Composite parent, int treeStyle, PatternFilter filter, boolean useNewLook) {
        this(parent, treeStyle, filter, useNewLook, false);
    }

    /**
     * Create a new instance of the receiver.
     *
     * @param parent
     *            the parent <code>Composite</code>
     * @param treeStyle
     *            the style bits for the <code>Tree</code>
     * @param filter
     *            the filter to be used
     * @param useNewLook
     *            <code>true</code> if the new <code>FilteredTree</code> look should
     *            be used
     * @param useFastHashLookup true, if tree should use fast hash lookup, else false
     * @since 6.0
     */
    public TriStateFilteredCheckboxTree(Composite parent, int treeStyle, PatternFilter filter, boolean useNewLook, boolean useFastHashLookup) {
        super(parent, treeStyle, filter, useNewLook, useFastHashLookup);
    }

    @Override
    public void setCheckedElements(Object[] elements) {
        super.setCheckedElements(elements);
        maintainAllCheckIntegrity();
    }

    @Override
    public boolean setSubtreeChecked(Object element, boolean state) {
        Set<Object> prevChecked = new HashSet<>(Arrays.asList(getCheckedElements()));
        if (state) {
            prevChecked.remove(element);
        } else {
            prevChecked.add(element);
        }
        for (IPreCheckStateListener preCheckStateListener : fPreCheckStateListeners) {
            if (preCheckStateListener != null && preCheckStateListener.setSubtreeChecked(element, state)) {
                // revert situation
                setCheckedElements(prevChecked.toArray());
                return false;
            }
        }
        checkSubtree(element, state);
        return getCheckboxTreeViewer().setSubtreeChecked(element, state);
    }

    @Override
    protected TreeViewer doCreateTreeViewer(Composite parentComposite, int style) {
        TreeViewer tree = super.doCreateTreeViewer(parentComposite, style);
        if (tree instanceof CheckboxTreeViewer) {
            CheckboxTreeViewer checkboxTree = (CheckboxTreeViewer) tree;
            checkboxTree.addCheckStateListener(event -> setSubtreeChecked(event.getElement(), event.getChecked()));
        }
        return tree;
    }

    @Override
    protected WorkbenchJob doCreateRefreshJob() {
        WorkbenchJob job = super.doCreateRefreshJob();
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                if (getCheckboxTreeViewer().getTree().isDisposed()) {
                    return;
                }
                maintainAllCheckIntegrity();
            }
        });
        return job;
    }

    @Override
    protected void checkSubtree(Object element, boolean state) {
        CheckboxTreeViewer checkboxTreeViewer = getCheckboxTreeViewer();
        if (checkboxTreeViewer.testFindItem(element) != null) {
            if (state) {
                fCheckedObjects.add(element);
            } else {
                fCheckedObjects.remove(element);
            }
        }
        boolean expanded = checkboxTreeViewer.getExpandedState(element);
        /* make sure element is expanded so that testFindItem will find the children */
        checkboxTreeViewer.setExpandedState(element, true);
        for (Object o : ((ITreeContentProvider) checkboxTreeViewer.getContentProvider()).getChildren(element)) {
            checkSubtree(o, state);
        }
        checkboxTreeViewer.setExpandedState(element, expanded);
        maintainAllCheckIntegrity();
    }

    /**
     * Returns the grayed state of the given element.
     */
    private boolean getGrayed(Object element) {
        return fGrayedObjects.contains(element);
    }

    /**
     * Sets the grayed state for the given element in this viewer.
     */
    private boolean setGrayed(Object element, boolean state) {
        boolean checkable = getCheckboxTreeViewer().setGrayed(element, state);
        if (!state) {
            fGrayedObjects.remove(element);
        } else if (checkable) {
            fGrayedObjects.add(element);
        }
        return checkable;
    }

    /**
     * Ensure that the state of the checkbox and its parents are correct.
     *
     * TODO: Create utils method for use in other checkboxes.
     *
     * @param element
     *            Tree element from which the verification needs to be made
     */
    private void maintainCheckIntegrity(final Object element) {
        CheckboxTreeViewer checkboxTreeViewer = getCheckboxTreeViewer();
        ITreeContentProvider contentProvider = (ITreeContentProvider) checkboxTreeViewer.getContentProvider();
        boolean allChecked = true;
        boolean oneChecked = false;
        boolean oneGrayed = false;

        for (Object child : contentProvider.getChildren(element)) {
            if (checkboxTreeViewer.testFindItem(child) == null) {
                continue;
            }

            boolean checked = getChecked(child);
            oneChecked |= checked;
            allChecked &= checked;
            oneGrayed |= (checked && getGrayed(child));

            if (oneGrayed || (oneChecked && !allChecked)) {
                setGrayed(element, true);
                setChecked(element, true);
            } else {
                setGrayed(element, false);
                setChecked(element, allChecked);
            }
        }

        Object parentElement = contentProvider.getParent(element);
        if (parentElement != null) {
            maintainCheckIntegrity(parentElement);
        }
    }

    private void maintainAllCheckIntegrity() {
        for (Object checkedElement : getCheckedElements()) {
            maintainCheckIntegrity(checkedElement);
        }
    }

    @Override
    public void setFilterText(String string) {
        // make public
        super.setFilterText(string);
    }

    /**
     * Set the listener for actions to execute before checking the tree.
     *
     * @param listener
     *            pre-check state listener.
     * @since 4.0
     */
    public void addPreCheckStateListener(IPreCheckStateListener listener) {
        fPreCheckStateListeners.add(listener);
    }

}
