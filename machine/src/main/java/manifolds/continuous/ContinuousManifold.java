package manifolds.continuous;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import machine.ManifoldSpace;
import machine.MotionVector;
import machine.Position;

/**
 * A ManifoldSpace implemented as a continuous coordinate space.
 *
 * Extends ManifoldSpace with continuous-specific operations:
 *   - chart transitions
 *   - metric tensor access
 *   - local coordinate frame queries
 *   - integration along paths
 *
 * Concrete implementations define topology:
 *   FlatPlane, ToroidalManifold, CylindricalManifold, FlatlandCube
 *
 * HOLONOMY POLICY: defined per implementation.
 *   Flat manifolds:   PRESERVE (trivially — no holonomy accumulates)
 *   Curved manifolds: must document PRESERVE, DAMP, or CORRECT
 */
public interface ContinuousManifold extends ManifoldSpace {

    // ----------------------------------------------------------------
    // Chart transitions
    // ----------------------------------------------------------------

    /**
     * Returns the preferred chart ID covering the given position.
     * If multiple charts overlap at this position, the implementation
     * must choose one consistently and document its selection strategy.
     *
     * CONVENTION: chartId values are candidates for CommonChartTypes (v1.1).
     */
    String getPrimaryChartIdAt(Position p);

    /**
     * Returns all chart IDs covering the given position.
     * Relevant for overlapping atlas charts near boundaries.
     * For single-chart manifolds, always returns a singleton list.
     */
    List<String> getChartsAt(Position p);

    /**
     * Transitions a ManifoldPoint from its current chart to the target chart.
     * Returns empty if the transition is not defined or the point does not
     * lie in the domain of the target chart.
     *
     * CONVENTION: chartId values are candidates for CommonChartTypes (v1.1).
     */
    Optional<ManifoldPoint> transitionToChart(ManifoldPoint point, String targetChartId);


    // ----------------------------------------------------------------
    // Metric tensor
    // ----------------------------------------------------------------

    /**
     * Returns the metric tensor at the given position as a 2D array.
     *
     * CONTRACT:
     *   - returned matrix is square: [n x n] where n = getLocalManifoldDimensionality(p)
     *   - callers must treat the returned array as read-only
     *   - implementations may return a defensive copy
     *   - the tensor is symmetric (g_ij == g_ji)
     *   - for flat manifolds, this is the identity matrix
     */
    double[][] getMetricTensor(Position p);

    /**
     * Computes the inner product of two tangent vectors at a position
     * using the local metric tensor: g(u, v).
     */
    double innerProduct(Position p, MotionVector u, MotionVector v);

    /**
     * Computes the norm of a tangent vector at a position
     * using the local metric tensor: sqrt(g(v, v)).
     */
    double norm(Position p, MotionVector v);


    // ----------------------------------------------------------------
    // Local coordinate frame
    // ----------------------------------------------------------------

    /**
     * Returns a basis for the local tangent space at the given position.
     *
     * BASIS CONVENTION: implementations must document whether the returned
     * basis is:
     *   - chart-aligned (coordinate basis vectors d/dx^i)
     *   - orthonormal (Gram-Schmidt or otherwise normalized)
     *   - implementation-defined
     *
     * The number of basis vectors equals getLocalManifoldDimensionality(p).
     */
    List<MotionVector> getLocalCoordinateFrame(Position p);

    /**
     * Projects a MotionVector onto the local tangent space at a position.
     * Removes any components not tangent to the manifold surface.
     * Essential for keeping motion vectors legal before transport or integration.
     */
    MotionVector projectToTangentSpace(Position p, MotionVector v);

    /**
     * Returns the normal vector at a position, if this manifold is embedded
     * in a higher-dimensional space.
     *
     * CONTRACT:
     *   - returns empty if the manifold is intrinsic or has no natural embedding
     *   - orientation (sign) may be arbitrary unless the implementation documents otherwise
     *   - for codimension > 1, the normal is not unique; implementations must document
     *     their convention
     */
    Optional<MotionVector> getNormalVector(Position p);


    // ----------------------------------------------------------------
    // Integration along paths
    // ----------------------------------------------------------------

    /**
     * Integrates a scalar field along the manifold's default path between
     * two positions, typically the geodesic when one exists.
     *
     * PATH SEMANTICS: implementations must document which path is used:
     *   - geodesic (preferred)
     *   - chart-straight interpolation
     *   - implementation-defined default
     *
     * @param from      start position
     * @param to        end position
     * @param field     scalar field to integrate (Position -> double)
     * @param steps     number of integration steps (resolution)
     * @return          the integral value, or empty if no path exists
     */
    Optional<Double> integrateAlongPath(
        Position from,
        Position to,
        Function<Position, Double> field,
        int steps
    );

    /**
     * Integrates a vector field along the manifold's default path between
     * two positions, returning the accumulated result as a MotionVector.
     *
     * PATH SEMANTICS: same as integrateAlongPath — implementations must
     * document which path is used.
     *
     * @param from      start position
     * @param to        end position
     * @param field     vector field to integrate (Position -> MotionVector)
     * @param steps     number of integration steps (resolution)
     * @return          the accumulated vector, or empty if no path exists
     */
    Optional<MotionVector> integrateVectorAlongPath(
        Position from,
        Position to,
        Function<Position, MotionVector> field,
        int steps
    );

    /**
     * Computes the arc length of the geodesic between two positions
     * using the metric tensor for accurate length measurement.
     *
     * For flat manifolds this equals Euclidean distance.
     * For curved manifolds this may differ significantly.
     *
     * Returns empty if:
     *   - no geodesic exists between the two positions
     *   - the geodesic is numerically unstable
     *   - the positions are on disconnected regions of the manifold
     *
     * @param steps     number of integration steps (resolution)
     */
    Optional<Double> arcLength(Position from, Position to, int steps);

	String getChartIdAt(Position p);
}