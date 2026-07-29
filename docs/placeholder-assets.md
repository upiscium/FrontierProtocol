# Asset Status

## Block assets

| Asset/state | Status and texture references |
| --- | --- |
| Tier 1 offline | Final: custom Frontier Protocol casing, face, ring, and dim red light textures |
| Tier 1 active | Final: custom Frontier Protocol active face/ring textures with rotating core, gear, green status light, and core pulse |
| Tier 1 grace | Final: custom Frontier Protocol grace face/ring textures with ratio-driven yellow warning light |
| Tier 2 offline | Placeholder: sides `minecraft:block/cut_copper`; top/bottom `minecraft:block/deepslate_tiles` |
| Tier 2 active | Placeholder: sides `minecraft:block/copper_block`; top `minecraft:block/oxidized_cut_copper`; bottom `minecraft:block/deepslate_tiles` |
| Tier 2 grace | Placeholder: sides `minecraft:block/exposed_cut_copper`; top `minecraft:block/copper_block`; bottom `minecraft:block/deepslate_tiles` |
| Tier 3 offline | Placeholder: sides `minecraft:block/netherite_block`; top `minecraft:block/polished_blackstone`; bottom `minecraft:block/obsidian` |
| Tier 3 active | Placeholder: sides `minecraft:block/gilded_blackstone`; top `minecraft:block/chiseled_polished_blackstone`; bottom `minecraft:block/obsidian` |
| Tier 3 grace | Placeholder: sides `minecraft:block/polished_blackstone_bricks`; top `minecraft:block/netherite_block`; bottom `minecraft:block/obsidian` |

Tier 1 uses final custom block assets. Tier 2 and Tier 3 remain internal-alpha placeholders. Registry IDs and recipes are independent of their future final art.

## Item assets

| Asset | Status | Model and texture |
| --- | --- | --- |
| Stabilization Compound | Final item art | `minecraft:item/generated`; `frontier_protocol:item/stabilization_compound`; custom 32x32 RGBA PNG |
| Stabilization Cell | Final item art | `minecraft:item/generated`; `frontier_protocol:item/stabilization_cell`; custom 32x32 RGBA PNG |

The Stabilization Compound and Stabilization Cell use custom final item textures. Their former Blaze Powder and Prismarine Crystals references are no longer used.

R6 established the Compound, Cell, and Tier 1 placeholders. R7 retains those models and adds Tier 2 and Tier 3 placeholders from the exact vanilla copper, deepslate, obsidian, netherite, and blackstone texture families listed above; it does not replace R6 assets with final art.

R8 retained every placeholder block and item model. R10 replaces only the two non-block item icons. R11 replaces the Tier 1 placeholder with four static block-model resources, 26 custom 32x32 RGBA textures, 12 facing/status variants, and Java `ModelPart` animated parts. Goggle text, line-based range rendering, and Ponder storyboards add no particle texture.

Production Ponder continues to use Create equipment models and TFMG's own Liquid Plastic rendering from the required dependency. The R10 item textures and R11 Tier 1 textures are Frontier Protocol assets and do not copy Vanilla, Create, TFMG, or Spore textures.
