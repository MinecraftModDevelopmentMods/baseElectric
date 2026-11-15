# baseElectric

Electric machines, generation, storage, automation and defence for basePower.
Targets Minecraft **1.21.1**, NeoForge **21.1.265** and Java **21**.
The build uses ModDevGradle **2.0.148** and Gradle **9.8.0**. Version:
**2.2.20.121012**, with coordinated basePower **2.4.19.121012**.

Registry names, migration aliases, machine inventory/NBT keys and gameplay
quantities are retained. The laser drill
still mines normal block drops. The crusher uses basePower's shared MMDLib
crushing lookup, including datapack recipe removal, without hard-coded fallback
outputs. The eight Electric crushing recipes now use `mmdlib:crushing` and `c`
tags. Machinery is not replaced with MMDLib machinery.

## Local Build

Use Java 21 and a separate Gradle user home. basePower **2.4.19.121012** is
published. The native dependency verifier downloads and checks its exact Maven
jar; select it with `-PpowerAdvantageJar=<verified-jar>`. Existing local workspaces
default to `../../PowerAdvantage/build/libs/basePower-2.4.19.121012.jar` and must
match the same published checksum. Published MMDLib **2.0.2.121012+Neo**,
Base Metals **4.0.1.121012+Neo** and OreSpawn **4.1.1.121012** resolve from
MMD Maven. Base Metals uses
`zone.moddev.mc.basemetals:BaseMetals:4.0.1.121012+Neo` by default;
the private development hook jar is no longer used.
`-PoreSpawnJar=<jar>` permits an explicit local OreSpawn input.
`-PbaseMetalsJar=<jar>` permits an explicit copy of the published Base Metals
jar, retaining its Maven filename.
`verifyReleaseDependencies` checks the exact Power, Base Metals, MMDLib and
OreSpawn hashes. Overrides must match those same pins.

```powershell
$env:JAVA_HOME='C:/Users/johnb/.jdks/temurin-21.0.7+6'
$env:GRADLE_USER_HOME='D:/PowerAdvantage/.gradle-electric1211neo'
./gradlew.bat check build verifyReleaseArtifacts writeReleaseChecksums
node tools/verify-native-port.js
node tools/generate-recipe-unlocks.js --check
```

Jars and SHA-256 checksums are written to `build/libs`. Dependencies remain
separate mods. `nativeResourceChecks` runs resource contracts independently of
Power source compilation, but still needs the pinned runtime dependencies.
Source-archive text is normalized to LF. Dispatcher checksums are also written
to `build/release/SHA256SUMS`.

## Eclipse And Runtime Checks

Import as an existing Gradle project using Java 21 and the Gradle wrapper.
Run `./gradlew.bat verifyEclipseProductionClasspath verifyEclipseEcj` after import.
The second task checks the generated project with Eclipse's own compiler.
Eclipse uses
the expanded NeoForge resources and Java 21. Launch `runClient` or `runServer`
from the Gradle Tasks view; old Forge `.launch` files are not applicable. IDE
and generated output remain ignored.

For all three mods together, use Power's `runClientAllAdvantage` task. The sibling
Power checkout's `docs/1.21.1-neoforge/TEST-RESULTS.md` records the coordinated
candidate and manual checks.

`runGameTestServer` uses test-only sources and a disposable directory selected by
`-PelectricGameTestRunDirectory=build/port-evidence/<new-run>`. Production jars
exclude the tests. Pass an existing accepted EULA using
`-PelectricGameTestEula=<path>`. Do not accept an EULA on somebody else's behalf or point a
test run at an existing world. Client GUI/rendering, manual gameplay and historic
world migration require separate acceptance on disposable copies. See
[NeoForge port notes](docs/NEOFORGE1211.md).

## Release Status

The NeoForge port passed manual testing. Qualification against the released
Power and dependency jars includes 110 native tests, 25 resource checks,
seven GameTests in each recipe mode, ECJ compilation and release audits.

The combined rig checks connected machine operation, legacy-world upgrades,
recipes, real chunk unloading and full restarts. Fresh and upgraded worlds
also completed 30-minute endurance runs and independent checkpoint replay.
The port notes distinguish these packaged checks from the retained
development-client and manual evidence.

Windows and hosted Linux builds produce the same player, sources and Javadoc
archives. The first hosted audit caught differing recipe line endings; text
resources now use UTF-8 and LF without changing any recipe values or class
files. See the [port notes](docs/NEOFORGE1211.md) for the exact final hashes.

Native CI and CodeQL use the same four verified dependency jars. Their checked-in
dependency lock is `HOSTED_INPUTS_READY`; the guard still requires exact public
bytes, merged source ancestry and Power's matching release tag. Hosted CI runs
unit/resource, build, Javadoc, package and
Eclipse checks; GameTests and ECJ remain local. Tag validation does not publish.
