# Minecraft 26.3 Published Release

baseElectric 2.2.19.2603001 was published on 13 November 2026. Its public
downloads match merged-source CI and the separately qualified final family.
This documentation follow-up does not rebuild or republish the release.

## Downloads

- [CurseForge file 9590229](https://www.curseforge.com/minecraft/mc-mods/baseelectric/files/9590229)
- [MMD Maven player jar](https://maven.moddev.zone/releases/zone/moddev/mc/baseelectric/baseElectric/2.2.19.2603001/baseElectric-2.2.19.2603001.jar)
- [GitHub release, sources and Javadoc](https://github.com/MinecraftModDevelopmentMods/baseElectric/releases/tag/2.2.19.2603001)

Published source: [22d0a9e962bb04382ff2cc75715fd85364be927c](https://github.com/MinecraftModDevelopmentMods/baseElectric/commit/22d0a9e962bb04382ff2cc75715fd85364be927c).
Tag: `2.2.19.2603001`. Maven coordinate:
`zone.moddev.mc.baseelectric:baseElectric:2.2.19.2603001`.
Forge 66 uses official names; the ordinary player jar is also the development jar.

| Archive | SHA-256 |
| --- | --- |
| Player / deobf | `7DD8DD0D2348360C41C64BDE9ADC88E95284842DE1FCEA73CEBC9A4938AD7819` |
| Sources | `99F4819A20112BF915CF26DB50DB9B1B70CC29742635586B103CD815A5C52826` |
| Javadoc | `AF0F8E1DDA91238C9CC080BC39B3E7F4154FB1B5689D2E005DD15A022A8F2A11` |

Seven anonymous downloads were checked: the player from all three channels,
plus sources and Javadoc from Maven and GitHub. The public POM and Maven index
were checked too. CurseForge accepted one upload; its existing file ID was
resumed after approval, without another upload.

## Dependencies And Promotion

Use published [basePower 2.4.18.2603001](https://www.curseforge.com/minecraft/mc-mods/basepower/files/9589309),
[Base Metals 3.1.2.2603001](https://www.curseforge.com/minecraft/mc-mods/base-metals/files/9589195)
and [OreSpawn 4.1.1.2603001](https://www.curseforge.com/minecraft/mc-mods/mmd-orespawn/files/9589145).
Power's release source remains `4fae0a51feb3e49875f09c500b869266122a711a`,
with player SHA-256
`97525CE9C75509C55225E0061D43BAF9D29BDBDE6DF853E1552A64A54C14C3DE`.
The independent test-only `unit_source_ref` remains
`c0eb83f7ba91cf8c83caea75e63057407a345949`; it is not the player source pin.

The [historical profile04 record](NATIVE263.md) retains the accepted source
`464f2e9aaae9e2e8ee91f68ea44503b8bc2b2d27` and player hash
`44ED38BFC7040D73251E523B2803718CDA2212482268A7E65BCF4681CF9A75E8`.
Manual acceptance of that exact kit was recorded on 13 November 2026.
Its original machine, recipe, broader migration, graphical, restart and
endurance receipts stay bound to those inputs; the old table is unchanged.

The published player differs only in `META-INF/MANIFEST.MF`:
`API-Pin-State` changed from `LOCAL_SOURCE` to `PUBLISHED`, and
`basePower-Source-Ref` changed from the test-support revision above to the
released Power source. Every other player entry is identical. Sources and
Javadoc are unchanged from the accepted archives. Registry and saved-data
identities, recipes, quantities, balance and public contracts are retained.

The final published-pin family separately passed original-1.10 operating
migration, two genuine 1,800-second worlds, raw/cold resumes including native
DataVersion 5023 worlds, and independent 66-checkpoint replay. Final custody
confirmed 479 sealed inputs and 633 protected files unchanged. This is not a
claim that every earlier migration route or graphical check was rerun for
the new player hash.

The final candidate also passed 119 unit tests, 30 native tests, fresh ECJ
coverage of all 119 units and the 52-task release/Eclipse gate, with no test
failures or skips. Earlier cold-cache, reproducibility, paths-with-spaces and
GameTest receipts retain their exact input hashes. The parent's final combined
Eclipse check passed on this family with 385 Power tests.

Earlier failed and superseded attempts remain intact. Documentation checks
add no gameplay result and do not qualify other Minecraft versions or every
modpack. Keep world backups and upgrade disposable copies first.
