package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Result of a spawn operation.
 */
public interface SpawnResult {
    boolean isSuccess();
    Optional<Flatlander> getSpawned();
    String getFailureReason();
}
