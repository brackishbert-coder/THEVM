package workbench;

import java.util.List;

public record SeedProvenance(
	    String smoothingMethod,
	    String samplingMethod,
	    double closureGap,
	    double resamplingError,
	    List<String> selfIntersectionWarnings,
	    int hotspotCandidatesRejected
	) {}