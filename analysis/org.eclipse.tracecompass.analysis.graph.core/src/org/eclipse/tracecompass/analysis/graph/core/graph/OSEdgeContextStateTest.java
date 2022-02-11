package org.eclipse.tracecompass.analysis.graph.core.graph;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author arnaud
 * @since 3.2
 *
 */
public class OSEdgeContextStateTest implements ITmfEdgeContextState {

    private OSEdgeContextEnum fContextState = OSEdgeContextEnum.DEFAULT;
    private static EnumMap<OSEdgeContextEnum, Map<String, Object>> fStyles;
    static {
        fStyles = new EnumMap<>(OSEdgeContextEnum.class);

    }

    enum OSEdgeContextEnum {
        EPS(0), NO_EDGE(1), UNKNOWN(2), DEFAULT(3), RUNNING(4), BLOCKED(5);

        private int code;
        private static Map<Integer, OSEdgeContextEnum> fMap;
        static {
            fMap = new HashMap<>();
            fMap.put(0, EPS);
            fMap.put(1, NO_EDGE);
            fMap.put(2, UNKNOWN);
            fMap.put(3, DEFAULT);
            fMap.put(4, RUNNING);
            fMap.put(4, BLOCKED);
        }

        OSEdgeContextEnum(int code) {
            this.code = code;
        }

        public static OSEdgeContextEnum fromValue(int code) {
            return fMap.getOrDefault(code, UNKNOWN);
        }

        public int serialize() {
            return this.code;
        }
    }

    @Override
    public TmfEdgeState getEdgeState() {
        switch(fContextState) {
        case EPS:
        case NO_EDGE:
            return TmfEdgeState.STOP;
        case UNKNOWN:
        case DEFAULT:
        case RUNNING:
        case BLOCKED:
            return TmfEdgeState.PASS;
        default:
            return TmfEdgeState.UNKNOWN;
        }
    }

    @Override
    public boolean isMatchable() {
        return false;
    }

    @Override
    public Map<String, Object> getStyles() {
        return fStyles.getOrDefault(fContextState, fStyles.get(OSEdgeContextEnum.DEFAULT));
    }

    @Override
    public int serialize() {
        return fContextState.serialize();
    }

    @Override
    public Enum<?> deserialize(int code) {
        return OSEdgeContextEnum.fromValue(code);
    }

    @Override
    public void setContextEnum(Enum<?> cause) {
        fContextState = (OSEdgeContextEnum) cause;
    }

    @Override
    public Enum<?> getContextEnum() {
        return fContextState;
    }

}
