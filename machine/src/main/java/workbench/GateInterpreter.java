package workbench;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import machine.ManifoldSpace;

import org.springframework.stereotype.Component;
@Component
public class GateInterpreter {

    private static final double MIN_INTENSITY_THRESHOLD = 0.4;
    private static final double CURVE_PROXIMITY_THRESHOLD = 0.05; // normalized units

    /**
     * Evaluate hotspot candidates against the constructed manifold.
     * Reject candidates that don't survive geometric validation.
     * Assign gate types based on intensity and local manifold properties.
     */
    public List<GateDescriptorDTO> interpret(
            List<NormalizedHotspot> candidates,
            ManifoldSpace space) {

        List<GateDescriptorDTO> gates = new ArrayList<>();

        for (NormalizedHotspot h : candidates) {
            if (h.intensity() < MIN_INTENSITY_THRESHOLD) continue;

            // TODO: validate candidate position against manifold
            //   1. Construct a Position at (h.x(), h.y())
            //   2. Check space.isValidPosition(p)
            //   3. Check geodesic distance to nearest curve point < CURVE_PROXIMITY_THRESHOLD
            //   4. Query local CurvatureField to inform gate type assignment

            String gateType   = assignGateType(h, space);
            double confidence = computeConfidence(h, space);

            gates.add(new GateDescriptorDTO(
                "gate_" + UUID.randomUUID().toString().replace("-","").substring(0,8),
                h.x(),
                h.y(),
                h.intensity(),
                gateType,
                confidence,
                h.candidateType()
            ));
        }

        return gates;
    }

    private String assignGateType(NormalizedHotspot h, ManifoldSpace space) {
        // TODO: policy-driven assignment
        // v1 heuristic:
        //   intensity > 0.8 → "portal"
        //   intensity > 0.6 → "attractor"
        //   intensity > 0.4 → "merge_zone"
        //   else            → "split_zone"
        if (h.intensity() > 0.8) return "portal";
        if (h.intensity() > 0.6) return "attractor";
        if (h.intensity() > 0.4) return "merge_zone";
        return "split_zone";
    }

    private double computeConfidence(NormalizedHotspot h, ManifoldSpace space) {
        // TODO: confidence based on local curvature consistency and proximity to curve
        return h.intensity() * 0.9; // placeholder
    }
}