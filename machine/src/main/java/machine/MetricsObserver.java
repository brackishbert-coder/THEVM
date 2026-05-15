package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Observer for execution metrics.
 *
 * NOTE: recordEvents() default delegates to recordEvent() one by one.
 * Override for batch performance when high event volume is expected.
 */
public interface MetricsObserver {
    void recordEvent(ManifoldEvent event);

    default void recordEvents(List<ManifoldEvent> events) {
        events.forEach(this::recordEvent);
    }

    void onStepComplete(SystemSnapshot snapshot);
}
