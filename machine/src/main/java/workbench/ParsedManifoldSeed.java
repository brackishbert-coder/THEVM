package workbench;

import java.util.List;

/**
 * Intermediate representation — backend-native, not yet VM objects.
 * Coordinates are normalized to [0,1] space.
 */
public record ParsedManifoldSeed(
    String manifoldSeedId,
    List<String> parentSeedIds,
    String derivedFrom,
    List<NormalizedPoint> normalizedSampledPoints,
    List<NormalizedHotspot> hotspotCandidates,
    NormalizationTransform normalizationTransform,
    SeedProvenance provenance
) {}






