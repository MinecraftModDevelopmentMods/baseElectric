# baseElectric

**Minecraft 26.3 Forge, version 2.2.19.2603001. Local port; exact-input qualification remains separate.**

baseElectric continues DrCyano's Electric Advantage and uses basePower's shared
API, machines and creative tab. Existing `baseelectric` registry and saved-data
identities, recipes, balance and public contracts are retained.

This port targets Forge **66.0.0**, official Minecraft **26.3** names,
MCP **20260918.230105**, Java **25.0.3+9**, ForgeGradle **7.0.34** and the
checksum-pinned Gradle **9.6.1** wrapper. Native data/client pack formats are
**121.0/97.1**. Forge **66.1.5** compatibility is a later, separate check.

## Dependencies

The coordinated local versions are basePower **2.4.18.2603001**, Base Metals
**3.1.2.2603001** with the shared migration hook, and OreSpawn
**4.1.1.2603001** with the retained-settings repair. Exact source and artifact
pins are recorded for the parent's immutable local compiler/test handoff.
Power source is `9d6e31a9c9d116b72a3758edfb18e81a25d939e3`, player SHA-256
`F225B3EC1B9B272CCBA82716B6D105D3BF0A613E31437BBFA0F9CAB39E4FE9BB`.
Unit-support sources and agent bytes are sealed to that accepted checkpoint.
This handoff includes the native unlock, fuel-hook and scoped pending-item-tag
phase repairs, plus complete native loot-table and legacy-importer repairs.
D03/D04 passing build/ECJ/cold results and D03's failed startup are
historical; they do not qualify this changed input. Completed
checks remain bound to their exact inputs; cold/reproduction and gameplay
acceptance are separate gates.
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

```text
./gradlew clean check build javadoc verifyReleaseDependencies verifyReleaseArtifacts writeReleaseChecksums --no-build-cache --no-daemon
./gradlew verifyEclipseProductionClasspath --no-daemon
node tools/verify-native-port.js
```

The normal Power path remains `../../PowerAdvantage`. Explicit
`*DeobfJar` and `*RuntimeJar` flags select paths, never different bytes.
Full unit, ECJ, GameTest, independent reproducibility, fresh cold-cache and
paths-with-spaces gates remain required. See [26.3 status](docs/NATIVE263.md)
for the current scope, blocked contracts and retained attempts.

## Qualification Boundary

No 26.3 combined gameplay, graphical, endurance, restart, migration, manual
acceptance or publication is claimed. The 26.2 results in
[the historical status](docs/NATIVE262.md) and the older records below
qualify only their original inputs. This branch is local-only: no push, PR,
tag or publication is authorized. Hosted builds retain the merged/public
dependency gates and remote Maven publication stays disabled.

## Candidate09 Verification History

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
