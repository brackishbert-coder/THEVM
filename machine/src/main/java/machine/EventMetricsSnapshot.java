package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Snapshot of event frequency metrics.
 */
public interface EventMetricsSnapshot {
    Map<String, Long> getEventCountsByType();
    long getTotalEventCount();
    Map<String, Double> getCustomMetrics();
}
