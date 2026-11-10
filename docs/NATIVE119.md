# baseElectric 1.19.4 local port

Branch: `feature/baseelectric-1.19.4-port`. The isolated, no-hardlinks
checkout starts at accepted 1.18.2 Electric commit
`4914e5c8a0d8d878f0938b3a9dc59d9f35f7c85e`. No Git remote is configured.

## Target and inputs

- [Forge 1.19.4-45.4.0](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.19.4.html)
  and [ForgeGradle 7](https://github.com/MinecraftForge/ForgeGradle/blob/FG_7.0/docs/Overview.md).
  Local Base Metals and OreSpawn establish ForgeGradle `7.0.34`, Gradle
  `9.6.1`, Renamer `1.1.5`, official 1.19.4 and MCP `20230314.122934`.
  Java 17 runs the game, compiles the mod, and runs Gradle; Java 25 is
  Mavenizer-only and Java 8 is for the Slime Launcher generation tool.
- Use the checksum-bound Mavenizer compatibility JAR from
  `D:/MinecraftMineralogy/OS119/OreSpawn/ci-fixtures/tools/`. It fixes the
  eight duplicate record accessors on this exact target and is never shipped.
  The local Forge installer SHA-1 matches the official download listing.
- Base Metals `3.1.2.119041` is the owner-approved migration-hook build
  from `a9fdd6842f2a2e1e2e5b9a0abf9178d5675df044`. Development and player JARs
  are under `D:/PowerAdvantage/_verification/base-119-port-20261109/Base Metals hook build/build/`
  in `libs-dev` and `libs` respectively. The original Base Metals 1.19
  checkout supplied the read-only Forge reference setup.
- OreSpawn `4.1.0.119041` JARs are in
  `D:/MinecraftMineralogy/OS119/OreSpawn/build/libs-dev` and `build/libs`.
- The local basePower `2.4.11.119041` audit uses owner-verified checkpoint
  `c9995315e2aee9d9bffec8eb8b1191ad8a0faeb7`. The development JAR, player
  JAR, unit-agent JAR and the agent's source are hash-checked and snapshotted in
  `build/port-evidence/power-checkpoint-c9995315/inputs` before building.
  The bootstrap source also matched the checkpoint's Git blob. Electric uses
  the canonical Forge 45 unit agent without an extra event transformer.
  This checkpoint includes Power's modern-upgrade repair. Electric's combined
  suite and reconciliation against merged source remain separate.

The exact local dependency digests are in [gradle.properties](../gradle.properties).
Do not substitute 1.18 artifacts. No sibling build, shared Gradle write, or
world migration is part of Electric's local build.

## Port changes

Forge 45 registration retains all registry paths and legacy aliases. Visible
items call basePower's shared `ItemGroups.show` at registration; the pump
pipe and evil turret remain hidden. Machine recipes use the level's native
registry access. Base Metals crushing stays optional with the vanilla fallback
unchanged. Machine and generator sound keep the original recipient cube,
without chunk queries, using Forge 45's native sound packet. Laser and
propeller use native damage types with the same message IDs. The renderer
uses Minecraft's JOML-backed transforms; the existing shader and textures
remain unchanged. BasePower supplies the shared legacy atlas folder support.

## Evidence

Keep each run in a fresh directory under `build/port-evidence`. These
receipts predate the pinned checkpoint:

| Receipt | Scope |
| --- | --- |
| `provisional-power-compile-b`, `provisional-test-compile-b` | Java 17 compiled 86 production and 28 test sources against the compile-only Power input. The source/classpath hashes and compiler output are retained. |
| `gradle-preflight-e` | Isolated warm-cache Gradle passed production compilation, resource processing, target, repository, recipe-guard and Base Metals/OreSpawn input audits. |
| `gradle-provisional-test-compile-a` | Gradle compiled test sources; did not execute tests. |
| `junit-no-power-a` | Eight loot, pack metadata, shader resource, recipe and historical workflow assertions passed with no Power JAR on the classpath. |
| `static-119-c.json` | Fourteen preservation contracts and changed-path hashes against accepted 1.18.2. The separate unlock generator checked all 50 recipe unlocks. |

Initial compile and cache failures remain under distinct paths. The successful
offline compile used a copied Gradle user home and Mavenizer cache, not a cold
Minecraft preparation. No game launched in this preflight.

### Superseded local checkpoint 1cf39fd

Evidence root: `build/port-evidence/power-checkpoint-1cf39fd`.

- `test-a` preserves the first 96-test run: four entity tests lacked Forge's
  fluid-type registry and five pickup tests lacked initialized fluid state
  caches. Only the worldless fixtures changed; no assertions were skipped.
- `lifecycle-b` passed all 96 tests in 27 suites, target and dependency
  audits, release archives and checksums, and the generated Java 17 Eclipse
  production classpath and launches.
- `ecj-a` independently compiled 86 main and 28 test sources and passed all
  96 tests with the pinned agent and no skips, ignores or assumptions. The
  restricted-JDK negative control failed as intended.
- `artifacts` contains the audited player, development, sources and Javadoc
  JARs. `archives.json` records all four hashes; the player SHA-256 is
  `11F26C99A8AA0EFF8ACAB33019EB31DAD7869A17063C302E475B46217FEA6E01`.
- `smoke-with-metals-a` preserves the first packaged failure: the live item
  selector found no entity after loot spawned inside the ore block.
  `smoke-with-metals-b` and `smoke-without-metals-a` passed drill NBT,
  live sulfur loot, damage-type registration, save and shutdown checks.

The independent Gradle/ECJ, package and dedicated-server checks must use exact
checkpoint pins. The smoke runner supports separate runs with Base Metals and
without it; both keep required OreSpawn. A successful smoke is not
combined-suite, client/GPU, original-world migration or manual acceptance.
