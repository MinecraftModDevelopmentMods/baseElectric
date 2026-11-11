package zone.moddev.mc.baseelectric.entities;

import zone.moddev.mc.baseelectric.verification.NativeMachineFixture;
import zone.moddev.mc.baseelectric.machines.HydroelectricGeneratorBlock;

import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import java.lang.reflect.Field;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import zone.moddev.mc.baseelectric.machines.HydroelectricGeneratorTileEntity;
import org.junit.Test;

import static org.junit.Assert.*;

public class HydroturbinePersistenceTest {
    @org.junit.Rule public final NativeMachineFixture machines =
            new NativeMachineFixture();
    @org.junit.BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }
    @Test public void generatorGuiOutputIsNormalizedWithoutChangingGenerationRate() {
        HydroelectricGeneratorTileEntity active = new HydroelectricGeneratorTileEntity(net.minecraft.core.BlockPos.ZERO, NativeMachineFixture.state(HydroelectricGeneratorBlock.class)) {
            @Override public boolean isActive() { return true; }
        };
        HydroelectricGeneratorTileEntity inactive = new HydroelectricGeneratorTileEntity(net.minecraft.core.BlockPos.ZERO, NativeMachineFixture.state(HydroelectricGeneratorBlock.class)) {
            @Override public boolean isActive() { return false; }
        };
        assertEquals(1F, active.getPowerOutput(), 0F);
        assertEquals(0F, inactive.getPowerOutput(), 0F);
        assertEquals(4F, HydroelectricGeneratorTileEntity.ENERGY_PER_TICK, 0F);
    }

    @Test public void preservesPendingGeneratorAcrossRepeatedReadSaveWithoutTick() throws Exception {
        CompoundTag source = new CompoundTag();
        source.putIntArray("parentPos", new int[] {164, 81, 8});
        for (int pass = 0; pass < 3; pass++) {
            HydroturbineEntity turbine = withoutWorld();
            turbine.readAdditionalSaveData(source);
            CompoundTag saved = new CompoundTag();
            turbine.addAdditionalSaveData(saved);
            assertArrayEquals(new int[] {164, 81, 8}, saved.getIntArray("parentPos"));
            source = saved;
        }
    }

    @Test public void readingUnlinkedEntityClearsPendingLink() throws Exception {
        HydroturbineEntity turbine = withoutWorld();
        CompoundTag source = new CompoundTag();
        source.putIntArray("parentPos", new int[] {164, 81, 8});
        turbine.readAdditionalSaveData(source);
        turbine.readAdditionalSaveData(new CompoundTag());
        CompoundTag saved = new CompoundTag();
        turbine.addAdditionalSaveData(saved);
        assertFalse(saved.contains("parentPos", 11));
    }

    @Test public void savesAttachedGeneratorUsingTheSameLegacyCoordinateKey() throws Exception {
        HydroturbineEntity turbine = withoutWorld();
        HydroelectricGeneratorTileEntity generator = new HydroelectricGeneratorTileEntity(new BlockPos(31, 72, -18), NativeMachineFixture.state(HydroelectricGeneratorBlock.class));
        turbine.parent = generator;
        CompoundTag saved = new CompoundTag();
        turbine.addAdditionalSaveData(saved);
        assertArrayEquals(new int[] {31, 72, -18}, saved.getIntArray("parentPos"));
    }

    @Test public void malformedLegacyCoordinatesDoNotCreateAPartialParentLink() throws Exception {
        HydroturbineEntity turbine = withoutWorld();
        CompoundTag source = new CompoundTag();
        source.putIntArray("parentPos", new int[] {31, 72});
        turbine.readAdditionalSaveData(source);
        CompoundTag saved = new CompoundTag();
        turbine.addAdditionalSaveData(saved);
        assertFalse(saved.contains("parentPos", 11));
    }

    // Only exercise the custom NBT methods; no world, registry or entity tick is needed.
    private static HydroturbineEntity withoutWorld() throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field field = allocator.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (HydroturbineEntity) allocator.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), HydroturbineEntity.class);
    }
}
