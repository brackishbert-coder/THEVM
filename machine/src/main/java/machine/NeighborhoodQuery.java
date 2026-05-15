package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A query for the neighborhood around a position.
 */
public interface NeighborhoodQuery {
    List<Position> getPositionsInRadius();
    List<Transition> getAvailableTransitions();
    double getRadius();
	List<Flatlander> getNearbyFlatlanders();
	boolean isFrozen();
	OptionalLong getCapturedAtStep();
	List<Position> getNearbyPositions();
}
