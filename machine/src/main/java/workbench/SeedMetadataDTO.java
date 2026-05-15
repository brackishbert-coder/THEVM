package workbench;

public record SeedMetadataDTO(
		 int sampleCount,
		 double totalArcLength,
		 BoundingBoxDTO boundingBox,
		 String smoothingMethod,
		 String samplingMethod
		) {}