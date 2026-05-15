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
 * A cylindrical manifold — periodic in the angular (x) dimension,
 * unbounded in the axial (y) dimension.
 *
 * HOLONOMY POLICY: PRESERVE
 * The cylinder is flat (K=0) but has a non-contractible loop in the angular direction.
 * A flatlander traversing a full angular loop returns with no holonomy (cylinder is flat),
 * but the topology permits winding numbers in the angular direction.
 *
 * CHART: single chart with wraparound in x: [0, circumference) x (-inf, +inf)
 * GEODESICS: exact — helical lines (straight lines on the unrolled cylinder)
 * BASIS CONVENTION: orthonormal {(1,0), (0,1)} — angular and axial
 * PATH SEMANTICS: shortest path on unrolled cylinder (helix)
 *
 * TOPOLOGY NOTE: unlike the torus, the cylinder has one non-contractible loop
 * (angular) and one contractible direction (axial). Gaussian curvature K=0.
 */
public class CylindricalManifold implements ContinuousManifold {

    private static final String CHART_ID      = "cylinder_chart";
    private static final String BACKEND_TYPE  = CommonBackendTypes.CONTINUOUS;
    private static final String TOPOLOGY_TYPE = CommonTopologyTypes.CYLINDRICAL;

    private final double circumference;
    private final ManifoldDescriptor descriptor;
    private final GeodesicPathfinder geodesicPathfinder;
    private final GeometricCurvatureCalculator curvatureCalculator;

    public CylindricalManifold(String name, double circumference) {
        this.circumference       = circumference;
        this.descriptor          = buildDescriptor(name);
        this.geodesicPathfinder  = new CylinderGeodesicPathfinder();
        this.curvatureCalculator = new CylinderCurvatureCalculator();
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
        return point(0.0, 0.0);
    }

    @Override
    public boolean isValidPosition(Position p) {
        return p instanceof CylinderPoint;
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
        return new CylinderCapabilities();
    }

    @Override
    public RegionPolicy getPolicyAt(Position p) {
        return new CylinderPolicy();
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — movement
    // ----------------------------------------------------------------

    @Override
    public Position move(Position from, MotionVector vector) {
        CylinderPoint cp = requireCylinderPoint(from);
        double[] v       = vector.getComponents();
        return point(cp.theta() + v[0], cp.y() + v[1]);
    }

    @Override
    public MotionAdaptationResult adaptMotion(Position at, MotionVector vector) {
        return new SimpleMotionAdaptationResult(vector, false, "cylinder: no adaptation needed");
    }

    /**
     * HOLONOMY POLICY: PRESERVE
     * Cylinder is flat — parallel transport never rotates vectors.
     * The angular wraparound does not accumulate holonomy (K=0).
     */
    @Override
    public MotionVector parallelTransport(Position from, Position to, MotionVector vector) {
        return vector; // flat cylinder: no holonomy
    }

    @Override
    public Optional<MotionVector> computeHolonomy(List<Position> closedPath) {
        // Flat cylinder: no holonomy on any loop (K=0 everywhere)
        return Optional.of(new FlatPlane.SimpleMotionVector(new double[]{0.0, 0.0}));
    }


    // ----------------------------------------------------------------
    // ManifoldSpace — transitions and neighborhood
    // ----------------------------------------------------------------

    @Override
    public List<Transition> getTransitionsFrom(Position p, TransitionQuery query) {
        CylinderPoint cp = requireCylinderPoint(p);
        double step      = 1.0;
        double nextTheta = wrapTheta(cp.theta() + step);
        double prevTheta = wrapTheta(cp.theta() - step);

        List<Transition> transitions = new ArrayList<>();
        boolean wrapRight = nextTheta < cp.theta();
        boolean wrapLeft  = prevTheta > cp.theta();

        transitions.add(new SimpleTransition(p, point(nextTheta, cp.y()),
            wrapRight ? CommonTransitionTypes.WRAPAROUND : CommonTransitionTypes.LOCAL_STEP, step));
        transitions.add(new SimpleTransition(p, point(prevTheta, cp.y()),
            wrapLeft  ? CommonTransitionTypes.WRAPAROUND : CommonTransitionTypes.LOCAL_STEP, step));
        transitions.add(new SimpleTransition(p, point(cp.theta(), cp.y() + step),
            CommonTransitionTypes.LOCAL_STEP, step));
        transitions.add(new SimpleTransition(p, point(cp.theta(), cp.y() - step),
            CommonTransitionTypes.LOCAL_STEP, step));

        return Collections.unmodifiableList(transitions);
    }

    @Override
    public NeighborhoodQuery neighborhoodQuery(Position center, double radius) {
        CylinderPoint cp = requireCylinderPoint(center);
        List<Position> nearby = new ArrayList<>();
        for (double dt = -radius; dt <= radius; dt += 1.0) {
            for (double dy = -radius; dy <= radius; dy += 1.0) {
                if (dt*dt + dy*dy <= radius*radius && !(dt == 0 && dy == 0)) {
                    nearby.add(point(cp.theta() + dt, cp.y() + dy));
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
        return geodesicPathfinder.findExactGeodesic(from, to)
            .map(GeodesicPathfinder.GeodesicResult::getPath);
    }

    @Override
    public double geodesicDistance(Position from, Position to) {
        CylinderPoint a = requireCylinderPoint(from);
        CylinderPoint b = requireCylinderPoint(to);
        // Shortest angular delta with wraparound
        double dTheta = Math.min(
            Math.abs(b.theta() - a.theta()),
            circumference - Math.abs(b.theta() - a.theta())
        );
        double dy = b.y() - a.y();
        return Math.sqrt(dTheta*dTheta + dy*dy);
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
        CylinderPoint cp = requireCylinderPoint(p);
        // Project onto 2D: theta -> x, y -> y
        return Optional.of(new SimplePositionDebug(cp.theta(), cp.y(),
            String.format("cyl(θ=%.2f, y=%.2f)", cp.theta(), cp.y())));
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
        // Flat cylinder: identity metric in (theta, y) coordinates
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
        // Orthonormal basis: angular direction and axial direction
        return List.of(
            new FlatPlane.SimpleMotionVector(new double[]{1.0, 0.0}), // angular
            new FlatPlane.SimpleMotionVector(new double[]{0.0, 1.0})  // axial
        );
    }

    @Override
    public MotionVector projectToTangentSpace(Position p, MotionVector v) {
        return v; // flat cylinder: all 2D vectors tangent
    }

    @Override
    public Optional<MotionVector> getNormalVector(Position p) {
        // Cylinder embedded in 3D: normal points radially outward
        // In (theta, y) coordinates, express as 3D unit radial vector
        CylinderPoint cp = requireCylinderPoint(p);
        double angle = (cp.theta() / circumference) * 2.0 * Math.PI;
        return Optional.of(new FlatPlane.SimpleMotionVector(
            new double[]{Math.cos(angle), 0.0, Math.sin(angle)}
        ));
    }


    // ----------------------------------------------------------------
    // ContinuousManifold — integration
    // ----------------------------------------------------------------

    @Override
    public Optional<Double> integrateAlongPath(Position from, Position to,
                                                Function<Position, Double> field, int steps) {
        // PATH SEMANTICS: shortest helical geodesic on cylinder
        CylinderPoint a = requireCylinderPoint(from);
        CylinderPoint b = requireCylinderPoint(to);
        double[] delta  = shortestDelta(a, b);
        double sum = 0.0;
        double ds  = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]) / steps;
        for (int i = 0; i <= steps; i++) {
            double t   = (double) i / steps;
            Position p = point(a.theta() + t*delta[0], a.y() + t*delta[1]);
            double w   = (i == 0 || i == steps) ? 0.5 : 1.0;
            sum += w * field.apply(p) * ds;
        }
        return Optional.of(sum);
    }

    @Override
    public Optional<MotionVector> integrateVectorAlongPath(Position from, Position to,
                                                            Function<Position, MotionVector> field,
                                                            int steps) {
        CylinderPoint a = requireCylinderPoint(from);
        CylinderPoint b = requireCylinderPoint(to);
        double[] delta  = shortestDelta(a, b);
        double[] acc    = {0.0, 0.0};
        double ds = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]) / steps;
        for (int i = 0; i <= steps; i++) {
            double t   = (double) i / steps;
            Position p = point(a.theta() + t*delta[0], a.y() + t*delta[1]);
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

    private CylinderPoint point(double theta, double y) {
        return new CylinderPoint(wrapTheta(theta), y, CHART_ID);
    }

    private double wrapTheta(double theta) {
        return ((theta % circumference) + circumference) % circumference;
    }

    private double[] shortestDelta(CylinderPoint a, CylinderPoint b) {
        double dTheta = b.theta() - a.theta();
        if (Math.abs(dTheta) > circumference / 2)
            dTheta -= Math.signum(dTheta) * circumference;
        return new double[]{dTheta, b.y() - a.y()};
    }

    private CylinderPoint requireCylinderPoint(Position p) {
        if (!(p instanceof CylinderPoint cp))
            throw new IllegalArgumentException("Position must be a CylinderPoint: " + p);
        return cp;
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
                return Map.of("circumference", circumference);
            }
        };
    }


    // ----------------------------------------------------------------
    // Inner value types
    // ----------------------------------------------------------------

    public record CylinderPoint(double theta, double y, String chartId) implements ManifoldPoint {
        @Override public double[] getCoordinates() { return new double[]{theta, y}; }
        @Override public int getRepresentationDimensionality() { return 2; }
        @Override public int getDimensionality() { return 2; }
        @Override public boolean isValid() { return true; }
        @Override public String getChartId() { return chartId; }
    }

    private record CylinderCapabilities() implements RegionCapabilities {
        @Override public boolean canSpawn() { return true; }
        @Override public boolean canMerge() { return true; }
        @Override public boolean canSplit() { return true; }
        @Override public boolean allowsLayerJump() { return false; }
        @Override public boolean allowsWraparound() { return true; }
        @Override public boolean isTraversable() { return true; }
    }

    private record CylinderPolicy() implements RegionPolicy {
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
    // Cylinder geodesic pathfinder
    // ----------------------------------------------------------------

    private class CylinderGeodesicPathfinder implements GeodesicPathfinder {

        @Override
        public Optional<GeodesicResult> findExactGeodesic(Position from, Position to) {
            CylinderPoint a = requireCylinderPoint(from);
            CylinderPoint b = requireCylinderPoint(to);
            if (a.equals(b)) return Optional.empty();
            double[] delta  = shortestDelta(a, b);
            double length   = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]);
            List<Position> path = new ArrayList<>();
            int samples = 64;
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / samples;
                path.add(point(a.theta() + t*delta[0], a.y() + t*delta[1]));
            }
            return Optional.of(new SimpleGeodesicResult(path, length, true, true));
        }

        @Override
        public boolean supportsExactGeodesic(Position from, Position to) {
            return isValidPosition(from) && isValidPosition(to);
        }

        @Override
        public List<GeodesicResult> findAllExactGeodesics(Position from, Position to) {
            CylinderPoint a = requireCylinderPoint(from);
            CylinderPoint b = requireCylinderPoint(to);
            List<GeodesicResult> results = new ArrayList<>();
            // Two angular directions: shortest and longest wrap
            double dTheta = b.theta() - a.theta();
            double[] shortDelta = shortestDelta(a, b);
            double[] longDelta  = {
                dTheta - Math.signum(dTheta) * circumference,
                b.y() - a.y()
            };
            for (double[] delta : new double[][]{shortDelta, longDelta}) {
                double len = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]);
                if (len > 0) {
                    List<Position> path = new ArrayList<>();
                    for (int i = 0; i <= 64; i++) {
                        double t = (double) i / 64;
                        path.add(point(a.theta() + t*delta[0], a.y() + t*delta[1]));
                    }
                    results.add(new SimpleGeodesicResult(path, len, true, false));
                }
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
    }


    // ----------------------------------------------------------------
    // Cylinder curvature calculator
    // ----------------------------------------------------------------

    private class CylinderCurvatureCalculator implements GeometricCurvatureCalculator {

        @Override
        public double computeScalarCurvature(Position p, double[][] metricTensor) {
            return 0.0; // cylinder is flat: K=0
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
            return Optional.of(new double[2][2][2][2]);
        }

        @Override
        public Optional<double[][]> computeRicciTensor(Position p, double[][] metricTensor) {
            return Optional.of(new double[][]{{0.0, 0.0}, {0.0, 0.0}});
        }

        @Override
        public Optional<Double> computeGaussianCurvature(Position p, double[][] metricTensor) {
            return Optional.of(0.0); // intrinsically flat
        }

        @Override
        public Optional<double[]> computePrincipalCurvatures(Position p, double[][] metricTensor) {
            // Cylinder embedded in 3D: one principal curvature is 1/r, other is 0
            double r = circumference / (2.0 * Math.PI);
            return Optional.of(new double[]{0.0, 1.0 / r}); // k1 <= k2
        }

        @Override
        public Optional<Double> computeMeanCurvature(Position p, double[][] metricTensor) {
            // H = (k1 + k2) / 2 = (0 + 1/r) / 2
            double r = circumference / (2.0 * Math.PI);
            return Optional.of(1.0 / (2.0 * r));
        }

        @Override
        public CurvatureField buildCurvatureField(Position p, double[][] metricTensor) {
            double r = circumference / (2.0 * Math.PI);
            double meanCurv = 1.0 / (2.0 * r);
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
requireCylinderPoint(p);		return CHART_ID;
	}
}