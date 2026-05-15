package metrics;

import java.util.List;
import java.util.Map;

import machine.Flatlander;
import machine.ManifoldSpace;

import java.util.List;
import java.util.Map;

/**
 * Computes and stores emergence-oriented metrics for the manifold system.
 *
 * Emergence metrics are intended to capture structure that arises from the
 * collective behavior of flatlanders over time, rather than from any one
 * flatlander in isolation.
 *
 * Examples:
 * - clustering
 * - spatial concentration
 * - dispersion
 * - motion coherence
 * - population diversity
 *
 * This interface is intentionally simple:
 * - update(...) ingests the current world state
 * - snapshot() exposes the most recent metric values
 */
public interface EmergenceMetrics {

    /**
     * Update emergence metrics from the current system state.
     *
     * @param flatlanders all active flatlanders
     * @param manifold the manifold they inhabit
     * @param stepCount current simulation step
     */
    void update(List<Flatlander> flatlanders, ManifoldSpace manifold, long stepCount);

    /**
     * Returns the latest metric values as a flat map.
     */
    Map<String, Double> snapshot();
}