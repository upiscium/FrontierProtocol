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
- Added a common-Cell operating-supply chain designed for Create production and logistics; final physical Mechanical Crafter and continuous Cell-supply smoke tests remain outstanding.
- Added explicit vanilla-texture placeholder models for all three tiers without custom PNG or final art.
- Added Engineer's Goggles diagnostics for all Stabilizer tiers.
- Added targeted, depth-tested chunk-range visualization while using Goggles.
- Added localized Ponder scenes for operation, coverage, and production.
- Added contextual tooltips for Stabilizers, Cells, and Compound.
- Added modpack quest-integration guidance without a quest-mod dependency.
- Stabilizer diagnostics retain and report actual stored Cells when configured capacity is reduced below the existing inventory.
- Corrected Ponder operation guidance to show the selected Tier and Create-logistics Cell insertion instead of direct Stabilizer interaction.

### Compatibility

- Requires Minecraft 1.21.1, Java 21, NeoForge 21.1.235 or newer, Create 6.0.11 or newer, and exactly Spore 2.2.0j.
- Existing infected terrain, structures, nests, hazards, and Block Entities are retained; cleanup removes only explicitly audited non-Block-Entity foliage.
- All three tiers and their common Stabilization Cells use the Create production chain; Stabilization Compound is an intermediate and is not accepted by any Stabilizer.
- Stabilizers do not restore terrain, destroy nests, load chunks, suppress moving contraptions, or add a Block Entity GUI. R8 diagnostics are informational only. Further recipe and operating balance is reserved for R9.
