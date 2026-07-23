# Changelog

All notable changes to Frontier Protocol are documented in this file.

## [0.1.0-alpha.1] - Unreleased

### Added

- A dimension-local, overlap-safe infection suppression API and runtime index.
- Permanent initial-spawn suppression with a persisted initial center and independently configurable enablement and radius.
- A Create-powered Tier 1 Stabilizer with consumable, RPM, ACTIVE, grace-period, and offline states.
- Initial-spawn ore-generation suppression for normal, scattered, and provisional spawn-search paths.
- Target-based integration with audited Spore 2.2.0j foliage, conversion, CDU, and Mound mutation paths.
- Budgeted, loaded-chunk-only cleanup of audited removable Spore foliage while Tier 1 is ACTIVE, with persisted cursors and overlap-safe pause/resume behavior.
- Integration GameTests for spawn protection, Tier 1 lifecycle and cleanup, dimensions, overlaps, negative coordinates, and Spore-specific mutations.
- Added Stabilization Cell as the common finished Stabilizer consumable.
- Changed Stabilization Compound into a production intermediate.
- Added Create Mixing, Deploying, and Mechanical Crafting production recipes.
- Added a fully automatable Tier 1 operating-supply chain.

### Compatibility

- Requires Minecraft 1.21.1, Java 21, NeoForge 21.1.235 or newer, Create 6.0.11 or newer, and exactly Spore 2.2.0j.
- Existing infected terrain, structures, nests, hazards, and Block Entities are retained; cleanup removes only explicitly audited non-Block-Entity foliage.
- Tier 1 and Stabilization Cells are produced through the minimal Create production chain; Stabilization Compound is an intermediate and is not accepted by the Stabilizer.
- Tier 2, Tier 3, terrain restoration, nest destruction, and containment UI are not included in this alpha.
