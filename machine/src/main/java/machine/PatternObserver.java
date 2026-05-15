package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Observer for emergent patterns across flatlander interactions.
 * Watches for higher-order structures that metrics alone won't capture.
 */
public interface PatternObserver {
    void onPatternDetected(String patternType, List<Flatlander> participants, SystemSnapshot snapshot);
}
