# Stable Readiness for 0.1.0

## Status and platform contract

`0.1.0` is the current stable release objective. It is not available yet;
`0.1.0-alpha.1` remains the latest published prerelease. Stable publication is
blocked until every required gate in
[`0.1.0-stable-gates.md`](releases/0.1.0-stable-gates.md) is complete.

The future target is version `0.1.0`, tag `v0.1.0`, title
`Frontier Protocol 0.1.0`, a non-draft/non-prerelease GitHub release, and
manifest status `public_stable_release`. None of those publication actions occur
as part of this readiness work.

The intended stable platform matrix is deliberately narrow:

| Component | Intended 0.1.0 support |
| --- | --- |
| Minecraft | `1.21.1` |
| Java | `21` |
| NeoForge | `21.1.235` or newer within Minecraft 1.21.1, only where verified |
| Create | `6.0.11` or newer within the audited compatible range |
| Create: The Factory Must Grow | `1.2.0` or newer within the audited compatible range |
| Fungal Infection: Spore | exactly `2.2.0j`, unless a new audit expands support |

Minimum-version declarations are not claims that every later dependency build is
compatible. The stable verification record must identify the exact versions
tested. Spore support cannot expand without a new descriptor and call-site audit.

## Stable 0.1.x guarantees

These are compatibility promises for the implemented `0.1.x` scope. Changing a
listed identifier, persistence field, formula, or semantic requires migration or
a documented compatibility decision.

| Surface | 0.1.x guarantee | Implementation | Evidence |
| --- | --- | --- | --- |
| Registry IDs | Namespace `frontier_protocol`; block IDs `tier_1_stabilizer`, `tier_2_stabilizer`, and `tier_3_stabilizer` remain stable. | `ModBlocks` | Tier registration GameTests and `StabilizerBlockTest` |
| Item IDs | The three Block Item IDs plus `stabilization_compound` and `stabilization_cell` remain stable. | `ModItems` | Registration/recipe GameTests and asset tests |
| Recipe IDs | The five generated IDs under `mixing/stabilization_compound`, `deploying/stabilization_cell`, and `mechanical_crafting/tier_{1,2,3}_stabilizer` remain stable. | `FrontierProtocolRecipeProvider` and generated resources | `ProductionRecipeGameTests`, `MechanicalCraftingGameTests` |
| Server config | Existing key names and documented semantics remain stable; stored world configs are not silently rewritten to new defaults. | `FrontierProtocolServerConfig`, `SuppressionConfigEvents` | `StabilizerBalanceContractTest`, `SuppressionConfigEventsTest`, [R9](r9-balance-hardening.md) |
| Inventory persistence | Stored Cells survive save/load and capacity reduction without deletion. | `StabilizerBlockEntity`, `StabilizerNbt` | `StabilizerNbtTest`, instrumentation GameTests, [R8](r8-graphical-verification.md) |
| Operating status persistence | `OFFLINE`, `ACTIVE`, and `GRACE_PERIOD` serialize by stable names. | `StabilizerNbt`, `StabilizerStatus` | `StabilizerNbtTest`, [R9 restart record](r9-balance-hardening.md) |
| Cell runtime persistence | Remaining Cell ticks survive reload and are clamped only to non-negative values. | `StabilizerNbt`, `StabilizerStateMachine` | `StabilizerNbtTest`, `StabilizerStateMachineTest`, [R9](r9-balance-hardening.md) |
| Grace persistence | Remaining per-Cell Grace survives reload, never refills on recovery, and clamps to the configured maximum. | `StabilizerNbt`, `StabilizerStateMachine` | unit tests, lifecycle GameTests, [R9](r9-balance-hardening.md) |
| Cleanup persistence | Per-dimension chunk cursor, completion, and restart-required state use schema 1 and survive save/load. | `InfectionCleanupSavedData`, `CleanupCursor` | `InfectionCleanupSavedDataTest`, cleanup GameTests |
| Facing and wrench | Horizontal `FACING` persists; a Create wrench rotates it and reconnects the perpendicular kinetic axis. | `StabilizerBlock`, block state | `StabilizerBlockTest`, `stabilizer_directional_shafts`, [R11](r11-stabilizer-casing-redesign.md) |
| Initial spawn | The initial Overworld center persists and supplies dimension-local permanent suppression with configured radius. | `SpawnProtectionSavedData`, `SpawnProtectionManager` | `SpawnProtectionSavedDataTest`, spawn/suppression GameTests |
| Source overlap | Source identity includes tier and position; removing one source cannot remove another source's coverage. | `StabilizerSuppressionSource`, `DimensionSuppressionIndex` | `StabilizerSuppressionSourceTest`, `DimensionSuppressionIndexTest`, overlap GameTests |
| Dimension isolation | Identical chunk coordinates in different dimensions remain independent. | per-level suppression and cleanup indexes | dimension unit tests and Overworld/Nether GameTests |
| Consumable datapack tag | `frontier_protocol:stabilizer_consumables` is public and contains Cell by default; datapacks may extend it. Compound is not included by default. | `ModItemTags`, generated item tag | `tier1` GameTest and recipe tests |
| Comparator | Empty/non-positive capacity is `0`; otherwise `min(15, 1 + floor(14 * min(count, capacity) / capacity))`. | `StabilizerComparatorSignal` | unit tests, instrumentation GameTests, [R15](r15-instrumentation-verification.md) |
| Nixie mapping | `OFFLINE` is red; `ACTIVE` and `GRACE_PERIOD` are green; unrelated Nixies are unchanged. | `StabilizerNixieSignalResolver`, client Mixin | resolver tests and [R15](r15-instrumentation-verification.md) |
| Kinetic input | Rotation is accepted only on the two horizontal faces perpendicular to `FACING`; front/back are rejected. | `StabilizerBlock#getRotationAxis` | block tests, `stabilizer_directional_shafts`, [R11](r11-stabilizer-casing-redesign.md) |
| Suppression API | `InfectionSuppressionApi#get()` and its query interface remain the intentionally public Java API; registration lifecycle remains internal. | `api/suppression` | suppression index and geometry tests; no external-consumer compatibility fixture yet |
| Commands | Frontier Protocol exposes no intentionally public command in 0.1.0. | production source inspection | No command registration exists; add a contract entry before adding one. |
| Client config | Existing display and range-overlay key names retain their meanings. | `FrontierProtocolClientConfig` | tooltip, display, and range tests; [R8](r8-graphical-verification.md) |
| Datapack surfaces | The consumable tag and five recipe IDs above are public. Cleanup block tags remain implementation allowlists, not a promise to clean arbitrary blocks. | generated tags and recipes | datagen cleanliness and recipe/GameTest coverage |
| Packwiz distribution | Assets are `frontier_protocol-${version}.jar`, matching `.sha256`, `release-manifest.json`, and `LICENSE`; each version uses an immutable tag URL. | release workflow and Gradle candidate tasks | offline release safeguards; alpha packwiz smoke record |

## Classified alpha boundaries

Every boundary from `docs/alpha-release.md` has one classification:

| Boundary | Classification | Stable interpretation |
| --- | --- | --- |
| Existing infection, structures, nests, hazards, and Block Entities are not restored, removed, replaced, or frozen; cleanup is allowlisted foliage only. | Supported limitation | Stable containment prevents selected new mutations and performs bounded foliage cleanup; it is not world repair. |
| Compound cannot power a Stabilizer; Cells and the extensible public consumable tag are the supply contract. | Supported limitation | This is intentional production and datapack behavior. |
| Current recipe quantities and operating values require an explicit decision before change. | Supported limitation | Stable config semantics and IDs are guaranteed; balance changes need separate review. |
| Final item and static casing artwork is used without dynamic machine parts. | Supported limitation | Dynamic rendering is not required for stable reliability. |
| Hostile movement, combat, block breaking, and explosions are outside containment. | Supported limitation | Physical defense remains player responsibility. |
| Existing Spore random ticks, scheduled ticks, and infected Block Entity state continue. | Supported limitation | Stable does not freeze existing infection lifecycle. |
| World-generation paths outside selected initial-spawn ore handling and audited runtime Spore paths are not globally intercepted. | Supported limitation | No broad world-write hook is promised. |
| GUI, HUD, particles, custom tab, tier-specific/Empty/Spent Cells, new materials, multiblock, moving-contraption suppression, chunk loading, terrain restoration, nest handling, and mob handling are absent. | Future feature | None is a blocker unless implemented behavior is shown broken. |
| No Minecraft-wide `Level#setBlock` hook is used. | Supported limitation | Narrow target Mixins are an intentional safety boundary. |

No accepted known issue is currently documented for stable operation. Missing
evidence below is not silently converted into a known issue; it remains a gate.

## Blocker evidence audit

`PASS` means the cited retained evidence or code inspection covers the stated
area. `NOT VERIFIED` is not a failure claim; it blocks stable publication until
the follow-up produces retained evidence.

| Area | Evidence | Result | Blocker | Follow-up |
| --- | --- | --- | --- | --- |
| Fresh-world startup | `dedicatedServerSmoke` deletes its server directory, reaches canonical `Done`, and stops cleanly. | PASS | No | Repeat on the final RC/stable candidate. |
| Initial-spawn initialization | [R19 startup matrix](r19-stable-startup-matrix.md) directly inspects schema 2 SavedData after two clean starts and retains one initialized Overworld center. | PASS | No | Repeat the same automated procedure on the final candidate for the separate ledger gate. |
| Upgrade from a 0.1.0-alpha.1 world | [R20 migration fixture](r20-alpha1-world-migration.md) was created and restarted twice with the exact published Alpha JAR, then loaded, saved, and restarted twice with the current packaged JAR. | PASS | No | Rerun the retained procedure on the RC/final candidate for the separate ledger gate. |
| Dedicated-server restart persistence | [R9 manual verification](r9-balance-hardening.md) records status, Cell time, and Grace across normal restart. | PASS | No | Repeat with an alpha.1 world on the RC. |
| Client reconnect synchronization | [R9](r9-balance-hardening.md) records reconnect restoration; display sync has unit/GameTest coverage. | PASS | No | Repeat focused RC smoke. |
| Tier 1/2/3 normal operation | Tier lifecycle GameTests cover all tiers; all required tests pass in CI. | PASS | No | Run on final candidate. |
| Insufficient RPM | Unit tests and [R9 manual lifecycle verification](r9-balance-hardening.md). | PASS | No | Repeat focused RC smoke. |
| Overstressed operation | Unit tests plus [R8](r8-graphical-verification.md) and [R9](r9-balance-hardening.md). | PASS | No | Repeat focused RC smoke. |
| Grace lifecycle | State-machine tests, lifecycle GameTests, and [R9](r9-balance-hardening.md). | PASS | No | Run final candidate suite. |
| Exact Cell rollover | Continuous Tier 2/3 GameTests and [R9](r9-balance-hardening.md) record exactly one Cell consumed without interruption. | PASS | No | Run final candidate suite. |
| Empty buffer | State-machine and Tier 1 GameTests; comparator instrumentation checks zero. | PASS | No | Run final candidate suite. |
| Over-capacity inventory | NBT/unit and physical comparator GameTests plus [R8](r8-graphical-verification.md). | PASS | No | Include in upgrade smoke. |
| Wrench rotation | Block tests, directional shaft GameTest, and [R11](r11-stabilizer-casing-redesign.md). | PASS | No | Run final candidate suite. |
| Chunk unload/reload | Stabilizer cleanup coverage-expansion GameTests exercise unload/reload behavior. | PASS | No | Run final candidate suite. |
| Suppression overlaps | Suppression index tests and Tier 1/Tier 3 overlap GameTests. | PASS | No | Run final candidate suite. |
| Dimension isolation | Dimension index tests and Nether GameTests for Tier 2/Tier 3. | PASS | No | Run final candidate suite. |
| Cleanup cursor persistence | `InfectionCleanupSavedDataTest`, cursor tests, and cleanup reload GameTests. | PASS | No | Include in alpha-world upgrade smoke. |
| Global cleanup budgets | `CleanupBudgetTest`, round-robin/index tests, and cleanup service GameTests. | PASS | No | Run final candidate suite. |
| Spore descriptors/call counts | Exact 2.2.0j audit, complete descriptors, `require` counts, and Spore integration GameTests. | PASS | No | Re-audit before any Spore expansion. |
| Missing dependency failure messages | [R19 startup matrix](r19-stable-startup-matrix.md) launches the production JAR without Create, TFMG, and Spore in turn and retains clear requester/range diagnostics before world creation. | PASS | No | Preserve the required metadata and rerun on dependency changes. |
| Malformed config handling | [R19 startup matrix](r19-stable-startup-matrix.md) records named malformed-TOML recreation and safe range correction for capacity, RPM, duration, radius, and cleanup budget values. | PASS | No | Preserve the matrix in normal CI. |
| Datagen cleanliness | Build workflow runs `runData` and requires a clean generated-resource status. | PASS | No | Repeat after final version bump. |
| Production-JAR contents | `verifyReleaseJar` checks required metadata/classes/assets/licenses and forbidden nested/dependency content. | PASS | No | Run on final candidate. |
| English and Japanese text | [R8 graphical verification](r8-graphical-verification.md), R10/R11 records, and language/resource tests. | PASS | No | Complete both final-candidate graphical gates. |
| Packwiz install path | [Alpha publication record](releases/0.1.0-alpha.1-publication.md) records public URL add/refresh/JAR checks. | PASS | No | Repeat against RC and stable URLs. |
| Licensing and notices | [Licensing audit](licensing.md) and `verifyReleaseJar` check BSD-3-Clause plus retained Create/NeoForge notices. | PASS | No | Run final candidate verification. |
| Old systems absent | Production source inspection, `oldGameplayContentIsNotRegistered`, and JAR dependency-prefix/nested-JAR checks find no active sector, custom infection, mob scaling, breach, nutrition, resource-node, or oil system. | PASS | No | Inspect final JAR class/resource listing. |
| Normal-operation log volume | Debug logging defaults false, but no retained bounded long-run log-volume measurement exists. | NOT VERIFIED | Yes | Run an RC soak and inspect Frontier Protocol log rate. |
| Duplication risks | Physical crafting and continuous Cell rollover assert consumption/output counts and no duplicate Cell consumption. | PASS | No | Exercise upgrade and RC soak. |
| Item-loss risks | [R20](r20-alpha1-world-migration.md) retains exact pre/first/second counts for five chest item IDs and all three internal Cell inventories, including the Tier 1 capacity boundary and absence of duplicate dropped items. | PASS | No | Rerun on the RC/final candidate. |
| World-corruption risks | [R20](r20-alpha1-world-migration.md) directly reads level, block/entity/POI region chunks, documented BlockStates/Block Entities, and both SavedData files after both migrated saves. | PASS | No | Rerun on the RC/final candidate. |
| Malformed-config crash-loop recovery | [R19 startup matrix](r19-stable-startup-matrix.md) shows NeoForge recreates the invalid file and reaches `Done` in the same isolated directory. | PASS | No | Preserve the matrix in normal CI. |
| Upgraded-world restart recovery | [R20](r20-alpha1-world-migration.md) records two clean current-candidate starts against the same migrated Alpha world with unchanged fixture state and readable persistence. | PASS | No | Rerun on the RC/final candidate. |
| Silent suppression-bypass risks | Required Mixin call counts fail startup on drift; target-path and lifecycle GameTests cover audited Spore 2.2.0j. | PASS | No | Preserve exact Spore pin and repeat RC suite. |
| Cross-dimension contamination | Dimension-local indexes, unit tests, and Nether/Overworld GameTests. | PASS | No | Run final candidate suite. |

## Stable release sequence

1. Fix demonstrated stable blockers.
2. Complete compatibility and alpha.1-world upgrade verification.
3. Bump to `0.1.0-rc.1` in a dedicated release change.
4. Publish `v0.1.0-rc.1` as a prerelease.
5. Perform an RC soak and modpack test.
6. Fix RC blockers.
7. Bump to `0.1.0`.
8. Freeze stable release notes and documentation.
9. Publish `v0.1.0` as a non-draft, non-prerelease release.
10. Perform post-publication packwiz, checksum, and byte-identity verification.

The pre-publication packwiz gate must use either the `v0.1.0-rc.1` public URL or
a disposable local HTTP server for the final candidate. It does not require the
future stable URL.

Post-publication stable packwiz procedure:

1. After `v0.1.0` is published, run `packwiz url add` against
   `https://github.com/upiscium/FrontierProtocol/releases/download/v0.1.0/frontier_protocol-0.1.0.jar`.
2. Verify the generated URL, hash format, and non-empty hash.
3. Run `packwiz refresh` and confirm that it preserves the stable URL.
4. Record the result and JAR-structure checks in the stable publication record.

`0.1.0-rc.1` is recommended. The project has one public alpha and relies on
narrow Mixins into one exact third-party Spore build; the current audit does not
provide strong evidence that an RC is unnecessary.
