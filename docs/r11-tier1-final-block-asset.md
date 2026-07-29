# R11 Tier 1 Final Block Asset

R11 replaces the Tier 1 Stabilizer's internal-alpha placeholder with a directional custom block model, state-specific textures, and client-only animated parts. It does not change containment behavior, recipes, balance, Cell consumption, cleanup, or the Tier 2 and Tier 3 assets.

## Orientation contract

- Placement turns the machine front toward the placer.
- `facing` is horizontal and `horizontal_axis` always equals `facing`'s axis.
- Create shaft input is exposed only on the rear face, opposite `facing`.
- Rotation and mirror operations update facing and axis together.
- Tier 2 and Tier 3 do not receive a facing property and retain their existing shaft behavior.
- The default state faces east so the established west-side GameTest motor remains attached to the rear. Normal player placement still selects the direction from the placer.

Existing placed Tier 1 Stabilizers may need to be broken and replaced to select their intended front and rear after this block-state change. No data migration is provided for this internal alpha.

## Static asset contract

The blockstate contains exactly 12 variants: four horizontal facings multiplied by `offline`, `active`, and `grace_period`. Facing rotation is selected in blockstate JSON; `horizontal_axis` is maintained by Java and is not duplicated in model selection.

Seven block-model resources define the shared seven-element body, three static status presentations, and the dynamic core, gear, and light overlay. The Tier 1 item model uses the static OFFLINE block model so inventory, held, dropped, Item Frame, JEI, and Ponder ItemStack rendering do not depend on a placed Block Entity renderer.

The asset set contains 26 custom 32x32 RGBA block textures. Static status textures keep OFFLINE, ACTIVE, and GRACE_PERIOD distinguishable if animated rendering is unavailable. The core, gear, and light mask require transparent backgrounds. Tier 1 final models do not reference its former Vanilla iron, copper, redstone, deepslate, or copper-grate placeholders and do not copy Vanilla, Create, TFMG, or Spore textures.

## Dynamic presentation

The Block Entity renderer is registered from client-only code and exits immediately for Tier 2 and Tier 3.

| State | Presentation |
| --- | --- |
| OFFLINE | Static body with a dim red full-bright status light |
| ACTIVE | Green full-bright status light, pulsing emissive core, and signed-speed core/gear rotation |
| GRACE_PERIOD | Yellow full-bright warning light; blink frequency rises as the finite Grace reserve falls |

ACTIVE rotation uses the kinetic speed sign and clamps visual motion to 2 through 9 degrees per tick for nonzero speed. The core pulse runs at 0.8 Hz. Grace warning cadence is 0.75 Hz above half reserve, 1.5 Hz above one fifth reserve, and 3 Hz at or below one fifth reserve. Warning alpha remains between 0.25 and 1.0.

Display synchronization uses schema version 2 and includes both `graceRemainingTicks` and `graceDurationTicks`. The client derives warning cadence from their ratio. Invalid snapshots, missing fields, wrong schema versions, and remaining Grace above duration are rejected. The visual angle is client-only and is not persisted or sent in server NBT.

## Automated verification

Automated coverage verifies:

- placer-facing, axis, rear-only shaft, rotation, mirror, and status-change invariants;
- absence of `facing` on Tier 2 and Tier 3;
- all 12 blockstate variants and all seven block-model resources;
- the static OFFLINE item-model parent and rejection of former Tier 1 placeholder references;
- all 26 textures for dimensions, alpha, visible content, color detail, and required transparent backgrounds;
- state colors, Grace frequency thresholds and alpha bounds, signed speed, speed clamp, and ACTIVE pulse;
- display schema v2 round trips and invalid-input rejection;
- final asset inclusion in the production JAR.

Unit tests, datagen cleanliness, clean build, and all 39 GameTests pass after aligning the default rear shaft with the existing physical fixture.

## Manual verification status

Focused client verification remains pending. Until it is completed, R11 does not claim that the following have passed in game:

- body, core, gear, and light alignment without clipping or z-fighting;
- all four facings and rear-only physical shaft attachment;
- OFFLINE, ACTIVE, and GRACE_PERIOD colors and animation;
- 32, 64, and 128+ RPM plus negative-speed rotation;
- high-, middle-, and low-reserve Grace warning cadence;
- Fast and Fancy graphics, English and Japanese, and GUI Scale 2 and Auto;
- inventory, held, dropped, Item Frame, JEI, and Operation/Coverage/Production Ponder presentation.

The dedicated server was repeated after the renderer addition. It loaded 3593 recipes, reached `Done` without a renderer client-class error, and stopped normally. The existing optional Farmers Delight recipe parse warning from Spore remains unrelated to R11. Tier 2 and Tier 3 final art, the project logo, licensing decisions, and public distribution remain pending.
