package zone.moddev.mc.baseelectric.entities;

import java.lang.reflect.Field;
import java.util.Collections;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.phys.Vec3;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ElectricOperationalHydroturbineAnchorTest {
    @BeforeClass public static void bootstrap() throws ReflectiveOperationException {
        NativeTestBootstrap.registerVanillaFluidTypes();
    }

    @Test public void nativeEntityLoadStillClampsOverspeedMotionAndPreservesItsBoundary() throws Exception {
        ControlEntity control = new ControlEntity(controlType(), world());
        CompoundTag tag = save(control);
        tag.put("Motion", motion(16.226, -10.001, 10));
        control.load(NativeTestBootstrap.input(tag));
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
        CompoundTag saved = save(turbine);
        saved.put("Motion", motion(5.5, -2, 0));
        turbine.load(NativeTestBootstrap.input(saved));
        assertEquals(new Vec3(5.5, -2, 0), turbine.getDeltaMovement());
        assertFalse(turbine.isPushedByFluid());
    }

    @Test public void anchoredTurbineSurvivesNativeSaveLoadWithItsParentAndIdentity() throws Exception {
        EntityType<HydroturbineEntity> type = turbineType();
        Level world = world();
        HydroturbineEntity original = new HydroturbineEntity(type, world);
        original.setPos(56856.5, 81, 50008.5);
        CompoundTag saved = save(original);
        int[] parent = {56856, 82, 50008};
        saved.putIntArray("parentPos", parent);
        for (int pass = 0; pass < 3; pass++) {
            HydroturbineEntity loaded = new HydroturbineEntity(type, world);
            loaded.load(NativeTestBootstrap.input(saved));
            CompoundTag next = save(loaded);
            assertEquals(saved.read("UUID", net.minecraft.core.UUIDUtil.CODEC).orElseThrow(),
                    next.read("UUID", net.minecraft.core.UUIDUtil.CODEC).orElseThrow());
            assertEquals(saved.getList("Pos").filter(list -> list.stream().allMatch(entry -> entry.getId() == 6)).orElseGet(net.minecraft.nbt.ListTag::new), next.getList("Pos").filter(list -> list.stream().allMatch(entry -> entry.getId() == 6)).orElseGet(net.minecraft.nbt.ListTag::new));
            assertEquals(motion(0, 0, 0), next.getList("Motion").filter(list -> list.stream().allMatch(entry -> entry.getId() == 6)).orElseGet(net.minecraft.nbt.ListTag::new));
            assertArrayEquals(parent, next.getIntArray("parentPos").orElseThrow());
            assertFalse(loaded.isPushedByFluid());
            saved = next;
        }
    }

    private static CompoundTag save(Entity entity) {
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, NativeTestBootstrap.registries());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static ListTag motion(double x, double y, double z) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(x)); list.add(DoubleTag.valueOf(y)); list.add(DoubleTag.valueOf(z));
        return list;
    }

    private static EntityType<HydroturbineEntity> turbineType() {
        return EntityType.Builder.<HydroturbineEntity>of(HydroturbineEntity::new, MobCategory.MISC)
                .sized(1, 1).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.parse("baseelectric:test_anchored_turbine")));
    }

    private static EntityType<ControlEntity> controlType() {
        return EntityType.Builder.<ControlEntity>of(ControlEntity::new, MobCategory.MISC)
                .sized(1, 1).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.parse("baseelectric:test_native_motion_control")));
    }

    private static Level world() throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field field = allocator.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Level world = (Level)allocator.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), NoTickWorld.class);
        Field random = Level.class.getField("random");
        random.setAccessible(true);
        random.set(world, RandomSource.create(0));
        return world;
    }

    private static final class NoTickWorld extends ServerLevel {
        private NoTickWorld() {
            super(null, null, null, null, null, null, false, 0L, Collections.emptyList(), false, null);
        }
    }

    private static final class ControlEntity extends Entity {
        ControlEntity(EntityType<?> type, Level world) { super(type, world); }
        @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
        @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {}
        @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {}
        @Override public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
        @Override public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity tracker) { return null; }
    }
}
