package machine;

/**
 * Canonical event type constants for ManifoldEvent.getEventType().
 *
 * For transition movement types see CommonTransitionTypes.
 * These are system-level execution and interaction events only.
 *
 * RULE: No new event type may be emitted without a constant defined here first.
 */
public final class CommonEventTypes {
    private CommonEventTypes() {}

    public static final String EVENT_SPAWN             = "spawn";
    public static final String EVENT_SPAWN_FAILED      = "spawn_failed";
    public static final String EVENT_MERGE             = "merge";
    public static final String EVENT_MERGE_FAILED      = "merge_failed";
    public static final String EVENT_SPLIT             = "split";
    public static final String EVENT_DORMANCY_ENTER    = "dormancy_enter";
    public static final String EVENT_DORMANCY_EXIT     = "dormancy_exit";
    public static final String EVENT_IMMOBILIZED       = "immobilized";
    public static final String EVENT_STALE             = "stale";
    public static final String EVENT_SIMILARITY_BOUND  = "similarity_bound";
    public static final String EVENT_HIGH_CURVATURE    = "high_curvature";
    public static final String EVENT_HOLONOMY_DETECTED = "holonomy_detected";
    public static final String EVENT_TOPOLOGY_CHANGE   = "topology_change";
    public static final String EVENT_BLOCKED_TRAVERSAL = "blocked_traversal";
}
