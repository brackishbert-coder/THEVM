package execution;

import java.util.List;

import machine.Flatlander;
import machine.ManifoldSpace;

import java.util.List;

/**
 * Observes manifold execution and feeds encoded state into a SOM-like system.
 *
 * This interface is intentionally observer-only:
 * - it does not move flatlanders
 * - it does not resolve collisions
 * - it does not compute runner control flow
 *
 * Its job is to watch the current simulation state and translate it into
 * feature vectors suitable for self-organizing learning.
 *
 * Typical uses:
 * - encode positions and motion into vectors
 * - track emergent behavioral clusters
 * - learn manifold-region usage patterns
 * - monitor lineage or interaction motifs
 */
public interface SOMObserver {

    /**
     * Observe the current system state after a step has settled.
     *
     * @param flatlanders all active flatlanders
     * @param manifold the manifold they inhabit
     * @param stepCount current simulation step
     */
    void observe(List<Flatlander> flatlanders, ManifoldSpace manifold, long stepCount);
}