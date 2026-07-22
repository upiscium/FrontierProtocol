# Initial Spawn Ore Suppression

R4-A suppresses `OreFeature` and `ScatteredOreFeature` placement in Overworld chunks around the immutable initial spawn center stored by `SpawnProtectionSavedData`.

## Thread boundary

Server-thread lifecycle code publishes an immutable `InitialSpawnOreSuppressionSnapshot` into a per-`MinecraftServer` concurrent registry. World-generation workers only read that snapshot. They do not access SavedData, the infection suppression index, or synchronous server tasks.

## Fresh-world ordering

Vanilla selects a safe spawn by requesting `FULL` chunks during `MinecraftServer#setInitialSpawn`; `FULL` includes the `FEATURES` stage. There is no event after the final safe position is known but before this search generates features. The initial-spawn Mixin therefore publishes a conservative provisional snapshot when vanilla writes its first candidate spawn. Its radius is the configured radius plus vanilla's five-chunk search extent. At method return, the final SavedData center is initialized and the snapshot is narrowed to the configured radius. This prevents ores in the final protected area while preserving vanilla spawn selection, at the cost of potentially suppressing ores in additional search-area chunks on first world creation.

Existing worlds publish their final snapshot during Overworld load. Config reload rebuilds infection protection and ore suppression in the same server-thread task. Server stop clears the per-server snapshot.

## Placement policy

Suppression uses the configured feature origin chunk and cancels the whole feature placement. Veins originating outside can cross into the protected area, and protected origins can otherwise have extended outside. Per-block filtering is intentionally not used to preserve deterministic worldgen and mod compatibility.

Mods using vanilla `OreFeature` or `ScatteredOreFeature` are covered automatically. Custom ore features must call `OreGenerationSuppressionApi.isSuppressed(WorldGenLevel, BlockPos)` before mutation.
