package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A system-level event emitted during execution.
 *
 * CONVENTION: eventType values in CommonEventTypes.
 * CONVENTION: attribute keys in AttributeConventions.
 */
public interface ManifoldEvent {
    String getEventType(); // see CommonEventTypes
    long getStep();
    Optional<String> getFlatlanderId();
    Optional<Position> getPosition();
    Map<String, Object> getAttributes();
}
