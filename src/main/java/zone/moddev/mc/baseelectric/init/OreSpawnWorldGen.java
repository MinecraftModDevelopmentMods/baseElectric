package zone.moddev.mc.baseelectric.init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.resources.Identifier;
import net.minecraftforge.fml.ModList;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.basepower.migration.OreSpawnProfileMigration;
import zone.moddev.mc.orespawn.api.GeologyFamily;
import zone.moddev.mc.orespawn.api.OrePattern;
import zone.moddev.mc.orespawn.api.OreSpawnApi;
import zone.moddev.mc.orespawn.api.WorldgenProvider;

/** Registers ElectricAdvantage's visible fallback mineral rules with OreSpawn 4. */
public final class OreSpawnWorldGen {
    public static final Identifier LITHIUM_ORE =
            Identifier.fromNamespaceAndPath(ElectricAdvantage.MODID, "ore/lithium");
    public static final Identifier SULFUR_ORE =
            Identifier.fromNamespaceAndPath(ElectricAdvantage.MODID, "ore/sulfur");
    private static final Identifier OVERWORLD = Identifier.fromNamespaceAndPath("minecraft", "overworld");
    private static final Identifier STONE = Identifier.fromNamespaceAndPath("minecraft", "stone");
    private static final Identifier STONE_TAG = Identifier.fromNamespaceAndPath("forge", "stone");

    private OreSpawnWorldGen() {
    }

    public static void registerMigrationDefaults(Path configDirectory) throws IOException {
        OreSpawnProfileMigration.registerProvider(configDirectory,
                createProvider(ModList.isLoaded("mineralogy"), ModList.isLoaded("baseminerals")).toJson());
    }

    public static void register(Path configDirectory) {
        boolean mineralogyLoaded = ModList.isLoaded("mineralogy");
        boolean baseMineralsLoaded = ModList.isLoaded("baseminerals");
        boolean accepted = OreSpawnApi.enqueue(createProvider(mineralogyLoaded, baseMineralsLoaded));
        String sulfurOwner = mineralogyLoaded ? "mineralogy"
                : baseMineralsLoaded ? "baseminerals" : ElectricAdvantage.MODID;
        String lithiumOwner = baseMineralsLoaded ? "baseminerals" : ElectricAdvantage.MODID;
        ElectricAdvantage.LOGGER.info(
                "OreSpawn sulfur candidates=[mineralogy, baseminerals, baseelectric], "
                        + "selected fresh-world owner={}; lithium candidates=[baseminerals, baseelectric], "
                        + "selected fresh-world owner={}; explicit override={}",
                sulfurOwner, lithiumOwner, describeOverride(configDirectory));
        if (!accepted) {
            ElectricAdvantage.LOGGER.warn("OreSpawn rejected ElectricAdvantage's typed provider; "
                    + "an authoritative provider override may already be active");
        }
    }

    static WorldgenProvider createProvider(boolean mineralogyLoaded, boolean baseMineralsLoaded) {
        boolean electricSulfurEnabled = !mineralogyLoaded && !baseMineralsLoaded;
        boolean electricLithiumEnabled = !baseMineralsLoaded;
        return WorldgenProvider.builder(ElectricAdvantage.MODID, 1)
                .ore(LITHIUM_ORE, Identifier.fromNamespaceAndPath(ElectricAdvantage.MODID, "li_ore"), ore -> ore
                        .enabled(electricLithiumEnabled)
                        .retrogen(false)
                        .dimension(OVERWORLD, dimension -> addHosts(dimension
                                .enabled(true)
                                .yRange(1, 31)
                                .attempts(0.125D)
                                .quantityRange(6, 9)
                                .pattern(OrePattern.DEFAULT))))
                .ore(SULFUR_ORE, Identifier.fromNamespaceAndPath(ElectricAdvantage.MODID, "sulfur_ore"), ore -> ore
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
        return "none";
    }
}
