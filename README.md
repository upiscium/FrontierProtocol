# Frontier Protocol

Frontier Protocol is a NeoForge addon for Fungal Infection: Spore and Create.
It introduces Create-powered containment infrastructure that suppresses Spore
infection across entire chunks, from the bottom to the top of the dimension.
Players can build a minimal automated production line and logistics network to
keep the Tier 1 containment device supplied and powered, allowing sustainable
settlements and industrial bases to exist inside an infected world.

Containment does not prevent hostile mobs from entering protected areas.
Physical defense remains the player's responsibility. Resource generation and
infinite ore extraction are intentionally delegated to other Create addons.

Frontier Protocolは、Fungal Infection: SporeとCreateを統合するNeoForge向け
アドオンMODです。Createの動力、生産設備および物流を利用して、チャンク全高に
わたるSpore感染抑制設備を継続運転できます。Tier 1 Stabilizerと運転用の
安定化セルはCreate加工で製造する。安定化化合物は中間素材であり、Stabilizerへ
直接投入できない。

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
- A Create-powered Tier 1 Stabilizer that protects its placement chunk while
  ACTIVE or in its configured grace period.
- Budgeted cleanup of audited removable Spore foliage in loaded chunks while
  the Tier 1 Stabilizer is ACTIVE, with persisted progress across reloads.
- Target-based integration with the audited environmental spread paths in
  Fungal Infection: Spore 2.2.0j.
- Dimension-local and overlap-safe suppression, including negative chunk
  coordinates.
- A minimal Create production chain for Stabilization Compound, Stabilization
  Cells, and the Tier 1 Stabilizer.

Suppression prevents selected new Spore mutations. While ACTIVE, Tier 1 also
removes only audited non-Block-Entity foliage under global and per-device
budgets; it does not restore infected terrain or destroy nests. Cleanup pauses
during the grace period and stops while the device is offline. Containment does not
block hostile mobs, prevent combat or explosions, or provide Tier 2/Tier 3
containment. The Spore integration is intentionally pinned to the exact audited
release. Tier 1 Stabilizer and its operating cells are produced through Create
processing. Stabilization Compound is an intermediate material and cannot
directly power a Stabilizer. No dedicated creative tab is included.

現在の開発versionは **0.1.0-alpha.1** です。このbuildは内部Alphaリリース候補で、
一般公開は延期されており、まだ公開配布されていません。テスト前にワールドを
バックアップしてください。本候補は初期スポーン周辺の永久抑制、初期地形の
鉱石生成抑制、Create動力を使うTier 1 Stabilizer、およびSpore 2.2.0jの監査済み
環境拡散経路との統合を提供します。ACTIVE中のTier 1は、ロード済みチャンク内の
監査済み非Block Entity感染植生だけを処理予算内で段階的に除去し、GRACE_PERIODと
OFFLINE中は浄化を停止します。感染地形の復元、巣の自動破壊、Mob侵入防止、
Tier 2/Tier 3は本Alphaの対象外です。Tier 1 Stabilizerと運転用の安定化セルは
Create加工で製造する。安定化化合物は中間素材であり、Stabilizerへ直接投入できない。

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

Tier 1, Compound, and Cell models currently use explicit internal-alpha
placeholder assets backed by vanilla textures. They are not final art; see
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
