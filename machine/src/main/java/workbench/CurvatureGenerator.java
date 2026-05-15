package workbench;

import machine.CurvatureField;
import machine.ManifoldSpace;
import machine.Position;
import manifolds.continuous.implemention.CurveManifold;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
@Component
public class CurvatureGenerator {

    /**
     * Sample curvature at `resolution` evenly-spaced positions along the manifold.
     *
     * For CurveManifold: strides directly through the point array using the
     * public accessor API — no manifold machinery needed.
     *
     * For other manifolds: falls back to arc-length stepping via ManifoldSpace.
     */
    public List<CurvatureSampleDTO> sample(ManifoldSpace space, int resolution) {
        if (space instanceof CurveManifold curve) {
            return sampleCurve(curve, resolution);
        }
        return sampleGeneric(space, resolution);
    }

    // ----------------------------------------------------------------
    // CurveManifold fast path
    // ----------------------------------------------------------------

    private List<CurvatureSampleDTO> sampleCurve(CurveManifold curve, int resolution) {
        List<CurvatureSampleDTO> samples = new ArrayList<>(resolution);

        int    n           = curve.getPointCount();
        double totalArc    = curve.getTotalArcLength();

        for (int i = 0; i < resolution; i++) {
            // Target arc-length position
            double t   = (double) i / Math.max(1, resolution - 1);
            double arc = t * totalArc;

            // Find the two bracketing indices by arc-length
            int lo = 0;
            for (int j = 0; j < n - 1; j++) {
                if (curve.getArcLengthAt(j + 1) >= arc) { lo = j; break; }
                lo = j;
            }
            int hi = Math.min(lo + 1, n - 1);

            double arcLo = curve.getArcLengthAt(lo);
            double arcHi = curve.getArcLengthAt(hi);
            double span  = arcHi - arcLo;
            double frac  = (span > 1e-12) ? (arc - arcLo) / span : 0.0;

            double[] ptLo = curve.getPoint(lo);
            double[] ptHi = curve.getPoint(hi);
            double x = ptLo[0] + frac * (ptHi[0] - ptLo[0]);
            double y = ptLo[1] + frac * (ptHi[1] - ptLo[1]);

            // Curvature: interpolate scalar value
            double kLo = curve.getCurvatureValueAt(lo);
            double kHi = curve.getCurvatureValueAt(hi);
            double k   = kLo + frac * (kHi - kLo);

            boolean singular = curve.isSingularAt(lo) || curve.isSingularAt(hi);
            boolean flat     = !singular && Math.abs(k) < 0.5;

            samples.add(new CurvatureSampleDTO(x, y, k, singular, flat));
        }

        return samples;
    }

    // ----------------------------------------------------------------
    // Generic fallback — arc-length stepping via ManifoldSpace
    // ----------------------------------------------------------------

    private List<CurvatureSampleDTO> sampleGeneric(ManifoldSpace space, int resolution) {
        List<CurvatureSampleDTO> samples = new ArrayList<>(resolution);

        Position current = space.origin();

        for (int i = 0; i < resolution; i++) {
            CurvatureField field = space.getCurvatureAt(current);

            // Project to 2D for x, y
            double x = 0.0, y = 0.0;
            var debug = space.debugProject(current);
            if (debug.isPresent()) {
                x = debug.get().getX();
                y = debug.get().getY();
            }

            samples.add(new CurvatureSampleDTO(
                x,
                y,
                field.getScalarIntensity(),
                field.isSingular(),
                field.isFlat()
            ));

            // Step forward by 1/resolution of the manifold
            // Uses a unit step along the first coordinate axis
            double[] components = new double[]{ 1.0 / resolution };
            current = space.move(current, new GenericStepVector(components));
        }

        return samples;
    }

    // ----------------------------------------------------------------
    // Minimal MotionVector for generic stepping
    // ----------------------------------------------------------------

    private static class GenericStepVector implements machine.MotionVector {
        private final double[] components;

        GenericStepVector(double[] components) {
            this.components = components;
        }

        @Override public double[]               getComponents() { return components; }
        @Override public double                 getMagnitude()  { return Math.abs(components[0]); }
        @Override public machine.MotionVector   scale(double f) { return new GenericStepVector(new double[]{ components[0] * f }); }
        @Override public machine.MotionVector   add(machine.MotionVector other) {
            double[] c = other.getComponents();
            return new GenericStepVector(new double[]{ components[0] + (c.length > 0 ? c[0] : 0.0) });
        }
    }
}