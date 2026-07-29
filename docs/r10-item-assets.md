# R10 Final Item Assets

R10 completed the non-block art for Stabilization Compound and Stabilization Cell. At the R10 milestone every Stabilizer block asset remained an internal-alpha placeholder; R11 subsequently replaces the Tier 1 placeholder.

## Asset contract

| Item | Resolution | Model parent | Texture |
| --- | ---: | --- | --- |
| Stabilization Compound | 32x32 RGBA | `minecraft:item/generated` | `assets/frontier_protocol/textures/item/stabilization_compound.png` |
| Stabilization Cell | 32x32 RGBA | `minecraft:item/generated` | `assets/frontier_protocol/textures/item/stabilization_cell.png` |

Both textures have a transparent background, no animation metadata, and use the standard generated-item transforms.

## Visual definitions

The Compound is a cooled industrial resin-composite slab. Its wide irregular plate, muted cyan-blue body, ice-white crystalline veins, pale sand inclusions, charcoal iron inclusions, and restrained highlight separate it from a magical crystal, powder pile, bottle, battery, or infected organic material.

The Cell is a disposable iron-sheet cartridge that seals one Compound. Its hard octagonal steel casing, folded seams, press mark, insertion notch, small muted-orange marker, and cyan inspection window distinguish it from the softer Compound silhouette. The window occupies less than one quarter of the casing and does not make the Cell look like a transparent liquid container.

Neither design uses a gemstone silhouette, rune, flame, lightning bolt, radiation mark, infection color, RF/FE gauge, Tier color, Tier number, or reusable-container cap.

## Automated verification

`FinalItemAssetsTest` reads both PNGs with Java `ImageIO`, verifies 32x32 dimensions, an alpha channel, transparent and visible pixels, nontrivial color use, and distinct image contents. It parses both model JSON resources, rejects the old Blaze Powder and Prismarine Crystals references, and opens the production JAR to verify that both models and both textures are packaged.

CI runs unit and asset tests, datagen, a clean-diff check, clean build, and all required GameTests. The internal alpha JAR is retained as a short-lived workflow artifact; no GitHub Release is created.

## In-game verification

Client verification passed in Japanese at GUI Scale Auto and English at GUI Scale 2. Compound and Cell remained distinct at inventory scale and rendered without a purple-black missing texture in the creative inventory, tooltips, JEI production views, Production Ponder controls, the player's hand, a dropped Item Entity, and an Item Frame. The Mixing output, Deploying input/output, and Ponder ItemStack overlays use the new models. Text did not overlap either icon, and no Frontier Protocol texture-atlas or model-load error was emitted.

## Remaining block work

- Tier 2 final block model and textures
- Tier 3 final block model and textures
- Focused in-game verification of the R11 Tier 1 OFFLINE, ACTIVE, and GRACE visual treatment

The Tier 1 replacement is documented in [R11 Tier 1 Final Block Asset](r11-tier1-final-block-asset.md). R10 does not claim that all project art or public-distribution preparation is complete.
