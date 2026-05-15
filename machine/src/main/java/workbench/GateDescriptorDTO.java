package workbench;

public record GateDescriptorDTO(
		 String gateId,
		 double x,
		 double y,
		 double intensity,
		 String gateType,
		 double confidence,
		 String sourceHotspotCandidateType
		) {}