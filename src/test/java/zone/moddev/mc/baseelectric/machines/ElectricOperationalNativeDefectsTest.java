package zone.moddev.mc.baseelectric.machines;

import cyano.poweradvantage.api.fluid.FluidStack;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.junit.BeforeClass;
import org.junit.Test;
import zone.moddev.mc.baseelectric.init.Power;

import static org.junit.Assert.*;

/** Focused regressions for retained independent native operating failures. */
public class ElectricOperationalNativeDefectsTest {
    @BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }

    @Test public void controllerCannotRequestMoreThanItsHundredUnitElectricalBuffer() throws Exception {
        GrowthChamberControllerTileEntity controller = controller();
        assertEquals(100, controller.getPowerRequest(Power.ELECTRIC_POWER).amount, 0);
        assertEquals(0, controller.getEnergy(Power.ELECTRIC_POWER), 0);
        assertEquals(1000, controller.getSoil(), 0);
        assertEquals(1800, controller.getTank().getFluidAmount());
    }

    @Test public void controllerSubtractsElectricityAlreadyStoredFromInputCapacity() throws Exception {
        GrowthChamberControllerTileEntity controller = controller();
        controller.setEnergy(40, Power.ELECTRIC_POWER);
        assertEquals(60, controller.getPowerRequest(Power.ELECTRIC_POWER).amount, 0);
        controller.setEnergy(100, Power.ELECTRIC_POWER);
        assertEquals(0, controller.getPowerRequest(Power.ELECTRIC_POWER).amount, 0);
    }

    @Test public void scarceResourcesAndExistingElectricityBoundTheRemainingDemand() throws Exception {
        GrowthChamberControllerTileEntity controller = controller();
        controller.setSoil(1);
        controller.getTank().setFluid(new FluidStack(Fluids.WATER, 2));
        controller.setEnergy(4, Power.ELECTRIC_POWER);
        assertEquals(4, controller.getPowerRequest(Power.ELECTRIC_POWER).amount, 0);
        controller.setEnergy(999.75f, Power.GROWTHCHAMBER_POWER);
        assertEquals(0, controller.getPowerRequest(Power.ELECTRIC_POWER).amount, 0);
    }

    @Test public void fullGreenhouseBufferCannotRequestMoreElectricity() throws Exception {
        GrowthChamberControllerTileEntity controller = controller();
        controller.setEnergy(1000, Power.GROWTHCHAMBER_POWER);
        assertEquals(0, controller.getPowerRequest(Power.ELECTRIC_POWER).amount, 0);
    }

    @Test public void panelSamplesIncidentLightAboveItsSolidBody() throws Exception {
        LightWorld world = withoutConstructor(LightWorld.class);
        BlockPos panel = new BlockPos(101, 82, -27);
        world.surface = panel.above();
        world.brightness = 14;
        assertEquals(0.1f * 14 * 0.0666666f,
                PhotovoltaicGeneratorTileEntity.getLightIntensityAt(panel, world), 0.000001f);
        assertEquals(1, world.skyReads);
        assertEquals(1, world.lightReads);
    }

    @Test public void darkPanelSurfaceProducesNoBlocklightPower() throws Exception {
        LightWorld world = withoutConstructor(LightWorld.class);
        BlockPos panel = new BlockPos(101, 82, -27);
        world.surface = panel.above();
        assertEquals(0, PhotovoltaicGeneratorTileEntity.getLightIntensityAt(panel, world), 0);
    }

    @Test public void beamIgnoresOnlyItsOwnCollisionPostAndStopsAtTheDeclaredRange() throws Exception {
        BeamWorld world = withoutConstructor(BeamWorld.class);
        world.turret = new BlockPos(101, 82, -27);
        StubTurret turret = new StubTurret(world.turret);
        turret.testWorld = world;
        Vec3 origin = new Vec3(101.5, 82.75, -26.5);
        Method trace = LaserTurretTileEntity.class.getDeclaredMethod("followRayToSolidBlock",
                Vec3.class, Vec3.class, double.class);
        trace.setAccessible(true);
        Vec3 end = (Vec3)trace.invoke(turret, origin, new Vec3(1, 0, 0), 32.0);
        assertEquals(1, world.traces);
        assertEquals(32, origin.distanceTo(end), 0.000001);
    }

    private static GrowthChamberControllerTileEntity controller() throws Exception {
        GrowthChamberControllerTileEntity controller = new GrowthChamberControllerTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.GrowthChamberControllerBlock().defaultBlockState());
        controller.setSoil(1000);
        controller.getTank().setFluid(new FluidStack(Fluids.WATER, 1800));
        Field redstone = GrowthChamberControllerTileEntity.class.getDeclaredField("redstone");
        redstone.setAccessible(true);
        redstone.setBoolean(controller, false);
        return controller;
    }

    private static <T> T withoutConstructor(Class<T> type) throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field field = allocator.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(allocator.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static final class LightWorld extends ServerLevel {
        BlockPos surface;
        int brightness, skyReads, lightReads;
        private LightWorld() {
            super(null, null, null, null, null, null, null, false, 0L, Collections.emptyList(), false, null);
        }
        @Override public boolean canSeeSky(BlockPos pos) {
            assertEquals("Sky access must be sampled above the solid panel", surface, pos);
            skyReads++;
            return false;
        }
        @Override public int getMaxLocalRawBrightness(BlockPos pos) {
            assertEquals("Incident block light must be sampled above the solid panel", surface, pos);
            lightReads++;
            return brightness;
        }
    }

    private static final class BeamWorld extends ServerLevel {
        BlockPos turret;
        int traces;
        private BeamWorld() {
            super(null, null, null, null, null, null, null, false, 0L, Collections.emptyList(), false, null);
        }
        @Override public BlockHitResult clip(ClipContext context) {
            traces++;
            assertTrue("Owning turret post must not stop its own beam",
                    context.getBlockShape(new LaserTurretBlock(false).defaultBlockState(), this, turret).isEmpty());
            assertFalse("An immediately adjacent solid block must still stop the beam",
                    context.getBlockShape(Blocks.STONE.defaultBlockState(), this, turret.east()).isEmpty());
            return BlockHitResult.miss(context.getTo(), Direction.EAST, BlockPos.containing(context.getTo()));
        }
    }

    private static final class StubTurret extends LaserTurretTileEntity {
        StubTurret(BlockPos pos) { super(pos, new LaserTurretBlock(false).defaultBlockState()); }
        Level testWorld;
        @Override public Level getLevel() { return testWorld; }
    }
}
