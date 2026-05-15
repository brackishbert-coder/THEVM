
Manifold VM Interface Expectations
Version: v1.0 skeleton

Overview
This system defines a virtual-machine-style execution layer in which autonomous entities called Flatlanders move and interact inside a ManifoldSpace.

Core ideas:
- the manifold is the execution substrate
- Flatlanders are first-class actors
- geometry and topology constrain behavior
- policy shapes permitted operations
- metrics and observers measure emergent behavior

Architectural Layers
Spatial / Geometry: Position, MotionVector, CurvatureField, Transition, Neighborhood.
Manifold: ManifoldSpace, ManifoldDescriptor, RegionCapabilities, RegionPolicy.
Flatlander: Flatlander actors, FlatlanderState, Lineage, SimilaritySignature.
Execution: ManifoldRunner, ExecutionContext, StoppingCondition, SystemSnapshot.
Events / Metrics: ManifoldEvent, MetricsObserver, PatternObserver.
Conventions: canonical string constants for event types, attributes, constraints.

Design Rules
1. Structural vs Policy Separation
RegionCapabilities defines what topology allows.
RegionPolicy defines runtime behavior weighting or corrections.

2. Flatlanders are Actors
Flatlanders sense, decide, move, spawn, and merge autonomously.
The runner only advances time.

3. Holonomy is Observable
Parallel transport around loops may rotate vectors on curved manifolds.
Implementations must not silently remove this behavior.

4. Convention Registries
All attribute names, event types, and constraint keys must come from the conventions package.

Spatial Interfaces
Position – backend‑agnostic manifold location.
MotionVector – direction and magnitude of movement.
CurvatureField – curvature information at a location.
Transition – possible movement between two positions.
Neighborhood – local awareness around a flatlander.

ManifoldSpace Responsibilities
- Provide geometry queries and curvature information
- Provide region capabilities and policy
- Support movement and vector transport
- Support neighborhood queries and transitions
- Provide execution context helpers

Flatlander Responsibilities
Flatlanders must:
- maintain state and lineage
- compute similarity signatures
- execute their own decision logic each step
- optionally spawn or merge with other flatlanders

Execution Model
ManifoldRunner:
- initializes the manifold and agents
- advances steps
- runs until a stopping condition
- produces snapshots for observers

SystemSnapshot:
Immutable view of system state used by observers and metrics.

Events and Observers
ManifoldEvent records significant system actions.
MetricsObserver collects numeric metrics.
PatternObserver detects higher‑order structures.

Metrics Categories
Emergence metrics – clustering, similarity patterns.
Performance metrics – runtime and population counts.
Topology-aware metrics – curvature exposure, geodesic errors.
Lineage metrics – genealogical depth and reproduction.
Event metrics – frequency of system events.

Summary
The Manifold VM is a geometry‑driven simulation architecture where agents operate inside a manifold substrate. The design separates geometry, behavior policy, agent autonomy, and measurement layers to allow experimentation with both discrete and continuous spaces.
