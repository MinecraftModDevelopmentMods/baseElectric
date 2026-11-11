package zone.moddev.mc.baseelectric.migration;

import cyano.poweradvantage.api.migration.LegacyMigrationApi;
import cyano.poweradvantage.api.simple.BlockSimplePowerMachine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.machines.ElectricDrillBlock;
import zone.moddev.mc.baseelectric.machines.ElectricGeneratorBlock;
import zone.moddev.mc.baseelectric.machines.ElectricMachineBlock;
import zone.moddev.mc.baseelectric.machines.LEDBlock;
import zone.moddev.mc.basepower.blocks.BlockPowerSwitch;

/** Registers previous Electric Advantage and baseElectric saves with the shared migration hook. */
public final class ElectricLegacyMigrations {
    private static boolean registered;

    private ElectricLegacyMigrations() {}

    public static synchronized void register() {
        if (registered) return;

        LegacyMigrationApi.registerNamespaceAlias(ElectricAdvantage.LEGACY_MODID, ElectricAdvantage.MODID);
        for (String path : new String[] {
                "electric_conduit", "li_ore", "sulfur_ore", "electric_track",
                "laser_turret", "laser_turret_evil", "led_bar", "photovoltaic_generator",
                "hydroelectric_generator", "steam_powered_generator", "electric_furnace",
                "electric_battery_array", "electric_crusher", "electric_drill",
                "electric_fabricator", "growth_chamber", "growth_chamber_controller",
                "electric_oven", "electric_switch", "electric_pump", "electric_still",
                "plastic_refinery", "pump_pipe_electric" }) {
            LegacyMigrationApi.registerBlockStateCodec(id(path), ElectricLegacyMigrations::decodeState);
        }

        for (String path : new String[] {
                "electric_furnace", "electric_crusher", "electric_drill", "electric_fabricator",
                "growth_chamber", "growth_chamber_controller", "electric_oven", "laser_turret",
                "led", "hydroelectric_generator", "photovoltaic_generator",
                "steam_powered_electric_generator", "electric_battery_array", "electric_pump",
                "electric_still", "plastic_refinery" }) {
            LegacyMigrationApi.registerTileEntityAliases(id(path));
        }
        String led = id("led").toString();
        String hydroturbine = id("hydroturbine").toString();
        for (String namespace : new String[] {ElectricAdvantage.LEGACY_MODID, ElectricAdvantage.MODID}) {
            LegacyMigrationApi.registerTileEntityAlias(namespace + ".l_e_d", led);
            LegacyMigrationApi.registerTileEntityAlias(namespace + ":l_e_d", led);
            LegacyMigrationApi.registerTileEntityAlias("minecraft:" + namespace + ".l_e_d", led);
            LegacyMigrationApi.registerEntityAlias(namespace + ":hydroturbine", hydroturbine);
            LegacyMigrationApi.registerEntityAlias(namespace + ".hydroturbine", hydroturbine);
            LegacyMigrationApi.registerEntityAlias("minecraft:" + namespace + ".hydroturbine", hydroturbine);
            String repeated = namespace + "." + namespace + ".hydroturbine";
            LegacyMigrationApi.registerEntityAlias(repeated, hydroturbine);
            LegacyMigrationApi.registerEntityAlias("minecraft:" + repeated, hydroturbine);
        }
        LegacyMigrationApi.registerEntityAlias("minecraft:electricadvantage.Hydroturbine", hydroturbine);
        LegacyMigrationApi.registerEntityAlias("electricadvantage.electricadvantage.Hydroturbine", hydroturbine);
        registered = true;
    }

    private static BlockState decodeState(Block block, int metadata) {
        if (block instanceof ElectricDrillBlock) {
            return block.defaultBlockState()
                    .setValue(ElectricDrillBlock.FACING, legacyDrillFacing(metadata))
                    .setValue(ElectricDrillBlock.ACTIVE, legacyDrillActive(metadata));
        }
        if (block instanceof LEDBlock) {
            return ((LEDBlock) block).getStateFromLegacyMeta(metadata);
        }
        if (block instanceof ElectricGeneratorBlock) {
            return ((ElectricGeneratorBlock) block).getStateFromLegacyMeta(metadata);
        }
        if (block instanceof ElectricMachineBlock) {
            return ((ElectricMachineBlock) block).getStateFromLegacyMeta(metadata);
        }
        if (block instanceof BlockPowerSwitch) {
            return ((BlockPowerSwitch) block).getStateFromMeta(metadata);
        }
        if (block instanceof BlockSimplePowerMachine) {
            return ((BlockSimplePowerMachine) block).getStateFromMeta(metadata);
        }
        return block.defaultBlockState();
    }

    static Direction legacyDrillFacing(int metadata) {
        return Direction.from3DDataValue(metadata & 0x7);
    }

    static boolean legacyDrillActive(int metadata) {
        return (metadata & 0x8) != 0;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ElectricAdvantage.MODID, path);
    }
}
