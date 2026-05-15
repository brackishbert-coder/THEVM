package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A possible local transition from one position to another.
 *
 * CONVENTION: transitionType values defined in CommonTransitionTypes.
 * Transition types are a separate registry from ManifoldEvent types.
 */
public interface Transition {
    Position getSource();
    Position getTarget();
    double getCost();
    boolean isTraversable();
    String getTransitionType(); // see CommonTransitionTypes
    Map<String, Double> getAttributes();
}
