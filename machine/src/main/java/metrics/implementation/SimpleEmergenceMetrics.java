package metrics.implementation;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import machine.Flatlander;
import machine.FlatlanderState;
import machine.ManifoldSpace;
import machine.MotionVector;
import machine.Position;
import metrics.EmergenceMetrics;

/**
 * A simple generic emergence-metrics implementation.
 *
 * Computed metrics:
 * - population_count
 * - unique_position_ratio
 * - average_pairwise_distance
 * - centroid_x
 * - centroid_y
 * - motion_coherence
 * - immobilized_ratio
 * - stale_ratio
 *
 * Notes:
 * - this implementation assumes positions expose numeric coordinates
 * - pairwise distance uses Euclidean distance over Position.getCoordinates()
 * - if coordinates are unavailable or malformed, some metrics fall back to 0.0
 *
 * This class is intended as a practical v1 baseline rather than a final theory
 * of emergence.
 */
public class SimpleEmergenceMetrics implements EmergenceMetrics {

    private final Map<String, Double> metrics = new HashMap<>();

    @Override
    public void update(List<Flatlander> flatlanders, ManifoldSpace manifold, long stepCount) {
        Objects.requireNonNull(flatlanders, "flatlanders must not be null");
        Objects.requireNonNull(manifold, "manifold must not be null");

        int population = flatlanders.size();
        metrics.put("population_count", (double) population);

        if (population == 0) {
            zeroOutEmptyCase();
            return;
        }

        int immobilizedCount = 0;
        int staleCount = 0;

        Map<String, Integer> occupiedPositions = new HashMap<>();

        double centroidX = 0.0;
        double centroidY = 0.0;
        int centroidSamples = 0;

        double motionMagnitudeSum = 0.0;
        double motionComponentSumX = 0.0;
        double motionComponentSumY = 0.0;
        int motionSamples = 0;

        for (Flatlander flatlander : flatlanders) {
            if (flatlander == null) {
                continue;
            }

            FlatlanderState state = flatlander.getState();
            if (state == null) {
                continue;
            }

            if (state.isImmobilized()) {
                immobilizedCount++;
            }

            if (state.isStale(stepCount, 1L)) {
                staleCount++;
            }

            Position position = state.getPosition();
            if (position != null) {
                double[] coords = position.getCoordinates();
                if (coords != null && coords.length >= 2) {
                    centroidX += coords[0];
                    centroidY += coords[1];
                    centroidSamples++;
                }

                occupiedPositions.merge(positionKey(position), 1, Integer::sum);
            }

            MotionVector velocity = state.getVelocity();
            if (velocity != null) {
                double[] components = velocity.getComponents();
                if (components != null && components.length >= 2) {
                    double mag = velocity.getMagnitude();
                    motionMagnitudeSum += mag;
                    motionComponentSumX += components[0];
                    motionComponentSumY += components[1];
                    motionSamples++;
                }
            }
        }

        metrics.put("immobilized_ratio", population == 0 ? 0.0 : ((double) immobilizedCount / population));
        metrics.put("stale_ratio", population == 0 ? 0.0 : ((double) staleCount / population));
        metrics.put("unique_position_ratio", population == 0 ? 0.0 : ((double) occupiedPositions.size() / population));

        if (centroidSamples > 0) {
            centroidX /= centroidSamples;
            centroidY /= centroidSamples;
        }
        metrics.put("centroid_x", centroidX);
        metrics.put("centroid_y", centroidY);

        metrics.put("average_pairwise_distance", computeAveragePairwiseDistance(flatlanders));
        metrics.put("motion_coherence", computeMotionCoherence(motionSamples, motionMagnitudeSum, motionComponentSumX, motionComponentSumY));
    }

    @Override
    public Map<String, Double> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(metrics));
    }

    // ------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------

    private void zeroOutEmptyCase() {
        metrics.put("unique_position_ratio", 0.0);
        metrics.put("average_pairwise_distance", 0.0);
        metrics.put("centroid_x", 0.0);
        metrics.put("centroid_y", 0.0);
        metrics.put("motion_coherence", 0.0);
        metrics.put("immobilized_ratio", 0.0);
        metrics.put("stale_ratio", 0.0);
    }

    private double computeAveragePairwiseDistance(List<Flatlander> flatlanders) {
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < flatlanders.size(); i++) {
            Flatlander a = flatlanders.get(i);
            if (a == null || a.getState() == null || a.getState().getPosition() == null) {
                continue;
            }

            double[] ac = a.getState().getPosition().getCoordinates();
            if (ac == null || ac.length < 2) {
                continue;
            }

            for (int j = i + 1; j < flatlanders.size(); j++) {
                Flatlander b = flatlanders.get(j);
                if (b == null || b.getState() == null || b.getState().getPosition() == null) {
                    continue;
                }

                double[] bc = b.getState().getPosition().getCoordinates();
                if (bc == null || bc.length < 2) {
                    continue;
                }

                double dx = bc[0] - ac[0];
                double dy = bc[1] - ac[1];
                sum += Math.sqrt(dx * dx + dy * dy);
                count++;
            }
        }

        return count == 0 ? 0.0 : sum / count;
    }

    /**
     * Measures how aligned motion is across the population.
     *
     * 1.0 = perfectly aligned
     * 0.0 = completely incoherent or no motion
     */
    private double computeMotionCoherence(
            int motionSamples,
            double motionMagnitudeSum,
            double motionComponentSumX,
            double motionComponentSumY
    ) {
        if (motionSamples == 0 || motionMagnitudeSum == 0.0) {
            return 0.0;
        }

        double resultantMagnitude = Math.sqrt(
                motionComponentSumX * motionComponentSumX +
                motionComponentSumY * motionComponentSumY
        );

        return resultantMagnitude / motionMagnitudeSum;
    }

    private String positionKey(Position position) {
        double[] coords = position.getCoordinates();
        if (coords == null || coords.length == 0) {
            return "null_position";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < coords.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(coords[i]);
        }
        return sb.toString();
    }
}