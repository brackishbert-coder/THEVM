package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * The primary manifold space contract.
 *
 * Implementations must document their holonomy policy as one of:
 *   PRESERVE  — parallel transport preserves holonomy faithfully
 *   DAMP      — holonomy accumulation is damped over time
 *   CORRECT   — holonomy is corrected back toward zero (use with caution)
 *
 * Silent correction is prohibited. Holonomy is an experiment-level
 * observable, not an error to hide.
 */
public interface ManifoldSpace {

    ManifoldDescriptor getDescriptor();

    // Position and geometry
    Position origin();
    boolean isValidPosition(Position p);
    int getLocalManifoldDimensionality(Position p);
    CurvatureField getCurvatureAt(Position p);
    RegionCapabilities getCapabilitiesAt(Position p);
    RegionPolicy getPolicyAt(Position p);

    // Movement
    Position move(Position from, MotionVector vector);
    MotionAdaptationResult adaptMotion(Position at, MotionVector vector);

    /**
     * Parallel transports a MotionVector along a path from one position to another.
     *
     * HOLONOMY CONTRACT: implementations must document their holonomy policy.
     * Discrete backends approximate parallel transport without true holonomy preservation;
     * this limitation must be documented explicitly in the implementation.
     */
    MotionVector parallelTransport(Position from, Position to, MotionVector vector);

    /**
     * Computes accumulated holonomy for a closed loop path.
     * Returns empty Optional if this backend does not support holonomy computation.
     */
    Optional<MotionVector> computeHolonomy(List<Position> closedPath);

    // Neighborhood and transitions
    List<Transition> getTransitionsFrom(Position p, TransitionQuery query);
    NeighborhoodQuery neighborhoodQuery(Position center, double radius);

    // Geodesics
    Optional<List<Position>> findGeodesic(Position from, Position to);
    double geodesicDistance(Position from, Position to);

    // Execution context
    /**
     * Returns a synthetic ExecutionContext for use outside active execution.
     * Callers must check isSynthetic() == true before using in time-sensitive logic.
     * Never use raw -1L sentinels as step substitutes.
     */
    ExecutionContext defaultContext();

    // Debug
    Optional<PositionDebug> debugProject(Position p);
}
