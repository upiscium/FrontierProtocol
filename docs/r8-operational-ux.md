# R8 Operational UX

R8 adds targeted operational explanation without changing Stabilizer suppression, cleanup, Cell consumption, recipes, tier values, or balance. The accepted architecture is recorded in [ADR 0003](adr/0003-operational-display-and-client-visualization.md), and exact dependency APIs are recorded in [the client API audit](r8-client-api-audit.md).

## Display contract

- Engineer's Goggles show the targeted Stabilizer's server-authoritative tier, lifecycle state, rotation requirement, Cell buffer and time, coverage, and primary diagnostic.
- Holding Shift adds exact center and chunk bounds, full-height behavior, and configured Stress impact.
- Display snapshots use the existing Block Entity update channel. Immediate changes are coalesced within a server tick; ACTIVE and GRACE_PERIOD countdown corrections are limited to one update per 20 ticks.
- Invalid or incomplete client NBT is rejected. Until the first valid snapshot arrives, Goggles show a synchronization message and the range overlay stays hidden.

## Range overlay

The overlay requires its client option, Engineer's Goggles, a targeted non-virtual Stabilizer, a valid snapshot, normal world rendering, and Shift by default. It draws one depth-tested chunk grid with an outer perimeter, internal chunk boundaries, and optional vertical corner lines. It does not fill faces, create particles, scan nearby machines, or display through walls.

Colors communicate but do not replace text: ACTIVE is teal-green, GRACE_PERIOD is amber, and OFFLINE is red.

## Other guidance

Static item tooltips explain tier roles, Cell and Compound production roles, progressive cleanup, and the need for physical defenses without embedding mutable server settings. Ponder provides Operation, Coverage, and Production scenes. Quest integrations remain documentation-only and introduce no quest-mod dependency.

Initial Overworld spawn protection is a permanent suppression field centered on the persisted initial spawn. Its default radius is two chunks, covering 5x5 chunks. It needs no Cell or rotation, does not move when world spawn later changes, performs no progressive cleanup, does not remove existing nests, and does not prevent hostile mobs. It is an initial refuge, not a complete safe zone.

## Verification status

Implementation and manual client verification status will be updated as R8 phases complete. Physical upper-tier Mechanical Crafting and continuous automated Cell supply remain separate, previously unverified production smoke checks.
