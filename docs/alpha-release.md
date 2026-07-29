# Frontier Protocol 0.1.0-alpha.1

## Candidate status

This is an internal alpha release candidate of the rebuilt Frontier Protocol design. It is intended for internal compatibility testing and integration validation. Worlds should be backed up before testing, and upgrading Spore independently is unsupported until its mutation paths are re-audited.

The candidate artifact produced by the build is `frontier_protocol-0.1.0-alpha.1.jar`. It has not been approved for public distribution. Frontier Protocol remains **All Rights Reserved**.

## Publication status

Public distribution of 0.1.0-alpha.1 has been deferred.
This version is currently used for internal testing and integration validation only.

The accepted Stabilizer consumable model is recorded in [ADR 0001](adr/0001-stabilizer-consumable-and-production-model.md), with the current TFMG Compound materials in [ADR 0004](adr/0004-tfmg-compound-production.md). The common tier architecture is recorded in [ADR 0002](adr/0002-common-stabilizer-tier-architecture.md), and the finite Grace contract in [ADR 0005](adr/0005-per-cell-grace-budget.md). Operational display architecture is recorded in [ADR 0003](adr/0003-operational-display-and-client-visualization.md), with current behavior documented in [R9 Balance Hardening](r9-balance-hardening.md).

## Compatibility matrix

| Component | Supported version |
| --- | --- |
| Minecraft | `1.21.1` |
| Java | `21` |
| NeoForge | `21.1.235` or newer for Minecraft 1.21.1 |
| Create | `6.0.11` or newer |
| Create: The Factory Must Grow | `1.2.0` or newer |
| Fungal Infection: Spore | exactly `2.2.0j` |

Spore 2.2.0j is the artifact audited by SHA-256 in `docs/spore-integration-audit.md`. The Mixins use complete descriptors and required call-site counts so an incompatible implementation fails at startup instead of silently bypassing suppression.

## Included behavior

- The persisted initial Overworld spawn center supplies permanent suppression, using a configurable radius of two chunks by default.
- Fresh-world spawn search publishes a provisional center before normal ore placement and replaces it with the final center when spawn selection completes.
- Tier 1, Tier 2, and Tier 3 use a real Create kinetic network and the same finished Stabilization Cell. Their default coverage is 1x1, 3x3, and 5x5 chunks. A machine consumes one Cell when beginning an ACTIVE duration and grants one finite Grace budget. ACTIVE and recovery do not refill spent Grace; only a new Cell does.
- All three Registry entries use one shared `StabilizerBlock`, one shared `StabilizerBlockEntity` class/type, and one state machine. Generic `STABILIZER` source IDs include tier and position as `stabilizer/<tier>/<x>_<y>_<z>`.
- While ACTIVE, each source incrementally removes audited removable Spore foliage from loaded covered chunks under its tier cleanup profile and hard server-global caps. Cleanup pauses during grace and resumes from persisted progress after power recovery or reload.
- Multiple sources can cover the same chunk without premature removal, and identical chunk coordinates remain independent across dimensions.
- Audited Spore environmental spread, offset foliage and branch writes, configured conversion, falling wood conversion, HiveTumor/Proto CDU replacement, and Mound additions query the actual mutation target before writing.
- Stabilization Compound is produced without heat by Mixing 100 mB TFMG Liquid Plastic, Sand, Blue Ice, and eight Iron Nuggets into one intermediate. A Deployer seals it into the common Cell. Mechanical Crafters produce Tier 1 and perform staged Tier 1-to-2 and Tier 2-to-3 upgrades. These are exactly five recipes with no normal-crafting bypass; exact upgrade patterns are in [R7 Stabilizer Tiers](r7-stabilizer-tiers.md).
- Engineer's Goggles expose server-authoritative Stabilizer operation, Shift details, and a targeted chunk-range overlay. Static item tooltips and localized Ponder scenes explain operation, coverage, production, physical-defense limits, and initial-spawn protection.
- Reducing configured Cell capacity does not delete existing inventory. Operational snapshots continue to report the actual over-capacity buffer, such as `32 / 8`, until Cells are consumed normally.
- Ponder Operation scenes show the selected Stabilizer Tier and represent Cell insertion through Create logistics. Stabilizers do not support direct right-click Cell insertion.

At defaults, Tier 1/2/3 require 32/64/128 absolute RPM, impose 16/64/256 Stress, hold 8/32/64 Cells, run 6000/3000/2000 ticks per Cell, and receive per-Cell Grace budgets of 1200/1800/2400 ticks. Their 1/9/25-chunk coverage yields 6000/27000/50000 protected chunk-ticks per Cell. Full buffers provide 40 minutes, 80 minutes, and 106 minutes 40 seconds. Existing world serverconfig values remain unchanged until an operator updates the three Grace keys manually.

## Alpha boundaries

- Existing infected terrain, structures, nests, active hazards, and Block Entities are not restored, removed, replaced, or frozen. Cleanup is an explicit allowlist of audited non-Block-Entity foliage and replaces it only with air or retained water.
- All tiers and Stabilization Cells have Create production recipes. Compound cannot power a Stabilizer directly; the bundled consumable tag accepts only Cells, while datapacks may explicitly extend that public tag. The common consumable decision is recorded in [ADR 0001](adr/0001-stabilizer-consumable-and-production-model.md), and the shared tier architecture in [ADR 0002](adr/0002-common-stabilizer-tier-architecture.md).
- R9 fixes the current recipe quantities and operating values for this internal candidate. Future changes require a new balance decision.
- Stabilization Compound and Stabilization Cell have final custom 32x32 RGBA item art. Tier 1, Tier 2, and Tier 3 use final static Copper, Andesite, and Brass Create-casing machine art; see [Asset Status](placeholder-assets.md), [R10 Final Item Assets](r10-item-assets.md), and [R11 Stabilizer Casing Redesign](r11-stabilizer-casing-redesign.md).
- Hostile mob movement, combat, block breaking, and explosions are not containment responsibilities.
- Spore random ticks, scheduled ticks, and existing infected block-entity state continue normally.
- World-generation features outside the selected runtime Spore spread paths are not globally intercepted.
- R9 adds no Block Entity GUI, persistent HUD, particles, custom creative tab, tier-specific Cells, Empty/Spent Cells or returns, new custom materials, multiblock, moving-contraption suppression, chunk loading, terrain restoration, or nest or mob handling. R10 changes Compound and Cell item art. R11 replaces block placeholders with static models and adds the three Block Items to the vanilla Functional Blocks tab without adding gameplay scope.
- No Minecraft-wide `Level#setBlock` hook is used.

## Verification status

Current automated verification includes passing unit tests and all 40 GameTests. RecipeManager validation covers the exact five serializers, ingredients, patterns, outputs, and absence of crafting bypasses. Physical tests additionally cover a Basin with no heat source and heat level `NONE`, Tier 1/2/3 Mechanical Crafter execution, prior-tier consumption, two default-duration Tier 2/Tier 3 Cell rollovers through Chest/Chute logistics without ACTIVE or suppression interruption, and rear-versus-front kinetic attachment for all three directional Stabilizers.

R9 verification launched the production dedicated server with the manually updated development Grace values. Create, TFMG, Spore, and Frontier Protocol loaded, 3593 recipes were accepted, and the server reached `Done` without config, Mixin, or client-class errors. The server Gradle task did not receive a normal console `stop`, so its clean task exit remains unverified.

The R8 production client command joined the creative smoke world under a 1920x1080 Xvfb display and completed the broad English/Japanese graphical checks in [R8 Graphical Verification](r8-graphical-verification.md). The focused R9 Tier 1 Grace lifecycle also passed there with Engineer's Goggles: ACTIVE entered GRACE_PERIOD after rotation loss, the displayed reserve decreased, rotation recovery preserved the partial reserve, a second outage resumed from that amount, exhaustion reached OFFLINE, and the next consumed Cell restored the configured maximum. No raw translation key appeared.

Extended R9 manual verification also passed for insufficient RPM, Overstressed operation, exact single-Cell backup rollover without leaving ACTIVE, no-backup fallback according to the remaining Grace reserve, client reconnect synchronization, and normal dedicated-server restart persistence of status, Cell time, and Grace.

R10 final item-art verification passed for Compound and Cell in Japanese at GUI Scale Auto and English at GUI Scale 2. Creative inventory, tooltips, JEI production views, Ponder controls, held, dropped, and Item Frame rendering showed distinct icons without a Frontier Protocol missing texture or atlas error. Automated tests also verify PNG quality, exact model references, and production-JAR packaging.

R11 casing verification passed for all three tiers and all four facings. The static OFFLINE red, ACTIVE green, and GRACE_PERIOD yellow top LEDs differ only inside the LED region. Front control plates, rear decorative pipes, dual-side shaft bearings, direct Create casing bottoms, Create equipment adjacency, inventory contexts, JEI, and all three Ponder scenes passed in English at GUI Scale 2 with Fast graphics and Japanese at GUI Scale Auto with Fancy graphics. With default `FACING=EAST`, physical NORTH/SOUTH motors supplied +16/-16 RPM while EAST/WEST motors supplied 0 RPM. A top-face Create wrench rotation disconnected the old NORTH network and accepted -16 RPM from the new EAST side. Automated tests verify the `FACING + STATUS` state schema, perpendicular derived rotation axis, wrench path, 36 variants, one full-cube parent, nine state models, three item models, 18 original textures, direct Create references, and production-JAR packaging.

Final asset status: Tier 1 FINAL; Tier 2 FINAL; Tier 3 FINAL; Compound FINAL; Cell FINAL. Asset completion does not by itself make the alpha approved for public distribution.

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
- [x] Re-inspect the revised Compound and unchanged Cell recipes in JEI.
- [x] Verify revised Production Ponder equipment in English and Japanese.
- [x] Execute the tier recipes in physical Mechanical Crafters.
- [x] Smoke-test continuous automated Cell supply at upper-tier consumption intervals.
- [x] Complete the focused R9 client Grace lifecycle smoke.
- [x] Verify client reconnect and dedicated-server lifecycle persistence.
- [x] Inspect the production JAR contents and generated metadata.
- Set the actual release date.
- Prepare an icon or screenshots only if required by the selected channel.
