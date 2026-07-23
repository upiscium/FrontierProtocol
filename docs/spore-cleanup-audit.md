# Spore Cleanup Audit

R5-A audits Fungal Infection: Spore `2.2.0j` (CurseForge file ID `8342823`) and defines a conservative, opt-in cleanup policy. This audit does not infer an original block from an infected block and does not authorize terrain restoration.

## Audited artifact

- Artifact: `fungal-infection-spore-678295-8342823.jar`
- Gradle cache path: `~/.gradle/caches/modules-2/files-2.1/curse.maven/fungal-infection-spore-678295/8342823/43b07f98a760328a6d092f8a8c502e6ea81989e5/fungal-infection-spore-678295-8342823.jar`
- SHA-256: `0cdb027eb53e6872bdb8de601dc52c176fcb6ddd33a0a2e20a66392937c3d5fe`
- Registry authority: `com.Harbinger.Spore.core.Sblocks`
- Block Entity authority: `com.Harbinger.Spore.core.SblockEntities`
- Evidence: registry bytecode, block implementation bytecode, embedded block tags, configured features, structure tags, structure templates, blockstates, and loot tables

Class names in the tables are relative to `com.Harbinger.Spore.Sblocks` unless fully qualified. Natural generation means an embedded feature or structure can place the block. Spread means `FoliageSpread` or a related infection lifecycle can place it. `Waterlogged` records whether the implementation can retain source water; replacement is always selected from the actual state's `FluidState`.

## Approved removable foliage

Only the following high-confidence, non-Block-Entity accretions are in `frontier_protocol:cleanup/removable`.

| Registry ID | Java class | Block Entity | Fluid / waterlogged | Natural | Spread | Random tick | Scheduled tick | Structure element | Player use | Classification | Replacement | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `spore:growths_big` | `GenericFoliageBlock` | No | Waterlogged | Yes | Yes | No | Water fluid only | Incidental | No | REMOVE | AIR / WATER | Non-solid ground/underwater foliage. |
| `spore:growths_small` | `GenericFoliageBlock` | No | Waterlogged | Yes | Yes | No | Water fluid only | Incidental | No | REMOVE | AIR / WATER | Non-solid ground/underwater foliage. |
| `spore:growth_mycelium` | `GenericFoliageBlock` | No | Waterlogged | No | Yes | No | Water fluid only | Incidental | No | REMOVE | AIR / WATER | Non-solid mycelial foliage, not the solid mycelium block. |
| `spore:fungal_stem_sapling` | `FungalSaplings` | No | No | Yes | Yes | Yes | No | Incidental | No | REMOVE | AIR | Non-structural growth precursor. |
| `spore:fungal_roots` | `HangingRoots` | No | No | No | Yes | No | No | Incidental | No | REMOVE | AIR | Hanging decoration without state to restore. |
| `spore:underwater_fungal_stem` | `UnderWaterFungalStem` | No | Waterlogged | No | Yes | No | Water fluid only | Incidental | No | REMOVE | WATER | Underwater foliage; actual fluid state preserves water. |
| `spore:underwater_fungal_stem_top` | `UnderWaterFungusTop` | No | Waterlogged | No | Yes | Yes | Water fluid only | Incidental | No | REMOVE | WATER | Underwater growth tip without inventory or terrain role. |
| `spore:wall_growths` | `WallFolliage` | No | Waterlogged | No | Yes | No | Water fluid only | Incidental | No | REMOVE | AIR / WATER | Face-attached decoration. |
| `spore:wall_growths_big` | `WallFolliage` | No | Waterlogged | No | Yes | No | Water fluid only | Incidental | No | REMOVE | AIR / WATER | Face-attached decoration. |
| `spore:wall_growths_fleshy` | `WallFolliage` | No | Waterlogged | No | Yes | No | Water fluid only | Incidental | No | REMOVE | AIR / WATER | Face-attached decoration. |
| `spore:hanging_fungal_stem` | `HangingStem` | No | No | No | Yes | No | No | Incidental | No | REMOVE | AIR | Hanging non-solid growth. |
| `spore:mycelium_veins` | `MyceliumVeins` | No | No | No | Yes | No | No | Incidental | No | REMOVE | AIR | Thin replaceable vein growth. |
| `spore:fungal_stem` | `FungalStem` | No | No | No | Yes | No | No | Incidental | No | REMOVE | AIR | Temporary foliage stem, not converted terrain. |
| `spore:fungal_stem_top` | `FungalStemTop` | No | No | No | Yes | No | No | Incidental | No | REMOVE | AIR | Temporary foliage tip. |
| `spore:rotten_bush` | `RottenBush` | No | Waterlogged | No | Yes | Yes | Water fluid only | No | No | REMOVE | AIR / WATER | Replaceable infected bush; no original terrain is needed. |
| `spore:rotten_grass` | `RottenBush` | No | Waterlogged | No | Yes | Yes | Water fluid only | No | No | REMOVE | AIR / WATER | Replaceable infected grass. |
| `spore:rotten_fern` | `RottenBush` | No | Waterlogged | No | Yes | Yes | Water fluid only | No | No | REMOVE | AIR / WATER | Replaceable infected fern. |
| `spore:rotten_crops` | `FungalCrops` | No | No | No | Yes | No | No | No | No | REMOVE | AIR | Infection replaces a crop in place; removal does not guess the crop type. |
| `spore:rotten_branch` | `Branch` | No | Waterlogged | No | Yes | No | Water fluid only | No | No | REMOVE | AIR / WATER | Non-colliding branch accretion placed beside converted wood. |
| `spore:glowshroom` | `HangingGlowShroom` | No | No | No | Yes | No | No | Incidental | No | REMOVE | AIR | Hanging decorative foliage. |

Natural structures may incidentally contain foliage, but these blocks are not a structure's load-bearing terrain, inventory, machine, nest, or living core. Cleanup remains limited to loaded chunks and does not identify or alter a structure as a unit.

## Explicit keep policy

The following groups are in `frontier_protocol:cleanup/never`. `KEEP` is established; `UNRESOLVED` is treated exactly as KEEP until a later audit proves removal safe.

| Registry ID | Java class | Block Entity | Fluid / waterlogged | Natural | Spread | Random tick | Scheduled tick | Structure element | Player use | Classification | Replacement | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `spore:cdu` | `CDUBlock` | `CDUBlockEntity` | No | No | Yes | BE tick | No | Equipment | Fuel/menu | KEEP | NONE | Persistent player machine that performs Spore's own cleanup. |
| `spore:container` | `Container` | `ContainerBlockEntity` | No | Yes | No | No | No | Yes | Inventory | KEEP | NONE | Randomizable container inventory. |
| `spore:cabinet` | `Cabinet` | `CabinetBlockEntity` | No | Yes | No | No | No | Yes | Inventory | KEEP | NONE | Persistent inventory. |
| `spore:surgery_table` | `SurgeryTableBlock` | `SurgeryTableBlockEntity` | No | Yes | No | BE tick | No | Yes | Interactive | KEEP | NONE | Player equipment with a Block Entity. |
| `spore:incubator` | `IncubatorBlock` | `IncubatorBlockEntity` | No | Yes | No | BE tick | No | Yes | Interactive | KEEP | NONE | Player equipment with persistent state. |
| `spore:zoaholic` | `ZoaholicBlock` | `ZoaholicBlockEntity` | No | Yes | No | BE tick | No | Yes | Interactive | KEEP | NONE | Player equipment with persistent state. |
| `spore:overgrown_spawner` | `OvergrownSpawner` | `OvergrownSpawnerEntity` | No | Yes | No | BE tick | No | Yes | No | KEEP | NONE | Active structure Block Entity. |
| `spore:brain_remnants` | `BrainRemnants` | `BrainRemnantBlockEntity` | No | Yes | No | BE tick | No | Yes | Interactive | KEEP | NONE | Active structure Block Entity. |
| `spore:outpost_watcher` | `OutpostWatcher` | `OutpostWatcherBlockEntity` | No | Yes | No | BE tick | No | Yes | No | KEEP | NONE | Spore tags it as foliage, but it is an active Block Entity. |
| `spore:biomass_lump` | `BiomassLump` | `BiomassLumpEntity` | No | Yes | Yes | BE tick | Yes | Living structure | No | KEEP | NONE | `LivingStructureBlocks` state and scheduled lifecycle. |
| `spore:hive_spawn` | `HiveSpawn` | `HiveSpawnBlockEntity` | Waterlogged | Yes | Yes | BE tick | Yes | Hive/nest | No | KEEP | NONE | Nest source with living-structure state. |
| `spore:infested_dirt` | `SelectableBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Original dirt variant cannot be recovered. |
| `spore:infested_stone` | `SelectableBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Removing it creates a terrain hole; original stone variant is unknown. |
| `spore:infested_netherrack` | `SelectableBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Converted terrain. |
| `spore:infested_soul_sand` | `SelectableBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Converted terrain. |
| `spore:infested_end_stone` | `SelectableBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Converted terrain. |
| `spore:infested_sand` | `SelectableFallingBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Converted falling terrain. |
| `spore:infested_gravel` | `SelectableFallingBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Converted falling terrain. |
| `spore:infested_deepslate` | `SelectableBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Converted terrain. |
| `spore:infested_red_sand` | `SelectableBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Converted terrain. |
| `spore:infested_clay` | `SelectableBlock` | No | No | No | Yes | No | No | Terrain | No | KEEP | NONE | Converted terrain. |
| `spore:infested_cobblestone` | `SelectableBlock` | No | No | No | Yes | No | No | Structure | No | KEEP | NONE | Original structure material is not recoverable. |
| `spore:infested_cobbled_deepslate` | `SelectableBlock` | No | No | No | Yes | No | No | Structure | No | KEEP | NONE | Original structure material is not recoverable. |
| `spore:infested_stone_bricks` | `SelectableBlock` | No | No | No | Yes | No | No | Structure | No | KEEP | NONE | Original structure material is not recoverable. |
| `spore:infested_bricks` | `SelectableBlock` | No | No | No | Yes | No | No | Structure | No | KEEP | NONE | Original structure material is not recoverable. |
| `spore:infested_laboratory_block` | `SelectableBlock` | No | No | No | Yes | No | No | Structure | No | KEEP | NONE | Laboratory structure conversion. |
| `spore:infested_laboratory_block1` | `SelectableBlock` | No | No | No | Yes | No | No | Structure | No | KEEP | NONE | Laboratory structure conversion. |
| `spore:infested_laboratory_block2` | `SelectableBlock` | No | No | No | Yes | No | No | Structure | No | KEEP | NONE | Laboratory structure conversion. |
| `spore:infested_laboratory_block3` | `SelectableBlock` | No | No | No | Yes | No | No | Structure | No | KEEP | NONE | Laboratory structure conversion. |
| `spore:rotten_log` | `FlamableRotatingBlock` | No | No | No | Yes | Yes | No | Structure | Building block | KEEP | NONE | Original wood species cannot be inferred. |
| `spore:rotten_planks` | `Block` | No | No | No | Yes | No | No | Structure | Building block | KEEP | NONE | Original wood species cannot be inferred. |
| `spore:rotten_stair` | `StairBlock` | No | No | No | Yes | No | No | Structure | Building block | KEEP | NONE | Original stair material cannot be inferred. |
| `spore:rotten_slab` | `SlabBlock` | No | Waterlogged | No | Yes | No | Water fluid only | Structure | Building block | KEEP | NONE | Original slab material cannot be inferred. |
| `spore:rooted_biomass` | `Block` | No | No | Yes | Yes | No | No | Biomass terrain | No | KEEP | NONE | Solid infection terrain. |
| `spore:biomass_block` | `Block` | No | No | Yes | Yes | No | No | Biomass structure | Building block | KEEP | NONE | Solid infection structure. |
| `spore:sicken_biomass_block` | `SickenBiomassBlock` | No | No | Yes | Yes | No | No | Biomass structure | Contact behavior | KEEP | NONE | Solid active biomass. |
| `spore:gastric_biomass_block` | `GastricBiomassBlock` | No | No | Yes | Yes | No | No | Biomass structure | Contact behavior | KEEP | NONE | Solid active biomass. |
| `spore:calcified_biomass_block` | `Block` | No | No | Yes | Yes | No | No | Biomass structure | Building block | KEEP | NONE | Solid infection structure. |
| `spore:membrane_block` | `MembraneBlock` | No | No | Yes | Yes | No | No | Biomass structure | Contact behavior | KEEP | NONE | Solid infection structure. |
| `spore:rooted_mycelium` | `Block` | No | No | Yes | Yes | No | No | Terrain | No | KEEP | NONE | Solid infection terrain. |
| `spore:fungal_shell` | `Block` | No | No | Yes | Yes | No | No | Structure | Building block | KEEP | NONE | Solid structure material. |
| `spore:mycelium_block` | `RotatedPillarBlock` | No | No | Yes | Yes | No | No | Structure | Building block | KEEP | NONE | Solid structure material. |
| `spore:mycelium_slab` | `SlabBlock` | No | Waterlogged | Yes | Yes | No | Water fluid only | Structure | Building block | KEEP | NONE | Solid structure material. |
| `spore:freeze_burned_biomass` | `FrozenBiomass` | No | No | No | No | Yes | No | Terrain | No | KEEP | NONE | Solid falling biomass with lifecycle state. |
| `spore:remains` | `Remains` | No | No | Yes | Yes | No | No | Incidental | Break effects/XP | KEEP | NONE | Meaningful remains with custom effects and drops. |
| `spore:wall_remains` | `WallRemainsBlock` | No | No | No | Yes | No | No | Incidental | Break effects | KEEP | NONE | Meaningful wall remains. |
| `spore:frozen_remains` | `FrozenRemains` | No | No | No | No | No | No | Incidental | Break effects | KEEP | NONE | Meaningful transformed remains. |
| `spore:cerebrum_block` | `Cerebrum` | No | No | Yes | Yes | No | No | Living structure | No | KEEP | NONE | Major organ block. |
| `spore:innards_block` | `Cerebrum` | No | No | Yes | Yes | No | No | Living structure | No | KEEP | NONE | Major organ block. |
| `spore:heart_block` | `Cerebrum` | No | No | Yes | Yes | No | No | Living structure | No | KEEP | NONE | Major organ block. |
| `spore:braio_block` | `Cerebrum` | No | No | Yes | Yes | No | No | Living structure | No | KEEP | NONE | Major organ block. |

## Unresolved blocks

These blocks are explicitly protected by `cleanup/never`. Some appear in Spore's own broad foliage tags, but specialized lifecycle or hazard behavior prevents an R5 REMOVE decision.

| Registry ID | Java class | Block Entity | Fluid / waterlogged | Natural | Spread | Random tick | Scheduled tick | Structure element | Player use | Classification | Replacement | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `spore:drowned_lump` | `DrownedLump` | No | Water context | No | Yes | No | No | No | No | UNRESOLVED | NONE | Specialized underwater behavior needs mutation testing. |
| `spore:bile_lump` | `BileLump` | No | Water context | No | Yes | No | No | No | No | UNRESOLVED | NONE | Specialized bile behavior is not decorative foliage. |
| `spore:fang_lump` | `FangLump` | No | No | No | Yes | No | No | No | No | UNRESOLVED | NONE | Specialized damaging behavior. |
| `spore:exploding_lump` | `ExplodingLump` | No | No | No | Yes | No | No | No | No | UNRESOLVED | NONE | Explosive lifecycle behavior. |
| `spore:poisoning_lump` | `CorrosiveDrownedLump` | No | Water context | No | Yes | No | No | No | No | UNRESOLVED | NONE | Corrosive lifecycle behavior. |
| `spore:organite` | `OrganiteBlock` | No | No | No | Yes | No | Yes | Growth | No | UNRESOLVED | NONE | Scheduled lifecycle mutation. |
| `spore:rotten_scraps` | `WoodenScraps` | No | Waterlogged | No | Yes | No | Water fluid only | Debris | No | UNRESOLVED | NONE | Falling debris rather than simple foliage. |
| `spore:biomass_bulb` | `BiomassBulb` | No | Water context | No | Yes | No | No | Growth | No | UNRESOLVED | NONE | Biomass growth role is not proven disposable. |
| `spore:hand` | `Hand` | No | No | No | Yes | Yes | No | Growth | No | UNRESOLVED | NONE | Active random-tick organ growth. |
| `spore:lungs` | `CancerLungs` | No | No | No | Yes | No | No | Growth | No | UNRESOLVED | NONE | Organ-like growth. |
| `spore:acidic_sack` | `FallingAcidSack` | No | No | No | Yes | No | No | Growth | No | UNRESOLVED | NONE | Falling hazardous organ. |
| `spore:vocals` | `WallVocalsBlock` | No | No | No | Yes | No | No | Growth | Entity interaction | UNRESOLVED | NONE | Specialized entity interaction. |
| `spore:blomfung` | `HangingPlant` | No | Waterlogged | Yes | Yes | Yes | Water fluid only | Incidental | Slowing and contact movement damage | UNRESOLVED | NONE | Active infection trap that changes into another block during random ticks. |
| `spore:bloomfung2` | `HangingPlantBub` | No | Waterlogged | Yes | Yes | Yes | Water fluid only | Incidental | Mycelium cloud and self-conversion | UNRESOLVED | NONE | Contact creates a Mycelium area-effect cloud and converts the block; random ticks generate hanging stem and foliage. |
| `spore:fungal_clamp` | `FungalClamp` | No | Waterlogged | No | Yes | Yes | Water fluid only | Growth | Damage, slowing, Mycelium application, and OPEN state lifecycle | UNRESOLVED | NONE | Active infection trap whose OPEN state closes on contact and reopens during random ticks. |

Promoting these three active hazards back to REMOVE requires an explicit game-design decision that infection traps are automatic cleanup targets and dedicated side-effect GameTests for each block.

## Hive, tumor, and structure boundaries

- `spore:hive_spawn` is the registered hive/nest block and has `HiveSpawnBlockEntity`; it is KEEP.
- `spore:mound`, `spore:proto`, and `spore:hivetumor` are entities, not blocks. A block-only policy cannot select or remove them.
- Infection Tendril is entity behavior and is outside block cleanup.
- Spore's `#spore:churches` and `#spore:laboratories` are worldgen structure tags, not block tags. They contain structure IDs such as `spore:church`, `spore:cathedral`, `spore:lab`, and `spore:mines`; there is no valid block-tag expansion to treat as cleanup foliage.
- Structure templates can include containers, laboratory blocks, infected masonry, biomass, organs, and hive blocks. Those block types are protected independently. R5 does not infer safety from a block's location inside or outside a structure.

## Policy and precedence

The policy reads Frontier Protocol's current data-pack tags on every decision. It does not cache registry membership or retain a world reference.

```text
BlockState.hasBlockEntity()
  -> KEEP

frontier_protocol:cleanup/never
  -> KEEP

frontier_protocol:cleanup/removable
  -> REMOVE

otherwise
  -> KEEP
```

The future mutation caller must also recheck the loaded world's actual Block Entity at the target before changing a block. The state-level guard protects every registered `EntityBlock`, including accidental additions to `cleanup/removable`; the runtime guard protects inconsistent or dynamically present Block Entities.

For REMOVE only:

```text
state.getFluidState().getType() is the water fluid family
  -> minecraft:water default state

otherwise
  -> minecraft:air default state
```

The policy never executes loot tables, drops items, awards experience, scans a world, or mutates a block. Lava and other fluids are not guessed. A Spore namespace match is never a cleanup criterion.

## Why reverse conversion is excluded

Spore conversion data maps original blocks or tags toward infected outputs. It does not provide an injective infected-to-original mapping. Multiple wood species, masonry variants, terrain blocks, and tag members can converge on the same infected state. Block properties, neighboring blocks, biome, and structure context cannot prove the original state. R5 therefore removes only foliage that safely becomes AIR or retained WATER and leaves all converted terrain and structures unchanged.

## Compatibility boundary

This policy is valid only for exact Spore `2.2.0j`. Any dependency update requires a new JAR hash audit, registry/resource comparison, and explicit review of both cleanup tags. Spore's own `fungal_blocks`, `ground_foliage`, and `removable_foliage` tags are intentionally not used as allowlists because they include active or Block-Entity-backed blocks such as `spore:outpost_watcher` and `spore:biomass_lump`.
