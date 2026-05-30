package execution.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import execution.SOMObserver;
import execution.SOMVectorSink;
import machine.Flatlander;
import machine.FlatlanderState;
import machine.ManifoldSpace;
import machine.MotionVector;
import machine.Position;

/**
 * A simple SOM observer that encodes flatlander state into numeric feature vectors
 * and forwards them to a pluggable sink.
 *
 * Default encoding includes:
 * - position coordinates
 * - velocity components
 * - optional step index
 *
 * This class does not implement a SOM directly.
 * Instead, it acts as the bridge between the manifold simulation and a SOM engine.
 */
public class SimpleSOMObserver implements SOMObserver {

    private final SOMVectorSink sink;
    private final boolean includeStepFeature;
    private final List<ObservationRecord> history = new ArrayList<>();

    public SimpleSOMObserver(SOMVectorSink sink) {
        this(sink, true);
    }

    public SimpleSOMObserver(SOMVectorSink sink, boolean includeStepFeature) {
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
        this.includeStepFeature = includeStepFeature;
    }

    @Override
    public void observe(List<Flatlander> flatlanders, ManifoldSpace manifold, long stepCount) {
        Objects.requireNonNull(flatlanders, "flatlanders must not be null");
        Objects.requireNonNull(manifold, "manifold must not be null");

        for (Flatlander flatlander : flatlanders) {
            if (flatlander == null) {
                continue;
            }

            double[] vector = encodeFlatlander(flatlander, stepCount);
            if (vector == null) {
                continue;
            }

            ObservationRecord record = new ObservationRecord(
                    stepCount,
                    flatlander.getId(),
                    vector
            );

            history.add(record);
            sink.accept(record);
        }
    }

    /**
     * Encodes one flatlander into a numeric feature vector.
     *
     * Current encoding:
     * - all position coordinates
     * - all velocity components
     * - optional step feature appended at the end
     */
    protected double[] encodeFlatlander(Flatlander flatlander, long stepCount) {
        FlatlanderState state = flatlander.getState();
        if (state == null) {
            return null;
        }

        Position position = state.getPosition();
        MotionVector velocity = state.getVelocity();

        if (position == null || velocity == null) {
            return null;
        }

        double[] coords = position.getCoordinates();
        double[] motion = velocity.getComponents();

        if (coords == null || motion == null) {
            return null;
        }

        int baseLength = coords.length + motion.length;
        int totalLength = includeStepFeature ? baseLength + 1 : baseLength;

        double[] result = new double[totalLength];
        int index = 0;

        for (double c : coords) {
            result[index++] = c;
        }

        for (double m : motion) {
            result[index++] = m;
        }

        if (includeStepFeature) {
            result[index] = stepCount;
        }

        return result;
    }

    public List<ObservationRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * One encoded SOM observation.
     */
    public record ObservationRecord(
            long stepCount,
            String flatlanderId,
            double[] vector
    ) {
    }
}