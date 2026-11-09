# baseElectric 1.18.2 port notes

The release branch uses the shared Base Metals 3.1.2 migration hook and the
published basePower 2.4.10.118021 build.
See [gradle.properties](../gradle.properties) for the exact Power, Base Metals
and OreSpawn pins.

The first locally tested Electric player jar had SHA-256
`C5C1A6DEAE795F8AF741DBE35B0D8941F7FA4BBC708758BDFE056B4AEBADCBC1`.
The notes below record earlier port milestones and their original inputs.

`feature/baseelectric-1.18.2-port` was cloned from the clean PA117
`feature/baseelectric-1.17.1-identity-migration` tip
`cf3d4b0e2d1591b2ae8e5d638002ca016e46eac4`.
The candidate is `2.2.11.118021`, for Forge `40.3.0`, official `1.18.2`,
MCP `20220404.173914`, Java 17 and ForgeGradle `7.0.34`.

## Native changes

- Blocks, items and block items construct only in their Forge registration
  events. The sulfur drop stack resolves after item registration; the
  original ore constructor, 1-4 sulfur range and silk touch behavior remain.
  GUI definitions register in enqueued common setup after registries.
- Machines use `saveAdditional` and `saveWithoutMetadata` for native saving,
  synchronization and drill transfer. Existing keys, energy, inventory and
  progress remain. The turret has separate render-only updates and full
  chunk data. Hydroturbine spawn uses Forge's moved network hook.
- Native tags replace removed item tag lookups. Recipe mode conditions work
  with the Forge context and retain the legacy caller. Electric's unshaded
  vertex shader uses the native 1.18 fog function and still omits diffuse light.
- All IDs, recipe JSON, unlocks, model and texture assets, legacy aliases,
  creative tab and machine balance values are retained. Pack metadata is
  Forge resource format 8 and data format 9.

## Exact local inputs

Versions, distinct development/player SHA-256 values, and source checkout
revisions are in [gradle.properties](../gradle.properties). The Base Metals
and OreSpawn source revisions describe the checked local checkouts, while the
verified hashes identify the bytes actually used. The local release inputs are:

- Base Metals: `D:/BaseMetals/BaseMetals 118/BaseMetals/build/libs-dev/BaseMetals-3.1.1.118021-deobf.jar` and `D:/BaseMetals/BaseMetals 118/BaseMetals/build/libs/BaseMetals-3.1.1.118021.jar`.
- OreSpawn: `D:/MinecraftMineralogy/OS118/OreSpawn/build/libs-dev/OreSpawn-4.1.0.118021-deobf.jar` and `D:/MinecraftMineralogy/OS118/OreSpawn/build/libs/OreSpawn-4.1.0.118021.jar`.
- basePower: `D:/PowerAdvantage/_verification/base-118-port-20261109/power-source-pin-a/basePower-2.4.10.118021-deobf.jar` and `D:/PowerAdvantage/_verification/base-118-port-20261109/power-source-pin-a/basePower-2.4.10.118021.jar`.
- Bootstrap: `D:/PowerAdvantage/PA118/PowerAdvantage/test-rig/unit-bootstrap/ForgeCapabilityBootstrap.java`. Its Git blob matches the pinned source revision.
- Forge installer: `D:/BaseMetals/.runtime-cache/forge-1.18.2-40.3.0-installer.jar`.
- Mavenizer: `D:/IronAgeFurniture/IAF118/IronAgeFurniture/ci-fixtures/tools/minecraft-mavenizer-0.5.21-orespawn-1.18.jar`.

The basePower pin is source commit `8bd3b8213554551fc688a46eb1653a0b572b8d0a`,
deobf SHA-256 `CDDB8E1BB6998F625BEEF677B4E2FC87847A450D77775DA86CF31566A926D28F`,
and player SHA-256 `3C3E9154BC0F7CC88C4AE4C5F0044527D7F39BBF12C71F83AD99CF2E9646E273`.
Both Electric manifests record these pins. The ForgeGradle audit checked the
resolved official MCP mapping ZIP, exact mod files on both classpaths, metadata,
and the exclusion of RF/RebornCore. The separate player-input audit checked all
three player mod JARs and the Forge installer.

## Source-pinned handoff

Frozen outputs are in `build/port-evidence/source-pin-a/artifacts`.

| Output | SHA-256 |
| --- | --- |
| `baseElectric-2.2.11.118021.jar` | `1094AE5A8AF02148C90111E8CAB46380DD111DF5D9387B2A966FB2F939F5AE35` |
| `baseElectric-2.2.11.118021-deobf.jar` | `AE13D69B8A8B06A8838CEFB4E725A5BCACEB79141A2E875080132C179AAC3B7D` |
| `baseElectric-2.2.11.118021-sources.jar` | `0BFE22BF6753CADB17812732EFFCF070403A95921056A12F6EC11A8E44B6AAC6` |
| `baseElectric-2.2.11.118021-javadoc.jar` | `860B84F5B49DD49C865088D1B6A056B7C06C5B504C9AE59943AEB8B9B3E8ECE3` |

Both `gradle-source-pin-a.log` and `gradle-source-pin-b.log` record all 37
tasks executed successfully, including 96 tests with no skips, release
audits and Java 17 Eclipse generation. Both runs used the explicit paths
above with the command in the README, `--offline`, and
`ORESPAWN_MAVENIZER_OFFLINE=true`. The second forced build reproduced all
four hashes (`source-pin-b/rebuild-comparison.json`). This proves repeatability
in the same checkout with the prepared dependency cache, not a cold-cache build.
Both runs retain the JUnit XML and Eclipse launch files separately.

`ecj-source-pin-a/ecj-results.json` records independent Java 17 compilation
and 96 JUnit tests with no failures, ignores or assumptions. Its restricted-JDK
negative control fails as intended. `source-final-pin-a.json` records the
static preservation and pin audit.

`packaged-source-pin-a/receipt.json` records the dedicated-server smoke of
the exact source-pinned player JARs: Forge readiness, a live drill NBT read,
sulfur loot and entity NBT, a flushed save, and shutdown with exit code 0.
The combined harness, client/GPU behavior and existing-world migration are
separate gates. No hosted workflow or publication was run.

## Provisional receipts retained

Local receipts are under `build/port-evidence`. `diagnostic-baseline.log` was a
failure-finding compile against the older Power ABI, not a 1.18.2 result.
`gradle-check-a.log` and its XML retain the first JUnit fixture failures;
`gradle-check-b.log` retains the stale Java 16 Eclipse audit failure.
`gradle-lifecycle-e.log` is the subsequent 36-task forced local build against
the provisional 1.18.2 API, including all 96 tests, package, assets, pins and generated
Java 17 Eclipse launch-file checks.

`ecj-lifecycle-e/ecj-results.json` independently compiles 85 production and
28 test sources on Temurin 17.0.1 and passes all 96 tests, with zero failures,
ignores or assumptions. Its restricted-JDK negative control fails as intended.
`source-lifecycle-e.json` passes all 14 static contracts; the unlock generator
checks all 50 unlocks. A second forced build reproduced all four provisional archives,
including the deobf JAR, byte-for-byte (`rebuild-lifecycle-comparison.json`).
`gradle-negative-power-pin.log` confirms the same-version Power player JAR
is rejected when supplied as the development JAR.

The provisional packaged server smoke passed in `packaged-c/receipt.json`:
Electric player SHA-256
`38A66525DE2F9FA525D9FFEE9AE33BD69F4457B23995A255F2B17B7C9CD9B465`
and the Power player's `smoke-power-c` copy SHA-256
`FE7F0EDD01F16A7FA33DD748E0395FEDE8DEC32FE77E5B27A6F13EED7D712C55`.
It reached Forge readiness, placed a drill and read native Energy, progress,
inventory and registry NBT, placed sulfur ore, confirmed its live one-item
sulfur stack, saved, and exited 0. `packaged-a` retains the initial harness
timeout: the game produced three powder, but the matcher expected generic
item-count text. `packaged-b` passed the assertions but retained the vanilla
flat-world preset parser's registry-ops diagnostic. The final smoke used the
default world preset and an explicitly loaded test chunk, with no error or
fatal messages and no changes to the four mod JARs.

Use `tools/verification/Invoke-ElectricPackagedSmoke.ps1` with the explicitly
selected Power player JAR/hash, a prepared Forge 40.3.0 seed and a fresh evidence
path. It runs only on loopback in a disposable world; the development JARs are
not runtime inputs. No client launch, gameplay acceptance, original-world
migration, hosted workflow, or publication is implied by a local module build
or a dedicated-server smoke.
