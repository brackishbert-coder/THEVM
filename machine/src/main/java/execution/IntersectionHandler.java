package execution;

import java.util.List;

import machine.Flatlander;
import machine.ManifoldSpace;

import java.util.List;

/**
 * Resolves post-movement intersections/interactions between flatlanders.
 *
 * This runs after geodesic flow has advanced flatlanders for the step.
 *
 * Responsibilities may include:
 * - detecting flatlanders at the same position
 * - detecting near-collisions or manifold-local overlap
 * - invoking merge or interaction logic
 * - deferring policy decisions to later implementations
 *
 * This interface intentionally does not prescribe how intersections
 * are interpreted. Different systems may treat them as:
 * - collisions
 * - merges
 * - information exchanges
 * - no-ops
 */
public interface IntersectionHandler {

    /**
     * Resolve intersections/interactions for the current step.
     *
     * @param flatlanders all active flatlanders
     * @param manifold the manifold they inhabit
     * @param stepCount current simulation step
     */
    void resolve(List<Flatlander> flatlanders, ManifoldSpace manifold, long stepCount);
}