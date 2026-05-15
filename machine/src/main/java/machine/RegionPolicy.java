package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Runtime behavioral policy for a manifold region.
 *
 * STRICT CONTRACT for getSuggestedCorrection():
 *   - Must be pure and stateless with respect to the inputs only
 *   - Must NOT query ManifoldSpace
 *   - Must NOT reference any Flatlander
 *   - Must NOT produce side effects
 *   - Must use ONLY the supplied MotionVector as input
 */
public interface RegionPolicy {
    double getOperationWeight(String operationName);
    Map<String, Double> getPolicyWeights();
    Optional<MotionVector> getSuggestedCorrection(MotionVector attemptedMotion);
    boolean allowsOperation(String operationName);
}
