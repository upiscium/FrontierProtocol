# Spore Integration Audit

R4-B targets Fungal Infection: Spore `2.2.0j`, the exact required runtime dependency declared by Frontier Protocol.

## Audited artifact

- Maven cache artifact: `fungal-infection-spore-678295-8342823.jar`
- SHA-256: `0cdb027eb53e6872bdb8de601dc52c176fcb6ddd33a0a2e20a66392937c3d5fe`
- Main environmental spread interface: `com.Harbinger.Spore.Sentities.FoliageSpread`

The artifact audit found 52 classes containing 110 calls to `Level#setBlock` or `Level#setBlockAndUpdate`. A global block-write hook was rejected because those calls also include unrelated entity behavior, item effects, explicit player actions, and existing-infection maintenance.

## Selected paths

`FoliageSpread#SpreadInfection(Level, double, BlockPos)` is the common entry point used by fungal bonemeal, mounds, hive tumors, protos, and GastGeber. R4-B checks its source position so a spread operation originating in a suppressed chunk cannot run its preliminary placers.

`FoliageSpread#SpreadFoliageAndConvert(Level, BlockState, BlockPos)` receives every position visited by the normal spherical spread loop. R4-B checks that target before Spore reads neighbors or performs conversion, allowing an unsuppressed source to spread normally while skipping targets in suppressed chunks.

`HiveTumor` and `Proto` override `SpreadFoliageAndConvert`, invoke the interface default, and then perform an additional CDU replacement. Their overrides are checked separately at method entry so cancellation of the default method cannot fall through to that extra mutation.

`Mound#additionPlacers` and `additionIgnoreConfigPlacers` can call the private `placeStructureBlock(Level, BlockPos)` outside the normal target loop. R4-B checks that final structure target separately.

All hooks are Spore-class-specific and use `InfectionSuppressionApi` on the Minecraft server thread. Client-side calls fail open. The integration prevents new mutations only; it does not scan for or remove existing Spore blocks.

## Compatibility boundary

The Mixins deliberately require the audited method names and descriptors. Frontier Protocol already requires exactly Spore `2.2.0j`, so a changed Spore implementation must be re-audited before updating that dependency. Infection mechanisms outside the selected environmental foliage/conversion path are not intercepted by a broad `Level#setBlock` hook.
