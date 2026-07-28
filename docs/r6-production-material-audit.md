# R6 Production Material Audit

Status: Superseded

Superseded by: [R9 TFMG Production Material Audit](r9-tfmg-production-material-audit.md)

R6 audits Fungal Infection: Spore `2.2.0j` for Minecraft `1.21.1` and NeoForge from CurseForge file ID `8342823`.

## Audited artifact

- Artifact: `fungal-infection-spore-678295-8342823.jar`
- SHA-256: `0cdb027eb53e6872bdb8de601dc52c176fcb6ddd33a0a2e20a66392937c3d5fe`
- Item registry authority: `com.Harbinger.Spore.core.Sitems`
- Block registry authority: `com.Harbinger.Spore.core.Sblocks`

## Selected material

R6 uses exactly one Spore material: `spore:biomass_block`.

| Check | Evidence |
| --- | --- |
| Item form | `Sitems.BIOMASS_BLOCK` is registered through `Sitems.block(Sblocks.BIOMASS_BLOCK)`, producing the BlockItem for the registered block. |
| Survival acquisition | The embedded `data/spore/loot_table/blocks/biomass_block.json` drops `spore:biomass_block` when the block is broken. |
| Renewable or continuous collection | Spore marks the solid biomass block as naturally generated and infection-spread material, so ongoing infection produces additional collectable blocks. |
| Not boss or one-time loot | Acquisition is from an ordinary spreadable block and its block loot, not a boss or chest-only table. |
| Not equipment or a Block Entity core | `Sblocks.BIOMASS_BLOCK` is a plain block and is separate from biomass lumps, hive spawn, and other living Block Entities. |
| Cleanup distinction | R5 classifies solid `spore:biomass_block` as KEEP. It is not one of the removable foliage entries. |

## Rejected first candidate

`spore:biomass` is a registered standalone `com.Harbinger.Spore.Sitems.Biomass` item. It is craftable from renewable vanilla meat and Spore body parts, but it is not a BlockItem and therefore does not satisfy the explicit R6 first-candidate condition. R6 selects `spore:biomass_block` instead and does not accept any other Spore recipe input.

## Production integration

The generated `frontier_protocol:mixing/stabilization_compound` recipe requires one exact `spore:biomass_block`; it does not use a broad Spore tag. The server RecipeManager GameTest verifies both the exact Registry ID and BlockItem form against the loaded Spore 2.2.0j registry.
