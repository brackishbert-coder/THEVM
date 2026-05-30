package manifolds.discrete.implementation;


import java.util.*;
import java.util.stream.Collectors;

import machine.CommonTransitionTypes;
import machine.CurvatureField;
import machine.ExecutionContext;
import machine.Flatlander;
import machine.ManifoldDescriptor;
import machine.ManifoldSpace;
import machine.MotionAdaptationResult;
import machine.MotionVector;
import machine.NeighborhoodQuery;
import machine.Position;
import machine.PositionDebug;
import machine.RegionCapabilities;
import machine.RegionPolicy;
import machine.Transition;
import machine.TransitionQuery;
import manifolds.discrete.DiscreteGraphManifold;
import manifolds.discrete.GraphEdge;
import manifolds.discrete.GraphNode;
import manifolds.discrete.LayeredGraphStructure;

/**
 * A concrete ManifoldSpace implemented as a discrete layered graph.
 *
 * Wraps a LayeredGraphStructure and implements ManifoldSpace semantics on top.
 * The underlying structure is not exposed — all access goes through ManifoldSpace
 * or DiscreteGraphManifold methods.
 *
 * HOLONOMY POLICY: APPROXIMATE
 * Discrete graph backends do not support true holonomy preservation.
 * parallelTransport() returns the input vector unchanged.
 * computeHolonomy() always returns Optional.empty().
 *
 * CURVATURE: Per-node, stored in node attributes under AttributeConventions keys.
 * Falls back to zero (flat) curvature if a node carries no curvature metadata.
 *
 * POLICY: Per-node, stored in node attributes. Falls back to a permissive default
 * policy if a node carries no policy metadata.
 */
public class SimpleDiscreteGraphManifold
        implements DiscreteGraphManifold {

    private static final String ATTR_CURVATURE_INTENSITY = "curvature_intensity";
    private static final String ATTR_REGION_POLICY       = "region_policy";

    private final LayeredGraphStructure<GraphNode, GraphEdge> graph;
    private final ManifoldDescriptor descriptor;
    private final RegionPolicy defaultPolicy;
    private final CurvatureField flatCurvature;

    public SimpleDiscreteGraphManifold(
            LayeredGraphStructure<GraphNode, GraphEdge> graph,
            ManifoldDescriptor descriptor) {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        this.graph       = graph;
        this.descriptor  = descriptor;
        this.defaultPolicy  = buildDefaultPolicy();
        this.flatCurvature  = buildFlatCurvature();
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — descriptor and structure
    // ----------------------------------------------------------------

    @Override
    public ManifoldDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Position origin() {
        // Origin is node "0" in layer 0, if it exists
        if (graph.hasLayer(0)) {
            List<GraphNode> nodes = graph.getNodesInLayer(0);
            if (!nodes.isEmpty()) {
                return nodeToPosition(nodes.get(0));
            }
        }
        return new DiscretePosition("0:origin", new double[]{0.0, 0.0});
    }

    @Override
    public boolean isValidPosition(Position p) {
        if (!(p instanceof DiscretePosition dp)) return false;
        String globalId = dp.getGlobalNodeId();
        int layerIndex  = graph.getLayerIndexFromGlobalId(globalId);
        String localId  = graph.getLocalNodeIdFromGlobalId(globalId);
        return graph.hasLayer(layerIndex) && graph.getNode(layerIndex, localId) != null;
    }

    @Override
    public int getLocalManifoldDimensionality(Position p) {
        // Discrete graph is always locally 1-dimensional (graph edges)
        return 1;
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — curvature and policy
    // ----------------------------------------------------------------

    @Override
    public CurvatureField getCurvatureAt(Position p) {
        GraphNode node = getGraphNodeForPosition(p);
        if (node == null) return flatCurvature;
        Object val = node.getAttributes().get(ATTR_CURVATURE_INTENSITY);
        if (!(val instanceof Double intensity)) return flatCurvature;
        return buildCurvatureField(intensity);
    }

    @Override
    public RegionCapabilities getCapabilitiesAt(Position p) {
        // All nodes have the same structural capabilities in this simple implementation
        return new DiscreteRegionCapabilities();
    }

    @Override
    public RegionPolicy getPolicyAt(Position p) {
        GraphNode node = getGraphNodeForPosition(p);
        if (node == null) return defaultPolicy;
        Object val = node.getAttributes().get(ATTR_REGION_POLICY);
        if (!(val instanceof RegionPolicy policy)) return defaultPolicy;
        return policy;
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — movement
    // ----------------------------------------------------------------

    @Override
    public Position move(Position from, MotionVector vector) {
        // In discrete space, movement means following an edge.
        // Use the first available transition if any.
        List<Transition> transitions = getTransitionsFrom(from, emptyQuery());
        if (transitions.isEmpty()) return from;
        return transitions.get(0).getTarget();
    }

    @Override
    public MotionAdaptationResult adaptMotion(Position at, MotionVector vector) {
        // Discrete graphs don't adapt motion vectors — return unchanged
        return new DiscreteMotionAdaptationResult(vector, false, "discrete graph: no adaptation");
    }

    /**
     * HOLONOMY POLICY: APPROXIMATE
     * Discrete backends do not support true parallel transport.
     * Returns the input vector unchanged.
     */
    @Override
    public MotionVector parallelTransport(Position from, Position to, MotionVector vector) {
        return vector;
    }

    /**
     * HOLONOMY POLICY: APPROXIMATE
     * Discrete backends do not support holonomy computation.
     * Always returns Optional.empty().
     */
    @Override
    public Optional<MotionVector> computeHolonomy(List<Position> closedPath) {
        return Optional.empty();
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — transitions and neighborhood
    // ----------------------------------------------------------------

    @Override
    public List<Transition> getTransitionsFrom(Position p, TransitionQuery query) {
        GraphNode node = getGraphNodeForPosition(p);
        if (node == null) return List.of();

        String globalId = node.getGlobalNodeId();
        List<GraphEdge> edges = graph.getAllEdgesFromNode(globalId);

        return edges.stream()
            .map(edge -> buildTransition(p, edge, globalId, query))
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public NeighborhoodQuery neighborhoodQuery(Position center, double radius) {
        List<Transition> transitions = getTransitionsFrom(center, emptyQuery());
        List<Position> positions = transitions.stream()
            .map(Transition::getTarget)
            .collect(Collectors.toList());
        return new DiscreteNeighborhoodQuery(positions, transitions, radius);
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — geodesics
    // ----------------------------------------------------------------

    @Override
    public Optional<List<Position>> findGeodesic(Position from, Position to) {
        // BFS shortest path between two nodes
        if (!isValidPosition(from) || !isValidPosition(to)) return Optional.empty();
        if (from.equals(to)) return Optional.of(List.of(from));

        Map<String, String> cameFrom = new HashMap<>();
        Queue<Position> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        String fromId = ((DiscretePosition) from).getGlobalNodeId();
        String toId   = ((DiscretePosition) to).getGlobalNodeId();

        queue.add(from);
        visited.add(fromId);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            String currentId = ((DiscretePosition) current).getGlobalNodeId();

            for (Transition t : getTransitionsFrom(current, emptyQuery())) {
                Position next   = t.getTarget();
                String nextId   = ((DiscretePosition) next).getGlobalNodeId();
                if (visited.contains(nextId)) continue;
                cameFrom.put(nextId, currentId);
                if (nextId.equals(toId)) return Optional.of(reconstructPath(cameFrom, from, to));
                visited.add(nextId);
                queue.add(next);
            }
        }
        return Optional.empty();
    }

    @Override
    public double geodesicDistance(Position from, Position to) {
        return findGeodesic(from, to).map(path -> (double)(path.size() - 1)).orElse(Double.MAX_VALUE);
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — context and debug
    // ----------------------------------------------------------------

    @Override
    public ExecutionContext defaultContext() {
        return new DiscreteExecutionContext(this);
    }

    @Override
    public Optional<PositionDebug> debugProject(Position p) {
        if (!(p instanceof DiscretePosition dp)) return Optional.empty();
        int layer = graph.getLayerIndexFromGlobalId(dp.getGlobalNodeId());
        return Optional.of(new DiscretePositionDebug(dp.getGlobalNodeId(), layer));
    }


    // ----------------------------------------------------------------
    // DiscreteGraphManifold — discrete-specific queries
    // ----------------------------------------------------------------

    @Override
    public int getLayerCount() {
        return graph.getLayerCount();
    }

    @Override
    public List<GraphNode> getNodesInLayer(int layerIndex) {
        return graph.getNodesInLayer(layerIndex);
    }

    @Override
    public List<GraphEdge> getWithinLayerEdges(int layerIndex) {
        return graph.getNodesInLayer(layerIndex).stream()
            .flatMap(node -> graph.getWithinLayerEdges(layerIndex, node.getLocalNodeId()).stream())
            .distinct()
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<GraphEdge> getCrossLayerEdges(int sourceLayer, int targetLayer) {
        return graph.getCrossLayerEdgesByLayerPair(sourceLayer, targetLayer);
    }

    @Override
    public GraphEdge getGraphEdgeForTransition(Transition transition) {
        if (!(transition instanceof DiscreteTransition dt)) return null;
        return dt.getBackingEdge();
    }

    @Override
    public GraphNode getGraphNodeForPosition(Position p) {
        if (!(p instanceof DiscretePosition dp)) return null;
        String globalId = dp.getGlobalNodeId();
        try {
            int layerIndex = graph.getLayerIndexFromGlobalId(globalId);
            String localId = graph.getLocalNodeIdFromGlobalId(globalId);
            return graph.getNode(layerIndex, localId);
        } catch (Exception e) {
            return null;
        }
    }


    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private Position nodeToPosition(GraphNode node) {
        return new DiscretePosition(
            node.getGlobalNodeId(),
            new double[]{node.getLayerIndex(), 0.0}
        );
    }

    private Transition buildTransition(Position from, GraphEdge edge,
                                       String sourceGlobalId, TransitionQuery query) {
        // Find the target node for this edge
        List<GraphEdge> crossEdges = graph.getCrossLayerEdgesBySourceNode(sourceGlobalId);
        boolean isCross = crossEdges.contains(edge);
        String type = isCross
            ? CommonTransitionTypes.LAYER_JUMP
            : CommonTransitionTypes.LOCAL_STEP;

        // For now, target is the first node reachable via this edge in any adjacent layer
        // In a full implementation, the graph would store source->target per edge
        // Here we approximate: cross edges go to next layer node with same local ID
        Position target = resolveTarget(sourceGlobalId, edge, isCross);
        if (target == null) return null;

        return new DiscreteTransition(from, target, edge, type, 1.0);
    }

    private Position resolveTarget(String sourceGlobalId, GraphEdge edge, boolean isCross) {
        // Simple heuristic: use edge attributes to find target global ID if present
        Object targetId = edge.getAttributes().get("target_global_id");
        if (targetId instanceof String globalId) {
            return new DiscretePosition(globalId, new double[]{
                graph.getLayerIndexFromGlobalId(globalId), 0.0
            });
        }
        return null;
    }

    private List<Position> reconstructPath(Map<String, String> cameFrom,
                                            Position from, Position to) {
        LinkedList<Position> path = new LinkedList<>();
        String current = ((DiscretePosition) to).getGlobalNodeId();
        String fromId  = ((DiscretePosition) from).getGlobalNodeId();

        while (!current.equals(fromId)) {
            int layer   = graph.getLayerIndexFromGlobalId(current);
            path.addFirst(new DiscretePosition(current, new double[]{layer, 0.0}));
            current = cameFrom.get(current);
            if (current == null) return List.of();
        }
        path.addFirst(from);
        return Collections.unmodifiableList(path);
    }

    private TransitionQuery emptyQuery() {
        return new TransitionQuery() {
            @Override public Map<String, Object> getConstraints() { return Map.of(); }
            @Override public int getMaxResults() { return Integer.MAX_VALUE; }
            @Override public boolean includeNonTraversable() { return false; }
        };
    }

    private RegionPolicy buildDefaultPolicy() {
        return new RegionPolicy() {
            @Override public double getOperationWeight(String op) { return 1.0; }
            @Override public Map<String, Double> getPolicyWeights() { return Map.of(); }
            @Override public Optional<MotionVector> getSuggestedCorrection(MotionVector v) { return Optional.empty(); }
            @Override public boolean allowsOperation(String op) { return true; }
        };
    }

    private CurvatureField buildFlatCurvature() {
        return buildCurvatureField(0.0);
    }

    private CurvatureField buildCurvatureField(double intensity) {
        return new CurvatureField() {
            @Override public double getScalarIntensity() { return intensity; }
            @Override public Optional<double[]> getCurvatureTensor() { return Optional.empty(); }
            @Override public boolean isFlat() { return intensity == 0.0; }
            @Override public boolean isSingular() { return false; }
        };
    }


    // ----------------------------------------------------------------
    // Inner value types
    // ----------------------------------------------------------------

    public record DiscretePosition(String globalNodeId, double[] coordinates)
            implements Position {
        @Override public double[] getCoordinates() { return coordinates; }
        @Override public int getRepresentationDimensionality() { return coordinates.length; }
        @Override public boolean isValid() { return globalNodeId != null && !globalNodeId.isBlank(); }
        public String getGlobalNodeId() { return globalNodeId; }
    }

    public record DiscreteTransition(
            Position source, Position target,
            GraphEdge backingEdge, String transitionType, double cost)
            implements Transition {
        @Override public Position getSource() { return source; }
        @Override public Position getTarget() { return target; }
        @Override public double getCost() { return cost; }
        @Override public boolean isTraversable() { return true; }
        @Override public String getTransitionType() { return transitionType; }
        @Override public Map<String, Double> getAttributes() { return Map.of(); }
        public GraphEdge getBackingEdge() { return backingEdge; }
    }

    private record DiscreteRegionCapabilities() implements RegionCapabilities {
        @Override public boolean canSpawn() { return true; }
        @Override public boolean canMerge() { return true; }
        @Override public boolean canSplit() { return true; }
        @Override public boolean allowsLayerJump() { return true; }
        @Override public boolean allowsWraparound() { return false; }
        @Override public boolean isTraversable() { return true; }
    }

    private record DiscreteMotionAdaptationResult(
            MotionVector adaptedVector, boolean wasModified, String modificationReason)
            implements MotionAdaptationResult {
        @Override public MotionVector getAdaptedVector() { return adaptedVector; }
        @Override public boolean wasModified() { return wasModified; }
        @Override public String getModificationReason() { return modificationReason; }
    }

    private record DiscreteNeighborhoodQuery(
            List<Position> positions, List<Transition> transitions, double radius)
            implements NeighborhoodQuery {
        @Override public List<Flatlander> getNearbyFlatlanders() { return List.of(); }
        @Override public List<Position> getPositionsInRadius() { return positions; }  // renamed
        @Override public List<Transition> getAvailableTransitions() { return transitions; }
        @Override public boolean isFrozen() { return true; }
        @Override public OptionalLong getCapturedAtStep() { return OptionalLong.empty(); }
        @Override public double getRadius() { return radius; }
		@Override
		public List<Position> getNearbyPositions() {
			// TODO Auto-generated method stub
			return positions;
		}
    }

    private record DiscretePositionDebug(String label, int layer) implements PositionDebug {
        @Override public double getX() { return layer; }
        @Override public double getY() { return 0.0; }
        @Override public String getLabel() { return label; }
    }

    private static class DiscreteExecutionContext implements ExecutionContext {
        private final ManifoldSpace space;
        DiscreteExecutionContext(ManifoldSpace space) { this.space = space; }
        @Override public long getCurrentStep() { return -1L; }
        @Override public ManifoldSpace getManifoldSpace() { return space; }
        @Override public boolean isSynthetic() { return true; }
        @Override public Map<String, Object> getContextAttributes() { return Map.of(); }
    }
}