package workbench;


import java.util.UUID;

import machine.ManifoldSpace;

/**
 * Holds a constructed ManifoldSpace alongside its assigned ID and gates.
 * Internal to the backend — never serialized directly.
 */
public record CompiledManifold(
    String manifoldId,
    ManifoldSpace manifoldSpace,
    java.util.List<GateDescriptorDTO> gates,
    ParsedManifoldSeed sourceSeed
) {}