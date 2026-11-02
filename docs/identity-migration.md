# baseElectric 1.10 Identity Migration

## Scope

This local candidate starts at ElectricAdvantage baseline
`c0f8424665f3810eb2d6d1a06b2cba85c5322e9a`. Its target is baseElectric
`2.2.6.110021` on Minecraft 1.10.2 / Forge 12.18.3.2511.

Only the original released Cyano Electric Advantage 1.10 saved identities are a required
migration input. Java implementation packages from unpublished intermediate candidates do
not become extra saved-world aliases. Historical fixtures must retain their original bytes.
The unchanged `cyano.poweradvantage.api` is a public API exception, not stale implementation.

## Saved Objects

- Blocks and items, including ItemBlocks: `electricadvantage:<path>` to `baseelectric:<path>`.
- The missing-mappings event delegates to basePower's `LegacyIdentity`, which reads
  `event.getAll()`. Filtering only the active new mod container misses the old owner's entries.
- All 16 registered tiles retain `electricadvantage.<derived_name>` as read alternatives.
  `GameRegistry.registerTileEntityWithAlternatives` receives `baseelectric.<derived_name>` as
  its primary name, so native saves write the new identity. LED's historical suffix is `l_e_d`.
- The released hydroturbine ID is `electricadvantage.electricadvantage.Hydroturbine`:
  the old code prefixed its registration name, and Forge prefixed it again. Its read-only
  `EntityList.NAME_TO_CLASS` alias must not replace `CLASS_TO_NAME`'s canonical
  `baseelectric.Hydroturbine` value or alter the local entity index/tracking settings.
- Also accept `electricadvantage.Hydroturbine` as a defensive read alias. This shorter spelling
  has not been verified as an original-release saved ID. Both read aliases resolve to the
  same class; neither creates another canonical registration. Validate both names for
  ownership conflicts before installing either alias.
- Unknown old registry entries fail migration rather than silently becoming air.

Inventory, energy, machine progress, coordinates, orientation, UUIDs, and generator links
remain under the existing serialization contracts. The rename does not alter their values.

The inspected original fixture is `ElectricAdvantage_1.10.2-2.2.0.jar`, SHA-256
`61AE5343A79F518F54F1717D508EA3F6DA7209EE69B0C4433E0B7A9722213EE0`, from the harness's
protected `20261023-working-legacy110/original110/mods` input. `javap -p -c` of
`cyano.electricadvantage.init.Entities` shows the `electricadvantage.` prefix and the legacy
`EntityRegistry.registerModEntity(Class,String,int,Object,int,int,boolean)` call. Forge then
adds the active mod ID to the saved name. The similarly named local jar with SHA-256
`29A6EFA25B9519B32A2FCA853F3D8CC89E18ADBFD44A7BC4C4D0EC5F4C6A551B` contains `com.mcmoddev`
classes and is not interchangeable original Cyano release evidence. No fixture was changed
or loaded in a game during this inspection.

## Configuration And Loot

`LegacyIdentity.migrateConfiguration` runs before Forge Configuration loads. It copies
`electricadvantage.cfg` to `baseelectric.cfg` only if the new file does not exist, without
changing the old bytes. A new file is always authoritative.

`LegacyIdentity.migrateAdditionalLootTables` runs before generating default tables. It
imports old tables with structured owned registry-value rewrites, preserves existing new
tables, and archives consumed files as `.json.before-baseelectric`. The loot loader scans
`.json`; retaining both active copies would inject loot twice. Default generation skips
existing files, preserving imported or new custom tables. Foreign registry values and
weights/counts remain unchanged.

## OreSpawn Coordination

Worldgen migration uses basePower's public `compat.LegacyWorldgenIdentity` helper against
pinned OreSpawn 4.0.8.110021. Provider owners and rule IDs must use `baseelectric`, not
`electricadvantage`: the pinned engine rejects unloaded owners and rules outside their
provider's namespace. Owned block outputs also become `baseelectric:*`.

basePower's `required-before:orespawn` ordering makes its preInit the early migration hook.
It calls `migrateConfig(configDirectory, oldId, newId)` for both Power and Electric before
OreSpawn preInit reads the global profile. Electric also calls it defensively before
enqueueing its typed provider, but this later, idempotent check does not replace the early
Power hook. Any `IOException` aborts startup; it must never fall through to fresh defaults.

basePower also calls `migrateWorld(selectedWorldRoot, oldId, newId)` for both owners before
OreSpawn's serverAboutToStart reads that world's saved profile. Electric adds no independent
world-file rewrite or world scan. Historical fixtures and protected source worlds remain
unchanged; qualification uses disposable copies.

The original OS1 file's settings remain authoritative through migration, not its old active
filename. The helper writes the migrated owner file and retires consumed old JSON only after
creating and verifying a hash-named `.bak` containing its original bytes. An explicit modern
provider overrides the typed defaults, and consumed OS1 input is retired so the bridge cannot
import it again as a standalone `orespawn` provider. Conflicting old/new authorities, an
already-completed bridge marker without its owned provider, and ambiguous legacy/modern rule
overlaps abort for explicit reconciliation. Saved manifests are migrated alongside their
rules so API defaults are not added beside retained imported rules.

Sulfur/lithium ownership priority, frequency, quantities, height limits, hosts, overrides,
and disabled retrogen must remain unchanged. Fresh and legacy-config starts require distinct
checks; parsing a fixture alone is not evidence that only one provider is active.

## Build And Publication Gates

Implementation references use `zone.moddev.mc.basepower`; public API references remain
`cyano.poweradvantage.api`. The required runtime constraint is
`required-after:basepower@[2.4.4.110021,);after:basemetals`, alongside the existing OreSpawn
requirement. Distribution relations are `basepower,base-metals,mmd-orespawn`.

The build pins basePower source commit `425af862ef5b41b61d3e1f7e665d1d691852ff4b` and development-jar
SHA-256 `2992F2CADC30D8F61A17216DF9AB02BDF8E8211BB8B494A6CCED70A3267F69FE` in properties,
the strict dependency audit and both dependency-building workflows. CI builds that exact source
before checking the jar checksum. Required class, manifest, classpath, Java 8 bytecode,
reproducibility and no-bundling audits remain.
basePower and OreSpawn classes must not enter the distributable baseElectric jar.

CurseForge ID `0` is local development metadata only. Publication and release-tag guards
refuse both `0` and old project `240985`; a newly assigned, authorized project is required.
Source delivery and publication are separate; normal CI never publishes a release.

## Verification Boundaries

New tests cover the actual missing-mapping handler, all tile read/write registrations,
native inventory/energy/position reload, entity read-only aliases, CFG precedence, and
custom-loot preservation. Worldgen tests retain the immutable original OS1 input, verify
new provider/rule/block identities, compare all imported placement settings, check byte-exact
backups and idempotence, and reject conflicting authorities or skipped-import states.
Existing operational regressions retain their assertions.

The [cross-mod qualification notes](https://github.com/MinecraftModDevelopmentMods/basePower/blob/master-1.10.2/docs/1.10/base-qualification.md)
record the exact-artifact original-world, operating and restart checks. They keep manual client
acceptance separate and do not claim every Electric machine has a 30-minute endurance run.

## Building And Reproducing The Jars

Build the pinned basePower and OreSpawn development artifacts first, then use the dependency
properties described in the README. Run the full checks without bypassing either checksum:

```text
gradlew.bat clean check build javadoc verifyReleaseDependencies verifyReleaseArtifacts writeReleaseChecksums --no-daemon --stacktrace
```

For reproducibility, run that command from independent source checkouts with the same pinned
dependencies and toolchains. Compare all three distributable jars and the development jar,
retaining each run's logs and checksums outside Git. Do not overwrite a frozen test receipt or
launch an original source world. Publication remains disabled with CurseForge project ID `0`.
