package manifolds.discrete.implemention;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import manifolds.discrete.GraphNode;

/**
 * A simple immutable implementation of GraphNode.
 *
 * Identity is defined by (layerIndex, localNodeId).
 * Global node ID is computed on demand per the interface contract.
 * Attributes are defensively copied on construction.
 */
public class SimpleGraphNode implements GraphNode {

    private final int layerIndex;
    private final String localNodeId;
    private final Map<String, Object> attributes;

    public SimpleGraphNode(int layerIndex, String localNodeId) {
        this(layerIndex, localNodeId, Collections.emptyMap());
    }

    public SimpleGraphNode(int layerIndex, String localNodeId, Map<String, Object> attributes) {
        if (layerIndex < 0) throw new IllegalArgumentException("layerIndex must be >= 0");
        if (localNodeId == null || localNodeId.isBlank())
            throw new IllegalArgumentException("localNodeId must not be blank");
        this.layerIndex  = layerIndex;
        this.localNodeId = localNodeId;
        this.attributes  = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    @Override
    public int getLayerIndex() {
        return layerIndex;
    }

    @Override
    public String getLocalNodeId() {
        return localNodeId;
    }

    // getGlobalNodeId() is inherited from the interface default method:
    //   layerIndex + ":" + localNodeId

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "SimpleGraphNode{globalId=" + getGlobalNodeId() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GraphNode)) return false;
        GraphNode other = (GraphNode) o;
        return layerIndex == other.getLayerIndex()
            && localNodeId.equals(other.getLocalNodeId());
    }

    @Override
    public int hashCode() {
        return 31 * layerIndex + localNodeId.hashCode();
    }
}