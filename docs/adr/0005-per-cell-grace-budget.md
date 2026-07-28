# ADR 0005: Per-Cell Grace Budget

## Status

Accepted

## Context

The original state machine restored the configured Grace duration on every ACTIVE tick. Briefly restoring rotation therefore replenished Grace without consuming another Cell, allowing repeated power cycling to extend suppression indefinitely. Grace needs a finite resource boundary consistent with the common Cell operating model.

## Decision

- Grace is a suppression-continuity budget granted once when a new Cell is consumed.
- ACTIVE ticks consume Cell time but do not replenish Grace.
- Recovering rotation with the same Cell preserves the remaining Grace budget.
- A new Cell is the only event that restores Grace to the configured maximum.
- Grace exhaustion does not discard remaining Cell time. Rotation can restore ACTIVE operation with zero Grace remaining.
- GRACE_PERIOD retains suppression, pauses cleanup, and pauses Cell-time consumption.
- The ACTIVE-to-GRACE_PERIOD transition tick is the first Grace tick. A configured budget of `N` is externally observable as exactly `N` GRACE_PERIOD ticks.
- Saved Grace is clamped to the current configured maximum on load and evaluation. An already-started Cell retains its saved remaining duration; the current Cell-duration setting applies to the next Cell.

## Rejected Alternatives

- Replenishing Grace on every ACTIVE tick was rejected because power cycling creates unbounded suppression continuity.
- Fully restoring Grace whenever rotation returns was rejected for the same reason.
- Running cleanup during Grace was rejected because Grace guarantees suppression continuity, not free cleanup work.
- Consuming Cell time and Grace simultaneously was rejected because it would spend both budgets for the same outage tick.

## Consequences

Each Cell now has a finite, inspectable outage budget. Existing valid config files retain their saved values; changing source-code defaults does not rewrite `serverconfig` values already stored in a world. Operators must update the three Grace keys manually when adopting the R9 defaults. No NBT key or representation changed, so the persistent schema remains version 1.
