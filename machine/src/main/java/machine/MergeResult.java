package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Result of a merge operation.
 */
public interface MergeResult {
    boolean isSuccess();
    Optional<Flatlander> getMerged();
    String getFailureReason();
}
