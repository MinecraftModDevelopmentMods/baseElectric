package zone.moddev.mc.baseelectric.blocks;

import cyano.poweradvantage.api.ConduitBlock;
import cyano.poweradvantage.api.PowerConnectorContext;
import cyano.poweradvantage.api.simple.BlockSimplePowerConduit;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import org.junit.BeforeClass;
import org.junit.Test;
import zone.moddev.mc.baseelectric.init.Power;
import zone.moddev.mc.baseelectric.machines.ElectricDrillBlock;
import zone.moddev.mc.baseelectric.machines.ElectricFurnaceBlock;
import zone.moddev.mc.baseelectric.machines.GrowthChamberBlock;
import zone.moddev.mc.baseelectric.machines.GrowthChamberControllerBlock;
import zone.moddev.mc.baseelectric.machines.LaserTurretBlock;
import zone.moddev.mc.baseelectric.machines.PhotovoltaicGeneratorBlock;
import static org.junit.Assert.*;

public class NativeBlockContractTest {
    @BeforeClass public static void bootstrapNativeRegistries() { NativeTestBootstrap.initialize(); }

    @Test public void scaffoldRetainsNonSolidFullCollisionAndSharedStateProperties() {
        ElectricScaffoldBlock block = new ElectricScaffoldBlock();
        BlockState state = block.defaultBlockState();
        assertFalse(state.canOcclude());
        assertFalse(state.isRedstoneConductor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertEquals(Shapes.block().bounds(),
                block.getCollisionShape(state, null, BlockPos.ZERO, CollisionContext.empty()).bounds());
        assertFalse(state.isSolidRender());
        assertTrue(state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertEquals(6, state.getProperties().size());
        assertSame(BlockSimplePowerConduit.NORTH, ElectricScaffoldBlock.NORTH);
        assertSame(BlockSimplePowerConduit.SOUTH, ElectricScaffoldBlock.SOUTH);
        assertSame(BlockSimplePowerConduit.EAST, ElectricScaffoldBlock.EAST);
        assertSame(BlockSimplePowerConduit.WEST, ElectricScaffoldBlock.WEST);
        assertSame(BlockSimplePowerConduit.UP, ElectricScaffoldBlock.UP);
        assertSame(BlockSimplePowerConduit.DOWN, ElectricScaffoldBlock.DOWN);
        for (PathComputationType type : PathComputationType.values()) assertFalse(block.isPathfindable(state, type));
    }

    @Test public void scaffoldInheritsSharedPlacementAndNetworkCallbacks() throws Exception {
        Class<ElectricScaffoldBlock> scaffold = ElectricScaffoldBlock.class;
        assertSame(BlockSimplePowerConduit.class, scaffold.getSuperclass());
        assertSame(BlockSimplePowerConduit.class,
                scaffold.getMethod("getStateForPlacement", BlockPlaceContext.class).getDeclaringClass());
        assertSame(BlockSimplePowerConduit.class,
                scaffold.getMethod("updateShape", BlockState.class, net.minecraft.world.level.LevelReader.class,
                        net.minecraft.world.level.ScheduledTickAccess.class, BlockPos.class, Direction.class,
                        BlockPos.class, BlockState.class, net.minecraft.util.RandomSource.class).getDeclaringClass());
        assertSame(BlockSimplePowerConduit.class,
                scaffold.getMethod("canAcceptConnection", PowerConnectorContext.class).getDeclaringClass());
        assertSame(ConduitBlock.class,
                scaffold.getMethod("onPlace", BlockState.class, Level.class, BlockPos.class,
                        BlockState.class, boolean.class).getDeclaringClass());
        assertSame(ConduitBlock.class,
                inheritedMethod(scaffold, "affectNeighborsAfterRemoval", BlockState.class,
                        net.minecraft.server.level.ServerLevel.class, BlockPos.class, boolean.class).getDeclaringClass());
        assertSame(ConduitBlock.class,
                scaffold.getMethod("wasExploded", net.minecraft.server.level.ServerLevel.class,
                        BlockPos.class, Explosion.class).getDeclaringClass());
        assertSame(ConduitBlock.class,
                scaffold.getMethod("wasExploded", Level.class, BlockPos.class, Explosion.class).getDeclaringClass());
        ElectricScaffoldBlock block = new ElectricScaffoldBlock();
        assertEquals(1, block.getTypes().length);
        assertSame(Power.ELECTRIC_POWER, block.getTypes()[0]);
        assertFalse(block.isPowerSink(Power.ELECTRIC_POWER));
        assertFalse(block.isPowerSource(Power.ELECTRIC_POWER));
    }

    private static java.lang.reflect.Method inheritedMethod(Class<?> type, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { return current.getDeclaredMethod(name, parameters); }
            catch (NoSuchMethodException absent) { /* Continue through protected native callbacks. */ }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    @Test public void customMachinePropertiesKeepDrillAndBothTurretsNonSolid() {
        assertFalse(new ElectricDrillBlock().defaultBlockState().canOcclude());
        assertFalse(new LaserTurretBlock(false).defaultBlockState().canOcclude());
        assertFalse(new LaserTurretBlock(true).defaultBlockState().canOcclude());
    }

    @Test public void hiddenPumpPipeRemainsNonSolidAndSolarPanelRemainsSolid() {
        BlockState pipe = new PumpPipeBlock().defaultBlockState();
        pipe.initCache();
        assertFalse(pipe.canOcclude());
        assertTrue("The metal pipe was solid independently of its occlusion shape", pipe.blocksMotion());
        assertSame(PushReaction.NORMAL, pipe.getPistonPushReaction());
        PhotovoltaicGeneratorBlock panel = new PhotovoltaicGeneratorBlock();
        panel.defaultBlockState().initCache();
        assertTrue("The 1.14 photovoltaic block explicitly overrides isSolid to true", panel.defaultBlockState().canOcclude());
        assertTrue(panel.defaultBlockState().blocksMotion());
        assertTrue(panel.defaultBlockState().isRedstoneConductor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    @Test public void removedPistonMaterialKeepsItsMapColorAndBlocksPistons() {
        for (var block : java.util.List.of(new ElectricConduitBlock(), new ElectricScaffoldBlock(),
                new ElectricDrillBlock(), new ElectricFurnaceBlock(), new GrowthChamberBlock(),
                new GrowthChamberControllerBlock(), new LaserTurretBlock(false))) {
            BlockState state = block.defaultBlockState();
            // Forge fills this cache for registered blocks after the registry events.
            state.initCache();
            String name = block.getClass().getSimpleName();
            assertSame(name, PushReaction.BLOCK, state.getPistonPushReaction());
            assertTrue(name, state.blocksMotion());
            assertSame(name, MapColor.STONE, state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        }
    }

    @Test public void hiddenPumpPipeUsesNativeNoLootTable() {
        BlockState state = new PumpPipeBlock().defaultBlockState();
        assertTrue(state.getDrops(new LootParams.Builder((net.minecraft.server.level.ServerLevel) null)).isEmpty());
    }
}
