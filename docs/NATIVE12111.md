# Electric on Minecraft 1.21.11

The local baseElectric 2.2.16.121111 port targets Forge 61.1.0, official
1.21.11 mappings, MCP 20251223.124241, Java 21, ForgeGradle 7.0.34 and
Gradle 9.6.1. The source baseline is the manually accepted 1.21.1 commit
`c0ee871f43c5f0691efd85c1e4abb6a1ab70ede3`.
The port is reconciled with merged 1.21.1 head
`b81645579c3330476b84149402c3ce78183df85f`, retaining its release history and guards.

## Compatibility

Persistent registrations, legacy Electric Advantage aliases, configuration
keys, root machine NBT, battery components and recipe progression are retained.
The 58 recipe files use native item and tag ingredients. All 50 discovery
advancements and all 64 track states retain their accepted contents. Native
item definitions point to the existing models and textures.

Machine persistence uses native value input/output with a root map codec;
it does not introduce a new enclosing compound. Native block-entity removal
owns inventory drops. Renderer submission uses extracted state and the native
entity shader's no-cardinal-lighting mode, retaining lightmap, overlay and fog.

Root pack metadata declares min=max [94,1] and mods.toml explicitly sets the
top-level showAsResourcePack=false. Forge's actual hidden client delegate
synthesizes 75.x metadata; the server uses the root's 94.1 metadata. Neither
omitted metadata nor a broad cross-PackType range is used. The child-JVM native
proof verifies all 306 client and 160 server files byte-for-byte without
warnings/errors. See FORGE61-RESOURCE-METADATA.md for the native path and the
separate Forge 61 server discovery TOO_NEW label defect. Both effective packs
are compatible with their actual native current version; mods.toml permits
only Minecraft 1.21.11.

## Verification Boundaries

The independent native resource/API checks do not require sibling mod jars.
They retain the previous 17 resource checks, add two native fabricator-plan
tests including root-NBT round-trips, and add three native pack range/path
checks. The independent suite expects 22 tests. The static audit retains all
106 accepted unit tests and four GameTests; the full suite now expects 111
unit tests without skips. A static inventory check does not establish that
the full suite compiles or executes.

The local Base Metals migration hook is pinned separately from public releases.
An owner-supplied, unqualified basePower 1.21.11 jar was frozen by its SHA-256
for compile diagnostics only. All 88 production sources, 36 test sources and
the GameTest source compile with javac and actual Eclipse ECJ; both fresh unit
outputs pass all 111 tests without failures, skips or assumptions. The new
Power integration input is frozen at SHA-256
0240E032075AF6B158887FBF98CE038504942329527036B82FD7CF9EEE602503;
the approved local source commit is
398439074a60bf9b8f8872f92485cffc4aa96be9. The default no-override release suite,
dependency/artifact/checksum audits, Javadoc, generated Eclipse metadata and
actual ECJ main/test/GameTest/client scopes pass on these inputs. This is a
LOCAL_SOURCE integration pin, not a merged/public 1.21.11 release.

Native ingredient-codec tests cover item strings, tag strings and alternative
arrays, including rejection of old item/tag objects. The live recipe GameTest
also decodes all 58 packaged recipe JSONs against actual runtime registries and
round-trips them through Recipe.CODEC. All four live GameTests pass. Native
test functions and test-instance resources are registered explicitly; the run
selects baseelectric:* and requires the exact four successful IDs in its report.
The network oracle compares exact bytes of the current native recipe-book
display packet, rather than the obsolete full-recipe synchronization path.
Forge 61's old optional-ingredient encoder throws for shaped empty slots, and
network-unpacked shaped recipes cannot encode their original source JSON.
Both failed observations are retained without patching frozen Forge jars.

Approved source/artifact pins are recorded and cannot be overridden. The
accepted 1.21.1 jars are not valid substitutes. Full combined packaged clean-log
and client visual gates remain pending. The development GameTest log retains
initial-config and upstream Power method-annotation warnings; a successful
GameTest run is not a clean-log or graphical qualification.

Final qualification must use one stable set of hashes, including actual
Eclipse ECJ main/test, GameTest and client-only scopes, the default build without
dependency path overrides, and independent cold/path-with-spaces reproduction.
Generated logs and failed receipts remain local under build/port-evidence.

No whole-world upgrade, family gameplay, restart, client visual acceptance,
publication or release readiness is established by the independent checks.
