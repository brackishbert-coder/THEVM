package execution;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import machine.Flatlander;
import machine.ManifoldSpace;
import machine.StoppingCondition;
import metrics.EmergenceMetrics;
import metrics.PerformanceMetrics;

/**
 * Orchestrates execution of a manifold-based flatlander system.
 *
 * Responsibilities:
 * - own the manifold instance
 * - own the active flatlander population
 * - advance the simulation clock
 * - delegate motion to GeodesicFlowEngine
 * - delegate interaction resolution to IntersectionHandler
 * - notify SOMObserver
 * - update emergence and performance metrics
 *
 * Non-responsibilities:
 * - does not compute geodesics directly
 * - does not implement collision logic directly
 * - does not embed observer logic directly
 *
 * Pipeline per step:
 *   1. snapshot current flatlanders
 *   2. advance each flatlander through the flow engine
 *   3. resolve intersections/interactions
 *   4. notify SOM observer
 *   5. update emergence metrics
 *   6. update performance metrics
 *
 * Thread safety:
 * - not thread-safe
 * - synchronize externally if accessed concurrently
 */
public class ManifoldSystemRunner {

    private final ManifoldSpace manifold;
    private final GeodesicFlowEngine geodesicFlowEngine;
    private final IntersectionHandler intersectionHandler;
    private final SOMObserver somObserver;
    private final EmergenceMetrics emergenceMetrics;
    private final PerformanceMetrics performanceMetrics;

    private final List<Flatlander> flatlanders = new ArrayList<>();
    private final List<Flatlander> pendingAdditions = new ArrayList<>();
    private final List<Flatlander> pendingRemovals = new ArrayList<>();

    private long stepCount = 0L;
    private boolean running = false;

    public ManifoldSystemRunner(
            ManifoldSpace manifold,
            GeodesicFlowEngine geodesicFlowEngine,
            IntersectionHandler intersectionHandler,
            SOMObserver somObserver,
            EmergenceMetrics emergenceMetrics,
            PerformanceMetrics performanceMetrics
    ) {
        this.manifold = Objects.requireNonNull(manifold, "manifold must not be null");
        this.geodesicFlowEngine = Objects.requireNonNull(geodesicFlowEngine, "geodesicFlowEngine must not be null");
        this.intersectionHandler = Objects.requireNonNull(intersectionHandler, "intersectionHandler must not be null");
        this.somObserver = Objects.requireNonNull(somObserver, "somObserver must not be null");
        this.emergenceMetrics = Objects.requireNonNull(emergenceMetrics, "emergenceMetrics must not be null");
        this.performanceMetrics = Objects.requireNonNull(performanceMetrics, "performanceMetrics must not be null");
    }

    // ------------------------------------------------------------
    // Population management
    // ------------------------------------------------------------

    /**
     * Adds a flatlander.
     *
     * If the runner is currently executing a step, the flatlander is staged and
     * applied after the step completes. Otherwise it is added immediately.
     */
    public void addFlatlander(Flatlander flatlander) {
        Objects.requireNonNull(flatlander, "flatlander must not be null");

        if (running) {
            pendingAdditions.add(flatlander);
        } else {
            flatlanders.add(flatlander);
        }
    }

    /**
     * Removes a flatlander.
     *
     * If the runner is currently executing a step, removal is staged and applied
     * after the step completes. Otherwise it is removed immediately.
     */
    public void removeFlatlander(Flatlander flatlander) {
        Objects.requireNonNull(flatlander, "flatlander must not be null");

        if (running) {
            pendingRemovals.add(flatlander);
        } else {
            flatlanders.remove(flatlander);
        }
    }

    public List<Flatlander> getFlatlanders() {
        return Collections.unmodifiableList(flatlanders);
    }

    public int getFlatlanderCount() {
        return flatlanders.size();
    }

    // ------------------------------------------------------------
    // Runner state
    // ------------------------------------------------------------

    public ManifoldSpace getManifold() {
        return manifold;
    }

    public long getStepCount() {
        return stepCount;
    }

    public boolean isRunning() {
        return running;
    }

    // ------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------

    /**
     * Executes exactly one simulation step.
     */
    public void step() {
        long startNanos = System.nanoTime();
        running = true;

        try {
            List<Flatlander> snapshot = List.copyOf(flatlanders);

            // 1. Advance motion/state through the flow engine.
            for (Flatlander flatlander : snapshot) {
                if (flatlander == null) {
                    continue;
                }
                geodesicFlowEngine.advance(flatlander, manifold, stepCount);
            }

            // 2. Resolve interactions after movement.
            intersectionHandler.resolve(flatlanders, manifold, stepCount);

            // 3. Notify observer after the world state has settled.
            somObserver.observe(flatlanders, manifold, stepCount);

            // 4. Update emergence metrics.
            emergenceMetrics.update(flatlanders, manifold, stepCount);

            // 5. Update performance metrics.
            long elapsedNanos = System.nanoTime() - startNanos;
            performanceMetrics.update(flatlanders, manifold, stepCount, elapsedNanos);

        } finally {
            running = false;
            applyPendingChanges();
            stepCount++;
        }
    }

    /**
     * Executes a fixed number of steps.
     */
    public void runSteps(int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("steps must be >= 0");
        }

        for (int i = 0; i < steps; i++) {
            step();
        }
    }

    /**
     * Runs until the supplied stopping condition returns true.
     */
    public void runUntil(StoppingCondition condition) {
        Objects.requireNonNull(condition, "condition must not be null");

        while (!condition.shouldStop(snapshot())) {
            step();
        }
    }

    /**
     * Stops the runner after the current step boundary.
     *
     * This is mostly useful if an outer loop is driving step() manually.
     */
    public void stop() {
        running = false;
    }

    // ------------------------------------------------------------
    // Snapshot
    // ------------------------------------------------------------

    /**
     * Creates a lightweight runtime snapshot view.
     */
    public RunnerSnapshot snapshot() {
        return new RunnerSnapshot(
                stepCount,
                List.copyOf(flatlanders),
                manifold,
                emergenceMetrics.snapshot(),
                performanceMetrics.snapshot()
        );
    }

    // ------------------------------------------------------------
    // Internal staging
    // ------------------------------------------------------------

    private void applyPendingChanges() {
        if (!pendingRemovals.isEmpty()) {
            flatlanders.removeAll(pendingRemovals);
            pendingRemovals.clear();
        }

        if (!pendingAdditions.isEmpty()) {
            flatlanders.addAll(pendingAdditions);
            pendingAdditions.clear();
        }
    }

    // ------------------------------------------------------------
    // Snapshot type
    // ------------------------------------------------------------

    /**
     * Lightweight immutable runtime snapshot for stopping conditions, debugging,
     * and future observer expansion.
     */
    public record RunnerSnapshot(
            long stepCount,
            List<Flatlander> flatlanders,
            ManifoldSpace manifold,
            java.util.Map<String, Double> emergenceMetrics,
            java.util.Map<String, Double> performanceMetrics
    ) {

		public int getStep() {
			// TODO Auto-generated method stub
			return (int) stepCount;
		}
    }
}