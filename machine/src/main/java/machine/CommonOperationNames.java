package machine;


/**
 * Canonical operation name constants for RegionPolicy.getOperationWeight()
 * and RegionPolicy.getPolicyWeights().
 *
 * RULE: No new operation name may be passed to getOperationWeight() without
 * a constant defined here first.
 *
 * For backend types see CommonBackendTypes.
 * For topology types see CommonTopologyTypes.
 */
public final class CommonOperationNames {
    private CommonOperationNames() {}

    public static final String SPAWN            = "spawn";
    public static final String MERGE            = "merge";
    public static final String SPLIT            = "split";
    public static final String VELOCITY_DAMPING = "velocity_damping";
    public static final String SIMILARITY_BIND  = "similarity_bind";
    public static final String DORMANCY_ENTER   = "dormancy_enter";
    public static final String DORMANCY_EXIT    = "dormancy_exit";
}