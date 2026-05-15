package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Result of adapting a motion vector to local manifold constraints.
 */
public interface MotionAdaptationResult {
    MotionVector getAdaptedVector();
    boolean wasModified();
    String getModificationReason();
}
