package machine;

/**
 * Canonical backend type constants for ManifoldDescriptor.getBackendType().
 *
 * RULE: No new backend type may be returned from getBackendType() without
 * a constant defined here first.
 */
public final class CommonBackendTypes {
    private CommonBackendTypes() {}

    public static final String DISCRETE   = "discrete";
    public static final String CONTINUOUS = "continuous";
}