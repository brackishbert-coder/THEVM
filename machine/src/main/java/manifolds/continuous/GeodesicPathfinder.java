package manifolds.continuous;


import java.util.List;
import java.util.Optional;

import machine.Position;

/**
 * A service interface for finding geodesics on a continuous manifold.
 *
 * Handles three cases:
 *   - exact geodesics (closed-form, topology-specific)
 *   - approximate geodesics (numerical, when exact is unavailable)
 *   - multiple geodesics (when topology produces more than one, e.g. toroidal manifolds)
 *
 * IDENTICAL ENDPOINT CONVENTION:
 *   If from and to are the same position, all methods return a zero-length
 *   geodesic with a singleton path containing only that position.
 *   This convention applies consistently across all methods in this interface.
 *
 * NUMERICAL NOTE: approximate geodesic methods must document their algorithm
 * (e.g. gradient descent, Runge-Kutta, shooting method) and expected error bounds.
 */
public interface GeodesicPathfinder {

    // ----------------------------------------------------------------
    // Exact geodesics
    // ----------------------------------------------------------------

    /**
     * Returns an exact geodesic between two positions, if a closed-form
     * solution exists for this manifold topology.
     *
     * If multiple exact geodesics exist, returns one shortest exact geodesic.
     * Use findAllExactGeodesics() to retrieve all of them.
     *
     * Returns empty if:
     *   - no closed-form geodesic exists for this topology
     *   - the positions are on disconnected regions
     *
     * IDENTICAL ENDPOINT: returns a zero-length singleton geodesic.
     */
    Optional<GeodesicResult> findExactGeodesic(Position from, Position to);

    /**
     * Returns true if this implementation can compute an exact geodesic
     * for the given endpoint pair.
     *
     * This is pair-specific — it reflects both whether the topology supports
     * exact geodesics in general and whether this particular pair admits one.
     */
    boolean supportsExactGeodesic(Position from, Position to);

    /**
     * Returns all exact geodesics between two positions, ordered by
     * ascending arc length.
     *
     * On topologies like toroids, multiple geodesics of equal or different
     * length may connect the same two points.
     *
     * Returns an empty list if no exact geodesics exist or the topology
     * does not support multiple geodesic enumeration.
     *
     * IDENTICAL ENDPOINT: returns a singleton list containing the zero-length geodesic.
     */
    List<GeodesicResult> findAllExactGeodesics(Position from, Position to);


    // ----------------------------------------------------------------
    // Approximate geodesics
    // ----------------------------------------------------------------

    /**
     * Returns an approximate geodesic between two positions using numerical methods.
     *
     * NUMERICAL CONTRACT: implementations must document:
     *   - algorithm used (e.g. gradient descent, shooting method, Runge-Kutta)
     *   - expected error bounds relative to true geodesic length
     *   - behavior near singularities
     *
     * IDENTICAL ENDPOINT: returns a zero-length singleton geodesic.
     *
     * @param steps     number of integration steps (controls accuracy vs. cost)
     * @return          approximate geodesic, or empty if computation fails
     */
    Optional<GeodesicResult> findApproximateGeodesic(Position from, Position to, int steps);

    /**
     * Attempts to return multiple approximate geodesic candidates between
     * two positions, ordered by ascending approximate arc length.
     *
     * On curved or multiply-connected manifolds, multiple locally minimal
     * paths may exist. This method makes a best-effort attempt to find them
     * numerically — it does not guarantee finding all geodesics.
     *
     * Returns an empty list if computation fails entirely.
     *
     * IDENTICAL ENDPOINT: returns a singleton list containing the zero-length geodesic.
     *
     * NUMERICAL CONTRACT: same as findApproximateGeodesic().
     *
     * @param steps         number of integration steps per geodesic candidate
     * @param maxResults    maximum number of candidates to return
     */
    List<GeodesicResult> findAllApproximateGeodesics(Position from, Position to,
                                                      int steps, int maxResults);


    // ----------------------------------------------------------------
    // Best available geodesic
    // ----------------------------------------------------------------

    /**
     * Returns the best available geodesic between two positions.
     *
     * Strategy:
     *   1. Attempts exact geodesic first via findExactGeodesic()
     *   2. Falls back to findApproximateGeodesic() if exact is unavailable
     *   3. Returns empty if both fail
     *
     * The result carries a flag indicating whether it is exact or approximate.
     * Callers that need to distinguish should check GeodesicResult.isExact().
     *
     * IDENTICAL ENDPOINT: returns a zero-length singleton geodesic.
     *
     * @param steps     number of steps to use if falling back to approximate
     */
    Optional<GeodesicResult> findBestGeodesic(Position from, Position to, int steps);


    // ----------------------------------------------------------------
    // Geodesic result type
    // ----------------------------------------------------------------

    /**
     * The result of a geodesic computation.
     *
     * Carries the path as an ordered list of positions, the arc length,
     * and flags indicating exactness and uniqueness.
     *
     * PATH SAMPLING: implementations must document their path sampling convention:
     *   - exact geodesics may be sparse or densely sampled
     *   - approximate geodesics typically produce one point per integration step
     */
    interface GeodesicResult {

        /**
         * The ordered list of positions along the geodesic, from source to target.
         * For a zero-length geodesic, this is a singleton list.
         */
        List<Position> getPath();

        /**
         * The arc length of this geodesic, computed using the metric tensor.
         * For approximate geodesics, this is an estimate.
         * For zero-length geodesics, returns 0.0.
         */
        double getArcLength();

        /**
         * Returns true if this geodesic was computed via exact closed-form methods.
         * Returns false if it was computed numerically or approximately.
         */
        boolean isExact();

        /**
         * Returns true if this is the unique shortest geodesic between the endpoints,
         * up to implementation-defined numerical tolerance.
         *
         * For approximate geodesics, uniqueness is determined within the discovered
         * candidate set and is tolerance-dependent — not a global guarantee.
         *
         * Implementations should set this conservatively — when in doubt, return false.
         */
        boolean isUnique();

        /**
         * Returns the numerical error estimate for this geodesic, if available.
         * Returns empty for exact geodesics or if error estimation is not supported.
         */
        Optional<Double> getErrorEstimate();
    }
}