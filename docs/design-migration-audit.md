# Frontier Protocol design migration audit

## Scope and status

This document is the R0 audit required by `impl-plan/v1.md`. It records the
state inspected before changing or removing existing gameplay code.

- Audit date: 2026-07-18
- Branch: `main`
- Repository state: no commits exist yet; every project file is untracked
- Baseline verification:
  - `./gradlew build`: successful
  - `./gradlew test --rerun-tasks`: successful; all 11 unit-test classes pass
  - `./gradlew runGameTestServer`: successful; all 31 required GameTests pass
- Baseline versions: Java 21, Minecraft 1.21.1, NeoForge 21.1.235, Create
  6.0.11-295
- Scope of this change: documentation only; no registrations, gameplay code,
  data, or tests were changed

The current implementation is an active old M0-M6 design. Sector discovery,
mob scaling, custom infection, breaching, nutrition, initial protection, and
reload listeners are connected through automatic NeoForge event subscribers.
The blocks are registered even though the repository contains no acquisition
recipes or creative-tab insertion.

## Current package inventory

All production code is below
`src/main/java/dev/upiscium/frontierprotocol`.

| Package | Current responsibility | New-design disposition |
| --- | --- | --- |
| root | Mod bootstrap and global server events | Reuse after removing old event routes |
| `registry` | Blocks, items, block entities, menus, attachments, data maps, tags | Reuse registry structure; replace old registrations |
| `config` | Sector, beacon, mob, infection, breach, nutrition, and client settings | Reuse config framework; replace old options |
| `world` | World `SavedData` for origin and sector state | Reuse initial origin and serialization basis |
| `protection` | Initial-spawn and beacon protection queries | Redesign as infection suppression core |
| `infection` | Custom pressure, core, nest, and nest-spawn simulation | Remove after R1 isolation |
| `mob` | Distance scaling and infection-carrier state | Remove after R1 isolation |
| `breach` | Custom wall-breaking AI and protected explosion handling | Remove after R1 isolation |
| `sector` | Deterministic sectors, traits, guarantees, and discovery | Freeze, unregister, then remove from release code |
| `resource` | Infinite item-producing resource nodes | Unregister and remove |
| `oil` | Optional-TFMG infinite crude-oil wells | Unregister and remove |
| `nutrition` | Repeated-food penalties and history | Disable and remove; outside final scope |
| `data` | JSON reload listeners and codec-backed definitions | Reuse pattern; remove old concrete domains |
| `network` | Sector and nutrition client payloads | Reuse payload registration pattern only |
| `command` | Sector, protection, infection, and nutrition administration | Reuse root command structure only |
| `client` | Resource-node and oil-well screens | Remove old screens; reuse menu-screen patterns if needed |
| `gametest` | In-game tests for old M0-M6 behavior | Reuse GameTest harness; replace old behavior tests |

No `api`, `stabilization`, `spawnprotection`, `integration.create`, or
`integration.spore` package currently exists.

## Registered and persisted content

### Blocks and items

`ModBlocks` and `ModItems` register five blocks and their block items.

| ID | Implementation | Classification |
| --- | --- | --- |
| `stabilization_beacon` | `StabilizationBeaconBlock` | Small-to-large redesign candidate; do not treat as final Tier 1 |
| `infection_core` | `InfectionCoreBlock` | Direct conflict; remove |
| `infection_nest` | `InfectionNestBlock` | Direct conflict; remove |
| `resource_node` | `ResourceNodeBlock` | Direct conflict; remove |
| `oil_well` | `OilWellBlock` | Direct conflict; remove |

There are no standalone component, consumable, filter, medium, or
stabilization-cell items. There is no creative tab or creative-tab insertion.

### Block entities and capabilities

`ModBlockEntities` registers:

| ID | Block entity | Capability |
| --- | --- | --- |
| `stabilization_beacon` | `StabilizationBeaconBlockEntity` | One-slot item handler |
| `infection_nest` | `InfectionNestBlockEntity` | None |
| `resource_node` | `ResourceNodeBlockEntity` | Extraction-only item handler |
| `oil_well` | `OilWellBlockEntity` | Extraction-only fluid handler |

The beacon persists its inventory, fuel ticks, and grace ticks. Its inventory
and NBT lifecycle are useful implementation references. It is not a Create
kinetic block entity and has no RPM, stress, overstress, tier, cleanup cursor,
or full final-state persistence.

### SavedData

`FrontierProtocolWorldData` is the only `SavedData`, stored as
`frontier_protocol_world` with schema version 1. It persists:

- A seed fingerprint
- Initial spawn chunk coordinates
- Sector size and placement version
- Initialization flags
- Forced sector-trait overrides
- Discovered sectors

The initial spawn chunk is captured only on first initialization and therefore
already has the required behavior of not following later world-spawn changes.
The sector fields conflict with the new design. There is no persisted spawn
protection enabled state, dedicated radius, or configured dimension.

### Data attachments

`ModAttachments` registers:

- `mob_scaling` on entities via `MobScalingState`
- `chunk_infection` on chunks via `ChunkInfectionState`
- `food_history` on players via `FoodHistoryState`, copied on death

All three carry old or out-of-scope gameplay state. The attachment registration
and Codec patterns are reusable, but none of these attachments is required by
the final design as currently defined.

### Data maps

`ModDataMaps` registers:

- Item map `stabilization_fuels` using `FuelDefinition`
- Entity-type map `mob_scaling` using `MobScalingDefinition`

The fuel data map is reusable after switching it to owned stabilization fuel.
The shipped values refer conditionally to `kubejs:stabilization_cell` and
`kubejs:industrial_stabilization_cell`; this mod does not register either item.
The mob-scaling map is a removal candidate.

### Network payloads

`NetworkRegistration` registers protocol version 1 and two play-to-client
payloads:

- `SectorInfoPayload` as `sector_info`
- `NutritionResultPayload` as `nutrition_result`

There is no stabilizer state, range, fuel, RPM, stress, or server-config sync.

### Commands

`FrontierProtocolCommands` registers permission-level-2 commands below
`/frontierprotocol`:

- `sector info`, `sector locate`, and `sector set`
- `protection status`
- `infection get`, `infection set`, and `infection clear`
- `nutrition inspect` and `nutrition clear`

The root registration pattern is reusable. The subcommands expose old systems.
Final spawn-protection management and suppression diagnostics do not exist.

## Existing gameplay systems

### Protection

`ProtectionService` exposes chunk and block queries plus source lookup.
`ServerProtectionService` combines a permanent Overworld initial-spawn square
with loaded beacons. `ProtectionGeometry` performs overflow-safe square-radius
checks. `ProtectionIndex` stores loaded beacon block entities by their source
chunk and searches nearby source chunks for each query.

Useful properties:

- Chunk-based and independent of block Y
- Correct lifecycle hooks in beacon `onLoad`, `onChunkUnloaded`, and
  `setRemoved`
- Server-thread guard in `ProtectionIndex`
- Initial spawn center persisted independently of later spawn changes
- Fuel to grace to offline transition already represented and tested

Required redesign:

- Rename and narrow semantics to infection suppression only
- Index each covered target chunk directly for near-O(1) queries
- Track source identity or a source set per target chunk so overlaps are safe
- Separate spawn protection and stabilizer sources
- Make the index dimension-local without restricting the service to Overworld
- Treat the index as a rebuildable cache, not persisted truth
- Remove calls that use protection to prevent mob block breaking or explosions
- Add final tier, kinetic, state, capacity, cleanup, and synchronization data

Current initial protection is always enabled and uses
`initialProtectionRadius` with default 2. The current beacon has configurable
radius 1 and grace 6000 ticks, consumes only fuel, and can be disabled by
redstone. It does not satisfy the final stabilizer operating conditions.

### Custom infection

The `infection` package implements a complete replacement infection ecology:

- `ChunkInfectionState` stores pressure, custom core position, maturation, and
  custom nest UUID in a chunk attachment
- `InfectionService` raises pressure from carrier mobs, places cores, matures
  nests, and spawns mobs
- `InfectionRuntimeIndex` tracks carriers, nests, nest mobs, and active chunks
- `InfectionEventHandlers` drives registration and budgeted slow ticks
- `InfectionCoreBlock`, `InfectionNestBlock`, and
  `InfectionNestBlockEntity` provide custom infection content

This directly conflicts with delegating infection ecology, cores, nests,
Hivemind, and infected mobs to Spore.

### Mob scaling and breaching

`MobScalingService` applies permanent health, attack, armor, and speed changes
based on distance from the saved origin. `MobEventHandlers` marks configured
natural mobs as carriers and applies scaling.

`BreachGoal` gives tagged mobs custom wall-breaking behavior. Protection blocks
that behavior, and `BreachEventHandlers` removes protected blocks from hostile
explosion affected-block lists. Distance-based strengthening, custom breach
gameplay, mob block-break prevention, and explosion prevention are all outside
or directly contrary to the final design.

### Sectors, resources, and oil

The `sector` package provides deterministic sector placement, guaranteed nearby
traits, discovery, a discovery event, payloads, commands, and persisted
overrides. `SectorPos.fromChunk` correctly uses `Math.floorDiv` and is a useful
negative-coordinate reference.

`ResourceNodeBlockEntity` selects a definition from the local sector and
produces item outputs forever. `OilWellBlockEntity` requires an oil-field trait
and optional TFMG, then produces crude oil forever. These are explicitly
excluded from Frontier Protocol's final responsibilities.

### Nutrition

The `nutrition` package tracks recent foods, classifies food tags, reduces food
and saturation gains for repetition, persists player history, and sends a
client result payload. It affects ordinary player eating without requiring
Frontier Protocol equipment. It is not part of the final narrowly defined
Spore/Create integration and should not remain active.

## Data and asset inventory

### Data definitions

- Sector traits: `ferrous_strata`, `fertile_basin`, `resonant_crust`, and
  `oil_field`
- Resource nodes: `ferrous_node`, `biomass_node`, and
  `resonant_crystal_node`
- Oil well: `oil_well`
- Item data map: `stabilization_fuels`

### Tags

- Entity types: `distance_scaled_mobs`, `outbreak_carriers`, `nest_spawns`,
  and `breacher_mobs`
- Blocks: `infection_core_ground`, `infection_core_replaceable`, and
  `mob_breakable`
- Food item categories: grain, vegetable, fruit, meat, fish, dairy, soup,
  dessert, and preserved
- Relocation: all five current blocks occur in Create, `c`, and Simulated
  non-movable/relocation-not-supported tags

The Create non-movable tag approach can be reused for stationary stabilizers.

### Models, loot, and language

All five blocks have blockstates, vanilla-texture block models, item models,
and loot tables. Infection core and nest intentionally drop nothing. English
and Japanese language files describe only old gameplay. There are no custom
PNG textures, sounds, particles, Ponder scenes, or final tier translations.

### Recipes and data generation

There are no recipe JSON files, Create processing recipes, or Java data
providers. `src/generated/resources` is configured but absent. The Gradle build
generates only an empty GameTest structure.

`examples/kubejs/server_scripts/frontier_protocol_compat.js` supplies optional
TFMG/Aeronautics example recipes unrelated to containment and is not packaged
as the mod's datapack recipe set.

## Test inventory

### Unit tests

There are 11 unit-test classes under `src/test/java`:

- `sector`: `SectorPosTest`, `SectorTraitDefinitionTest`, and
  `SectorPlacementServiceTest`
- `protection`: `ProtectionGeometryTest` and `BeaconFuelStateTest`
- `mob`: `MobScalingServiceTest`
- `infection`: `ChunkInfectionStateTest`
- `breach`: `BreachRulesTest`
- `nutrition`: `NutritionServiceTest`
- `resource`: `ResourceNodeDefinitionTest`
- `oil`: `OilWellDefinitionTest`

The chunk-boundary, negative-coordinate, Codec-validation, and fuel/grace test
patterns are reusable after changing their final semantics. Tests asserting
old sectors, custom infection, scaling, breaching, nutrition, resources, or oil
must not remain as final product specifications.

### GameTests

Eight holders under the production `gametest` package test sectors, protection,
mob scaling, custom infection, breaching, nutrition, resource nodes, and oil
wells. The GameTest run configuration, holder pattern, and generated
`empty.nbt` are reusable. Most test cases specify behavior that R1 will disable;
notably, existing breach tests require protected walls and explosion immunity,
which the final design forbids.

## Dependency and integration audit

### Create

Create is a required build and metadata dependency. No production Java source
imports a Create API. Current integration consists only of dependency metadata,
non-movable tags, and optional example script references. There is no
`KineticBlock`, `KineticBlockEntity`, shaft connection, RPM requirement, stress
impact, goggles information, Ponder scene, or Create processing recipe.

### Spore

Spore is entirely absent:

- No Gradle dependency or version property
- No required `neoforge.mods.toml` dependency
- No public API or event usage
- No compatibility package or content classifier
- No tags or recipes
- No mixin configuration, Access Transformer, or reflection
- No concrete infection-generation hook targets documented yet

An exact Spore version and artifact source must be established before R1 can
claim the required development environment and before R4 can identify target
classes and methods. No mixin should be added until those targets, reasons, and
event/API alternatives are documented.

### Metadata and documentation

The metadata description still says "Sector exploration and progression
systems for Create-centered modpacks." The README is mostly stock MDK setup
text. Both require final-design updates, but README's complete product wording
belongs after the gameplay migration rather than this documentation-only R0.

## Four-way migration classification

### 1. Reuse

- Java 21, NeoForge 1.21.1, Gradle wrapper, and Create dependency foundation
- Mod bootstrap and deferred-register organization
- Config registration mechanism and value validation pattern
- Codec and reload-listener implementation patterns
- Payload registration pattern
- GameTest and JUnit infrastructure
- CI workflow and Nix development shell
- Common logger
- `ProtectionGeometry` square chunk calculations
- Non-movable tag strategy

### 2. Modify

- `ProtectionService` as the basis for `InfectionSuppressionService`
- `ProtectionIndex` lifecycle as the basis for a covered-chunk/source index
- `ServerProtectionService` initial spawn lookup as a spawn suppression source
- Initial origin fields and SavedData mechanics in `FrontierProtocolWorldData`
- Beacon inventory, capability, persistence, and load/unload hooks
- `BeaconFuelState` as a basis for ACTIVE/grace transitions
- `FuelDefinition` and the stabilization-fuel data-map concept
- Menu/screen synchronization patterns
- Root command registration
- Negative-coordinate tests from `SectorPosTest` and protection tests
- Unit and GameTests retained as infrastructure must be rewritten to assert the
  new suppression behavior rather than old gameplay
- `README.md`, metadata, English translations, and Japanese translations must
  be updated to describe the final Spore/Create responsibility split
- CI remains reusable, but its GameTest expectations must follow the active R1
  feature set

### 3. Freeze

- Sector coordinate, placement, trait Codec, guarantee, and discovery code
- Sector, resource-node, and oil definition reload code and JSON definitions
- Sector discovery event and state fields
- Old data definitions that may help migration analysis

These items must not be automatically active or registered in the release JAR.
Because this repository has no committed history or released compatibility
contract, R1 may delete them after their references and useful patterns are
captured rather than maintaining compatibility shims.

### 4. Delete

- Custom infection pressure, core, nest, and spawning implementation
- Infection blocks, block entity, attachment, assets, tags, commands, and tests
- Distance-dependent mob scaling and carrier gameplay
- Custom mob breach AI and explosion protection
- Resource-node and oil-well gameplay registrations, menus, screens, assets,
  definitions, and tests
- Old stabilization beacon as a protection/CDU substitute; retain only its
  generic implementation patterns
- Sector gameplay registration and infinite-resource exposure
- Nutrition gameplay unless a separate product decision explicitly restores it
- Old-only translations and optional scripts

## New-design conflicts and gaps

- No Spore dependency or integration exists.
- Existing custom infection replaces responsibilities that must belong to
  Spore.
- Existing protection blocks mob breaching and hostile explosions; final
  suppression must not do either.
- Existing beacon has no Create kinetic integration or tier definition.
- Existing index searches source neighborhoods instead of directly indexing
  covered chunks.
- Existing spawn protection is always enabled, Overworld-only, and not a
  first-class source registration.
- No three-tier model, final operating-state model, cleanup cursor, or global
  cleanup budget exists.
- No stabilization cell or automated Create manufacturing line exists.
- No Spore infection creation, spread, erosion, vegetation, or nest/Hivemind
  progression hook exists.
- No containment state/range user interface exists.
- No recipes exist for any current or final content.

## Spore integration candidate boundaries

The current repository cannot identify exact Spore classes or methods because
the dependency is absent. The following Frontier Protocol-side boundaries are
the intended candidates after adding and inspecting the supported Spore
artifact:

1. Convert `ProtectionService` into a public, Spore-independent suppression
   query API accepting `ServerLevel`, `BlockPos`, and `ChunkPos`.
2. Convert `ProtectionIndex` into a dimension-local cache from target chunk to
   one or more source identities.
3. Register persisted initial spawn protection and loaded stabilizers through
   the same index API.
4. Place Spore-specific event handlers or narrowly scoped mixins only under
   `integration/spore`; they may call the public suppression API, but
   stabilizer code must not call Spore internals.
5. Investigate Spore's public events/APIs first for target block placement,
   spread, underground erosion, vegetation growth, terrain conversion, and
   nest/Hivemind progression.
6. For every missing public hook, document the Spore class, method, injection
   point, reason, version constraint, and rejected alternative before adding a
   mixin.

The old `InfectionService` call to `ServerProtectionService.isChunkProtected`
is a useful example of the desired dependency direction only. The custom
infection service itself is not a Spore compatibility layer and must not be
retained as one.

## Updated implementation order

### R1: disable old gameplay and stabilize the build

1. Add the selected Spore repository, artifact, exact version property, and
   required metadata dependency; verify its actual mod ID and supported
   Minecraft/NeoForge version.
2. Remove old automatic event routes from `FrontierProtocolEvents` and the
   `@EventBusSubscriber` classes for sectors, custom infection, mob scaling,
   breaching, and nutrition.
3. Remove old block, item, block-entity, capability, menu, attachment, data-map,
   payload, reload-listener, and client-screen registrations.
4. Keep only bootstrap/config/test infrastructure and any unregistered code
   needed temporarily to make deletion reviewable; do not ship dormant
   registry entries.
5. Remove dangling old assets, tags, definitions, translations, commands, and
   tests after confirming no remaining registration references them.
6. Update metadata and the short README overview to the final Spore/Create
   responsibility split.
7. Run `build`, GameTest server, client, dedicated server, new-world, and
   existing-world smoke checks. Existing-world migration should tolerate
   missing old blocks/entities without a fatal startup crash; no compatibility
   layer is otherwise assumed at this unreleased, no-history stage.

R1 must not introduce final stabilizer gameplay or Spore mixins.

### R1 planned file changes

The list below is the planned R1 change set. Directory entries mean every file
currently below that path. R1 must verify references immediately before each
deletion and may narrow this list if a reusable implementation pattern needs to
remain temporarily unregistered.

Files to modify:

- `build.gradle`
- `gradle.properties`
- `README.md`
- `src/main/templates/META-INF/neoforge.mods.toml`
- `src/main/java/dev/upiscium/frontierprotocol/FrontierProtocolMod.java`
- `src/main/java/dev/upiscium/frontierprotocol/FrontierProtocolEvents.java`
- `src/main/java/dev/upiscium/frontierprotocol/config/FrontierProtocolServerConfig.java`
- `src/main/java/dev/upiscium/frontierprotocol/config/FrontierProtocolClientConfig.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModBlocks.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModItems.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModBlockEntities.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModMenus.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModAttachments.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModDataMaps.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModBlockTags.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModEntityTypeTags.java`
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModItemTags.java`
- `src/main/java/dev/upiscium/frontierprotocol/network/NetworkRegistration.java`
- `src/main/java/dev/upiscium/frontierprotocol/command/FrontierProtocolCommands.java`
- `src/main/java/dev/upiscium/frontierprotocol/world/FrontierProtocolWorldData.java`
- `src/main/resources/assets/frontier_protocol/lang/en_us.json`
- `src/main/resources/assets/frontier_protocol/lang/ja_jp.json`
- `src/main/resources/data/create/tags/block/non_movable.json`
- `src/main/resources/data/c/tags/block/relocation_not_supported.json`
- `src/main/resources/data/simulated/tags/block/non_movable.json`
- `.github/workflows/build.yml` only if the active GameTest command or
  dependency setup requires adjustment

Old Java implementation planned for freeze/unregistration and then deletion
from the release source set:

- `src/main/java/dev/upiscium/frontierprotocol/breach/`
- `src/main/java/dev/upiscium/frontierprotocol/infection/`
- `src/main/java/dev/upiscium/frontierprotocol/mob/`
- `src/main/java/dev/upiscium/frontierprotocol/nutrition/`
- `src/main/java/dev/upiscium/frontierprotocol/oil/`
- `src/main/java/dev/upiscium/frontierprotocol/resource/`
- `src/main/java/dev/upiscium/frontierprotocol/sector/`
- `src/main/java/dev/upiscium/frontierprotocol/client/ClientMenuRegistration.java`
- `src/main/java/dev/upiscium/frontierprotocol/client/OilWellScreen.java`
- `src/main/java/dev/upiscium/frontierprotocol/client/ResourceNodeScreen.java`
- `src/main/java/dev/upiscium/frontierprotocol/data/OilWellReloadListener.java`
- `src/main/java/dev/upiscium/frontierprotocol/data/ResourceNodeReloadListener.java`
- `src/main/java/dev/upiscium/frontierprotocol/data/TraitReloadListener.java`
- `src/main/java/dev/upiscium/frontierprotocol/data/MobScalingDefinition.java`
- `src/main/java/dev/upiscium/frontierprotocol/network/NutritionResultPayload.java`
- `src/main/java/dev/upiscium/frontierprotocol/network/SectorInfoPayload.java`

Protection code planned for selective retention or removal after its generic
patterns are captured:

- `src/main/java/dev/upiscium/frontierprotocol/protection/ProtectionService.java`
- `src/main/java/dev/upiscium/frontierprotocol/protection/ServerProtectionService.java`
- `src/main/java/dev/upiscium/frontierprotocol/protection/ProtectionIndex.java`
- `src/main/java/dev/upiscium/frontierprotocol/protection/ProtectionSource.java`
- `src/main/java/dev/upiscium/frontierprotocol/protection/StabilizationBeaconBlock.java`
- `src/main/java/dev/upiscium/frontierprotocol/protection/StabilizationBeaconBlockEntity.java`
- `src/main/java/dev/upiscium/frontierprotocol/protection/BeaconFuelState.java`
- `src/main/java/dev/upiscium/frontierprotocol/protection/BeaconStatus.java`
- `src/main/java/dev/upiscium/frontierprotocol/data/FuelDefinition.java`

`ProtectionGeometry.java` is planned to remain as reusable chunk geometry.
R1 should preserve or move it without changing behavior.

Old resources planned for deletion after their registrations are removed:

- `src/main/resources/assets/frontier_protocol/blockstates/`
- `src/main/resources/assets/frontier_protocol/models/block/`
- `src/main/resources/assets/frontier_protocol/models/item/`
- `src/main/resources/data/frontier_protocol/data_maps/`
- `src/main/resources/data/frontier_protocol/frontier_protocol/oil_wells/`
- `src/main/resources/data/frontier_protocol/frontier_protocol/resource_nodes/`
- `src/main/resources/data/frontier_protocol/frontier_protocol/sector_traits/`
- `src/main/resources/data/frontier_protocol/loot_table/blocks/`
- `src/main/resources/data/frontier_protocol/tags/block/`
- `src/main/resources/data/frontier_protocol/tags/entity_type/`
- `src/main/resources/data/frontier_protocol/tags/item/food_categories/`
- `examples/kubejs/server_scripts/frontier_protocol_compat.js`

Old tests planned for deletion or replacement:

- `src/test/java/dev/upiscium/frontierprotocol/breach/`
- `src/test/java/dev/upiscium/frontierprotocol/infection/`
- `src/test/java/dev/upiscium/frontierprotocol/mob/`
- `src/test/java/dev/upiscium/frontierprotocol/nutrition/`
- `src/test/java/dev/upiscium/frontierprotocol/oil/`
- `src/test/java/dev/upiscium/frontierprotocol/resource/`
- `src/test/java/dev/upiscium/frontierprotocol/sector/`
- `src/test/java/dev/upiscium/frontierprotocol/protection/BeaconFuelStateTest.java`
- `src/test/java/dev/upiscium/frontierprotocol/protection/ProtectionGeometryTest.java`
  remains and is updated only if package naming changes
- `src/main/java/dev/upiscium/frontierprotocol/gametest/BreachGameTests.java`
- `src/main/java/dev/upiscium/frontierprotocol/gametest/InfectionGameTests.java`
- `src/main/java/dev/upiscium/frontierprotocol/gametest/MobScalingGameTests.java`
- `src/main/java/dev/upiscium/frontierprotocol/gametest/NutritionGameTests.java`
- `src/main/java/dev/upiscium/frontierprotocol/gametest/OilWellGameTests.java`
- `src/main/java/dev/upiscium/frontierprotocol/gametest/ProtectionGameTests.java`
- `src/main/java/dev/upiscium/frontierprotocol/gametest/ResourceNodeGameTests.java`
- `src/main/java/dev/upiscium/frontierprotocol/gametest/SectorGameTests.java`

### R2: infection suppression core

1. Introduce the public suppression API and immutable source identity/type.
2. Implement one dimension-local covered-chunk index with source sets or
   equivalent overlap-safe reference handling.
3. Make register, update, and unregister operations explicitly replace each
   source's covered chunk set.
4. Persist the initial spawn center and spawn-protection settings as source
   truth; rebuild the runtime index on server/world initialization.
5. Add bounded server config for enablement and radius, defaulting to radius 2.
6. Test negative chunks, block-to-chunk boundaries, all build heights,
   dimensions, 5x5 spawn range, overlap removal, and SavedData reload.

R2 completes the query/index contract before attempting to suppress every
Spore pathway.

### R3 and later

- R3 adapts the reusable beacon lifecycle into a shared tier-driven Create
  kinetic stabilizer implementation, beginning with Tier 1 only.
- R4 inspects and documents exact Spore pathways, then uses standard events or
  public APIs before any narrowly scoped mixin.
- R5 adds budgeted cleanup of loaded chunks without full-height repeated scans
  and without deleting nests.
- R6 adds the smallest owned stabilization-cell production chain and Create
  recipes.
- R7 adds Tier 2 and Tier 3 through the shared implementation.
- R8 adds range/state display, goggles information, tooltips, and Ponder.
- R9 performs load, compatibility, dedicated-server, balance, documentation,
  license, and release verification.

No attempt should combine R0 through R9 into one change. The immediate next
implementation milestone after this audit is R1.
