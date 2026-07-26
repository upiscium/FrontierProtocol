# R7 Stabilizer Tiers

R7 implements the common three-tier architecture accepted by [ADR 0002](adr/0002-common-stabilizer-tier-architecture.md). The shared Cell and Create-owned production model remains governed by [ADR 0001](adr/0001-stabilizer-consumable-and-production-model.md).

## Common implementation

- `tier_1_stabilizer`, `tier_2_stabilizer`, and `tier_3_stabilizer` are distinct Registry entries backed by one shared `StabilizerBlock` class, one shared `StabilizerBlockEntity` class and Block Entity type, and one operating state machine.
- `StabilizerTier` supplies stable identity. `StabilizerTierDefinition` resolves current server-config values rather than retaining stale config snapshots.
- All tiers consume `frontier_protocol:stabilization_cell`; Compound is rejected. A Cell is consumed when a powered machine begins a configured ACTIVE duration.
- ACTIVE and GRACE_PERIOD project dimension-local, overlap-safe suppression. ACTIVE performs cleanup; GRACE_PERIOD retains suppression but pauses cleanup; OFFLINE does neither. Unloading, removing, or destroying the block unregisters its source.
- Every machine registers the generic `STABILIZER` source type. Its source ID has tier and position in the exact shape `stabilizer/<tier>/<x>_<y>_<z>`; dimension-local services keep equal positions in different dimensions independent.
- Each source contributes its tier's cleanup interval, inspection budget, and mutation budget. Server-global caps of 512 inspections and 16 mutations per tick remain hard aggregate limits across sources and dimensions.
- Coverage is a square centered on the placement chunk and never loads chunks. Cleanup touches only loaded chunks and only audited removable non-Block-Entity Spore foliage.
- Coverage changes while ACTIVE, GRACE_PERIOD, or unloaded preserve existing chunk cursors while starting a fresh pass only for newly covered chunks.

## Default definitions

| Setting | Tier 1 | Tier 2 | Tier 3 |
| --- | ---: | ---: | ---: |
| Chunk radius | 0 | 1 | 2 |
| Coverage | 1x1 (1 chunk) | 3x3 (9 chunks) | 5x5 (25 chunks) |
| Minimum absolute RPM | 32 | 64 | 128 |
| Stress impact | 16 | 64 | 256 |
| Cell capacity | 8 | 32 | 64 |
| Cell duration ticks | 6000 | 3000 | 2000 |
| Grace period ticks | 6000 | 9000 | 12000 |
| Cleanup interval ticks | 20 | 20 | 20 |
| Cleanup inspection budget per cycle/source | 128 | 2048 | 8192 |
| Cleanup mutation budget per cycle/source | 4 | 64 | 256 |

At default coverage and duration, one Cell provides 6000, 27000, and 50000 protected chunk-ticks for Tier 1, Tier 2, and Tier 3 respectively. Upper tiers are more Cell-efficient by protected area, but consume the common Cell at shorter machine intervals: every 3000 ticks for Tier 2 and every 2000 ticks for Tier 3 instead of every 6000 ticks for Tier 1. Their input logistics therefore need faster delivery and enough buffering despite the higher area efficiency.

## Production upgrades

R7 extends the three R6 recipes with two staged `create:mechanical_crafting` recipes, for exactly five Frontier Protocol recipes in total. There is no normal crafting or other acquisition bypass for Compound, Cells, or any Stabilizer tier. Tier 2 consumes a Tier 1 Stabilizer in its pattern; Tier 3 consumes a Tier 2 Stabilizer.

### Tier 2 Stabilizer

- ID: `frontier_protocol:mechanical_crafting/tier_2_stabilizer`
- Pattern and size: 3x3
- Output: one `frontier_protocol:tier_2_stabilizer`

```text
SCS
P1P
SBS
```

- `S`: `create:sturdy_sheet`
- `C`: `frontier_protocol:stabilization_cell`
- `P`: `create:precision_mechanism`
- `1`: `frontier_protocol:tier_1_stabilizer`
- `B`: `create:brass_casing`

### Tier 3 Stabilizer

- ID: `frontier_protocol:mechanical_crafting/tier_3_stabilizer`
- Pattern and size: 5x5
- Output: one `frontier_protocol:tier_3_stabilizer`

```text
SSCSS
SRPRS
CP2PC
SRPRS
SSCSS
```

- `S`: `create:sturdy_sheet`
- `C`: `frontier_protocol:stabilization_cell`
- `R`: `create:railway_casing`
- `P`: `create:precision_mechanism`
- `2`: `frontier_protocol:tier_2_stabilizer`

The complete five-recipe chain is Compound Mixing, Cell Deploying, and Tier 1, Tier 2, and Tier 3 Mechanical Crafting. The inherited R6 recipe details remain in [R6 Minimal Create Production Line](r6-production-line.md). Recipe and operating balance remains provisional and is reserved for R9.

## Assets

All tier state models are explicit placeholders using only vanilla texture references. Compound and Cell retain their R6 vanilla item-model parents. R7 adds no custom PNG and no final art; the exact references are listed in [Placeholder Assets](placeholder-assets.md).

## Non-goals

R7 does not add a GUI, Goggles information, Ponder scenes, particles, chunk-boundary visualization, a custom creative tab, tier-specific Cells, Empty or Spent Cells, container returns, new custom materials, TFMG integration, a multiblock tier, suppression from moving/virtual Create contraptions, chunk loading, terrain restoration, nest handling, mob handling, or final assets. Operational displays are deferred to R8. Balance changes are deferred to R9.

## Verification status

Automated verification already performed covers the unit tests, build, all 33 GameTests, RecipeManager assertions for exact serializers/ingredients/patterns/results and no bypasses, and datagen output. These checks cover shared registration and lifecycle, all tier definitions, coverage, suppression IDs, overlap behavior, per-source cleanup profiles/global caps, staged recipes, and generated resources.

Final smoke testing launched the production dedicated server through readiness and confirmed shutdown with all dimensions saved. The graphical client reached the title screen, joined the existing creative smoke world, rendered all three placeholder Stabilizers without Frontier Protocol model errors, and displayed Mixing, Deploying, and all three Mechanical Crafting recipes in JEI. Physical Mechanical Crafter execution for the Tier 2 and Tier 3 upgrades and continuous automated Cell-supply smoke testing remain unverified. The physical R6 Mixing GameTest does not satisfy those remaining R7 checks, so the complete line must not be described as fully automation-verified.
