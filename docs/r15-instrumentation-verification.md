# R15 Stabilizer Instrumentation Verification

## Player contract

A Create Nixie Tube directly attached to any Stabilizer tier uses Create's
existing train-signal panel renderer. The display is intentionally binary:

| Stabilizer status | Nixie state | Meaning |
| --- | --- | --- |
| `OFFLINE` | red | containment is offline |
| `ACTIVE` | green | infection suppression is active |
| `GRACE_PERIOD` | green | infection suppression continues while Grace is spent |

The mapping follows `StabilizerStatus#suppressesInfection()`. Yellow is never
used. The panel does not distinguish `ACTIVE` from `GRACE_PERIOD`; Engineer's
Goggles remain the detailed interface for RPM, Stress, Grace, Cell time, Cell
count, capacity, and coverage diagnostics. Create's ComputerCraft control keeps
precedence when that optional integration is available.

A vanilla comparator reports normalized stored Stabilization Cell fullness:

```text
count <= 0 or capacity <= 0 -> 0
otherwise -> min(15, 1 + floor(14 * min(count, capacity) / capacity))
```

The output is `0` when empty, at least `1` when non-empty, and `15` at configured
capacity or above it. It is not operating status and does not estimate remaining
runtime. Stabilizers remain non-signal-source blocks and emit no direct weak or
strong redstone power.

## Automated verification

- Comparator unit cases cover counts `0`, `1`, representative partial values,
  exact half-full, exact full, over-capacity, non-positive capacity, and default
  capacities `8`, `32`, and `64`.
- Nixie resolver unit cases cover `OFFLINE -> RED`, `ACTIVE -> GREEN`,
  `GRACE_PERIOD -> GREEN`, and no override for a non-Stabilizer block.
- GameTests verify all three tiers expose analog output without becoming direct
  signal sources; empty, half-full, and full buffers produce `0`, `8`, and `15`.
- A physical adjacent comparator verifies external insertion `0 -> 15`, automatic
  Cell consumption `15 -> 13`, capacity expansion `13 -> 7`, capacity reduction
  `7 -> 15`, and over-capacity NBT reload retention at `15`.
- The complete GameTest suite retains rotation, Grace, suppression, cleanup,
  lifecycle reload, and continuous Cell rollover coverage.
- Dedicated-server smoke verifies the client-only Nixie Mixin is not loaded on a
  production server.

## Client smoke evidence

The R15 client was launched on 2026-07-30 with `./gradlew runClient` under a
1920x1080 Xvfb display and joined the existing `aR6 Create Smoke` creative world
using quick play. It completed resource and renderer startup with the Frontier
Protocol client Mixin configuration present, all block atlases created, and 77
Flywheel shader sources loaded. Fast graphics reported 34 FPS through Mesa
llvmpipe. No Frontier Protocol Mixin apply, renderer, missing-model, or
client-class error was logged. Existing TFMG and Spore asset warnings remain
dependency-originated and are unrelated to R15.

A second Xvfb launch with Fancy graphics also completed resource, atlas, and all
77 Flywheel shader-source loads without a Frontier Protocol error. This verifies
both graphics modes reach renderer startup; it is not a substitute for the
separate local in-world verification below.

## Local in-world verification

On 2026-07-30, the user completed the requested local in-world verification on
the review branch and reported no issue. The following checks passed:

- Tier 1 Nixie integration.
- Tier 2 Nixie integration.
- Tier 3 Nixie integration.
- `OFFLINE` displays red.
- `ACTIVE` displays green.
- `GRACE_PERIOD` displays green.
- Horizontal Stabilizer facings.
- Representative wall attachment.
- Representative floor attachment.
- Representative ceiling attachment.
- Detaching restores the standard Nixie numeric/redstone display.
- Reattaching restores the train-signal panel.
- Fast graphics.
- Fancy graphics.
- No visible rendering or behavior defect was observed.

ComputerCraft precedence was not exercised because the optional runtime was
unavailable.

No custom renderer, texture, model, packet, or persistent Nixie NBT is introduced.
The integration runs after Create's normal Nixie tick handling, uses
`NixieTubeBlock.getFacing`, and changes Create's public `signalState` only for a
directly attached Stabilizer when no computer is attached.
