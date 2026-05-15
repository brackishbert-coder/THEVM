package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Metadata describing a manifold space instance.
 *
 * CONVENTION: backendType values in CommonBackendTypes.
 * CONVENTION: topologyType values in CommonTopologyTypes.
 */
public interface ManifoldDescriptor {
    String getName();
    String getBackendType();
    String getTopologyType();
    int getIntrinsicDimensionality();
    Map<String, Object> getProperties();
}
