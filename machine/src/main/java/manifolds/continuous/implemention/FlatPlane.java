package manifolds.continuous.implemention;



import java.util.*;
import java.util.function.Function;

import machine.CommonBackendTypes;
import machine.CommonTopologyTypes;
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
import manifolds.continuous.ContinuousManifold;
import manifolds.continuous.GeodesicPathfinder;
import manifolds.continuous.GeometricCurvatureCalculator;
import manifolds.continuous.ManifoldPoint;

/**
 * A flat 2D Euclidean plane — the simplest continuous manifold.
 *
 * HOLONOMY POLICY: PRESERVE (trivially — flat space accumulates no holonomy)
 * CHART: single chart, identity metric, zero curvature everywhere
 * GEODESICS: exact, straight lines
 * BASIS CONVENTION: standard orthonormal basis {(1,0), (0,1)}
 * PATH SEMANTICS: straight-line interpolation (equivalent to geodesic on flat space)
 */
public class FlatPlane implements ContinuousManifold {

    private static final String CHART_ID = "flat_plane_chart";

    private final ManifoldDescriptor descriptor;
    private final GeodesicPathfinder geodesicPathfinder;
    private final GeometricCurvatureCalculator curvatureCalculator;

    public FlatPlane(String name) {
        this.descriptor = buildDescriptor(name);
        this.geodesicPathfinder = new FlatPlaneGeodesicPathfinder();
        this.curvatureCalculator = new FlatPlaneCurvatureCalculator();
    }

    // ------------------------------------------------------------
    // ManifoldSpace
    // ------------------------------------------------------------

    @Override
    public ManifoldDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Position origin() {
        return point(0.0, 0.0);
    }

    @Override
    public boolean isValidPosition(Position p) {
        return p instanceof FlatPoint;
    }

    @Override
    public int getLocalManifoldDimensionality(Position p) {
        requireFlatPoint(p);
        return 2;
    }

    @Override
    public CurvatureField getCurvatureAt(Position p) {
        requireFlatPoint(p);
        return curvatureCalculator.buildCurvatureField(p, getMetricTensor(p));
    }

    @Override
    public RegionCapabilities getCapabilitiesAt(Position p) {
        requireFlatPoint(p);
        return FlatPlaneCapabilities.INSTANCE;
    }

    @Override
    public RegionPolicy getPolicyAt(Position p) {
        requireFlatPoint(p);
        return FlatPlanePolicy.INSTANCE;
    }

    @Override
    public Position move(Position from, MotionVector vector) {
        FlatPoint fp = requireFlatPoint(from);
        double[] v = require2D(vector);
        return point(fp.x() + v[0], fp.y() + v[1]);
    }

    @Override
    public MotionAdaptationResult adaptMotion(Position at, MotionVector vector) {
        requireFlatPoint(at);
        require2D(vector);
        return new SimpleMotionAdaptationResult(vector, false, "flat plane: no adaptation needed");
    }

    @Override
    public MotionVector parallelTransport(Position from, Position to, MotionVector vector) {
        requireFlatPoint(from);
        requireFlatPoint(to);
        require2D(vector);
        return vector;
    }

    @Override
    public Optional<MotionVector> computeHolonomy(List<Position> closedPath) {
        if (closedPath == null || closedPath.isEmpty()) {
            return Optional.empty();
        }
        for (Position p : closedPath) {
            requireFlatPoint(p);
        }
        return Optional.of(zeroVector2D());
    }

    @Override
    public List<Transition> getTransitionsFrom(Position p, TransitionQuery query) {
        FlatPoint fp = requireFlatPoint(p);

        double step = 1.0;
        List<Transition> transitions = new ArrayList<>();
        transitions.add(simpleTransition(p, point(fp.x() + step, fp.y()), CommonTransitionTypes.LOCAL_STEP, step));
        transitions.add(simpleTransition(p, point(fp.x() - step, fp.y()), CommonTransitionTypes.LOCAL_STEP, step));
        transitions.add(simpleTransition(p, point(fp.x(), fp.y() + step), CommonTransitionTypes.LOCAL_STEP, step));
        transitions.add(simpleTransition(p, point(fp.x(), fp.y() - step), CommonTransitionTypes.LOCAL_STEP, step));

        int maxResults = query != null ? Math.max(0, query.getMaxResults()) : transitions.size();
        if (maxResults > 0 && maxResults < transitions.size()) {
            return Collections.unmodifiableList(transitions.subList(0, maxResults));
        }
        return Collections.unmodifiableList(transitions);
    }

    @Override
    public NeighborhoodQuery neighborhoodQuery(Position center, double radius) {
        FlatPoint fp = requireFlatPoint(center);

        List<Position> positions = new ArrayList<>();
        for (double dx = -radius; dx <= radius; dx += 1.0) {
            for (double dy = -radius; dy <= radius; dy += 1.0) {
                if (dx == 0.0 && dy == 0.0) {
                    continue;
                }
                if ((dx * dx + dy * dy) <= radius * radius) {
                    positions.add(point(fp.x() + dx, fp.y() + dy));
                }
            }
        }

        List<Transition> transitions = getTransitionsFrom(center, emptyTransitionQuery());
        return new SimpleNeighborhoodQuery(
        	    positions,          // nearbyPositions
        	    positions,          // positionsInRadius (often same list for flat plane)
        	    transitions,
        	    radius
        	);
    }

    @Override
    public Optional<List<Position>> findGeodesic(Position from, Position to) {
        return geodesicPathfinder.findBestGeodesic(from, to, 32)
                .map(GeodesicPathfinder.GeodesicResult::getPath);
    }

    @Override
    public double geodesicDistance(Position from, Position to) {
        FlatPoint a = requireFlatPoint(from);
        FlatPoint b = requireFlatPoint(to);
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public ExecutionContext defaultContext() {
        return new SyntheticExecutionContext(this);
    }

    @Override
    public Optional<PositionDebug> debugProject(Position p) {
        FlatPoint fp = requireFlatPoint(p);
        return Optional.of(new SimplePositionDebug(
                fp.x(),
                fp.y(),
                String.format("flat(%.3f, %.3f)", fp.x(), fp.y())
        ));
    }

    // ------------------------------------------------------------
    // ContinuousManifold — chart transitions
    // ------------------------------------------------------------

    @Override
    public String getChartIdAt(Position p) {
        requireFlatPoint(p);
        return CHART_ID;
    }

    @Override
    public Optional<ManifoldPoint> transitionToChart(ManifoldPoint point, String targetChartId) {
        if (point == null || targetChartId == null) {
            return Optional.empty();
        }
        if (!CHART_ID.equals(targetChartId)) {
            return Optional.empty();
        }
        return Optional.of(point);
    }

    @Override
    public List<String> getChartsAt(Position p) {
        requireFlatPoint(p);
        return List.of(CHART_ID);
    }

    // ------------------------------------------------------------
    // ContinuousManifold — metric tensor
    // ------------------------------------------------------------

    @Override
    public double[][] getMetricTensor(Position p) {
        requireFlatPoint(p);
        return new double[][]{
                {1.0, 0.0},
                {0.0, 1.0}
        };
    }

    @Override
    public double innerProduct(Position p, MotionVector u, MotionVector v) {
        requireFlatPoint(p);
        double[] uc = require2D(u);
        double[] vc = require2D(v);
        return uc[0] * vc[0] + uc[1] * vc[1];
    }

    @Override
    public double norm(Position p, MotionVector v) {
        return Math.sqrt(innerProduct(p, v, v));
    }

    // ------------------------------------------------------------
    // ContinuousManifold — local coordinate frame
    // ------------------------------------------------------------

    @Override
    public List<MotionVector> getLocalCoordinateFrame(Position p) {
        requireFlatPoint(p);
        return List.of(
                new SimpleMotionVector(new double[]{1.0, 0.0}),
                new SimpleMotionVector(new double[]{0.0, 1.0})
        );
    }

    @Override
    public MotionVector projectToTangentSpace(Position p, MotionVector v) {
        requireFlatPoint(p);
        require2D(v);
        return v;
    }

    @Override
    public Optional<MotionVector> getNormalVector(Position p) {
        requireFlatPoint(p);
        return Optional.of(new SimpleMotionVector(new double[]{0.0, 0.0, 1.0}));
    }

    // ------------------------------------------------------------
    // ContinuousManifold — integration along paths
    // PATH SEMANTICS: straight-line interpolation
    // ------------------------------------------------------------

    @Override
    public Optional<Double> integrateAlongPath(
            Position from,
            Position to,
            Function<Position, Double> field,
            int steps
    ) {
        FlatPoint a = requireFlatPoint(from);
        FlatPoint b = requireFlatPoint(to);
        Objects.requireNonNull(field, "field must not be null");

        if (steps <= 0) {
            return Optional.empty();
        }

        double dx = (b.x() - a.x()) / steps;
        double dy = (b.y() - a.y()) / steps;
        double ds = Math.sqrt(dx * dx + dy * dy);

        double sum = 0.0;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Position sample = point(
                    a.x() + t * (b.x() - a.x()),
                    a.y() + t * (b.y() - a.y())
            );
            double weight = (i == 0 || i == steps) ? 0.5 : 1.0;
            sum += weight * field.apply(sample) * ds;
        }
        return Optional.of(sum);
    }

    @Override
    public Optional<MotionVector> integrateVectorAlongPath(
            Position from,
            Position to,
            Function<Position, MotionVector> field,
            int steps
    ) {
        FlatPoint a = requireFlatPoint(from);
        FlatPoint b = requireFlatPoint(to);
        Objects.requireNonNull(field, "field must not be null");

        if (steps <= 0) {
            return Optional.empty();
        }

        double dx = (b.x() - a.x()) / steps;
        double dy = (b.y() - a.y()) / steps;
        double ds = Math.sqrt(dx * dx + dy * dy);

        double[] acc = new double[]{0.0, 0.0};
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Position sample = point(
                    a.x() + t * (b.x() - a.x()),
                    a.y() + t * (b.y() - a.y())
            );
            double[] v = require2D(field.apply(sample));
            double weight = (i == 0 || i == steps) ? 0.5 : 1.0;
            acc[0] += weight * v[0] * ds;
            acc[1] += weight * v[1] * ds;
        }
        return Optional.of(new SimpleMotionVector(acc));
    }

    @Override
    public Optional<Double> arcLength(Position from, Position to, int steps) {
        requireFlatPoint(from);
        requireFlatPoint(to);
        return Optional.of(geodesicDistance(from, to));
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private FlatPoint point(double x, double y) {
        return new FlatPoint(x, y, CHART_ID);
    }

    private FlatPoint requireFlatPoint(Position p) {
        if (!(p instanceof FlatPoint fp)) {
            throw new IllegalArgumentException("Position must be a FlatPoint: " + p);
        }
        return fp;
    }

    private double[] require2D(MotionVector v) {
        if (v == null) {
            throw new IllegalArgumentException("MotionVector must not be null");
        }
        double[] c = v.getComponents();
        if (c == null || c.length != 2) {
            throw new IllegalArgumentException("MotionVector must have exactly 2 components");
        }
        return c;
    }

    private MotionVector zeroVector2D() {
        return new SimpleMotionVector(new double[]{0.0, 0.0});
    }

    private Transition simpleTransition(Position from, Position to, String type, double cost) {
        return new SimpleTransition(from, to, type, cost);
    }

    private TransitionQuery emptyTransitionQuery() {
        return new TransitionQuery() {
            @Override
            public Map<String, Object> getConstraints() {
                return Map.of();
            }

            @Override
            public int getMaxResults() {
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean includeNonTraversable() {
                return false;
            }
        };
    }

    private ManifoldDescriptor buildDescriptor(String name) {
        return new ManifoldDescriptor() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getBackendType() {
                return CommonBackendTypes.CONTINUOUS;
            }

            @Override
            public String getTopologyType() {
                return CommonTopologyTypes.FLAT;
            }

            @Override
            public int getIntrinsicDimensionality() {
                return 2;
            }

            @Override
            public Map<String, Object> getProperties() {
                return Map.of();
            }
        };
    }

    // ------------------------------------------------------------
    // Value Types
    // ------------------------------------------------------------

    public record FlatPoint(double x, double y, String chartId) implements ManifoldPoint {
        @Override
        public double[] getCoordinates() {
            return new double[]{x, y};
        }

        @Override
        public int getRepresentationDimensionality() {
            return 2;
        }

        @Override
        public int getDimensionality() {
            return 2;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String getChartId() {
            return chartId;
        }
    }

    record SimpleMotionVector(double[] components) implements MotionVector {
        @Override
        public double[] getComponents() {
            return Arrays.copyOf(components, components.length);
        }

        @Override
        public double getMagnitude() {
            double sum = 0.0;
            for (double c : components) {
                sum += c * c;
            }
            return Math.sqrt(sum);
        }

        @Override
        public MotionVector scale(double factor) {
            double[] scaled = new double[components.length];
            for (int i = 0; i < components.length; i++) {
                scaled[i] = components[i] * factor;
            }
            return new SimpleMotionVector(scaled);
        }

        @Override
        public MotionVector add(MotionVector other) {
            double[] oc = other.getComponents();
            if (oc.length != components.length) {
                throw new IllegalArgumentException("MotionVector dimensionality mismatch");
            }
            double[] result = new double[components.length];
            for (int i = 0; i < components.length; i++) {
                result[i] = components[i] + oc[i];
            }
            return new SimpleMotionVector(result);
        }
    }

    private record SimpleMotionAdaptationResult(
            MotionVector adaptedVector,
            boolean wasModified,
            String modificationReason
    ) implements MotionAdaptationResult {
        @Override
        public MotionVector getAdaptedVector() {
            return adaptedVector;
        }

        @Override
        public boolean wasModified() {
            return wasModified;
        }

        @Override
        public String getModificationReason() {
            return modificationReason;
        }
    }

    private record SimpleTransition(
            Position source,
            Position target,
            String transitionType,
            double cost
    ) implements Transition {
        @Override
        public Position getSource() {
            return source;
        }

        @Override
        public Position getTarget() {
            return target;
        }

        @Override
        public double getCost() {
            return cost;
        }

        @Override
        public boolean isTraversable() {
            return true;
        }

        @Override
        public String getTransitionType() {
            return transitionType;
        }

        @Override
        public Map<String, Double> getAttributes() {
            return Map.of();
        }
    }

    private record SimpleNeighborhoodQuery(
            List<Position> nearbyPositions,
            List<Position> positionsInRadius,
            List<Transition> availableTransitions,
            double radius
    ) implements NeighborhoodQuery {

        @Override
        public List<Flatlander> getNearbyFlatlanders() {
            return List.of();
        }

        @Override
        public List<Position> getNearbyPositions() {
            return Collections.unmodifiableList(nearbyPositions);
        }

        @Override
        public List<Transition> getAvailableTransitions() {
            return Collections.unmodifiableList(availableTransitions);
        }

        @Override
        public boolean isFrozen() {
            return true;
        }

        @Override
        public OptionalLong getCapturedAtStep() {
            return OptionalLong.empty();
        }

        @Override
        public double getRadius() {
            return radius;
        }

        @Override
        public List<Position> getPositionsInRadius() {
            return Collections.unmodifiableList(positionsInRadius);
        }

    }

    private record SimplePositionDebug(double x, double y, String label) implements PositionDebug {
        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    private static final class SyntheticExecutionContext implements ExecutionContext {
        private final ManifoldSpace manifoldSpace;

        private SyntheticExecutionContext(ManifoldSpace manifoldSpace) {
            this.manifoldSpace = manifoldSpace;
        }

        @Override
        public long getCurrentStep() {
            return -1L;
        }

        @Override
        public ManifoldSpace getManifoldSpace() {
            return manifoldSpace;
        }

        @Override
        public boolean isSynthetic() {
            return true;
        }

        @Override
        public Map<String, Object> getContextAttributes() {
            return Map.of();
        }
    }

    private enum FlatPlaneCapabilities implements RegionCapabilities {
        INSTANCE;

        @Override
        public boolean canSpawn() {
            return true;
        }

        @Override
        public boolean canMerge() {
            return true;
        }

        @Override
        public boolean canSplit() {
            return true;
        }

        @Override
        public boolean allowsLayerJump() {
            return false;
        }

        @Override
        public boolean allowsWraparound() {
            return false;
        }

        @Override
        public boolean isTraversable() {
            return true;
        }
    }

    private enum FlatPlanePolicy implements RegionPolicy {
        INSTANCE;

        @Override
        public double getOperationWeight(String operationName) {
            return 1.0;
        }

        @Override
        public Map<String, Double> getPolicyWeights() {
            return Map.of();
        }

        @Override
        public Optional<MotionVector> getSuggestedCorrection(MotionVector attemptedMotion) {
            return Optional.empty();
        }

        @Override
        public boolean allowsOperation(String operationName) {
            return true;
        }
    }

    // ------------------------------------------------------------
    // Geodesics
    // ------------------------------------------------------------

    private final class FlatPlaneGeodesicPathfinder implements GeodesicPathfinder {

        @Override
        public Optional<GeodesicResult> findExactGeodesic(Position from, Position to) {
            FlatPoint a = requireFlatPoint(from);
            FlatPoint b = requireFlatPoint(to);

            if (a.equals(b)) {
                return Optional.empty();
            }

            int samples = 32;
            List<Position> path = new ArrayList<>(samples + 1);
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / samples;
                path.add(point(
                        a.x() + t * (b.x() - a.x()),
                        a.y() + t * (b.y() - a.y())
                ));
            }

            return Optional.of(new SimpleGeodesicResult(
                    Collections.unmodifiableList(path),
                    geodesicDistance(from, to),
                    true,
                    true
            ));
        }

        @Override
        public boolean supportsExactGeodesic(Position from, Position to) {
            return isValidPosition(from) && isValidPosition(to);
        }

        @Override
        public List<GeodesicResult> findAllExactGeodesics(Position from, Position to) {
            return findExactGeodesic(from, to).map(List::of).orElseGet(List::of);
        }

        @Override
        public Optional<GeodesicResult> findApproximateGeodesic(Position from, Position to, int steps) {
            return findExactGeodesic(from, to);
        }

        @Override
        public List<GeodesicResult> findAllApproximateGeodesics(
                Position from,
                Position to,
                int steps,
                int maxResults
        ) {
            List<GeodesicResult> results = findAllExactGeodesics(from, to);
            if (maxResults <= 0 || results.isEmpty()) {
                return List.of();
            }
            return results.subList(0, Math.min(results.size(), maxResults));
        }

        @Override
        public Optional<GeodesicResult> findBestGeodesic(Position from, Position to, int steps) {
            return findExactGeodesic(from, to);
        }
    }

    // ------------------------------------------------------------
    // Curvature
    // ------------------------------------------------------------

    private class FlatPlaneCurvatureCalculator implements GeometricCurvatureCalculator {

        @Override
        public double computeScalarCurvature(Position p, double[][] metricTensor) {
            return 0.0;
        }

        @Override
        public boolean isSingular(Position p, double[][] metricTensor) {
            return false;
        }

        @Override
        public boolean isFlat(Position p, double[][] metricTensor) {
            return true;
        }

        @Override
        public Optional<double[][][][]> computeRiemannTensor(Position p, double[][] metricTensor) {
            double[][][][] tensor = new double[2][2][2][2];
            return Optional.of(tensor);
        }

        @Override
        public Optional<double[][]> computeRicciTensor(Position p, double[][] metricTensor) {
            return Optional.of(new double[][]{{0.0, 0.0}, {0.0, 0.0}});
        }

        @Override
        public Optional<Double> computeGaussianCurvature(Position p, double[][] metricTensor) {
            return Optional.of(0.0);
        }

        @Override
        public Optional<double[]> computePrincipalCurvatures(Position p, double[][] metricTensor) {
            return Optional.of(new double[]{0.0, 0.0});
        }

        @Override
        public Optional<Double> computeMeanCurvature(Position p, double[][] metricTensor) {
            return Optional.of(0.0);
        }

        @Override
        public CurvatureField buildCurvatureField(Position p, double[][] metricTensor) {
            return new CurvatureField() {
                @Override public double getScalarIntensity() { return 0.0; }
                @Override public Optional<double[]> getCurvatureTensor() { return Optional.of(new double[]{0.0, 0.0, 0.0, 0.0}); }
                @Override public boolean isFlat() { return true; }
                @Override public boolean isSingular() { return false; }
            };
        }

        @Override
        public double computeScalarIntensity(Position p, double[][] metricTensor) {

            Optional<Double> gaussian = computeGaussianCurvature(p, metricTensor);

            if (gaussian.isEmpty()) {
                return 0.0;
            }

            // Scalar curvature R = 2K for 2D surfaces
            return 2.0 * gaussian.get();
        }
    }
    private record SimpleGeodesicResult(
            List<Position> path,
            double arcLength,
            boolean exact,
            boolean unique
    ) implements GeodesicPathfinder.GeodesicResult {
        @Override
        public List<Position> getPath() {
            return path;
        }

        @Override
        public double getArcLength() {
            return arcLength;
        }

        @Override
        public boolean isExact() {
            return exact;
        }

        @Override
        public boolean isUnique() {
            return unique;
        }

        @Override
        public Optional<Double> getErrorEstimate() {
            return Optional.empty();
        }
    }
    @Override
    public String getPrimaryChartIdAt(Position p) {
        requireFlatPoint(p);
        return CHART_ID;
    }
}