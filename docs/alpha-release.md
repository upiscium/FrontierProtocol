# Frontier Protocol 0.1.0-alpha.1

## Candidate status

This is an internal alpha release candidate of the rebuilt Frontier Protocol design. It is intended for internal compatibility testing and integration validation. Worlds should be backed up before testing, and upgrading Spore independently is unsupported until its mutation paths are re-audited.

The candidate artifact produced by the build is `frontier_protocol-0.1.0-alpha.1.jar`. It has not been approved for public distribution. Frontier Protocol remains **All Rights Reserved**.

## Publication status

Public distribution of 0.1.0-alpha.1 has been deferred.
This version is currently used for internal testing and integration validation only.

## Compatibility matrix

| Component | Supported version |
| --- | --- |
| Minecraft | `1.21.1` |
| Java | `21` |
| NeoForge | `21.1.235` or newer for Minecraft 1.21.1 |
| Create | `6.0.11` or newer |
| Fungal Infection: Spore | exactly `2.2.0j` |

Spore 2.2.0j is the artifact audited by SHA-256 in `docs/spore-integration-audit.md`. The Mixins use complete descriptors and required call-site counts so an incompatible implementation fails at startup instead of silently bypassing suppression.

## Included behavior

- The persisted initial Overworld spawn center supplies permanent suppression, using a configurable radius of two chunks by default.
- Fresh-world spawn search publishes a provisional center before normal ore placement and replaces it with the final center when spawn selection completes.
- The Tier 1 Stabilizer uses a real Create kinetic network and Stabilization Compound. It suppresses its chunk while ACTIVE and during its configured grace period, then unregisters when OFFLINE, unloaded, removed, or destroyed.
- While ACTIVE, Tier 1 incrementally removes audited removable Spore foliage from its loaded placement chunk under global and per-source budgets. Cleanup pauses during grace and resumes from its persisted cursor after power recovery or reload.
- Multiple sources can cover the same chunk without premature removal, and identical chunk coordinates remain independent across dimensions.
- Audited Spore environmental spread, offset foliage and branch writes, configured conversion, falling wood conversion, HiveTumor/Proto CDU replacement, and Mound additions query the actual mutation target before writing.

## Alpha boundaries

- Existing infected terrain, structures, nests, active hazards, and Block Entities are not restored, removed, replaced, or frozen. Cleanup is an explicit allowlist of audited non-Block-Entity foliage and replaces it only with air or retained water.
- Tier 1 and Stabilization Compound have no survival recipes or creative-tab entries in this alpha. Use `/give @s frontier_protocol:tier_1_stabilizer` and `/give @s frontier_protocol:stabilization_compound` for testing.
- Hostile mob movement, combat, block breaking, and explosions are not containment responsibilities.
- Spore random ticks, scheduled ticks, and existing infected block-entity state continue normally.
- World-generation features outside the selected runtime Spore spread paths are not globally intercepted.
- Tier 2, Tier 3, terrain restoration, nest destruction, goggles information, containment UI, and Ponder scenes are not included.
- No Minecraft-wide `Level#setBlock` hook is used.

## Release verification

The release candidate must pass:

```sh
./gradlew clean build
./gradlew runGameTestServer
./gradlew runServer
./gradlew runClient
```

The built JAR must contain `META-INF/neoforge.mods.toml`, `frontier_protocol.mixins.json`, and the Spore integration classes. A production NeoForge dedicated server smoke test should load the built JAR with Create and Spore and reach the server-ready state without a Mixin application or client-class error.

`runClient` requires a graphical display. A `glfwInit` failure when `DISPLAY` is unavailable is an environment limitation and does not constitute a successful client smoke test.

## Future publication checklist

- Decide the distribution channel.
- Confirm the license and distribution terms.
- Add any required `LICENSE` document before publication.
- Confirm the release branch and default branch.
- Run a clean build.
- Run the complete GameTest suite.
- Complete a dedicated-server smoke test.
- Complete a client smoke test with a graphical display.
- Inspect the production JAR contents and generated metadata.
- Set the actual release date.
- Prepare an icon or screenshots only if required by the selected channel.
