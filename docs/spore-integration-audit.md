# Spore Integration Audit

R4-B targets Fungal Infection: Spore `2.2.0j`, the exact required runtime dependency declared by Frontier Protocol.

## Audited artifact

- Maven cache artifact: `fungal-infection-spore-678295-8342823.jar`
- SHA-256: `0cdb027eb53e6872bdb8de601dc52c176fcb6ddd33a0a2e20a66392937c3d5fe`
- Main environmental spread interface: `com.Harbinger.Spore.Sentities.FoliageSpread`
- Audit result: 52 classes contain 110 calls to `Level#setBlock` or `Level#setBlockAndUpdate`.

A global block-write hook was rejected because those calls include unrelated entity behavior, item effects, explicit player actions, world generation, and maintenance of existing infection. R4-B is target-based: neither the source passed to `SpreadInfection` nor the base passed to default `SpreadFoliageAndConvert` suppresses an entire operation. Every selected base or offset mutation checks the actual `BlockPos` immediately before writing. A protected base can therefore still produce an allowed mutation at an unprotected neighbor.

## Selected environmental paths

All descriptors below are JVM descriptors from the audited 2.2.0j artifact. These paths run on the logical server; `SporeSuppressionQueries` deliberately fails open for a client `Level`.

| Class | Method and complete descriptor | Caller | Mutation position | Same as base? | Thread | Decision | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `FoliageSpread` | `SpreadInfection(Level,double,BlockPos): (Lnet/minecraft/world/level/Level;DLnet/minecraft/core/BlockPos;)V` | `FungalBonemeal`, `Mound`, `HiveTumor`, `Proto`, `GastGeber` | Dispatches a sphere of target positions and implementation-specific additions | No | Server | Do not hook source | Cancelling here blocks valid targets and unrelated Mound entity effects. |
| `FoliageSpread` | `SpreadFoliageAndConvert(Level,BlockState,BlockPos): (Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V` | `SpreadInfection` loop | Dispatches base and offset helpers | Mixed | Server | Do not hook base | The default method must complete so an unprotected offset remains eligible when its base is protected. |
| `FoliageSpread` | `placeGroundFoliage(BlockState,Level,BlockPos,BlockState): (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V` | `SpreadFoliageAndConvert` | `base.above()` | No | Server | Redirect 1 `setBlock` | Actual target can cross a vertical section boundary and is checked before mutation. |
| `FoliageSpread` | `placeCropsFoliage(Level,BlockPos,BlockState): (Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V` | `SpreadFoliageAndConvert` | `base` | Yes | Server | Redirect 1 `setBlock` | Checks the actual crop replacement target without cancelling sibling helpers. |
| `FoliageSpread` | `placeRottenBush(BlockState,Level,BlockPos,BlockState): (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V` | `SpreadFoliageAndConvert` | `base.above()` | No | Server | Redirect 1 `setBlock` | Checks the actual bush target. |
| `FoliageSpread` | `placeWaterFoliage(BlockState,Level,BlockPos,BlockState): (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V` | `SpreadFoliageAndConvert` | `base.above()` | No | Server | Redirect 2 `setBlock` sites | Covers waterlogged and non-waterlogged branches. |
| `FoliageSpread` | `placeHangingFoliage(BlockState,Level,BlockPos,BlockState): (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V` | `SpreadFoliageAndConvert` | `base.below()` | No | Server | Redirect 2 `setBlock` sites | Covers hanging-property and plain-state branches. |
| `FoliageSpread` | `convertFromJson(Level,BlockState,BlockPos): (Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V` | `SpreadFoliageAndConvert` | `base` | Yes | Server | Redirect 1 `setBlock` | Checks the actual JSON conversion target. |
| `FoliageSpread` | `placeWallFoliage(BlockState,BlockState,BlockState,BlockState,boolean,boolean,boolean,boolean,Level,BlockPos,BlockState): (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;ZZZZLnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V` | `SpreadFoliageAndConvert` | `base.north/south/west/east()` | No | Server | Redirect 4 `setBlock` sites | Each horizontal actual target is independent at chunk boundaries. |
| `FoliageSpread` | `placeBranches(Level,BlockPos,BlockState): (Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V` | `convertWood` | `base.north/south/west/east()` | No | Server | Redirect 4 `setBlock` sites | A protected neighbor is rejected while unprotected neighbors in the same call remain eligible. |
| `FoliageSpread` | `convertBlocks(BlockState,Level,BlockPos): (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V` | `SpreadFoliageAndConvert` | `base` | Yes | Server | Redirect 1 `setBlock` | Checks the actual configured conversion target. |
| `FoliageSpread` | `convertWood(Level,BlockState,BlockPos): (Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V` | `SpreadFoliageAndConvert` | `base`; branch helper uses neighbors | Mixed | Server | Redirect 4 `setBlock`, 3 `FallingBlockEntity.fall`; branch Redirect 4 | Every direct and falling base mutation is checked independently; the method continues to independently guarded neighbors. |
| `HiveTumor` | `SpreadFoliageAndConvert(Level,BlockState,BlockPos): (Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V` | Entity tick spread | Default helper mutations, then one `CDUBlock.replaceCDU(base,level)` | Mixed | Server entity tick | Redirect 1 CDU static call | Default processing is not cancelled; only the protected post-default CDU mutation is skipped. |
| `Proto` | `SpreadFoliageAndConvert(Level,BlockState,BlockPos): (Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V` | Entity tick and death spread | Default helper mutations, then one `CDUBlock.replaceCDU(base,level)` | Mixed | Server entity tick | Redirect 1 CDU static call | Default processing is not cancelled; only the protected post-default CDU mutation is skipped. |
| `Mound` | `placeStructureBlock(Level,BlockPos): (Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V` | `additionPlacers`, `additionIgnoreConfigPlacers` | Method target argument | Yes | Server entity tick | HEAD target check | Protects the dynamic structure target without cancelling Mound's complete spread or entity behavior. |

Redirects name both the complete enclosing method descriptor and the complete invoked descriptor for `Level#setBlock`, `FallingBlockEntity.fall`, or `CDUBlock.replaceCDU`. Their `require` values are the audited call-site counts, so an incompatible Spore update fails during Mixin application instead of silently losing protection.

## Entry-point classification

| Category | Audited examples or path | Existing-state change or new infection | Decision | Reason |
| --- | --- | --- | --- | --- |
| Random tick | `Hand`, `HangingPlant`, `UnderWaterFungusTop`, `RottenBush`, `FungalSaplings`, biomass/acid blocks | Primarily lifecycle or growth of existing Spore blocks | Not adopted | R4-B does not freeze, delete, or repair infection already present in a protected area. |
| Scheduled tick | `OrganiteBlock`, `HiveSpawn`, `BiomassLump` | Existing infection lifecycle | Not adopted | Same existing-infection rule; cancelling these would alter maintenance behavior rather than only prevent entry. |
| Entity tick | `Mound`, `HiveTumor`, `Proto`, `GastGeber` through `SpreadInfection` | New environmental infection | Adopted at each target | Entity behavior continues; only protected mutation targets are rejected. Other entity block effects are not globally intercepted. |
| Block Entity tick | CDU and other Spore block entities | Existing infection state/fuel/activity | Not adopted | Existing infected machinery remains functional; R4-B is not a cleanup or freeze system. |
| Item | `FungalBonemeal#useOn` through `SpreadInfection` | New environmental infection | Adopted at each target | Explicit source cancellation is avoided; protected targets are still blocked by the common target path. Other item effects remain untouched. |
| Projectile | Spore projectile impact paths | Entity damage/effects and specialized block effects | Not adopted | Not part of the audited common foliage/conversion path; a Minecraft-wide block-write hook would be overbroad. |
| Feature | Spore world-generation content | New world-generation content | Not adopted in R4-B | Feature placement is not a runtime `FoliageSpread` mutation and must not be conflated with R4-A ore suppression. |
| Structure | `Mound#placeStructureBlock`; data/world-generation structures | New infection structure or world generation | Mound adopted; world generation not adopted | The Mound runtime target is known and guarded. Generic structure placement is outside this Spore runtime spread path. |
| Command | Spore command handlers | Administrative explicit action | Not adopted | Operator commands are intentional administration, not automatic environmental spread. |
| Player placement | Normal block/item placement | Explicit player action | Not adopted | Protection suppresses automatic infection mutation, not player building or removal. |
| Existing infection state change | Random/scheduled/block-entity ticks and already infected blocks | Existing infection | Not adopted | Protected chunks are not scanned, deleted, replaced, or frozen. |
| New infection generation | `SpreadFoliageAndConvert`, offset helpers, HiveTumor/Proto CDU conversion, Mound additions | New infection | Adopted for the selected runtime spread paths | Every selected mutation is decided from its actual target and current dimension. |

## Test boundary

GameTests use actual Spore classes and deterministic mutation paths where available. `HiveTumor` and `Proto` use their post-default CDU replacement, while `Mound#placeStructureBlock` is reached through a test-only Mixin invoker without widening Spore's production API. Base guards have deterministic crop, configured block, direct wood, falling wood, CDU, and Mound assertions.

Branch boundary cases cover both directions at north, south, west, and east chunk edges: an unprotected base cannot write into a protected neighbor, and a protected base cannot mutate itself but can still write to an outward unprotected neighbor. These cases exercise real `placeBranches` calls through `convertWood`; they are not replaced by direct guard calls. Spore 2.2.0j hard-codes `Math.random() < 0.3` inside each branch direction, so neighbor placement is not completely deterministic and uses a bounded 256-attempt loop. Deterministic base and internal guard assertions run alongside it, and the former 4096-attempt crop-conversion test remains removed.

The R2 initial-spawn test and the R3 real Tier 1 block entity/Create Creative Motor/Stabilization Compound lifecycle test invoke actual Proto CDU conversion. They cover permanent spawn protection, ACTIVE, GRACE_PERIOD, OFFLINE, overlapping sources, partial and complete removal, negative coordinates, and identical Overworld/Nether chunk coordinates.

## Compatibility boundary

Frontier Protocol requires exactly Spore `2.2.0j`. Any dependency update requires repeating this audit and updating descriptors and required call-site counts. Client calls fail open, dedicated-server code references no client-only class, and no Mixin targets Minecraft's global `Level#setBlock` implementation.
