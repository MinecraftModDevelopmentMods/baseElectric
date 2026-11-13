# Minecraft 26.2 Published Release

baseElectric 2.2.18.2602001 was published on 13 November 2026. Its public
downloads were checked against merged-source CI and the qualified publication
promotion. This documentation update does not rebuild or republish it.

## Downloads

- [CurseForge file 9584544](https://www.curseforge.com/minecraft/mc-mods/baseelectric/files/9584544)
- [MMD Maven player jar](https://maven.moddev.zone/releases/zone/moddev/mc/baseelectric/baseElectric/2.2.18.2602001/baseElectric-2.2.18.2602001.jar)
- [GitHub release, sources and Javadoc](https://github.com/MinecraftModDevelopmentMods/baseElectric/releases/tag/2.2.18.2602001)

Published source: [e1e3b54b34e1607cf256356cd7d74d23afaf7202](https://github.com/MinecraftModDevelopmentMods/baseElectric/commit/e1e3b54b34e1607cf256356cd7d74d23afaf7202).
Tag: `2.2.18.2602001`.

Published player SHA-256:
`2928E1C6CEBAC42AC761E5E36D695F1B8490E47E71BBC57CE563F56A069929E0`.
Forge 65 uses the same jar for play and development.

## Dependencies And Promotion

Use published [basePower 2.4.17.2602001](https://www.curseforge.com/minecraft/mc-mods/basepower/files/9583433),
[Base Metals 3.1.2.2602001](https://www.curseforge.com/minecraft/mc-mods/base-metals/files/9583122)
and [OreSpawn 4.1.1.2602001](https://www.curseforge.com/minecraft/mc-mods/mmd-orespawn/files/9582731).
These include the shared migration-hook and retained-settings repairs.

The [12 November profile03 candidate record](NATIVE262.md) keeps its original
player hash:
`A7D12A66CA91A6BACCFEFD2CED9D3EC962F5FEB73BF632E1683F99084384F938`.
Do not substitute the published jar in that old table or its sealed fixture.

The publication promotion changed only `META-INF/MANIFEST.MF` pin metadata;
the classes and other resources match the original candidate. That did not
make the old receipt qualify the new jar. Profile04 separately passed the
original-1.10 operating migration, two genuine 1,800-second worlds, cold
restart, resumed processing and an independent 66-checkpoint replay. Its
public player, sources and Javadoc downloads matched merged-source CI.

Manual acceptance of the original candidate was recorded on 12 November 2026.
The earlier failures and candidate records remain unchanged. This follow-up
checks publication links and documentation hygiene only; it adds no gameplay result and does not
qualify other Minecraft versions or every modpack. Keep world backups and
upgrade disposable copies first.
