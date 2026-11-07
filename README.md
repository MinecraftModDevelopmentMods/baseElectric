# baseElectric

baseElectric continues DrCyano's Electric Advantage. It adds electric machines,
generation, storage, automation and defence to basePower, and its blocks and items
appear in the shared basePower creative tab.

This is the local **Minecraft 1.17.1 candidate, 2.2.10.117011**, not a published
release. Back up your world before upgrading. The port retains the mappings for
older Electric Advantage saves, as well as saves that already use baseElectric IDs.

## Target

- Forge `37.1.1`, official `1.17.1` mappings, MCP `20210706.113038`.
- ForgeGradle `7.0.34`, Renamer `1.1.5`, Gradle `9.6.1`.
- Java 17 runs Gradle; Java 25 runs Mavenizer. Production and tests use
  Java `16.0.2+7`, class major `60`. The Eclipse generator also needs Java 8
  for ForgeGradle's Slime Launcher tooling; the game launches use Java 16.
- Production artifact: `build/libs/baseElectric-2.2.10.117011.jar`.
  The separate deobfuscated API artifact stays under `build/libs-dev`.

basePower is required. Base Metals remains optional in Forge metadata;
the existing required CurseForge installation relationship and ordering are unchanged.
basePower, Base Metals and OreSpawn are separate dependencies, never embedded.

## Current State

The identity port keeps the native 1.17.1 models, shader, GUI, loot and recipe
book behavior. The 96-test suite is retained, with additional legacy ID cases in
the existing migration test. The prior Electric Advantage build and ECJ results
are historical; they do not qualify this candidate. Local builds are recorded
against exact source and artifact hashes. Packaged world migration and manual
gameplay are separate checks.

See [the native port notes](docs/NATIVE117.md) for the API audit and remaining gates.
The retained [1.16 notes](docs/NATIVE116.md) describe the previous target, not proof
that this one passes.

## Dependencies

The coordinated basePower `2.4.9.117011` is pinned to local source
`308d42a89483d56d0fad4805cdb65dac483d9e31`. Its development jar, player jar,
and test bootstrap have separate SHA-256 pins in `gradle.properties`.
Base Metals `3.1.1.117011` is pinned to the local startup fix
`0deeeeecdbbc1fbcdecf910c59477de8efe164d7` and its verified deobf SHA-256 in
`gradle.properties`. That source is unpushed; hosted builds cannot fetch it.
OreSpawn `4.1.0.117011` uses public source
`b56e2b62a1a6ea4d1dd96f8646a87801a817dbdf`.
Record the runtime SHA-256 values separately from the development jar hashes.
Local runtime qualification must use the exact coordinated jars; it does not
establish public availability or release readiness.

Once the pins are set, run from this repository with the arguments below:

```text
./gradlew clean check build javadoc verifyReleaseDependencies verifyReleaseArtifacts writeReleaseChecksums verifyEclipseProductionClasspath
```

On Windows, use `gradlew.bat` instead of `./gradlew`. Set `JAVA_HOME` to Java 17
for Gradle and install the Java 16 production toolchain, Java 25 Mavenizer
runtime and Java 8 build tooling listed above.

Append these arguments using immutable local inputs. In particular, the build
does not discover a basePower jar or bootstrap source in a sibling checkout:

```text
-PpowerAdvantageDeobfJar="/path/to/basePower-2.4.9.117011-deobf.jar"
-PbaseMetalsDeobfJar="/path/to/BaseMetals-3.1.1.117011-deobf.jar"
-PoreSpawnDeobfJar="/path/to/OreSpawn-4.1.0.117011-deobf.jar"
-PpowerAdvantageUnitBootstrapSource="/path/to/ForgeCapabilityBootstrap.java"
```

The build checks these inputs against the pinned hashes; do not substitute jars
from another Minecraft version. If Gradle cannot find the installed JDKs, pass
`-Porg.gradle.java.installations.paths=<java8-home>,<java16-home>,<java17-home>,<java25-home>`.
No machine-specific helper or shared Gradle cache is required.

Tests use basePower's hash-checked, build-only Forge capability bootstrap source
from `-PpowerAdvantageUnitBootstrapSource`. Electric's build-only wrapper also
runs Forge's event-bus plugin for the native tier/harvest checks. Neither replaces
capability tokens or events, and neither is included in production jars.
Independent ECJ verification uses `tools/verification/Verify-ElectricEcj.ps1`
after Gradle has produced a fresh classpath, unit bootstrap jar and complete test inventory.

Static checks need no Gradle or game launch:

```text
node tools/verify-native-port.js --report build/baseelectric-117-static-unique.json
node tools/generate-recipe-unlocks.js --check
```

Use a new report name on each run. The audit checks the merged 1.16.5 and
pre-rename native 1.17.1 baselines. It does not prove compilation, native ticking,
shader compilation or migration safety.

## Compatibility

The mod ID is now `baseelectric` and the implementation package is
`zone.moddev.mc.baseelectric`. The old namespace maps to the new one through
basePower's migration API. Registry paths after the namespace, machine NBT keys,
and the legacy `cyano.poweradvantage.api` integration are retained.
Native constructors carry each machine's real position and block state, including
moving drills. Numeric machine constants and existing recipes are unchanged.

All 46 crafting variants and four smelting recipes retain their recipe-book
unlocks. The eight Base Metals crushing recipes remain non-crafting entries.
The crusher continues to consume the real Base Metals recipe implementation;
no replacement raw-metal conversion or balance change is introduced.

Eclipse imports use the Gradle wrapper, processed resources and production-only
launches. Generate metadata through `verifyEclipseProductionClasspath`; do not
hand-edit generated classpaths or disable restricted-reference tests.
Repeated refreshes replace stale runtime entries while keeping the native Forge
launcher and excluding compile-only libraries from production launches.
Use Java `16.0.2+7` for the generated client and server launches, not Java 8.
Java 17 is for Gradle and Buildship; Java 25 is for Mavenizer.

Test only disposable copies of worlds. Preserve originals, backups, failed
receipts and version-specific checkouts. Manual GUI interaction and captions,
hydroturbine animation, sound, multiplayer, weapons and player/entity behavior
remain separate acceptance gates.
