package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Curvature information at a point on the manifold.
 */
public interface CurvatureField {
    double getScalarIntensity();
    Optional<double[]> getCurvatureTensor();
    boolean isFlat();
    boolean isSingular();
}
