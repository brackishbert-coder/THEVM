package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import execution.ManifoldSystemRunner;

/**
 * A predicate determining when execution should halt.
 */
public interface StoppingCondition {
    boolean shouldStop(SystemSnapshot snapshot);
    String getDescription();
    boolean shouldStop(ManifoldSystemRunner.RunnerSnapshot snapshot);
}
