# Electric on Minecraft 1.20.6

baseElectric 2.2.14.120061 uses Forge 50.2.0, Java 21 and basePower
2.4.13.120061. The ordinary native jar is used both in a game and as a
development dependency. No companion mods are bundled.

## Bringing an old world forward

Back up your save and upgrade a copy. Replace Electric Advantage and Power
Advantage with baseElectric and basePower. Keep the original save for use with
the old mods.

Machine, block and entity IDs and saved data keys have not changed. Batteries
retain their charge when legacy item data moves into Minecraft's component
format. An explicit zero still means empty; a missing charge still means full.
Drills preserve their direction, inventory, target and progress when moving
along their track. basePower's migration also reads old nested inventory items.

## Recipes and checks

Material-dependent recipes are available when the corresponding tags have
ingredients. Their recipe-book advancements stay loaded across recipe modes.
The live tests check machine operation, battery conversion, sulfur loot,
damage types and recipe registration. The packaged startup smoke is a
fresh-world check; it is separate from world-upgrade, restart and client tests.

See the [README](../README.md) for the build and verification commands.
