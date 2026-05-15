package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Result of evaluating traversal cost for a transition.
 */
public interface TraversalCostResult {
    double getCost();
    boolean isFeasible();
    String getRejectionReason();
}
