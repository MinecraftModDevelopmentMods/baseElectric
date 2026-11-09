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
import org.junit.BeforeClass;
import org.junit.Test;
import zone.moddev.mc.baseelectric.init.Power;
import zone.moddev.mc.baseelectric.machines.ElectricDrillBlock;
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
        assertFalse(state.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertTrue(state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertEquals(6, state.getProperties().size());
        assertSame(BlockSimplePowerConduit.NORTH, ElectricScaffoldBlock.NORTH);
        assertSame(BlockSimplePowerConduit.SOUTH, ElectricScaffoldBlock.SOUTH);
        assertSame(BlockSimplePowerConduit.EAST, ElectricScaffoldBlock.EAST);
        assertSame(BlockSimplePowerConduit.WEST, ElectricScaffoldBlock.WEST);
        assertSame(BlockSimplePowerConduit.UP, ElectricScaffoldBlock.UP);
        assertSame(BlockSimplePowerConduit.DOWN, ElectricScaffoldBlock.DOWN);
        for (PathComputationType type : PathComputationType.values()) assertFalse(block.isPathfindable(state, null, BlockPos.ZERO, type));
    }

    @Test public void scaffoldInheritsSharedPlacementAndNetworkCallbacks() throws Exception {
        Class<ElectricScaffoldBlock> scaffold = ElectricScaffoldBlock.class;
        assertSame(BlockSimplePowerConduit.class, scaffold.getSuperclass());
        assertSame(BlockSimplePowerConduit.class,
                scaffold.getMethod("getStateForPlacement", BlockPlaceContext.class).getDeclaringClass());
        assertSame(BlockSimplePowerConduit.class,
                scaffold.getMethod("updateShape", BlockState.class, Direction.class, BlockState.class,
                        LevelAccessor.class, BlockPos.class, BlockPos.class).getDeclaringClass());
        assertSame(BlockSimplePowerConduit.class,
                scaffold.getMethod("canAcceptConnection", PowerConnectorContext.class).getDeclaringClass());
        assertSame(ConduitBlock.class,
                scaffold.getMethod("onPlace", BlockState.class, Level.class, BlockPos.class,
                        BlockState.class, boolean.class).getDeclaringClass());
        assertSame(ConduitBlock.class,
                scaffold.getMethod("onRemove", BlockState.class, Level.class, BlockPos.class,
                        BlockState.class, boolean.class).getDeclaringClass());
        assertSame(ConduitBlock.class,
                scaffold.getMethod("wasExploded", Level.class, BlockPos.class, Explosion.class).getDeclaringClass());
        ElectricScaffoldBlock block = new ElectricScaffoldBlock();
        assertEquals(1, block.getTypes().length);
        assertSame(Power.ELECTRIC_POWER, block.getTypes()[0]);
        assertFalse(block.isPowerSink(Power.ELECTRIC_POWER));
        assertFalse(block.isPowerSource(Power.ELECTRIC_POWER));
    }

    @Test public void customMachinePropertiesKeepDrillAndBothTurretsNonSolid() {
        assertFalse(new ElectricDrillBlock().defaultBlockState().canOcclude());
        assertFalse(new LaserTurretBlock(false).defaultBlockState().canOcclude());
        assertFalse(new LaserTurretBlock(true).defaultBlockState().canOcclude());
    }

    @Test public void hiddenPumpPipeRemainsNonSolidAndSolarPanelRemainsSolid() {
        assertFalse(new PumpPipeBlock().defaultBlockState().canOcclude());
        PhotovoltaicGeneratorBlock panel = new PhotovoltaicGeneratorBlock();
        assertTrue("The 1.14 photovoltaic block explicitly overrides isSolid to true", panel.defaultBlockState().canOcclude());
        assertTrue(panel.defaultBlockState().isRedstoneConductor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }
}
