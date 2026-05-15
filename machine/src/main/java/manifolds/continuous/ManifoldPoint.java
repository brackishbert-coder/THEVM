package manifolds.continuous;

import machine.Position;

/**
 * A position in continuous manifold space.
 *
 * Extends Position with chart-aware structure. Carries coordinates,
 * dimensionality, and a chart identifier.
 *
 * Chart type is a String for now — candidate for CommonChartTypes (v1.1).
 *
 * The global node ID concept does not apply here — continuous positions
 * are identified by their coordinates within a chart.
 */
public interface ManifoldPoint extends Position  {
    double[] getCoordinates();
    int getDimensionality();
    String getChartId();
    boolean isValid();
}