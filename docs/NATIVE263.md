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

## Pending Handoff

Release, full-suite, ECJ, Eclipse, packaged gameplay and reproduction gates
must fail closed until the parent provides:

- basePower 2.4.18.2603001 verified source SHA and development/player SHA-256.
- Base Metals 3.1.2.2603001 migration-hook source and development/player pins.
- OreSpawn 4.1.1.2603001 retained-settings repair source and jar pins.
- Exact 26.3 unit bootstrap agent plus registry, loader and entity-tracker source
  pins, including the deterministic rebuilt agent hash.
- Installed baseline server/shim/universal hashes for packaged verification.

Electric requires the unchanged `cyano.poweradvantage.api` surface: typed
conduits and connection context, powered-machine superclass/ticking and
save/update hooks, GUI containers/screens, fluid custody, crafting and
migration bridges. `LegacyMigrationApi` must retain Electric's registration
contract. Base Metals must retain `CrushingRecipe` and the shared
`Legacy112MigrationExtension` hook. OreSpawn must retain provider, family,
pattern and `OreSpawnApi` registration plus the retained-settings behavior.

No older jars, fake pins or generated stubs may qualify release outputs.

The parent compiler-only handoff for BM/OS is recorded in the external Electric
status receipt and has been hash-checked. It is not promoted to release pins.
When a Power compiler jar arrives, `compileNativeProductionDiagnostics` takes
all three explicit `powerAdvantage` / `baseMetals` / `oreSpawn` `DiagnosticJar`
and `DiagnosticSha256` properties. Every input is hashed; the task produces only
isolated diagnostic classes, never a candidate jar or runtime qualification.

## Evidence

Current run receipts and failed attempts live at
`D:/PowerAdvantage/_verification/base-263-port-20261112/children/electric`.
The isolated Gradle home is `D:/GradleCaches/baseelectric-263-port`.
Only Electric source/build outputs and its evidence are writable in this task.

Current checkpoint results:

- Native API subset compilation and 29 sibling-independent native tests PASS
  on bare Forge 66.0.0, zero failures/errors/skips (`attempt11`, including chest
  modifier/weight round-trips and real holder-set registry fixtures). This is not
  compilation of every production class or execution of the full 118 tests.
- All 50 recipe unlocks match the generator.
- Native Eclipse launch-argument check PASS, asset index 34,
  SHA-1 `e99f9973047785b5a9d52eaf72020b97545053c9`. This is not the complete
  Eclipse production-classpath or ECJ gate.
- Production `compileJava` stops before compilation with the required exact
  basePower 26.3 pin error. No candidate jar or artifact hash exists yet.
- Earlier attempts retain the argument, compile and native test failures.
  The first cache started empty, but its command failed before task selection;
  subsequent checks reuse the prepared SDK. No full cold-cache gate is claimed.
- Holder-set regression is red-to-green: attempt10 fails on uninitialized
  native `HOLDER_SET_TYPES`; attempt11 registers Forge's stock types through
  actual `NewRegistryEvent` / `RegisterEvent` dispatch, then decodes a native
  `forge:and` intersection and rejects an unknown holder-set type as a codec
  error. No fake codec or registry is substituted. The test inventory is unchanged.
- Native chest negative controls reject untyped min/max count objects and
  unknown integer-provider types, without an old NumberProviders fallback.
- The final static audit has 8 passing checks and fails only the incomplete
  qualification-pin gate. All resource-content projections, 58 recipes,
  migration identities, track geometry and retained coverage checks pass.

Full unit/ECJ/source-GameTest, complete Eclipse, all-jar reproduction,
cold-cache explicit input paths with spaces, packaged family and Forge 66.1.5
compatibility gates remain blocked on exact parent/dependency handoffs.

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
The retained source GameTest now also decodes all 21 exact packaged tables with
the actual registered Electric items; that server gate still awaits Power.
The chest table retains all nine item weights and count ranges (48 item weight,
144 empty weight). Its native `modifier`/`type`/uniform provider fields are
round-tripped. The separate legacy `additional-loot-tables` config export in
`TreasureChests` remains unchanged for Power's shared importer; preserving its
legacy count-function contract is a parent integration gate.

No 26.2 receipt is 26.3 proof. Combined family, graphical, endurance and manual
acceptance gates remain separate. No push, PR, tag or publication is authorized.
