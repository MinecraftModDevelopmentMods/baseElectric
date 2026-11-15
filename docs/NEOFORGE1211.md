# baseElectric For NeoForge 1.21.1

Forge comparison baseline: `b81645579c3330476b84149402c3ce78183df85f`.
Native loader/toolchain: NeoForge 21.1.265, Java 21, ModDevGradle 2.0.148,
Gradle 9.8.0. Electric 2.2.20.121012 depends on Power 2.4.19.121012.

## Dependencies

MMDLib 2.0.2.121012+Neo, Base Metals 4.0.1.121012+Neo and OreSpawn
4.1.1.121012 resolve from MMD Maven. The Base Metals default is the published
`zone.moddev.mc.basemetals:BaseMetals:4.0.1.121012+Neo` artifact, not the
private migration-hook candidate. Its source commit is
`39b240bb0d9f80997847ab4df70a2dd23e6ea826` and its SHA-256 is
`B4BB1CBCD54DFCF2EC8465ACE82715EDCCC5BF1E5C15E14C627AE7CC58E8FED1`.
The MMDLib SHA-256 is
`EE417C82D588B0050A49709B6430CF836315E71E4DB2DF4036D3735454BC5311`.
The exact runtime inputs are checked by `verifyReleaseDependencies`.

## Compatibility

The namespace remains `baseelectric`; all legacy aliases and serialized machine
fields remain intact. Rendering uses native NeoForge client events. Menus,
capabilities and packet transport use the coordinated Power implementation.
Item component and native entity/block-entity persistence paths remain unchanged.
Common resource tags use `c`, recipe conditions use NeoForge's condition list,
and crushing results use MMDLib's ingredient/result codec.

Back up whole worlds and test upgrades on disposable copies. Keep the original
world for its old mod set; no test suite covers every modpack.

## Qualification

Current candidate: `BC866F83F55FAD289EC36808D33A71B2602B8062EB71FC2E80C9CE3B0495F6D8`.
The canonical-LF repair passed 110 native tests, 25 resource checks, ECJ and
release audits. Exact entry comparison found only 62 JSON CRLF-to-LF changes;
all 95 class entries and the TOML metadata are byte-identical, and all 331 JSON
documents are semantically equal. Git-LF resource inputs reproduce this same
jar. Packaged requalification is **PENDING_PARENT**; the old `61457EDF...`
approval does not qualify this candidate. The campaign results below predate
this player-format correction and remain historical evidence only.

`tools/verify-native-port.js` compares the actual Forge baseline to the local
candidate. It checks byte-identical visual assets, recipe IDs and counts,
common tag members, migration/GUI contracts and normal drill mining.
JUnit tests cover machine behavior, persistence and data resources; GameTests
exercise loaded registries, recipe serialization and block-entity updates.
The GameTest run selects `gameTestValidation` as its actual runtime source set.
Its loaded-mod assertion reads native `ModList`, requires the exact eight
IDs/versions (including published Base Metals `4.0.1.121012+Neo`), and prints
`ELECTRIC_GAMETEST_LOADED_MODS` into the native server log. The Gradle run also
requires that receipt and all seven passing tests, so missing test discovery
cannot masquerade as success. A legacy-classpath listing alone is not evidence
of a loaded mod. These isolated Electric tests do not qualify Steam, the
combined stack, or a client probe.
Automated checks do not establish manual client or historic-world acceptance.

Manual testing has been accepted for this release campaign. Local qualification
passed on 2026-11-15 using Power SHA-256
`E9ED662E15C3510E2A48CE85448D3D39C6EF2667208F29EB281791200507F77E`.
The checks passed as follows:

- 110 native JUnit tests and 25 independent resource checks against the pinned public inputs, with no failures or skips.
- Seven GameTests repeated in each of NORMAL, TECH_PROGRESSION and APOCALYPTIC after recipe-resource normalization, with exact native loaded-mod receipts.
- Dependency hashes, release configuration, player/sources/Javadoc jars, checksums and repository hygiene.
- Eclipse production metadata, native IDE synchronization and three repeated classpath merges with a duplicate-entry negative control.
- ECJ compilation of 86 production and 35 test sources, plus three isolation/module negative controls; this gate does not execute JUnit.
- Seven offline source/resource contracts and all 50 recipe unlocks.
- 70 offline hosted-dependency guard checks, including Power release-tag controls. Earlier workflow syntax and eight actual ECJ container-policy proofs are retained.
- An independent full rebuild with a fresh Gradle user home containing spaces, forced task execution and no build cache reproduced the player and sources archives. Its earlier Javadoc proof is retained separately from the later documentation-only fix.

The sources archive has LF-only text across all 424 checked entries. Its
SHA-256 is `A355355EDC7CD77D315BF536DDCCC0ACD36C76BB5104D8EF7E1F125210362452`.
The Javadoc archive SHA-256 is
`774C0D2A82140169E7FB936F21936A1BDE1AE6B867593161FD8C0B48FAA3881A`.
The shared `gradle/reproducible-docs.gradle` helper replaces Windows JDK legal
pointer files with the three complete `java.base` notices and normalizes
documentation text, including `element-list`, to LF. All 145 text entries pass
the LF audit, the three legal files exactly match the full notices, and two
forced documentation builds produce the same archive. Player and sources
checksums are unchanged. Before/after jars and entry-level proofs are retained
under `javadoc-reproducibility-20261115-01` in the campaign evidence directory.
The independent-cache result establishes same-platform reproducibility, not a
Linux hosted-player comparison.

Compiler deprecation warnings and native development-runtime warnings remain
recorded in the evidence; these checks were not warning-free. Power source
commit `c5550ce133ce0f5d3fe3e5227f021b390cdc7953` and tag `2.4.19.121012` are now
pinned. The strict network guard and final native release suite against freshly
downloaded public inputs passed. All three archive hashes match their prior
sealed values. Previous results remain evidence
for their original inputs. Combined-stack, packaged runtime and publication
checks are coordinated separately.

This Electric candidate is unmerged and unpublished. Hosted CI remains gated
on approved merged/public Power pins. Keep all previous failure logs and test
reports. New release-campaign evidence belongs under
`D:/PowerAdvantage/_verification/base-1211neo-release-20261115/electric`, with
native GameTest receipts retained under unique `build/port-evidence` run paths.
The public-pin run is `public-power-pins-20261115-01`; the subsequent resource
packaging proof and native rerun are in `canonical-player-lf-20261115-01`.
The final player jar SHA-256 is
`BC866F83F55FAD289EC36808D33A71B2602B8062EB71FC2E80C9CE3B0495F6D8`.
The rejected `61457EDF...` jar and failed hosted audit are retained separately.
Their 62 differences are recipe JSON CRLF-to-LF changes only: all 95 class
files and every other resource are unchanged, and all 331 JSON documents have
identical values. A Git-normalized resource fixture reproduces the final jar.

The final jar received fresh combined machine, migration, recipe, unloading,
drilling and generation checks. Fresh and upgraded worlds completed separate
30-minute endurance periods, full restarts and resumed operation. Independent
replay verified 33 checkpoints and 528 station snapshots per world. These runs
use the `lf-a` suffix under the shared campaign evidence directory.
The earlier rendered development-client evidence covers unchanged client code
and assets; no new exact-jar-bound client run is claimed for this packaging fix.
Do not use the historical Forge packaged-smoke or ECJ launcher scripts for this
NeoForge candidate.

## Hosted Delivery

The native CI, CodeQL and tag workflows enforce `.github/native-dependencies.json`.
It is now `HOSTED_INPUTS_READY`: Power PR #51 is merged and an independent
anonymous Maven download returned HTTP 200 with the approved Power hash.
Setting the status alone cannot qualify an input; the lock and project
properties must agree on published state, source ancestry, release tag and
exact hashes. There is no pending-source bypass.

The verifier checks merged ancestry for Power, Base Metals and OreSpawn,
requires Power's release tag to resolve to the approved commit, fetches the four
exact Maven artifacts, and
checks their SHA-256, native metadata, mod versions and required API entries.
The Gradle initialization guard consumes only those verified file overrides.
`CHILD_VERSION` identifies Electric's candidate version.
`release_player_sha256` freezes the qualified Electric player bytes; artifact
verification fails on any drift, and tag validation requires the frozen pin.
CurseForge requirements include `basepower`, `base-metals` and `mmdlib`.

Hosted CI stays limited to native unit/resource checks, build/Javadoc, dependency
and package audits, and Eclipse synchronization. ECJ and GameTests stay local.
CodeQL compiles native Java 21 sources through ModDevGradle. Wrapper validation
targets `master-1.21.1-neo`; tag validation requires that native target, merged
tagged source and successful push CI on the same release-branch commit.

Candidate ZIPs retain `build/libs/` jar paths, `build/release/SHA256SUMS` and
`CHANGELOG.txt` for the default-branch dispatcher. Tags validate identity only;
publication remains a separate, explicitly approved step. Hosted candidate
checks passed, and all three hosted Linux archives match the local Windows
archives. Only the approved post-merge artifact may be published.
