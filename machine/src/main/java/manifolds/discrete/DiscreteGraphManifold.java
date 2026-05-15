package manifolds.discrete;

import java.util.List;

import machine.ManifoldSpace;
import machine.Position;
import machine.Transition;

/**
 * A ManifoldSpace implemented as a discrete layered graph.
 *
 * Flatlanders flow along nodes and edges in a layered graph structure.
 * Within-layer movement follows edges in that layer; cross-layer jumps
 * use cross-layer edges.
 *
 * DiscreteGraphManifold wraps a LayeredGraphStructure and implements
 * ManifoldSpace on top of it. The underlying structure is not exposed.
 */
public interface DiscreteGraphManifold extends ManifoldSpace {
    // Discrete-specific queries
    int getLayerCount();
    List<GraphNode> getNodesInLayer(int layerIndex);
    
    /**
     * Returns all edges that exist within a single layer.
     */
    List<GraphEdge> getWithinLayerEdges(int layerIndex);
    
    /**
     * Returns all edges connecting sourceLayer to targetLayer.
     * Includes edges in both directions (sourceLayer -> targetLayer
     * and targetLayer -> sourceLayer).
     */
    List<GraphEdge> getCrossLayerEdges(int sourceLayer, int targetLayer);
    
    GraphEdge getGraphEdgeForTransition(Transition transition);
    GraphNode getGraphNodeForPosition(Position p);
}