package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Snapshot of lineage and genealogy metrics.
 */
public interface LineageMetricsSnapshot {
    int getTotalSpawnCount();
    int getTotalMergeCount();
    double getAverageGenerationDepth();
    int getMaxGenerationDepth();
    Map<String, Double> getCustomMetrics();
}
