package workbench;

import java.util.List;

public record SeedDebugDTO(
		 double closureGapBeforeSnap,
		 double resamplingErrorEstimate,
		 List<String> selfIntersectionWarnings,
		 int hotspotCandidatesRejected
		) {}