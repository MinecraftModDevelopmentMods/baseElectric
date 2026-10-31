# Electric Advantage 1.17.1 Port

Status: locally verified against Power source/API checkpoint
`28247f936ac99e7a417da1bc1c89bc8394773a26` / `7B2B2676CDB9A0B933DA04CC743787803C68E1466FD44E2EFBAEFB3ED47ABB3D`.
Combined packaged runtime, migration and manual acceptance remain pending.
No commit or remote delivery has been made for this port.

## Native API Audit

Class moves were derived from the cached 1.16.5 MCP joined mappings and Mojang
class names, then checked against the mapped Forge `1.17.1-37.1.1` jar. All explicit
Minecraft imports resolve there. Strings and NBT keys were not globally renamed.
Changed descriptors were checked with `javap` against that same mapped jar.

The native block-entity construction, loading and ticking requirements also match
[Forge's 1.17 documentation](https://docs.minecraftforge.net/en/1.17.x/blockentities/blockentity/).

- All 16 machine types use native two-argument factories and `BlockEntitySupplier`.
  Each block passes its actual `BlockPos` and `BlockState` into its machine.
- Persistence overrides use `load(CompoundTag)` and call the shared superclass.
  NBT coordinates cannot replace the native constructor position/state.
- Moving drills load their payload into the newly placed machine without a
  reflective position rewrite, then debit the retained movement cost.
- Shared GUI block parents own `PoweredEntity.tick()` routing on both logical
  sides. LED is a conduit rather than a GUI block and supplies its own ticker.
  First-use server redstone sampling and the eight-tick update order are retained.
- Hydroturbine constructors use `EntityType` and `Level`; native `discard()`
  replaces the obsolete no-argument removal call. Saved parent links survive an
  unloaded parent chunk and can resolve a replacement live block entity.
- Laser packets use constructor coordinates; renderer registration uses
  `EntityRenderersEvent.RegisterRenderers`, native contexts, `PoseStack` and
  `VertexConsumer`. Buffer geometry, normals, UVs and tolerances are retained.
- The native unshaded shader retains lightmap, overlay, alpha and fog processing
  without diffuse directional lighting. GPU compilation and appearance remain
  untested; static resource tests are not visual acceptance.
- Pump pickup uses native `BucketPickup.pickupBlock`, including waterlogged
  sources. Null/empty refusal controls and energy/fluid assertions are retained.
- Lithium uses `mineable/pickaxe` and `needs_stone_tool`; sulfur uses pickaxe only.
  Existing drop quantities, fortune/silk behavior and self-drop resources remain.
- Capability ownership remains in Power Advantage. Electric does not introduce a
  second Forge Energy/fluid adapter or suppress invalidation/revival checks.
- Configuration loading uses `ModConfigEvent.Loading`; existing options and
  defaults are unchanged.

## Retained Watchpoints

The inherited regression suite covers all six drill planes, progress/NBT,
native ray geometry, first-tick redstone caching, solar brightness above the
panel, controller input capacity, still output preflight, waterlogged pickup,
sound recipient bounds, turbine save/load/anchoring and normalized hydro GUI
output. New tests check native factories, tag-only load positions, turret packet
coordinates, the LED ticker type and shader resources. All 95 tests pass on the
current checkpoint with both Gradle/JUnit and independent ECJ/JUnit.

The plain-JVM harvest fixture initializes Forge's real tier ordering and native
tag bindings. The build-only agent calls Forge's own capability and event-bus
plugins; no replacement event or capability implementation is shipped. Earlier
game-version/bootstrap/tag-fixture failures remain in the local evidence.

All existing recipes are byte-for-byte retained. The published Base Metals 1.17
`CrushingRecipe` remains the integration API. Its raw-metal recipes must be
exercised with public runtime bytes in the shared rig. The supplied public jar's
copper, iron and gold recipes each produce two powders per ore block but one
per raw item. A new dependency-resource test retains that published distinction;
no fabricated conversion or silent balance adjustment is included here.

## Qualification Gates

Current local evidence: `electric-checkpoint-i.log` in the shared campaign records
35 tasks executed successfully with `--no-build-cache --rerun-tasks`, including
all 95 tests and every deterministic release/Eclipse audit. Independent evidence
is `evidence-117-20261031/ecj-checkpoint-a/ecj-results.json`: fresh compilation of
85 production and 27 test sources, class major 60, 95 tests, zero failures,
ignored tests or assumptions. The restricted-JDK negative control rejected
`sun.security.x509.X500Name` as expected. The local static contracts and 50 recipe
unlocks pass. Hosted CI and interactive IDE launches have not been run.

The production jar SHA-256 is
`CCA5F24A079BA75FDE0833A641FF9DAA57F55C16505C221D308CD045BF892B3B`.
The development jar SHA-256 is
`637225151CE258AE9237BA6D8E9CD98FF8AE22533D0E118E74E2D37B406FDCD0`.
Both hashes were unchanged by the two full forced rebuilds H and I; H retained a
failed old-format Eclipse audit, while I passed the native bootstrap audit.
This is a local checkpoint, not final qualification or manual acceptance.

1. Keep the exact current Power local source/API checkpoint and published API
   checksums pinned. Any coordinated source/API replacement requires new proof.
2. Use the campaign lease helper for fresh compile, all 95 JUnit tests, strict
   dependency/resource/release audits, Javadocs and Eclipse generation/audit.
3. Run independent ECJ compilation and strict JUnit on Java 16, preserving access
   restrictions, failed diagnostics and exact input hashes. Verify class major 60.
   Native Forge capability tokens require the parent's verified build-only unit
   bootstrap agent in both Gradle and standalone JUnit. Do not replace those tokens
   or weaken capability tests to make a plain JVM run green.
4. Use immutable packaged Electric/Power candidates and exact public dependency
   jars for the parent's disposable client/server, operating and migration rig.
   Include finite power/fluid conservation, dirty-tank saves, battery caches and
   charge NBT, capability revival, drill movement debit, refinery/still capacity,
   helper entities, first-tick redstone and GUI caption rows.
5. Reproduce the same artifacts and retain both successful and failed receipts.
6. Obtain explicit approval for each local frozen module commit. Manual gameplay
   acceptance is still required before any port-code push, PR, merge, tag or release.

Original worlds and old-version checkouts are read-only. No hosted workflow,
publication or manual acceptance is implied by a local build result.
