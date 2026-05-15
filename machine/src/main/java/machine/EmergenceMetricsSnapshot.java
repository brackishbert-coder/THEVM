package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Snapshot of emergence-level metrics.
 */
public interface EmergenceMetricsSnapshot {
    double getClusteringCoefficient();
    double getAverageSimilarityScore();
    int getActiveClusterCount();
    Map<String, Double> getCustomMetrics();
}
