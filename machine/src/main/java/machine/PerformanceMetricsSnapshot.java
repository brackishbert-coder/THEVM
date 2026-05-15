package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Snapshot of system performance metrics.
 */
public interface PerformanceMetricsSnapshot {
    double getAverageStepDurationMs();
    long getTotalStepsExecuted();
    int getActiveFlatlanderCount();
    int getImmobilizedCount();
    int getStaleCount();
    Map<String, Double> getCustomMetrics();
}
