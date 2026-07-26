# R8 Operational UX

R8 adds targeted operational explanation without changing Stabilizer suppression, cleanup, Cell consumption, recipes, tier values, or balance. The accepted architecture is recorded in [ADR 0003](adr/0003-operational-display-and-client-visualization.md), and exact dependency APIs are recorded in [the client API audit](r8-client-api-audit.md).

## Display contract

- Engineer's Goggles show the targeted Stabilizer's server-authoritative tier, lifecycle state, rotation requirement, Cell buffer and time, coverage, and primary diagnostic.
- Holding Shift adds exact center and chunk bounds, full-height behavior, and configured Stress impact.
- Display snapshots use the existing Block Entity update channel. Immediate changes are coalesced within a server tick; ACTIVE and GRACE_PERIOD countdown corrections are limited to one update per 20 ticks.
- Invalid or incomplete client NBT is rejected. Until the first valid snapshot arrives, Goggles show a synchronization message and the range overlay stays hidden.

The normal Goggle section contains tier, state, current/theoretical and required RPM, Cell count/capacity, active or grace time, coverage, suppressed chunk count, and one prioritized diagnostic. Shift adds center chunk, exact X/Z ranges, full-dimension-height behavior, configured Stress impact, and configured Cell duration. Diagnostic priority is overstressed, no rotation, insufficient RPM, no Cell or stored time, grace, then operational.

Client options are `showStabilizerGoggleDetails`, `showStabilizerRangeOverlay`, `rangeOverlayRequiresSneaking`, and `showRangeVerticalCorners`. They affect display only.

If configured Cell capacity is reduced below existing inventory, stored Cells are retained and displayed at their actual count over the new capacity, such as `32 / 8`, until normal consumption reduces the buffer. Display synchronization does not clamp, delete, or drop those Cells.

## Range overlay

The overlay requires its client option, Engineer's Goggles, a targeted non-virtual Stabilizer, a valid snapshot, normal world rendering, and Shift by default. It draws one depth-tested chunk grid with an outer perimeter, internal chunk boundaries, and optional vertical corner lines. It does not fill faces, create particles, scan nearby machines, or display through walls.

Colors communicate but do not replace text: ACTIVE is teal-green, GRACE_PERIOD is amber, and OFFLINE is red.

## Other guidance

Static item tooltips explain tier roles, Cell and Compound production roles, progressive cleanup, and the need for physical defenses without embedding mutable server settings. Ponder provides Operation, Coverage, and Production scenes. Quest integrations remain documentation-only and introduce no quest-mod dependency.

Ponder schematics are committed at `assets/frontier_protocol/ponder/stabilizer/{operation,coverage,production}.nbt`. They are gzip-compressed copies of the generated one-block empty GameTest structure; storyboards construct all explanatory blocks, grids, state changes, and item prompts through scene APIs. They are not generated into the build directory only.

The Operation storyboard selects the matching Stabilizer block for each Tier; the Cell uses Tier 1 as its representative operation scene. It demonstrates Cell insertion through a Depot and Funnel representing Create logistics. Direct right-click insertion into a Stabilizer is not supported or depicted.

Initial Overworld spawn suppression is a permanent field centered on the persisted initial spawn. Its default radius is two chunks, covering 5x5 chunks. It needs no Cell or rotation, does not move when world spawn later changes, performs no progressive cleanup, does not remove existing nests, and does not prevent hostile mobs. It is an initial refuge, not a complete safe zone.

## Verification status

Unit tests cover snapshot validation, malformed display NBT, sync cadence, diagnostics, duration formatting, Goggle and static tooltip keys, Ponder resource/localization parity, and range geometry. The `stabilizer_display_sync` GameTest covers all three tiers, OFFLINE/ACTIVE/GRACE_PERIOD, Cell insertion and consumption, live config refresh, persistent NBT, and display-only packet boundaries. All 34 GameTests pass.

The dedicated server reached ready without loading Frontier Protocol client classes and saved all dimensions through its shutdown hook. The Gradle task still returned exit 143 because the smoke harness sent SIGTERM. A normal display was unavailable. The required direct client command failed at `glfwInit`; a supplemental Xvfb run reached client resource reload and registered Ponder scenes and tags without a Frontier Protocol missing-resource error, but automated window shutdown did not complete normally. In-world Goggle, range, item-tooltip, Ponder playback, and Japanese visual checks therefore remain unverified, so R8 is not yet considered complete.

Physical upper-tier Mechanical Crafting and continuous automated Cell supply remain separate, previously unverified production smoke checks.
