package manifolds.discrete;

import java.util.Map;

/**
 * A node in a discrete graph layer.
 *
 * Node identity is defined by:
 *
 *     (layerIndex, localNodeId)
 *
 * These two values are canonical and must uniquely identify a node
 * within the LayeredGraphStructure.
 *
 * The global node ID is derived from these values and must NOT be
 * stored independently. Implementations should compute it on demand
 * to avoid identity inconsistencies.
 */
public interface GraphNode {

    /**
     * The layer that contains this node.
     */
    int getLayerIndex();

    /**
     * The node identifier within its layer.
     * Must be unique within that layer.
     */
    String getLocalNodeId();

    /**
     * A globally unique identifier derived from
     * (layerIndex, localNodeId).
     *
     * Example format:
     *
     *     "layerIndex:localNodeId"
     *
     * Implementations should compute this value rather than store it.
     */
    default String getGlobalNodeId() {
        return getLayerIndex() + ":" + getLocalNodeId();
    }

    /**
     * Extensible metadata associated with the node.
     */
    Map<String, Object> getAttributes();
}