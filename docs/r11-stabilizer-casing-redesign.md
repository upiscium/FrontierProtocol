# R11 Stabilizer Casing Redesign

## Decision

PR #5's open-frame animated prototype was not accepted. Its central 3D core,
rotating gear, four-face emissive ring, and custom Block Entity Renderer were
superseded by a simpler static design built from the latest `main` without
cherry-picking that prototype.

All three Stabilizers are now directional 1x1x1 Create-casing machines. They
use one shared `StabilizerBlock`, one static full-cube parent model, ordinary
BlockState models, and no block renderer, dynamic model, translucent geometry,
emissive overlay, animation, or particle effect.

## Visual contract

| Tier | Base and bottom | Front | Back | Sides |
| --- | --- | --- | --- | --- |
| Tier 1 | `create:block/copper_casing` | Added-on signal control plate | Decorative clamped elbow pipe | Dual-side shaft bearings |
| Tier 2 | `create:block/andesite_casing` | Added-on signal control plate | Decorative clamped elbow pipe | Dual-side shaft bearings |
| Tier 3 | `create:block/brass_casing` | Added-on signal control plate | Decorative clamped elbow pipe | Dual-side shaft bearings |

The original 32x32 face art borrows only Create's casing-machine visual
language. No Create PNG is copied into Frontier Protocol. Each tier supplies
exactly six custom RGBA textures: `front`, `back`, `side`, `top_offline`,
`top_active`, and `top_grace`. The dependency's verified casing texture is
referenced directly for the bottom.

Only the centered top LED changes with status:

- OFFLINE: red `#A83B3B`
- ACTIVE: green `#39D47A`
- GRACE_PERIOD: yellow `#F1C840`

The bezel, casing, bolts, front, back, and sides remain identical between
states. The LED is an ordinary lit texture pixel, not full-bright or animated.

## Direction contract

Every tier has only `FACING` and `STATUS`. The default is `FACING=EAST` and
`STATUS=OFFLINE`. Placement faces the front toward the player. Create's
`HorizontalKineticBlock` owns placement, rotation, and mirroring, including the
same `HORIZONTAL_FACING` property exposed as `FACING`.

The horizontal rotation axis is derived perpendicular to `FACING`; it is not
stored in BlockState. Both faces on that axis expose a Create shaft. The front,
rear, top, and bottom do not connect. For default EAST, NORTH and SOUTH are the
two shaft sides. A top- or bottom-face wrench rotation changes `FACING`, which
atomically moves the model, derived axis, shaft faces, and kinetic network.

R11 is an unmerged internal-alpha branch, so no released world depends on its
intermediate shaft schema. Development-world Stabilizers should be broken
and replaced after the final R11 schema change. No DataFixer is added.
Existing inventory-drop,
display-snapshot, display-NBT schema, persistent NBT, sync, balance, recipe,
Stress, Cell, Grace, cleanup, and suppression contracts are unchanged.

## Asset layout

- Three blockstate files, each with 12 `facing x status` variants
- One `stabilizer_casing_machine` parent with one 0-to-16 cube element
- Nine tier/status child models
- Three static item models parented to the matching OFFLINE model
- Eighteen original 32x32 RGBA textures
- Three Functional Blocks entries ordered Tier 1, Tier 2, Tier 3

## Verification

Automated verification passed:

- `./gradlew test`
- `./gradlew runData` with a clean generated-resource diff
- `./gradlew clean build`
- `./gradlew runGameTestServer`: 40/40
- Dedicated server: 3593 recipes loaded and `Done` reached without a
  Stabilizer renderer, model, Codec, or client-class error

Focused client verification passed for all three tiers:

- NORTH, EAST, SOUTH, and WEST presentation
- Front control plate, rear decorative pipe, both side bearings, top LED, and casing bottom
- OFFLINE, ACTIVE, and GRACE_PERIOD with LED-only differences
- Default EAST side motors: NORTH +16 RPM and SOUTH -16 RPM
- Default EAST front/back motors: EAST/WEST 0 RPM
- Top-face wrench rotation: old NORTH network 0 RPM, new EAST side -16 RPM
- Copper, Andesite, and Brass Casing adjacency plus Mechanical Press, Fluid
  Pipe, Redstone Link, and Shaft context
- Inventory, creative inventory, held, dropped, Item Frame, JEI, and Ponder
- Operation, Coverage, and Production Ponder scenes
- English with GUI Scale 2 and Fast graphics
- Japanese with GUI Scale Auto and Fancy graphics

## Final asset status

- Tier 1: FINAL
- Tier 2: FINAL
- Tier 3: FINAL
- Compound: FINAL
- Cell: FINAL
