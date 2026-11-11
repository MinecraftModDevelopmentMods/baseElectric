# Electric on Minecraft 1.21.1

baseElectric 2.2.15.121011 targets Forge 52.1.0 and Java 21 with basePower
2.4.14.121011. It remains a local port until the coordinated dependency pins
and qualification gates are complete. Nothing has been published.

## Saves and Machines

Back up your save and try upgrades on a disposable copy. Keep the original
world for the old mods. Block, item, entity and configuration IDs are unchanged.
Legacy Electric Advantage aliases and saved data keys remain in place.

Battery charge stays in native item components. An explicit zero remains empty;
missing charge remains full. Drill direction, movement state, inventories,
targets and progress retain their existing storage. Machine costs, capacities,
generation rates and redstone timing have not been rebalanced.

The Forge 52 port uses native recipe inputs, resource-location factories and
vertex consumers. Electric fluid gauges still use basePower's renderer and its
normalized sprite UVs. The hidden pump helper still has no loot table.

## Data Packs

Registry directories are now `recipe`, `advancement`, `loot_table`, `tags/item`
and `tags/block`. Paths inside each registry, including `recipes/` within the
advancement registry and `blocks/` within loot tables, have not changed.
All 58 recipe files and 50 unlock advancements retain their released contents.
Old plural registry folders are not a supported alternative.

## Verification Boundaries

The static audit compares against the released 1.20.6 commit and retains all
102 accepted tests and their assertions. New contracts check singular registry
paths and real block-entity fixture types, including cleanup after failures.

`nativeResourceChecks` and `compileNativeApiDiagnostics` are independent of
basePower. Full native and ECJ runs pass 106 tests without skips. The four
GameTests, archive and dependency audits, Javadoc and Eclipse checks pass
against the coordinated local basePower commit. GameTest and client-only
contract sources compile separately from the player jar.

These checks do not replace whole-world upgrade, family gameplay, restart or
visual acceptance. Rebuilt dependencies require fresh qualification.

Hosted CI and publication remain gated on merged and public dependency pins.
Local compilation is not evidence of runtime, migration or visual acceptance.
