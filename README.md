# baseElectric

**Minecraft 26.3 Forge, version 2.2.19.2603001. Manual testing accepted; release preparation in progress.**

baseElectric continues DrCyano's Electric Advantage and uses basePower's shared
API, machines and creative tab. Existing `baseelectric` registry and saved-data
identities, recipes, balance and public contracts are retained.

This port targets Forge **66.0.0**, official Minecraft **26.3** names,
MCP **20260918.230105**, Java **25.0.3+9**, ForgeGradle **7.0.34** and the
checksum-pinned Gradle **9.6.1** wrapper. Native data/client pack formats are
**121.0/97.1**. Forge **66.1.5** has a separate bounded server check, not the
full 66.0.0 baseline suite.

The [published 26.2 release record](docs/RELEASE262.md) keeps its public
downloads and exact source/hash separate from this 26.3 candidate. The
[26.1.2 release](https://www.curseforge.com/minecraft/mc-mods/baseelectric/files/9577930)
also remains available for that Minecraft version.

## Dependencies

Use published [basePower **2.4.18.2603001**](https://www.curseforge.com/minecraft/mc-mods/basepower/files/9589309),
[Base Metals **3.1.2.2603001**](https://www.curseforge.com/minecraft/mc-mods/base-metals/files/9589195)
with the shared migration hook, and
[OreSpawn **4.1.1.2603001**](https://www.curseforge.com/minecraft/mc-mods/mmd-orespawn/files/9589145)
with the retained-settings repair. All three player downloads were checked
against their release builds; their bytes match the profile04 dependencies.
Power is also available from [MMD Maven](https://maven.moddev.zone/releases/zone/moddev/mc/basepower/basePower/2.4.18.2603001/basePower-2.4.18.2603001.jar)
and [GitHub Releases](https://github.com/MinecraftModDevelopmentMods/basePower/releases/tag/2.4.18.2603001).
Power source is `4fae0a51feb3e49875f09c500b869266122a711a`, player SHA-256
`97525CE9C75509C55225E0061D43BAF9D29BDBDE6DF853E1552A64A54C14C3DE`.
The test bootstrap and support sources remain independently sealed to
`c0eb83f7ba91cf8c83caea75e63057407a345949`; they do not follow the player source pin.
This handoff includes the native unlock, fuel-hook and scoped pending-item-tag
phase repairs, complete native loot-table and legacy-importer repairs, and
strict retirement/archive handling for removed vanilla brain memories, and
optional recipe-tag discovery that survives mode changes. Earlier candidate
receipts, including failed/superseded attempts, remain historical and do not
qualify this changed input. Completed
checks remain bound to their exact inputs. Cold/reproduction and the combined
gameplay tests passed. Manual testing of that exact kit was accepted on
13 November 2026. A published-pin Electric candidate does not inherit that
proof merely because its dependency player bytes are unchanged.
An independent SHA-sealed Electric 2.2.18 jar fixture protects all 21 loot
tables through the actual native whole-table codec. The strict inventories
are 119 units and 30 native tests, including the added golden comparison.
The older 26.2 jars and public 26.3 BM 3.1.1/OS 4.1.0 jars
are not substitutes. Base Metals remains required on both client and server.

The Forge 66 Mavenizer fixture and its source rules, patch, licence and
provenance are checksum-sealed from the read-only OS263 reference checkout.
The established `-Pforge64MavenizerJar` property is retained as a path alias.
Dependencies stay separate mods, with byte checks on explicit input paths.

## Local Checks

Use Temurin 25.0.3+9 and clear `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS` and
`_JAVA_OPTIONS`. This checkout uses its own Gradle home at
`D:/GradleCaches/baseelectric-263-port`. Sibling-independent checks:

```text
./gradlew nativeResourceChecks --no-build-cache --no-daemon
node tools/generate-recipe-unlocks.js --check
```

With the exact dependency and unit-support pins:

Unit support keeps its separately qualified Power source revision in
`unit_source_ref`. Pass that source directory with `-PpowerUnitSource`
when the sibling Power checkout is newer; the four source hashes remain fixed.
Hosted builds check out this test-only revision separately from the public
Power player pin.

```text
./gradlew clean check nativeResourceChecks build javadoc verifyReleaseDependencies verifyReleaseArtifacts writeReleaseChecksums --no-build-cache --no-daemon
./gradlew verifyEclipseProductionClasspath --no-daemon
node tools/verify-native-port.js
```

The normal Power path remains `../../PowerAdvantage`. Explicit
`*DeobfJar` and `*RuntimeJar` flags select paths, never different bytes.
Full unit, ECJ, GameTest, independent reproducibility, fresh cold-cache and
paths-with-spaces gates remain required. See [26.3 status](docs/NATIVE263.md)
for the current scope, blocked contracts and retained attempts.

## Qualification Boundary

The matching basePower checkout's `docs/26.3/QUALIFICATION.md` records the
completed machine, recipe, old-world upgrade, graphical, full-restart and
two-world 30-minute endurance checks against the accepted player jar
`44ED38BFC7040D73251E523B2803718CDA2212482268A7E65BCF4681CF9A75E8`.
Manual testing was accepted on 13 November 2026. Publication is authorized
in dependency order, but this release is not public yet. The 26.2 results in
[the historical status](docs/NATIVE262.md) and the older records below
qualify only their original inputs. Power's verified public release permits
Electric source PR and CI work. Electric publication still waits for public
Steam and the parent's explicit qualification gate for the final jar.
If pin metadata changes its player hash, the new family needs its own
combined checks and two 30-minute worlds. Hosted builds retain the merged/public
dependency gates and remote Maven publication stays disabled.

## Historical Candidate09 Record (12 November 2026)

This section records the earlier 26.1.2 work, not qualification of this 26.3 candidate.

The previous candidate09 passed 117 unit tests, 28 independent native resource
tests and 117 tests from fresh ECJ output, with no failures or skips. The ECJ
gate compares every compiled test method with the Gradle XML inventory.
Electric registers its pure JSON migration defaults during construction, after
the old OS1 importer and before lifecycle setup. Migration refusal or an I/O
failure stops startup; old provider identities and saved choices are not
silently replaced by fresh defaults.
Both independent reproduction builds match all three jars exactly; each passes
117 unit and 28 native checks. One reuses the previously cold-generated engine
cache, without claiming a new cold download. All four named source GameTests
also pass on these pins. Their first-boot warnings remain in the receipts;
this is not warning-free or manual gameplay proof. The current receipts are in
`build/port-evidence/candidate09-lifecycle-pin-01`. Candidate06's four
GameTests, reproductions and narrow packaged smoke remain historical proof,
not qualification of the new player jar. Combined gameplay, restart, migration
and actual client checks remain separate parent-owned gates.
The Power pin includes the parent's retained-profile, provider-schema and
profile-loading lifecycle repairs, without an OreSpawn patch or log waiver.
Candidate07's actual OS1 startup failure remains recorded separately, despite
its passing local build checks. Candidate08 retained-profile runs passed the
original OS1 route but failed both released-source first boots on the old quartz
output; passing restarts do not waive those failures. Further family runs are
held until the current retained-profile gates are resolved. Candidate09's saved
Forge alias is valid, but the old canonical-ID-only audit still records quartz
failures; a separate Base Metals duplicate-placement review is also unresolved.
Each qualification receipt retains its exact input hashes. In particular,
no old screenshot or earlier version's result qualifies this port.

The atlas and abstract-battery definition repairs have native codec checks and
a real registered-item census. Their sources and packaged archives reject
global directories, foreign namespaces, missing sprites and duplicates across
atlases. The earlier diagnostic asset jar is not a release or final proof.

Run `runGameTestServer` with the same pinned mod inputs, an existing accepted
EULA passed as `-PelectricGameTestEula=<path>`, and a new directory passed as
`-PelectricGameTestRunDirectory=build/port-evidence/<run>`. The test code and
its structure are not in the production jar.

Run the packaged smoke script only with the exact pinned jars, a matching Forge
server seed and an already accepted EULA. It stages launcher files only and
creates a fresh loopback-only world under `build/port-evidence`. For an upgrade,
use a separate disposable copy of a world. Neither a build nor this smoke
substitutes for manual gameplay or client verification.
