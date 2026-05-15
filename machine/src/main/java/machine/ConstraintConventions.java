package machine;

/**
 * Canonical constraint key constants for TransitionQuery.getConstraints()
 * and NeighborhoodQuery constraint maps.
 *
 * RULE: No new constraint key may be passed to any query map without a
 * constant defined here first.
 */
public final class ConstraintConventions {
    private ConstraintConventions() {}

    public static final String ALLOW_LAYER_JUMP          = "allow_layer_jump";
    public static final String ALLOW_WRAPAROUND          = "allow_wraparound";
    public static final String MAX_CURVATURE             = "max_curvature";
    public static final String REQUIRED_TRANSITION_TYPE  = "required_transition_type";
    public static final String EXCLUDE_IMMOBILIZED       = "exclude_immobilized";
    public static final String EXCLUDE_STALE             = "exclude_stale";
    public static final String MIN_SIMILARITY            = "min_similarity";
    public static final String LINEAGE_FILTER            = "lineage_filter";
}