# Electric on Minecraft 1.21.11

The local baseElectric 2.2.16.121111 port targets Forge 61.1.0, official
1.21.11 mappings, MCP 20251223.124241, Java 21, ForgeGradle 7.0.34 and
Gradle 9.6.1. The source baseline is the manually accepted 1.21.1 commit
`c0ee871f43c5f0691efd85c1e4abb6a1ab70ede3`.
The port is reconciled with merged 1.21.1 head
`b81645579c3330476b84149402c3ce78183df85f`, retaining its release history and guards.

## Compatibility

Base Metals 3.1.2.121111 is REQUIRED on both client and server. Forge metadata
declares mandatory=true, version range [3.1.2.121111,4.0.0), AFTER ordering and
BOTH sides. A runtime without Base Metals is not a supported optional profile.
The approved local migration-hook pin remains separate from public releases.

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
checks, plus a native ModInfo dependency regression with an optional-metadata
negative control. The independent suite expects 23 tests. The static audit retains all
106 accepted unit tests and four GameTests; the full suite now expects 112
unit tests without skips. A static inventory check does not establish that
the full suite compiles or executes.

The approved local Power integration source is
41959572b11dce236df98f23e97c6a3868cdda03, with player/development SHA-256
C98D2CF163BE2480D9AFB0023BBD2D92D401AE0D50BEF73191235BF3F83774A7.
This includes the required-Base-Metals metadata correction and the native
registry/DFU, description-prefix, atlas and biome-index repairs. This is a
LOCAL_SOURCE integration pin, not a merged/public 1.21.11 release.
Base Metals is pinned to the separately approved local migration hook
616d527e50e4bf0137eeb9c44fa586e34a0452a0 at SHA-256
598D3FB0FF956EBCC67FE477A01CE393C75D0013AF0CE00A79E2072B63E9F2BA.
These inputs repair the shared migration RETURN hooks to mutate the same NBT
in place without cancelling the other mod's callback. Earlier 0240/6D24 inputs
and their receipts are retained, not relabelled as qualification of these bytes.
The earlier D8BA/598D and 71E0/598D candidates and their receipts are also
preserved, as are the C2 and D inputs and their scoped receipts. The D player
jar at 6FB613EECAE677A66BF4A9BEDACD632A621CFD61BC4981CAF2F5372BB89A0240
remains held separately. Entry-by-entry comparison confirms Electric production
classes and resources are unchanged: only mods.toml and the provenance manifest
differ from D in the new player jar. The sources jar differs only in mods.toml;
the Javadoc jar is byte-identical. Graphical C2 receipts remain bound to C2, not relabelled
as current qualification. Combined/raw/endurance and manual gates remain
parent-owned.

The required-dependency correction's native parser and static dependency
checks are separate from the prior D release/GameTest receipts:
the optional metadata first failed the native mandatory assertion, then the
correction passed all 23 independent tests without failures or skips and all
seven static audits. Release-configuration, recipe, layout and hygiene checks
also pass. On the approved 4195957/C98D input, the default no-path-override
release suite passes with 46 forced tasks, 112 unit tests in 33 suites and the
23 independent resource tests, all without failures or skips. Dependency,
artifact, checksum, Javadoc and generated production Eclipse audits pass.
Actual Eclipse ECJ compiles all 88 main, 36 test, one GameTest and three client
sources; its fresh Java 21 JUnit output passes all 112 tests without failures,
ignored tests or assumptions. Its restricted-JDK negative control is rejected.
Player/source/Javadoc SHA-256 values are generated from actual bytes in
build/release/SHA256SUMS. OreSpawn, mod versions and publication state are
unchanged. Family qualification remains separate.

Native ingredient-codec tests cover item strings, tag strings and alternative
arrays, including rejection of old item/tag objects. The live recipe GameTest
also decodes all 58 packaged recipe JSONs against actual runtime registries and
round-trips them through Recipe.CODEC. All four live GameTests passed on their
original candidates; no world/GameTest run is repeated for this metadata batch.
Native test functions and test-instance resources are registered explicitly; the run
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
