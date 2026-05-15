package manifolds.discrete;

import java.util.List;
import java.util.Map;

/**
 * A layered graph structure where nodes exist inside layers and edges can
 * connect either nodes within the same layer or nodes across layers.
 *
 * Nodes are identified locally by (layerIndex, localNodeId).
 * A global node id uniquely identifies a node across the entire structure.
 *
 * @param <N> node type
 * @param <E> edge type
 */
public interface LayeredGraphStructure<N, E> {

    // ------------------------------------------------------------
    // Layer management
    // ------------------------------------------------------------

    void addLayer(int layerIndex);

    boolean hasLayer(int layerIndex);

    int getLayerCount();


    // ------------------------------------------------------------
    // Node management
    // ------------------------------------------------------------

    void addNode(int layerIndex, N node, String localNodeId);

    N getNode(int layerIndex, String localNodeId);

    List<N> getNodesInLayer(int layerIndex);


    // ------------------------------------------------------------
    // Node ID translation
    // ------------------------------------------------------------

    String getGlobalNodeId(int layerIndex, String localNodeId);

    int getLayerIndexFromGlobalId(String globalNodeId);

    String getLocalNodeIdFromGlobalId(String globalNodeId);


    // ------------------------------------------------------------
    // Within-layer edges
    // ------------------------------------------------------------

    void addWithinLayerEdge(
        int layerIndex,
        E edge,
        String sourceLocalId,
        String targetLocalId
    );

    List<E> getWithinLayerEdges(
        int layerIndex,
        String sourceLocalId
    );


    // ------------------------------------------------------------
    // Cross-layer edges
    // ------------------------------------------------------------

    void addCrossLayerEdge(
        E edge,
        String sourceGlobalId,
        String targetGlobalId,
        Map<String, Object> constraints
    );

    List<E> getCrossLayerEdgesByLayerPair(
        int sourceLayer,
        int targetLayer
    );

    List<E> getCrossLayerEdgesBySourceNode(
        String sourceGlobalId
    );

    List<E> getCrossLayerEdgesByTargetNode(
        String targetGlobalId
    );


    // ------------------------------------------------------------
    // Combined queries
    // ------------------------------------------------------------

    List<E> getAllEdgesFromNode(String globalNodeId);
}