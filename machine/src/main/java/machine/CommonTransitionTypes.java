package machine;


/**
 * Canonical transition type constants for Transition.getTransitionType().
 *
 * Kept separate from CommonEventTypes because transition types describe
 * movement semantics between positions, not system-level events.
 *
 * RULE: No new transition type may be returned from getTransitionType()
 * without a constant defined here first.
 */
public final class CommonTransitionTypes {
    private CommonTransitionTypes() {}

    public static final String LOCAL_STEP = "local_step";
    public static final String LAYER_JUMP = "layer_jump";
    public static final String WRAPAROUND = "wraparound";
    public static final String SHORTCUT   = "shortcut";
    public static final String PORTAL     = "portal";
}