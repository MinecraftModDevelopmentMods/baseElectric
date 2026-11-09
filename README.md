# baseElectric

baseElectric continues DrCyano's Electric Advantage. It adds electric machines,
generation, storage, automation and defence to basePower, and its blocks and items
appear in the shared basePower creative tab.

This is **baseElectric 2.2.11.118021 for Minecraft 1.18.2**.
Back up your world before upgrading. The port retains the mappings for
older Electric Advantage saves, as well as saves that already use baseElectric IDs.

## Target

- Forge `40.3.0`, official `1.18.2` mappings, MCP `20220404.173914`.
- ForgeGradle `7.0.34`, Renamer `1.1.5`, Gradle `9.6.1`.
- Java 17 runs Gradle; Java 25 runs Mavenizer. Production and tests use
  Java 17, class major `61`. The Eclipse generator also needs Java 8
  for ForgeGradle's Slime Launcher tooling; the game launches use Java 17.
- Player artifact: `build/libs/baseElectric-2.2.11.118021.jar`.
  The separate deobfuscated API artifact stays under `build/libs-dev`.

basePower is required. Base Metals remains optional in Forge metadata;
the existing required CurseForge installation relationship and ordering are unchanged.
basePower, Base Metals and OreSpawn are separate dependencies, never embedded.

## Current State

The port keeps the 96-test suite, registry paths, machine NBT, loot and recipes.
See [the 1.18.2 port notes](docs/NATIVE118.md) for the exact inputs, evidence and
remaining gates. Results in the [1.17.1 notes](docs/NATIVE117.md) and
[1.16.5 notes](docs/NATIVE116.md) are historical.

## Dependencies

The [basePower 2.4.10.118021 release](https://github.com/MinecraftModDevelopmentMods/basePower/releases/tag/2.4.10.118021)
provides the player jar. The build pins its source, development jar and test
bootstrap separately. Base Metals
`3.1.2.118021`, OreSpawn `4.1.0.118021` and the Forge installer also have
explicit hashes in `gradle.properties`. Hosted checks build the development
dependencies and verify the published basePower jar from MMD Maven.

Run from this repository with the explicit input arguments below:

```text
./gradlew --no-build-cache --rerun-tasks build verifyLocalRuntimeInputs writeReleaseChecksums prepareEclipse verifyEclipseProductionClasspath
```

On Windows, use `gradlew.bat` instead of `./gradlew`. Set `JAVA_HOME` to Java 17
for Gradle and production, with Java 25 for Mavenizer and Java 8 for Eclipse
launch generation. The checksum-verified IAF118 Mavenizer fixture fixes a
decompiler error in Forge 40's `Holder.Direct` record.

Append these arguments using immutable local inputs. In particular, the build
does not discover a basePower jar or bootstrap source in a sibling checkout:

```text
-PpowerAdvantageDeobfJar="/path/to/basePower-2.4.10.118021-deobf.jar"
-PpowerAdvantageRuntimeJar="/path/to/basePower-2.4.10.118021.jar"
-PbaseMetalsDeobfJar="/path/to/BaseMetals-3.1.2.118021-deobf.jar"
-PbaseMetalsRuntimeJar="/path/to/BaseMetals-3.1.2.118021.jar"
-PoreSpawnDeobfJar="/path/to/OreSpawn-4.1.0.118021-deobf.jar"
-PoreSpawnRuntimeJar="/path/to/OreSpawn-4.1.0.118021.jar"
-PpowerAdvantageUnitBootstrapSource="/path/to/ForgeCapabilityBootstrap.java"
-PmavenizerCompatibilityJar="/path/to/minecraft-mavenizer-0.5.21-orespawn-1.18.jar"
-PforgeInstallerJar="/path/to/forge-1.18.2-40.3.0-installer.jar"
```

The build checks these inputs against the pinned hashes; do not substitute jars
from another Minecraft version. If Gradle cannot find the installed JDKs, pass
`-Porg.gradle.java.installations.paths=<java8-home>,<java17-home>,<java25-home>`.
For a prepared offline cache, also set `ORESPAWN_MAVENIZER_OFFLINE=true` and use
Gradle's `--offline` flag.

Tests use basePower's hash-checked, build-only Forge capability bootstrap source
from `-PpowerAdvantageUnitBootstrapSource`. Electric's build-only wrapper also
runs Forge's event-bus plugin for the native tier/harvest checks. Neither replaces
capability tokens or events, and neither is included in production jars.
Independent ECJ verification uses `tools/verification/Verify-ElectricEcj.ps1`
after Gradle has produced a fresh classpath, unit bootstrap jar and complete test inventory.

Static checks need no Gradle or game launch:

```text
node tools/verify-native-port.js --report build/baseelectric-118-static-unique.json
node tools/generate-recipe-unlocks.js --check
```

Use a new report name on each run. The audit checks the historical native
baselines and the exact port delta from the PA117 clone point. It does not prove compilation, native ticking,
shader compilation or migration safety.

## Compatibility

The mod ID remains `baseelectric` and the implementation package is
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
Use Java 17 for the generated client and server launches, not Java 8.
Java 17 is for Gradle and Buildship; Java 25 is for Mavenizer.

Test only disposable copies of worlds. Preserve originals, backups, failed
receipts and version-specific checkouts. Manual GUI interaction and captions,
hydroturbine animation, sound, multiplayer, weapons and player/entity behavior
remain separate acceptance gates.
