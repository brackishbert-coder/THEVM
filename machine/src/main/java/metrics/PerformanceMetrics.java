package metrics;

import java.util.List;
import java.util.Map;

import machine.Flatlander;
import machine.ManifoldSpace;

import java.util.List;
import java.util.Map;

/**
 * Computes and stores runtime/performance-oriented metrics for the manifold system.
 *
 * Performance metrics are about execution cost and operational state rather than
 * emergence or behavioral structure.
 *
 * Examples:
 * - average step duration
 * - total steps executed
 * - active population
 * - immobilized count
 * - stale count
 *
 * This interface is intentionally simple:
 * - update(...) ingests current execution timing and world state
 * - snapshot() exposes the latest metric values
 */
public interface PerformanceMetrics {

    /**
     * Update performance metrics from the current system state.
     *
     * @param flatlanders all active flatlanders
     * @param manifold the manifold they inhabit
     * @param stepCount current simulation step
     * @param elapsedNanos execution time for the completed step in nanoseconds
     */
    void update(
            List<Flatlander> flatlanders,
            ManifoldSpace manifold,
            long stepCount,
            long elapsedNanos
    );

    /**
     * Returns the latest metric values as a flat map.
     */
    Map<String, Double> snapshot();
}