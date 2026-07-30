# R20 Alpha.1 World Migration

Verification date: `2026-07-30`

## Provenance

The committed fixture was generated from the immutable public Alpha Release,
not from a locally rebuilt approximation.

| Field | Verified value |
| --- | --- |
| Version | `0.1.0-alpha.1` |
| Tag | `v0.1.0-alpha.1` |
| Source commit | `fed467ec0cd52a936f06751cd922efcc259914a1` |
| JAR URL | `https://github.com/upiscium/FrontierProtocol/releases/download/v0.1.0-alpha.1/frontier_protocol-0.1.0-alpha.1.jar` |
| Checksum URL | `https://github.com/upiscium/FrontierProtocol/releases/download/v0.1.0-alpha.1/frontier_protocol-0.1.0-alpha.1.jar.sha256` |
| Published JAR SHA-256 | `60cb68f625eab26e8a37fe86eed125c52a1c00efca5dcae85ff3db5085274a8f` |
| Fixture archive | `world-fixture.zip` |
| Fixture archive SHA-256 | `fbb996b918c34a317bda9eb84a6d3d87bb836fc9e523b779ff1fdf5c780ae886` |
| Fixture archive size | `1,240,514` bytes |
| Enforced size ceiling | `16,777,216` bytes |
| Manifest SHA-256 | `e9b80d9fc5789b0d50a4ebb57a91eadd0cd159dadb61b3a98a88e2cac9944259` |
| Fixed level seed | `8675309` |

The fixture runtime used Java `21`, Minecraft `1.21.1`, NeoForge `21.1.235`,
Create `6.0.11-295` (mod `6.0.11`), TFMG `1.2.0`, and Spore `2.2.0j`
(file `8342823`).

## Generation

`generateAlpha1MigrationFixture -PallowAlpha1FixtureNetwork=true` is the only
network-enabled path. It downloads the published JAR and checksum into `build/`,
verifies the exact filename, SHA-256, packaged mod version, metadata, and
integration classes, and starts the Alpha server twice:

1. The first Alpha start loads a generation-only helper mod, creates the bounded
   fixture state, reaches canonical `Done`, accepts `stop`, and exits `0`.
2. The helper JAR is removed.
3. The second Alpha start loads the same world using only the published Alpha
   and pinned dependencies, reaches `Done`, accepts `stop`, and exits `0`.
4. Direct NBT/region inspection accepts the post-second-restart state.
5. Only the sanitized `world/` files are archived deterministically.

The helper has its own `fixtureBuilder` source set. It is absent from the
production JAR, ordinary test runtime, committed fixture, normal Build CI, and
Release CI. `promoteAlpha1MigrationFixture -PpromoteAlpha1Fixture=true` is a
separate explicit review/promotion step.

The archive excludes `session.lock`, logs, crash reports, mods, libraries,
caches, downloaded binaries, playerdata, stats, and advancements. It contains
no Alpha JAR, dependency JAR, personal account data, local path, or IP address.

## Fixture State

The documented blocks are in Overworld chunk `(1,5)` at Y `100`.

| Machine | Position | Facing | Status | Cells | Cell ticks | Grace ticks | Registered radius |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: |
| Tier 1 | `(20,100,84)` | north | `offline` | 8 | 0 | 0 | 0 |
| Tier 2 | `(22,100,84)` | east | `grace_period` | 4 | 1800 | 1000 | 1 |
| Tier 3 | `(24,100,84)` | south | `grace_period` | 12 | 1200 | 2000 | 2 |

Tier 1 is exactly at its configured capacity boundary. All three Block IDs are
`frontier_protocol:tier_{1,2,3}_stabilizer`, all Block Entities are
`frontier_protocol:stabilizer`, and schema-1 tier values match block identity.

The chest at `(20,100,87)` records these exact stacks:

| Item | Count |
| --- | ---: |
| `frontier_protocol:stabilization_compound` | 11 |
| `frontier_protocol:stabilization_cell` | 13 |
| `frontier_protocol:tier_1_stabilizer` | 1 |
| `frontier_protocol:tier_2_stabilizer` | 2 |
| `frontier_protocol:tier_3_stabilizer` | 3 |

The total documented Cell contract is 37: 24 internal Cells plus 13 chest
Cells. No Frontier Protocol dropped-item entity exists in the fixture or either
migrated save.

Spawn SavedData is schema `2`, `initialized=true`, center chunk `(1,5)`, and
exactly one `frontier_protocol_spawn_protection.dat` record. Cleanup SavedData
is schema `1` with chunk `(7,-3)`, section index `2`, local block index `321`,
`completed=false`, `restartRequired=true`, minimum section `-4`, and section
count `24`.

## Migration Results

`alpha1WorldUpgradeSmoke --offline` verifies manifest/archive hashes and
provenance, checks the archive allowlist before extraction, directly inspects
the source world, installs the current packaged production JAR, and performs two
current-candidate starts against the same migrated world.

| State | Tier 1 | Tier 2 | Tier 3 | Chest items | Spawn | Cleanup cursor |
| --- | --- | --- | --- | --- | --- | --- |
| Pre-migration | `offline`, 8 Cells, 0/0 ticks | `grace_period`, 4 Cells, 1800/1000 ticks | `grace_period`, 12 Cells, 1200/2000 ticks | exact | schema 2, `(1,5)` | schema 1, `2/321` |
| First current save | unchanged | unchanged | unchanged | exact | unchanged | unchanged |
| Second current save | unchanged | unchanged | unchanged | exact | unchanged | unchanged |

The short unpowered starts produced no legitimate Cell consumption or timer
movement. The harness nevertheless permits only non-increasing Cell/Grace
timers and rejects refill, negative values, unknown status, tier/radius changes,
inventory loss/duplication, and capacity-boundary truncation.

After each save, direct inspection reads `level.dat`, every occupied Overworld
block/entity/POI region chunk, all documented block-state palettes and Block
Entities, both SavedData files, and exact item IDs/counts. Both starts reached
`Done`, stopped normally, and exited `0`; no crash report or migration-fatal
NBT, registry, DataFixer, chunk, Block Entity, Mixin, or SavedData diagnostic
occurred. The committed archive remained byte-identical.

Generated evidence is retained below `build/alpha1-world-upgrade/`:

- `server/current-first-start.log`
- `server/current-second-start.log`
- `alpha1-world-upgrade-results.properties`

## Remaining Boundaries

This verifies the development `0.1.0-alpha.1` candidate against a real published
Alpha fixture. The `alpha1-world-upgrade` Stable gate remains `INCOMPLETE` until
the same procedure is rerun and frozen on `0.1.0-rc.1` or the final `0.1.0`
candidate. The long-running RC log-volume soak remains `NOT VERIFIED`, as do the
final graphical, packwiz, release-note, license/provenance, and publication
gates.
