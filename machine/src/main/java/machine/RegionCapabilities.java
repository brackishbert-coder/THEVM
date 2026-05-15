package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Structural affordances of a manifold region — what topology permits.
 *
 * IMPORTANT: Capabilities are structural only. They describe what the
 * manifold geometry allows. They must never encode runtime policy or
 * behavioral weights. Use RegionPolicy for those.
 */
public interface RegionCapabilities {
    boolean canSpawn();
    boolean canMerge();
    boolean canSplit();
    boolean allowsLayerJump();
    boolean allowsWraparound();
    boolean isTraversable();
}
