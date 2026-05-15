package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Listener for structural changes to the manifold (topology changes,
 * region mutations, etc.)
 */
public interface ManifoldStructureListener {
    void onStructureChanged(ManifoldEvent event);
}
