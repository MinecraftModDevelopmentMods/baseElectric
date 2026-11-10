# Local Forge 1.20.6 port

- Accepted source: `D:/PowerAdvantage/PA1201/ElectricAdvantage/ElectricAdvantage`,
  `28dd438dde6898fcb6d6e1d17a62a4c0f527169e`.
- Local checkout: `D:/PowerAdvantage/PA1206/ElectricAdvantage/ElectricAdvantage`.
  The local `master-1.20.6` points to the accepted source; the port is on
  `feature/baseelectric-1.20.6-port`. The source had no remotes.
- Target: `2.2.14.120061`, Forge `50.2.0`, Temurin `21.0.7+6`, official
  mappings, MCP `20240429.135109`. Forge 50 uses the ordinary jar for both
  development and the packaged game.

## Compatibility

- Batteries read legacy `energy` from the `minecraft:custom_data` component
  populated by vanilla's item upgrade. Changing charge preserves unrelated
  data. A missing charge still means full, and a stored zero still means empty.
- Drill track moves use registry-aware block entity transfer, preserving
  inventory stack components and world state. Legacy nested inventory items
  are upgraded by basePower's shared reader. Drill target coordinates, target
  snapshots and progress keep their accepted NBT keys.
- Block-entity updates use native registry-aware packets, and the hydroturbine
  uses vanilla entity spawning. No registry IDs or alias mappings changed.
- Common machine definitions remain available on dedicated servers; render
  overrides and helpers with client signatures are stripped by Forge. The
  unshaded client shader uses the native 1.20.6 fog function.
- Recipe stacks use `id` and Forge conditions are registered as codecs. Recipe
  availability is conditional; generated advancements use native item
  predicates but stay loaded across recipe-mode or provider changes. Ordinary
  block loot table data is unchanged.
- Electric uses Power's `basepower:tag_not_empty` for material guards. Forge
  50's built-in tag-empty condition does not correctly test tag contents on
  the registry-ops load path.

## Local evidence and gates

The pinned OreSpawn input is `4.1.0.120061` at
`4085315345ff043448b316d75e03f4b8da45402d`, with the native jar hash recorded in
`gradle.properties`. Base Metals is pinned to its shared-hook checkpoint
`3.1.2.120061` at `9e706db5c85fa2bc7f36dee94f39a5f2fa8ab367`, SHA-256
`6186AC62C7E2E333373EA0D8AD8DE53FF859C520EBF568DFB47517C4F77C8C5D`.
The basePower input is `2.4.13.120061` at
`e6ecfb341006186d0adba9dcc3f296297cd34aec`, with native jar SHA-256
`29C1BD5EF384F8BB2441029061615E52A4B7302B9FC5480F2CE9C84C357A3181`.
The frozen inputs, including the unchanged Power bootstrap, are recorded in
`build/port-evidence/final-e6ecfb3-9e706db/input-hashes.json`. Electric uses
its own worldless test fixture; the bootstrap is not a runtime input.

The frozen candidate is
`build/port-evidence/final-e6ecfb3-9e706db/candidate/baseElectric-2.2.14.120061.jar`,
SHA-256 `E9002244F220D7DDBCDBD7B512AB0F34710D8B49276DB4CD72BA131536FE0330`.
The same directory contains the sources and Javadoc jars and `SHA256SUMS`.
The forced Gradle rebuild reproduced the player jar byte for byte. The
entry-by-entry comparison with the corrected provisional jar showed only
the new Power source reference in the manifest.

Receipts under `build/port-evidence/final-e6ecfb3-9e706db`:

- `full-audit.log`: 102 JUnit tests, 11 isolated resource tests, release
  artifact and runtime input audits, checksums and Eclipse configuration.
- `gametest.log`: 4 required Forge GameTests, including material recipes.
- `packaged/receipt.json`: dedicated Forge smoke on the frozen jar, including
  legacy inventory conversion, sulfur loot, damage types and clean shutdown.
- `jar-comparison.json` and `static-audit.json`: archive and recipe parity.

Local Electric qualification is separate from family endurance, client visual
acceptance and whole-world upgrade coverage. The jar is not manual-ready
until the combined gate is met.

Earlier exact receipts, including `jar-comparison-75297fd.json` and
`packaged-exact-75297fd/receipt.json`, remain under `build/port-evidence`.
The comparison in `jar-material-guards-75297fd-comparison.json` documents
137 restored guards in 48 recipes, with no ingredient or output changes.
Earlier runs with Power `75297fd` predate the new Base Metals migration pin.
Integrated runs with Power `E192657C...` are provisional because of its loot regression.
The earlier advancement failure is recorded in
`D:/PowerAdvantage/_verification/base-1206-port-20261110/native1206-recipes-provisional-a.log`.

- `build/port-evidence/static-first.json`: accepted-resource, registry and
  test inventory checks; all 58 recipe conversions checked.
- `build/port-evidence/native-resources-prepin-final.log`: Java 21 / Forge 50,
  11 isolated tests, resource processing, recipe guards and repository checks passed.
  Earlier failed harness runs and their XML are retained separately.
- `node tools/generate-recipe-unlocks.js --check`: 50 checked-in unlocks.
- `build/port-evidence/native-api-unit-full-third.log`: all 102 worldless
  tests passed against the provisional Power API and shared-hook Base Metals.
  The Electric test fixture does not need Power's earlier unit agent.
- `build/port-evidence/packaged-prepin-c/receipt.json`: real packaged Forge
  dedicated server passed common setup, drill placement, live conversion of a
  legacy battery entry, sulfur loot, both damage types, save and clean shutdown.
  This is a provisional diagnostic with recorded jar hashes, not the
  exact-source gate.

For the packaged smoke, use the pinned installer and an existing installed
Forge 50.2.0 Windows server seed (for example, the IronAgeFurniture
`server-production-core-02` fixture). The smoke script copies only the
launcher, libraries and the already accepted EULA to a fresh
`build/port-evidence` directory. Keep the baseline and any user worlds intact.
The smoke is a fresh-world check, not migration or manual gameplay acceptance.
