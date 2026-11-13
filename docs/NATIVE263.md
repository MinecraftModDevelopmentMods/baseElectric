# Minecraft 26.3 local port

Starting source: `a34f6bba1ec87f911da65ec2b50eab89629f2ea8`.
Branch: `feature/baseelectric-26.3-port`. Version: `2.2.19.2603001`.

## Native Target

Forge 66.0.0, official 26.3, MCP 20260918.230105, Temurin 25.0.3+9,
ForgeGradle 7.0.34 and checksum-pinned Gradle 9.6.1. Native client/data pack
formats are 97.1/121.0, read from the actual 26.3 client `version.json`.
Forge 66.1.5 compatibility is a later gate; it does not replace the baseline.

Primary SDK inputs downloaded read-only from Forge Maven:

| Input | SHA-256 |
| --- | --- |
| 66.0.0 installer | C6173EEA56AED0B947722F314542196E28A35285844961D517C7FB571492BBE3 |
| 66.0.0 MDK | 00B86DF0597632CEBAA2D357CD9A5DCAE0D958CE04C4733C70FC2B27BB0E61B8 |
| MCP config | 8679F4EAA95F9779CFE9C4A59998BA2B3BE9DCE84A42D835A05248DC72C83DF6 |

[Forge downloads](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html),
[exact MDK](https://maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.0.0/forge-26.3-66.0.0-mdk.zip),
[exact MCP](https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/26.3-20260918.230105/mcp_config-26.3-20260918.230105.zip).
The MDK specifies Java 25 and native `minecraft.dependency` setup; the MCP
descriptor confirms official names and Java target 25. BM263, OS263 and IAF263
are read-only references, not qualified dependencies for this candidate.

Reference heads observed: BM `8ee91846ed4dd9028e84ff3878c55a700a16d318`,
OS `c5799770774a7535b5c436966f6f6cf70f649c5d`,
IAF `8e6de134b075811a7295e15ad32a224cf3c04134`.

## Local Input Handoffs

Power's immutable compiler handoff is accepted: source
`c0eb83f7ba91cf8c83caea75e63057407a345949`, SHA-256
`97525CE9C75509C55225E0061D43BAF9D29BDBDE6DF853E1552A64A54C14C3DE`.
The exact approved BM/OS compiler snapshots are likewise pinned by source
and SHA-256. These pins identify local inputs, not gameplay/public acceptance.
Handoff07 restores future recipe discovery with optional native recipe tags,
retaining strict retired-brain-memory archival, native loot/ALT repairs and
the scoped pending-item-tag mixin. The parent owns shared migration code;
Electric owns its recipe/tag/advancement generator and resources. Earlier
results stay historical; no prior pass transfers to this changed input.

Full-suite, ECJ, complete Eclipse, packaged and reproduction gates remain
separate. The remaining required handoffs are:

- Installed baseline server/shim/universal hashes for packaged verification.

The earlier uncommitted helper mismatch is resolved by the approved test-rig
checkpoint 26ad8, unchanged in current c0eb83f. Exact source files are frozen
under the child's `unit-inputs-01`; its native agent rebuild remains sealed.
Earlier helper-blocked attempts remain in the external receipts, not passes.

Electric requires the unchanged `cyano.poweradvantage.api` surface: typed
conduits and connection context, powered-machine superclass/ticking and
save/update hooks, GUI containers/screens, fluid custody, crafting and
migration bridges. `LegacyMigrationApi` must retain Electric's registration
contract. Base Metals must retain `CrushingRecipe` and the shared
`Legacy112MigrationExtension` hook. OreSpawn must retain provider, family,
pattern and `OreSpawnApi` registration plus the retained-settings behavior.

No older jars, fake pins or generated stubs may qualify release outputs.

The parent compiler-only handoff for BM/OS is recorded in the external Electric
status receipt and has been hash-checked. These exact local compile/player
identity pins do not establish runtime or public qualification.
For provisional troubleshooting, `compileNativeProductionDiagnostics` takes
all three explicit `powerAdvantage` / `baseMetals` / `oreSpawn` `DiagnosticJar`
and `DiagnosticSha256` properties. Every input is hashed; the task produces only
isolated diagnostic classes, never a candidate jar or runtime qualification.

## Evidence

Current run receipts and failed attempts live at
`D:/PowerAdvantage/_verification/base-263-port-20261112/children/electric`.
The isolated Gradle home is `D:/GradleCaches/baseelectric-263-port`.
Only Electric source/build outputs and its evidence are writable in this task.

Historical D03 checkpoint results (not handoff04 qualification):

- All 87 production sources, complete 118-unit / 34-suite Gradle inventory,
  client/GameTest compilation, build/Javadoc, dependency/artifact/checksum
  audits and complete Eclipse production classpath PASS (`attempt23`, 51 tasks,
  Power handoff03).
- Independent ECJ main/test/helper compilation and the exact 118-test inventory
  PASS (`attempt24`), zero failures/ignored tests/assumptions, Java 25 bytecode.
- The separate 29 native resource tests PASS (also `attempt23`), including native
  recipe-holder criteria, exact variant matching and condition-deletion controls.
- All 50 recipe unlocks match the generator.
- Native Eclipse launch-argument check PASS, asset index 34,
  SHA-1 `e99f9973047785b5a9d52eaf72020b97545053c9`.
- Production compile and dependency audit PASS (`attempt13`) after nine native
  compiler errors were preserved (`attempt12`). The exact old hydro placement
  predicate retains its cobweb/bamboo-sapling exceptions. Quaternion rotation
  and piston reactions use their native replacements without geometry changes.
  GameTest compilation/Javadoc/model assets PASS (`attempt14`).
- These initial full gates use Power handoff03. Older attempt20/21
  results on handoff02 remain preserved, not transferred to this input.
  Actual startup failed in attempt25 on unbound item tags in Power's material
  condition. The full failed run is preserved without skipping native data or
  Forge conditions; handoff04 is the parent's scoped repair for this failure.
- Attempt26 passed all 52 cold/spaced-path tasks on D03 with a new empty Gradle
  home and project cache. All three jars match immutable Electric handoff01.
  This result remains D03-only; changed-input cold/reproduction gates remain.
- Earlier attempts retain the argument, compile and native test failures.
  The first cache started empty, but its command failed before task selection;
  subsequent initial checks reused the prepared SDK. Attempt26 is the later
  genuine cold-cache pass, not a relabelled retry of that first failure.
- Holder-set regression is red-to-green: attempt10 fails on uninitialized
  native `HOLDER_SET_TYPES`; attempt11 registers Forge's stock types through
  actual `NewRegistryEvent` / `RegisterEvent` dispatch, then decodes a native
  `forge:and` intersection and rejects an unknown holder-set type as a codec
  error. No fake codec or registry is substituted. The test inventory is unchanged.
- Native chest negative controls reject untyped min/max count objects and
  unknown integer-provider types, without an old NumberProviders fallback.
- Static comparison retains exact projections for native metadata, loot and
  advancement schemas; recipe bodies/IDs/rewards and geometry remain protected.

Initial unit/ECJ/Eclipse passes do not waive actual source-GameTest startup,
all-jar reproduction, new cold cache with
input paths containing spaces, packaged family or Forge 66.1.5 qualification.

The retained 117 unit and 28 native resource tests are unchanged in coverage;
one new stem-fruit regression raises the strict totals to 118 and 29.
Fresh ECJ output must match that exact inventory, plus source
GameTests, independent all-jar reproduction, a new cold cache and explicit
input paths with spaces once exact dependencies are available.

Parent confirmed the final shared SDK is bare Forge 66 with no redundant
Power access transformer and no API contract change. Electric does not copy
Power AT configuration. Native adaptations use RenderPearl pipeline imports,
`ContextMap.builder().set().buildAndValidate()`, `Recipe.DIRECT_CODEC` for
raw recipe JSON, and the checked official stem fruit key after removal of
`StemBlock.CODEC`. The new regression covers melon, pumpkin and a custom
fruit key, without hardcoding modded stems to vanilla fruit.
The native pack proof uses separate metadata and flattened resource streams,
retaining exact resource bytes, compatibility ranges and strict diagnostics.
Native trade data uses singular `given_item_modifier`; keeping the old plural
field silently loses the bulk counts. Prices, count ranges and librarian tiers
are unchanged. Shader contracts follow native `#include` syntax and still
require lightmap, overlay and fog sampling.
All 20 block-loot tables use the singular native pool `condition` and predicate
`type` discriminator; the original explosion survival condition is retained.
Trade modifiers likewise use native `type`. The worldless native loot syntax
proof uses a vanilla item and explicitly detects loss of the old plural field.
All 21 Electric loot tables already use singular native container fields:
20 explosion conditions and nine chest count modifiers. The whole-table proof
now compares every modifier and condition recursively through pools, entries
and composite children, using native canonical leaf values only as expectations
for the actual `LootTable.DIRECT_CODEC` output. Synthetic whole tables cover
table/pool/entry modifiers, pool/entry conditions, inline modifier sequences and
multi-condition `minecraft:all_of` terms. Five old-plural counterfactuals
successfully decode but lose their fields; the semantic oracle rejects each.
The registered-item source GameTest applies the same complete container-scope
checks in addition to its existing exact quantity/weight/explosion assertions.
The 29-test native gate passed this expansion in attempt34. Power handoff07
is now selected for rebuilt/shared-runtime qualification; handoff05's local
full/ECJ/native/source-GameTest/cold/reproduction results remain preserved.

An independent golden test uses the 21 exact loot JSON entries extracted from
accepted `baseElectric-2.2.18.2602001.jar`, SHA-256
`A7D12A66CA91A6BACCFEFD2CED9D3EC962F5FEB73BF632E1683F99084384F938`.
The manifest and each raw file are SHA-sealed; extraction rejects any other
jar. Syntax translation is test-only and does not call the production loot
importer. Real built-in item IDs form a bijection for worldless codec input;
encoded names are reversed before comparison, without registry mutation.
Actual mod IDs still require the registered-item server gate. Normalization
is restricted to singleton modifier arrays, native default weight/quality,
empty pools, zero bonus rolls, false add and namespaces on type fields only.
The chest's nonsemantic comment must match before codec projection. Count,
weight, item-identity, plural-modifier and explosion-condition counterfactuals
must fail the full-table semantic comparison. Global Forge loot modifiers'
`conditions` arrays are outside this fixture and remain unchanged.
The added mandatory test raises the exact gates to 119 units in 35 suites
and 30 native tests in 13 suites; older 118/29 receipts stay historical.
The retained source GameTest now also decodes all 21 exact packaged tables with
the actual registered Electric items; actual server startup remains pending.
The chest table retains all nine item weights and count ranges (48 item weight,
144 empty weight). Its native `modifier`/`type`/uniform provider fields are
round-tripped. The separate legacy `additional-loot-tables` config export in
`TreasureChests` remains unchanged for Power's shared importer; preserving its
legacy count-function contract is a parent integration gate.

All 50 recipe unlocks remain unconditional, preserving future discovery across
NORMAL/TECH mode changes. Recipe `forge:condition` fields remain untouched;
advancements do not copy them. Each trigger uses native `Recipe.LIST_CODEC`
through `conditions.recipes: "#baseelectric:discovery/<stem>"`. Matching
`tags/recipe/discovery/<stem>.json` files contain exactly one optional recipe
entry (`id` unchanged, `required:false`). A disabled recipe leaves a defined
empty tag rather than a missing direct holder; an active tag contains only its
exact recipe. Rewards remain `Recipe.KEY_CODEC` keys and may safely refer to
temporarily absent recipes. Ingredient alternatives, OR groups, recipe IDs,
reward IDs and gameplay quantities are unchanged.

Native trigger/TagFile/reward codecs test all 50 populated and all 50 empty
tags, exact/unrelated matching, undefined-tag rejection, optional-vs-required
loading, required-flag and ID mutation guards, and stable encoding. The source
GameTest retains the original material-gated discovery assertion and requires
all 50 advancements to load with correct active/empty tag membership. Existing
recipe-mode assertions remain intact. Attempt49 preserves the red contract
against the old conditional advancement resources. The superseded Power06
cold attempt48 is interrupted and preserved, not counted as a cold pass.

No 26.2 receipt is 26.3 proof. Combined family, graphical, endurance and manual
acceptance gates remain separate. No push, PR, tag or publication is authorized.
