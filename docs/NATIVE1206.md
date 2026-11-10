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
- Recipe stacks use `id`, Forge conditions are registered as codecs, and
  generated recipe advancements use native item predicates and the same guards
  as their recipes. Ordinary block loot table data is unchanged; the Forge
  smoke runner now checks the 1.20.6 item stack representation.

## Local evidence and gates

The pinned OreSpawn input is `4.1.0.120061` at
`4085315345ff043448b316d75e03f4b8da45402d`, with the native jar hash recorded in
`gradle.properties`. Base Metals is pinned to its shared-hook checkpoint
`3.1.2.120061` at `0c2cdd4d0e8c3bf2e5fa234d49e57960bbf80bde`. The exact
coordinated basePower pin is required before packaging. A compile-only Base
Metals API jar cannot be used to package or qualify Electric.

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
- The exact-pin Gradle build and packaged Forge smoke are separate gates.
  Client and world upgrade acceptance remain separate as well.

For the packaged smoke, use the pinned installer and an existing installed
Forge 50.2.0 Windows server seed (for example, the IronAgeFurniture
`server-production-core-02` fixture). The smoke script copies only the
launcher, libraries and the already accepted EULA to a fresh
`build/port-evidence` directory. Keep the baseline and any user worlds intact.
The smoke is a fresh-world check, not migration or manual gameplay acceptance.
