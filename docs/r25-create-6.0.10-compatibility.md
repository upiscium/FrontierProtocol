# R25 Create 6.0.10 Compatibility

## Target

- Minecraft: `1.21.1`
- NeoForge development baseline: `21.1.235`
- NeoForge ModPack validation target: `21.1.247`
- Create artifact: `6.0.10-280`
- Create mod: `6.0.10`
- Create: The Factory Must Grow: `1.2.0`
- Fungal Infection: Spore: `2.2.0j`

## Server and migration verification

- Dependency resolution: PASS
- Clean compilation: PASS
- Unit tests: PASS
- GameTests: PASS
- Dedicated-server smoke: PASS
- Fresh-world startup and restart: PASS
- Alpha.1 world migration and two restarts: PASS

## Client verification

Client validation: PASS

Verified areas:

- client startup
- Stabilizer placement
- Create rotation input
- Stabilization Cell insertion
- Engineer's Goggles integration
- Nixie Tube integration
- Ponder scenes
- models, textures, and localization
- world save and restart

## Conclusion

Frontier Protocol 0.1.1 supports Create 6.0.10 or newer within the
Minecraft 1.21.1 release line, subject to the other declared dependency
constraints.
