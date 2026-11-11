package zone.moddev.mc.baseelectric.verification;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import zone.moddev.mc.baseelectric.init.Entities;
import zone.moddev.mc.baseelectric.machines.*;

/** Real native type/state pairs, scoped to one worldless test invocation. */
public final class NativeMachineFixture implements TestRule {
    private static final ThreadLocal<NativeMachineFixture> ACTIVE = new ThreadLocal<>();
    private final Map<Field, Object> previous = new LinkedHashMap<>();
    private final Map<Class<? extends Block>, Block> blocks = new LinkedHashMap<>();
    private final List<Block> allBlocks = new ArrayList<>();

    @Override public Statement apply(Statement test, Description description) {
        return new Statement() {
            @Override public void evaluate() throws Throwable {
                if (ACTIVE.get() != null) throw new IllegalStateException("Nested machine fixture");
                ACTIVE.set(NativeMachineFixture.this);
                try {
                    NativeTestBootstrap.initialize();
                    install("ELECTRIC_FURNACE", ElectricFurnaceTileEntity::new, new ElectricFurnaceBlock());
                    install("ELECTRIC_CRUSHER", ElectricCrusherTileEntity::new, new ElectricCrusherBlock());
                    install("ELECTRIC_DRILL", ElectricDrillTileEntity::new, new ElectricDrillBlock());
                    install("ELECTRIC_FABRICATOR", ElectricFabricatorTileEntity::new, new ElectricFabricatorBlock());
                    install("GROWTH_CHAMBER", GrowthChamberTileEntity::new, new GrowthChamberBlock());
                    install("GROWTH_CHAMBER_CONTROLLER", GrowthChamberControllerTileEntity::new, new GrowthChamberControllerBlock());
                    install("ELECTRIC_OVEN", ElectricOvenTileEntity::new, new ElectricOvenBlock());
                    install("LASER_TURRET", LaserTurretTileEntity::new, new LaserTurretBlock(false), new LaserTurretBlock(true));
                    install("LED", LEDTileEntity::new, new LEDBlock());
                    install("HYDROELECTRIC_GENERATOR", HydroelectricGeneratorTileEntity::new, new HydroelectricGeneratorBlock());
                    install("PHOTOVOLTAIC_GENERATOR", PhotovoltaicGeneratorTileEntity::new, new PhotovoltaicGeneratorBlock());
                    install("STEAM_POWERED_ELECTRIC_GENERATOR", SteamPoweredElectricGeneratorTileEntity::new, new SteamPoweredElectricGeneratorBlock());
                    install("ELECTRIC_BATTERY_ARRAY", ElectricBatteryArrayTileEntity::new, new ElectricBatteryArrayBlock());
                    install("ELECTRIC_PUMP", ElectricPumpTileEntity::new, new ElectricPumpBlock());
                    install("ELECTRIC_STILL", ElectricStillTileEntity::new, new ElectricStillBlock());
                    install("PLASTIC_REFINERY", PlasticRefineryTileEntity::new, new PlasticRefineryBlock());
                    test.evaluate();
                } finally {
                    try {
                        for (var entry : previous.entrySet()) entry.getKey().set(null, entry.getValue());
                    } finally {
                        previous.clear();
                        blocks.clear();
                        allBlocks.clear();
                        ACTIVE.remove();
                    }
                }
            }
        };
    }

    private <T extends BlockEntity> void install(String name, BlockEntityType.BlockEntitySupplier<T> factory,
            Block... validBlocks) throws ReflectiveOperationException {
        Field field = Entities.class.getField(name);
        previous.put(field, field.get(null));
        field.set(null, BlockEntityType.Builder.of(factory, validBlocks).build(null));
        for (Block block : validBlocks) blocks.putIfAbsent(block.getClass(), block);
        allBlocks.addAll(List.of(validBlocks));
    }

    public static BlockState state(Class<? extends Block> type) {
        NativeMachineFixture fixture = ACTIVE.get();
        if (fixture == null || !fixture.blocks.containsKey(type)) {
            throw new IllegalStateException("No native machine fixture for " + type.getName());
        }
        return fixture.blocks.get(type).defaultBlockState();
    }

    public static Block[] blocks() {
        NativeMachineFixture fixture = ACTIVE.get();
        if (fixture == null) throw new IllegalStateException("No native machine fixture");
        return fixture.allBlocks.toArray(Block[]::new);
    }
}
