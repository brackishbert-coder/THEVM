package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A self-describing similarity signature for a flatlander.
 *
 * CONVENTION NOTE: signatureType is a String for now.
 * Likely candidate for a dedicated CommonSignatureTypes constants class
 * once signature types stabilize across implementations.
 * Do not reference CommonEventTypes here — these are separate registries.
 */
public interface SimilaritySignature {
    String getSignatureType();
    Map<String, Double> getFeatures();
    Map<String, String> getFeatureDescriptions();
}
