package workbench;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import machine.ManifoldSpace;
import manifolds.continuous.implementation.CurveManifold;

import org.springframework.stereotype.Component;

@Component
public class ManifoldBuilder {

    public CompiledManifold build(ParsedManifoldSeed seed) {
        String manifoldId = "manifold_" + UUID.randomUUID()
            .toString().replace("-", "").substring(0, 10);

        ManifoldSpace space = switch (seed.derivedFrom()) {
            case "user_drawn_curve"    -> buildCurveManifold(seed, manifoldId);
            case "optimization_output" -> buildOptimizationManifold(seed, manifoldId);
            case "som_output"          -> buildSomManifold(seed, manifoldId);
            case "imported_seed"       -> buildImportedSeedManifold(seed, manifoldId);
            case "derived_seed"        -> buildDerivedSeedManifold(seed, manifoldId);
            default -> throw new IllegalArgumentException(
                "Unsupported derivedFrom: " + seed.derivedFrom());
        };

        return new CompiledManifold(manifoldId, space, List.of(), seed);
    }

    private ManifoldSpace buildCurveManifold(ParsedManifoldSeed seed, String manifoldId) {
        return buildCurveFromNormalizedPoints(seed.normalizedSampledPoints(), manifoldId, "user_drawn_curve");
    }

    private ManifoldSpace buildOptimizationManifold(ParsedManifoldSeed seed, String manifoldId) {
        return buildCurveFromNormalizedPoints(seed.normalizedSampledPoints(), manifoldId, "optimization_output");
    }

    private ManifoldSpace buildSomManifold(ParsedManifoldSeed seed, String manifoldId) {
        throw new UnsupportedOperationException("som_output not implemented yet.");
    }

    private ManifoldSpace buildImportedSeedManifold(ParsedManifoldSeed seed, String manifoldId) {
        return buildCurveFromNormalizedPoints(seed.normalizedSampledPoints(), manifoldId, "imported_seed");
    }

    private ManifoldSpace buildDerivedSeedManifold(ParsedManifoldSeed seed, String manifoldId) {
        return buildCurveFromNormalizedPoints(seed.normalizedSampledPoints(), manifoldId, "derived_seed");
    }

    private ManifoldSpace buildCurveFromNormalizedPoints(
            List<NormalizedPoint> normalized,
            String manifoldId,
            String sourceType) {

        if (normalized == null || normalized.size() < 4) {
            throw new IllegalArgumentException(
                sourceType + " requires at least 4 normalized sampled points.");
        }

        List<double[]> pts = new ArrayList<>(normalized.size());
        for (NormalizedPoint p : normalized) {
            pts.add(new double[]{ p.x(), p.y() });
        }

        return new CurveManifold(manifoldId, pts);
    }
}