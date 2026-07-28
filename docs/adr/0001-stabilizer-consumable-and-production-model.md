# ADR 0001: Stabilizer Consumable and Production Model

## Status

Accepted

## Context

Frontier Protocol needs a production model that makes Stabilizer operation compatible with Create automation without multiplying Tier-specific items or container states. The model must keep the R6 production chain small while leaving later Tier balance adjustable.

## Decision

- Stabilization Compound is a manufacturing intermediate. It cannot be inserted directly into a Stabilizer.
- Stabilization Cell is the finished consumable accepted and consumed by Stabilizers.
- All Stabilizer Tiers use the same Stabilization Cell.
- Tier differences are expressed through fuel economy, internal capacity, efficiency, Stress impact, RPM requirements, and cleanup performance rather than Tier-specific Cells.
- R6 adds no other custom intermediate material.
- R6 does not implement Empty Cells, Spent Cells, Tier-specific Cells, or container returns.
- R6 defines exactly three Create recipes: Compound Mixing, Cell Deploying, and Tier 1 Mechanical Crafting.
- No normal crafting or other bypass recipe is provided for the R6 production outputs.
- Recipe balance remains provisional and may be adjusted in R9 without changing this consumable model.
- R7 inherits this decision when introducing later Stabilizer Tiers.

## Rejected Alternatives

- Allowing Compound to power a Stabilizer directly was rejected because it bypasses the finished Cell and its Create production step.
- Empty and Spent Cell states or container returns were rejected because they add inventory states and return logistics outside R6 scope.
- Tier-specific Cells were rejected because Tier identity belongs in machine performance and operating cost, not duplicate consumable Registry IDs.
- Additional custom intermediates and normal-crafting fallbacks were rejected to keep the production chain minimal and Create-owned.

## Consequences

Production automation has one common finished consumable and one custom intermediate. Later Tiers can change consumption and machine performance without introducing new Cell types or migrating stored consumables.

If the fuel model changes, this ADR must be marked `Superseded` and a new ADR must record the replacement decision.
