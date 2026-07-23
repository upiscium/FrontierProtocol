# Changelog

All notable changes to Frontier Protocol are documented in this file.

## [0.1.0-alpha.1] - Unreleased

### Added

- A dimension-local, overlap-safe infection suppression API and runtime index.
- Permanent initial-spawn suppression with a persisted initial center and independently configurable enablement and radius.
- A Create-powered Tier 1 Stabilizer with consumable, RPM, ACTIVE, grace-period, and offline states.
- Initial-spawn ore-generation suppression for normal, scattered, and provisional spawn-search paths.
- Target-based integration with audited Spore 2.2.0j foliage, conversion, CDU, and Mound mutation paths.
- Integration GameTests for spawn protection, Tier 1 lifecycle, dimensions, overlaps, negative coordinates, and Spore-specific mutations.

### Compatibility

- Requires Minecraft 1.21.1, Java 21, NeoForge 21.1.235 or newer, Create 6.0.11 or newer, and exactly Spore 2.2.0j.
- Existing Spore infection is retained; suppression applies only to selected new mutations.
- Tier 1 and Stabilization Compound are command-only in this alpha; survival recipes and the owned Create production chain are not included.
- Tier 2, Tier 3, infection cleanup, and containment UI are not included in this alpha.
