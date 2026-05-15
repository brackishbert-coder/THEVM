package workbench;

//-------------------------------------------------------
//Response DTOs
//-------------------------------------------------------


import java.util.List;
import java.util.Map;

public record ManifoldSeedResponseDTO(
 String manifoldSeedId,
 String manifoldId,
 String status,                         // "accepted" | "rejected" | "partial"
 ValidationResultDTO validationResult,
 ManifoldDescriptorViewDTO compiledDescriptor,
 List<PointDTO> normalizedCurve,
 List<CurvatureSampleDTO> curvatureSamples,
 List<GateDescriptorDTO> gateDescriptors,
 NormalizationTransformDTO normalizationTransform,
 ProvenanceSummaryDTO provenanceSummary,
 List<String> warnings
) {}










