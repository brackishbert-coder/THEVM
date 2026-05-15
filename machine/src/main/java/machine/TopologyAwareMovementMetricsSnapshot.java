package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Snapshot of topology-aware movement metrics.
 */
public interface TopologyAwareMovementMetricsSnapshot {
    double getAverageGeodesicError();
    int getWraparoundEventCount();
    int getLayerJumpCount();
    double getAverageCurvatureEncountered();
    Map<String, Double> getCustomMetrics();
}
