package org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath;

import java.util.Map;

public abstract class AbstractMixedCriticalPathAlgorithm {

    abstract protected Map<Enum<?>, Enum<?>> matchContextState();

    // TODO: definir les classes implementant les differents critical path
    // TODO: avoir un graphe par classe.
    // TODO: IMPLEMENTER LA PARTIE RESOLVE
    // Joindre les graphes
    // match les etats partages ?
}
