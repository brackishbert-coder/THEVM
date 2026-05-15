package workbench;


import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
@Component
public class ManifoldResponseAssembler {

    public ManifoldSeedResponseDTO rejected(String seedId, ValidationResult validation) {
        return new ManifoldSeedResponseDTO(
            seedId, null, "rejected",
            new ValidationResultDTO(false, validation.errors(), validation.warnings()),
            null, List.of(), List.of(), List.of(), null, null,
            validation.errors()
        );
    }

    public ManifoldSeedResponseDTO assemble(
            ManifoldSeedRequestDTO request,
            ParsedManifoldSeed parsed,
            CompiledManifold compiled,
            List<CurvatureSampleDTO> curvatureSamples,
            List<GateDescriptorDTO> gates,
            ValidationResult validation) {

        // Project normalized points back to response DTOs
        List<PointDTO> normalizedCurve = parsed.normalizedSampledPoints().stream()
            .map(p -> new PointDTO(p.x(), p.y()))
            .toList();

        NormalizationTransformDTO transform = new NormalizationTransformDTO(
            parsed.normalizationTransform().offsetX(),
            parsed.normalizationTransform().offsetY(),
            parsed.normalizationTransform().scale()
        );

        ProvenanceSummaryDTO provenance = new ProvenanceSummaryDTO(
            parsed.manifoldSeedId(),
            parsed.derivedFrom(),
            parsed.parentSeedIds(),
            parsed.provenance().smoothingMethod(),
            parsed.provenance().samplingMethod(),
            parsed.provenance().closureGap(),
            parsed.provenance().resamplingError(),
            parsed.provenance().selfIntersectionWarnings(),
            parsed.provenance().hotspotCandidatesRejected(),
            gates.size()
        );

        return new ManifoldSeedResponseDTO(
            parsed.manifoldSeedId(),
            compiled.manifoldId(),
            "accepted",
            new ValidationResultDTO(true, List.of(), validation.warnings()),
            descriptorView(compiled),
            normalizedCurve,
            curvatureSamples,
            gates,
            transform,
            provenance,
            validation.warnings()
        );
    }

    public ManifoldDescriptorViewDTO descriptorView(CompiledManifold compiled) {
        var descriptor = compiled.manifoldSpace().getDescriptor();
        var origin     = compiled.manifoldSpace().origin();
        int dims       = compiled.manifoldSpace().getLocalManifoldDimensionality(origin);

        double arcLength = 0.0;
        if (compiled.manifoldSpace() instanceof manifolds.continuous.implemention.CurveManifold curve) {
            arcLength = curve.getTotalArcLength();
        }

        return new ManifoldDescriptorViewDTO(
            compiled.manifoldId(),
            descriptor.getTopologyType()  != null ? descriptor.getTopologyType().toString()  : "CUSTOM",
            descriptor.getBackendType()   != null ? descriptor.getBackendType().toString()    : "CONTINUOUS",
            dims,
            arcLength,
            null,
            compiled.gates().size(),
            descriptor.getProperties() != null ? descriptor.getProperties() : Map.of()
        );
    }
}