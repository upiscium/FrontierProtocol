# Changelog

All notable changes to Frontier Protocol are documented in this file.

## [0.1.0-alpha.1] - Unreleased

### Added

- A dimension-local, overlap-safe infection suppression API and runtime index.
- Permanent initial-spawn suppression with a persisted initial center and independently configurable enablement and radius.
- Three Create-powered Stabilizer tiers with shared Block/Block Entity implementation and a shared consumable, RPM, ACTIVE, grace-period, and offline state machine.
- Initial-spawn ore-generation suppression for normal, scattered, and provisional spawn-search paths.
- Target-based integration with audited Spore 2.2.0j foliage, conversion, CDU, and Mound mutation paths.
- Generic tier-and-position `STABILIZER` suppression sources covering 1x1, 3x3, and 5x5 chunk areas.
- Budgeted, loaded-chunk-only cleanup of audited removable Spore foliage while a Stabilizer is ACTIVE, with per-source tier profiles, hard global caps, persisted cursors, and overlap-safe pause/resume behavior.
- Integration GameTests for spawn protection, all three Stabilizer lifecycles and cleanup profiles, dimensions, overlaps, negative coordinates, and Spore-specific mutations.
- Added Stabilization Cell as the common finished Stabilizer consumable.
- Changed Stabilization Compound into a production intermediate.
- Added exactly five Create Mixing, Deploying, and Mechanical Crafting recipes, including staged Tier 1-to-2 and Tier 2-to-3 upgrades with no crafting bypass.
- Added a common-Cell operating-supply chain verified with physical Chest/Chute delivery through two default-duration Tier 2 and Tier 3 Cell rollovers.
- Added explicit vanilla-texture placeholder models for all three tiers without custom PNG or final art.
- Added Engineer's Goggles diagnostics for all Stabilizer tiers.
- Added targeted, depth-tested chunk-range visualization while using Goggles.
- Added localized Ponder scenes for operation, coverage, and production.
- Added contextual tooltips for Stabilizers, Cells, and Compound.
- Added modpack quest-integration guidance without a quest-mod dependency.
- Stabilizer diagnostics retain and report actual stored Cells when configured capacity is reduced below the existing inventory.
- Corrected Ponder operation guidance to show the selected Tier and Create-logistics Cell insertion instead of direct Stabilizer interaction.
- Corrected GRACE_PERIOD diagnostics so loss of rotation reports the active grace state instead of a misleading no-rotation failure.
- Populated and reframed the committed Ponder schematics, corrected their 1-based localization keys, and aligned Japanese Tier and lifecycle wording.
- Replaced the Spore-derived heated Compound recipe with unheated Mixing of 100 mB TFMG Liquid Plastic, Sand, Blue Ice, and eight Iron Nuggets for one Compound.
- Made Create: The Factory Must Grow 1.2.0 or newer a required client and server dependency.
- Added visible Mixer, Basin, fluid supply, Deployer, Depot, and Mechanical Crafter equipment to Production Ponder.
- Changed Grace into a finite per-Cell suppression-continuity budget that is replenished only when a new Cell is consumed, blocking recovery and power-cycle refills.
- Changed Tier 1/2/3 Grace defaults from 6000/9000/12000 to 1200/1800/2400 ticks and clamp saved excess Grace to the configured limit.
- Added physical unheated Mixing assertions and Tier 1/2/3 Mechanical Crafter execution tests, including exact output and upgrade-consumption checks.
- Added the remaining per-Cell Grace reserve to the ACTIVE Engineer's Goggles display in English and Japanese.
- Verified backup-Cell rollover, insufficient-RPM and Overstressed Grace behavior, client reconnect synchronization, and dedicated-server lifecycle persistence.
- Replaced the Stabilization Compound and Stabilization Cell vanilla placeholder icons with custom 32x32 item textures.
- Added automated validation for final item models, PNG properties, visual distinction, and production-JAR packaging.
- Strengthened pull-request CI with unit and asset tests, datagen clean-diff enforcement, clean build, GameTests, and a short-lived internal alpha JAR artifact.
- Replaced the Tier 1 Stabilizer placeholder with a custom directional model and 32x32 RGBA block textures for OFFLINE, ACTIVE, and GRACE_PERIOD.
- Added Tier 1 horizontal facing with front-facing placement and rear-only Create shaft input while keeping Tier 2 and Tier 3 orientation behavior unchanged.
- Added client-only Tier 1 core, gear, and full-bright status-light rendering. ACTIVE rotates and pulses according to signed kinetic speed, GRACE_PERIOD accelerates its warning blink as reserve falls, and OFFLINE uses a dim red light.
- Added Grace duration to display schema v2 so clients derive warning cadence from the server-authoritative remaining-to-total ratio.
- Added automated validation for all 12 Tier 1 facing/status variants, final models and textures, animation behavior, orientation invariants, display schema v2, and production-JAR packaging.
- Corrected the Tier 1 front gear to rotate around its local Z axis and made positive and negative angle interpolation continuous across full turns.
- Rebuilt the Tier 1 body around four real window apertures with one central 3D core, synchronized front/left/right/top status lights, an eight-tooth front gear, and a rear bearing housing.
- Added Tier 1-only non-occluding rendering while preserving full-cube collision, and added a dedicated Tier 1 block codec that reconstructs the directional subclass.
- Removed three unused dynamic block-model JSON files; Java `ModelPart` geometry is now the sole dynamic-model definition.

### Compatibility

- Requires Minecraft 1.21.1, Java 21, NeoForge 21.1.235 or newer, Create 6.0.11 or newer, and exactly Spore 2.2.0j.
- Existing infected terrain, structures, nests, hazards, and Block Entities are retained; cleanup removes only explicitly audited non-Block-Entity foliage.
- All three tiers and their common Stabilization Cells use the Create production chain; Stabilization Compound is an intermediate and is not accepted by any Stabilizer.
- Stabilizers do not restore terrain, destroy nests, load chunks, suppress moving contraptions, or add a Block Entity GUI. Existing world serverconfig values are retained and must be updated manually to adopt new R9 defaults.
- Existing placed Tier 1 Stabilizers may need to be broken and replaced to select their intended front and rear after the directional block-state change. Tier 2 and Tier 3 retain their previous block states and placeholder assets.
