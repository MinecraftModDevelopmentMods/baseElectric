# baseElectric

**Minecraft 26.2 Forge, version 2.2.18.2602001. Combined checks passed; final root build pending.**

baseElectric continues DrCyano's Electric Advantage and uses basePower's shared
API, machines and creative tab. Existing `baseelectric` registry and saved-data
identities are retained.

This port targets Forge **65.1.0**, official Minecraft **26.2** names,
MCP **20260616.103818**, Java **25.0.3+9**, ForgeGradle **7.0.34** and the
checksum-pinned Gradle **9.6.1** wrapper. Native pack formats are data
**107.1** and client **88.0**.

## Build

The coordinated local dependency versions are basePower **2.4.17.2602001**
and Base Metals **3.1.2.2602001**. The Base Metals hook source is
`523335a59ecff399e40a7c66eb121a3aabdc45ba`, player SHA-256
`15CBD0C8DD95BBA846969929B8767887BA4B8152DFB10C9C58FB314AD39FB9E1`.
Base Metals is REQUIRED on both client and server, not an optional integration.
The approved local Power262 handoff is source
`f84b2362449e39eff5e76696dd64ce4cb144da4f`, player SHA-256
`7E456DE1A45140E86415A701D186FEDE08C4AB63D53C6B7848935DB70CBC68B1`.

OreSpawn **4.1.1.2602001** is the approved local retained-settings repair,
source `dcaaa933299dd8b1ebc73e31c9e5acd36dd7957c`, SHA-256
`BC0658DF2E149CC23E65E5771F3FF62E2BB13FFDA4E316AB632EB92EF7FA5A1F`.
It is not published; the public file-ID pin is deliberately empty. The older
4.1.0 public download is not substituted for these bytes.

The Mavenizer fixture and its source-rule, patch, licence and provenance files
are checksum-sealed from the existing OS262 checkout. The established
`-Pforge64MavenizerJar` property remains a path alias; it selects the Forge65
fixture here. Dependencies stay separate mods and must match the source and
artifact pins in `gradle.properties`.

Use Temurin 25 and clear `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS` and
`_JAVA_OPTIONS` before qualification. The prepared sibling layout is the
default; explicit `*DeobfJar` and `*RuntimeJar`/`*PlayerJar` flags change
paths, not the required bytes. With these exact coordinated inputs:

```text
./gradlew clean check build javadoc verifyReleaseDependencies verifyReleaseArtifacts writeReleaseChecksums --no-build-cache --no-daemon
./gradlew verifyEclipseProductionClasspath --no-daemon
node tools/verify-native-port.js
node tools/generate-recipe-unlocks.js --check
```

## Qualification

Combined profile03 passed the machine, migration, actor/dimension, actual
transformer-order and real-client checks, plus two 30-minute worlds, restart
and independent 66-checkpoint replay. See [26.2 port status](docs/NATIVE262.md)
for the counts and exact inputs. The [family qualification record](../../PowerAdvantage/docs/26.2/QUALIFICATION.md)
is being finalized in the sibling Power checkout.

Ready for manual testing once the final root cold-cache gate is confirmed.
Overall sign-off and user manual acceptance are not claimed here. Base Metals
and OreSpawn are unpublished local 26.2 inputs; these changes remain local,
with no 26.2 push or publication. The accepted 26.1.2 records remain historical
proof of their original bytes, not qualification of this port.

## Candidate09 Verification History

This section records the earlier 26.1.2 work, not qualification of this 26.2 candidate.

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
