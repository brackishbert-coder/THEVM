package workbench;

import java.util.List;

public record ProvenanceSummaryDTO(
		 String manifoldSeedId,
		 String derivedFrom,
		 List<String> parentSeedIds,
		 String smoothingMethod,
		 String samplingMethod,
		 double closureGapBeforeSnap,
		 double resamplingErrorEstimate,
		 List<String> selfIntersectionWarnings,
		 int hotspotCandidatesRejected,
		 int hotspotCandidatesAccepted
		) {}
