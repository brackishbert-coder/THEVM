package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A snapshot of a flatlander's local neighborhood.
 *
 * FROZEN vs LIVE:
 *   isFrozen() must be checked before use in time-sensitive contexts.
 *   getCapturedAtStep() returns OptionalLong — empty if live.
 *   getAvailableTransitions() omits ExecutionContext intentionally:
 *     frozen neighborhoods bake it in at capture time;
 *     live neighborhoods retain it internally.
 */
public interface Neighborhood {
    List<Flatlander> getNearbyFlatlanders();
    List<Position> getNearbyPositions();
    List<Transition> getAvailableTransitions();
    boolean isFrozen();
    OptionalLong getCapturedAtStep();
}
