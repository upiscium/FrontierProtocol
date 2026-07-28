# R9 TFMG Production Material Audit

## Status

Accepted

## Audited Artifact

- Artifact: `create-tfmg-1.2.0.jar`
- Maven coordinate: `maven.modrinth:create-tfmg:1.2.0`
- TFMG version: `1.2.0`
- SHA-256: `fc824a91cfe22e137c9014ed608ee9bf35f16490d2162b687666332c6eb3a555`
- Fluid Registry authority: `com.drmangotea.tfmg.registry.TFMGFluids`
- Public holder: `TFMGFluids.MOLTEN_PLASTIC`
- Fluid Registry ID: `tfmg:molten_plastic`

The audit used the Gradle-resolved Modrinth artifact, its NeoForge metadata, archive contents, and `javap` output. `TFMGFluids.MOLTEN_PLASTIC` is a public Registrate `FluidEntry`, and the class initializer registers it under `molten_plastic`. Recipe generation accepts its flowing holder, while runtime `FluidStack` values use the entry's public `getSource()` accessor. The loaded registry and RecipeManager GameTest verify the complete source Fluid ID `tfmg:molten_plastic`.

## Mixing Compatibility

Create 6.0.11 exposes `ProcessingRecipeBuilder.require(FlowingFluid, int)`. The Compound datagen provider passes `TFMGFluids.MOLTEN_PLASTIC.get()` and `100`, producing a `neoforge:single` fluid ingredient with exactly 100 mB. The physical Mixer GameTest fills a real Basin fluid capability with the same Fluid holder and verifies that it accepts exactly 100 mB before processing.

## Dependency Decision

Stabilization Compound now requires TFMG Liquid Plastic, so TFMG is a required client and server dependency rather than optional development compatibility. The dependency is resolved from a published Maven artifact; no local or untracked JAR is used.

The previous `spore:biomass_block`, Redstone, Charcoal, Water, and heated Mixing inputs are removed from the Compound recipe. Fungal Infection: Spore remains required because Frontier Protocol still integrates with its infection mutation paths and suppression behavior; only Spore-derived production material was removed.
