# baseElectric

baseElectric continues DrCyano's Electric Advantage, adding generators, batteries, wiring and electrically powered machines to basePower for Minecraft 1.10.2.

Version `2.2.6.110021` uses mod ID `baseelectric`, artifact `baseElectric`, and implementation
namespace `zone.moddev.mc.baseelectric`. It requires basePower `2.4.4.110021` or newer; update
the two mods together. The public `cyano.poweradvantage.api` remains unchanged.

This version is not published. The [cross-mod qualification notes](https://github.com/MinecraftModDevelopmentMods/basePower/blob/master-1.10.2/docs/1.10/base-qualification.md)
record the exact jars used for local unit tests, original-release migration fixtures, operating
cases and full restarts. Not every machine has a 30-minute endurance run, and manual gameplay
acceptance remains separate from those automated checks.

The cross-mod [Advantage Works 1.10.2 Handbook](https://github.com/MinecraftModDevelopmentMods/basePower/blob/master-1.10.2/docs/1.10/README.md) includes the Electrician's Handbook, rated figures, commissioning layouts, and persistence tests. The [MMD material and fluid compatibility specification](https://github.com/MinecraftModDevelopmentMods/basePower/blob/master-1.10.2/docs/1.10/mmd-material-fluid-compatibility.md) defines sulfur/lithium ownership, migration, and Ore Dictionary aliases.

## Released-World Migration

The supported input is an original released Cyano Electric Advantage 1.10 world, not every
unpublished implementation identity. Back up the world and configuration before upgrading;
qualify migration on disposable copies. See [the migration contract](docs/identity-migration.md).

Released `electricadvantage` blocks and items load as `baseelectric`. Original tile and
hydroturbine IDs remain read aliases; subsequent saves use the new canonical IDs.
An existing `baseelectric.cfg` wins; otherwise `electricadvantage.cfg` is copied unchanged.
Custom additional-loot tables are imported with owned item IDs updated, then the old JSON
files are archived outside the loader's `.json` suffix so they cannot inject duplicate loot.
Existing new loot tables are never overwritten by generated defaults.

basePower migrates both mods' OreSpawn configuration and selected-world profiles before
OreSpawn reads them. Provider, rule and owned block IDs become `baseelectric`; old active
provider IDs cannot be retained with the pinned engine. Original config bytes are backed up,
and consumed legacy input is retired to prevent duplicate generation. Ambiguous or failed
migrations stop startup instead of silently using fresh defaults.

No recipes, machine rates, energy capacities, crop behavior, or ore placement settings are
changed by this identity migration. Existing bug fixes and regression tests from baseline
`c0f8424665f3810eb2d6d1a06b2cba85c5322e9a` are retained.

## Drill Reload Fix

Version `2.2.5.110021` keeps unfinished drill work when a chunk or world reloads.
Existing inventories, energy, orientation and saved-world names are unchanged.
The regression tests cover resumed progress and full server restarts.

## Building Minecraft 1.10.2

The `master-1.10.2` build uses ForgeGradle 7.0.34 and Gradle 9.6.1. Run Gradle with Java 17; Gradle resolves the Java 8 toolchain used for compilation.

Build the pinned OreSpawn commit `5a50df1158e948c8db55814e819090b34c12d765` first. Then build the pinned sibling basePower development jar:

```text
cd ..\..\PowerAdvantage
gradlew.bat deobfJar
```

The dependency locations can be overridden with `-PoreSpawnDeobfJar=<path>` and `-PpowerAdvantageDeobfJar=<path>`.

Then build and audit baseElectric:

```text
gradlew.bat clean check build javadoc verifyReleaseDependencies verifyReleaseArtifacts writeReleaseChecksums
```

Release jars are written to `build/libs`. The deobfuscated development jar is written to `build/libs-dev` by `deobfJar` or `build`.

For Eclipse, import this repository as an existing Gradle project and run:

```text
gradlew.bat cleanEclipse verifyEclipseProductionClasspath
```

basePower `2.4.4.110021` or newer and OreSpawn are required at compile time and runtime but are never bundled. Base Metals remains a required distribution dependency while its integrations stay reflection-based and absent from the compile classpath. CurseForge relations are `basepower`, `base-metals`, and `mmd-orespawn`.

The build pins basePower source commit `425af862ef5b41b61d3e1f7e665d1d691852ff4b` and development-jar SHA-256
`2992F2CADC30D8F61A17216DF9AB02BDF8E8211BB8B494A6CCED70A3267F69FE` in properties, audits and
both dependency-building workflows. CI builds that exact MMD basePower source and checks the
resulting jar before compiling baseElectric. Do not substitute an old jar or a moving branch.

CurseForge project ID `0` means unassigned development metadata. No CurseForge project has
been created or authorized. Maven publication, prepared release validation, and release-tag
validation refuse unassigned IDs and the old Electric Advantage project ID. Local builds
and checks are separate from publication approval.
