package zone.moddev.mc.baseelectric;

import java.util.HashSet;
import java.util.Set;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import cyano.poweradvantage.api.migration.LegacyConfigMigration;
import cyano.poweradvantage.api.migration.LegacyConfigMigration.Mapping;
import cyano.poweradvantage.api.migration.LegacyConfigMigration.ValueType;
import cyano.poweradvantage.api.migration.LegacyOreSpawnConfigBridge;
import zone.moddev.mc.baseelectric.config.ElectricAdvantageConfig;
import zone.moddev.mc.baseelectric.init.GUI;
import zone.moddev.mc.baseelectric.init.OreSpawnWorldGen;
import zone.moddev.mc.baseelectric.init.TreasureChests;
import zone.moddev.mc.baseelectric.init.Villages;
import zone.moddev.mc.baseelectric.recipe.RecipeConditions;
import zone.moddev.mc.baseelectric.migration.ElectricLegacyMigrations;

/** Forge 1.21.11 entry point for baseElectric. */
@Mod(ElectricAdvantage.MODID)
public final class ElectricAdvantage {
    public static final String MODID = "baseelectric";
    public static final String LEGACY_MODID = "electricadvantage";
    public static final String NAME = "baseElectric";
    public static final String VERSION = "2.2.16.121111";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static ElectricAdvantage INSTANCE;

    public final Set<String> PLASTIC_FLUID_MATERIALS = new HashSet<>();
    public String LASER_SOUND = "minecraft:block.note_block.bass";

    public ElectricAdvantage(FMLJavaModLoadingContext context) {
        INSTANCE = this;
        ElectricLegacyMigrations.register();
        var bus = context.getModBusGroup();
        RecipeConditions.register(bus);

        migrateLegacyOreSpawnConfiguration();
        migrateLegacyConfiguration();
        context.registerConfig(ModConfig.Type.COMMON, ElectricAdvantageConfig.SPEC);
        net.minecraftforge.fml.event.config.ModConfigEvent.Loading.getBus(bus)
                .addListener(ElectricAdvantageConfig::onConfigLoading);
        net.minecraftforge.fml.event.config.ModConfigEvent.Reloading.getBus(bus)
                .addListener(ElectricAdvantageConfig::onConfigReloading);
        FMLCommonSetupEvent.getBus(bus).addListener(this::commonSetup);
        InterModEnqueueEvent.getBus(bus).addListener(this::enqueueInterModCommunication);
    }

    private static void migrateLegacyOreSpawnConfiguration() {
        try {
            LegacyOreSpawnConfigBridge.Result result =
                    LegacyOreSpawnConfigBridge.migrate(FMLPaths.CONFIGDIR.get(), MODID, LEGACY_MODID);
            if (result.wasMigrated()) {
                LOGGER.info("Imported {} legacy Electric Advantage OreSpawn rule(s) from '{}' into '{}'",
                        result.getRuleCount(), result.getSource(), result.getTarget());
            }
        } catch (IOException exception) {
            LOGGER.error("Could not migrate legacy Electric Advantage OreSpawn configuration", exception);
        }
    }

    private static void migrateLegacyConfiguration() {
        try {
            LegacyConfigMigration.Result result = LegacyConfigMigration.migrate(FMLPaths.CONFIGDIR.get(), MODID, LEGACY_MODID,
                    Mapping.of("fabricator", "recursion_limit", "fabricator", "recursionLimit", ValueType.INTEGER),
                    Mapping.of("options", "laser_sound", "options", "laserSound", ValueType.STRING),
                    Mapping.of("options", "petrolplastic_fluids", "options", "petrolplasticFluids", ValueType.STRING));
            if (result.wasMigrated()) {
                LOGGER.info("Imported {} legacy Electric Advantage settings; report: {}",
                        result.getImportedValues(), result.getReportFile());
            }
        } catch (IOException exception) {
            LOGGER.error("Could not migrate legacy Electric Advantage configuration", exception);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(GUI::init);
        ElectricAdvantageConfig.apply();
        TreasureChests.init(FMLPaths.CONFIGDIR.get());
        Villages.init();
        LOGGER.info("{} {} common setup complete", NAME, VERSION);
    }

    private void enqueueInterModCommunication(InterModEnqueueEvent event) {
        OreSpawnWorldGen.register(FMLPaths.CONFIGDIR.get());
    }
}
