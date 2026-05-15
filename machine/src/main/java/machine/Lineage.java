package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Genealogical lineage of a flatlander, with multi-parent support.
 *
 * SIBLING SCOPE: getSiblingFlatlanderIds(parentId) is scoped per parent.
 * getAllSiblingFlatlanderIds() returns the union — use with caution when
 * multiple parents exist, as siblings across parents may be unrelated.
 */
public interface Lineage {
    String getFlatlanderId();
    List<String> getParentIds();
    boolean hasMultipleParents();
    int getGenerationDepth();
    List<String> getSiblingFlatlanderIds(String parentId);
    List<String> getAllSiblingFlatlanderIds();
    List<String> getChildIds();
    Map<String, Object> getLineageAttributes();

    // V1.1 ROADMAP: getLineageContribution(String parentId) -> double [0.0, 1.0]
    // Deferred until merge semantics depend on weighted inheritance.
}
