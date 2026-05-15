package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A direction and magnitude of movement, backend-agnostic.
 */
public interface MotionVector {
    double[] getComponents();
    double getMagnitude();
    MotionVector scale(double factor);
    MotionVector add(MotionVector other);
}
