package org.eclipse.tracecompass.analysis.graph.core.graph;

/**
 * @author arnaud
 * @since 3.2
 *
 */
public interface IEdgeContextStateFactory {
    public abstract ITmfEdgeContextState createContextState(int code);
}
