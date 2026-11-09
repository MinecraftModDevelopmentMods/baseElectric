# baseElectric

baseElectric continues DrCyano's Electric Advantage. It adds electric machines,
generation, storage, automation and defence to basePower. Its equipment appears
in the shared basePower creative tab.

This branch targets Minecraft **1.20.1** with Forge **47.4.10**, official mappings,
ForgeGradle **7.0.34**, and Java **17**. The local candidate is **2.2.13.120011**.
Registry names, machine save data, and legacy Electric Advantage migration
mappings are unchanged.

## Building locally

Use Java 17 for Gradle, the game and Eclipse Buildship. ForgeGradle uses Java 25
for Mavenizer and Java 8 for its Slime Launcher tooling. If necessary, supply
`-Porg.gradle.java.installations.paths=<java17-home>,<java25-home>,<java8-home>`.
Use an isolated Gradle user home for clean-room checks.

Pass the following input paths to the Gradle wrapper. Each input is checked against
the version and SHA-256 recorded in `gradle.properties`.

```text
-PpowerAdvantageDeobfJar="/path/to/basePower-2.4.12.120011-deobf.jar"
-PpowerAdvantageRuntimeJar="/path/to/basePower-2.4.12.120011.jar"
-PpowerAdvantageUnitBootstrapSource="/path/to/ForgeCapabilityBootstrap.java"
-PpowerAdvantageUnitBootstrapJar="/path/to/forge-native-unit-bootstrap.jar"
-PbaseMetalsDeobfJar="/path/to/BaseMetals-3.1.2.120011-deobf.jar"
-PbaseMetalsRuntimeJar="/path/to/BaseMetals-3.1.2.120011.jar"
-PforgeInstallerJar="/path/to/forge-1.20.1-47.4.10-installer.jar"
```

By default the OreSpawn development and player jars, and its audited Forge 47
Mavenizer fixture, come from the sibling `MinecraftMineralogy/OS1201/OreSpawn`
checkout. Use `-PoreSpawnDeobfJar`, `-PoreSpawnRuntimeJar` and
`-Pforge47MavenizerJar` to point to the exact same inputs in another layout.
For a prepared offline cache, use `--offline` and
`ORESPAWN_MAVENIZER_OFFLINE=true`.

```text
./gradlew clean check build javadoc verifyLocalRuntimeInputs writeReleaseChecksums verifyEclipseProductionClasspath --no-build-cache
node tools/verify-native-port.js
node tools/generate-recipe-unlocks.js --check
```

Use `gradlew.bat` on Windows. The player, sources and Javadoc jars go to
`build/libs`; the deobfuscated jar goes to `build/libs-dev`. The test agent is
used only by the unit tests. basePower, Base Metals and OreSpawn remain separate
mods. Base Metals is optional in Forge metadata and remains required on
CurseForge.

## Eclipse and testing

Import this checkout as an existing Gradle project named `ElectricAdvantage`.
Use the generated `runClient.launch` or `runServer.launch` after
`verifyEclipseProductionClasspath`. The build checks package paths against
Gradle and Eclipse source roots and keeps test classes out of production runs.
`tools/verification/Verify-ElectricEcj.ps1` independently compiles and runs the
test inventory using Eclipse's compiler.

The [1.19.4 notes](docs/NATIVE119.md) and [1.18.2 notes](docs/NATIVE118.md) are
historical. To check a world upgrade, test only a disposable copy. A build and
resource audit do not substitute for a migration, gameplay or client check.
