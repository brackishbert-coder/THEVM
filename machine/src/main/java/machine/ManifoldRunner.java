package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * The execution runner for a manifold system.
 */
public interface ManifoldRunner {
    void initialize(ManifoldSpace space, List<Flatlander> initialFlatlanders);
    void step();
    void runUntil(StoppingCondition condition);
    SystemSnapshot snapshot();
    void addObserver(MetricsObserver observer);
    void addPatternObserver(PatternObserver observer);
    boolean isRunning();
    void stop();
}
