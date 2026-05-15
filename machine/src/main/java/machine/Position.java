package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Backend-agnostic position in manifold space.
 *
 * IMPORTANT: getRepresentationDimensionality() returns the internal
 * encoding dimensionality only — NOT the manifold's intrinsic dimensionality.
 * Use ManifoldSpace.getLocalManifoldDimensionality(Position) for geometry.
 */
public interface Position {
    double[] getCoordinates();
    int getRepresentationDimensionality();
    boolean isValid();
}
