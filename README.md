# baseElectric

baseElectric continues DrCyano's Electric Advantage. It adds electric machines,
generation, storage, automation and defence to basePower. Its equipment appears
in the shared basePower creative tab.

baseElectric **2.2.16.121111** runs on Minecraft **1.21.11**, Forge **61.1.0**,
official mappings with MCP **20251223.124241**, ForgeGradle **7.0.34**, Gradle
**9.6.1**, and Java **21**. It requires
[basePower **2.4.15.121111**](https://www.curseforge.com/minecraft/mc-mods/basepower/files/9566998).
[Base Metals **3.1.2.121111**](https://www.curseforge.com/minecraft/mc-mods/base-metals/files/9566738)
is also **REQUIRED** on both client and server;
Forge enforces this dependency. The coordinated OreSpawn version is
**4.1.0.121111**. This version has been accepted for release but is not yet published.
See the [1.21.11 notes](docs/NATIVE12111.md) for compatibility and verification status.

Combined machine, upgrade, graphical and two-world 30-minute endurance checks
passed before the released dependency retarget. The full record and its manual-test limits are in
basePower's `docs/1.21.11/QUALIFICATION.md`. Those limits still apply.
Publication is waiting for the final combined restart/endurance checks and
baseSteam's public release. The released dependency pins are recorded in
`gradle.properties`.

Back up your world and try the upgrade on a copy. Replace Electric Advantage
and Power Advantage with baseElectric and basePower; do not install both sets.

## Building locally

Use Temurin 21.0.7+6 for Gradle and Minecraft. The pinned OreSpawn Mavenizer
fixture uses Java 25, and ForgeGradle's Slime Launcher tooling uses Java 8. Set
`-Porg.gradle.java.installations.paths=<java21-home>,<java25-home>,<java8-home>`
if Gradle cannot discover all three.

The independent Forge/resource checks do not require sibling jars:

```text
./gradlew --offline nativeResourceChecks compileNativeApiDiagnostics verifyRecipeTagGuards processResources
node tools/verify-native-port.js
node tools/generate-recipe-unlocks.js --check
```

For a full local build, use the exact dependency jars recorded in
`gradle.properties`. The sibling paths are the defaults and these flags are
optional path overrides:

```text
-PpowerAdvantageRuntimeJar=/path/to/basePower-2.4.15.121111.jar
-PbaseMetalsRuntimeJar=/path/to/BaseMetals-3.1.2.121111.jar
-PforgeInstallerJar=/path/to/forge-1.21.11-61.1.0-installer.jar
```

Forge 61 uses the ordinary native jar for both play and development. The former
`*DeobfJar` path flags remain aliases for the same bytes. OreSpawn and the
Mavenizer fixture default to the sibling `MinecraftMineralogy/OS12111/OreSpawn`
checkout. Use `-PoreSpawnRuntimeJar` and `-Pforge61MavenizerJar` to override paths.
For the prepared cache, use `--offline` with `ORESPAWN_MAVENIZER_OFFLINE=true`.

```text
./gradlew check build javadoc verifyLocalRuntimeInputs writeReleaseChecksums verifyEclipseProductionClasspath --no-build-cache
```

Use `gradlew.bat` on Windows. Output jars go to `build/libs`. Dependencies
remain separate mods. The 1.21.11 CI and CodeQL jobs require merged source pins
and checksum-matching public dependencies before checkout or compilation.
Local jars do not satisfy that gate.

## Verification

The current local checks pass 114 unit tests, 25 native resource tests and
114 tests from fresh Eclipse compiler output, with no failures or skips.

Run `runGameTestServer` with the same pinned mod inputs, an existing accepted
EULA passed as `-PelectricGameTestEula=<path>`, and a new directory passed as
`-PelectricGameTestRunDirectory=build/port-evidence/<run>`. The test code and
its structure are not in the production jar.

Run the packaged smoke script only with the exact pinned jars, a matching Forge
server seed and an already accepted EULA. It stages launcher files only and
creates a fresh loopback-only world under `build/port-evidence`. For an upgrade,
use a separate disposable copy of a world. Neither a build nor this smoke
substitutes for manual gameplay or client verification.
