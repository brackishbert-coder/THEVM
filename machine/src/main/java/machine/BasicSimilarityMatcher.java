package machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple similarity matcher over SimilaritySignature feature maps.
 *
 * Similarity is computed as:
 *   1 / (1 + euclideanDistance)
 *
 * Result range:
 * - 1.0   => identical features
 * - ~0.0  => very dissimilar features
 */
public class BasicSimilarityMatcher implements SimilarityMatcher {

    @Override
    public double computeSimilarity(SimilaritySignature a, SimilaritySignature b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");

        Map<String, Double> fa = a.getFeatures();
        Map<String, Double> fb = b.getFeatures();

        if (fa.isEmpty() && fb.isEmpty()) {
            return 1.0;
        }

        double sumSq = 0.0;

        for (String key : fa.keySet()) {
            double av = fa.getOrDefault(key, 0.0);
            double bv = fb.getOrDefault(key, 0.0);
            double d = av - bv;
            sumSq += d * d;
        }

        for (String key : fb.keySet()) {
            if (!fa.containsKey(key)) {
                double bv = fb.getOrDefault(key, 0.0);
                sumSq += bv * bv;
            }
        }

        double distance = Math.sqrt(sumSq);
        return 1.0 / (1.0 + distance);
    }

    @Override
    public List<Flatlander> findSimilar(
            Flatlander target,
            List<Flatlander> candidates,
            double threshold
    ) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");

        List<Flatlander> matches = new ArrayList<>();
        SimilaritySignature targetSig = target.getSimilaritySignature();

        for (Flatlander candidate : candidates) {
            if (candidate == null || candidate == target) {
                continue;
            }

            double score = computeSimilarity(targetSig, candidate.getSimilaritySignature());
            if (score >= threshold) {
                matches.add(candidate);
            }
        }

        return matches;
    }
}