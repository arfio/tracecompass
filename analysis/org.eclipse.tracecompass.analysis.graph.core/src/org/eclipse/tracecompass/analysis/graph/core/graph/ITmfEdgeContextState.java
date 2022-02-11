package org.eclipse.tracecompass.analysis.graph.core.graph;

import java.util.Map;

/**
 * @author arnaud
 * @since 3.2
 *
 */
public interface ITmfEdgeContextState {
    public TmfEdgeState getEdgeState();
    public Map<String, Object> getStyles();
    public void setContextEnum(Enum<?> contextState);
    public Enum<?> getContextEnum();
    public int serialize();
    public Enum<?> deserialize(int code);
    public boolean isMatchable();
}
