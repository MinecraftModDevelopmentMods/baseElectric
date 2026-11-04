# baseElectric for Minecraft 1.12.2

baseElectric continues DrCyano's Electric Advantage, adding generators, batteries, wiring, greenhouses and electric machines to basePower.

## Upgrading a world

Replace the installed Advantage family together. baseElectric `2.2.6.112021` reads the original Electric Advantage and released baseElectric 1.10.2 tile and turbine identities. Back up the complete save and its configuration, then test a copy and check charge, fluid and machine state again after a full restart.

[World upgrade guide](https://github.com/MinecraftModDevelopmentMods/basePower/blob/master-1.12.2/docs/1.12-upgrade.md) | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/baseelectric)

## Building Minecraft 1.12.2

The `master-1.12.2` build uses ForgeGradle 7.0.34 and Gradle 9.6.1. Run Gradle with Java 17; Gradle resolves the Java 8 toolchain used for compilation.

First build the pinned OreSpawn and sibling basePower development jars:

```text
cd ..\..\PowerAdvantage
gradlew.bat deobfJar
```

Back in the baseElectric checkout, build and audit the release:

```text
gradlew.bat clean check build javadoc verifyReleaseDependencies verifyReleaseArtifacts writeReleaseChecksums verifyEclipseProductionClasspath
```

Release jars are written to `build/libs`. The deobfuscated development jar is written to `build/libs-dev` by `deobfJar` or `build`.

For Eclipse, import this repository as an existing Gradle project and run:

```text
gradlew.bat cleanEclipse verifyEclipseProductionClasspath
```

basePower is required at compile time and runtime but is never bundled. For a different checkout layout, supply `-PpowerAdvantageDeobfJar=<path>` and `-PoreSpawnDeobfJar=<path>`. The expected commits and hashes are in `gradle.properties`. Base Metals remains a distribution dependency, but its integrations do not require it at compile time.

## Hydroturbine Persistence

Version `2.2.2.112021` retains the generator coordinate when a loaded turbine is saved before its first update. The existing `parentPos` key, entity identity and generated electricity remain unchanged. This also protects the intermediate 1.12 save during a chained upgrade to 1.13; it cannot recover a coordinate already missing from an old save.

The normal `check` task includes repeated pre-tick save tests, stale-link clearing and live-generator position checks. The Advantage Works migration harness tests the packaged jar through full 1.12 and 1.13 process restarts.
