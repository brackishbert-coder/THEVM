package execution.implmentation;

import java.util.List;

import execution.IntersectionHandler;
import machine.Flatlander;
import machine.ManifoldSpace;

/**
 * A no-op intersection handler.
 *
 * Useful for early testing when movement should work but collisions
 * and interactions are not yet implemented.
 */
public class NoOpIntersectionHandler implements IntersectionHandler {

    @Override
    public void resolve(List<Flatlander> flatlanders, ManifoldSpace manifold, long stepCount) {
        // Intentionally does nothing.
    }
}