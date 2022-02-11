package org.eclipse.tracecompass.analysis.graph.core.graph;

/**
 * @author arnaud
 * @since 3.2
 *
 */
public enum TmfEdgeState {
    PASS(0),
    STOP(1),
    UNKNOWN(2);

    private final int code;

    TmfEdgeState(int i) {
        this.code = i;
    }

    public int serialize() {
        return code;
    }
}
