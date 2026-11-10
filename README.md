# baseElectric

baseElectric continues DrCyano's Electric Advantage. It adds electric machines,
generation, storage, automation and defence to basePower. Its equipment appears
in the shared basePower creative tab.

This version targets Minecraft **1.20.6**, Forge **50.2.0**, official mappings,
ForgeGradle **7.0.34**, and Java **21**. The version is **2.2.14.120061**.
Install [basePower 2.4.13.120061](https://www.curseforge.com/minecraft/mc-mods/basepower/files/9545972)
for Minecraft 1.20.6. CurseForge also installs
[Base Metals](https://www.curseforge.com/minecraft/mc-mods/base-metals); this
build was tested with 3.1.2.120061 and OreSpawn 4.1.0.120061.
See the [1.20.6 notes](docs/NATIVE1206.md) for save compatibility and test coverage.

Back up your world and try the upgrade on a copy. Replace Electric Advantage
and Power Advantage with baseElectric and basePower; do not install both sets.

## Building locally

Use Temurin 21.0.7+6 for Gradle and Minecraft. The pinned OreSpawn Mavenizer
fixture uses Java 25, and ForgeGradle's Slime Launcher tooling uses Java 8. Set
`-Porg.gradle.java.installations.paths=<java21-home>,<java25-home>,<java8-home>`
if Gradle cannot discover all three.

The independent Forge/resource checks do not require sibling jars:

```text
./gradlew --offline nativeResourceChecks verifyRecipeTagGuards processResources
node tools/verify-native-port.js
node tools/generate-recipe-unlocks.js --check
```

For the full local build, supply the files matching the pins in `gradle.properties`:

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
remain separate mods. The 1.20.6 CI jobs check against the pinned Forge 50
runtime and public dependencies.

## Verification

Run `runGameTestServer` with the same pinned mod inputs, an existing accepted
EULA passed as `-PelectricGameTestEula=<path>`, and a new directory passed as
`-PelectricGameTestRunDirectory=build/port-evidence/<run>`. The test code and
its structure are not in the production jar.

Run the packaged smoke script only with the exact pinned jars, a matching Forge
server seed and an already accepted EULA. It stages launcher files only and
creates a fresh loopback-only world under `build/port-evidence`. For an upgrade,
use a separate disposable copy of a world. Neither a build nor this smoke
substitutes for manual gameplay or client verification.
