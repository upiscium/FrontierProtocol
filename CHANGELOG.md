# Changelog

All notable changes to Frontier Protocol are documented in this file.

## [0.1.0] - Unreleased

This is the first internal Stable candidate. Stable `0.1.0` is not published.
Exact-main automated evidence and English/Japanese graphical smoke remain later
work before publication.

### Compatibility

- Gameplay scope is unchanged from the accepted `0.1.0-rc.1` prerelease.
- Balance defaults, registry IDs, and persistence schemas are unchanged.
- Minecraft, Java, NeoForge, Create, TFMG, and Spore dependency/platform
  versions are unchanged.

## [0.1.0-rc.1] - 2026-07-31

Published as a public GitHub prerelease from source commit
`5fe7b0f9931560747af7c9c25df367c1e4db9014`. It is the latest public
prerelease, not a Stable release.

### Added

- Distinct Alpha, RC, and Stable internal/public release classifications.
- Tagged RC/Stable reruns of the R19 startup and R20 Alpha-world migration matrices.
- A retained minimum-120-minute RC log-volume and persistence soak path, plus an explicit short infrastructure-validation mode.
- A read-only manual workflow for later evidence collection from an exact merged RC or Stable candidate.
- Accepted retained 120-minute RC soak evidence for the exact published source.
- Accepted the public RC Release and immutable four-asset set.
- Accepted public-URL Packwiz add, refresh, metadata hash, and JAR hash verification.

### Compatibility

- Gameplay, balance defaults, registry IDs, persistence schemas, Minecraft, Java, NeoForge, Create, TFMG, and Spore versions are unchanged from `0.1.0-alpha.1`.
- Stable compatibility guarantees begin only when final `0.1.0` is published.

## [0.1.0-alpha.1] - 2026-07-30

Previously published as a public GitHub prerelease from source commit
`fed467ec0cd52a936f06751cd922efcc259914a1`. This is not a stable release.
It remains available as the previous public prerelease while unavailable Stable
`0.1.0` is the current release objective.

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
- Superseded the unmerged open-frame animated Stabilizer prototype with static full-cube Create-casing machines for all three tiers.
- Added shared horizontal facing and dual-side Create shaft input while preserving each tier in the existing shared Block and tier-aware Codec; the perpendicular rotation axis is derived from `FACING` without a second axis property.
- Replaced all Stabilizer placeholder models with Copper, Andesite, and Brass casing designs, 18 original 32x32 face textures, and static red, green, and yellow top status LEDs.
- Added all three Stabilizer Block Items to Functional Blocks in Tier order and verified inventory, JEI, held, dropped, Item Frame, and Ponder presentation.
- Added block, wrench, Codec, model, texture, Create-reference, and production-JAR tests plus all-tier side-versus-front/back kinetic GameTest coverage and wrench-network reconnection checks.
- Excluded datagen cache files from the production JAR.
- Added SHA-256 and source/dependency manifest generation.
- Bundled the verified production JAR and metadata in the CI artifact.
- Added a timeout-bounded dedicated-server smoke that detects readiness, sends a normal stop command, verifies zero-status exit, and retains its log.
- Adopted BSD-3-Clause for Frontier Protocol source code and original assets, with the authoritative license packaged in the JAR and release candidate.
- Expanded the verified release candidate to exactly four files by adding a byte-identical standalone `LICENSE`.
- Documented source and asset provenance while preserving the NeoForge MDK, Gradle wrapper, and audited Create fixture terms.
- Added binary Create Nixie Tube status panels for all Stabilizer tiers: OFFLINE is red, while ACTIVE and GRACE_PERIOD are green because both suppress infection.
- Added vanilla comparator output for normalized stored Cell fullness, including change-only updates after insertion, consumption, reload, drops, and live capacity changes.
- Added a tag-driven GitHub prerelease workflow that independently verifies and publishes the exact four-file release candidate without replacing existing releases or assets.
- Selected permanent, immutable GitHub Release assets as the packwiz distribution channel while retaining Actions artifacts as temporary internal verification bundles.
- Added post-publication checksum, byte-identity, manifest, release-state, and exact asset-set verification for the `v0.1.0-alpha.1` GitHub prerelease.
- Recorded the completed alpha publication and disposable packwiz public-URL smoke verification.
- Defined the future `0.1.0` stable support contract, blocker evidence audit, incomplete publication gates, and recommended `0.1.0-rc.1` sequence.
- Generalized release classification and publication checks for SemVer prereleases and stable releases without changing the current alpha version.
- Added an offline Stable startup matrix covering repeated fresh-world center persistence, actionable required-dependency diagnostics, malformed config recreation, safe numeric range correction, and bounded process cleanup.
- Added a provenance-checked published-Alpha world fixture and offline two-restart migration smoke covering Stabilizer state, exact item counts, spawn/cleanup SavedData, region readability, and fixture immutability.

### Compatibility

- Requires Minecraft 1.21.1, Java 21, NeoForge 21.1.235 or newer, Create 6.0.11 or newer, and exactly Spore 2.2.0j.
- Existing infected terrain, structures, nests, hazards, and Block Entities are retained; cleanup removes only explicitly audited non-Block-Entity foliage.
- All three tiers and their common Stabilization Cells use the Create production chain; Stabilization Compound is an intermediate and is not accepted by any Stabilizer.
- Stabilizers do not restore terrain, destroy nests, load chunks, suppress moving contraptions, or add a Block Entity GUI. Existing world serverconfig values are retained and must be updated manually to adopt new R9 defaults.
