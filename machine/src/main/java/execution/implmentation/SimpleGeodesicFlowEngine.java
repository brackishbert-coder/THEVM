package execution.implmentation;

import execution.GeodesicFlowEngine;
import machine.Flatlander;
import machine.FlatlanderState;
import machine.ManifoldSpace;
import machine.MotionAdaptationResult;
import machine.MotionVector;
import machine.Position;
import machine.RegionCapabilities;
import machine.RegionPolicy;

import java.util.Objects;
import java.util.Optional;

/**
 * A simple default geodesic flow engine.
 *
 * Behavior:
 * - reads the flatlander's current position and velocity
 * - checks structural traversability at the current region
 * - applies optional region policy correction to the current motion
 * - applies manifold-level motion adaptation
 * - advances using manifold.move(...)
 * - writes the resulting position back into the flatlander state
 *
 * This implementation is intentionally conservative:
 * - no collision handling
 * - no acceleration model
 * - no explicit timestep scaling
 * - no path buffering
 *
 * It is a good first engine for getting the system moving.
 */
public class SimpleGeodesicFlowEngine implements GeodesicFlowEngine {

    @Override
    public void advance(Flatlander flatlander, ManifoldSpace manifold, long stepCount) {
        Objects.requireNonNull(flatlander, "flatlander must not be null");
        Objects.requireNonNull(manifold, "manifold must not be null");

        FlatlanderState state = flatlander.getState();
        if (state == null) {
            return;
        }

        Position currentPosition = state.getPosition();
        MotionVector currentVelocity = state.getVelocity();

        if (currentPosition == null || currentVelocity == null) {
            return;
        }

        if (!manifold.isValidPosition(currentPosition)) {
            return;
        }

        RegionCapabilities capabilities = manifold.getCapabilitiesAt(currentPosition);
        if (capabilities == null || !capabilities.isTraversable()) {
            return;
        }

        MotionVector workingVelocity = currentVelocity;

        RegionPolicy policy = manifold.getPolicyAt(currentPosition);
        if (policy != null) {
            Optional<MotionVector> corrected = policy.getSuggestedCorrection(workingVelocity);
            if (corrected.isPresent()) {
                workingVelocity = corrected.get();
            }
        }

        MotionAdaptationResult adapted = manifold.adaptMotion(currentPosition, workingVelocity);
        if (adapted != null && adapted.getAdaptedVector() != null) {
            workingVelocity = adapted.getAdaptedVector();
        }

        Position nextPosition = manifold.move(currentPosition, workingVelocity);

        if (nextPosition != null && manifold.isValidPosition(nextPosition)) {
            state.setPosition(nextPosition);
        }
    }
}