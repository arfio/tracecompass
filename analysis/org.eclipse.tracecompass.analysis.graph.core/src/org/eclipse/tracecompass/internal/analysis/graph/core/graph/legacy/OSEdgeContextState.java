package org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tracecompass.internal.analysis.graph.core.base.Messages;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.TmfEdgeState;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;

import com.google.common.collect.ImmutableMap;

/**
 * @author arnaud
 *
 */
public class OSEdgeContextState implements ITmfEdgeContextState {

    OSEdgeContextEnum fContextState = OSEdgeContextEnum.DEFAULT;
    private static EnumMap<OSEdgeContextEnum, Map<String, Object>> fStyles;
    static {
        fStyles = new EnumMap<>(OSEdgeContextEnum.class);
        fStyles.put(OSEdgeContextEnum.NO_EDGE,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Unknown),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x40, 0x3b, 0x33),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.DEFAULT,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Unknown),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x40, 0x3b, 0x33),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
    }

    public enum OSEdgeContextEnum {
        NO_EDGE(0), EPS(1), UNKNOWN(2), DEFAULT(3), RUNNING(4), BLOCKED(5), INTERRUPTED(6),
        PREEMPTED(7), TIMER(8), NETWORK(9), USER_INPUT(10), BLOCK_DEVICE(11), IPI(12);

        private int code;
        private static Map<Integer, OSEdgeContextEnum> fMap;
        static {
            fMap = new HashMap<>();
            fMap.put(0, NO_EDGE);
            fMap.put(1, EPS);
            fMap.put(2, UNKNOWN);
            fMap.put(3, DEFAULT);
            fMap.put(4, RUNNING);
            fMap.put(5, BLOCKED);
            fMap.put(6, INTERRUPTED);
            fMap.put(7, PREEMPTED);
            fMap.put(8, TIMER);
            fMap.put(9, NETWORK);
            fMap.put(10, USER_INPUT);
            fMap.put(11, BLOCK_DEVICE);
            fMap.put(12, IPI);
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

    public OSEdgeContextState(OSEdgeContextEnum type) {
        fContextState = type;
    }

    public OSEdgeContextState(int code) {
        fContextState = (OSEdgeContextEnum) deserialize(code);
    }

    public OSEdgeContextState(TmfEdge.EdgeType type) {
        switch (type) {
        case BLOCKED:
            fContextState = OSEdgeContextEnum.BLOCKED;
            break;
        case BLOCK_DEVICE:
            fContextState = OSEdgeContextEnum.BLOCK_DEVICE;
            break;
        case EPS:
            fContextState = OSEdgeContextEnum.EPS;
            break;
        case DEFAULT:
            fContextState = OSEdgeContextEnum.DEFAULT;
            break;
        case INTERRUPTED:
            fContextState = OSEdgeContextEnum.INTERRUPTED;
            break;
        case IPI:
            fContextState = OSEdgeContextEnum.IPI;
            break;
        case NETWORK:
            fContextState = OSEdgeContextEnum.NETWORK;
            break;
        case PREEMPTED:
            fContextState = OSEdgeContextEnum.PREEMPTED;
            break;
        case RUNNING:
            fContextState = OSEdgeContextEnum.RUNNING;
            break;
        case TIMER:
            fContextState = OSEdgeContextEnum.TIMER;
            break;
        case USER_INPUT:
            fContextState = OSEdgeContextEnum.USER_INPUT;
            break;
        case UNKNOWN:
        default:
            fContextState = OSEdgeContextEnum.UNKNOWN;
            break;
        }
    }

    public TmfEdge.EdgeType getOldEdgeType() {
        switch (fContextState) {
        case BLOCKED:
            return TmfEdge.EdgeType.BLOCKED;
        case BLOCK_DEVICE:
            return TmfEdge.EdgeType.BLOCK_DEVICE;
        case DEFAULT:
            return TmfEdge.EdgeType.DEFAULT;
        case EPS:
            return TmfEdge.EdgeType.EPS;
        case INTERRUPTED:
            return TmfEdge.EdgeType.INTERRUPTED;
        case IPI:
            return TmfEdge.EdgeType.IPI;
        case NETWORK:
            return TmfEdge.EdgeType.NETWORK;
        case NO_EDGE:
            return TmfEdge.EdgeType.DEFAULT;
        case PREEMPTED:
            return TmfEdge.EdgeType.PREEMPTED;
        case RUNNING:
            return TmfEdge.EdgeType.RUNNING;
        case TIMER:
            return TmfEdge.EdgeType.TIMER;
        case USER_INPUT:
            return TmfEdge.EdgeType.USER_INPUT;
        case UNKNOWN:
        default:
            return TmfEdge.EdgeType.UNKNOWN;
        }
    }

    @Override
    public TmfEdgeState getEdgeState() {
        switch (fContextState) {
        case IPI:
        case USER_INPUT:
        case BLOCK_DEVICE:
        case TIMER:
        case INTERRUPTED:
        case PREEMPTED:
        case RUNNING:
        case UNKNOWN:
        case NO_EDGE:
            return TmfEdgeState.PASS;
        case NETWORK:
        case BLOCKED:
            return TmfEdgeState.STOP;
        case EPS:
        case DEFAULT:
        default:
            return TmfEdgeState.UNKNOWN;
        }
    }
    @Override
    public boolean isMatchable() {
        return fContextState == OSEdgeContextEnum.NETWORK ? true : false;
    }

    @Override
    public Map<String, Object> getStyles() {
        return fStyles.getOrDefault(fContextState, fStyles.get(OSEdgeContextEnum.DEFAULT));
    }

    public static Map<String, Object> getStyles(OSEdgeContextEnum contextState) {
        return fStyles.getOrDefault(contextState, fStyles.get(OSEdgeContextEnum.DEFAULT));
    }

    @Override
    public void setContextEnum(Enum<?> contextState) {
        fContextState = (OSEdgeContextEnum) contextState;
    }

    @Override
    public Enum<?> getContextEnum() {
        return fContextState;
    }

    @Override
    public int serialize() {
        return fContextState.ordinal();
    }

    @Override
    public Enum<?> deserialize(int code) {
        return OSEdgeContextEnum.fromValue(code);
    }

}
