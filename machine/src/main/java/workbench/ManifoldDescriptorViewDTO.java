package workbench;

import java.util.Map;

public record ManifoldDescriptorViewDTO(
		 String manifoldId,
		 String topologyType,
		 String backendType,
		 int intrinsicDimensionality,
		 double totalArcLength,
		 BoundingBoxDTO boundingBox,
		 int gateCount,
		 Map<String, Object> properties
		) {}
