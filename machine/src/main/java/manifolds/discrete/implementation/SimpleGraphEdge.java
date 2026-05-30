package manifolds.discrete.implementation;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import manifolds.discrete.GraphEdge;

/**
 * A simple immutable implementation of GraphEdge.
 *
 * An edge is a pure data container. It carries no knowledge of its
 * source or target — that relationship is owned by LayeredGraphStructure.
 * It carries no cost, weight, or traversal metadata — those semantics
 * belong to DiscreteGraphManifold or the execution layer.
 *
 * Attributes are defensively copied on construction.
 */
public class SimpleGraphEdge implements GraphEdge {

    private final String edgeId;
    private final Map<String, Object> attributes;

    public SimpleGraphEdge(String edgeId) {
        this(edgeId, Collections.emptyMap());
    }

    public SimpleGraphEdge(String edgeId, Map<String, Object> attributes) {
        if (edgeId == null || edgeId.isBlank())
            throw new IllegalArgumentException("edgeId must not be blank");
        this.edgeId     = edgeId;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    @Override
    public String getEdgeId() {
        return edgeId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "SimpleGraphEdge{edgeId=" + edgeId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GraphEdge)) return false;
        GraphEdge other = (GraphEdge) o;
        return edgeId.equals(other.getEdgeId());
    }

    @Override
    public int hashCode() {
        return edgeId.hashCode();
    }
}