package execution;

import machine.Flatlander;
import machine.ManifoldSpace;

/**
 * Responsible for advancing a flatlander forward through manifold space
 * for a single simulation step.
 *
 * The engine owns movement/orientation flow semantics but does not own:
 * - collision resolution
 * - spawning
 * - merging
 * - metric collection
 *
 * Those belong elsewhere in the runner pipeline.
 */
public interface GeodesicFlowEngine {

    /**
     * Advance one flatlander by one step.
     *
     * @param flatlander the flatlander to advance
     * @param manifold the manifold it moves through
     * @param stepCount the current simulation step
     */
    void advance(Flatlander flatlander, ManifoldSpace manifold, long stepCount);
}