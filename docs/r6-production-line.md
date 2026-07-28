# R6 Minimal Create Production Line

R6 adds one finished consumable and exactly three Create recipes. It does not add Tier 2, Tier 3, empty or spent containers, a GUI, Ponder scenes, or custom logistics blocks. Recipe values are provisional and are reserved for balancing in R9.

The common consumable design is recorded in [ADR 0001](adr/0001-stabilizer-consumable-and-production-model.md). [ADR 0004](adr/0004-tfmg-compound-production.md) supersedes the original Compound material selection.

## Content contract

- `frontier_protocol:stabilization_compound` is the only Frontier Protocol production intermediate. A Stabilizer rejects it.
- `frontier_protocol:stabilization_cell` is the common finished consumable. Tier 1 accepts it through its NeoForge item capability, prevents external extraction, and consumes one Cell per configured operating duration.
- `frontier_protocol:tier_1_stabilizer` remains the only Stabilizer block.
- `frontier_protocol:stabilizer_consumables` contains the Cell and does not contain Compound by default.

## Recipes

### Stabilization Compound

- ID: `frontier_protocol:mixing/stabilization_compound`
- Type: `create:mixing`
- Inputs: 100 mB `tfmg:molten_plastic`, one `minecraft:sand`, one `minecraft:blue_ice`, and eight `minecraft:iron_nugget`
- Heat: none
- Output: one `frontier_protocol:stabilization_compound`

TFMG Liquid Plastic supplies the industrial binder. TFMG is therefore required on both client and server. The superseded Spore Biomass decision remains in `r6-production-material-audit.md`; current artifact and Registry evidence is in `r9-tfmg-production-material-audit.md`.

### Stabilization Cell

- ID: `frontier_protocol:deploying/stabilization_cell`
- Type: `create:deploying`
- Belt/Depot processed input: one `create:iron_sheet`
- Deployer-held applied input: one `frontier_protocol:stabilization_compound`
- Output: one `frontier_protocol:stabilization_cell`

The Iron Sheet is the Cell shell. The recipe returns no empty container and does not use Sequenced Assembly.

### Tier 1 Stabilizer

- ID: `frontier_protocol:mechanical_crafting/tier_1_stabilizer`
- Type: `create:mechanical_crafting`
- Output: one `frontier_protocol:tier_1_stabilizer`

```text
ICI
APA
ISI
```

- `I`: `create:iron_sheet`
- `C`: `frontier_protocol:stabilization_cell`
- `A`: `create:andesite_casing`
- `P`: `create:precision_mechanism`
- `S`: `create:shaft`

There are no normal crafting, stonecutting, cooking, smithing, or other alternate Frontier Protocol recipes for these three outputs.

## Automation path

The intended line uses an unheated Basin and Mechanical Mixer with TFMG Liquid Plastic supply for Compound, a Belt or Depot with a Deployer for Cell sealing, and a 3 by 3 Mechanical Crafter arrangement for Tier 1. Standard Create funnels, chutes, belts, or another normal NeoForge item-capability connection can supply Cells to Tier 1. Compound is rejected at the same capability boundary.

GameTests verify the loaded Create recipe serializers, exact ingredients and pattern, output counts, no-heat and fluid contract, absence of alternate recipes, item-capability Cell insertion, Compound rejection, and one-at-a-time consumption. The `r6_create_equipment` GameTest also fills a real Basin with 100 mB Liquid Plastic, executes unheated Mechanical Mixing, and verifies that the one-item output reaches the structure's output chest through Create logistics. Physical Deployer execution, Mechanical Crafter execution, and continuous funnel/chute Cell supply remain manual development-world checks; the implementation is therefore described as designed for full automation rather than fully automation-verified.

## JEI and EMI

Frontier Protocol has no required JEI or EMI dependency. The recipes use Create's standard Mixing, Deploying, and Mechanical Crafting serializers, so a compatible viewer discovers them through Create's categories without custom integration. RecipeManager tests confirm the category-defining serializers, exact 100 mB Liquid Plastic input, eight Iron Nuggets, no heat requirement, outputs, and absence of normal-crafting duplicates.

For development verification, `-PenableRecipeViewerRuntime` adds JEI `19.39.0.370` to `localRuntime` only; it does not change published mod metadata. A graphical Xvfb development world confirmed the revised Compound Mixing view with `tfmg:molten_plastic` at 100 mB, all item inputs, one output, and no heat, plus the unchanged Compound-to-Cell Deploying view. EMI was not tested.
