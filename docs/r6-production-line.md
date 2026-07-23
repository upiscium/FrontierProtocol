# R6 Minimal Create Production Line

R6 adds one finished consumable and exactly three Create recipes. It does not add Tier 2, Tier 3, empty or spent containers, a GUI, Ponder scenes, or custom logistics blocks. Recipe values are provisional and are reserved for balancing in R9.

## Content contract

- `frontier_protocol:stabilization_compound` is the only Frontier Protocol production intermediate. A Stabilizer rejects it.
- `frontier_protocol:stabilization_cell` is the common finished consumable. Tier 1 accepts it through its NeoForge item capability, prevents external extraction, and consumes one Cell per configured operating duration.
- `frontier_protocol:tier_1_stabilizer` remains the only Stabilizer block.
- `frontier_protocol:stabilizer_consumables` contains the Cell and does not contain Compound by default.

## Recipes

### Stabilization Compound

- ID: `frontier_protocol:mixing/stabilization_compound`
- Type: `create:mixing`
- Inputs: one `spore:biomass_block`, one `minecraft:redstone`, one `minecraft:charcoal`, and 250 mB `minecraft:water`
- Heat: `heated`
- Output: four `frontier_protocol:stabilization_compound`

The Spore input is the audited infection-derived reactant. Redstone provides control, charcoal provides adsorption/filter media, water is the mixing medium, and heated mixing is the industrial treatment step. See `r6-production-material-audit.md` for the exact artifact evidence.

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

The intended line uses a heated Basin and Mechanical Mixer for Compound, a Belt or Depot with a Deployer for Cell sealing, and a 3 by 3 Mechanical Crafter arrangement for Tier 1. Standard Create funnels, chutes, belts, or another normal NeoForge item-capability connection can supply Cells to Tier 1. Compound is rejected at the same capability boundary.

GameTests verify the loaded Create recipe serializers, exact ingredients and pattern, output counts, heat and fluid contract, absence of alternate recipes, item-capability Cell insertion, Compound rejection, and one-at-a-time consumption. A complete physical Create-machine execution and continuous funnel/chute supply smoke test was not performed in the headless test environment and remains a manual development-world check.

## JEI and EMI

Frontier Protocol has no direct JEI or EMI dependency. The recipes use Create's standard Mixing, Deploying, and Mechanical Crafting serializers, so a compatible viewer can discover them through Create's categories without custom integration. RecipeManager tests confirm the category-defining serializers, inputs, 250 mB water, heated requirement, outputs, and absence of normal-crafting duplicates.

JEI/EMI was not installed in the current development runtime, and no graphical display was available. Visual category rendering and click-through navigation were therefore not executed and are not reported as successful.
