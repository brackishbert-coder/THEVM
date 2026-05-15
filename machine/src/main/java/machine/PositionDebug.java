package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Debug projection of a position to 2D for visualization.
 */
public interface PositionDebug {
    double getX();
    double getY();
    String getLabel();
}
