# baseElectric

baseElectric continues DrCyano's Electric Advantage. It adds electric machines,
generation, storage, automation and defence to basePower. Its equipment appears
in the shared basePower creative tab.

This local port targets Minecraft **1.20.6**, Forge **50.2.0**, official mappings,
ForgeGradle **7.0.34**, and Java **21**. The tentative version is **2.2.14.120061**.
See [the local port record](docs/NATIVE1206.md) for the checkpoint and test status.

## Building locally

Use Temurin 21.0.7+6 for Gradle and Minecraft. The pinned OreSpawn Mavenizer
fixture uses Java 25, and ForgeGradle's Slime Launcher tooling uses Java 8. Set
`-Porg.gradle.java.installations.paths=<java21-home>,<java25-home>,<java8-home>`
if Gradle cannot discover all three.

Until the exact coordinated basePower checkpoint is pinned,
the independent Forge/resource checks are available:

```text
./gradlew --offline nativeResourceChecks verifyRecipeTagGuards processResources
node tools/verify-native-port.js
node tools/generate-recipe-unlocks.js --check
```

After the pins are recorded, supply the matching files using:

```text
-PpowerAdvantageRuntimeJar=/path/to/basePower-2.4.13.120061.jar
-PbaseMetalsRuntimeJar=/path/to/BaseMetals-3.1.2.120061.jar
-PforgeInstallerJar=/path/to/forge-1.20.6-50.2.0-installer.jar
```

Forge 50 uses the ordinary native jar for both play and development. The former
`*DeobfJar` path flags remain aliases for the same bytes. OreSpawn and the
Mavenizer fixture default to the sibling `MinecraftMineralogy/OS1206/OreSpawn`
checkout. Use `-PoreSpawnRuntimeJar` and `-Pforge50MavenizerJar` to override paths.
For the prepared cache, use `--offline` with `ORESPAWN_MAVENIZER_OFFLINE=true`.

```text
./gradlew check build javadoc verifyLocalRuntimeInputs writeReleaseChecksums verifyEclipseProductionClasspath --no-build-cache
```

Use `gradlew.bat` on Windows. Output jars go to `build/libs`. Dependencies
remain separate mods. The inherited Forge 47 hosted and ECJ scripts are
historical; they do not qualify this local Forge 50 port.

## Verification

Run the packaged smoke script only with the exact pinned jars, a matching Forge
server seed and an already accepted EULA. It stages launcher files only and
creates a fresh loopback-only world under `build/port-evidence`. For an upgrade,
use a separate disposable copy of a world. Neither a build nor this smoke
substitutes for manual gameplay or client verification.
