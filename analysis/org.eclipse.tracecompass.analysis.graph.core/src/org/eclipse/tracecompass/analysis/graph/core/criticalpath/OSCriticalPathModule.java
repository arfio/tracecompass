package org.eclipse.tracecompass.analysis.graph.core.criticalpath;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.WorkerSerializer;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.HistoryTreeTmfGraph;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.OsHistoryTreeGraph;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author arnaud
 * @since 3.2
 *
 */
public class OSCriticalPathModule extends AbstractCriticalPathModule {

    public OSCriticalPathModule(AbstractTmfGraphBuilderModule graph) {
        super(graph);
    }

    /**
     * Constructor with the parameter. Can be used by benchmarks, to avoid that
     * setting the parameter causes the module to be schedule and run in a job
     * which keeps it in memory forever (and thus can cause OOME)
     *
     * @param graph
     *            The graph module that will be used to calculate the critical
     *            path on
     * @param worker
     *            The worker parameter to set
     * @since 3.1
     */
    @VisibleForTesting
    public OSCriticalPathModule(AbstractTmfGraphBuilderModule graph, IGraphWorker worker) {
        super(graph, worker);
    }

    @Override
    protected @Nullable ITmfGraph createGraphInstance(Path htFile, WorkerSerializer workerSerializer, long startTime, int version) {
        HistoryTreeTmfGraph graph;
        try {
            graph = new OsHistoryTreeGraph(htFile, version, workerSerializer, startTime);
        } catch (IOException e) {
            return null;
        }

        return graph;
    }

}
