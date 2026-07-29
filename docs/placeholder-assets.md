# Asset Status

## Block assets

| Asset/state | Status and exact vanilla texture references |
| --- | --- |
| Tier 1 offline | Placeholder: sides `minecraft:block/iron_block`; top/bottom `minecraft:block/deepslate_tiles` |
| Tier 1 active | Placeholder: sides `minecraft:block/copper_block`; top `minecraft:block/redstone_block`; bottom `minecraft:block/deepslate_tiles` |
| Tier 1 grace | Placeholder: sides `minecraft:block/exposed_copper`; top `minecraft:block/copper_grate`; bottom `minecraft:block/deepslate_tiles` |
| Tier 2 offline | Placeholder: sides `minecraft:block/cut_copper`; top/bottom `minecraft:block/deepslate_tiles` |
| Tier 2 active | Placeholder: sides `minecraft:block/copper_block`; top `minecraft:block/oxidized_cut_copper`; bottom `minecraft:block/deepslate_tiles` |
| Tier 2 grace | Placeholder: sides `minecraft:block/exposed_cut_copper`; top `minecraft:block/copper_block`; bottom `minecraft:block/deepslate_tiles` |
| Tier 3 offline | Placeholder: sides `minecraft:block/netherite_block`; top `minecraft:block/polished_blackstone`; bottom `minecraft:block/obsidian` |
| Tier 3 active | Placeholder: sides `minecraft:block/gilded_blackstone`; top `minecraft:block/chiseled_polished_blackstone`; bottom `minecraft:block/obsidian` |
| Tier 3 grace | Placeholder: sides `minecraft:block/polished_blackstone_bricks`; top `minecraft:block/netherite_block`; bottom `minecraft:block/obsidian` |

All three Stabilizer block models and textures remain internal-alpha placeholders. Registry IDs and recipes are independent of their future final art.

## Item assets

| Asset | Status | Model and texture |
| --- | --- | --- |
| Stabilization Compound | Final item art | `minecraft:item/generated`; `frontier_protocol:item/stabilization_compound`; custom 32x32 RGBA PNG |
| Stabilization Cell | Final item art | `minecraft:item/generated`; `frontier_protocol:item/stabilization_cell`; custom 32x32 RGBA PNG |

The Stabilization Compound and Stabilization Cell use custom final item textures. Their former Blaze Powder and Prismarine Crystals references are no longer used.

R6 established the Compound, Cell, and Tier 1 placeholders. R7 retains those models and adds Tier 2 and Tier 3 placeholders from the exact vanilla copper, deepslate, obsidian, netherite, and blackstone texture families listed above; it does not replace R6 assets with final art.

R8 retained every placeholder block and item model. R10 replaces only the two non-block item icons; Goggle text, line-based range rendering, and Ponder storyboards add no particle texture or emissive asset.

Production Ponder continues to use Create equipment models and TFMG's own Liquid Plastic rendering from the required dependency. The two R10 item textures are original Frontier Protocol assets and do not copy a TFMG texture.
