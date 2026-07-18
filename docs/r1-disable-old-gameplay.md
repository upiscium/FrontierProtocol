# R1 old gameplay disablement

## Baseline

- R0 baseline commit: `76948670646b6d5bb7e446ff776ee3d695638ff2`
- R1 branch: `refactor/r1-disable-old-gameplay`
- R0 remains unchanged on `design/r0-migration-audit`.

## Spore dependency

The supported dependency was verified from both the official CurseForge page
and the downloaded release JAR metadata before old registrations were removed.

- Project: Fungal Infection: Spore
- CurseForge project ID: `678295`
- CurseForge file ID: `8342823`
- File: `spore_1.21.1_2.2.0j_neo.jar`
- Minecraft: `1.21.1`
- Loader: NeoForge
- Mod ID: `spore`
- Mod version: `2.2.0j`
- Spore's minimum NeoForge version: `21.1.212`
- Frontier Protocol NeoForge version: `21.1.235`
- CurseMaven coordinate:
  `curse.maven:fungal-infection-spore-678295:8342823`
- Downloaded JAR SHA-256:
  `0cdb027eb53e6872bdb8de601dc52c176fcb6ddd33a0a2e20a66392937c3d5fe`

Frontier Protocol metadata requires mod ID `spore` at exact version
`[2.2.0j]`. The third-party JAR itself is not committed.

## Pre-removal startup gate

The following checks were completed with Spore on the runtime classpath before
any old gameplay registration was removed:

- Client: started under Xvfb, loaded `Spore 2.2.0j`, reloaded resources with
  `mod/spore`, initialized sound and GUI atlases, and remained running until
  the bounded smoke-test timeout.
- Dedicated server: loaded `com.Harbinger.Spore.Spore`, prepared the existing
  world, and reached `Done` before bounded shutdown.

Spore logs a non-fatal recipe parse error for its Farmers Delight integration
when Farmers Delight is absent. It also logs missing model, sound, subtitle,
and optional Linux narrator warnings on the client. These originate in Spore
or the headless development environment and did not prevent startup.

## Changed files

- `build.gradle`: add the exclusive CurseMaven repository and required Spore
  dependency, expose the pinned version to metadata generation, and omit empty
  legacy resource directories from built JARs.
- `gradle.properties`: pin Spore version `2.2.0j` and CurseForge file ID
  `8342823`.
- `src/main/templates/META-INF/neoforge.mods.toml`: require exact Spore version
  and replace the old sector description.
- `README.md`: replace MDK/sector text with the final Spore/Create scope.
- `src/main/java/dev/upiscium/frontierprotocol/FrontierProtocolMod.java`:
  remove old payload, data-map, capability, attachment, menu, and client-config
  registration.
- `src/main/java/dev/upiscium/frontierprotocol/config/FrontierProtocolServerConfig.java`:
  remove all old gameplay options while retaining the config foundation.
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModBlocks.java`:
  retain an empty registry foundation with no old blocks.
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModItems.java`:
  retain an empty registry foundation with no old items.
- `src/main/java/dev/upiscium/frontierprotocol/registry/ModBlockEntities.java`:
  retain an empty registry foundation with no old Block Entities or
  capabilities.

## Added files

- `src/main/java/dev/upiscium/frontierprotocol/gametest/R1RegistrationGameTests.java`:
  verifies all five old block/item IDs are absent.
- `src/reference/README.md`: defines the non-packaged reference-source policy.
- `src/reference/java/dev/upiscium/frontierprotocol/protection/`: preserved old
  protection query, index, Block, Block Entity, inventory, NBT, fuel, grace,
  and lifecycle implementation.
- `src/reference/java/dev/upiscium/frontierprotocol/data/FuelDefinition.java`
- `src/reference/java/dev/upiscium/frontierprotocol/world/FrontierProtocolWorldData.java`
- `src/reference/test/dev/upiscium/frontierprotocol/protection/BeaconFuelStateTest.java`
- `src/reference/gametest/dev/upiscium/frontierprotocol/gametest/ProtectionGameTests.java`

## Retained production files

- `src/main/java/dev/upiscium/frontierprotocol/protection/ProtectionGeometry.java`
- `src/test/java/dev/upiscium/frontierprotocol/protection/ProtectionGeometryTest.java`
- Java 21, NeoForge, Create, Gradle, JUnit, GameTest, CI, and Nix foundations

## Deleted files

The following old release-source groups were deleted after the startup gate:

- `src/main/java/dev/upiscium/frontierprotocol/FrontierProtocolEvents.java`
- `src/main/java/dev/upiscium/frontierprotocol/breach/`
- `src/main/java/dev/upiscium/frontierprotocol/client/`
- `src/main/java/dev/upiscium/frontierprotocol/command/`
- Old concrete classes under `src/main/java/dev/upiscium/frontierprotocol/data/`
- Old GameTest holders under `src/main/java/dev/upiscium/frontierprotocol/gametest/`
- `src/main/java/dev/upiscium/frontierprotocol/infection/`
- `src/main/java/dev/upiscium/frontierprotocol/mob/`
- `src/main/java/dev/upiscium/frontierprotocol/network/`
- `src/main/java/dev/upiscium/frontierprotocol/nutrition/`
- `src/main/java/dev/upiscium/frontierprotocol/oil/`
- Active old protection classes moved from `src/main` to `src/reference`
- Old tag, attachment, data-map, and menu registry classes
- `src/main/java/dev/upiscium/frontierprotocol/resource/`
- `src/main/java/dev/upiscium/frontierprotocol/sector/`
- Active old world `SavedData` moved from `src/main` to `src/reference`
- Old unit tests except `ProtectionGeometryTest`
- All old Frontier Protocol blockstates, models, language entries, loot tables,
  data maps, content definitions, and gameplay tags under `src/main/resources`
- Old relocation tags under the `c`, `create`, and `simulated` namespaces
- `examples/kubejs/server_scripts/frontier_protocol_compat.js`

The authoritative per-file list is the Git name-status diff from the R0 commit
to the R1 commit.

## R1 scope boundary

R1 does not implement a final stabilizer, Spore mixin, suppression hook,
infection cleanup, or R2 suppression service. It only establishes dependencies,
removes old gameplay exposure, and preserves required reference material.

## Final verification

- `./gradlew clean build`: successful; the remaining
  `ProtectionGeometryTest` passes.
- `./gradlew runGameTestServer`: successful; the one required R1 GameTest
  passes and verifies that all five old block and item IDs are unregistered.
- Final dedicated-server smoke test: Spore 2.2.0j loaded, the pre-existing
  development world opened, and the server reached `Done`.
- Final client smoke test: Spore 2.2.0j loaded under Xvfb, resources included
  `mod/spore`, and sound plus GUI atlases initialized before bounded shutdown.
- Clean JAR inspection: no legacy gameplay classes, JSON data files, assets, or
  reference-source classes are packaged. `ProtectionGeometry` remains present.
- Generated metadata inspection: Create remains required and Spore is required
  with exact version range `[2.2.0j]`.

Known external warning: Spore 2.2.0j includes a Farmers Delight recipe that
logs a parse error when Farmers Delight is absent. Spore also emits missing
asset/subtitle and Linux narrator warnings in the client smoke environment.
These warnings are non-fatal and are not produced by Frontier Protocol content.
