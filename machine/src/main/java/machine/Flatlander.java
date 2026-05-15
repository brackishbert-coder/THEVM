package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A first-class actor (process) flowing along manifold geometry.
 *
 * ACTOR CONTRACT: The flatlander drives its own decisions via executeStep().
 * It MUST respect RegionCapabilities and RegionPolicy at all times.
 * Runner retains authority via StoppingCondition and mobility hooks only.
 */
public interface Flatlander {
    String getId();
    FlatlanderState getState();
    Lineage getLineage();
    SimilaritySignature getSimilaritySignature();

    /**
     * Execute one step. Actor model — flatlander drives its own movement
     * and computation. Must respect capabilities and policy of current region.
     */
    void executeStep(ExecutionContext context);

    SpawnResult spawn(ExecutionContext context);
    MergeResult mergeWith(Flatlander other, ExecutionContext context);
	boolean canMergeWith(Flatlander b);
}
