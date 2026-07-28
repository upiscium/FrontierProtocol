# ADR 0002: Common Stabilizer Tier Architecture

## Status

Accepted

## Context

R7 introduces three Stabilizer tiers with different coverage, kinetic cost, Cell economy, resilience, and cleanup performance. The tier model must preserve overlap-safe suppression and cleanup behavior without creating three independent machine implementations.

## Decision

- Tier 1, Tier 2, and Tier 3 use one shared Stabilizer Block class, one shared Stabilizer Block Entity class, and one shared operating state machine.
- The three tiers have three distinct block Registry entries: `tier_1_stabilizer`, `tier_2_stabilizer`, and `tier_3_stabilizer`. All three blocks use the one shared Block Entity type.
- [ADR 0001](0001-stabilizer-consumable-and-production-model.md) remains authoritative: every tier consumes the same Stabilization Cell.
- `StabilizerTier` contains stable tier identity and Registry naming. `StabilizerTierDefinition` contains tier operating values, and definitions are resolved from current server config values rather than cached config snapshots.
- Tier differences are definition and configuration data: chunk radius, minimum RPM, Stress impact, Cell capacity, Cell duration, grace duration, and the cleanup source profile.
- Tier 3 remains a single block.
- Virtual Stabilizer operation and operation while assembled into a Create Contraption are disabled for every tier. A moved or virtual machine does not project a moving suppression area.
- Stabilizers use one generic `STABILIZER` suppression source type. Source IDs include the tier and BlockPos, using the shape `stabilizer/<tier>/<x>_<y>_<z>`; dimension-local services keep equal positions in different dimensions independent.
- Cleanup scheduling uses a per-source `CleanupSourceProfile` containing interval, inspection budget, and mutation budget. Global cleanup budgets remain hard aggregate limits.
- Upgrade recipes are staged: Tier 2 upgrades a Tier 1 Stabilizer, and Tier 3 upgrades a Tier 2 Stabilizer. Their concrete Create processing steps and ingredients are defined in the recipe phase.

## Rejected Alternatives

- Copying the Tier 1 Block, Block Entity, and state machine for each tier was rejected because lifecycle fixes and persistence behavior would diverge.
- Tier-specific Stabilization Cells were rejected because machine performance and operating cost define tiers; ADR 0001 deliberately provides one common Cell.
- A Tier 3 multiblock was rejected because it expands validation, assembly, persistence, and unload behavior without being required for 5x5 chunk coverage.

## Consequences

Tier behavior is selected by stable block identity and current server configuration while lifecycle and persistence logic remain shared. Distinct Registry IDs still allow separate recipes, assets, names, and balance for each tier.

Any future change that replaces the shared Block, Block Entity, state-machine, common-Cell, single-block Tier 3, or stationary-source model must mark this ADR `Superseded` and record the replacement in a new ADR.
