# R7 Stabilizer Tiers

R7 adds the foundations for a common three-tier Stabilizer implementation. The accepted design is recorded in [ADR 0002](adr/0002-common-stabilizer-tier-architecture.md); the common Cell model remains governed by [ADR 0001](adr/0001-stabilizer-consumable-and-production-model.md).

## R7-A foundations

R7-A provides only tier identity, validated definition data, dynamic server-config resolution, cleanup profile data, and pure chunk coverage geometry.

`StabilizerTier` defines `TIER_1`, `TIER_2`, and `TIER_3`, serialized as `tier_1`, `tier_2`, and `tier_3`. It derives a registered Stabilizer's tier from the stable block Registry path. Registry-path lookup allows Tier 2 and Tier 3 to resolve after their blocks are registered without adding placeholder registrations in R7-A.

`StabilizerTierDefinition` contains:

- tier identity
- chunk radius
- minimum RPM
- Stress impact
- Cell capacity
- Cell duration in ticks
- grace period in ticks
- per-source cleanup profile

`StabilizerTierDefinitions.resolve` reads the current server config on every call. Callers must not retain a definition as a permanent config snapshot across config changes.

`StabilizerCoverage` computes an immutable square of `ChunkPos` values without querying a level or loading chunks. Results are unique and iterate in row-major order from minimum Z/minimum X to maximum Z/maximum X.

## Default definitions

| Setting | Tier 1 | Tier 2 | Tier 3 |
| --- | ---: | ---: | ---: |
| Chunk radius | 0 | 1 | 2 |
| Minimum RPM | 32 | 64 | 128 |
| Stress impact | 16 | 64 | 256 |
| Cell capacity | 8 | 32 | 64 |
| Cell duration ticks | 6000 | 12000 | 24000 |
| Grace period ticks | 6000 | 12000 | 24000 |
| Cleanup interval ticks | 20 | 10 | 5 |
| Cleanup inspection budget per cycle | 128 | 512 | 2048 |
| Cleanup mutation budget per cycle | 4 | 16 | 64 |

Global cleanup inspection and mutation budgets are unchanged. They continue to cap aggregate server work independently of per-source profiles.

## Planned R7 phases

The following behavior is planned and is not implemented by R7-A:

- Replace the current Tier 1-specific Block, Block Entity, and state machine with the shared Stabilizer implementation.
- Register `tier_2_stabilizer` and `tier_3_stabilizer`, then move all three Registry entries to the shared Block and one shared Block Entity type.
- Replace the Tier 1 suppression source type with generic `STABILIZER` sources whose IDs contain tier and position.
- Connect coverage definitions to suppression registration and overlap-safe lifecycle updates.
- Connect per-source cleanup profiles to cleanup scheduling while retaining global budgets.
- Enforce disabled virtual and Contraption operation.
- Add staged Tier 1 to Tier 2 and Tier 2 to Tier 3 Create upgrade recipes.
- Add tier-specific block items, assets, translations, data generation, GameTests, and presentation.

Until those phases land, runtime gameplay remains the existing Tier 1 implementation. Tier 2 and Tier 3 are definition/configuration foundations only and are not registered, craftable, placeable, or active.
