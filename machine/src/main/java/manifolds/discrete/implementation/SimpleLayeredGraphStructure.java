package manifolds.discrete.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import manifolds.discrete.GraphEdge;
import manifolds.discrete.GraphNode;
import manifolds.discrete.LayeredGraphStructure;

/**
 * A concrete implementation of LayeredGraphStructure.
 *
 * Layers are keyed by integer index. Each layer holds its own nodes and
 * within-layer edges. Cross-layer edges (including same-layer "shortcut"
 * edges) are managed centrally by this structure.
 *
 * Thread safety: this implementation is NOT thread-safe. Synchronize
 * externally if concurrent modification is required.
 *
 * Global node ID format: "layerIndex:localNodeId"
 */
public class SimpleLayeredGraphStructure<N extends GraphNode, E extends GraphEdge>
        implements LayeredGraphStructure<N, E> {

    // Layer index -> list of nodes in that layer
    private final Map<Integer, List<N>> layerNodes = new HashMap<>();

    // Layer index -> list of within-layer edge records
    private final Map<Integer, List<WithinLayerEdgeRecord<E>>> withinLayerEdges = new HashMap<>();

    // All cross-layer edge records (including same-layer cross edges)
    private final List<CrossLayerEdgeRecord<E>> crossLayerEdges = new ArrayList<>();

    // Global node ID -> node (for fast lookup)
    private final Map<String, N> globalNodeIndex = new HashMap<>();


    // ----------------------------------------------------------------
    // Inner record types
    // ----------------------------------------------------------------

    private static class WithinLayerEdgeRecord<E> {
        final E edge;
        final String sourceLocalId;
        final String targetLocalId;

        WithinLayerEdgeRecord(E edge, String sourceLocalId, String targetLocalId) {
            this.edge          = edge;
            this.sourceLocalId = sourceLocalId;
            this.targetLocalId = targetLocalId;
        }
    }

    private static class CrossLayerEdgeRecord<E> {
        final E edge;
        final String sourceGlobalId;
        final String targetGlobalId;
        final Map<String, Object> constraints;

        CrossLayerEdgeRecord(E edge, String sourceGlobalId, String targetGlobalId,
                             Map<String, Object> constraints) {
            this.edge           = edge;
            this.sourceGlobalId = sourceGlobalId;
            this.targetGlobalId = targetGlobalId;
            this.constraints    = Collections.unmodifiableMap(new HashMap<>(constraints));
        }
    }


    // ----------------------------------------------------------------
    // Layer management
    // ----------------------------------------------------------------

    @Override
    public void addLayer(int layerIndex) {
        if (layerIndex < 0) throw new IllegalArgumentException("layerIndex must be >= 0");
        layerNodes.putIfAbsent(layerIndex, new ArrayList<>());
        withinLayerEdges.putIfAbsent(layerIndex, new ArrayList<>());
    }

    @Override
    public boolean hasLayer(int layerIndex) {
        return layerNodes.containsKey(layerIndex);
    }

    @Override
    public int getLayerCount() {
        return layerNodes.size();
    }


    // ----------------------------------------------------------------
    // Node management
    // ----------------------------------------------------------------

    @Override
    public void addNode(int layerIndex, N node, String localNodeId) {
        requireLayer(layerIndex);
        if (localNodeId == null || localNodeId.isBlank())
            throw new IllegalArgumentException("localNodeId must not be blank");

        String globalId = buildGlobalId(layerIndex, localNodeId);
        if (globalNodeIndex.containsKey(globalId))
            throw new IllegalStateException("Node already exists: " + globalId);

        layerNodes.get(layerIndex).add(node);
        globalNodeIndex.put(globalId, node);
    }

    @Override
    public N getNode(int layerIndex, String localNodeId) {
        String globalId = buildGlobalId(layerIndex, localNodeId);
        N node = globalNodeIndex.get(globalId);
        if (node == null)
            throw new IllegalArgumentException("No node found: " + globalId);
        return node;
    }

    @Override
    public List<N> getNodesInLayer(int layerIndex) {
        requireLayer(layerIndex);
        return Collections.unmodifiableList(layerNodes.get(layerIndex));
    }


    // ----------------------------------------------------------------
    // Node ID translation
    // ----------------------------------------------------------------

    @Override
    public String getGlobalNodeId(int layerIndex, String localNodeId) {
        return buildGlobalId(layerIndex, localNodeId);
    }

    @Override
    public int getLayerIndexFromGlobalId(String globalNodeId) {
        return Integer.parseInt(splitGlobalId(globalNodeId)[0]);
    }

    @Override
    public String getLocalNodeIdFromGlobalId(String globalNodeId) {
        return splitGlobalId(globalNodeId)[1];
    }


    // ----------------------------------------------------------------
    // Within-layer edges
    // ----------------------------------------------------------------

    @Override
    public void addWithinLayerEdge(int layerIndex, E edge,
                                   String sourceLocalId, String targetLocalId) {
        requireLayer(layerIndex);
        requireNodeInLayer(layerIndex, sourceLocalId);
        requireNodeInLayer(layerIndex, targetLocalId);
        withinLayerEdges.get(layerIndex)
            .add(new WithinLayerEdgeRecord<>(edge, sourceLocalId, targetLocalId));
    }

    @Override
    public List<E> getWithinLayerEdges(int layerIndex, String sourceLocalId) {
        requireLayer(layerIndex);
        return withinLayerEdges.get(layerIndex).stream()
            .filter(r -> r.sourceLocalId.equals(sourceLocalId))
            .map(r -> r.edge)
            .collect(Collectors.toUnmodifiableList());
    }


    // ----------------------------------------------------------------
    // Cross-layer edges
    // ----------------------------------------------------------------

    @Override
    public void addCrossLayerEdge(E edge, String sourceGlobalId,
                                  String targetGlobalId,
                                  Map<String, Object> constraints) {
        requireGlobalNode(sourceGlobalId);
        requireGlobalNode(targetGlobalId);
        crossLayerEdges.add(new CrossLayerEdgeRecord<>(
            edge, sourceGlobalId, targetGlobalId,
            constraints != null ? constraints : Collections.emptyMap()
        ));
    }

    @Override
    public List<E> getCrossLayerEdgesByLayerPair(int sourceLayer, int targetLayer) {
        return crossLayerEdges.stream()
            .filter(r -> {
                int src = getLayerIndexFromGlobalId(r.sourceGlobalId);
                int tgt = getLayerIndexFromGlobalId(r.targetGlobalId);
                return (src == sourceLayer && tgt == targetLayer)
                    || (src == targetLayer && tgt == sourceLayer);
            })
            .map(r -> r.edge)
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<E> getCrossLayerEdgesBySourceNode(String sourceGlobalId) {
        requireGlobalNode(sourceGlobalId);
        return crossLayerEdges.stream()
            .filter(r -> r.sourceGlobalId.equals(sourceGlobalId))
            .map(r -> r.edge)
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<E> getCrossLayerEdgesByTargetNode(String targetGlobalId) {
        requireGlobalNode(targetGlobalId);
        return crossLayerEdges.stream()
            .filter(r -> r.targetGlobalId.equals(targetGlobalId))
            .map(r -> r.edge)
            .collect(Collectors.toUnmodifiableList());
    }


    // ----------------------------------------------------------------
    // Combined queries
    // ----------------------------------------------------------------

    @Override
    public List<E> getAllEdgesFromNode(String globalNodeId) {
        requireGlobalNode(globalNodeId);
        int layerIndex   = getLayerIndexFromGlobalId(globalNodeId);
        String localId   = getLocalNodeIdFromGlobalId(globalNodeId);

        List<E> result = new ArrayList<>();
        result.addAll(getWithinLayerEdges(layerIndex, localId));
        result.addAll(getCrossLayerEdgesBySourceNode(globalNodeId));
        return Collections.unmodifiableList(result);
    }


    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private String buildGlobalId(int layerIndex, String localNodeId) {
        return layerIndex + ":" + localNodeId;
    }

    private String[] splitGlobalId(String globalNodeId) {
        String[] parts = globalNodeId.split(":", 2);
        if (parts.length != 2)
            throw new IllegalArgumentException("Invalid global node ID: " + globalNodeId);
        return parts;
    }

    private void requireLayer(int layerIndex) {
        if (!hasLayer(layerIndex))
            throw new IllegalArgumentException("Layer does not exist: " + layerIndex);
    }

    private void requireNodeInLayer(int layerIndex, String localNodeId) {
        String globalId = buildGlobalId(layerIndex, localNodeId);
        if (!globalNodeIndex.containsKey(globalId))
            throw new IllegalArgumentException("Node not found: " + globalId);
    }

    private void requireGlobalNode(String globalNodeId) {
        if (!globalNodeIndex.containsKey(globalNodeId))
            throw new IllegalArgumentException("Node not found: " + globalNodeId);
    }
}