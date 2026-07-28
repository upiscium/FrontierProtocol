# ADR 0003: Operational Display and Client Visualization

## Status

Accepted

## Context

R8 must explain live Stabilizer operation without changing containment gameplay or adding a machine GUI. Multiplayer clients cannot infer server configuration, and rendering must remain targeted and bounded.

## Decision

- Engineer's Goggles are the primary live-diagnostics surface. R8 adds no Block Entity GUI, container menu, or persistent custom HUD.
- Live values are server-authoritative and use the existing Block Entity update tag and packet path. R8 adds no custom network payload.
- ACTIVE and GRACE_PERIOD countdowns synchronize at most approximately once per second; countdowns do not send a packet every tick.
- Item tooltips do not display mutable server values. Numerical diagnostics belong to the targeted in-world Goggle display.
- The range overlay renders only while Goggles are worn and, by default, Shift is held. It renders only the Stabilizer under the crosshair and never scans nearby Block Entities each frame.
- Range visualization uses depth-tested lines, not filled faces or particles. R8 adds no persistent particles.
- Ponder scenes are explanatory virtual scenes. They manipulate scene state and never invoke suppression, cleanup, SavedData, or server configuration.
- FTB Quests, Heracles, Better Questing, Jade, WAILA, and WTHIT remain optional external integrations. Quest support is documentation and stable Registry IDs.
- Initial Overworld spawn suppression is explained without changing its behavior.
- Client bootstrap, rendering, tooltip events, and Ponder registration remain in client-only classes. Common code contains only side-safe display data, NBT, diagnostics, and formatting contracts.

## Rejected Alternatives

- A permanent custom HUD or dedicated GUI was rejected as unnecessary for targeted machine inspection.
- Per-tick synchronization was rejected because countdown precision does not justify the packet rate.
- Client-side server-config inference was rejected because integrated and dedicated server values can differ.
- Rendering every Stabilizer or using dense particles was rejected because it requires repeated world scans and creates avoidable visual and CPU load.
- Required Jade or quest-mod dependencies were rejected because R8 must remain usable without either ecosystem.

## Consequences

The server publishes a small validated display snapshot. Clients can safely show stale data for at most approximately one second while a machine is ACTIVE or in GRACE_PERIOD. Before the first valid snapshot, Goggles report synchronization and range rendering remains disabled.

Implementation and user-facing behavior are documented in [R8 Operational UX](../r8-operational-ux.md), release boundaries in [Alpha Release](../alpha-release.md), and basic operation in the project [README](../../README.md).
