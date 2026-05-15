package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Current runtime state of a flatlander.
 *
 * MOBILITY DISTINCTION:
 *   isImmobilized() — topological trap. Runner response: reboot or re-seed.
 *   isStale(step, threshold) — computation stall. Runner response: prune or dormancy.
 *   These are distinct conditions requiring distinct runner responses.
 */
public interface FlatlanderState {
    Position getPosition();
    MotionVector getVelocity();
    long getCurrentStep();
    Map<String, Object> getAttributes();

    boolean isImmobilized();
    boolean isStale(long currentStep, long threshold);
    boolean isDormant();
	void setPosition(Position next);
}
