# Frontier Protocol

[English](README.md) | [Japanese](README.ja.md)

Frontier Protocol is a NeoForge addon for Fungal Infection: Spore, Create, and
Create: The Factory Must Grow (TFMG).
It introduces Create-powered containment infrastructure that suppresses Spore
infection across entire chunks, from the bottom to the top of the dimension.
Players can use Create production and logistics to make the common Stabilization
Cells and supply one of three stationary Stabilizer tiers, allowing sustainable
settlements and industrial bases to exist inside an infected world.

Containment does not prevent hostile mobs from entering protected areas.
Physical defense remains the player's responsibility. Resource generation and
infinite ore extraction are intentionally delegated to other Create addons.

## RC Prerelease

The latest public version is **0.1.0-rc.1**, published as a prerelease on
2026-07-31 from exact source commit
`5fe7b0f9931560747af7c9c25df367c1e4db9014`. Current main source is the
release-ready **0.1.0 Stable candidate**, with all pre-publication gates
complete. The Stable tag and GitHub Release have not been published.
The [stable-readiness contract](docs/stable-readiness-0.1.0.md) defines its
support promises, accepted evidence, required gates, and publication sequence.
`0.1.0-alpha.1` remains available as the previous public prerelease, and its
published assets remain immutable. Later source candidates and untagged workflow
builds are not public releases. Back up worlds before testing a prerelease. The
current source retains the following containment integration scope:

- Permanent infection suppression in the 5x5 chunk area centered on the
  initial Overworld spawn by default.
- Initial-spawn ore-generation suppression for newly generated terrain.
- Three Create-powered Stabilizer tiers covering 1x1, 3x3, or 5x5 chunks while
  ACTIVE or spending the finite Grace budget granted by their current Cell.
- Budgeted cleanup of audited removable Spore foliage in loaded chunks while
  a Stabilizer is ACTIVE, with tier-specific source profiles, hard global caps,
  and persisted progress across reloads.
- Target-based integration with the audited environmental spread paths in
  Fungal Infection: Spore 2.2.0j.
- Dimension-local and overlap-safe suppression, including negative chunk
  coordinates.
- Five Create recipes for Stabilization Compound, common Stabilization Cells,
  Tier 1, and staged Tier 1-to-2 and Tier 2-to-3 Stabilizer upgrades.
- Unheated Compound Mixing with 100 mB of TFMG Liquid Plastic, Sand, Blue Ice,
  and eight Iron Nuggets.
- Engineer's Goggles diagnostics, targeted chunk-range visualization, static
  item guidance, and localized Ponder scenes for operation, coverage, and production.
- Create Nixie Tube binary status panels and vanilla comparator Cell-buffer
  instrumentation for every Stabilizer tier.
- Per-Cell Grace budgets of 1200, 1800, and 2400 ticks for Tier 1, Tier 2, and
  Tier 3. ACTIVE and power recovery do not refill spent Grace; only a new Cell does.
- Physical Create GameTests for unheated Compound Mixing, all three Mechanical
  Crafter recipes, and default-duration Tier 2/Tier 3 continuous Cell logistics.

Suppression prevents selected new Spore mutations. One shared `StabilizerBlock`
class, one shared `StabilizerBlockEntity` class/type, and one state machine serve
all three Registry entries. Generic `STABILIZER` source IDs contain tier and
position. While ACTIVE, each source removes only audited non-Block-Entity foliage
under its tier profile and server-global caps; cleanup pauses during grace and
stops offline. Stabilizers do not restore infected terrain, destroy nests, load
chunks, suppress moving contraptions, or block hostile mobs. The Spore integration
is pinned to the exact audited release. All tiers consume the same Cell through
Create-oriented production; Compound cannot power a Stabilizer. See
[`docs/r7-stabilizer-tiers.md`](docs/r7-stabilizer-tiers.md),
[ADR 0002](docs/adr/0002-common-stabilizer-tier-architecture.md), and
[ADR 0001](docs/adr/0001-stabilizer-consumable-and-production-model.md). The
revised Compound material decision is recorded in
[ADR 0004](docs/adr/0004-tfmg-compound-production.md).
The finite Grace decision and final R9 values are recorded in
[ADR 0005](docs/adr/0005-per-cell-grace-budget.md) and
[R9 Balance Hardening](docs/r9-balance-hardening.md).

## Inspecting a Stabilizer

1. Equip Engineer's Goggles.
2. Look directly at a Stabilizer to inspect its live server-authoritative operation.
3. Hold Shift for exact chunk bounds, configured operating details, and the targeted range overlay.

The overlay is shown only for the targeted Stabilizer and does not indicate protection from hostile mobs. Use Create's configured Ponder key while hovering a Stabilizer, Stabilization Cell, or Stabilization Compound to open the Operation, Coverage, and Production explanations. The completed English and Japanese client smoke is recorded in [R8 Graphical Verification](docs/r8-graphical-verification.md).

A Create Nixie Tube mounted directly against a Stabilizer uses Create's train-signal
panel visual as a binary containment indicator. Red means containment is offline.
Green means infection suppression is active, including `GRACE_PERIOD`, because
suppression continues while Grace is spent. The panel never uses yellow and does
not distinguish `ACTIVE` from `GRACE_PERIOD`; Engineer's Goggles remain the
detailed diagnostic interface.

A vanilla comparator reads normalized stored Stabilization Cell fullness from
any Stabilizer tier. Signal `0` is empty, any non-empty buffer is at least `1`,
and configured-full or persisted over-capacity buffers are `15`. This signal is
inventory instrumentation only: it does not report operating status or remaining
runtime, and the Stabilizer emits no direct weak or strong redstone power. See
[R15 Instrumentation Verification](docs/r15-instrumentation-verification.md).

The initial Overworld spawn center is persisted and has permanent infection suppression with a default radius of two chunks, covering 5x5 chunks. Changing world spawn later does not move it. It needs no Cell or rotation and performs no progressive cleanup. It does not remove existing nests or stop hostile mobs; it is an initial refuge, not a complete safe zone. See [R8 Operational UX](docs/r8-operational-ux.md), [ADR 0003](docs/adr/0003-operational-display-and-client-visualization.md), and [Quest Integration](docs/quest-integration.md).

## Distribution

GitHub Releases is the public distribution channel. Pull-request and manual
Build workflow artifacts remain temporary, 14-day internal verification bundles.
They are not packwiz endpoints. The tag-driven release published the four
verified candidate files as permanent GitHub prerelease assets.

The immutable JAR URL for `v0.1.0-rc.1` is:

```text
https://github.com/upiscium/FrontierProtocol/releases/download/v0.1.0-rc.1/frontier_protocol-0.1.0-rc.1.jar
```

Add it from a modpack root with:

```bash
packwiz url add --meta-folder mods "Frontier Protocol" \
  "https://github.com/upiscium/FrontierProtocol/releases/download/v0.1.0-rc.1/frontier_protocol-0.1.0-rc.1.jar"
```

packwiz stores this direct URL and its calculated file hash in the generated
`.pw.toml` metadata. Future versions receive new immutable version tags and URLs.
The `v0.1.0-rc.1` and previous `v0.1.0-alpha.1` assets must never be replaced or
mutated.

## Requirements

- Minecraft 1.21.1
- Java 21
- NeoForge 21.1.235 or newer for Minecraft 1.21.1
- Create 6.0.11 or newer
- Create: The Factory Must Grow 1.2.0 or newer
- Fungal Infection: Spore 2.2.0j exactly

## Internal Testing

For internal testing, build the release-ready candidate from source, install the matching
NeoForge release, Create, TFMG, and Spore, then place
`frontier_protocol-0.1.0.jar` in the instance's `mods` directory. This is an
unpublished Stable candidate, not the public installation artifact. Do not use
a different Spore version with this Stable candidate.

Release-specific compatibility notes and verification details are documented
in the [0.1.0 Stable release notes](docs/releases/0.1.0.md). Public RC
verification remains in the
[0.1.0-rc.1 publication record](docs/releases/0.1.0-rc.1-publication.md).

Stabilization Compound and Stabilization Cell use custom final 32x32 item icons.
All three Stabilizers use final static Create-casing machine models with a
top status LED, a front control panel, a rear decorative pipe, and dual-side
shaft bearings. Rotation input is accepted only from the two side faces, with
the horizontal rotation axis derived perpendicular to `FACING`.
They use no custom Block Entity Renderer or dynamic block parts. See
[`docs/placeholder-assets.md`](docs/placeholder-assets.md) and
[R11 Stabilizer Casing Redesign](docs/r11-stabilizer-casing-redesign.md).
The project logo remains pending.

## License

Frontier Protocol source code and original assets are licensed under the
[BSD 3-Clause License](LICENSE). Third-party projects and dependency content
retain their own licenses; they are not relicensed by Frontier Protocol. See
the [licensing and provenance audit](docs/licensing.md) for the exact scope and
the notices retained for repository templates and the audited Create GameTest
fixture.

## Development

The project targets Minecraft 1.21.1 with Java 21 and NeoForge. Create, TFMG,
and Fungal Infection: Spore are required dependencies.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer
```

Spore 2.2.0j is resolved from CurseForge project 678295, file 8342823 through
CurseMaven. The dependency and mod metadata are pinned to that exact release.
