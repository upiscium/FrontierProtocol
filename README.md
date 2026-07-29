# Frontier Protocol

Frontier Protocol is a NeoForge addon for Fungal Infection: Spore, Create, and
Create: The Factory Must Grow (TFMG).
It introduces Create-powered containment infrastructure that suppresses Spore
infection across entire chunks, from the bottom to the top of the dimension.
Players can use Create production and logistics to make the common Stabilization
Cells and supply one of three stationary Stabilizer tiers, allowing sustainable
settlements and industrial bases to exist inside an infected world.

Containment does not prevent hostile mobs from entering protected areas.
Physical defense remains the player's responsibility. Resource generation and
infinite ore extraction are intentionally delegated to other Create addons.

Frontier Protocolは、Fungal Infection: SporeとCreateを統合するNeoForge向け
アドオンMODです。Createの動力、生産設備および物流を利用して、チャンク全高に
わたるSpore感染抑制設備を継続運転できます。3段階のStabilizerと全Tier共通の
安定化セルはCreate加工で製造します。安定化化合物は中間素材であり、Stabilizerへ
直接投入できません。

抑制範囲は感染の生成と拡大を防ぎますが、外部から侵入するMobを阻止しません。
拠点の物理的な防衛はプレイヤー自身が構築する必要があります。鉱石や流体の
無限生成は本MODの責務外であり、他のCreateアドオンへ委譲します。

## Alpha Release

The current development version is **0.1.0-alpha.1**.
This build is an internal alpha release candidate and has not been publicly published.
Back up worlds before testing an alpha build. This candidate establishes the first
containment integration preview:

- Permanent infection suppression in the 5x5 chunk area centered on the
  initial Overworld spawn by default.
- Initial-spawn ore-generation suppression for newly generated terrain.
- Three Create-powered Stabilizer tiers covering 1x1, 3x3, or 5x5 chunks while
  ACTIVE or spending the finite Grace budget granted by their current Cell.
- Budgeted cleanup of audited removable Spore foliage in loaded chunks while
  a Stabilizer is ACTIVE, with tier-specific source profiles, hard global caps,
  and persisted progress across reloads.
- Target-based integration with the audited environmental spread paths in
  Fungal Infection: Spore 2.2.0j.
- Dimension-local and overlap-safe suppression, including negative chunk
  coordinates.
- Five Create recipes for Stabilization Compound, common Stabilization Cells,
  Tier 1, and staged Tier 1-to-2 and Tier 2-to-3 Stabilizer upgrades.
- Unheated Compound Mixing with 100 mB of TFMG Liquid Plastic, Sand, Blue Ice,
  and eight Iron Nuggets.
- Engineer's Goggles diagnostics, targeted chunk-range visualization, static
  item guidance, and localized Ponder scenes for operation, coverage, and production.
- Per-Cell Grace budgets of 1200, 1800, and 2400 ticks for Tier 1, Tier 2, and
  Tier 3. ACTIVE and power recovery do not refill spent Grace; only a new Cell does.
- Physical Create GameTests for unheated Compound Mixing, all three Mechanical
  Crafter recipes, and default-duration Tier 2/Tier 3 continuous Cell logistics.
- A final directional Tier 1 block asset with state lights, an ACTIVE rotating
  core and gear, and Grace warning pulses; Tier 2 and Tier 3 retain placeholder art.

Suppression prevents selected new Spore mutations. Tier 1 uses a small
`StabilizerBlock` subclass for horizontal facing and rear-only shaft input, while
all tiers retain one shared `StabilizerBlockEntity` class/type and state machine.
Generic `STABILIZER` source IDs contain tier and
position. While ACTIVE, each source removes only audited non-Block-Entity foliage
under its tier profile and server-global caps; cleanup pauses during grace and
stops offline. Stabilizers do not restore infected terrain, destroy nests, load
chunks, suppress moving contraptions, or block hostile mobs. The Spore integration
is pinned to the exact audited release. All tiers consume the same Cell through
Create-oriented production; Compound cannot power a Stabilizer. See
[`docs/r7-stabilizer-tiers.md`](docs/r7-stabilizer-tiers.md),
[ADR 0002](docs/adr/0002-common-stabilizer-tier-architecture.md), and
[ADR 0001](docs/adr/0001-stabilizer-consumable-and-production-model.md). The
revised Compound material decision is recorded in
[ADR 0004](docs/adr/0004-tfmg-compound-production.md).
The finite Grace decision and final R9 values are recorded in
[ADR 0005](docs/adr/0005-per-cell-grace-budget.md) and
[R9 Balance Hardening](docs/r9-balance-hardening.md). The Tier 1 visual and
orientation contract is recorded in
[R11 Tier 1 Final Block Asset](docs/r11-tier1-final-block-asset.md).

## Inspecting a Stabilizer

1. Equip Engineer's Goggles.
2. Look directly at a Stabilizer to inspect its live server-authoritative operation.
3. Hold Shift for exact chunk bounds, configured operating details, and the targeted range overlay.

The overlay is shown only for the targeted Stabilizer and does not indicate protection from hostile mobs. Use Create's configured Ponder key while hovering a Stabilizer, Stabilization Cell, or Stabilization Compound to open the Operation, Coverage, and Production explanations. The completed English and Japanese client smoke is recorded in [R8 Graphical Verification](docs/r8-graphical-verification.md).

The initial Overworld spawn center is persisted and has permanent infection suppression with a default radius of two chunks, covering 5x5 chunks. Changing world spawn later does not move it. It needs no Cell or rotation and performs no progressive cleanup. It does not remove existing nests or stop hostile mobs; it is an initial refuge, not a complete safe zone. See [R8 Operational UX](docs/r8-operational-ux.md), [ADR 0003](docs/adr/0003-operational-display-and-client-visualization.md), and [Quest Integration](docs/quest-integration.md).

現在の開発versionは **0.1.0-alpha.1** です。このbuildは内部Alphaリリース候補で、
一般公開は延期されており、まだ公開配布されていません。テスト前にワールドを
バックアップしてください。本候補は初期スポーン周辺の永久抑制、初期地形の
鉱石生成抑制、Create動力を使う3段階のStabilizer、およびSpore 2.2.0jの監査済み
環境拡散経路との統合を提供します。ACTIVE中のStabilizerは、ロード済みチャンク内の
監査済み非Block Entity感染植生だけを処理予算内で段階的に除去し、GRACE_PERIODと
OFFLINE中は浄化を停止します。感染地形の復元、巣の自動破壊、Mob侵入防止、
チャンクロードは行いません。猶予はCellごとに一度だけ付与される有限の障害耐性予算で、
ACTIVE復帰や電源操作では回復せず、次のCell消費時だけ補充されます。全Tierと運転用の共通安定化セルはCreate加工で製造し、
安定化化合物は中間素材であり、Stabilizerへ直接投入できません。

Engineer's Gogglesを装着してStabilizerを見ると、server同期された運転情報を確認できます。Shiftを押すと正確なチャンク範囲と対象1台だけのrange overlayを表示します。この範囲は敵対Mobからの保護を示すものではありません。

## Requirements

- Minecraft 1.21.1
- Java 21
- NeoForge 21.1.235 or newer for Minecraft 1.21.1
- Create 6.0.11 or newer
- Create: The Factory Must Grow 1.2.0 or newer
- Fungal Infection: Spore 2.2.0j exactly

## Internal Testing

For internal testing, build the candidate from source, install the matching
NeoForge release, Create, TFMG, and Spore, then place
`frontier_protocol-0.1.0-alpha.1.jar` in the instance's `mods` directory. Do not
use a different Spore version with this alpha build.

Release-specific compatibility notes and verification details are documented
in [`docs/alpha-release.md`](docs/alpha-release.md).

Stabilization Compound and Stabilization Cell use custom final 32x32 item icons.
Tier 1 uses its final custom directional model, textures, state lights, and ACTIVE
animation. Tier 2 and Tier 3 still use internal-alpha placeholder models and
vanilla textures; see [`docs/placeholder-assets.md`](docs/placeholder-assets.md),
[R10 Final Item Assets](docs/r10-item-assets.md), and
[R11 Tier 1 Final Block Asset](docs/r11-tier1-final-block-asset.md). Tier 2/3 final
block art, the project logo, licensing decisions, and public distribution remain pending.

## Development

The project targets Minecraft 1.21.1 with Java 21 and NeoForge. Create, TFMG,
and Fungal Infection: Spore are required dependencies.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer
```

Spore 2.2.0j is resolved from CurseForge project 678295, file 8342823 through
CurseMaven. The dependency and mod metadata are pinned to that exact release.
