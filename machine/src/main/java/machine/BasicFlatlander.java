package machine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A minimal concrete Flatlander implementation.
 *
 * Behavior:
 * - carries position + velocity
 * - moves once per step using the manifold
 * - exposes a similarity signature based on position and velocity
 * - spawn/merge are conservative no-ops by default
 *
 * This is intended as a bootstrap actor, not a final gameplay/research actor.
 */
public class BasicFlatlander implements Flatlander {

    private final String id;
    private final BasicFlatlanderState state;
    private final BasicLineage lineage;
    private final SimilaritySignature signature;

    public BasicFlatlander(String id, Position position, MotionVector velocity) {
        this(id, position, velocity, List.of(), 0);
    }

    public BasicFlatlander(
            String id,
            Position position,
            MotionVector velocity,
            List<String> parentIds,
            int generationDepth
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.state = new BasicFlatlanderState(position, velocity);
        this.lineage = new BasicLineage(id, parentIds, generationDepth);
        this.signature = new BasicSimilaritySignature(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public FlatlanderState getState() {
        return state;
    }

    @Override
    public Lineage getLineage() {
        return lineage;
    }

    @Override
    public SimilaritySignature getSimilaritySignature() {
        return signature;
    }

    @Override
    public void executeStep(ExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");

        ManifoldSpace manifold = context.getManifoldSpace();
        if (manifold == null) {
            return;
        }

        Position current = state.getPosition();
        MotionVector velocity = state.getVelocity();

        if (current == null || velocity == null) {
            return;
        }

        if (!manifold.isValidPosition(current)) {
            return;
        }

        RegionCapabilities capabilities = manifold.getCapabilitiesAt(current);
        if (capabilities == null || !capabilities.isTraversable()) {
            state.setImmobilized(true);
            return;
        }

        MotionVector workingVelocity = velocity;

        RegionPolicy policy = manifold.getPolicyAt(current);
        if (policy != null) {
            Optional<MotionVector> corrected = policy.getSuggestedCorrection(workingVelocity);
            if (corrected.isPresent()) {
                workingVelocity = corrected.get();
            }
        }

        MotionAdaptationResult adapted = manifold.adaptMotion(current, workingVelocity);
        if (adapted != null && adapted.getAdaptedVector() != null) {
            workingVelocity = adapted.getAdaptedVector();
        }

        Position next = manifold.move(current, workingVelocity);
        if (next != null && manifold.isValidPosition(next)) {
            state.setPosition(next);
            state.setVelocity(workingVelocity);
            state.touch(context.getCurrentStep());
            state.setImmobilized(false);
            state.setDormant(false);
        } else {
            state.setImmobilized(true);
        }
    }

    @Override
    public SpawnResult spawn(ExecutionContext context) {
        return new SpawnFailure("spawn not implemented in BasicFlatlander");
    }

    @Override
    public MergeResult mergeWith(Flatlander other, ExecutionContext context) {
        if (other == null || other == this) {
            return new MergeFailure("merge target invalid");
        }

        Position myPos = state.getPosition();
        Position otherPos = other.getState() != null ? other.getState().getPosition() : null;

        if (!samePosition(myPos, otherPos)) {
            return new MergeFailure("flatlanders are not co-located");
        }

        // Conservative v1: report success by producing a merged child-like flatlander.
        MotionVector myVel = state.getVelocity();
        MotionVector otherVel = other.getState() != null ? other.getState().getVelocity() : null;

        MotionVector mergedVelocity = myVel;
        if (myVel != null && otherVel != null) {
            mergedVelocity = myVel.add(otherVel).scale(0.5);
        }

        BasicFlatlander merged = new BasicFlatlander(
                this.id + "_merged_" + other.getId(),
                myPos,
                mergedVelocity,
                List.of(this.id, other.getId()),
                Math.max(this.lineage.getGenerationDepth(),
                         safeGenerationDepth(other.getLineage())) + 1
        );

        return new MergeSuccess(merged);
    }

    public boolean canMergeWith(Flatlander other) {
        if (other == null || other == this || other.getState() == null) {
            return false;
        }
        return samePosition(this.state.getPosition(), other.getState().getPosition());
    }

    private boolean samePosition(Position a, Position b) {
        if (a == null || b == null) {
            return false;
        }
        double[] ac = a.getCoordinates();
        double[] bc = b.getCoordinates();
        if (ac == null || bc == null || ac.length != bc.length) {
            return false;
        }
        for (int i = 0; i < ac.length; i++) {
            if (Double.compare(ac[i], bc[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    private int safeGenerationDepth(Lineage lineage) {
        return lineage == null ? 0 : lineage.getGenerationDepth();
    }

    // ----------------------------------------------------------------
    // Inner state
    // ----------------------------------------------------------------

    private static final class BasicFlatlanderState implements FlatlanderState {
        private Position position;
        private MotionVector velocity;
        private long lastActiveStep = 0L;
        private boolean immobilized = false;
        private boolean dormant = false;
        private final Map<String, Object> attributes = new HashMap<>();

        private BasicFlatlanderState(Position position, MotionVector velocity) {
            this.position = position;
            this.velocity = velocity;
        }

        @Override
        public Position getPosition() {
            return position;
        }

        @Override
        public MotionVector getVelocity() {
            return velocity;
        }

        @Override
        public long getCurrentStep() {
            return lastActiveStep;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public boolean isImmobilized() {
            return immobilized;
        }

        @Override
        public boolean isStale(long currentStep, long threshold) {
            return (currentStep - lastActiveStep) > threshold;
        }

        @Override
        public boolean isDormant() {
            return dormant;
        }

        public void setPosition(Position position) {
            this.position = position;
        }

        public void setVelocity(MotionVector velocity) {
            this.velocity = velocity;
        }

        public void touch(long step) {
            this.lastActiveStep = step;
        }

        public void setImmobilized(boolean immobilized) {
            this.immobilized = immobilized;
        }

        public void setDormant(boolean dormant) {
            this.dormant = dormant;
        }
    }

    private static final class BasicLineage implements Lineage {
        private final String flatlanderId;
        private final List<String> parentIds;
        private final int generationDepth;
        private final List<String> childIds = List.of();
        private final Map<String, Object> lineageAttributes = new HashMap<>();

        private BasicLineage(String flatlanderId, List<String> parentIds, int generationDepth) {
            this.flatlanderId = flatlanderId;
            this.parentIds = List.copyOf(parentIds);
            this.generationDepth = generationDepth;
        }

        @Override
        public String getFlatlanderId() {
            return flatlanderId;
        }

        @Override
        public List<String> getParentIds() {
            return parentIds;
        }

        @Override
        public boolean hasMultipleParents() {
            return parentIds.size() > 1;
        }

        @Override
        public int getGenerationDepth() {
            return generationDepth;
        }

        @Override
        public List<String> getSiblingFlatlanderIds(String parentId) {
            return List.of();
        }

        @Override
        public List<String> getAllSiblingFlatlanderIds() {
            return List.of();
        }

        @Override
        public List<String> getChildIds() {
            return childIds;
        }

        @Override
        public Map<String, Object> getLineageAttributes() {
            return lineageAttributes;
        }
    }

    private static final class BasicSimilaritySignature implements SimilaritySignature {
        private final BasicFlatlander owner;

        private BasicSimilaritySignature(BasicFlatlander owner) {
            this.owner = owner;
        }

        @Override
        public String getSignatureType() {
            return "basic_position_velocity";
        }

        @Override
        public Map<String, Double> getFeatures() {
            Map<String, Double> out = new HashMap<>();

            Position p = owner.state.getPosition();
            if (p != null && p.getCoordinates() != null) {
                double[] c = p.getCoordinates();
                for (int i = 0; i < c.length; i++) {
                    out.put("pos_" + i, c[i]);
                }
            }

            MotionVector v = owner.state.getVelocity();
            if (v != null && v.getComponents() != null) {
                double[] vc = v.getComponents();
                for (int i = 0; i < vc.length; i++) {
                    out.put("vel_" + i, vc[i]);
                }
                out.put("vel_mag", v.getMagnitude());
            }

            out.put("generation_depth", (double) owner.lineage.getGenerationDepth());
            return out;
        }

        @Override
        public Map<String, String> getFeatureDescriptions() {
            Map<String, String> out = new HashMap<>();
            out.put("pos_0", "Position coordinate 0");
            out.put("pos_1", "Position coordinate 1");
            out.put("vel_0", "Velocity component 0");
            out.put("vel_1", "Velocity component 1");
            out.put("vel_mag", "Velocity magnitude");
            out.put("generation_depth", "Genealogical generation depth");
            return out;
        }
    }

    private record SpawnFailure(String failureReason) implements SpawnResult {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<Flatlander> getSpawned() {
            return Optional.empty();
        }

        @Override
        public String getFailureReason() {
            return failureReason;
        }
    }

    private record MergeFailure(String failureReason) implements MergeResult {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<Flatlander> getMerged() {
            return Optional.empty();
        }

        @Override
        public String getFailureReason() {
            return failureReason;
        }
    }

    private record MergeSuccess(Flatlander merged) implements MergeResult {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<Flatlander> getMerged() {
            return Optional.ofNullable(merged);
        }

        @Override
        public String getFailureReason() {
            return "";
        }
    }
}