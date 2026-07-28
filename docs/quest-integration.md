# Quest Integration

Frontier Protocol does not depend on FTB Quests, Heracles, Better Questing, or another quest system. Modpack authors can copy the stable IDs and progression text below into their preferred system.

## Stable IDs

Items and blocks:

- `frontier_protocol:stabilization_compound`
- `frontier_protocol:stabilization_cell`
- `frontier_protocol:tier_1_stabilizer`
- `frontier_protocol:tier_2_stabilizer`
- `frontier_protocol:tier_3_stabilizer`

Public item tag:

- `frontier_protocol:stabilizer_consumables`

Recipes:

- `frontier_protocol:mixing/stabilization_compound`
- `frontier_protocol:deploying/stabilization_cell`
- `frontier_protocol:mechanical_crafting/tier_1_stabilizer`
- `frontier_protocol:mechanical_crafting/tier_2_stabilizer`
- `frontier_protocol:mechanical_crafting/tier_3_stabilizer`

## Suggested quest chain

### 1. Initial Refuge

Explanation-only objective:

> Initial Overworld spawn is covered by permanent infection suppression. The persisted initial center does not move when world spawn later changes. Its default radius is two chunks, covering 5x5 chunks. It needs no Cell or Create rotation, but performs no progressive cleanup, removes no existing nests, and does not stop hostile mobs. It is an initial refuge, not a complete safe zone.

### 2. Stabilization Compound

Obtain `frontier_protocol:stabilization_compound` by Mixing 100 mB TFMG Liquid Plastic with Sand, Blue Ice, and eight Iron Nuggets without heat.

### 3. Seal the Cell

Obtain `frontier_protocol:stabilization_cell` by Deploying Compound onto an Iron Sheet. Compound is an intermediate and cannot power a Stabilizer directly.

### 4. Field Containment

Obtain or place `frontier_protocol:tier_1_stabilizer`. Supply rotation and Cells, then verify ACTIVE operation with Engineer's Goggles. Each consumed Cell grants one finite Grace budget; restoring rotation preserves the remaining budget and only the next consumed Cell replenishes it.

### 5. Automated Supply

Build Create logistics that continuously deliver Stabilization Cells. Tier 2 needs delivery every 3000 ticks and Tier 3 every 2000 ticks. Quest systems that cannot reliably detect uninterrupted ACTIVE operation should use a manual check rather than infer operation from possession alone.

### 6. Industrial Containment

Obtain `frontier_protocol:tier_2_stabilizer` through the staged Tier 1 upgrade recipe.

### 7. Regional Stabilization

Obtain `frontier_protocol:tier_3_stabilizer` through the staged Tier 2 upgrade recipe.

## Rewards

Frontier Protocol does not prescribe quest rewards. Modpack authors should fit rewards to their own economy; this guide does not recommend unlimited Cells or rare production materials.
