# Frontier Protocol

Frontier Protocol is a NeoForge addon for Fungal Infection: Spore and Create.
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
  ACTIVE or in their configured grace period.
- Budgeted cleanup of audited removable Spore foliage in loaded chunks while
  a Stabilizer is ACTIVE, with tier-specific source profiles, hard global caps,
  and persisted progress across reloads.
- Target-based integration with the audited environmental spread paths in
  Fungal Infection: Spore 2.2.0j.
- Dimension-local and overlap-safe suppression, including negative chunk
  coordinates.
- Five Create recipes for Stabilization Compound, common Stabilization Cells,
  Tier 1, and staged Tier 1-to-2 and Tier 2-to-3 Stabilizer upgrades.

Suppression prevents selected new Spore mutations. One shared `StabilizerBlock`
class, one shared `StabilizerBlockEntity` class/type, and one state machine serve
all three Registry entries. Generic `STABILIZER` source IDs contain tier and
position. While ACTIVE, each source removes only audited non-Block-Entity foliage
under its tier profile and server-global caps; cleanup pauses during grace and
stops offline. Stabilizers do not restore infected terrain, destroy nests, load
chunks, suppress moving contraptions, or block hostile mobs. The Spore integration
is pinned to the exact audited release. All tiers consume the same Cell through
Create-oriented production; Compound cannot power a Stabilizer. See
[`docs/r7-stabilizer-tiers.md`](docs/r7-stabilizer-tiers.md),
[ADR 0002](docs/adr/0002-common-stabilizer-tier-architecture.md), and
[ADR 0001](docs/adr/0001-stabilizer-consumable-and-production-model.md).

現在の開発versionは **0.1.0-alpha.1** です。このbuildは内部Alphaリリース候補で、
一般公開は延期されており、まだ公開配布されていません。テスト前にワールドを
バックアップしてください。本候補は初期スポーン周辺の永久抑制、初期地形の
鉱石生成抑制、Create動力を使う3段階のStabilizer、およびSpore 2.2.0jの監査済み
環境拡散経路との統合を提供します。ACTIVE中のStabilizerは、ロード済みチャンク内の
監査済み非Block Entity感染植生だけを処理予算内で段階的に除去し、GRACE_PERIODと
OFFLINE中は浄化を停止します。感染地形の復元、巣の自動破壊、Mob侵入防止、
チャンクロードは行いません。全Tierと運転用の共通安定化セルはCreate加工で製造し、
安定化化合物は中間素材であり、Stabilizerへ直接投入できません。

## Requirements

- Minecraft 1.21.1
- Java 21
- NeoForge 21.1.235 or newer for Minecraft 1.21.1
- Create 6.0.11 or newer
- Fungal Infection: Spore 2.2.0j exactly

## Internal Testing

For internal testing, build the candidate from source, install the matching
NeoForge release, Create, and Spore, then place
`frontier_protocol-0.1.0-alpha.1.jar` in the instance's `mods` directory. Do not
use a different Spore version with this alpha build.

Release-specific compatibility notes and verification details are documented
in [`docs/alpha-release.md`](docs/alpha-release.md).

All three tiers, Compound, and Cell currently use explicit internal-alpha
placeholder models backed only by vanilla textures. No custom PNG or final art
is included; see
[`docs/placeholder-assets.md`](docs/placeholder-assets.md).

## Development

The project targets Minecraft 1.21.1 with Java 21 and NeoForge. Create and
Fungal Infection: Spore are required dependencies.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer
```

Spore 2.2.0j is resolved from CurseForge project 678295, file 8342823 through
CurseMaven. The dependency and mod metadata are pinned to that exact release.
