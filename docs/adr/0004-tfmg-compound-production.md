# ADR 0004: TFMG Compound Production

## Status

Accepted

## Context

ADR 0001 established one common finished Stabilization Cell and one Frontier Protocol manufacturing intermediate. The original Compound material selection used a Spore-derived input, while the revised production design requires an industrial Create processing path based on TFMG Liquid Plastic.

## Decision

- ADR 0001's common Cell model remains accepted.
- This ADR revises only ADR 0001's Compound material selection and production dependency.
- Stabilization Compound remains the only Frontier Protocol intermediate and cannot power a Stabilizer directly.
- Stabilization Cell remains the finished consumable shared by every Stabilizer Tier.
- TFMG 1.2.0 or newer is a required client and server dependency.
- Compound uses unheated Create Mixing with 100 mB of `tfmg:molten_plastic`, one Sand, one Blue Ice, and eight Iron Nuggets.
- Mixing produces one Stabilization Compound.
- No Spore-derived item is used as a Compound production material.
- The existing Iron Sheet plus Compound Deploying recipe remains the only Cell recipe.

## Rejected Alternatives

- Keeping TFMG optional was rejected because the only Compound recipe would be unavailable without it.
- Falling back to Water or an empty Fluid when Liquid Plastic lookup fails was rejected because it would silently change the production contract.
- Retaining Spore Biomass as an alternate recipe was rejected because it would preserve the superseded material path.
- Adding a normal crafting, smithing, or stonecutting fallback was rejected because Cell production remains Create-owned.

## Consequences

Frontier Protocol now requires Create: The Factory Must Grow in every runtime. Spore remains required for infection integration, but its items are no longer part of Compound manufacturing. The production chain still has exactly five Frontier Protocol recipes and preserves the common Cell and staged Stabilizer upgrade model.
