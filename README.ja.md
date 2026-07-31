# Frontier Protocol

[英語](README.md) | [日本語](README.ja.md)

Frontier Protocolは、Fungal Infection: Spore、Create、および
Create: The Factory Must Grow（TFMG）向けのNeoForgeアドオンです。
Create動力で稼働する封じ込め設備を追加し、ディメンションの最下部から最上部まで、
チャンク全高にわたってSpore感染を抑制します。プレイヤーはCreateの生産設備と物流を
利用して全Tier共通のStabilization Cellを製造し、3段階の固定式Stabilizerへ供給する
ことで、感染した世界にも持続可能な居住地や工業拠点を構築できます。

封じ込めは、保護範囲の外から侵入する敵対Mobを阻止しません。物理的な防衛は
プレイヤー自身の責任です。資源生成と鉱石の無限採掘は、意図的に他のCreateアドオンへ
委ねています。

## RC prerelease

公開されている最新versionは **0.1.0-rc.1** で、exact source commit
`5fe7b0f9931560747af7c9c25df367c1e4db9014`から2026-07-31にprereleaseとして
公開されました。現在のmain sourceは、未公開の **0.1.0 internal Stable candidate**
です。Stable GitHub Releaseはまだ公開されていません。
[stable-readiness contract](docs/stable-readiness-0.1.0.md)に、support保証、blocker
evidence、必須gate、および推奨RC sequenceを定義しています。
`0.1.0-alpha.1`は以前の公開prereleaseとして引き続き利用でき、その公開assetは
変更不可能なまま維持されます。後続のsource候補およびtagなしのworkflowビルドは、
公開releaseではありません。prereleaseをテストする前にワールドをバックアップして
ください。現在のsourceは次の封じ込め統合scopeを維持します。

- 初期Overworldスポーンを中心とする5x5チャンク範囲の永久感染抑制（デフォルト）。
- 新規生成地形に対する初期スポーン周辺の鉱石生成抑制。
- `ACTIVE`中、または現在のCellから付与された有限のGrace予算を消費している間、
  1x1、3x3、5x5チャンクを保護する3段階のCreate動力式Stabilizer。
- Stabilizerが`ACTIVE`の間、ロード済みチャンク内の監査済み除去可能Spore植生を、
  Tier別source profile、厳格なglobal cap、およびreloadを越えて保持される進捗に基づき
  予算制で浄化。
- Fungal Infection: Spore 2.2.0jの監査済み環境拡散経路に対するtarget-based統合。
- 負のチャンク座標を含む、ディメンション単位かつ重複安全な抑制。
- Stabilization Compound、共通Stabilization Cell、Tier 1 Stabilizer、および
  Tier 1から2、Tier 2から3への段階的アップグレードを含む5つのCreateレシピ。
- TFMG Liquid Plastic 100 mB、Sand、Blue Ice、Iron Nugget 8個を使う、
  加熱不要のCompound Mixing。
- Engineer's Goggles診断、対象チャンク範囲の可視化、固定item guidance、および
  運転、範囲、生産を説明するlocalization済みPonder scene。
- 全Stabilizer Tier向けのCreate Nixie Tube二値status panelと、vanilla comparatorによる
  Cell buffer計測。
- Tier 1、Tier 2、Tier 3でそれぞれ1200、1800、2400 tickのCell単位Grace予算。
  `ACTIVE`復帰や動力復旧では使用済みGraceは回復せず、新しいCellだけが補充します。
- 加熱不要Compound Mixing、3種類すべてのMechanical Crafterレシピ、および
  デフォルトdurationでのTier 2／Tier 3連続Cell物流を検証する物理Create GameTest。

抑制は、選択された新規Spore変異を防ぎます。3つのRegistry entryは、1つの共有
`StabilizerBlock` class、1つの共有`StabilizerBlockEntity` class／type、および1つの
state machineを使用します。汎用`STABILIZER` source IDにはTierと位置が含まれます。
各sourceは`ACTIVE`中にのみ、Tier profileとserver全体のglobal capに従って、監査済みの
非Block Entity植生を除去します。浄化はGrace中に一時停止し、offline中に停止します。
Stabilizerは感染地形を復元せず、巣を破壊せず、チャンクをロードせず、移動contraptionを
抑制せず、敵対Mobを阻止しません。Spore統合は監査対象の正確なreleaseへ固定されています。
全TierはCreate中心の生産工程で作る同一のCellを消費し、CompoundではStabilizerを
稼働できません。詳細は[`docs/r7-stabilizer-tiers.md`](docs/r7-stabilizer-tiers.md)、
[ADR 0002](docs/adr/0002-common-stabilizer-tier-architecture.md)、および
[ADR 0001](docs/adr/0001-stabilizer-consumable-and-production-model.md)を参照してください。
改訂後のCompound素材判断は
[ADR 0004](docs/adr/0004-tfmg-compound-production.md)に記録されています。
有限Graceの判断と最終R9値は
[ADR 0005](docs/adr/0005-per-cell-grace-budget.md)および
[R9 Balance Hardening](docs/r9-balance-hardening.md)に記録されています。

## Stabilizerの確認

1. Engineer's Gogglesを装着します。
2. Stabilizerを直接見て、server-authoritativeな稼働状態を確認します。
3. Shiftを押して、正確なチャンク境界、設定済み運転詳細、および対象範囲overlayを確認します。

overlayは見ているStabilizerだけに表示され、敵対Mobからの保護を示すものではありません。
Stabilizer、Stabilization Cell、またはStabilization Compoundへカーソルを合わせ、Createで
設定されたPonder keyを使用すると、運転、範囲、生産の説明を開けます。英語と日本語の
client smoke完了記録は
[R8 Graphical Verification](docs/r8-graphical-verification.md)を参照してください。

Stabilizerへ直接取り付けたCreate Nixie Tubeは、Createのtrain-signal panel表示を二値の
封じ込めindicatorとして使用します。赤は封じ込めがofflineであることを示します。緑は
感染抑制が有効であることを示し、抑制が継続する`GRACE_PERIOD`も緑です。panelは黄色を
使用せず、`ACTIVE`と`GRACE_PERIOD`を区別しません。詳細診断には引き続き
Engineer's Gogglesを使用します。

vanilla comparatorは、全Stabilizer Tierで保存済みStabilization Cellの満杯度を正規化して
読み取ります。空ならsignal `0`、1個以上なら最低`1`、設定容量まで満杯、または保存済み
over-capacity状態なら`15`です。このsignalはinventory計測専用であり、稼働状態や残り
稼働時間を示しません。Stabilizer自身はweak／strong redstone powerを直接出力しません。
詳細は[R15 Instrumentation Verification](docs/r15-instrumentation-verification.md)を
参照してください。

初期Overworldスポーン中心は保存され、デフォルト半径2チャンク、すなわち5x5チャンクの
永久感染抑制を持ちます。後からworld spawnを変更しても中心は移動しません。Cellも回転も
不要で、progressive cleanupは行いません。既存の巣を除去せず、敵対Mobも阻止しないため、
完全なsafe zoneではなく初期避難所です。詳細は
[R8 Operational UX](docs/r8-operational-ux.md)、
[ADR 0003](docs/adr/0003-operational-display-and-client-visualization.md)、および
[Quest Integration](docs/quest-integration.md)を参照してください。

## 配布

公開配布先はGitHub Releasesです。pull requestおよびmanual Build workflowのartifactは、
14日間だけ保持される内部検証用bundleであり、packwizの取得先ではありません。
tag-driven release workflowは、検証済みの4つのcandidate fileを永続的なGitHub prerelease
assetとして公開しました。

`v0.1.0-rc.1`の変更しないJAR URLは次のとおりです。

```text
https://github.com/upiscium/FrontierProtocol/releases/download/v0.1.0-rc.1/frontier_protocol-0.1.0-rc.1.jar
```

modpackのrootで次を実行して追加できます。

```bash
packwiz url add --meta-folder mods "Frontier Protocol" \
  "https://github.com/upiscium/FrontierProtocol/releases/download/v0.1.0-rc.1/frontier_protocol-0.1.0-rc.1.jar"
```

packwizは、このdirect URLと計算したfile hashを生成する`.pw.toml` metadataへ保存します。
将来のversionでは、新しい変更不可能なversion tagとURLを使用します。
`v0.1.0-rc.1`および以前の`v0.1.0-alpha.1`のassetを置換または変更してはいけません。

## 必要環境

- Minecraft 1.21.1
- Java 21
- Minecraft 1.21.1向けNeoForge 21.1.235以降
- Create 6.0.11以降
- Create: The Factory Must Grow 1.2.0以降
- Fungal Infection: Spore 2.2.0j（完全一致）

## 内部テスト

内部テストではsourceからcandidateをbuildし、対応するNeoForge release、Create、TFMG、
Sporeを導入して、`frontier_protocol-0.1.0.jar`をinstanceの`mods` directoryへ
配置してください。これは公開installation artifactではなく、未公開の内部candidateです。
このStable candidateを別のSpore versionと組み合わせないでください。

release固有の互換性noteとverification詳細は
[0.1.0 Stable candidate notes](docs/releases/0.1.0.md)に記載されています。公開RCの
verificationは[0.1.0-rc.1 publication record](docs/releases/0.1.0-rc.1-publication.md)に
引き続き記録されています。

Stabilization CompoundとStabilization Cellは最終版のcustom 32x32 item iconを使用します。
3つのStabilizerはすべて、上面status LED、前面control panel、背面decorative pipe、両側の
shaft bearingを持つ最終版のstatic Create-casing machine modelを使用します。回転入力は
両側面からのみ受け付け、水平rotation axisは`FACING`に対して垂直に導出されます。
custom Block Entity Rendererやdynamic block partは使用しません。詳細は
[`docs/placeholder-assets.md`](docs/placeholder-assets.md)および
[R11 Stabilizer Casing Redesign](docs/r11-stabilizer-casing-redesign.md)を参照してください。
project logoは引き続き未完了です。

## ライセンス

Frontier Protocolのsource codeとoriginal assetは
[BSD 3-Clause License](LICENSE)の下で提供されます。third-party projectとdependency contentは
それぞれのlicenseを維持し、Frontier Protocolによって再licenseされません。正確な範囲と、
repository templateおよび監査済みCreate GameTest fixtureのために保持されるnoticeは、
[licensing and provenance audit](docs/licensing.md)を参照してください。

## 開発

このprojectはMinecraft 1.21.1、Java 21、NeoForgeを対象とします。Create、TFMG、および
Fungal Infection: Sporeは必須dependencyです。

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer
```

Spore 2.2.0jはCurseForge project 678295、file 8342823をCurseMaven経由で解決します。
dependencyとmod metadataは、そのreleaseへ固定されています。
