package execution.implementation;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import machine.EmergenceMetricsSnapshot;
import machine.ExecutionContext;
import machine.Flatlander;
import machine.FlatlanderState;
import machine.ManifoldRunner;
import machine.ManifoldSpace;
import machine.MetricsObserver;
import machine.PatternObserver;
import machine.PerformanceMetricsSnapshot;
import machine.SimilarityMatcher;
import machine.StoppingCondition;
import machine.SystemSnapshot;

/**
 * A simple concrete ManifoldRunner implementation.
 *
 * Responsibilities:
 * - hold the manifold
 * - hold the active flatlander population
 * - advance the simulation one step at a time
 * - invoke the SOM observer and metrics observer after each step
 * - provide immutable snapshots for stopping conditions and observers
 *
 * Pipeline per step:
 *   1. build an ExecutionContext
 *   2. ask each flatlander to executeStep(...)
 *   3. notify PatternObserver
 *   4. notify MetricsObserver
 *   5. increment step counter
 *
 * This implementation is intentionally conservative:
 * - no staged add/remove queue
 * - no explicit collision/intersection handling
 * - no separate flow engine
 * - no event buffering beyond direct observer calls
 *
 * It is a good v1 runner for getting the system moving.
 *
 * Thread safety:
 * - not thread-safe
 * - synchronize externally if accessed concurrently
 */
public class SimpleManifoldRunner implements ManifoldRunner {

    private ManifoldSpace manifoldSpace;
    private final List<Flatlander> flatlanders = new ArrayList<>();
    private final List<MetricsObserver> metricsObservers = new ArrayList<>();
    private final List<PatternObserver> patternObservers = new ArrayList<>();

    private long currentStep = 0L;
    private boolean running = false;
    private final SimilarityMatcher similarityMatcher;
    
    
    public SimpleManifoldRunner(SimilarityMatcher similarityMatcher) {
        this.similarityMatcher = Objects.requireNonNull(similarityMatcher, "similarityMatcher must not be null");
    }
    @Override
    public void initialize(ManifoldSpace space, List<Flatlander> initialFlatlanders) {
        this.manifoldSpace = Objects.requireNonNull(space, "space must not be null");

        flatlanders.clear();
        if (initialFlatlanders != null) {
            for (Flatlander flatlander : initialFlatlanders) {
                if (flatlander != null) {
                    flatlanders.add(flatlander);
                }
            }
        }

        currentStep = 0L;
        running = false;
    }

    @Override
    public void step() {
        requireInitialized();

        long stepStart = System.nanoTime();
        running = true;

        try {
            ExecutionContext context = new RunnerExecutionContext(currentStep, manifoldSpace, false, Map.of());

            List<Flatlander> snapshot = List.copyOf(flatlanders);

            // 1. Advance all flatlanders
            for (Flatlander flatlander : snapshot) {
                if (flatlander == null) {
                    continue;
                }
                flatlander.executeStep(context);
            }

            // 2. Build snapshot after state changes
            SystemSnapshot systemSnapshot = buildSnapshot(stepStart);

            // 3. Pattern observers
            for (PatternObserver observer : patternObservers) {
                observer.onPatternDetected("step_complete", snapshot, systemSnapshot);
            }

            // 4. Metrics observers
            for (MetricsObserver observer : metricsObservers) {
                observer.onStepComplete(systemSnapshot);
            }

        } finally {
            currentStep++;
            running = false;
        }
    }

    @Override
    public void runUntil(StoppingCondition condition) {
        requireInitialized();
        Objects.requireNonNull(condition, "condition must not be null");

        running = true;
        try {
            while (!condition.shouldStop(snapshot())) {
                step();
            }
        } finally {
            running = false;
        }
    }

    @Override
    public SystemSnapshot snapshot() {
        requireInitialized();
        return buildSnapshot(-1L);
    }

    @Override
    public void addObserver(MetricsObserver observer) {
        metricsObservers.add(Objects.requireNonNull(observer, "observer must not be null"));
    }

    @Override
    public void addPatternObserver(PatternObserver observer) {
        patternObservers.add(Objects.requireNonNull(observer, "observer must not be null"));
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void stop() {
        running = false;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void requireInitialized() {
        if (manifoldSpace == null) {
            throw new IllegalStateException("ManifoldRunner has not been initialized");
        }
    }

    private SystemSnapshot buildSnapshot(long stepStartNanos) {
        long elapsedNanos = stepStartNanos < 0 ? 0L : (System.nanoTime() - stepStartNanos);

        int activeCount = 0;
        int immobilizedCount = 0;
        int staleCount = 0;

        for (Flatlander flatlander : flatlanders) {
            if (flatlander == null || flatlander.getState() == null) {
                continue;
            }

            activeCount++;

            FlatlanderState state = flatlander.getState();
            if (state.isImmobilized()) {
                immobilizedCount++;
            }
            if (state.isStale(currentStep, 1L)) {
                staleCount++;
            }
        }

        EmergenceMetricsSnapshot emergence = new SimpleEmergenceSnapshot(flatlanders);
        PerformanceMetricsSnapshot performance = new SimplePerformanceSnapshot(
                currentStep,
                activeCount,
                immobilizedCount,
                staleCount,
                elapsedNanos
        );

        return new RunnerSystemSnapshot(
                currentStep,
                List.copyOf(flatlanders),
                manifoldSpace,
                similarityMatcher,
                Map.of(
                        "active_flatlanders", activeCount,
                        "immobilized_count", immobilizedCount,
                        "stale_count", staleCount
                ),
                emergence,
                performance
        );
    }

    // ----------------------------------------------------------------
    // Inner types
    // ----------------------------------------------------------------

    private record RunnerExecutionContext(
            long currentStep,
            ManifoldSpace manifoldSpace,
            boolean synthetic,
            Map<String, Object> contextAttributes
    ) implements ExecutionContext {
        @Override
        public long getCurrentStep() {
            return currentStep;
        }

        @Override
        public ManifoldSpace getManifoldSpace() {
            return manifoldSpace;
        }

        @Override
        public boolean isSynthetic() {
            return synthetic;
        }

        @Override
        public Map<String, Object> getContextAttributes() {
            return contextAttributes;
        }
    }

    private record RunnerSystemSnapshot(
            long step,
            List<Flatlander> activeFlatlanders,
            ManifoldSpace manifoldSpace,
            SimilarityMatcher similarityMatcher,
            Map<String, Object> systemAttributes,
            EmergenceMetricsSnapshot emergenceMetrics,
            PerformanceMetricsSnapshot performanceMetrics
    ) implements SystemSnapshot {

        @Override
        public long getStep() {
            return step;
        }

        @Override
        public List<Flatlander> getActiveFlatlanders() {
            return Collections.unmodifiableList(activeFlatlanders);
        }

        @Override
        public ManifoldSpace getManifoldSpace() {
            return manifoldSpace;
        }

        @Override
        public SimilarityMatcher getSimilarityMatcher() {
            return similarityMatcher;
        }

        @Override
        public Map<String, Object> getSystemAttributes() {
            return Collections.unmodifiableMap(systemAttributes);
        }

        @Override
        public EmergenceMetricsSnapshot getEmergenceMetrics() {
            return emergenceMetrics;
        }

        @Override
        public PerformanceMetricsSnapshot getPerformanceMetrics() {
            return performanceMetrics;
        }
    }

    private static final class SimpleEmergenceSnapshot implements EmergenceMetricsSnapshot {
        private final double clusteringCoefficient;
        private final double averageSimilarityScore;
        private final int activeClusterCount;
        private final Map<String, Double> customMetrics;

        private SimpleEmergenceSnapshot(List<Flatlander> flatlanders) {
            int population = flatlanders == null ? 0 : flatlanders.size();

            // Conservative v1 placeholders
            this.clusteringCoefficient = population > 1 ? 0.0 : 0.0;
            this.averageSimilarityScore = 0.0;
            this.activeClusterCount = population > 0 ? 1 : 0;
            this.customMetrics = Map.of(
                    "population_count", (double) population
            );
        }

        @Override
        public double getClusteringCoefficient() {
            return clusteringCoefficient;
        }

        @Override
        public double getAverageSimilarityScore() {
            return averageSimilarityScore;
        }

        @Override
        public int getActiveClusterCount() {
            return activeClusterCount;
        }

        @Override
        public Map<String, Double> getCustomMetrics() {
            return customMetrics;
        }
    }

    private static final class SimplePerformanceSnapshot implements PerformanceMetricsSnapshot {
        private final double averageStepDurationMs;
        private final long totalStepsExecuted;
        private final int activeFlatlanderCount;
        private final int immobilizedCount;
        private final int staleCount;
        private final Map<String, Double> customMetrics;

        private SimplePerformanceSnapshot(
                long totalStepsExecuted,
                int activeFlatlanderCount,
                int immobilizedCount,
                int staleCount,
                long elapsedNanos
        ) {
            this.averageStepDurationMs = elapsedNanos / 1_000_000.0;
            this.totalStepsExecuted = totalStepsExecuted;
            this.activeFlatlanderCount = activeFlatlanderCount;
            this.immobilizedCount = immobilizedCount;
            this.staleCount = staleCount;
            this.customMetrics = Map.of(
                    "last_step_duration_ms", averageStepDurationMs
            );
        }

        @Override
        public double getAverageStepDurationMs() {
            return averageStepDurationMs;
        }

        @Override
        public long getTotalStepsExecuted() {
            return totalStepsExecuted;
        }

        @Override
        public int getActiveFlatlanderCount() {
            return activeFlatlanderCount;
        }

        @Override
        public int getImmobilizedCount() {
            return immobilizedCount;
        }

        @Override
        public int getStaleCount() {
            return staleCount;
        }

        @Override
        public Map<String, Double> getCustomMetrics() {
            return customMetrics;
        }
    }
}