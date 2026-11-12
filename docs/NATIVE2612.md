# Minecraft 26.1.2

This is a local port, not a release. The starting source is the released
1.21.11 merge `951fc3921f27e963e96f29c78a38ee7093a65fde`.

Candidate01 was stopped after the parent found an actual Power inventory-lock
reflection failure during finite operations. The parent repaired that code.
Historical candidate01 results remain bound to their original inputs, including Electric
`D836D4AC22D1C46BFCD98B77A10A9475429D95141192B9C632F59DC87C273D73`.
They must not be relabeled as candidate02 or full family acceptance.

## Current Checkpoint

The frozen candidate06 player jar is
`DFC63C4E619A74BEE2A22146D9720ADB2B32801865EAE1B716726424015775FA`.
It uses Power `0ac874de66f792c6efca5bccd0536b1b113fb786`, ordinary jar
`618C3F96736392CE2853575F5A6A076EAC2A9B9F76F9303D86DEF58E418857D0`,
and Base Metals `331fa46914f854db71a8c33500cc559a8878b7d3` / `67862388...`.
Java classes and PNGs remain byte-identical to the previous `1936D3E2...`
candidate. The intended resource differences are the two atlas definitions and
removal of the abstract battery's standalone item definition, plus dependency
provenance in the manifest. This is not final family or manual acceptance.

Actual native client loading exposed an 8-pixel direct-render laser beam being
included by Power's global materials directory, and a standalone item definition
for the unregistered abstract battery parent. Hegel replaced the atlas imports
with explicit namespace-owned sprites. Electric's common native registry census
confirms `battery_base` is absent and all four real batteries are registered.
Only `items/battery_base.json` is removed; its inherited parent model and every
PNG remain unchanged. A focused resource regression fails before that deletion
and passes after it, including native atlas codec and required-sprite checks.

The earlier diagnostic asset jar is
`982D94A4B469216643D1B6609B3C16990447F82DD4A47BD155EE00CA91BE60C4`.
Against the intermediate jar it changes only the blocks atlas, adds the items
atlas and removes the abstract item definition. All class and PNG bytes match.
It uses the sealed `000A...` compile input and is not final qualification.
Its original receipt is retained. Candidate06 uses the later coherent atlas
pins and needs its own graphical, migration and endurance receipts.

The current candidate's forced fresh 51-task suite passes 115 unit and 28 native resource tests,
with zero failures or skips. Fresh ECJ output passes all 115 tests with the
complete compiled-method inventory and negative controls. All four named
source GameTests and both new-pin reproduction builds pass. Receipts are
recorded separately in `build/port-evidence/candidate06-final-assets-01`.
This local checkpoint is not final family qualification.

The parent repaired the shared migration context after candidate05's actual
original110 actor run lost four Steam enchantments from cart and dropped-item
stacks. The raw failure, source lineage and independently assessed dimension
results remain bound to candidate05. New qualification must use the repaired
Power and Base Metals inputs below; no old result is relabeled for candidate06.
The intermediate `fb5fb0e` source-pin jar is retained separately and is not the
final candidate. The later `540bc666` context jar and `4D2C5563...` child also
remain historical, not relabeled as the atlas-repaired candidate06.

## Target

- Minecraft 26.1.2, Forge 64.1.5, MCP 20260409.101008, official runtime names.
- Temurin 25.0.3+9 for all roles, ForgeGradle 7.0.34, Gradle 9.6.1, `reobf=false`.
- baseElectric 2.2.17.2601021; basePower 2.4.16.2601021 and Base Metals 3.1.2.2601021 required.
- OreSpawn 4.1.0.2601021, source `c45ea2a111520ef904e994dd8dcd90babfcef29b`,
  ordinary jar SHA-256 `6D5CE2017C3314968083D140309E2F1A54BC239AC65C48944C995E9ECF937948`.

The local Power checkpoint is `0ac874de66f792c6efca5bccd0536b1b113fb786`;
the Base Metals shared-hook checkpoint is `331fa46914f854db71a8c33500cc559a8878b7d3`.
Their exact ordinary jar hashes are recorded in `gradle.properties`.
These are local integration inputs, not public releases or completed family
qualification. Earlier provisional diagnostics remain under their original
hashes and scope labels.

## Compatibility

Registry IDs, legacy aliases, flat NBT keys, recipes, unlock IDs and machine
balance remain unchanged. Native recipe assembly takes only the input;
results use native stack templates. Client renderers use native extraction
and the target camera-state package. Rendered scenes remain a required gate.

The old villager trade event no longer exists. The six Electric librarian
offers are now `villager_trade` resources, added to the existing novice and
apprentice tags without replacing vanilla offers. The same quantity and
emerald ranges, seven uses, one XP and zero price multiplier are retained.

Root pack metadata declares data format 101.1. Forge's hidden client pack
must synthesize native resource format 84.0. Both paths are tested through
actual Forge discovery, with strict warning and error rejection unchanged.

## Verification Status

The independent native suite passes 28 tests without skips. All 58 recipe
resources and 50 unlocks are retained. Native pack discovery accepts resource
84.0 and data 101.1, with zero warning/error diagnostics.

Compilation of all 87 production sources and all 36 test sources
passes against Power `618C3F96736392CE2853575F5A6A076EAC2A9B9F76F9303D86DEF58E418857D0`
and Base Metals `6786238835D4D8A653C769F3B0E61D226959146300E17523799CFDF17540FD19`.
All 115 unit tests pass without skips. Fresh ECJ output also passes 115 tests;
main, test, GameTest and client-contract scopes compile as Java 25. Both pinned
unit-support sources are compiled with the tests. The separately rebuilt agent
must match `088FC137BEBB9B2E75C596DE868499E6099CF228BECFCF07EAE36791245FD1B6`.
It never enters production jars or game launches. The ECJ gate scans every
fresh compiled `@Test` method, rejects filtered/stale XML and unreviewed runners,
and retains forbidden-JDK-access and namespace-exclusion negative controls.

The full 51-task local release/dependency/artifact/Eclipse suite passes against
these local pins. Native resource children use an explicit four-CPU argument,
clear/reject all three inherited JVM-option variables and record runtime
arguments and classpaths. No warning or error filtering was added.

All four named source GameTests passed again against the current Power pin on
actual Forge 64.1.5, followed by normal save and shutdown:

- `baseelectric:drillupgradesandtransferslegacybattery`
- `baseelectric:recipeavailabilitydoesnotremovediscovery`
- `baseelectric:siliconandsolderresolverealmaterialtags`
- `baseelectric:turretusesnativeupdatepacket`

The XML and runtime receipt are in `candidate06-final-assets-01/gametest-01`.
This is source-output GameTest evidence, not frozen-player-jar proof. The
first-boot log retains configuration-default corrections and native union-URL
warnings; its clean-log gate is not green. The runtime receipt records Java
25.0.3+9, four CPUs, explicit JVM arguments and absent inherited JVM options.

The previous Power pin's two independent source snapshots in paths containing spaces pass the same
51-task suite, each with 115 unit and 28 native checks and no skips. One uses
the established isolated cache; the other starts with a new empty cache,
downloads Gradle and native inputs, and regenerates the Forge engine. That
engine and all three Electric jars match the primary bytes exactly. The
completed warm/cold receipts remain under `candidate02-2612-01`. These runs
started before the fuel-repair hold; no later pin is covered by them. The new
checkpoint has separate passing reproduction receipts, using the established
cache and the previously cold-generated engine cache rather than repeating
downloads. Both use new source snapshots and output directories containing
spaces, force all 51 tasks without a build cache, and reproduce the ordinary,
sources and Javadoc jars exactly. Both again pass 115 unit and 28 native checks.

The current frozen ordinary Electric and Power jars also pass the packaged
server smoke's 19 narrow checks, followed by normal save/stop. The run uses
Java 25 with explicit CPU arguments and no inherited JVM-option variables.
It is not a restart, whole-world, fuel-container or clean-log qualification.

GUI/assets/scenes and combined development and packaged classpaths remain
parent-owned gates. Current logs, including failures, are retained under
`build/port-evidence/initial-2612-01` for candidate01 and the separate candidate02
folder and separate candidate05/candidate06 folders; none is a release acceptance claim.

A candidate01 packaged smoke already in flight at the hold completed 19 narrow
checks, including drill inventory conversion, battery components, sulfur drops,
damage types and a normal save/stop. It did not exercise the failed inventory
lock path, a restart, or a whole-world upgrade. It remains historical evidence.

The approved Base Metals and public OreSpawn jars both retain their exact
`26.1.2-64.0.9` build stamps. Electric targets runtime 64.1.5 and audits those
dependency stamps separately without rebuilding or changing their jars.
Actual coordinated client/server checks still determine runtime compatibility.

No protected or manual world may be started by these checks. The parent owns
combined migrations, gameplay campaigns and endurance. The historical turbine
manual-world limit is not a pass for this version.

No 26.1.2 push, PR, tag or publication is authorized.
