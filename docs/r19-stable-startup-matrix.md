# R19 Stable Startup Matrix

Verification date: `2026-07-30`

## Scope

`stableStartupMatrix` extends the existing ModDev dedicated-server launch
infrastructure with isolated production-JAR scenarios under
`build/stable-startup-matrix/`. It has three focused tasks:

- `freshWorldInitializationSmoke`
- `dependencyFailureSmoke`
- `serverConfigRecoverySmoke`

Every successful server start must reach the canonical `Done` line, accept the
normal console `stop` command, and exit with status `0`. Startup and shutdown
waits are bounded, a timeout is always a failure, and the harness terminates the
server and its descendants on every exit path. Result summaries contain only
relative evidence paths.

The server scenarios use the packaged `frontier_protocol-0.1.0-alpha.1.jar` and
Gradle's already-resolved runtime artifacts. They do not download dependencies
or modify tracked resources during execution.

## Versions

| Component | Verified version |
| --- | --- |
| Java | `21` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.235` |
| Create | `6.0.11` (`6.0.11-295` artifact) |
| Create: The Factory Must Grow | `1.2.0` |
| Fungal Infection: Spore | `2.2.0j` (file `8342823`) |
| Frontier Protocol | `0.1.0-alpha.1` |

## Results

| Scenario | Expected | Actual | Retained build evidence |
| --- | --- | --- | --- |
| Fresh world, first start | `Done`, normal stop, status `0`, initialized center persisted | PASS | `fresh-world/first-start.log` and `fresh-results.properties` |
| Same fresh world, second start | Same center, one SavedData record, clean restart | PASS | `fresh-world/second-start.log` and `fresh-results.properties` |
| Create absent | Terminal loader diagnostic before world creation | PASS | `missing-create/startup.log` |
| TFMG absent | Terminal loader diagnostic before world creation | PASS | `missing-tfmg/startup.log` |
| Spore absent | Terminal loader diagnostic before world creation | PASS | `missing-spore/startup.log` |
| Invalid Frontier Protocol TOML | Named parse warning, deterministic recreation, then clean startup | PASS | `malformed-config/invalid-start.log` |
| Out-of-range numeric values | Values corrected into declared ranges, then clean startup | PASS | `out-of-range-config/bootstrap-start.log`, `out-of-range-config/corrected-start.log`, and `config-results.properties` |

All paths in the table are relative to `build/stable-startup-matrix/`. Runtime
worlds, configs, caches, binaries, and full logs remain generated build outputs
and are not committed.

## Persisted Center

The fixed seed `8675309` produced one Overworld SavedData record at
`world/data/frontier_protocol_spawn_protection.dat`:

- dimension: `minecraft:overworld`, inferred from the authoritative Overworld
  data location rather than a nonexistent NBT field;
- schema: `2`;
- center chunk: `(1, 5)`;
- `initialized`: `true`;
- radius state: not persisted in this schema; the effective radius comes from
  `frontier_protocol-server.toml`.

Direct compressed-NBT inspection after both shutdowns found the same values and
exactly one matching SavedData file.

## Dependency Diagnostics

The packaged `META-INF/neoforge.mods.toml` remained authoritative. Removing one
resolved dependency JAR at a time produced these concise loader diagnostics:

```text
Mod ID: 'create', Requested by: 'frontier_protocol', Expected range: '[6.0.11,)', Actual version: '[MISSING]'
Mod ID: 'tfmg', Requested by: 'frontier_protocol', Expected range: '[1.2.0,)', Actual version: '[MISSING]'
Mod ID: 'spore', Requested by: 'frontier_protocol', Expected range: '[2.2.0j,2.2.0j]', Actual version: '[MISSING]'
```

None reached `Done`, created `world/level.dat`, or fell through to a
`ClassNotFoundException`, `NoClassDefFoundError`, or Mixin application crash.

## Config Behavior

NeoForge loads the registered Frontier Protocol server config from the isolated
server's `config/frontier_protocol-server.toml`. The malformed fixture emitted:

```text
Failed to load config frontier_protocol-server.toml: ... ParsingException: Invalid integer: not valid. Attempting to recreate
```

NeoForge recreated a valid file, reached `Done`, and stopped normally in the
same directory. Because this path recovered without a rejected startup, no
second recovery launch was required.

The range scenario first generated a valid file, then supplied both invalid low
and high values. NeoForge corrected the effective file to:

| Key | Invalid fixture | Corrected value | Declared range |
| --- | ---: | ---: | ---: |
| `tier1CellCapacity` | `0` | `1` | `1..64` |
| `tier3MinimumRpm` | `999` | `256` | `1..256` |
| `tier1CellDurationTicks` | `0` | `1` | `1..72000` |
| `spawnProtectionRadiusChunks` | `99` | `16` | `0..16` |
| `cleanupGlobalMutationBudgetPerTick` | `0` | `1` | `1..4096` |

Existing `StabilizerNbtTest` and instrumentation coverage remain authoritative
for the separate invariant that reducing configured capacity does not delete
persisted Cells. The startup matrix does not create gameplay inventory merely
to duplicate that coverage.

## Remaining Boundaries

This is development-version evidence, not final-candidate evidence. It does not
complete `fresh-world-smoke` in the Stable ledger. The following remain `NOT
VERIFIED` blockers for later work:

- real `0.1.0-alpha.1` world upgrade and second-restart recovery;
- migration item-loss and world-corruption validation;
- final RC/stable-candidate rerun of the fresh-world procedure;
- long-running RC log-volume soak;
- final-candidate graphical, packwiz, release-note, and publication gates.
