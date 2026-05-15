package manifolds.discrete;

import java.util.Map;

/**
 * An edge in a discrete graph, either within a layer or across layers.
 *
 * An edge is a data container. Its source and target relationship is
 * owned by LayeredGraphStructure, not by the edge itself.
 *
 * Source and target are identified by global node IDs.
 */
public interface GraphEdge {
    /**
     * A unique identifier for this edge.
     */
    String getEdgeId();
    
    /**
     * Extensible metadata associated with the edge.
     */
    Map<String, Object> getAttributes();
}