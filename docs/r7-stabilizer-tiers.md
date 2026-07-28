# R7 Stabilizer Tiers

R7 implements the common three-tier architecture accepted by [ADR 0002](adr/0002-common-stabilizer-tier-architecture.md). The shared Cell and Create-owned production model remains governed by [ADR 0001](adr/0001-stabilizer-consumable-and-production-model.md).

## Common implementation

- `tier_1_stabilizer`, `tier_2_stabilizer`, and `tier_3_stabilizer` are distinct Registry entries backed by one shared `StabilizerBlock` class, one shared `StabilizerBlockEntity` class and Block Entity type, and one operating state machine.
- `StabilizerTier` supplies stable identity. `StabilizerTierDefinition` resolves current server-config values rather than retaining stale config snapshots.
- All tiers consume `frontier_protocol:stabilization_cell`; Compound is rejected. A Cell is consumed when a powered machine begins a configured ACTIVE duration and grants one finite Grace budget. ACTIVE or recovery with the same Cell never refills that budget.
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
| Grace budget ticks per Cell | 1200 | 1800 | 2400 |
| Cleanup interval ticks | 20 | 20 | 20 |
| Cleanup inspection budget per cycle/source | 128 | 2048 | 8192 |
| Cleanup mutation budget per cycle/source | 4 | 64 | 256 |

At default coverage and duration, one Cell provides 6000, 27000, and 50000 protected chunk-ticks for Tier 1, Tier 2, and Tier 3 respectively. Full buffers run for 48000, 96000, and 128000 ticks, or 40 minutes, 80 minutes, and 106 minutes 40 seconds. Upper tiers are more Cell-efficient by protected area, but consume the common Cell every 3000 and 2000 ticks. R9 physically verifies that Chest/Chute supply follows both intervals without interrupting ACTIVE or suppression.

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

The complete five-recipe chain is Compound Mixing, Cell Deploying, and Tier 1, Tier 2, and Tier 3 Mechanical Crafting. The inherited R6 recipe details remain in [R6 Minimal Create Production Line](r6-production-line.md). R9 preserves all five recipes and fixes the accepted operating balance in [R9 Balance Hardening](r9-balance-hardening.md).

## Assets

All tier state models are explicit placeholders using only vanilla texture references. Compound and Cell retain their R6 vanilla item-model parents. R7 adds no custom PNG and no final art; the exact references are listed in [Placeholder Assets](placeholder-assets.md).

## Non-goals

R7 does not add a GUI, Goggles information, Ponder scenes, particles, chunk-boundary visualization, a custom creative tab, tier-specific Cells, Empty or Spent Cells, container returns, new custom materials, TFMG integration, a multiblock tier, suppression from moving/virtual Create contraptions, chunk loading, terrain restoration, nest handling, mob handling, or final assets. R8 adds informational operational displays. R9 changes only Grace semantics/defaults and verification coverage described in ADR 0005.

## Verification status

Automated R9 verification covers the unit tests and all 39 GameTests, RecipeManager assertions for exact serializers/ingredients/patterns/results and no bypasses, physical execution of all three Mechanical Crafting recipes, and two default-duration Cell rollovers for both upper tiers.

Earlier smoke testing launched the production dedicated server through readiness and exercised the graphical client. R9 now physically executes Tier 1/2/3 Mechanical Crafting and continuous Tier 2/Tier 3 Cell delivery in dedicated GameTests. The Deployer recipe remains RecipeManager/JEI verified rather than physically executed.
