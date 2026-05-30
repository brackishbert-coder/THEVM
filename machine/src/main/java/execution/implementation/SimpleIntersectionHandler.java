package execution.implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import execution.IntersectionHandler;
import machine.Flatlander;
import machine.FlatlanderState;
import machine.ManifoldSpace;
import machine.MergeResult;
import machine.Position;

/**
 * A conservative intersection handler that detects flatlanders
 * occupying the same position and attempts pairwise merges.
 *
 * Behavior:
 * - groups flatlanders by their current Position
 * - if 2 or more flatlanders share the same position, they are considered intersecting
 * - for each intersecting group, attempts pairwise merges
 * - merge decisions remain owned by Flatlander.canMergeWith(...) and mergeWith(...)
 *
 * Notes:
 * - this implementation only handles exact position equality
 * - it does not handle radius-based overlap or geodesic crossing detection
 * - it does not remove merged flatlanders from the runner population itself;
 *   that remains runner/lifecycle policy
 */
public class SimpleIntersectionHandler implements IntersectionHandler {

    @Override
    public void resolve(List<Flatlander> flatlanders, ManifoldSpace manifold, long stepCount) {
        Objects.requireNonNull(flatlanders, "flatlanders must not be null");
        Objects.requireNonNull(manifold, "manifold must not be null");

        Map<Position, List<Flatlander>> byPosition = new HashMap<>();

        for (Flatlander flatlander : flatlanders) {
            if (flatlander == null) {
                continue;
            }

            FlatlanderState state = flatlander.getState();
            if (state == null) {
                continue;
            }

            Position position = state.getPosition();
            if (position == null) {
                continue;
            }

            byPosition.computeIfAbsent(position, p -> new ArrayList<>()).add(flatlander);
        }

        for (List<Flatlander> collidingGroup : byPosition.values()) {
            if (collidingGroup.size() < 2) {
                continue;
            }

            resolveGroup(collidingGroup);
        }
    }

    /**
     * Attempt pairwise merges within one colliding group.
     */
    protected void resolveGroup(List<Flatlander> collidingGroup) {
        for (int i = 0; i < collidingGroup.size(); i++) {
            Flatlander a = collidingGroup.get(i);
            if (a == null) {
                continue;
            }

            for (int j = i + 1; j < collidingGroup.size(); j++) {
                Flatlander b = collidingGroup.get(j);
                if (b == null || a == b) {
                    continue;
                }

                if (!a.canMergeWith(b)) {
                    continue;
                }

                MergeResult result = a.mergeWith(b, null);

                // Conservative v1 behavior:
                // we do not enforce lifecycle removal here.
                // The merge result is allowed to carry meaning without
                // this handler mutating runner population directly.
                if (result != null && result.isSuccess()) {
                    // Stop after first successful merge involving 'a'
                    // to avoid repeated merges in the same pass.
                    break;
                }
            }
        }
    }
}