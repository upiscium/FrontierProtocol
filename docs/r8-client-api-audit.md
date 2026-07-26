# R8 Client API Audit

## Resolved artifacts

| Component | Resolved artifact | SHA-256 |
| --- | --- | --- |
| Create | `create-1.21.1-6.0.11-295.jar` | `feecba85e035b59a4f0181eb50e60eb721a1013f4b377d9e91606e6ab3613e4e` |
| Ponder | `ponder-neoforge-1.0.82+mc1.21.1.jar` | `0cf4611ad853042b689ac386184c5bbe02950efcffddb49e5f604e82baddb0dc` |
| NeoForge | `neoforge-21.1.235-universal.jar` | `dbded1a88b4a4f4e30a981672e58132dbd0cae64677e33a2ad867d00a4343d5e` |

Create embeds the same Ponder JAR at `META-INF/jarjar/ponder-neoforge-1.0.82+mc1.21.1.jar`; its bytes and SHA-256 match the resolved standalone artifact. The audit used the Gradle-resolved artifacts, `javap`, and archive contents rather than examples for another Minecraft or NeoForge version.

## Create Goggles

The following are public, common-side Create API unless noted otherwise.

| Class | Signature used | Role | Alternative |
| --- | --- | --- | --- |
| `IHaveGoggleInformation` | `boolean addToGoggleTooltip(List<Component>, boolean)` | Targeted live diagnostics | A custom HUD was rejected. |
| `IHaveHoveringInformation` | `boolean addToTooltip(List<Component>, boolean)` | Audited but not required for R8 | NeoForge item tooltip event is used for inventory items. |
| `IHaveCustomOverlayIcon` | `ItemStack getIcon(boolean)` | Tier BlockItem icon | The default Create icon could be retained. |
| `GogglesItem` | `static boolean isWearingGoggles(Player)` | Range visibility gate | Inspecting armor slots directly would duplicate Create behavior. |
| `KineticBlockEntity` | `boolean addToGoggleTooltip(List<Component>, boolean)` | Preserve Create kinetic diagnostics | Reimplementing Stress diagnostics was rejected. |

`KineticBlockEntity` implements the two tooltip interfaces. Its Goggle method is common-side. `tickAudio()` is client-only and is not used. No private field, Mixin accessor, or reflection is required.

## Ponder

Ponder `1.0.82+mc1.21.1` is present as a Create runtime dependency. Registration interfaces are public and side-neutral, but scene registry implementation references `Minecraft`; registration therefore occurs only from the Frontier Protocol client entrypoint.

| Class | Signature used | Role | Alternative |
| --- | --- | --- | --- |
| `PonderIndex` | `static void addPlugin(PonderPlugin)` | Register one client plugin | Manual internal registry mutation was rejected. |
| `PonderPlugin` | `getModId`, `registerScenes`, `registerTags` | Mod registration boundary | No internal Create plugin subclass. |
| `PonderSceneRegistrationHelper` | `withKeyFunction`, `forComponents`, `addStoryBoard` | Public scene registration | Direct scene-registry access was rejected. |
| `PonderTagRegistrationHelper` | `withKeyFunction`, `registerTag`, `addToTag` | Public index/tag registration | Direct tag-registry access was rejected. |

For schematic ID `frontier_protocol:stabilizer/operation`, the audited loader resolves `assets/frontier_protocol/ponder/stabilizer/operation.nbt`. It does not use the GameTest/data-pack structure path. Create's own structures follow the same `assets/create/ponder/...` layout.

## NeoForge client events

| Event | Public signature used | Side | Alternative |
| --- | --- | --- | --- |
| `RenderLevelStageEvent` | `getStage`, `getPoseStack`, `getCamera`; stage `AFTER_TRANSLUCENT_BLOCKS` | Logical client main event bus | A `LevelRenderer` Mixin was rejected. |
| `ItemTooltipEvent` | `getItemStack`, `getToolTip`, `getEntity` | Common package, emitted by client tooltip paths | Custom Item subclasses were unnecessary. |

`RenderLevelStageEvent` also exposes renderer, matrices, render tick, partial tick, camera, and frustum. R8 uses the event's supplied pose stack and camera and does not reflect into renderer fields. `ItemTooltipEvent#getEntity()` may return `null` during startup, so tooltip generation cannot require a player.

## Trust and side boundaries

- Display snapshots are common immutable data, accepted only after validation.
- Client packet NBT is distinct from persistent machine NBT and is never read back by server gameplay code.
- `Minecraft`, rendering, Screen state, client events, and Ponder scene classes live under `client` and are not referenced by common bootstrap.
- Create/Ponder private implementation and Mixin accessors are not used.
