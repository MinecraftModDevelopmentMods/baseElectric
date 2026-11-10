package zone.moddev.mc.baseelectric.init;

import java.util.LinkedHashMap;
import java.util.Map;

import cyano.poweradvantage.api.GUIBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.blocks.ElectricConduitBlock;
import zone.moddev.mc.baseelectric.blocks.ElectricScaffoldBlock;
import zone.moddev.mc.baseelectric.blocks.Ore;
import zone.moddev.mc.baseelectric.blocks.PumpPipeBlock;
import zone.moddev.mc.baseelectric.machines.ElectricBatteryArrayBlock;
import zone.moddev.mc.baseelectric.machines.ElectricCrusherBlock;
import zone.moddev.mc.baseelectric.machines.ElectricDrillBlock;
import zone.moddev.mc.baseelectric.machines.ElectricFabricatorBlock;
import zone.moddev.mc.baseelectric.machines.ElectricFurnaceBlock;
import zone.moddev.mc.baseelectric.machines.ElectricOvenBlock;
import zone.moddev.mc.baseelectric.machines.ElectricPumpBlock;
import zone.moddev.mc.baseelectric.machines.ElectricStillBlock;
import zone.moddev.mc.baseelectric.machines.GrowthChamberBlock;
import zone.moddev.mc.baseelectric.machines.GrowthChamberControllerBlock;
import zone.moddev.mc.baseelectric.machines.HydroelectricGeneratorBlock;
import zone.moddev.mc.baseelectric.machines.LEDBlock;
import zone.moddev.mc.baseelectric.machines.LaserTurretBlock;
import zone.moddev.mc.baseelectric.machines.PhotovoltaicGeneratorBlock;
import zone.moddev.mc.baseelectric.machines.PlasticRefineryBlock;
import zone.moddev.mc.baseelectric.machines.SteamPoweredElectricGeneratorBlock;
import zone.moddev.mc.basepower.blocks.BlockPowerSwitch;
import zone.moddev.mc.basepower.init.ItemGroups;

/** Stable block registry for Electric Advantage. */
@Mod.EventBusSubscriber(modid = ElectricAdvantage.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Blocks {
    private static final Map<String, Block> ALL_BLOCKS = new LinkedHashMap<>();

    public static Block electric_conduit;
    public static Block lithium_ore;
    public static Block sulfur_ore;
    public static Block electric_track;
    public static Block led_bar;
    public static GUIBlock steam_powered_generator;
    public static GUIBlock photovoltaic_generator;
    public static GUIBlock hydroelectric_generator;
    public static GUIBlock arc_furnace;
    public static GUIBlock battery_array;
    public static GUIBlock rock_crusher;
    public static GUIBlock laser_drill;
    public static GUIBlock laser_turret;
    public static GUIBlock laser_turret_evil;
    public static GUIBlock fabricator;
    public static GUIBlock growth_chamber;
    public static GUIBlock growth_chamber_controller;
    public static GUIBlock oven;
    public static Block pump_pipe_electric;
    public static Block electric_switch;
    public static GUIBlock electric_still;
    public static GUIBlock electric_pump;
    public static GUIBlock plastic_refinery;

    private static boolean initialized;
    private Blocks() {}

    /** Kept for callers of the legacy bootstrap; Forge owns block construction. */
    public static void init() {}

    private static synchronized void constructBlocks() {
        if (initialized) return;
        electric_conduit = add(new ElectricConduitBlock(), "electric_conduit");
        lithium_ore = add(new Ore(1), "li_ore");
        sulfur_ore = add(Ore.withDeferredDrop(() -> new ItemStack(Items.sulfur_powder, 1), 0, 3), "sulfur_ore");
        electric_track = add(new ElectricScaffoldBlock(), "electric_track");
        laser_turret = (GUIBlock) add(new LaserTurretBlock(false), "laser_turret");
        laser_turret_evil = (GUIBlock) add(new LaserTurretBlock(true), "laser_turret_evil");
        led_bar = add(new LEDBlock(), "led_bar");
        photovoltaic_generator = (GUIBlock) add(new PhotovoltaicGeneratorBlock(), "photovoltaic_generator");
        hydroelectric_generator = (GUIBlock) add(new HydroelectricGeneratorBlock(), "hydroelectric_generator");
        steam_powered_generator = (GUIBlock) add(new SteamPoweredElectricGeneratorBlock(), "steam_powered_generator");
        arc_furnace = (GUIBlock) add(new ElectricFurnaceBlock(), "electric_furnace");
        battery_array = (GUIBlock) add(new ElectricBatteryArrayBlock(), "electric_battery_array");
        rock_crusher = (GUIBlock) add(new ElectricCrusherBlock(), "electric_crusher");
        laser_drill = (GUIBlock) add(new ElectricDrillBlock(), "electric_drill");
        fabricator = (GUIBlock) add(new ElectricFabricatorBlock(), "electric_fabricator");
        growth_chamber = (GUIBlock) add(new GrowthChamberBlock(), "growth_chamber");
        growth_chamber_controller = (GUIBlock) add(new GrowthChamberControllerBlock(), "growth_chamber_controller");
        oven = (GUIBlock) add(new ElectricOvenBlock(), "electric_oven");
        electric_switch = add(new BlockPowerSwitch(Power.ELECTRIC_POWER), "electric_switch");
        electric_pump = (GUIBlock) add(new ElectricPumpBlock(), "electric_pump");
        electric_still = (GUIBlock) add(new ElectricStillBlock(), "electric_still");
        plastic_refinery = (GUIBlock) add(new PlasticRefineryBlock(), "plastic_refinery");
        pump_pipe_electric = add(new PumpPipeBlock(), "pump_pipe_electric");
        initialized = true;
    }

    private static Block add(Block block, String id) {
        ALL_BLOCKS.put(id, block);
        return block;
    }

    @SubscribeEvent public static void registerBlocks(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, registry -> {
            constructBlocks();
            ALL_BLOCKS.forEach((id, block) -> registry.register(new ResourceLocation(ElectricAdvantage.MODID, id), block));
        });
    }

    @SubscribeEvent public static void registerBlockItems(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, registry -> ALL_BLOCKS.forEach((id, block) -> {
            BlockItem item = new BlockItem(block, new Item.Properties());
            if (!"pump_pipe_electric".equals(id) && !"laser_turret_evil".equals(id)) {
                ItemGroups.show(item);
            }
            registry.register(new ResourceLocation(ElectricAdvantage.MODID, id), item);
        }));
    }
}
