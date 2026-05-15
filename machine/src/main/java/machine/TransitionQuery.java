package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Query parameters for requesting transitions from a position.
 *
 * CONVENTION: constraint keys in ConstraintConventions.
 */
public interface TransitionQuery {
    Map<String, Object> getConstraints();
    int getMaxResults();
    boolean includeNonTraversable();
}
