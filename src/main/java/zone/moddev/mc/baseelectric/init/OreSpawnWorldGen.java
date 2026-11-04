package zone.moddev.mc.baseelectric.init;

import zone.moddev.mc.baseelectric.ElectricAdvantage;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import zone.moddev.mc.orespawn.api.GeologyFamily;
import zone.moddev.mc.orespawn.api.OrePattern;
import zone.moddev.mc.orespawn.api.OreSpawnApi;
import zone.moddev.mc.orespawn.api.WorldgenProvider;
import zone.moddev.mc.basepower.compat.LegacyWorldgenIdentity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Registers baseElectric's fallback mineral rules with OreSpawn 4. */
public final class OreSpawnWorldGen {
    public static final ResourceLocation LITHIUM_ORE =
            new ResourceLocation(ElectricAdvantage.MODID, "ore/lithium");
    public static final ResourceLocation SULFUR_ORE =
            new ResourceLocation(ElectricAdvantage.MODID, "ore/sulfur");
    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");
    private static final ResourceLocation STONE = new ResourceLocation("minecraft", "stone");
    private static final ResourceLocation STONE_TAG = new ResourceLocation("forge", "stone");

    private OreSpawnWorldGen() {
    }

    public static void register(Path configDirectory) {
        migrateConfiguration(configDirectory);
        boolean mineralogyLoaded = Loader.isModLoaded("mineralogy");
        boolean baseMineralsLoaded = Loader.isModLoaded("baseminerals");
        boolean accepted = OreSpawnApi.enqueue(createProvider(mineralogyLoaded, baseMineralsLoaded));
        String sulfurOwner = mineralogyLoaded ? "mineralogy"
                : baseMineralsLoaded ? "baseminerals" : ElectricAdvantage.MODID;
        String lithiumOwner = baseMineralsLoaded ? "baseminerals" : ElectricAdvantage.MODID;
        FMLLog.info("%s: OreSpawn sulfur candidates=[mineralogy, baseminerals, baseelectric], "
                        + "selected fresh-world owner=%s; lithium candidates=[baseminerals, baseelectric], "
                        + "selected fresh-world owner=%s; explicit override=%s",
                ElectricAdvantage.MODID, sulfurOwner, lithiumOwner, describeOverride(configDirectory));
        if (!accepted) {
            FMLLog.warning("%s: OreSpawn rejected the typed world-generation provider; "
                    + "an authoritative provider override may already be active", ElectricAdvantage.MODID);
        }
    }

    static int migrateConfiguration(Path configDirectory) {
        try {
            // basePower already migrates both owners before OreSpawn preInit reads profiles.
            return LegacyWorldgenIdentity.migrateConfig(configDirectory,
                    ElectricAdvantage.LEGACY_MODID, ElectricAdvantage.MODID);
        } catch (IOException failure) {
            throw new IllegalStateException("baseElectric cannot safely migrate its OreSpawn configuration", failure);
        }
    }

    public static WorldgenProvider createProvider(boolean mineralogyLoaded, boolean baseMineralsLoaded) {
        boolean electricSulfurEnabled = !mineralogyLoaded && !baseMineralsLoaded;
        boolean electricLithiumEnabled = !baseMineralsLoaded;
        return WorldgenProvider.builder(ElectricAdvantage.MODID, 1)
                .ore(LITHIUM_ORE, new ResourceLocation(ElectricAdvantage.MODID, "li_ore"), ore -> ore
                        .enabled(electricLithiumEnabled)
                        .retrogen(false)
                        .dimension(OVERWORLD, dimension -> addHosts(dimension
                                .enabled(true)
                                .yRange(1, 31)
                                .attempts(0.125D)
                                .quantityRange(6, 9)
                                .pattern(OrePattern.DEFAULT))))
                .ore(SULFUR_ORE, new ResourceLocation(ElectricAdvantage.MODID, "sulfur_ore"), ore -> ore
                        .enabled(electricSulfurEnabled)
                        .retrogen(false)
                        .dimension(OVERWORLD, dimension -> addHosts(dimension
                                .enabled(true)
                                .yRange(1, 31)
                                .attempts(1.0D)
                                .quantityRange(8, 23)
                                .pattern(OrePattern.DEFAULT))))
                .build();
    }

    private static WorldgenProvider.OreDimensionDefinition.Builder addHosts(
            WorldgenProvider.OreDimensionDefinition.Builder dimension) {
        return dimension
                .hostBlock(STONE)
                .hostTag(STONE_TAG)
                .hostFamily(GeologyFamily.SEDIMENTARY)
                .hostFamily(GeologyFamily.METAMORPHIC)
                .hostFamily(GeologyFamily.IGNEOUS_INTRUSIVE)
                .hostFamily(GeologyFamily.IGNEOUS_VOLCANIC);
    }

    static String describeOverride(Path configDirectory) {
        if (Files.isRegularFile(configDirectory.resolve(ElectricAdvantage.MODID + "-orespawn.json"))) {
            return "config/" + ElectricAdvantage.MODID + "-orespawn.json (authoritative)";
        }
        if (Files.isRegularFile(configDirectory.resolve("orespawn")
                .resolve(ElectricAdvantage.MODID + ".json"))) {
            return "config/orespawn/" + ElectricAdvantage.MODID + ".json (legacy importer authority)";
        }
        if (Files.isRegularFile(configDirectory.resolve("orespawn3")
                .resolve(ElectricAdvantage.MODID + ".json"))) {
            return "config/orespawn3/" + ElectricAdvantage.MODID + ".json (legacy importer authority)";
        }
        return "none";
    }
}
