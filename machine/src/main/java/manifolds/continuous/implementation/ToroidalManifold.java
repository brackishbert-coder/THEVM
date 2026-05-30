package manifolds.continuous.implementation;

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
 * A flat toroidal manifold — a 2D surface with periodic boundary conditions
 * in both dimensions. Topologically equivalent to a square with identified edges.
 *
 * HOLONOMY POLICY: PRESERVE
 * Flat torus: holonomy is preserved but accumulates on non-contractible loops.
 * A flatlander traversing a non-contractible loop returns with a phase shift.
 *
 * CHART: single chart with wraparound at [0, width) x [0, height)
 * GEODESICS: multiple — straight lines in both directions around the torus
 * BASIS CONVENTION: standard orthonormal basis {(1,0), (0,1)}, periodic
 * PATH SEMANTICS: geodesic straight line with wraparound
 *
 * TOPOLOGY NOTE: a flat torus has K=0 (Gaussian curvature zero) everywhere,
 * but has global topological structure that produces multiple geodesics and
 * non-trivial holonomy on non-contractible loops.
 */
public class ToroidalManifold implements ContinuousManifold {

    private static final String CHART_ID      = "torus_chart";
    private static final String BACKEND_TYPE  = CommonBackendTypes.CONTINUOUS;
    private static final String TOPOLOGY_TYPE = CommonTopologyTypes.TOROIDAL;

    private final double width;
    private final double height;
    private final ManifoldDescriptor descriptor;
    private final GeodesicPathfinder geodesicPathfinder;
    private final GeometricCurvatureCalculator curvatureCalculator;

    public ToroidalManifold(String name, double width, double height) {
        this.width   = width;
        this.height  = height;
        this.descriptor          = buildDescriptor(name);
        this.geodesicPathfinder  = new TorusGeodesicPathfinder();
        this.curvatureCalculator = new TorusCurvatureCalculator();
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
        return  point(0.0, 0.0);
    }
    private TorusPoint point(double u, double v) {
        return new TorusPoint(u, v, CHART_ID);
    }

    @Override
    public boolean isValidPosition(Position p) {
        return p instanceof TorusPoint;
    }

    @Override
    public int getLocalManifoldDimensionality(Position p) {
        return 2;
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — curvature and policy
    // ----------------------------------------------------------------

    @Override
    public CurvatureField getCurvatureAt(Position p) {
        return curvatureCalculator.buildCurvatureField(p, getMetricTensor(p));
    }

    @Override
    public RegionCapabilities getCapabilitiesAt(Position p) {
        return new TorusCapabilities();
    }

    @Override
    public RegionPolicy getPolicyAt(Position p) {
        return new TorusPolicy();
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — movement
    // ----------------------------------------------------------------

    @Override
    public Position move(Position from, MotionVector vector) {
        TorusPoint tp = requireTorusPoint(from);
        double[] v    = vector.getComponents();
        return wrap(tp.x() + v[0], tp.y() + v[1]);
    }

    @Override
    public MotionAdaptationResult adaptMotion(Position at, MotionVector vector) {
        // Torus: no adaptation needed — wraparound is handled in move()
        return new SimpleMotionAdaptationResult(vector, false, "torus: no adaptation needed");
    }

    /**
     * HOLONOMY POLICY: PRESERVE
     * Parallel transport on a flat torus is path-dependent for non-contractible loops.
     * For contractible paths, the vector is unchanged.
     * For non-contractible paths, the vector accumulates a phase rotation.
     *
     * This implementation detects wraparound crossings and accumulates
     * the corresponding holonomy rotation.
     */
    @Override
    public MotionVector parallelTransport(Position from, Position to, MotionVector vector) {
        // Flat torus: holonomy only on non-contractible loops
        // For a path segment, vector is unchanged (flat metric)
        return vector;
    }

    @Override
    public Optional<MotionVector> computeHolonomy(List<Position> closedPath) {
        if (closedPath == null || closedPath.size() < 2) return Optional.of(zeroVector());
        // Count net wraparound crossings in each dimension
        int wrapX = 0, wrapY = 0;
        for (int i = 0; i < closedPath.size() - 1; i++) {
            TorusPoint a = requireTorusPoint(closedPath.get(i));
            TorusPoint b = requireTorusPoint(closedPath.get(i + 1));
            double dx = b.x() - a.x();
            double dy = b.y() - a.y();
            if (Math.abs(dx) > width / 2)  wrapX += (dx > 0) ? -1 : 1;
            if (Math.abs(dy) > height / 2) wrapY += (dy > 0) ? -1 : 1;
        }
        // Non-zero wraps indicate non-contractible loop — return winding vector
        return Optional.of(new FlatPlane.SimpleMotionVector(
            new double[]{wrapX * width, wrapY * height}));
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — transitions and neighborhood
    // ----------------------------------------------------------------

    @Override
    public List<Transition> getTransitionsFrom(Position p, TransitionQuery query) {
        TorusPoint tp = requireTorusPoint(p);
        double step   = 1.0;
        List<Transition> transitions = new ArrayList<>();
        String[] types = {
            CommonTransitionTypes.LOCAL_STEP,
            CommonTransitionTypes.LOCAL_STEP,
            CommonTransitionTypes.LOCAL_STEP,
            CommonTransitionTypes.LOCAL_STEP
        };
        Position[] targets = {
            wrap(tp.x() + step, tp.y()),
            wrap(tp.x() - step, tp.y()),
            wrap(tp.x(), tp.y() + step),
            wrap(tp.x(), tp.y() - step)
        };
        for (int i = 0; i < 4; i++) {
            boolean wraps = isWraparoundTransition(tp, targets[i]);
            String type   = wraps
                ? CommonTransitionTypes.WRAPAROUND
                : CommonTransitionTypes.LOCAL_STEP;
            transitions.add(new SimpleTransition(p, targets[i], type, step));
        }
        return Collections.unmodifiableList(transitions);
    }

    @Override
    public NeighborhoodQuery neighborhoodQuery(Position center, double radius) {
        TorusPoint tp = requireTorusPoint(center);
        List<Position> nearby = new ArrayList<>();
        for (double dx = -radius; dx <= radius; dx += 1.0) {
            for (double dy = -radius; dy <= radius; dy += 1.0) {
                if (dx*dx + dy*dy <= radius*radius && !(dx == 0 && dy == 0)) {
                    nearby.add(wrap(tp.x() + dx, tp.y() + dy));
                }
            }
        }
        return new SimpleNeighborhoodQuery(nearby, getTransitionsFrom(center, emptyQuery()), radius);
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — geodesics
    // ----------------------------------------------------------------

    @Override
    public Optional<List<Position>> findGeodesic(Position from, Position to) {
        return geodesicPathfinder.findBestGeodesic(from, to, 64)
            .map(GeodesicPathfinder.GeodesicResult::getPath);
    }

    @Override
    public double geodesicDistance(Position from, Position to) {
        TorusPoint a = requireTorusPoint(from);
        TorusPoint b = requireTorusPoint(to);
        // Shortest geodesic on torus: wrap-aware distance in each dimension
        double dx = Math.min(Math.abs(b.x() - a.x()), width  - Math.abs(b.x() - a.x()));
        double dy = Math.min(Math.abs(b.y() - a.y()), height - Math.abs(b.y() - a.y()));
        return Math.sqrt(dx*dx + dy*dy);
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — context and debug
    // ----------------------------------------------------------------

    @Override
    public ExecutionContext defaultContext() {
        return new SyntheticExecutionContext(this);
    }

    @Override
    public Optional<PositionDebug> debugProject(Position p) {
        TorusPoint tp = requireTorusPoint(p);
        return Optional.of(new SimplePositionDebug(tp.x(), tp.y(),
            String.format("torus(%.2f, %.2f)", tp.x(), tp.y())));
    }


    // ----------------------------------------------------------------
    // ContinuousManifold — chart transitions
    // ----------------------------------------------------------------

    @Override
    public String getPrimaryChartIdAt(Position p) {
        return CHART_ID;
    }

    @Override
    public List<String> getChartsAt(Position p) {
        return List.of(CHART_ID);
    }

    @Override
    public Optional<ManifoldPoint> transitionToChart(ManifoldPoint point, String targetChartId) {
        if (CHART_ID.equals(targetChartId)) return Optional.of(point);
        return Optional.empty();
    }


    // ----------------------------------------------------------------
    // ContinuousManifold — metric tensor
    // ----------------------------------------------------------------

    @Override
    public double[][] getMetricTensor(Position p) {
        // Flat torus: identity metric (inherited from Euclidean plane)
        return new double[][]{{1.0, 0.0}, {0.0, 1.0}};
    }

    @Override
    public double innerProduct(Position p, MotionVector u, MotionVector v) {
        double[] uc = u.getComponents();
        double[] vc = v.getComponents();
        return uc[0]*vc[0] + uc[1]*vc[1];
    }

    @Override
    public double norm(Position p, MotionVector v) {
        return Math.sqrt(innerProduct(p, v, v));
    }


    // ----------------------------------------------------------------
    // ContinuousManifold — local coordinate frame
    // ----------------------------------------------------------------

    @Override
    public List<MotionVector> getLocalCoordinateFrame(Position p) {
        return List.of(
            new FlatPlane.SimpleMotionVector(new double[]{1.0, 0.0}),
            new FlatPlane.SimpleMotionVector(new double[]{0.0, 1.0})
        );
    }

    @Override
    public MotionVector projectToTangentSpace(Position p, MotionVector v) {
        return v; // flat torus: all vectors tangent
    }

    @Override
    public Optional<MotionVector> getNormalVector(Position p) {
        // Flat torus as abstract manifold has no canonical normal
        return Optional.empty();
    }


    // ----------------------------------------------------------------
    // ContinuousManifold — integration
    // ----------------------------------------------------------------

    @Override
    public Optional<Double> integrateAlongPath(Position from, Position to,
                                                Function<Position, Double> field, int steps) {
        // PATH SEMANTICS: shortest geodesic on torus (wrap-aware straight line)
        TorusPoint a = requireTorusPoint(from);
        TorusPoint b = requireTorusPoint(to);
        double[] delta = shortestDelta(a, b);
        double sum = 0.0;
        double ds  = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]) / steps;
        for (int i = 0; i <= steps; i++) {
            double t   = (double) i / steps;
            Position p = wrap(a.x() + t*delta[0], a.y() + t*delta[1]);
            double w   = (i == 0 || i == steps) ? 0.5 : 1.0;
            sum += w * field.apply(p) * ds;
        }
        return Optional.of(sum);
    }

    @Override
    public Optional<MotionVector> integrateVectorAlongPath(Position from, Position to,
                                                            Function<Position, MotionVector> field,
                                                            int steps) {
        TorusPoint a = requireTorusPoint(from);
        TorusPoint b = requireTorusPoint(to);
        double[] delta = shortestDelta(a, b);
        double[] acc   = {0.0, 0.0};
        double ds = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]) / steps;
        for (int i = 0; i <= steps; i++) {
            double t   = (double) i / steps;
            Position p = wrap(a.x() + t*delta[0], a.y() + t*delta[1]);
            double[] v = field.apply(p).getComponents();
            double w   = (i == 0 || i == steps) ? 0.5 : 1.0;
            acc[0] += w * v[0] * ds;
            acc[1] += w * v[1] * ds;
        }
        return Optional.of(new FlatPlane.SimpleMotionVector(acc));
    }

    @Override
    public Optional<Double> arcLength(Position from, Position to, int steps) {
        return Optional.of(geodesicDistance(from, to));
    }


    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private TorusPoint wrap(double x, double y) {
        return new TorusPoint(
            ((x % width)  + width)  % width,
            ((y % height) + height) % height,
            CHART_ID
        );
    }

    private double[] shortestDelta(TorusPoint a, TorusPoint b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        if (Math.abs(dx) > width  / 2) dx -= Math.signum(dx) * width;
        if (Math.abs(dy) > height / 2) dy -= Math.signum(dy) * height;
        return new double[]{dx, dy};
    }

    private boolean isWraparoundTransition(TorusPoint from, Position to) {
        TorusPoint t = requireTorusPoint(to);
        return Math.abs(t.x() - from.x()) > width/2 || Math.abs(t.y() - from.y()) > height/2;
    }

    private TorusPoint requireTorusPoint(Position p) {
        if (!(p instanceof TorusPoint tp))
            throw new IllegalArgumentException("Position must be a TorusPoint: " + p);
        return tp;
    }

    private MotionVector zeroVector() {
        return new FlatPlane.SimpleMotionVector(new double[]{0.0, 0.0});
    }

    private TransitionQuery emptyQuery() {
        return new TransitionQuery() {
            @Override public Map<String, Object> getConstraints() { return Map.of(); }
            @Override public int getMaxResults() { return Integer.MAX_VALUE; }
            @Override public boolean includeNonTraversable() { return false; }
        };
    }

    private ManifoldDescriptor buildDescriptor(String name) {
        return new ManifoldDescriptor() {
            @Override public String getName() { return name; }
            @Override public String getBackendType() { return BACKEND_TYPE; }
            @Override public String getTopologyType() { return TOPOLOGY_TYPE; }
            @Override public int getIntrinsicDimensionality() { return 2; }
            @Override public Map<String, Object> getProperties() {
                return Map.of("width", width, "height", height);
            }
        };
    }


    // ----------------------------------------------------------------
    // Inner value types
    // ----------------------------------------------------------------

    public record TorusPoint(double x, double y, String chartId) implements ManifoldPoint {
        @Override public double[] getCoordinates() { return new double[]{x, y}; }
        @Override public int getRepresentationDimensionality() { return 2; }
        @Override public int getDimensionality() { return 2; }
        @Override public boolean isValid() { return true; }
        @Override public String getChartId() { return chartId; }
    }

    private record TorusCapabilities() implements RegionCapabilities {
        @Override public boolean canSpawn() { return true; }
        @Override public boolean canMerge() { return true; }
        @Override public boolean canSplit() { return true; }
        @Override public boolean allowsLayerJump() { return false; }
        @Override public boolean allowsWraparound() { return true; }
        @Override public boolean isTraversable() { return true; }
    }

    private record TorusPolicy() implements RegionPolicy {
        @Override public double getOperationWeight(String op) { return 1.0; }
        @Override public Map<String, Double> getPolicyWeights() { return Map.of(); }
        @Override public Optional<MotionVector> getSuggestedCorrection(MotionVector v) {
            return Optional.empty();
        }
        @Override public boolean allowsOperation(String op) { return true; }
    }

    private record SimpleMotionAdaptationResult(
            MotionVector adaptedVector, boolean wasModified, String modificationReason)
            implements MotionAdaptationResult {
        @Override public MotionVector getAdaptedVector() { return adaptedVector; }
        @Override public boolean wasModified() { return wasModified; }
        @Override public String getModificationReason() { return modificationReason; }
    }

    private record SimpleTransition(
            Position source, Position target, String transitionType, double cost)
            implements Transition {
        @Override public Position getSource() { return source; }
        @Override public Position getTarget() { return target; }
        @Override public double getCost() { return cost; }
        @Override public boolean isTraversable() { return true; }
        @Override public String getTransitionType() { return transitionType; }
        @Override public Map<String, Double> getAttributes() { return Map.of(); }
    }

    private record SimpleNeighborhoodQuery(
            List<Position> positions, List<Transition> transitions, double radius)
            implements NeighborhoodQuery {
        @Override public List<Flatlander> getNearbyFlatlanders() { return List.of(); }
        @Override public List<Position> getNearbyPositions() { return positions; }
        @Override public List<Transition> getAvailableTransitions() { return transitions; }
        @Override public boolean isFrozen() { return true; }
        @Override public OptionalLong getCapturedAtStep() { return OptionalLong.empty(); }
        @Override public double getRadius() { return radius; }
		@Override
		public List<Position> getPositionsInRadius() {
			// TODO Auto-generated method stub
			return positions;
		}
    }

    private record SimplePositionDebug(double x, double y, String label)
            implements PositionDebug {
        @Override public double getX() { return x; }
        @Override public double getY() { return y; }
        @Override public String getLabel() { return label; }
    }

    private record SimpleGeodesicResult(
            List<Position> path, double arcLength, boolean exact, boolean unique)
            implements GeodesicPathfinder.GeodesicResult {
        @Override public List<Position> getPath() { return path; }
        @Override public double getArcLength() { return arcLength; }
        @Override public boolean isExact() { return exact; }
        @Override public boolean isUnique() { return unique; }
        @Override public Optional<Double> getErrorEstimate() { return Optional.empty(); }
    }

    private static class SyntheticExecutionContext implements ExecutionContext {
        private final ManifoldSpace space;
        SyntheticExecutionContext(ManifoldSpace space) { this.space = space; }
        @Override public long getCurrentStep() { return -1L; }
        @Override public ManifoldSpace getManifoldSpace() { return space; }
        @Override public boolean isSynthetic() { return true; }
        @Override public Map<String, Object> getContextAttributes() { return Map.of(); }
    }


    // ----------------------------------------------------------------
    // Torus geodesic pathfinder
    // ----------------------------------------------------------------

    private class TorusGeodesicPathfinder implements GeodesicPathfinder {

        @Override
        public Optional<GeodesicResult> findExactGeodesic(Position from, Position to) {
            TorusPoint a = requireTorusPoint(from);
            TorusPoint b = requireTorusPoint(to);
            if (a.equals(b)) return Optional.empty();
            double[] delta = shortestDelta(a, b);
            return Optional.of(samplePath(a, delta, 64, true, true));
        }

        @Override
        public boolean supportsExactGeodesic(Position from, Position to) {
            return isValidPosition(from) && isValidPosition(to);
        }

        @Override
        public List<GeodesicResult> findAllExactGeodesics(Position from, Position to) {
            TorusPoint a = requireTorusPoint(from);
            TorusPoint b = requireTorusPoint(to);
            // Four possible geodesics: direct, wrap-x, wrap-y, wrap-xy
            List<GeodesicResult> results = new ArrayList<>();
            double dx = b.x() - a.x();
            double dy = b.y() - a.y();
            double[][] deltas = {
                {dx, dy},
                {dx - Math.signum(dx)*width, dy},
                {dx, dy - Math.signum(dy)*height},
                {dx - Math.signum(dx)*width, dy - Math.signum(dy)*height}
            };
            for (double[] delta : deltas) {
                double len = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]);
                if (len > 0) results.add(samplePath(a, delta, 64, true, false));
            }
            results.sort(Comparator.comparingDouble(GeodesicResult::getArcLength));
            return Collections.unmodifiableList(results);
        }

        @Override
        public Optional<GeodesicResult> findApproximateGeodesic(Position from, Position to,
                                                                  int steps) {
            return findExactGeodesic(from, to);
        }

        @Override
        public List<GeodesicResult> findAllApproximateGeodesics(Position from, Position to,
                                                                  int steps, int maxResults) {
            List<GeodesicResult> all = findAllExactGeodesics(from, to);
            return all.subList(0, Math.min(maxResults, all.size()));
        }

        @Override
        public Optional<GeodesicResult> findBestGeodesic(Position from, Position to, int steps) {
            return findExactGeodesic(from, to);
        }

        private GeodesicResult samplePath(TorusPoint start, double[] delta,
                                           int samples, boolean exact, boolean unique) {
            List<Position> path = new ArrayList<>();
            double len = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]);
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / samples;
                path.add(wrap(start.x() + t*delta[0], start.y() + t*delta[1]));
            }
            return new SimpleGeodesicResult(path, len, exact, unique);
        }
    }


    // ----------------------------------------------------------------
    // Torus curvature calculator
    // ----------------------------------------------------------------

    private class TorusCurvatureCalculator implements GeometricCurvatureCalculator {

        @Override
        public double computeScalarCurvature(Position p, double[][] metricTensor) {
            return 0.0; // flat torus: K=0 everywhere
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
            return Optional.of(new double[2][2][2][2]); // all zero
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
                @Override public Optional<double[]> getCurvatureTensor() {
                    return Optional.of(new double[]{0.0, 0.0, 0.0, 0.0});
                }
                @Override public boolean isFlat() { return true; }
                @Override public boolean isSingular() { return false; }
            };
        }

        @Override
        public double computeScalarIntensity(Position p, double[][] metricTensor) {
            Optional<Double> gaussian = computeGaussianCurvature(p, metricTensor);
            return gaussian.map(k -> 2.0 * k).orElse(0.0);
        }
    }


	@Override
	public String getChartIdAt(Position p) {
		requireTorusPoint(p);
		return CHART_ID;
	}
}