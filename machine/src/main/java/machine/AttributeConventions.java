package machine;

/**
 * Canonical attribute key constants for all Map<String, Object> attribute maps
 * used in FlatlanderState, SystemSnapshot, ManifoldEvent, ExecutionContext, etc.
 *
 * RULE: No new key may be inserted into any attribute map without a constant
 * defined here first. No local ad hoc keys.
 */
public final class AttributeConventions {
    private AttributeConventions() {}

    // FlatlanderState attributes
    public static final String VELOCITY_DAMPING      = "velocity_damping";
    public static final String DORMANCY_DEPTH        = "dormancy_depth";
    public static final String LAST_ACTIVE_STEP      = "last_active_step";
    public static final String SPAWN_COUNT           = "spawn_count";
    public static final String MERGE_COUNT           = "merge_count";
    public static final String HOLONOMY_ACCUMULATED  = "holonomy_accumulated";

    // ManifoldDescriptor / backend attributes
    public static final String BACKEND_TYPE          = "backend_type";
    public static final String PARTITION_ID          = "partition_id";
    public static final String CACHE_HIT_RATE        = "cache_hit_rate";

    // ManifoldEvent attributes
    public static final String CURVATURE_AT_EVENT    = "curvature_at_event";
    public static final String HOLONOMY_ANGLE        = "holonomy_angle";
    public static final String GEODESIC_ERROR        = "geodesic_error";

    // SystemSnapshot / ExecutionContext attributes
    public static final String RUN_ID                = "run_id";
    public static final String MANIFOLD_DESCRIPTOR   = "manifold_descriptor";
    public static final String RANDOM_SEED           = "random_seed";
    public static final String TOTAL_STEP_COUNT      = "total_step_count";
    public static final String ACTIVE_FLATLANDERS    = "active_flatlanders";
    public static final String IMMOBILIZED_COUNT     = "immobilized_count";
    public static final String STALE_COUNT           = "stale_count";
}