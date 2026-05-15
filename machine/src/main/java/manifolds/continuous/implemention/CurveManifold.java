package manifolds.continuous.implemention;

import java.util.*;

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

/**
 * CurveManifold — a 1D manifold embedded in 2D normalized space.
 *
 * Built from a closed, arc-length-sampled curve drawn by the user.
 *
 * HOLONOMY POLICY: PRESERVE
 * COORDINATE CONVENTION: Position.getCoordinates() = [s, x, y]
 * MOTION CONVENTION: MotionVector.getComponents() = [ds]
 */
public class CurveManifold implements ManifoldSpace {

    // ----------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------

    private static final double SINGULARITY_THRESHOLD  = 50.0;
    private static final double FLATNESS_THRESHOLD     = 0.5;
    private static final int    GEODESIC_SAMPLE_STEPS  = 64;

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    private final String          name;
    private final List<double[]>  points;
    private final double[]        arcLengths;
    private final double[][]      tangents;
    private final double[]        curvatures;
    private final double          totalArcLength;
    private final int             n;

    private final ManifoldDescriptor descriptor;
    private final RegionCapabilities capabilities;
    private final RegionPolicy       policy;

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    public CurveManifold(String name, List<double[]> pts) {
        if (pts == null || pts.size() < 4)
            throw new IllegalArgumentException("CurveManifold requires at least 4 points.");

        this.name  = name;
        this.n     = pts.size();

        // Defensive copy
        List<double[]> copy = new ArrayList<>(n);
        for (double[] p : pts) copy.add(Arrays.copyOf(p, 2));
        this.points = Collections.unmodifiableList(copy);

        this.arcLengths     = computeArcLengths(this.points);
        this.totalArcLength = arcLengths[n - 1];
        this.tangents       = computeTangents(this.points);
        this.curvatures     = computeCurvatures(this.tangents, this.arcLengths);

        this.descriptor  = new CurveDescriptor(name);
        this.capabilities = new CurveCapabilities();
        this.policy       = new CurvePolicy();
    }

    // ----------------------------------------------------------------
    // ManifoldSpace — identity
    // ----------------------------------------------------------------

    @Override
    public ManifoldDescriptor getDescriptor() {
        return descriptor;
    }

    // ----------------------------------------------------------------
    // ManifoldSpace — position and geometry
    // ----------------------------------------------------------------

    @Override
    public Position origin() {
        return curvePosition(0.0);
    }

    @Override
    public boolean isValidPosition(Position p) {
        if (p == null) return false;
        double[] coords = p.getCoordinates();
        if (coords == null || coords.length < 1) return false;
        double s = coords[0];
        return s >= 0.0 && s <= totalArcLength;
    }

    @Override
    public int getLocalManifoldDimensionality(Position p) {
        return 1;
    }

    @Override
    public CurvatureField getCurvatureAt(Position p) {
        double s   = extractS(p);
        int    idx = nearestIndex(s);
        return new CurveCurvatureField(
            curvatures[idx],
            isSingularAt(idx),
            isFlatAt(idx)
        );
    }

    @Override
    public RegionCapabilities getCapabilitiesAt(Position p) {
        return capabilities;
    }

    @Override
    public RegionPolicy getPolicyAt(Position p) {
        return policy;
    }

    // ----------------------------------------------------------------
    // ManifoldSpace — movement
    // ----------------------------------------------------------------

    @Override
    public Position move(Position from, MotionVector vector) {
        double s  = extractS(from);
        double ds = vector.getComponents()[0];
        return curvePosition(wrapS(s + ds));
    }

    @Override
    public MotionAdaptationResult adaptMotion(Position at, MotionVector vector) {
        int      idx     = nearestIndex(extractS(at));
        double[] tangent = tangents[idx];
        double[] comps   = vector.getComponents();

        // Input may be 1D [ds] or 2D [vx, vy]
        double vx, vy;
        if (comps.length >= 2) {
            vx = comps[0];
            vy = comps[1];
        } else {
            // Already a 1D curve vector — no adaptation needed
            return new CurveAdaptationResult(vector, false, null);
        }

        // Project onto tangent: ds = v · t
        double ds       = vx * tangent[0] + vy * tangent[1];
        double origMag  = Math.sqrt(vx*vx + vy*vy);
        double projMag  = Math.abs(ds);
        boolean modified = Math.abs(origMag - projMag) > 1e-9;

        MotionVector adapted = new CurveMotionVector(ds);
        String reason = modified
            ? "Transverse component removed — projected onto curve tangent"
            : null;

        return new CurveAdaptationResult(adapted, modified, reason);
    }

    @Override
    public MotionVector parallelTransport(Position from, Position to, MotionVector vector) {
        int    fromIdx = nearestIndex(extractS(from));
        int    toIdx   = nearestIndex(extractS(to));
        double[] tFrom = tangents[fromIdx];
        double[] tTo   = tangents[toIdx];
        double[] comps = vector.getComponents();

        double vx = comps.length >= 2 ? comps[0] : comps[0] * tFrom[0];
        double vy = comps.length >= 2 ? comps[1] : comps[0] * tFrom[1];

        // Project onto departure tangent
        double proj = vx * tFrom[0] + vy * tFrom[1];

        // Re-express along arrival tangent as ds
        return new CurveMotionVector(proj * Math.signum(tTo[0] * tFrom[0] + tTo[1] * tFrom[1]));
    }

    @Override
    public Optional<MotionVector> computeHolonomy(List<Position> closedPath) {
        if (closedPath == null || closedPath.size() < 2)
            return Optional.of(new CurveMotionVector(0.0));

        double totalDTheta = 0.0;
        for (int i = 0; i < closedPath.size() - 1; i++) {
            int idxA = nearestIndex(extractS(closedPath.get(i)));
            int idxB = nearestIndex(extractS(closedPath.get(i + 1)));
            double thetaA = Math.atan2(tangents[idxA][1], tangents[idxA][0]);
            double thetaB = Math.atan2(tangents[idxB][1], tangents[idxB][0]);
            totalDTheta  += angleDiff(thetaB, thetaA);
        }

        return Optional.of(new CurveMotionVector(totalDTheta));
    }

    // ----------------------------------------------------------------
    // ManifoldSpace — neighborhood and transitions
    // ----------------------------------------------------------------

    @Override
    public List<Transition> getTransitionsFrom(Position p, TransitionQuery query) {
        double s      = extractS(p);
        int    idx    = nearestIndex(s);
        int    max    = query.getMaxResults();

        int    fwdIdx = (idx + 1) % n;
        int    bwdIdx = (idx - 1 + n) % n;

        Position fwdPos  = curvePosition(arcLengths[fwdIdx]);
        Position bwdPos  = curvePosition(arcLengths[bwdIdx]);

        double fwdCost   = Math.abs(arcLengths[fwdIdx] - arcLengths[idx]);
        double bwdCost   = Math.abs(arcLengths[idx] - arcLengths[bwdIdx]);

        // Seam correction
        if (idx == n - 1) fwdCost = totalArcLength - arcLengths[idx] + arcLengths[0];
        if (idx == 0)     bwdCost = arcLengths[idx] + totalArcLength - arcLengths[n - 1];

        List<Transition> result = new ArrayList<>();
        result.add(new CurveTransition(p, fwdPos, fwdCost, "curve_step_forward"));
        if (max > 1 || query.includeNonTraversable()) {
            result.add(new CurveTransition(p, bwdPos, bwdCost, "curve_step_backward"));
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public NeighborhoodQuery neighborhoodQuery(Position center, double radius) {
        double         s         = extractS(center);
        List<Position> nearby    = new ArrayList<>();
        List<Transition> trans   = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            double ds = Math.min(
                Math.abs(arcLengths[i] - s),
                totalArcLength - Math.abs(arcLengths[i] - s)
            );
            if (ds <= radius) {
                Position pos = curvePosition(arcLengths[i]);
                nearby.add(pos);
                trans.add(new CurveTransition(center, pos, ds, "curve_step_forward"));
            }
        }

        final List<Position>   finalNearby = Collections.unmodifiableList(nearby);
        final List<Transition> finalTrans  = Collections.unmodifiableList(trans);
        final double           finalRadius = radius;

        return new NeighborhoodQuery() {
            @Override public List<Position>   getPositionsInRadius()    { return finalNearby; }
            @Override public List<Transition> getAvailableTransitions() { return finalTrans; }
            @Override public double           getRadius()               { return finalRadius; }
            @Override public List<Flatlander> getNearbyFlatlanders()    { return Collections.emptyList(); }
            @Override public boolean          isFrozen()                { return false; }
            @Override public OptionalLong     getCapturedAtStep()       { return OptionalLong.empty(); }
            @Override public List<Position>   getNearbyPositions()      { return finalNearby; }
        };
    }

    // ----------------------------------------------------------------
    // ManifoldSpace — geodesics
    // ----------------------------------------------------------------

    @Override
    public Optional<List<Position>> findGeodesic(Position from, Position to) {
        double sFrom = wrapS(extractS(from));
        double sTo   = wrapS(extractS(to));

        double fwdLen = sTo >= sFrom
            ? sTo - sFrom
            : (totalArcLength - sFrom) + sTo;
        double bwdLen = totalArcLength - fwdLen;

        boolean goForward = fwdLen <= bwdLen;
        double  arcLen    = goForward ? fwdLen : bwdLen;

        List<Position> path = new ArrayList<>();
        for (int i = 0; i <= GEODESIC_SAMPLE_STEPS; i++) {
            double t  = (double) i / GEODESIC_SAMPLE_STEPS;
            double ds = goForward ? t * arcLen : -t * arcLen;
            path.add(curvePosition(wrapS(sFrom + ds)));
        }

        return Optional.of(Collections.unmodifiableList(path));
    }

    @Override
    public double geodesicDistance(Position from, Position to) {
        double sFrom  = wrapS(extractS(from));
        double sTo    = wrapS(extractS(to));
        double fwdLen = sTo >= sFrom
            ? sTo - sFrom
            : (totalArcLength - sFrom) + sTo;
        return Math.min(fwdLen, totalArcLength - fwdLen);
    }

    // ----------------------------------------------------------------
    // ManifoldSpace — execution context
    // ----------------------------------------------------------------

    @Override
    public ExecutionContext defaultContext() {
        final ManifoldSpace self = this;
        return new ExecutionContext() {
            @Override public long            getCurrentStep()      { return -1L; }
            @Override public ManifoldSpace   getManifoldSpace()    { return self; }
            @Override public boolean         isSynthetic()         { return true; }
            @Override public Map<String, Object> getContextAttributes() {
                return Collections.singletonMap("synthetic", true);
            }
        };
    }

    // ----------------------------------------------------------------
    // ManifoldSpace — debug
    // ----------------------------------------------------------------

    @Override
    public Optional<PositionDebug> debugProject(Position p) {
        double[] coords = p.getCoordinates();
        if (coords == null || coords.length < 3) return Optional.empty();

        final double px = coords[1];
        final double py = coords[2];
        final String label = String.format("s=%.3f", coords[0]);

        return Optional.of(new PositionDebug() {
            @Override public double getX()     { return px; }
            @Override public double getY()     { return py; }
            @Override public String getLabel() { return label; }
        });
    }

    // ----------------------------------------------------------------
    // Public accessors for pipeline (CurvatureGenerator etc.)
    // ----------------------------------------------------------------

    public int     getPointCount()             { return n; }
    public double  getTotalArcLength()         { return totalArcLength; }
    public double[] getPoint(int i)            { return Arrays.copyOf(points.get(i), 2); }
    public double  getArcLengthAt(int i)       { return arcLengths[i]; }
    public double  getCurvatureValueAt(int i)  { return curvatures[i]; }
    public double[] getTangentAt(int i)        { return Arrays.copyOf(tangents[i], 2); }
    public boolean isSingularAt(int i)         { return Math.abs(curvatures[i]) > SINGULARITY_THRESHOLD; }
    public boolean isFlatAt(int i)             { return Math.abs(curvatures[i]) < FLATNESS_THRESHOLD; }

    // ----------------------------------------------------------------
    // Geometry computations
    // ----------------------------------------------------------------

    private double[] computeArcLengths(List<double[]> pts) {
        double[] s = new double[n];
        s[0] = 0.0;
        for (int i = 1; i < n; i++) {
            double dx = pts.get(i)[0] - pts.get(i-1)[0];
            double dy = pts.get(i)[1] - pts.get(i-1)[1];
            s[i] = s[i-1] + Math.sqrt(dx*dx + dy*dy);
        }
        return s;
    }

    private double[][] computeTangents(List<double[]> pts) {
        double[][] t = new double[n][2];
        for (int i = 0; i < n; i++) {
            int    prev = (i - 1 + n) % n;
            int    next = (i + 1) % n;
            double dx   = pts.get(next)[0] - pts.get(prev)[0];
            double dy   = pts.get(next)[1] - pts.get(prev)[1];
            double len  = Math.sqrt(dx*dx + dy*dy);
            if (len < 1e-12) { t[i][0] = 1.0; t[i][1] = 0.0; }
            else              { t[i][0] = dx/len; t[i][1] = dy/len; }
        }
        return t;
    }

    private double[] computeCurvatures(double[][] tans, double[] s) {
        double[] kappa = new double[n];
        for (int i = 0; i < n; i++) {
            int    prev     = (i - 1 + n) % n;
            int    next     = (i + 1) % n;
            double thetaPrev = Math.atan2(tans[prev][1], tans[prev][0]);
            double thetaNext = Math.atan2(tans[next][1], tans[next][0]);
            double dTheta    = angleDiff(thetaNext, thetaPrev);

            // Arc-length span between prev and next, with seam handling
            double ds;
            if (i == 0) {
                // prev is n-1, next is 1 — span crosses seam
                ds = s[1] + (s[n-1] > 0 ? (totalArcLength - s[n-1]) : 0);
            } else if (i == n - 1) {
                // interpolate from neighbors to avoid seam spike
                kappa[i] = (kappa[i-1] + kappa[1]) / 2.0;
                continue;
            } else {
                ds = s[next] - s[prev];
            }

            kappa[i] = (Math.abs(ds) < 1e-12) ? 0.0 : dTheta / ds;
        }
        return kappa;
    }

    private double angleDiff(double a, double b) {
        double d = a - b;
        while (d >  Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return d;
    }

    private double wrapS(double s) {
        if (totalArcLength < 1e-12) return 0.0;
        s = s % totalArcLength;
        if (s < 0) s += totalArcLength;
        return s;
    }

    private int nearestIndex(double s) {
        s = wrapS(s);
        int lo = 0, hi = n - 1;
        while (lo < hi - 1) {
            int mid = (lo + hi) / 2;
            if (arcLengths[mid] <= s) lo = mid;
            else hi = mid;
        }
        return Math.abs(arcLengths[lo] - s) <= Math.abs(arcLengths[hi] - s) ? lo : hi;
    }

    private double[] interpolatePoint(double s) {
        s = wrapS(s);
        int lo = 0, hi = n - 1;
        while (lo < hi - 1) {
            int mid = (lo + hi) / 2;
            if (arcLengths[mid] <= s) lo = mid;
            else hi = mid;
        }
        double segLen = arcLengths[hi] - arcLengths[lo];
        double t      = (segLen < 1e-12) ? 0.0 : (s - arcLengths[lo]) / segLen;
        double x = points.get(lo)[0] + t * (points.get(hi)[0] - points.get(lo)[0]);
        double y = points.get(lo)[1] + t * (points.get(hi)[1] - points.get(lo)[1]);
        return new double[]{ x, y };
    }

    private double extractS(Position p) {
        double[] coords = p.getCoordinates();
        return (coords != null && coords.length >= 1) ? coords[0] : 0.0;
    }

    private Position curvePosition(double s) {
        s = wrapS(s);
        double[] xy = interpolatePoint(s);
        return new CurvePoint(s, xy[0], xy[1]);
    }

    // ----------------------------------------------------------------
    // Inner types
    // ----------------------------------------------------------------

    private static class CurvePoint implements Position {
        private final double[] coords; // [s, x, y]

        CurvePoint(double s, double x, double y) {
            this.coords = new double[]{ s, x, y };
        }

        @Override public double[] getCoordinates()             { return Arrays.copyOf(coords, 3); }
        @Override public int      getRepresentationDimensionality() { return 3; }
        @Override public boolean  isValid()                    { return coords[0] >= 0; }
    }

    private static class CurveMotionVector implements MotionVector {
        private final double ds;

        CurveMotionVector(double ds) { this.ds = ds; }

        @Override public double[]     getComponents() { return new double[]{ ds }; }
        @Override public double       getMagnitude()  { return Math.abs(ds); }
        @Override public MotionVector scale(double f) { return new CurveMotionVector(ds * f); }
        @Override public MotionVector add(MotionVector other) {
            double[] c = other.getComponents();
            return new CurveMotionVector(ds + (c.length > 0 ? c[0] : 0.0));
        }
    }

    private static class CurveDescriptor implements ManifoldDescriptor {
        private final String name;

        CurveDescriptor(String name) { this.name = name; }

        @Override public String              getName()                   { return name; }
        @Override public String              getBackendType()            { return "continuous"; }
        @Override public String              getTopologyType()           { return "closed_curve"; }
        @Override public int                 getIntrinsicDimensionality(){ return 1; }
        @Override public Map<String, Object> getProperties()            { return Collections.emptyMap(); }
    }

    private static class CurveCapabilities implements RegionCapabilities {
        @Override public boolean canSpawn()         { return true;  }
        @Override public boolean canMerge()         { return false; }
        @Override public boolean canSplit()         { return false; }
        @Override public boolean allowsLayerJump()  { return false; }
        @Override public boolean allowsWraparound() { return true;  }
        @Override public boolean isTraversable()    { return true;  }
    }

    private static class CurvePolicy implements RegionPolicy {
        @Override public double getOperationWeight(String op) { return 1.0; }
        @Override public Map<String, Double> getPolicyWeights() { return Collections.emptyMap(); }
        @Override public Optional<MotionVector> getSuggestedCorrection(MotionVector v) {
            return Optional.empty();
        }
        @Override public boolean allowsOperation(String op) { return true; }
    }

    private static class CurveCurvatureField implements CurvatureField {
        private final double  kappa;
        private final boolean singular;
        private final boolean flat;

        CurveCurvatureField(double kappa, boolean singular, boolean flat) {
            this.kappa    = kappa;
            this.singular = singular;
            this.flat     = flat;
        }

        @Override public double              getScalarIntensity()    { return kappa; }
        @Override public Optional<double[]>  getCurvatureTensor()    { return Optional.of(new double[]{ kappa }); }
        @Override public boolean             isFlat()                { return flat; }
        @Override public boolean             isSingular()            { return singular; }
    }

    private static class CurveAdaptationResult implements MotionAdaptationResult {
        private final MotionVector adapted;
        private final boolean      modified;
        private final String       reason;

        CurveAdaptationResult(MotionVector adapted, boolean modified, String reason) {
            this.adapted  = adapted;
            this.modified = modified;
            this.reason   = reason;
        }

        @Override public MotionVector getAdaptedVector()    { return adapted; }
        @Override public boolean      wasModified()         { return modified; }
        @Override public String       getModificationReason() { return reason != null ? reason : ""; }
    }

    private static class CurveTransition implements Transition {
        private final Position source;
        private final Position target;
        private final double   cost;
        private final String   type;

        CurveTransition(Position source, Position target, double cost, String type) {
            this.source = source;
            this.target = target;
            this.cost   = cost;
            this.type   = type;
        }

        @Override public Position             getSource()         { return source; }
        @Override public Position             getTarget()         { return target; }
        @Override public double               getCost()           { return cost; }
        @Override public boolean              isTraversable()     { return true; }
        @Override public String               getTransitionType() { return type; }
        @Override public Map<String, Double>  getAttributes()     { return Collections.emptyMap(); }
    }
}