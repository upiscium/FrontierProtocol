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

Four block-model resources define the open-frame base and three static status presentations. The former core, gear, and light-overlay JSON files were unused and have been removed. Java `LayerDefinition` and `ModelPart` geometry in `StabilizerRenderModels` is the source of truth for every dynamic part. The Tier 1 item model uses the static OFFLINE block model so inventory, held, dropped, Item Frame, JEI, and Ponder ItemStack rendering do not depend on a placed Block Entity renderer.

The base model has real north, west, east, and top apertures around one central chamber rather than transparent markings over a solid cuboid. Its south side remains sealed and includes a centered reinforcement plate, bearing housing, shaft collar reaching the `z=16` connection plane, and two non-emissive conduits. Tier 1 disables occlusion so adjacent faces remain visible through its cut corners and windows; its collision shape remains a full cube. Tier 2 and Tier 3 retain their original occlusion and models.

The asset set contains 18 referenced custom 32x32 RGBA block textures. Every production texture is referenced by a static model or the Block Entity renderer. Static status textures keep OFFLINE, ACTIVE, and GRACE_PERIOD distinguishable if animated rendering is unavailable. The core, gear, and light mask require transparent backgrounds. Eight unused casing, armor, plate, warning, window, and state-ring textures were removed. Tier 1 final models do not reference its former Vanilla iron, copper, redstone, deepslate, or copper-grate placeholders and do not copy Vanilla, Create, TFMG, or Spore textures.

## Dynamic presentation

The Block Entity renderer is registered from client-only code and exits immediately for Tier 2 and Tier 3. It renders one fixed three-stage central 3D core, one eight-tooth frame inside the front window, and synchronized full-bright rings on the front, left, right, and top faces.

| State | Presentation |
| --- | --- |
| OFFLINE | Static body and non-emissive central core with four dim red full-bright status rings |
| ACTIVE | Four green full-bright status rings, a pulsing emissive fixed core, and signed-speed gear rotation |
| GRACE_PERIOD | Non-emissive central core and four yellow full-bright warning rings; blink frequency rises as the finite Grace reserve falls |

The gear rotates around its local Z axis so it stays parallel to the north-facing front window. A single negative sign in the renderer converts Create's kinetic angle to the model coordinate system; animation state retains the Create speed sign unchanged. The client keeps a continuous angle across forward and reverse wraps, rebases both interpolation endpoints together only beyond 36,000 degrees, and applies modulo only before rendering. ACTIVE motion is clamped to 2 through 9 degrees per tick for nonzero speed. The fixed core pulse runs at 0.8 Hz. Grace warning cadence is 0.75 Hz above half reserve, 1.5 Hz above one fifth reserve, and 3 Hz at or below one fifth reserve. All four warning rings share one color, alpha, and phase.

Display synchronization uses schema version 2 and includes both `graceRemainingTicks` and `graceDurationTicks`. The client derives warning cadence from their ratio. Invalid snapshots, missing fields, wrong schema versions, and remaining Grace above duration are rejected. The visual angle is client-only and is not persisted or sent in server NBT.

The server-authoritative display snapshot remains the first visual source. When it is absent, including in Ponder and before the first display packet, Tier 1 resolves its visual state from the BlockState `status` property and uses the configured minimum RPM. Missing `status` falls back to OFFLINE. Snapshotless GRACE_PERIOD uses the same warning renderer with a fixed 0.75 Hz low-frequency blink. Ponder does not write synthetic display NBT and uses this same resolver for its ACTIVE gear, core, and lights.

`TierOneStabilizerBlock` owns a dedicated `simpleCodec` that reconstructs the Tier 1 subclass and its `facing` contract. The tier remains fixed by the class and is not serialized as a codec field. Tier 2 and Tier 3 continue to use `StabilizerBlock.CODEC`.

## Automated verification

Automated coverage verifies:

- placer-facing, axis, rear-only shaft, rotation, mirror, and status-change invariants;
- absence of `facing` on Tier 2 and Tier 3;
- all 12 blockstate variants and all four static block-model resources;
- removal of the three unused dynamic JSON models, with Java `ModelPart` retained as the dynamic source of truth;
- open-frame texture slots, absence of the former solid central cuboid, and rear bearing geometry reaching `z=16`;
- the static OFFLINE item-model parent and rejection of former Tier 1 placeholder references;
- all 18 referenced textures for dimensions, alpha, visible content, color detail, required transparent backgrounds, and production-JAR inclusion, plus classpath and JAR absence for the eight removed textures;
- snapshot-first visual status, BlockState fallback, configured minimum-RPM fallback, missing-property OFFLINE fallback, and snapshotless Grace blink;
- state colors, Grace frequency thresholds and alpha bounds, signed speed, speed clamp, wrap-safe interpolation, long-running-angle rebasing, and ACTIVE pulse;
- Tier 1 dedicated codec encode/decode, restored subclass and rear shaft behavior, Tier 1 no-occlusion, full-cube collision, and Tier 2/3 codec and occlusion regressions;
- display schema v2 round trips and invalid-input rejection;
- final asset inclusion in the production JAR.

Unit tests, datagen cleanliness, clean build, and all 39 GameTests pass after aligning the default rear shaft with the existing physical fixture.

## Manual verification status

Focused client verification: **PASS**.

- NORTH, EAST, SOUTH, and WEST placement, rotated windows and UVs, rear bearing, rear-only physical kinetic attachment, and Stone/Glass/Iron/Tier 1 adjacency passed.
- OFFLINE dim-red, ACTIVE green/core pulse/local-Z gear motion, and GRACE_PERIOD synchronized yellow warning lights passed without clipping or z-fighting.
- Actual kinetic speeds of positive and negative 32, 64, and 128 RPM passed; frame sequences showed direction reversal, visible speed differences, planar gear motion, and continuous full-turn animation.
- Grace reserves above 60%, near 30%, and near 10% passed with visibly increasing 0.75, 1.5, and 3 Hz cadence bands.
- Japanese with GUI Scale Auto and Fancy graphics, and English with GUI Scale 2 and Fast graphics, both passed ACTIVE and GRACE_PERIOD world rendering.
- Inventory, creative catalog, held, dropped Item Entity, Item Frame, JEI, and Ponder ItemStack rendering passed with the static OFFLINE item model.
- Operation Ponder passed OFFLINE, ACTIVE gear/core/green lights, and GRACE_PERIOD yellow blink; Coverage and Production completed without a scene crash or Frontier Protocol missing texture.

The dedicated server was repeated after the renderer addition. It loaded 3593 recipes, reached `Done` without a renderer client-class error, and stopped normally. The existing optional Farmers Delight recipe parse warning from Spore remains unrelated to R11. Tier 2 and Tier 3 final art, the project logo, licensing decisions, and public distribution remain pending.
