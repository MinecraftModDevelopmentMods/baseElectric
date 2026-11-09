# baseElectric

baseElectric continues DrCyano's Electric Advantage. It adds electric machines,
generation, storage, automation and defence to basePower, and its blocks and items
appear in the shared basePower creative tab.

This is the **Minecraft 1.19.4 port, 2.2.12.119041**. It retains the mappings for
older Electric Advantage saves, as well as saves that already use baseElectric IDs.

## Target

- Forge `45.4.0`, official `1.19.4` mappings, MCP `20230314.122934`.
- ForgeGradle `7.0.34`, Renamer `1.1.5`, Gradle `9.6.1`.
- Java 17 runs Gradle; Java 25 runs Mavenizer. Production and tests use
  Java 17, class major `61`. The Eclipse generator also needs Java 8
  for ForgeGradle's Slime Launcher tooling; the game launches use Java 17.
- Player artifact path: `build/libs/baseElectric-2.2.12.119041.jar`.
  The separate deobfuscated API artifact stays under `build/libs-dev`.

basePower is required. Base Metals remains optional in Forge metadata;
the existing required CurseForge installation relationship and ordering are unchanged.
basePower, Base Metals and OreSpawn are separate dependencies, never embedded.

## Current State

The port keeps the 96-test inventory, registry paths, machine NBT, loot and
recipes. See [the 1.19.4 port notes](docs/NATIVE119.md) for exact inputs and
verification status. Earlier port notes, including
[1.18.2](docs/NATIVE118.md), are historical.

## Dependencies

The build pins the released basePower `2.4.11.119041` source and player jar,
along with its separate development jar and test agent. Base Metals
`3.1.2.119041`, OreSpawn `4.1.0.119041` and the Forge installer have
explicit hashes in `gradle.properties`. Forge 45 CI and CodeQL use Electric's
init adapter only for pinned Power. Pinned Base Metals uses its own verified
fixture. The dependency source checkouts stay unchanged. Releases are made
through the guarded workflow, using the verified CI artifacts.

Run from this repository with the explicit input arguments below:

```text
./gradlew --no-build-cache --rerun-tasks build verifyLocalRuntimeInputs writeReleaseChecksums prepareEclipse verifyEclipseProductionClasspath
```

On Windows, use `gradlew.bat` instead of `./gradlew`. Set `GRADLE_USER_HOME` to
an isolated directory (the ignored `.gradle-user-home` was used locally).
Set `JAVA_HOME` to Java 17 for Gradle and production, with Java 25 for
Mavenizer and Java 8 for Eclipse launch generation. The checksum-verified
OreSpawn Mavenizer fixture fixes duplicate decompiled record accessors in
Forge 45.4.0. The default is the sibling `MinecraftMineralogy/OS119/OreSpawn`
checkout; use `-Pforge45MavenizerJar` for another checkout layout.

Append these arguments using immutable local inputs. In particular, the build
does not discover a basePower jar or bootstrap source in a sibling checkout:

```text
-PpowerAdvantageDeobfJar="/path/to/basePower-2.4.11.119041-deobf.jar"
-PpowerAdvantageRuntimeJar="/path/to/basePower-2.4.11.119041.jar"
-PbaseMetalsDeobfJar="/path/to/BaseMetals-3.1.2.119041-deobf.jar"
-PbaseMetalsRuntimeJar="/path/to/BaseMetals-3.1.2.119041.jar"
-PoreSpawnDeobfJar="/path/to/OreSpawn-4.1.0.119041-deobf.jar"
-PoreSpawnRuntimeJar="/path/to/OreSpawn-4.1.0.119041.jar"
-PpowerAdvantageUnitBootstrapSource="/path/to/ForgeCapabilityBootstrap.java"
-PpowerAdvantageUnitBootstrapJar="/path/to/forge-native-unit-bootstrap.jar"
-Pforge45MavenizerJar="/path/to/minecraft-mavenizer-0.5.21-orespawn-compat.jar"
-PforgeInstallerJar="/path/to/forge-1.19.4-45.4.0-installer.jar"
```

The build checks these inputs against the pinned hashes; do not substitute jars
from another Minecraft version. If Gradle cannot find the installed JDKs, pass
`-Porg.gradle.java.installations.paths=<java8-home>,<java17-home>,<java25-home>`.
For a prepared offline cache, also set `ORESPAWN_MAVENIZER_OFFLINE=true` and use
Gradle's `--offline` flag.

Tests use the exact basePower agent supplied by
`-PpowerAdvantageUnitBootstrapJar` and verify its checkpoint source via
`-PpowerAdvantageUnitBootstrapSource`. The build-only agent runs Forge's
native capability and event transforms and is not included in production jars.
Independent ECJ verification uses `tools/verification/Verify-ElectricEcj.ps1`
after Gradle has produced a fresh classpath, unit bootstrap jar and complete test inventory.

Static checks need no Gradle or game launch:

```text
node tools/verify-native-port.js --report build/port-evidence/static-119-unique.json
node tools/generate-recipe-unlocks.js --check
```

Use a new report name on each run. The audit checks preservation against the
accepted 1.18.2 clone point and historical migration contracts. It does not
prove compilation, native ticking, shader compilation or migration safety.

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
launches. The Eclipse and Gradle project name is `ElectricAdvantage`, matching
the checkout folder; the mod and archives keep the baseElectric identity.
Generate metadata through `verifyEclipseProductionClasspath`; do not
hand-edit generated classpaths or disable restricted-reference tests.
Repeated refreshes replace stale runtime entries while keeping the native Forge
launcher and excluding compile-only libraries from production launches.
Use Java 17 for the generated client and server launches, not Java 8.
Java 17 is for Gradle and Buildship; Java 25 is for Mavenizer.

Test only disposable copies of worlds. Preserve originals, backups, failed
receipts and version-specific checkouts. Manual GUI interaction and captions,
hydroturbine animation, sound, multiplayer, weapons and player/entity behavior
remain separate acceptance gates.
