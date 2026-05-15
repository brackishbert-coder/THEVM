# Manifold VM Interface Contract

This document is intended for implementers. It preserves method signatures and summarizes what each interface is for, without including full implementation code.

## Global expectations

- Use the constants in the conventions package for event types, operation names, topology names, backend names, transition types, attribute keys, and constraint keys.
- Keep structural permissions separate from runtime policy.
- Treat `Flatlander` as an actor: it makes local decisions during `executeStep(...)`.
- Do not silently hide holonomy behavior in continuous backends.
- When using flexible maps, document supported keys and value types.

## Conventions

### `AttributeConventions`
- **Package:** `machine`
- **Source file:** `AttributeConventions.java`
- **Purpose:** Canonical attribute key constants for all Map<String, Object> attribute maps used in FlatlanderState, SystemSnapshot, ManifoldEvent, ExecutionContext, etc. RULE: No new key may be inserted into any attribute map without a constant

**Constants**

```java
public static final String VELOCITY_DAMPING      = "velocity_damping";
public static final String DORMANCY_DEPTH        = "dormancy_depth";
public static final String LAST_ACTIVE_STEP      = "last_active_step";
public static final String SPAWN_COUNT           = "spawn_count";
public static final String MERGE_COUNT           = "merge_count";
public static final String HOLONOMY_ACCUMULATED  = "holonomy_accumulated";
public static final String BACKEND_TYPE          = "backend_type";
public static final String PARTITION_ID          = "partition_id";
public static final String CACHE_HIT_RATE        = "cache_hit_rate";
public static final String CURVATURE_AT_EVENT    = "curvature_at_event";
public static final String HOLONOMY_ANGLE        = "holonomy_angle";
public static final String GEODESIC_ERROR        = "geodesic_error";
public static final String RUN_ID                = "run_id";
public static final String MANIFOLD_DESCRIPTOR   = "manifold_descriptor";
public static final String RANDOM_SEED           = "random_seed";
public static final String TOTAL_STEP_COUNT      = "total_step_count";
public static final String ACTIVE_FLATLANDERS    = "active_flatlanders";
public static final String IMMOBILIZED_COUNT     = "immobilized_count";
public static final String STALE_COUNT           = "stale_count";
```

### `CommonBackendTypes`
- **Package:** `machine`
- **Source file:** `CommonBackendTypes.java`
- **Purpose:** Canonical backend type constants for ManifoldDescriptor.getBackendType(). RULE: No new backend type may be returned from getBackendType() without a constant defined here first.

**Constants**

```java
public static final String DISCRETE   = "discrete";
public static final String CONTINUOUS = "continuous";
```

### `CommonEventTypes`
- **Package:** `machine`
- **Source file:** `CommonEventTypes.java`
- **Purpose:** Canonical event type constants for ManifoldEvent.getEventType(). For transition movement types see CommonTransitionTypes. These are system-level execution and interaction events only.

**Constants**

```java
public static final String EVENT_SPAWN             = "spawn";
public static final String EVENT_SPAWN_FAILED      = "spawn_failed";
public static final String EVENT_MERGE             = "merge";
public static final String EVENT_MERGE_FAILED      = "merge_failed";
public static final String EVENT_SPLIT             = "split";
public static final String EVENT_DORMANCY_ENTER    = "dormancy_enter";
public static final String EVENT_DORMANCY_EXIT     = "dormancy_exit";
public static final String EVENT_IMMOBILIZED       = "immobilized";
public static final String EVENT_STALE             = "stale";
public static final String EVENT_SIMILARITY_BOUND  = "similarity_bound";
public static final String EVENT_HIGH_CURVATURE    = "high_curvature";
public static final String EVENT_HOLONOMY_DETECTED = "holonomy_detected";
public static final String EVENT_TOPOLOGY_CHANGE   = "topology_change";
public static final String EVENT_BLOCKED_TRAVERSAL = "blocked_traversal";
```

### `CommonOperationNames`
- **Package:** `machine`
- **Source file:** `CommonOperationNames.java`
- **Purpose:** Canonical operation name constants for RegionPolicy.getOperationWeight() and RegionPolicy.getPolicyWeights(). RULE: No new operation name may be passed to getOperationWeight() without

**Constants**

```java
public static final String SPAWN            = "spawn";
public static final String MERGE            = "merge";
public static final String SPLIT            = "split";
public static final String VELOCITY_DAMPING = "velocity_damping";
public static final String SIMILARITY_BIND  = "similarity_bind";
public static final String DORMANCY_ENTER   = "dormancy_enter";
public static final String DORMANCY_EXIT    = "dormancy_exit";
```

### `CommonTopologyTypes`
- **Package:** `machine`
- **Source file:** `CommonTopologyTypes.java`
- **Purpose:** Canonical topology type constants for ManifoldDescriptor.getTopologyType(). RULE: No new topology type may be returned from getTopologyType() without a constant defined here first.

**Constants**

```java
public static final String FLAT          = "flat";
public static final String TOROIDAL      = "toroidal";
public static final String CYLINDRICAL   = "cylindrical";
public static final String FLATLAND_CUBE = "flatland_cube";
public static final String CUSTOM        = "custom";
```

### `CommonTransitionTypes`
- **Package:** `machine`
- **Source file:** `CommonTransitionTypes.java`
- **Purpose:** Canonical transition type constants for Transition.getTransitionType(). Kept separate from CommonEventTypes because transition types describe movement semantics between positions, not system-level events.

**Constants**

```java
public static final String LOCAL_STEP = "local_step";
public static final String LAYER_JUMP = "layer_jump";
public static final String WRAPAROUND = "wraparound";
public static final String SHORTCUT   = "shortcut";
public static final String PORTAL     = "portal";
```

### `ConstraintConventions`
- **Package:** `machine`
- **Source file:** `ConstraintConventions.java`
- **Purpose:** Canonical constraint key constants for TransitionQuery.getConstraints() and NeighborhoodQuery constraint maps. RULE: No new constraint key may be passed to any query map without a

**Constants**

```java
public static final String ALLOW_LAYER_JUMP          = "allow_layer_jump";
public static final String ALLOW_WRAPAROUND          = "allow_wraparound";
public static final String MAX_CURVATURE             = "max_curvature";
public static final String REQUIRED_TRANSITION_TYPE  = "required_transition_type";
public static final String EXCLUDE_IMMOBILIZED       = "exclude_immobilized";
public static final String EXCLUDE_STALE             = "exclude_stale";
public static final String MIN_SIMILARITY            = "min_similarity";
public static final String LINEAGE_FILTER            = "lineage_filter";
```


## Spatial and Geometry

### `Position`
- **Package:** `machine`
- **Source file:** `Position.java`
- **Purpose:** Backend-agnostic position in manifold space. IMPORTANT: getRepresentationDimensionality() returns the internal encoding dimensionality only — NOT the manifold's intrinsic dimensionality.

**Method signatures**

```java
double[] getCoordinates();
int getRepresentationDimensionality();
boolean isValid();
```

### `PositionDebug`
- **Package:** `machine`
- **Source file:** `PositionDebug.java`
- **Purpose:** Debug projection of a position to 2D for visualization.

**Method signatures**

```java
double getX();
double getY();
String getLabel();
```

### `MotionVector`
- **Package:** `machine`
- **Source file:** `MotionVector.java`
- **Purpose:** A direction and magnitude of movement, backend-agnostic.

**Method signatures**

```java
double[] getComponents();
double getMagnitude();
MotionVector scale(double factor);
MotionVector add(MotionVector other);
```

### `MotionAdaptationResult`
- **Package:** `machine`
- **Source file:** `MotionAdaptationResult.java`
- **Purpose:** Result of adapting a motion vector to local manifold constraints.

**Method signatures**

```java
MotionVector getAdaptedVector();
boolean wasModified();
String getModificationReason();
```

### `CurvatureField`
- **Package:** `machine`
- **Source file:** `CurvatureField.java`
- **Purpose:** Curvature information at a point on the manifold.

**Method signatures**

```java
double getScalarIntensity();
Optional<double[]> getCurvatureTensor();
boolean isFlat();
boolean isSingular();
```

### `Transition`
- **Package:** `machine`
- **Source file:** `Transition.java`
- **Purpose:** A possible local transition from one position to another. CONVENTION: transitionType values defined in CommonTransitionTypes. Transition types are a separate registry from ManifoldEvent types.

**Method signatures**

```java
Position getSource();
Position getTarget();
double getCost();
boolean isTraversable();
Map<String, Double> getAttributes();
```

### `TransitionQuery`
- **Package:** `machine`
- **Source file:** `TransitionQuery.java`
- **Purpose:** Query parameters for requesting transitions from a position. CONVENTION: constraint keys in ConstraintConventions.

**Method signatures**

```java
Map<String, Object> getConstraints();
int getMaxResults();
boolean includeNonTraversable();
```

### `Neighborhood`
- **Package:** `machine`
- **Source file:** `Neighborhood.java`
- **Purpose:** A snapshot of a flatlander's local neighborhood. FROZEN vs LIVE:   isFrozen() must be checked before use in time-sensitive contexts.

**Method signatures**

```java
List<Flatlander> getNearbyFlatlanders();
List<Position> getNearbyPositions();
List<Transition> getAvailableTransitions();
boolean isFrozen();
OptionalLong getCapturedAtStep();
```

### `NeighborhoodQuery`
- **Package:** `machine`
- **Source file:** `NeighborhoodQuery.java`
- **Purpose:** A query for the neighborhood around a position.

**Method signatures**

```java
List<Position> getPositionsInRadius();
List<Transition> getAvailableTransitions();
double getRadius();
```

### `TraversalCostResult`
- **Package:** `machine`
- **Source file:** `TraversalCostResult.java`
- **Purpose:** Result of evaluating traversal cost for a transition.

**Method signatures**

```java
double getCost();
boolean isFeasible();
String getRejectionReason();
```


## Manifold Core

### `ManifoldDescriptor`
- **Package:** `machine`
- **Source file:** `ManifoldDescriptor.java`
- **Purpose:** Metadata describing a manifold space instance. CONVENTION: backendType values in CommonBackendTypes. CONVENTION: topologyType values in CommonTopologyTypes.

**Method signatures**

```java
String getName();
String getBackendType();
String getTopologyType();
int getIntrinsicDimensionality();
Map<String, Object> getProperties();
```

### `ManifoldSpace`
- **Package:** `machine`
- **Source file:** `ManifoldSpace.java`
- **Purpose:** The primary manifold space contract. Implementations must document their holonomy policy as one of:   PRESERVE  — parallel transport preserves holonomy faithfully

**Method signatures**

```java
ManifoldDescriptor getDescriptor();
Position origin();
boolean isValidPosition(Position p);
int getLocalManifoldDimensionality(Position p);
CurvatureField getCurvatureAt(Position p);
RegionCapabilities getCapabilitiesAt(Position p);
RegionPolicy getPolicyAt(Position p);
Position move(Position from, MotionVector vector);
MotionAdaptationResult adaptMotion(Position at, MotionVector vector);
MotionVector parallelTransport(Position from, Position to, MotionVector vector);
Optional<MotionVector> computeHolonomy(List<Position> closedPath);
List<Transition> getTransitionsFrom(Position p, TransitionQuery query);
NeighborhoodQuery neighborhoodQuery(Position center, double radius);
Optional<List<Position>> findGeodesic(Position from, Position to);
double geodesicDistance(Position from, Position to);
ExecutionContext defaultContext();
Optional<PositionDebug> debugProject(Position p);
```

### `ManifoldStructureListener`
- **Package:** `machine`
- **Source file:** `ManifoldStructureListener.java`
- **Purpose:** Listener for structural changes to the manifold (topology changes, region mutations, etc.)

**Method signatures**

```java
void onStructureChanged(ManifoldEvent event);
```

### `RegionCapabilities`
- **Package:** `machine`
- **Source file:** `RegionCapabilities.java`
- **Purpose:** Structural affordances of a manifold region — what topology permits. IMPORTANT: Capabilities are structural only. They describe what the manifold geometry allows. They must never encode runtime policy or

**Method signatures**

```java
boolean canSpawn();
boolean canMerge();
boolean canSplit();
boolean allowsLayerJump();
boolean allowsWraparound();
boolean isTraversable();
```

### `RegionPolicy`
- **Package:** `machine`
- **Source file:** `RegionPolicy.java`
- **Purpose:** Runtime behavioral policy for a manifold region. STRICT CONTRACT for getSuggestedCorrection():   - Must be pure and stateless with respect to the inputs only

**Method signatures**

```java
double getOperationWeight(String operationName);
Map<String, Double> getPolicyWeights();
Optional<MotionVector> getSuggestedCorrection(MotionVector attemptedMotion);
boolean allowsOperation(String operationName);
```


## Flatlander System

### `Flatlander`
- **Package:** `machine`
- **Source file:** `Flatlander.java`
- **Purpose:** A first-class actor (process) flowing along manifold geometry. ACTOR CONTRACT: The flatlander drives its own decisions via executeStep(). It MUST respect RegionCapabilities and RegionPolicy at all times.

**Method signatures**

```java
String getId();
FlatlanderState getState();
Lineage getLineage();
SimilaritySignature getSimilaritySignature();
void executeStep(ExecutionContext context);
SpawnResult spawn(ExecutionContext context);
MergeResult mergeWith(Flatlander other, ExecutionContext context);
```

### `FlatlanderState`
- **Package:** `machine`
- **Source file:** `FlatlanderState.java`
- **Purpose:** Current runtime state of a flatlander. MOBILITY DISTINCTION:   isImmobilized() — topological trap. Runner response: reboot or re-seed.

**Method signatures**

```java
Position getPosition();
MotionVector getVelocity();
long getCurrentStep();
Map<String, Object> getAttributes();
boolean isImmobilized();
boolean isStale(long currentStep, long threshold);
boolean isDormant();
```

### `Lineage`
- **Package:** `machine`
- **Source file:** `Lineage.java`
- **Purpose:** Genealogical lineage of a flatlander, with multi-parent support. SIBLING SCOPE: getSiblingFlatlanderIds(parentId) is scoped per parent. getAllSiblingFlatlanderIds() returns the union — use with caution when

**Method signatures**

```java
String getFlatlanderId();
List<String> getParentIds();
boolean hasMultipleParents();
int getGenerationDepth();
List<String> getSiblingFlatlanderIds(String parentId);
List<String> getAllSiblingFlatlanderIds();
List<String> getChildIds();
Map<String, Object> getLineageAttributes();
```

### `SimilarityMatcher`
- **Package:** `machine`
- **Source file:** `SimilarityMatcher.java`
- **Purpose:** System-level service for computing similarity between flatlanders. Not owned by the flatlander itself.

**Method signatures**

```java
double computeSimilarity(SimilaritySignature a, SimilaritySignature b);
List<Flatlander> findSimilar(Flatlander target, List<Flatlander> candidates, double threshold);
```

### `SimilaritySignature`
- **Package:** `machine`
- **Source file:** `SimilaritySignature.java`
- **Purpose:** A self-describing similarity signature for a flatlander. CONVENTION NOTE: signatureType is a String for now. Likely candidate for a dedicated CommonSignatureTypes constants class

**Method signatures**

```java
String getSignatureType();
Map<String, Double> getFeatures();
Map<String, String> getFeatureDescriptions();
```

### `SpawnResult`
- **Package:** `machine`
- **Source file:** `SpawnResult.java`
- **Purpose:** Result of a spawn operation.

**Method signatures**

```java
boolean isSuccess();
Optional<Flatlander> getSpawned();
String getFailureReason();
```

### `MergeResult`
- **Package:** `machine`
- **Source file:** `MergeResult.java`
- **Purpose:** Result of a merge operation.

**Method signatures**

```java
boolean isSuccess();
Optional<Flatlander> getMerged();
String getFailureReason();
```


## Execution

### `ExecutionContext`
- **Package:** `machine`
- **Source file:** `ExecutionContext.java`
- **Purpose:** Context passed to flatlanders during execution. SYNTHETIC CONTEXT: isSynthetic() == true indicates this context was produced by ManifoldSpace.defaultContext() outside active execution.

**Method signatures**

```java
long getCurrentStep();
ManifoldSpace getManifoldSpace();
boolean isSynthetic();
Map<String, Object> getContextAttributes();
```

### `StoppingCondition`
- **Package:** `machine`
- **Source file:** `StoppingCondition.java`
- **Purpose:** A predicate determining when execution should halt.

**Method signatures**

```java
boolean shouldStop(SystemSnapshot snapshot);
String getDescription();
```

### `SystemSnapshot`
- **Package:** `machine`
- **Source file:** `SystemSnapshot.java`
- **Purpose:** An immutable snapshot of system state at a given step. CONVENTION: attribute keys in AttributeConventions.

**Method signatures**

```java
long getStep();
List<Flatlander> getActiveFlatlanders();
ManifoldSpace getManifoldSpace();
Map<String, Object> getSystemAttributes();
EmergenceMetricsSnapshot getEmergenceMetrics();
PerformanceMetricsSnapshot getPerformanceMetrics();
```

### `ManifoldRunner`
- **Package:** `machine`
- **Source file:** `ManifoldRunner.java`
- **Purpose:** The execution runner for a manifold system.

**Method signatures**

```java
void initialize(ManifoldSpace space, List<Flatlander> initialFlatlanders);
void step();
void runUntil(StoppingCondition condition);
SystemSnapshot snapshot();
void addObserver(MetricsObserver observer);
void addPatternObserver(PatternObserver observer);
boolean isRunning();
void stop();
```


## Events and Observers

### `ManifoldEvent`
- **Package:** `machine`
- **Source file:** `ManifoldEvent.java`
- **Purpose:** A system-level event emitted during execution. CONVENTION: eventType values in CommonEventTypes. CONVENTION: attribute keys in AttributeConventions.

**Method signatures**

```java
long getStep();
Optional<String> getFlatlanderId();
Optional<Position> getPosition();
Map<String, Object> getAttributes();
```

### `MetricsObserver`
- **Package:** `machine`
- **Source file:** `MetricsObserver.java`
- **Purpose:** Observer for execution metrics. NOTE: recordEvents() default delegates to recordEvent() one by one. Override for batch performance when high event volume is expected.

**Method signatures**

```java
void recordEvent(ManifoldEvent event);
events.forEach(this::recordEvent);
void onStepComplete(SystemSnapshot snapshot);
```

### `PatternObserver`
- **Package:** `machine`
- **Source file:** `PatternObserver.java`
- **Purpose:** Observer for emergent patterns across flatlander interactions. Watches for higher-order structures that metrics alone won't capture.

**Method signatures**

```java
void onPatternDetected(String patternType, List<Flatlander> participants, SystemSnapshot snapshot);
```


## Metrics Snapshots

### `EmergenceMetricsSnapshot`
- **Package:** `machine`
- **Source file:** `EmergenceMetricsSnapshot.java`
- **Purpose:** Snapshot of emergence-level metrics.

**Method signatures**

```java
double getClusteringCoefficient();
double getAverageSimilarityScore();
int getActiveClusterCount();
Map<String, Double> getCustomMetrics();
```

### `PerformanceMetricsSnapshot`
- **Package:** `machine`
- **Source file:** `PerformanceMetricsSnapshot.java`
- **Purpose:** Snapshot of system performance metrics.

**Method signatures**

```java
double getAverageStepDurationMs();
long getTotalStepsExecuted();
int getActiveFlatlanderCount();
int getImmobilizedCount();
int getStaleCount();
Map<String, Double> getCustomMetrics();
```

### `TopologyAwareMovementMetricsSnapshot`
- **Package:** `machine`
- **Source file:** `TopologyAwareMovementMetricsSnapshot.java`
- **Purpose:** Snapshot of topology-aware movement metrics.

**Method signatures**

```java
double getAverageGeodesicError();
int getWraparoundEventCount();
int getLayerJumpCount();
double getAverageCurvatureEncountered();
Map<String, Double> getCustomMetrics();
```

### `LineageMetricsSnapshot`
- **Package:** `machine`
- **Source file:** `LineageMetricsSnapshot.java`
- **Purpose:** Snapshot of lineage and genealogy metrics.

**Method signatures**

```java
int getTotalSpawnCount();
int getTotalMergeCount();
double getAverageGenerationDepth();
int getMaxGenerationDepth();
Map<String, Double> getCustomMetrics();
```

### `EventMetricsSnapshot`
- **Package:** `machine`
- **Source file:** `EventMetricsSnapshot.java`
- **Purpose:** Snapshot of event frequency metrics.

**Method signatures**

```java
Map<String, Long> getEventCountsByType();
long getTotalEventCount();
Map<String, Double> getCustomMetrics();
```

