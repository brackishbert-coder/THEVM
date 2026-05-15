package execution.implmentation;

import java.util.List;

import execution.SOMObserver;
import machine.Flatlander;
import machine.ManifoldSpace;

/**
 * A no-op SOM observer.
 *
 * Useful while wiring up the simulation pipeline before integrating
 * a real self-organizing map or feature encoder.
 */
public class NoOpSOMObserver implements SOMObserver {

    @Override
    public void observe(List<Flatlander> flatlanders, ManifoldSpace manifold, long stepCount) {
        // Intentionally does nothing.
    }
}