package workbench;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class ManifoldSeedValidator {

    private static final int MIN_SAMPLE_COUNT = 10;
    private static final int MAX_SAMPLE_COUNT = 2000;
    private static final double MAX_CLOSURE_GAP_PCT = 0.15;
    private static final double MAX_RESAMPLING_ERR = 0.05;
    private static final double LOOP_CLOSURE_EPSILON = 1e-6;

    public ValidationResult validate(ManifoldSeedRequestDTO req) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (req == null) {
            errors.add("Request must not be null.");
            return new ValidationResult(false, errors, warnings);
        }

        validateRequiredFields(req, errors);
        if (!errors.isEmpty()) {
            return new ValidationResult(false, errors, warnings);
        }

        validateSeedIdentity(req, errors, warnings);
        validatePointCollections(req, errors, warnings);
        validateMetadata(req, errors, warnings);
        validateDebug(req, errors, warnings);
        validateHotspots(req, errors, warnings);
        validateSourceSpecificRules(req, errors, warnings);

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private void validateRequiredFields(ManifoldSeedRequestDTO req, List<String> errors) {
        if (isBlank(req.manifoldSeedId())) {
            errors.add("manifoldSeedId must not be blank.");
        }

        if (req.arcLengthSampledPoints() == null || req.arcLengthSampledPoints().isEmpty()) {
            errors.add("arcLengthSampledPoints must not be empty.");
        }

        if (req.metadata() == null) {
            errors.add("metadata must not be null.");
        }
    }

    private void validateSeedIdentity(ManifoldSeedRequestDTO req, List<String> errors, List<String> warnings) {
        if (!isKnownSeedSource(req.derivedFrom())) {
            errors.add("Unsupported derivedFrom value '" + req.derivedFrom() + "'.");
        }

        if (req.parentSeedIds() != null) {
            Set<String> seen = new HashSet<>();
            for (String id : req.parentSeedIds()) {
                if (isBlank(id)) {
                    warnings.add("parentSeedIds contains a blank value.");
                    continue;
                }
                if (!seen.add(id)) {
                    warnings.add("parentSeedIds contains duplicate value '" + id + "'.");
                }
            }
        }
    }

    private void validatePointCollections(ManifoldSeedRequestDTO req, List<String> errors, List<String> warnings) {
        List<PointDTO> sampled = req.arcLengthSampledPoints();
        int n = sampled.size();

        if (n < MIN_SAMPLE_COUNT) {
            errors.add("Sample count " + n + " below minimum " + MIN_SAMPLE_COUNT + ".");
        }
        if (n > MAX_SAMPLE_COUNT) {
            errors.add("Sample count " + n + " above maximum " + MAX_SAMPLE_COUNT + ".");
        }

        validatePoints("arcLengthSampledPoints", sampled, true, errors);

        if (req.rawStrokePoints() != null) {
            validatePoints("rawStrokePoints", req.rawStrokePoints(), false, errors);
        }

        if (req.smoothedCurvePoints() != null) {
            validatePoints("smoothedCurvePoints", req.smoothedCurvePoints(), false, errors);
        }

        if (hasDuplicateAdjacentPoints(sampled)) {
            warnings.add("arcLengthSampledPoints contains adjacent duplicate points.");
        }
    }

    private void validateMetadata(ManifoldSeedRequestDTO req, List<String> errors, List<String> warnings) {
        SeedMetadataDTO metadata = req.metadata();
        int actualCount = req.arcLengthSampledPoints().size();

        if (metadata.sampleCount() != actualCount) {
            warnings.add("metadata.sampleCount " + metadata.sampleCount()
                + " does not match actual point count " + actualCount + ".");
        }

        if (metadata.totalArcLength() <= 0) {
            errors.add("metadata.totalArcLength must be positive.");
        }

        if (metadata.boundingBox() == null) {
            errors.add("metadata.boundingBox must not be null.");
        } else {
            BoundingBoxDTO bb = metadata.boundingBox();
            if (!isFinite(bb.minX()) || !isFinite(bb.minY()) || !isFinite(bb.maxX()) || !isFinite(bb.maxY())) {
                errors.add("metadata.boundingBox contains non-finite values.");
            } else {
                if (bb.maxX() < bb.minX()) {
                    errors.add("metadata.boundingBox.maxX must be >= minX.");
                }
                if (bb.maxY() < bb.minY()) {
                    errors.add("metadata.boundingBox.maxY must be >= minY.");
                }
                if ((bb.maxX() - bb.minX()) == 0 && (bb.maxY() - bb.minY()) == 0) {
                    errors.add("metadata.boundingBox must span a non-zero area.");
                }
            }
        }

        if (isBlank(metadata.smoothingMethod())) {
            warnings.add("metadata.smoothingMethod is blank.");
        }

        if (isBlank(metadata.samplingMethod())) {
            warnings.add("metadata.samplingMethod is blank.");
        }
    }

    private void validateDebug(ManifoldSeedRequestDTO req, List<String> errors, List<String> warnings) {
        SeedDebugDTO debug = req.debug();
        if (debug == null) {
            return;
        }

        if (debug.closureGapBeforeSnap() < 0) {
            errors.add("debug.closureGapBeforeSnap must be >= 0.");
        }

        if (debug.resamplingErrorEstimate() < 0) {
            errors.add("debug.resamplingErrorEstimate must be >= 0.");
        }

        if (debug.hotspotCandidatesRejected() < 0) {
            errors.add("debug.hotspotCandidatesRejected must be >= 0.");
        }

        if (req.metadata() != null && req.metadata().totalArcLength() > 0) {
            double gapPct = debug.closureGapBeforeSnap() / req.metadata().totalArcLength();
            if (gapPct > MAX_CLOSURE_GAP_PCT) {
                warnings.add(String.format(
                    "Closure gap %.6f is %.1f%% of arc length — curve may not be well-closed.",
                    debug.closureGapBeforeSnap(), gapPct * 100.0));
            }
        }

        if (debug.resamplingErrorEstimate() > MAX_RESAMPLING_ERR) {
            warnings.add("High resampling error: " + debug.resamplingErrorEstimate());
        }

        if (debug.selfIntersectionWarnings() != null && !debug.selfIntersectionWarnings().isEmpty()) {
            warnings.addAll(debug.selfIntersectionWarnings().stream()
                .map(w -> "Self-intersection detected: " + w)
                .toList());
        }
    }

    private void validateHotspots(ManifoldSeedRequestDTO req, List<String> errors, List<String> warnings) {
        if (req.rectangleHotspots() == null) {
            return;
        }

        for (int i = 0; i < req.rectangleHotspots().size(); i++) {
            HotspotCandidateDTO h = req.rectangleHotspots().get(i);
            if (h == null) {
                errors.add("rectangleHotspots[" + i + "] must not be null.");
                continue;
            }

            if (!isFinite(h.x()) || !isFinite(h.y())) {
                errors.add("rectangleHotspots[" + i + "] contains non-finite coordinates.");
            }

            if (!isFinite(h.intensity())) {
                errors.add("rectangleHotspots[" + i + "].intensity must be finite.");
            } else if (h.intensity() < 0.0 || h.intensity() > 1.0) {
                warnings.add("rectangleHotspots[" + i + "].intensity " + h.intensity()
                    + " is outside expected range [0,1].");
            }

            if (isBlank(h.candidateType())) {
                warnings.add("rectangleHotspots[" + i + "].candidateType is blank.");
            }
        }
    }

    private void validateSourceSpecificRules(ManifoldSeedRequestDTO req, List<String> errors, List<String> warnings) {
        String source = req.derivedFrom();
        if (source == null) {
            return;
        }

        switch (source) {
            case "user_drawn_curve" -> validateUserDrawnCurve(req, warnings);
            case "optimization_output" -> validateOptimizationOutput(req, errors, warnings);
            case "som_output" -> validateSomOutput(req, warnings);
            case "imported_seed" -> validateImportedSeed(req, warnings);
            case "derived_seed" -> validateDerivedSeed(req, warnings);
            default -> {
                // unsupported source is already an error
            }
        }
    }

    private void validateUserDrawnCurve(ManifoldSeedRequestDTO req, List<String> warnings) {
        if (req.rawStrokePoints() == null || req.rawStrokePoints().isEmpty()) {
            warnings.add("user_drawn_curve usually includes rawStrokePoints.");
        }
        if (req.smoothedCurvePoints() == null || req.smoothedCurvePoints().isEmpty()) {
            warnings.add("user_drawn_curve usually includes smoothedCurvePoints.");
        }
    }

    private void validateOptimizationOutput(ManifoldSeedRequestDTO req, List<String> errors, List<String> warnings) {
        List<PointDTO> sampled = req.arcLengthSampledPoints();

        if (!isClosedLoop(sampled, LOOP_CLOSURE_EPSILON)) {
            errors.add("optimization_output requires arcLengthSampledPoints to form a closed loop.");
        }

        if (req.metadata() != null && req.metadata().totalArcLength() <= 0) {
            errors.add("optimization_output requires metadata.totalArcLength > 0.");
        }

        if (req.rectangleHotspots() == null || req.rectangleHotspots().isEmpty()) {
            warnings.add("optimization_output has no rectangleHotspots.");
        }

        if (req.rawStrokePoints() != null && !req.rawStrokePoints().isEmpty()) {
            warnings.add("optimization_output usually does not require rawStrokePoints.");
        }
    }

    private void validateSomOutput(ManifoldSeedRequestDTO req, List<String> warnings) {
        if (req.rectangleHotspots() == null || req.rectangleHotspots().isEmpty()) {
            warnings.add("som_output currently provides no hotspots.");
        }
    }

    private void validateImportedSeed(ManifoldSeedRequestDTO req, List<String> warnings) {
        if (req.parentSeedIds() == null || req.parentSeedIds().isEmpty()) {
            warnings.add("imported_seed usually includes parentSeedIds or external provenance.");
        }
    }

    private void validateDerivedSeed(ManifoldSeedRequestDTO req, List<String> warnings) {
        if (req.parentSeedIds() == null || req.parentSeedIds().isEmpty()) {
            warnings.add("derived_seed should include at least one parentSeedId.");
        }
    }

    private void validatePoints(String fieldName, List<PointDTO> points, boolean required, List<String> errors) {
        if (points == null) {
            if (required) {
                errors.add(fieldName + " must not be null.");
            }
            return;
        }

        for (int i = 0; i < points.size(); i++) {
            PointDTO p = points.get(i);
            if (p == null) {
                errors.add(fieldName + "[" + i + "] must not be null.");
                continue;
            }
            if (!isFinite(p.x()) || !isFinite(p.y())) {
                errors.add(fieldName + "[" + i + "] contains non-finite coordinates.");
            }
        }
    }

    private boolean hasDuplicateAdjacentPoints(List<PointDTO> points) {
        for (int i = 1; i < points.size(); i++) {
            PointDTO a = points.get(i - 1);
            PointDTO b = points.get(i);
            if (a != null && b != null && a.x() == b.x() && a.y() == b.y()) {
                return true;
            }
        }
        return false;
    }

    private boolean isClosedLoop(List<PointDTO> points, double epsilon) {
        if (points == null || points.size() < 3) {
            return false;
        }
        PointDTO first = points.get(0);
        PointDTO last = points.get(points.size() - 1);
        if (first == null || last == null) {
            return false;
        }
        return distance(first, last) <= epsilon;
    }

    private double distance(PointDTO a, PointDTO b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private boolean isKnownSeedSource(String derivedFrom) {
        if (derivedFrom == null) return true;
        return switch (derivedFrom) {
            case "user_drawn_curve",
                 "som_output",
                 "optimization_output",
                 "imported_seed",
                 "derived_seed" -> true;
            default -> false;
        };
    }

    private boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

record ValidationResult(boolean isValid, List<String> errors, List<String> warnings) {}