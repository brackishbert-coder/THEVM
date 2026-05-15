package workbench;


import java.util.List;

import org.springframework.stereotype.Component;
@Component
public class ManifoldSeedParser {

    public ParsedManifoldSeed parse(ManifoldSeedRequestDTO req) {
        // Compute normalization transform from bounding box
        BoundingBoxDTO bb   = req.metadata().boundingBox();
        double width        = bb.maxX() - bb.minX();
        double height       = bb.maxY() - bb.minY();
        double scale        = Math.max(width, height);
        NormalizationTransform transform =
            new NormalizationTransform(bb.minX(), bb.minY(), scale > 0 ? scale : 1.0);

        // Normalize sampled points
        List<NormalizedPoint> normalizedPoints = req.arcLengthSampledPoints().stream()
            .map(p -> transform.apply(p.x(), p.y()))
            .toList();

        // Normalize hotspot candidates
        List<NormalizedHotspot> hotspots = req.rectangleHotspots() == null
            ? List.of()
            : req.rectangleHotspots().stream()
                .map(h -> {
                    NormalizedPoint np = transform.apply(h.x(), h.y());
                    return new NormalizedHotspot(
                        np.x(), np.y(), h.intensity(), h.candidateType());
                })
                .toList();

        // Build provenance record
        SeedProvenance provenance = req.debug() == null
            ? new SeedProvenance(
                req.metadata().smoothingMethod(),
                req.metadata().samplingMethod(),
                0, 0, List.of(), 0)
            : new SeedProvenance(
                req.metadata().smoothingMethod(),
                req.metadata().samplingMethod(),
                req.debug().closureGapBeforeSnap(),
                req.debug().resamplingErrorEstimate(),
                req.debug().selfIntersectionWarnings(),
                req.debug().hotspotCandidatesRejected());

        return new ParsedManifoldSeed(
            req.manifoldSeedId(),
            req.parentSeedIds(),
            req.derivedFrom(),
            normalizedPoints,
            hotspots,
            transform,
            provenance
        );
    }
}