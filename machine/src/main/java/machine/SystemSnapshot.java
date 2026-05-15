package machine;

import java.util.List;
import java.util.Map;

/**
 * Shared read-only system snapshot for metrics, observers, SOMs, and analysis tools.
 *
 * ALIGNMENT PATCH:
 * Added getSimilarityMatcher() so observers and analytics can evaluate similarity
 * relations without requiring an external service lookup.
 * This matches the fuller architecture we converged on during interface design.
 *
 * CONVENTION: attribute keys in AttributeConventions.
 */
public interface SystemSnapshot {
    long getStep();
    List<Flatlander> getActiveFlatlanders();
    ManifoldSpace getManifoldSpace();
    SimilarityMatcher getSimilarityMatcher();
    Map<String, Object> getSystemAttributes();
    EmergenceMetricsSnapshot getEmergenceMetrics();
    PerformanceMetricsSnapshot getPerformanceMetrics();
}
