package metrics.implementaion;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import machine.Flatlander;
import machine.FlatlanderState;
import machine.ManifoldSpace;
import metrics.PerformanceMetrics;

/**
 * A simple performance metrics implementation.
 *
 * Computed metrics:
 * - last_step_duration_ms
 * - average_step_duration_ms
 * - min_step_duration_ms
 * - max_step_duration_ms
 * - total_steps_executed
 * - active_flatlander_count
 * - immobilized_count
 * - stale_count
 * - throughput_flatlanders_per_step
 *
 * Notes:
 * - average step duration is cumulative over all update(...) calls
 * - stale_count uses isStale(stepCount, 1L) as a conservative default
 * - all values are exposed as doubles for easy charting/export
 */
public class SimplePerformanceMetrics implements PerformanceMetrics {

    private final Map<String, Double> metrics = new HashMap<>();

    private long observedSteps = 0L;
    private long totalElapsedNanos = 0L;
    private long minElapsedNanos = Long.MAX_VALUE;
    private long maxElapsedNanos = Long.MIN_VALUE;

    @Override
    public void update(
            List<Flatlander> flatlanders,
            ManifoldSpace manifold,
            long stepCount,
            long elapsedNanos
    ) {
        Objects.requireNonNull(flatlanders, "flatlanders must not be null");
        Objects.requireNonNull(manifold, "manifold must not be null");

        observedSteps++;
        totalElapsedNanos += elapsedNanos;
        minElapsedNanos = Math.min(minElapsedNanos, elapsedNanos);
        maxElapsedNanos = Math.max(maxElapsedNanos, elapsedNanos);

        int activeCount = 0;
        int immobilizedCount = 0;
        int staleCount = 0;

        for (Flatlander flatlander : flatlanders) {
            if (flatlander == null) {
                continue;
            }

            FlatlanderState state = flatlander.getState();
            if (state == null) {
                continue;
            }

            activeCount++;

            if (state.isImmobilized()) {
                immobilizedCount++;
            }

            if (state.isStale(stepCount, 1L)) {
                staleCount++;
            }
        }

        double lastStepMs = nanosToMillis(elapsedNanos);
        double avgStepMs = observedSteps == 0
                ? 0.0
                : nanosToMillis(totalElapsedNanos) / observedSteps;

        double minStepMs = (minElapsedNanos == Long.MAX_VALUE)
                ? 0.0
                : nanosToMillis(minElapsedNanos);

        double maxStepMs = (maxElapsedNanos == Long.MIN_VALUE)
                ? 0.0
                : nanosToMillis(maxElapsedNanos);

        double throughput = observedSteps == 0
                ? 0.0
                : ((double) activeCount);

        metrics.put("last_step_duration_ms", lastStepMs);
        metrics.put("average_step_duration_ms", avgStepMs);
        metrics.put("min_step_duration_ms", minStepMs);
        metrics.put("max_step_duration_ms", maxStepMs);
        metrics.put("total_steps_executed", (double) observedSteps);
        metrics.put("active_flatlander_count", (double) activeCount);
        metrics.put("immobilized_count", (double) immobilizedCount);
        metrics.put("stale_count", (double) staleCount);
        metrics.put("throughput_flatlanders_per_step", throughput);
    }

    @Override
    public Map<String, Double> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(metrics));
    }

    private double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
