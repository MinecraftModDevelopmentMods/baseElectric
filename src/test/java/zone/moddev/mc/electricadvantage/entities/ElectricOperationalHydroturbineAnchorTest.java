package zone.moddev.mc.electricadvantage.entities;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Random;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ElectricOperationalHydroturbineAnchorTest {
    @BeforeClass public static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }

    @Test public void nativeEntityLoadStillClampsOverspeedMotionAndPreservesItsBoundary() throws Exception {
        ControlEntity control = new ControlEntity(controlType(), world());
        CompoundTag tag = control.saveWithoutId(new CompoundTag());
        tag.put("Motion", motion(16.226, -10.001, 10));
        control.load(tag);
        assertEquals(new Vec3(0, 0, 10), control.getDeltaMovement());
        assertTrue(control.isPushedByFluid());
    }

    @Test public void turbineRejectsLinearWaterPushingWithoutChangingRotationRate() throws Exception {
        HydroturbineEntity turbine = new HydroturbineEntity(turbineType(), world());
        assertFalse(turbine.isPushedByFluid());
        assertEquals(Vec3.ZERO, turbine.getDeltaMovement());
        assertEquals(30, HydroturbineEntity.DEGREES_PER_TICK, 0);
    }

    @Test public void legacyMotionInsideTheNativeLimitIsNotArbitrarilyCleared() throws Exception {
        HydroturbineEntity turbine = new HydroturbineEntity(turbineType(), world());
        CompoundTag saved = turbine.saveWithoutId(new CompoundTag());
        saved.put("Motion", motion(5.5, -2, 0));
        turbine.load(saved);
        assertEquals(new Vec3(5.5, -2, 0), turbine.getDeltaMovement());
        assertFalse(turbine.isPushedByFluid());
    }

    @Test public void anchoredTurbineSurvivesNativeSaveLoadWithItsParentAndIdentity() throws Exception {
        EntityType<HydroturbineEntity> type = turbineType();
        Level world = world();
        HydroturbineEntity original = new HydroturbineEntity(type, world);
        original.setPos(56856.5, 81, 50008.5);
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        int[] parent = {56856, 82, 50008};
        saved.putIntArray("parentPos", parent);
        for (int pass = 0; pass < 3; pass++) {
            HydroturbineEntity loaded = new HydroturbineEntity(type, world);
            loaded.load(saved);
            CompoundTag next = loaded.saveWithoutId(new CompoundTag());
            assertEquals(saved.getUUID("UUID"), next.getUUID("UUID"));
            assertEquals(saved.getList("Pos", 6), next.getList("Pos", 6));
            assertEquals(motion(0, 0, 0), next.getList("Motion", 6));
            assertArrayEquals(parent, next.getIntArray("parentPos"));
            assertFalse(loaded.isPushedByFluid());
            saved = next;
        }
    }

    private static ListTag motion(double x, double y, double z) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(x)); list.add(DoubleTag.valueOf(y)); list.add(DoubleTag.valueOf(z));
        return list;
    }

    private static EntityType<HydroturbineEntity> turbineType() {
        return EntityType.Builder.<HydroturbineEntity>of(HydroturbineEntity::new, MobCategory.MISC)
                .sized(1, 1).build("electricadvantage:test_anchored_turbine");
    }

    private static EntityType<ControlEntity> controlType() {
        return EntityType.Builder.<ControlEntity>of(ControlEntity::new, MobCategory.MISC)
                .sized(1, 1).build("electricadvantage:test_native_motion_control");
    }

    private static Level world() throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field field = allocator.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Level world = (Level)allocator.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), NoTickWorld.class);
        Field random = Level.class.getField("random");
        random.setAccessible(true);
        random.set(world, new Random(0));
        return world;
    }

    private static final class NoTickWorld extends ServerLevel {
        private NoTickWorld() {
            super(null, null, null, null, null, null, null, null, false, 0L, Collections.emptyList(), false);
        }
    }

    private static final class ControlEntity extends Entity {
        ControlEntity(EntityType<?> type, Level world) { super(type, world); }
        @Override protected void defineSynchedData() {}
        @Override protected void readAdditionalSaveData(CompoundTag tag) {}
        @Override protected void addAdditionalSaveData(CompoundTag tag) {}
        @Override public Packet<?> getAddEntityPacket() { return null; }
    }
}
