# Stable Readiness for 0.1.0

## Status and platform contract

The latest public version is `0.1.0-rc.1`. Current source is the release-ready
`0.1.0` Stable candidate. Every pre-publication gate in
[`0.1.0-stable-gates.md`](releases/0.1.0-stable-gates.md) is complete. The Stable
tag and GitHub Release are not published.

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
| Packwiz distribution | Assets are `frontier_protocol-${version}.jar`, matching `.sha256`, `release-manifest.json`, and `LICENSE`; each version uses an immutable tag URL. | release workflow and Gradle candidate tasks | offline release safeguards; accepted [RC public-URL Packwiz smoke](releases/0.1.0-rc.1-publication.md) |

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

No accepted known issue is currently documented for Stable operation, and no
pre-publication evidence blocker remains.

## Final evidence audit

[Issue #33](https://github.com/upiscium/FrontierProtocol/issues/33), original
evidence `5144235666`, focused supplement `5148923492`, and independent
acceptance `5148981054` provide the exact-main final-candidate evidence.
[R24](r24-stable-gate-closeout.md) preserves the closeout and byte-identity
bridge.

| Area | Final evidence | Result | Blocker |
| --- | --- | --- | --- |
| Exact-main automated suite | Build run `30619876967`, job `91121675163`, accepted in Issue #33 | PASS | No |
| Fresh-world startup and initialization | Final-candidate `dedicatedServerSmoke` and R19 matrix in accepted Build run | PASS | No |
| Upgrade from a 0.1.0-alpha.1 world | Final-candidate R20 migration and second restart in accepted Build run | PASS | No |
| Upgraded-world restart recovery | Exact item, Stabilizer, SavedData, and region checks in the final-candidate migration run | PASS | No |
| Unit, asset, datagen, build, and GameTests | Accepted exact-main Build run | PASS | No |
| English graphical smoke | Issue #33 original evidence plus focused supplement | PASS | No |
| Japanese graphical smoke | Separate Japanese client process in Issue #33 plus focused supplement | PASS | No |
| Insufficient RPM, lifecycle, Nixie, and comparator | Focused English/Japanese evidence accepted in comment `5148981054` | PASS | No |
| Production-JAR contents | Accepted artifact `8789117654`, exact four-file set, no nested/dependency classes | PASS | No |
| Licensing and notices | Root BSD-3-Clause license and retained Create/NeoForge notices; Issue #33 and R24 | PASS | No |
| Packwiz install path | Accepted immutable RC public-URL smoke in the [RC publication record](releases/0.1.0-rc.1-publication.md) and [Issue #28](https://github.com/upiscium/FrontierProtocol/issues/28) | PASS | No |
| Normal-operation log volume | [Issue #27](https://github.com/upiscium/FrontierProtocol/issues/27) and the RC publication record retain zero Frontier Protocol steady-state lines | PASS | No |

## Stable release sequence

1. `0.1.0-rc.1` version preparation, prerelease publication, retained soak, and
   public Packwiz candidate smoke are complete; see the
   [RC publication record](releases/0.1.0-rc.1-publication.md).
2. The source version is prepared as the unpublished `0.1.0` internal Stable
   candidate.
3. Exact-main automated evidence and both graphical smoke gates are complete in
   [Issue #33](https://github.com/upiscium/FrontierProtocol/issues/33).
4. Final gate closeout, release notes, changelog, and license/provenance freeze
   are complete in [R24](r24-stable-gate-closeout.md).
5. Create tag `v0.1.0` and let the generic workflow publish a non-draft,
   non-prerelease release.
6. Record workflow checksum/byte identity and perform post-publication Packwiz
   verification.

The pre-publication packwiz gate must use either the `v0.1.0-rc.1` public URL or
a disposable local HTTP server for the final candidate. It does not require the
future stable URL.

Post-publication stable packwiz procedure:

1. After `v0.1.0` is published, run `packwiz url add` against the Stable JAR URL
   documented in [`0.1.0.md`](releases/0.1.0.md).
2. Verify the generated URL, hash format, and non-empty hash.
3. Run `packwiz refresh` and confirm that it preserves the stable URL.
4. Record the result and JAR-structure checks in the stable publication record.

`0.1.0-rc.1` remains the latest public version until step 5. The only remaining
work is tag creation, the Release workflow, and post-publication verification.
