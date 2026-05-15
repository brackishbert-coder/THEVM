package workbench;

//-------------------------------------------------------
//Request DTOs
//-------------------------------------------------------

import java.util.List;
import java.util.Map;

public record ManifoldSeedRequestDTO(
 String manifoldSeedId,
 List<String> parentSeedIds,
 String derivedFrom,
 List<PointDTO> rawStrokePoints,
 List<PointDTO> smoothedCurvePoints,
 List<PointDTO> arcLengthSampledPoints,
 List<HotspotCandidateDTO> rectangleHotspots,
 SeedMetadataDTO metadata,
 SeedDebugDTO debug
) {}






