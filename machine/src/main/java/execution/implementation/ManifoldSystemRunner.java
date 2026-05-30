package execution.implementation;


import java.util.List;
import java.util.Map;

import execution.ManifoldSystemRunner.RunnerSnapshot;
import machine.BasicFlatlander;
import machine.BasicSimilarityMatcher;
import machine.Flatlander;
import machine.ManifoldEvent;
import machine.MetricsObserver;
import machine.MotionVector;
import machine.PatternObserver;
import machine.SimilarityMatcher;
import machine.StoppingCondition;
import machine.SystemSnapshot;
import manifolds.continuous.implementation.FlatPlane;

/**
 * Minimal bootstrap runner for the manifold system.
 *
 * Wires together:
 * - a manifold
 * - a similarity matcher
 * - a few starter flatlanders
 * - a concrete runner
 * - console observers
 *
 * This is intended as an end-to-end smoke test.
 */
public class ManifoldSystemRunner {

    public static void main(String[] args) {
        FlatPlane manifold = new FlatPlane("demo-flat-plane");
        SimilarityMatcher similarityMatcher = new BasicSimilarityMatcher();

        // Starter flatlanders
        Flatlander a = new BasicFlatlander(
                "flatlander_A",
                manifold.origin(),
                new Vector2(1.0, 0.0)
        );

        Flatlander b = new BasicFlatlander(
                "flatlander_B",
                manifold.move(manifold.origin(), new Vector2(2.0, 0.0)),
                new Vector2(0.0, 1.0)
        );

        Flatlander c = new BasicFlatlander(
                "flatlander_C",
                manifold.move(manifold.origin(), new Vector2(-1.0, -1.0)),
                new Vector2(0.5, 0.5)
        );

        // Assumes you have SimpleManifoldRunner available
        SimpleManifoldRunner runner = new SimpleManifoldRunner(similarityMatcher);

        runner.addObserver(new MetricsObserver() {
            @Override
            public void recordEvent(ManifoldEvent event) {
                // no-op for this bootstrap
            }

            @Override
            public void onStepComplete(SystemSnapshot snapshot) {
                System.out.println(
                        "[Metrics] step=" + snapshot.getStep()
                        + " active=" + snapshot.getActiveFlatlanders().size()
                        + " emergence=" + snapshot.getEmergenceMetrics().getCustomMetrics()
                        + " performance=" + snapshot.getPerformanceMetrics().getCustomMetrics()
                );
            }
        });

        runner.addPatternObserver(new PatternObserver() {
            @Override
            public void onPatternDetected(String patternType, List<Flatlander> participants, SystemSnapshot snapshot) {
                System.out.println(
                        "[Pattern] type=" + patternType
                        + " participants=" + participants.size()
                        + " step=" + snapshot.getStep()
                );
            }
        });

        runner.initialize(manifold, List.of(a, b, c));

        runner.runUntil(new StoppingCondition() {

           

            @Override
            public String getDescription() {
                return "Stop after 10 steps";
            }

			@Override
			public boolean shouldStop(SystemSnapshot snapshot) {
				return snapshot.getStep() >= 10;
			}

			@Override
			public boolean shouldStop(RunnerSnapshot snapshot) {
				return snapshot.getStep() >= 10;
			}
        });

        // Similarity demo after run
        double simAB = similarityMatcher.computeSimilarity(
                a.getSimilaritySignature(),
                b.getSimilaritySignature()
        );

        System.out.println("[Similarity] A vs B = " + simAB);
        System.out.println("[Done] Final step = " + runner.snapshot().getStep());
    }

    /**
     * Minimal 2D motion vector implementation for bootstrap/demo use.
     */
    private record Vector2(double x, double y) implements MotionVector {
        @Override
        public double[] getComponents() {
            return new double[]{x, y};
        }

        @Override
        public double getMagnitude() {
            return Math.sqrt(x * x + y * y);
        }

        @Override
        public MotionVector scale(double factor) {
            return new Vector2(x * factor, y * factor);
        }

        @Override
        public MotionVector add(MotionVector other) {
            double[] c = other.getComponents();
            return new Vector2(x + c[0], y + c[1]);
        }
    }
}