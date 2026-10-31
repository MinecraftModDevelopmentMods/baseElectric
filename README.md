# Electric Advantage

Electric machines, generation, storage, automation and defence for Power Advantage.

This is the independent **Minecraft 1.17.1 port candidate, 2.2.9.117011**.
It is local work, not a published release. Manual acceptance is pending.
No port-code push, pull request, merge, tag or release is approved before manual testing.

## Target

- Forge `37.1.1`, official `1.17.1` mappings, MCP `20210706.113038`.
- ForgeGradle `7.0.34`, Renamer `1.1.5`, Gradle `9.6.1`.
- Java 17 runs Gradle; Java 25 runs Mavenizer. Production and tests use
  Java `16.0.2+7`, class major `60`. Java 8 is only for old dependency build utilities.
- Production artifact: `build/libs/ElectricAdvantage-2.2.9.117011.jar`.
  The separate deobfuscated API artifact stays under `build/libs-dev`.

Power Advantage is required. Base Metals remains optional in Forge metadata;
the existing required CurseForge installation relationship and ordering are unchanged.
Power, Base Metals and OreSpawn are separate dependencies, never embedded.

## Current State

The source and resource migration is prepared. The source inventory is 85 production
files and 27 test files containing 95 tests: all 89 inherited tests plus six native
construction, shader and dependency-recipe checks.

On 31 October 2026 the complete local deterministic build passed all 95 JUnit
tests, Javadocs, dependency/resource/artifact audits, checksums and Eclipse
production isolation checks. Independent ECJ compilation and all 95 tests also
passed on Java 16, with no ignored tests or assumptions; its restricted-JDK
negative control failed for the expected access error.

These results use the parent's local checkpoint, not a final combined release:
Power Advantage `2.4.8.117011`, source `fc4c47f2ed8cfebbaa010e6a0e807137a3329f95`,
API SHA-256 `BDF486E16C1FE3B4CE2FE1ED78670AABA594063BCD2D4251265124B17FA9E767`.
The metadata records `LOCAL_CHECKPOINT`; any coordinated re-pin needs fresh proof.
Earlier `API_PIN_PROVISIONAL` results are retained separately. A 1.16.5 Power jar
is never accepted. Combined packaged runtime and migration are verified
separately; manual gameplay acceptance remains required before remote delivery.
Prepared CI has not been run on a hosted service.

See [the native port notes](docs/NATIVE117.md) for the API audit and remaining gates.
The retained [1.16 notes](docs/NATIVE116.md) describe the previous target, not proof
that this one passes.

## Dependencies

Base Metals `3.1.0.117011` is built for the development API from public source
`dacfaa3af2e3310d5cb6b081da79f09260ae103d`.
OreSpawn `4.1.0.117011` uses public source
`b56e2b62a1a6ea4d1dd96f8646a87801a817dbdf`.
Exact API and public-runtime SHA-256 values are recorded separately in
`gradle.properties`. Published runtime jars, not unpublished dependency builds,
must be used for packaged qualification.

For this local coordinated campaign, every Gradle invocation must go through
`D:/PowerAdvantage/_verification/advantage-117-port-20261031/Invoke-Gradle117.ps1`.
It holds the exclusive `gradle-shared.lease` and uses
`D:/GradleCaches/advantage-117-20261031`. Do not bypass the helper, delete caches,
overwrite evidence, or build against another module's mutable output.

The required local deterministic tasks are:

```text
clean check build javadoc verifyReleaseDependencies verifyReleaseArtifacts writeReleaseChecksums verifyEclipseProductionClasspath
```

Pass the exact immutable paths with `-PpowerAdvantageDeobfJar`,
`-PbaseMetalsDeobfJar` and `-PoreSpawnDeobfJar` through the helper's `Extra` argument.
Tests use the parent's hash-checked, build-only Forge capability bootstrap source
through `-PpowerAdvantageUnitBootstrapSource`. Electric's build-only wrapper also
runs Forge's event-bus plugin for the native tier/harvest checks. Neither replaces
capability tokens or events, and neither is included in production jars.
Independent ECJ verification uses `tools/verification/Verify-ElectricEcj.ps1`
after Gradle has produced a fresh classpath, unit bootstrap jar and complete test inventory.

Static checks need no Gradle or game launch:

```text
node tools/verify-native-port.js --report build/native117-static-unique.json
node tools/generate-recipe-unlocks.js --check
```

Use a new report name on each run. The static comparison is against the merged
1.16.5 baseline `c5c8c9ea1d6f2010b8cff61e4bfae86f4c96c978`. It does not prove
compilation, native ticking, shader compilation or migration safety.

## Compatibility

The implementation namespace remains `zone.moddev.mc.electricadvantage` and the
legacy `cyano.poweradvantage.api` integration is retained. Mod ID, asset paths,
registries, aliases, entity IDs, NBT keys and OreSpawn provider IDs stay stable.
Native constructors carry each machine's real position and block state, including
moving drills. Numeric machine constants and existing recipes are unchanged.

All 46 crafting variants and four smelting recipes retain their recipe-book
unlocks. The eight Base Metals crushing recipes remain non-crafting entries.
The crusher continues to consume the real Base Metals recipe implementation;
no replacement raw-metal conversion or balance change is introduced.

Eclipse imports use the Gradle wrapper, processed resources and production-only
launches. Generate metadata through `verifyEclipseProductionClasspath`; do not
hand-edit generated classpaths or disable restricted-reference tests.

Test only disposable copies of worlds. Preserve originals, backups, failed
receipts and version-specific checkouts. Manual GUI interaction and captions,
hydroturbine animation, sound, multiplayer, weapons and player/entity behavior
remain separate acceptance gates.
