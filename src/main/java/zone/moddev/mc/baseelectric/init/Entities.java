package zone.moddev.mc.baseelectric.init;


import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.entities.HydroturbineEntity;
import zone.moddev.mc.baseelectric.machines.ElectricBatteryArrayTileEntity;
import zone.moddev.mc.baseelectric.machines.ElectricCrusherTileEntity;
import zone.moddev.mc.baseelectric.machines.ElectricDrillTileEntity;
import zone.moddev.mc.baseelectric.machines.ElectricFabricatorTileEntity;
import zone.moddev.mc.baseelectric.machines.ElectricFurnaceTileEntity;
import zone.moddev.mc.baseelectric.machines.ElectricOvenTileEntity;
import zone.moddev.mc.baseelectric.machines.ElectricPumpTileEntity;
import zone.moddev.mc.baseelectric.machines.ElectricStillTileEntity;
import zone.moddev.mc.baseelectric.machines.GrowthChamberControllerTileEntity;
import zone.moddev.mc.baseelectric.machines.GrowthChamberTileEntity;
import zone.moddev.mc.baseelectric.machines.HydroelectricGeneratorTileEntity;
import zone.moddev.mc.baseelectric.machines.LEDTileEntity;
import zone.moddev.mc.baseelectric.machines.LaserTurretTileEntity;
import zone.moddev.mc.baseelectric.machines.PhotovoltaicGeneratorTileEntity;
import zone.moddev.mc.baseelectric.machines.PlasticRefineryTileEntity;
import zone.moddev.mc.baseelectric.machines.SteamPoweredElectricGeneratorTileEntity;

/** Stable tile/entity registrations; IDs are unchanged from 1.12. */
@Mod.EventBusSubscriber(modid = ElectricAdvantage.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Entities {
    public static BlockEntityType<ElectricFurnaceTileEntity> ELECTRIC_FURNACE;
    public static BlockEntityType<ElectricCrusherTileEntity> ELECTRIC_CRUSHER;
    public static BlockEntityType<ElectricDrillTileEntity> ELECTRIC_DRILL;
    public static BlockEntityType<ElectricFabricatorTileEntity> ELECTRIC_FABRICATOR;
    public static BlockEntityType<GrowthChamberTileEntity> GROWTH_CHAMBER;
    public static BlockEntityType<GrowthChamberControllerTileEntity> GROWTH_CHAMBER_CONTROLLER;
    public static BlockEntityType<ElectricOvenTileEntity> ELECTRIC_OVEN;
    public static BlockEntityType<LaserTurretTileEntity> LASER_TURRET;
    public static BlockEntityType<LEDTileEntity> LED;
    public static BlockEntityType<HydroelectricGeneratorTileEntity> HYDROELECTRIC_GENERATOR;
    public static BlockEntityType<PhotovoltaicGeneratorTileEntity> PHOTOVOLTAIC_GENERATOR;
    public static BlockEntityType<SteamPoweredElectricGeneratorTileEntity> STEAM_POWERED_ELECTRIC_GENERATOR;
    public static BlockEntityType<ElectricBatteryArrayTileEntity> ELECTRIC_BATTERY_ARRAY;
    public static BlockEntityType<ElectricPumpTileEntity> ELECTRIC_PUMP;
    public static BlockEntityType<ElectricStillTileEntity> ELECTRIC_STILL;
    public static BlockEntityType<PlasticRefineryTileEntity> PLASTIC_REFINERY;
    public static EntityType<HydroturbineEntity> HYDROTURBINE;

    private Entities() {}
    public static void init() {}

    @SubscribeEvent public static void registerTiles(RegistryEvent.Register<BlockEntityType<?>> event) {
        ELECTRIC_FURNACE = tile(event, "electric_furnace", ElectricFurnaceTileEntity::new, Blocks.arc_furnace);
        ELECTRIC_CRUSHER = tile(event, "electric_crusher", ElectricCrusherTileEntity::new, Blocks.rock_crusher);
        ELECTRIC_DRILL = tile(event, "electric_drill", ElectricDrillTileEntity::new, Blocks.laser_drill);
        ELECTRIC_FABRICATOR = tile(event, "electric_fabricator", ElectricFabricatorTileEntity::new, Blocks.fabricator);
        GROWTH_CHAMBER = tile(event, "growth_chamber", GrowthChamberTileEntity::new, Blocks.growth_chamber);
        GROWTH_CHAMBER_CONTROLLER = tile(event, "growth_chamber_controller", GrowthChamberControllerTileEntity::new, Blocks.growth_chamber_controller);
        ELECTRIC_OVEN = tile(event, "electric_oven", ElectricOvenTileEntity::new, Blocks.oven);
        LASER_TURRET = tile(event, "laser_turret", LaserTurretTileEntity::new, Blocks.laser_turret, Blocks.laser_turret_evil);
        LED = tile(event, "led", LEDTileEntity::new, Blocks.led_bar);
        HYDROELECTRIC_GENERATOR = tile(event, "hydroelectric_generator", HydroelectricGeneratorTileEntity::new, Blocks.hydroelectric_generator);
        PHOTOVOLTAIC_GENERATOR = tile(event, "photovoltaic_generator", PhotovoltaicGeneratorTileEntity::new, Blocks.photovoltaic_generator);
        STEAM_POWERED_ELECTRIC_GENERATOR = tile(event, "steam_powered_electric_generator", SteamPoweredElectricGeneratorTileEntity::new, Blocks.steam_powered_generator);
        ELECTRIC_BATTERY_ARRAY = tile(event, "electric_battery_array", ElectricBatteryArrayTileEntity::new, Blocks.battery_array);
        ELECTRIC_PUMP = tile(event, "electric_pump", ElectricPumpTileEntity::new, Blocks.electric_pump);
        ELECTRIC_STILL = tile(event, "electric_still", ElectricStillTileEntity::new, Blocks.electric_still);
        PLASTIC_REFINERY = tile(event, "plastic_refinery", PlasticRefineryTileEntity::new, Blocks.plastic_refinery);
    }

    @SubscribeEvent public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        HYDROTURBINE = EntityType.Builder.<HydroturbineEntity>of(
                HydroturbineEntity::new, MobCategory.MISC)
                .sized(0.9375F, 0.9375F)
                .setTrackingRange(64).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true)
                .build(ElectricAdvantage.MODID + ":hydroturbine");
        HYDROTURBINE.setRegistryName(ElectricAdvantage.MODID, "hydroturbine");
        event.getRegistry().register(HYDROTURBINE);
    }

    private static <T extends BlockEntity> BlockEntityType<T> tile(
            RegistryEvent.Register<BlockEntityType<?>> event, String id, BlockEntityType.BlockEntitySupplier<T> factory,
            net.minecraft.world.level.block.Block... blocks) {
        BlockEntityType<T> type = BlockEntityType.Builder.<T>of(factory, blocks).build(null);
        type.setRegistryName(ElectricAdvantage.MODID, id);
        event.getRegistry().register(type);
        return type;
    }

}
