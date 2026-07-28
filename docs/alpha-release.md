# Frontier Protocol 0.1.0-alpha.1

## Candidate status

This is an internal alpha release candidate of the rebuilt Frontier Protocol design. It is intended for internal compatibility testing and integration validation. Worlds should be backed up before testing, and upgrading Spore independently is unsupported until its mutation paths are re-audited.

The candidate artifact produced by the build is `frontier_protocol-0.1.0-alpha.1.jar`. It has not been approved for public distribution. Frontier Protocol remains **All Rights Reserved**.

## Publication status

Public distribution of 0.1.0-alpha.1 has been deferred.
This version is currently used for internal testing and integration validation only.

The accepted Stabilizer consumable and production model is recorded in [ADR 0001](adr/0001-stabilizer-consumable-and-production-model.md). The accepted common tier architecture is recorded in [ADR 0002](adr/0002-common-stabilizer-tier-architecture.md). Operational display architecture is recorded in [ADR 0003](adr/0003-operational-display-and-client-visualization.md), with current behavior documented in [R7 Stabilizer Tiers](r7-stabilizer-tiers.md) and [R8 Operational UX](r8-operational-ux.md).

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
- Tier 1, Tier 2, and Tier 3 use a real Create kinetic network and the same finished Stabilization Cell. Their default coverage is 1x1, 3x3, and 5x5 chunks. A machine consumes one Cell when beginning an ACTIVE duration, retains suppression during its configured grace period, then unregisters when OFFLINE, unloaded, removed, or destroyed.
- All three Registry entries use one shared `StabilizerBlock`, one shared `StabilizerBlockEntity` class/type, and one state machine. Generic `STABILIZER` source IDs include tier and position as `stabilizer/<tier>/<x>_<y>_<z>`.
- While ACTIVE, each source incrementally removes audited removable Spore foliage from loaded covered chunks under its tier cleanup profile and hard server-global caps. Cleanup pauses during grace and resumes from persisted progress after power recovery or reload.
- Multiple sources can cover the same chunk without premature removal, and identical chunk coordinates remain independent across dimensions.
- Audited Spore environmental spread, offset foliage and branch writes, configured conversion, falling wood conversion, HiveTumor/Proto CDU replacement, and Mound additions query the actual mutation target before writing.
- Stabilization Compound is produced by heated Create Mixing and is only an intermediate. A Deployer seals it into the common Cell. Mechanical Crafters produce Tier 1 and perform staged Tier 1-to-2 and Tier 2-to-3 upgrades. These are exactly five recipes with no normal-crafting bypass; exact upgrade patterns are in [R7 Stabilizer Tiers](r7-stabilizer-tiers.md).
- Engineer's Goggles expose server-authoritative Stabilizer operation, Shift details, and a targeted chunk-range overlay. Static item tooltips and localized Ponder scenes explain operation, coverage, production, physical-defense limits, and initial-spawn protection.
- Reducing configured Cell capacity does not delete existing inventory. Operational snapshots continue to report the actual over-capacity buffer, such as `32 / 8`, until Cells are consumed normally.
- Ponder Operation scenes show the selected Stabilizer Tier and represent Cell insertion through Create logistics. Stabilizers do not support direct right-click Cell insertion.

At defaults, Tier 1/2/3 require 32/64/128 absolute RPM, impose 16/64/256 Stress, hold 8/32/64 Cells, run 6000/3000/2000 ticks per Cell, and retain grace for 6000/9000/12000 ticks. Their 1/9/25-chunk coverage yields 6000/27000/50000 protected chunk-ticks per Cell. Tier 2 and Tier 3 are more area-efficient but consume Cells at shorter machine intervals, so their supply logistics need faster delivery and adequate buffers. The complete cleanup values and recipes are documented in [R7 Stabilizer Tiers](r7-stabilizer-tiers.md).

## Alpha boundaries

- Existing infected terrain, structures, nests, active hazards, and Block Entities are not restored, removed, replaced, or frozen. Cleanup is an explicit allowlist of audited non-Block-Entity foliage and replaces it only with air or retained water.
- All tiers and Stabilization Cells have Create production recipes. Compound cannot power a Stabilizer directly; the bundled consumable tag accepts only Cells, while datapacks may explicitly extend that public tag. The common consumable decision is recorded in [ADR 0001](adr/0001-stabilizer-consumable-and-production-model.md), and the shared tier architecture in [ADR 0002](adr/0002-common-stabilizer-tier-architecture.md).
- Recipe quantities and operating values are provisional and may change during R9 balancing.
- All three Stabilizer tiers, Compound, and Cell use explicit placeholder models referencing exact vanilla texture families. No custom PNG or final art is included; see [Placeholder Assets](placeholder-assets.md).
- Hostile mob movement, combat, block breaking, and explosions are not containment responsibilities.
- Spore random ticks, scheduled ticks, and existing infected block-entity state continue normally.
- World-generation features outside the selected runtime Spore spread paths are not globally intercepted.
- R8 adds no Block Entity GUI, persistent HUD, particles, custom creative tab, tier-specific Cells, Empty/Spent Cells or returns, new custom materials, TFMG integration, multiblock, moving-contraption suppression, chunk loading, terrain restoration, nest or mob handling, or final assets. R9 balancing remains later work.
- No Minecraft-wide `Level#setBlock` hook is used.

## Verification status

Automated verification includes passing unit tests and all 34 GameTests, including the R8 display snapshot, client-packet boundary, all tier lifecycles, coverage, suppression, cleanup profiles/global caps, persistence, overlaps, and production contracts. RecipeManager validation continues to cover the exact five serializers, ingredients, patterns, outputs, and absence of crafting bypasses. `runData` produced no repository changes, and the final clean build produced `frontier_protocol-0.1.0-alpha.1.jar` with the required metadata, Mixins, Spore integration classes, and Ponder schematics.

Final R8 verification launched the production dedicated server through readiness without Frontier Protocol client-class errors and confirmed shutdown-hook saving for all dimensions. The server Gradle task did not receive a normal console `stop` through the smoke harness, so its clean task exit remains unverified.

The production client command joined the creative smoke world under a 1920x1080 Xvfb display. English and Japanese interaction checks passed for live Tier displays and diagnostics, Shift details, over-capacity reporting, client-option toggles, Goggle removal and crosshair cleanup, range overlays at all three sizes and negative coordinates, item tooltips, and complete Operation/Coverage/Production Ponder playback. The Overstressed diagnostic remains unit-tested but was not reproduced with a graphical kinetic fixture. See [R8 Graphical Verification](r8-graphical-verification.md). Physical Mechanical Crafter execution for the Tier 2 and Tier 3 upgrades and continuous automated Cell-supply smoke testing also remain unverified.

The built JAR must contain `META-INF/neoforge.mods.toml`, `frontier_protocol.mixins.json`, and the Spore integration classes. The dedicated-server smoke reached the server-ready state with Create and Spore without a Mixin application or client-class error. The client smoke used a graphical display; a `glfwInit` failure when `DISPLAY` is unavailable would not constitute a successful client smoke test.

## Future publication checklist

- Decide the distribution channel.
- Confirm the license and distribution terms.
- Add any required `LICENSE` document before publication.
- Confirm the release branch and default branch.
- [x] Run a clean build.
- [x] Run the complete GameTest suite.
- [x] Complete a dedicated-server smoke test.
- [x] Complete the pre-R8 title-screen, world, model, and JEI client smoke.
- [x] Complete the R8 Goggles, range, tooltip, Ponder playback, and Japanese client smoke.
- [x] Inspect all five recipes in JEI.
- [ ] Execute the tier recipes in physical Mechanical Crafters.
- [ ] Smoke-test continuous automated Cell supply at upper-tier consumption intervals.
- [x] Inspect the production JAR contents and generated metadata.
- Set the actual release date.
- Prepare an icon or screenshots only if required by the selected channel.
