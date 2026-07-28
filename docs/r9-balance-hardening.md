# R9 Balance Hardening and Physical Production Verification

R9 fixes the Grace power-cycle exploit, establishes the accepted balance contract, and executes the previously outstanding Create equipment and logistics checks. [ADR 0005](adr/0005-per-cell-grace-budget.md) defines the lifecycle decision.

## Grace contract

- A newly consumed Cell grants its configured ACTIVE duration and one full Grace budget.
- ACTIVE decrements Cell time only. It never increases Grace.
- Power loss, missing rotation, insufficient RPM, or Overstressed operation enters GRACE_PERIOD when Grace remains.
- The transition tick is the first Grace tick. Configured values of 1, 2, and 3 produce exactly 1, 2, and 3 externally observable GRACE_PERIOD ticks.
- Recovery with the same Cell preserves remaining Grace. Repeated power cycling can only reduce it.
- At zero Grace, the machine is OFFLINE without discarding stored Cell time. Rotation can restore ACTIVE operation, but Grace remains zero until the next Cell is consumed.
- ACTIVE provides suppression and cleanup. GRACE_PERIOD provides suppression only. OFFLINE provides neither.
- Saved Grace above the current setting is clamped. Saved Cell time is retained for an already-started Cell, and a new Cell uses the current duration setting.

## R9 defaults

| Setting | Tier 1 | Tier 2 | Tier 3 |
| --- | ---: | ---: | ---: |
| Coverage chunks | 1 | 9 | 25 |
| Minimum RPM | 32 | 64 | 128 |
| Stress impact | 16 | 64 | 256 |
| Cell capacity | 8 | 32 | 64 |
| Cell duration ticks | 6000 | 3000 | 2000 |
| Grace budget ticks per Cell | 1200 | 1800 | 2400 |
| Cell delivery interval | 5 min | 2 min 30 sec | 1 min 40 sec |
| Cleanup interval ticks | 20 | 20 | 20 |
| Inspection budget per cycle/source | 128 | 2048 | 8192 |
| Mutation budget per cycle/source | 4 | 64 | 256 |
| Protected chunk-ticks per Cell | 6000 | 27000 | 50000 |
| Full Cell buffer ticks | 48000 | 96000 | 128000 |
| Full Cell buffer time | 40 min | 80 min | 106 min 40 sec |

The hard global cleanup limits remain 512 inspections and 16 mutations per server tick. A source profile can sponsor more work over a cycle, but it cannot replace or bypass those aggregate caps.

Existing world `serverconfig/frontier_protocol-server.toml` files are not rewritten when code defaults change. Set `tier1GracePeriodTicks = 1200`, `tier2GracePeriodTicks = 1800`, and `tier3GracePeriodTicks = 2400` manually to adopt the R9 defaults. Newly generated config files receive these values automatically.

## Recipe cost

The five-recipe contract is unchanged:

- Compound: 100 mB `tfmg:molten_plastic`, Sand x1, Blue Ice x1, and Iron Nugget x8 produce Compound x1 in an unheated Basin and Mixer.
- Cell: Compound x1 and Iron Sheet x1 produce Cell x1 through Deploying.
- Tier 1: 3x3 `ICI / APA / ISI`, using Iron Sheets x4, Cell x1, Andesite Casings x2, Precision Mechanism x1, and Shaft x1.
- Tier 2: 3x3 `SCS / P1P / SBS`, using Sturdy Sheets x4, Cell x1, Precision Mechanisms x2, Tier 1 Stabilizer x1, and Brass Casing x1.
- Tier 3: 5x5 `SSCSS / SRPRS / CP2PC / SRPRS / SSCSS`, using Sturdy Sheets x12, Cells x4, Railway Casings x4, Precision Mechanisms x4, and Tier 2 Stabilizer x1.

No normal crafting or alternate acquisition recipe exists for these outputs.

## Physical verification

- `r9_unheated_mixing` removes the creative kindled Blaze Burner from Create's audited Mixing fixture, waits two ticks, asserts Basin heat `NONE`, accepts all exact inputs, and delivers exactly one Compound through the output logistics.
- `r9_physical_mechanical_crafting` physically connects and powers 3x3 Tier 1 and Tier 2 arrays and a 5x5 Tier 3 array. All slots are oriented, each input is consumed, exactly one expected output reaches the chest, and the prior-tier upgrade is not returned.
- `r9_continuous_cell_supply` uses Chest-to-Chute delivery rather than direct Stabilizer capability insertion. Tier 2 and Tier 3 run at the default 3000- and 2000-tick Cell durations through two Cell rollovers without GRACE_PERIOD/OFFLINE, duplicate consumption, over-capacity insertion, or suppression-source interruption.
- The complete suite currently passes 39/39 required GameTests. Unit tests cover per-Cell allocation, non-refilling ACTIVE/recovery behavior, repeated cycling, exact Grace tick counts, zero Grace, legacy clamp, and the numeric balance contract.

## Runtime status

- The dedicated server loaded Create, TFMG, Spore, and Frontier Protocol, loaded 3593 recipes, accepted the manually updated 1200/1800/2400 development config, and reached `Done`. The Gradle JavaExec process still required external termination after readiness, so a clean task exit is not claimed.
- The R9 client reached the Japanese title screen and joined the existing creative smoke world under Xvfb without a startup, model, localization-key, or scene-load crash.
- The attempted in-world Tier 1 fixture did not reach ACTIVE because the graphical automation failed to establish its kinetic/logistics setup. The focused Goggle sequence covering partial Grace consumption, recovery with the preserved amount, a second outage, and next-Cell refill remains unverified. Unit tests and GameTests cover the semantics but are not reported as a substitute for this client smoke.

## Boundaries

R9 does not change recipes, Cell duration or capacity, coverage, RPM, Stress, cleanup budgets, suppression targets, Spore Mixins, terrain or nest handling, GUI scope, persistent HUD, final assets, moving contraptions, or chunk loading.
