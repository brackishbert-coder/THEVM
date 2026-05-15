package workbench;



import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;



@Service
public class ManifoldSeedService {

    private final ManifoldSeedValidator   validator;
    private final ManifoldSeedParser      parser;
    private final ManifoldBuilder         builder;
    private final CurvatureGenerator      curvatureGenerator;
    private final GateInterpreter         gateInterpreter;
    private final ManifoldResponseAssembler assembler;

    // In-memory store — replace with persistent store in v1.1
    private final ConcurrentHashMap<String, CompiledManifold> store =
        new ConcurrentHashMap<>();

    public ManifoldSeedService(
            ManifoldSeedValidator validator,
            ManifoldSeedParser parser,
            ManifoldBuilder builder,
            CurvatureGenerator curvatureGenerator,
            GateInterpreter gateInterpreter,
            ManifoldResponseAssembler assembler) {
        this.validator          = validator;
        this.parser             = parser;
        this.builder            = builder;
        this.curvatureGenerator = curvatureGenerator;
        this.gateInterpreter    = gateInterpreter;
        this.assembler          = assembler;
    }

    public ManifoldSeedResponseDTO ingestSeed(ManifoldSeedRequestDTO request) {
        // Step 1: validate
        ValidationResult validation = validator.validate(request);
        if (!validation.isValid()) {
            return assembler.rejected(request.manifoldSeedId(), validation);
        }

        // Step 2: parse and normalize
        ParsedManifoldSeed parsed = parser.parse(request);

        // Step 3: build ManifoldSpace
        CompiledManifold compiled = builder.build(parsed);

        // Step 4: compute curvature
        List<CurvatureSampleDTO> curvatureSamples =
            curvatureGenerator.sample(compiled.manifoldSpace(), 100);

        // Step 5: interpret gates
        List<GateDescriptorDTO> gates =
            gateInterpreter.interpret(parsed.hotspotCandidates(), compiled.manifoldSpace());

        // Step 6: store
        store.put(compiled.manifoldId(), compiled);

        // Step 7: assemble response
        return assembler.assemble(request, parsed, compiled, curvatureSamples, gates, validation);
    }

    public Optional<ManifoldDescriptorViewDTO> getDescriptor(String manifoldId) {
        return Optional.ofNullable(store.get(manifoldId))
            .map(assembler::descriptorView);
    }

    public Optional<List<CurvatureSampleDTO>> getCurvatureSamples(
            String manifoldId, int resolution) {
        return Optional.ofNullable(store.get(manifoldId))
            .map(c -> curvatureGenerator.sample(c.manifoldSpace(), resolution));
    }

    public Optional<List<GateDescriptorDTO>> getGates(String manifoldId) {
        return Optional.ofNullable(store.get(manifoldId))
            .map(CompiledManifold::gates);
    }

    public Optional<?> dryRun(String manifoldId, DryRunRequestDTO request) {
        // TODO: spawn probe actors on the stored manifold, run stepCount steps,
        // return trail snapshots. Deferred to v1.1 simulation layer.
        return Optional.ofNullable(store.get(manifoldId))
            .map(c -> {
                // TODO: ManifoldRunner.dryRun(c.manifoldSpace(), request.probeCount(), request.stepCount())
                return new Object(); // placeholder
            });
    }
}