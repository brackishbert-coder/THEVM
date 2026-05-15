package workbench;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manifold")

public class ManifoldSeedController {

    private final ManifoldSeedService service;

    public ManifoldSeedController(ManifoldSeedService service) {
        this.service = service;
    }

    /**
     * Submit a manifold seed. Validates, compiles, and returns the full response.
     */
    @PostMapping("/seed")
    public ResponseEntity<ManifoldSeedResponseDTO> submitSeed(
            @RequestBody ManifoldSeedRequestDTO request) {
        ManifoldSeedResponseDTO response = service.ingestSeed(request);
        if ("rejected".equals(response.status())) {
            return ResponseEntity.unprocessableEntity().body(response);
        }
        return ResponseEntity.ok(response);
    }
   
    @org.springframework.web.bind.annotation.GetMapping("/workbench")
    public org.springframework.core.io.Resource workbench() throws java.net.MalformedURLException {
        java.nio.file.Path p = java.nio.file.Paths.get("src/main/resources/workbench.html");
        return new org.springframework.core.io.UrlResource(p.toUri());
    }
    /**
     * Return the compiled descriptor for an existing manifold.
     */
    @GetMapping("/{manifoldId}")
    public ResponseEntity<ManifoldDescriptorViewDTO> getDescriptor(
            @PathVariable String manifoldId) {
        return service.getDescriptor(manifoldId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Return curvature samples across the manifold.
     */
    @GetMapping("/{manifoldId}/curvature")
    public ResponseEntity<List<CurvatureSampleDTO>> getCurvature(
            @PathVariable String manifoldId,
            @RequestParam(defaultValue = "100") int resolution) {
        return service.getCurvatureSamples(manifoldId, resolution)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Return all gate descriptors for the manifold.
     */
    @GetMapping("/{manifoldId}/gates")
    public ResponseEntity<List<GateDescriptorDTO>> getGates(
            @PathVariable String manifoldId) {
        return service.getGates(manifoldId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Run a lightweight dry-run probe simulation.
     */
    @PostMapping("/{manifoldId}/simulate/dry-run")
    public ResponseEntity<?> dryRun(
            @PathVariable String manifoldId,
            @RequestBody DryRunRequestDTO request) {
        return service.dryRun(manifoldId, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

// Minimal dry-run request record (can be expanded)
record DryRunRequestDTO(int stepCount, int probeCount) {}