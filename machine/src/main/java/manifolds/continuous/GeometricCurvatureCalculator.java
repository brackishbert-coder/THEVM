package manifolds.continuous;


import java.util.Optional;

import machine.CurvatureField;
import machine.Position;

/**
 * A service interface for computing curvature at positions on a continuous manifold.
 *
 * Implementations are topology-specific — a flat plane computes trivially,
 * a toroidal manifold computes differently from a cylindrical one.
 *
 * METRIC TENSOR CONTRACT: the metric tensor supplied to every method must
 * correspond to the same position and local chart as the supplied Position p.
 * Passing a stale or mismatched tensor will produce silent numerical nonsense.
 * Callers should always obtain the metric tensor from
 * ContinuousManifold.getMetricTensor(p) immediately before invoking
 * curvature computations.
 *
 * NUMERICAL NOTE: curvature computations on discrete approximations of
 * continuous manifolds are inherently approximate. Implementations must
 * document their numerical method and expected error bounds.
 */
public interface GeometricCurvatureCalculator {

    // ----------------------------------------------------------------
    // Scalar curvature
    // ----------------------------------------------------------------

    /**
     * Returns the scalar curvature at the given position.
     *
     * Scalar curvature is the full contraction of the Riemann tensor —
     * equivalently, the trace of the Ricci tensor: R = g^{ij} R_{ij}.
     *
     * This is strict differential-geometric scalar curvature, not a
     * summary magnitude or norm.
     *
     * For flat regions: returns 0.0.
     * For singular regions: behavior is implementation-defined; check isSingular() first.
     */
    double computeScalarCurvature(Position p, double[][] metricTensor);

    /**
     * Returns true if the given position is at or near a curvature singularity.
     * Callers should check this before relying on any curvature values at this position.
     */
    boolean isSingular(Position p, double[][] metricTensor);

    /**
     * Returns true if the manifold is locally flat at the given position.
     * Flat means scalar curvature is effectively zero within numerical tolerance.
     */
    boolean isFlat(Position p, double[][] metricTensor);


    // ----------------------------------------------------------------
    // Full curvature tensor
    // ----------------------------------------------------------------

    /**
     * Returns the Riemann curvature tensor at the given position.
     *
     * Shape: [n x n x n x n] where n = local manifold dimensionality.
     * Returns empty if not supported by this implementation or position is singular.
     *
     * CONTRACT: callers must treat returned arrays as read-only.
     */
    Optional<double[][][][]> computeRiemannTensor(Position p, double[][] metricTensor);

    /**
     * Returns the Ricci curvature tensor at the given position.
     * The Ricci tensor is the trace of the Riemann tensor: R_ij = R^k_ikj.
     *
     * Shape: [n x n] where n = local manifold dimensionality.
     * Returns empty if not supported or position is singular.
     *
     * CONTRACT: callers must treat returned arrays as read-only.
     */
    Optional<double[][]> computeRicciTensor(Position p, double[][] metricTensor);


    // ----------------------------------------------------------------
    // Gaussian curvature
    // ----------------------------------------------------------------

    /**
     * Returns the Gaussian curvature at the given position.
     *
     * Gaussian curvature K is the product of principal curvatures (k1 * k2).
     * For surfaces embedded in 3D:
     *   K > 0 — elliptic (sphere-like)
     *   K = 0 — flat (plane or cylinder)
     *   K < 0 — hyperbolic (saddle-like)
     *
     * Returns empty if manifold dimensionality != 2, or if singular.
     */
    Optional<Double> computeGaussianCurvature(Position p, double[][] metricTensor);

    /**
     * Returns the principal curvatures at the given position as [k1, k2].
     *
     * CONTRACT:
     *   - k1 <= k2 by convention
     *   - returned array length is exactly 2
     *
     * Returns empty if manifold dimensionality != 2, or if singular.
     */
    Optional<double[]> computePrincipalCurvatures(Position p, double[][] metricTensor);


    // ----------------------------------------------------------------
    // Mean curvature
    // ----------------------------------------------------------------

    /**
     * Returns the mean curvature at the given position.
     *
     * Mean curvature H = (k1 + k2) / 2.
     * A minimal surface has H = 0 everywhere.
     * Only meaningful for surfaces embedded in a higher-dimensional space.
     *
     * Returns empty if manifold dimensionality != 2, or if singular.
     */
    Optional<Double> computeMeanCurvature(Position p, double[][] metricTensor);


    // ----------------------------------------------------------------
    // CurvatureField factory
    // ----------------------------------------------------------------

    /**
     * Assembles a CurvatureField from computed values at the given position.
     *
     * SINGULARITY CONTRACT:
     *   - if isSingular() returns true for this position, the returned
     *     CurvatureField must have isSingular() == true
     *   - implementations must NOT return a flat fallback silently at singularities
     *   - callers are responsible for checking isSingular() on the result
     *     before using curvature values
     */
    CurvatureField buildCurvatureField(Position p, double[][] metricTensor);

	double computeScalarIntensity(Position p, double[][] metricTensor);
}