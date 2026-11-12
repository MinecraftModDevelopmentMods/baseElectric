# baseElectric

baseElectric continues DrCyano's Electric Advantage. It adds electric machines,
generation, storage, automation and defence to basePower. Its equipment appears
in the shared basePower creative tab.

This checkout is the local **2.2.17.2601021** port for Minecraft **26.1.2** and
Forge **64.1.5**, using official runtime names, MCP **20260409.101008**,
ForgeGradle **7.0.34**, Gradle **9.6.1**, and Temurin **25.0.3+9**.
It is not a released or qualified build yet.

basePower **2.4.16.2601021** and Base Metals **3.1.2.2601021** are required on
both client and server. Their local source and ordinary-jar checkpoints are
recorded in `gradle.properties`; they are not public releases or a completed
family qualification. The build rejects old 1.21.11 inputs. OreSpawn
**4.1.0.2601021** is pinned to its public release. See the [26.1.2 notes](docs/NATIVE2612.md).

The completed [1.21.11 release](https://www.curseforge.com/minecraft/mc-mods/baseelectric/files/9567881)
and its [qualification notes](docs/NATIVE12111.md) are historical evidence,
not proof of this port. New gameplay, migration and graphical checks must use
the actual 26.1.2 jars.

Back up your world and try the upgrade on a copy. Replace Electric Advantage
and Power Advantage with baseElectric and basePower; do not install both sets.

## Building locally

Use Temurin 25.0.3+9 for Gradle, Minecraft, compilation, tests, Mavenizer and
Slime Launcher. No Java 8 helper is required. Set
`-Porg.gradle.java.installations.paths=<java25-home>` if needed.
Use the isolated `D:/GradleCaches/baseelectric-2612-port` cache for local work.

The independent Forge/resource checks do not require sibling jars:

```text
./gradlew nativeResourceChecks compileNativeApiDiagnostics verifyRecipeTagGuards processResources --max-workers=2 --no-daemon
node tools/verify-native-port.js
node tools/generate-recipe-unlocks.js --check
```

For a full local build, use the exact local dependency pins in
`gradle.properties`. Path overrides never replace source or hash checks:

```text
-PpowerAdvantageRuntimeJar=/path/to/basePower-2.4.16.2601021.jar
-PbaseMetalsRuntimeJar=/path/to/BaseMetals-3.1.2.2601021.jar
-PforgeInstallerJar=/path/to/forge-26.1.2-64.1.5-installer.jar
```

Forge 64 uses the ordinary native jar for both play and development. The former
`*DeobfJar` path flags remain aliases for the same bytes. OreSpawn and the
Mavenizer fixture default to the sibling `MinecraftMineralogy/OS2612/OreSpawn`
checkout. Use `-PoreSpawnRuntimeJar` and `-Pforge64MavenizerJar` to override paths.
For the prepared cache, use `--offline` with `ORESPAWN_MAVENIZER_OFFLINE=true`.

```text
./gradlew check build javadoc verifyLocalRuntimeInputs writeReleaseChecksums verifyEclipseProductionClasspath --no-build-cache
```

Use `gradlew.bat` on Windows. Output jars go to `build/libs`. Dependencies
remain separate mods. The 26.1.2 CI and CodeQL jobs require merged source pins
and checksum-matching public dependencies before checkout or compilation.
Local jars do not satisfy that gate.

## Verification

The current local candidate passes 117 unit tests, 28 independent native resource
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
