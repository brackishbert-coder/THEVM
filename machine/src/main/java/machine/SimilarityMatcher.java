package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * System-level service for computing similarity between flatlanders.
 * Not owned by the flatlander itself.
 */
public interface SimilarityMatcher {
    double computeSimilarity(SimilaritySignature a, SimilaritySignature b);
    List<Flatlander> findSimilar(Flatlander target, List<Flatlander> candidates, double threshold);
}
