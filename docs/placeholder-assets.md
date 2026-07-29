# Asset Status

## Final status

| Asset | Status | Model and texture contract |
| --- | --- | --- |
| Tier 1 Stabilizer | FINAL | Copper Casing base; six custom 32x32 RGBA faces; three static state models |
| Tier 2 Stabilizer | FINAL | Andesite Casing base; six custom 32x32 RGBA faces; three static state models |
| Tier 3 Stabilizer | FINAL | Brass Casing base; six custom 32x32 RGBA faces; three static state models |
| Stabilization Compound | FINAL | `minecraft:item/generated`; custom 32x32 RGBA PNG |
| Stabilization Cell | FINAL | `minecraft:item/generated`; custom 32x32 RGBA PNG |

## Stabilizer block assets

R11 replaces the nine internal-alpha placeholder models with one static
full-cube parent and nine tier/status child models. Each tier provides exactly
six original textures: `front`, `back`, `side`, `top_offline`, `top_active`,
and `top_grace`. All 18 are 32x32 RGBA pixel art.

The bottom face directly references the verified Create 6.0.11 resources:

| Tier | Base resource |
| --- | --- |
| Tier 1 | `create:block/copper_casing` |
| Tier 2 | `create:block/andesite_casing` |
| Tier 3 | `create:block/brass_casing` |

Create PNGs are not copied or modified into Frontier Protocol. The custom
front signal panel, rear bearing, side pipe, and top casing art are original.
The old unmerged open-frame animated prototype was not adopted. No block
renderer, dynamic part, translucent window, emissive overlay, or animated
texture is part of the final block asset set.

Only the centered LED changes between state textures: OFFLINE red, ACTIVE
green, and GRACE_PERIOD yellow. Automated tests constrain state differences to
the LED region, resolve every model and Create reference, and require the exact
static asset set in the production JAR.

## Item assets

Each Stabilizer Block Item statically parents the matching OFFLINE block model.
Compound and Cell retain their original final R10 item textures; their former
Blaze Powder and Prismarine Crystals placeholders are no longer used.

Production Ponder continues to use Create equipment models and TFMG's own
Liquid Plastic rendering from the required dependency. Frontier Protocol does
not copy a TFMG texture.
