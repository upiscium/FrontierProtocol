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
| Tier 1 | `create:block/copper_casing` | Added-on signal control plate | Centered shaft bearing | Clamped elbow pipe |
| Tier 2 | `create:block/andesite_casing` | Added-on signal control plate | Centered shaft bearing | Clamped elbow pipe |
| Tier 3 | `create:block/brass_casing` | Added-on signal control plate | Centered shaft bearing | Clamped elbow pipe |

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

Every tier has `FACING`, `HORIZONTAL_AXIS`, and `STATUS`. The default is
`FACING=EAST`, `HORIZONTAL_AXIS=X`, and `STATUS=OFFLINE`. Placement faces the
front toward the player. Rotation and mirroring derive the axis from the new
facing, preserving `HORIZONTAL_AXIS == FACING.getAxis()`.

Only `FACING.getOpposite()` exposes a Create shaft. The front, both sides, top,
and bottom do not connect. The default west rear remains aligned with existing
Ponder and GameTest shaft fixtures.

Existing placed Stabilizers from all tiers should be broken and replaced
after this redesign so their front and rear direction is selected explicitly.

No DataFixer is added for this internal alpha. Existing inventory-drop,
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
- Front control plate, rear bearing, both pipe sides, top LED, and casing bottom
- OFFLINE, ACTIVE, and GRACE_PERIOD with LED-only differences
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
