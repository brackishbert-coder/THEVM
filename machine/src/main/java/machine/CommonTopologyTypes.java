package machine;


/**
 * Canonical topology type constants for ManifoldDescriptor.getTopologyType().
 *
 * RULE: No new topology type may be returned from getTopologyType() without
 * a constant defined here first.
 */
public final class CommonTopologyTypes {
    private CommonTopologyTypes() {}

    public static final String FLAT          = "flat";
    public static final String TOROIDAL      = "toroidal";
    public static final String CYLINDRICAL   = "cylindrical";
    public static final String FLATLAND_CUBE = "flatland_cube";
    public static final String CUSTOM        = "custom";
}