package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.verification.NativeMachineFixture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class Native118ConstructionTest {
    @org.junit.Rule public final NativeMachineFixture machines =
            new NativeMachineFixture();
    @BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }

    @Test public void everyFactoryPreservesItsNativePositionAndState() {
        BlockPos pos = new BlockPos(-117, 83, 4917);
        assertEquals(17, blocks().length);
        for (Block block : blocks()) {
            BlockState state = block.defaultBlockState();
            BlockEntity tile = ((EntityBlock) block).newBlockEntity(pos, state);
            assertNotNull(block.getClass().getName(), tile);
            assertEquals(pos, tile.getBlockPos());
            assertSame(state, tile.getBlockState());
            assertTrue(tile.getType().isValid(state));
            CompoundTag saved = new CompoundTag();
            saved.putInt("x", 0); saved.putInt("y", 1); saved.putInt("z", 2);
            tile.loadWithComponents(saved, NativeTestBootstrap.registries());
            assertEquals("NBT coordinates cannot move a native block entity", pos, tile.getBlockPos());
            assertSame(state, tile.getBlockState());
        }
    }

    @Test public void turretPacketUsesConstructorCoordinatesBeforeWorldAttachment() throws Exception {
        BlockPos pos = new BlockPos(1531, 91, -475);
        LaserTurretBlock block = new LaserTurretBlock(false);
        BlockEntityType<LaserTurretTileEntity> type = BlockEntityType.Builder.of(LaserTurretTileEntity::new, block).build(null);
        BlockEntityType<LaserTurretTileEntity> previous = zone.moddev.mc.baseelectric.init.Entities.LASER_TURRET;
        try {
            zone.moddev.mc.baseelectric.init.Entities.LASER_TURRET = type;
            LaserTurretTileEntity turret = new LaserTurretTileEntity(pos, block.defaultBlockState());
            assertNull("The native packet factory needs a level for registry access", turret.getUpdatePacket());
            assertTrue(turret.getUpdateTag(NativeTestBootstrap.registries()).contains("player"));
            turret.setOpticPosition();
            assertEquals(pos, turret.getBlockPos());

            Class<?> allocator = Class.forName("sun.misc.Unsafe");
            var unsafe = allocator.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            PacketWorld level = (PacketWorld) allocator.getMethod("allocateInstance", Class.class)
                    .invoke(unsafe.get(null), PacketWorld.class);
            turret.setLevel(level);
            var packet = turret.getUpdatePacket();
            assertEquals(pos, packet.getPos());
            assertSame(type, packet.getType());
            assertEquals(java.util.Set.of("energy", "lock"), packet.getTag().getAllKeys());
        } finally {
            zone.moddev.mc.baseelectric.init.Entities.LASER_TURRET = previous;
        }
    }

    private static final class PacketWorld extends ServerLevel {
        private PacketWorld() {
            super(null, null, null, null, null, null, null, false, 0L, java.util.List.of(), false, null);
        }

        @Override public RegistryAccess registryAccess() {
            return NativeTestBootstrap.registries();
        }
    }

    @Test public void ledOnlyOffersItsOwnNativeTicker() {
        LEDBlock block = new LEDBlock();
        BlockEntityType<LEDTileEntity> type = BlockEntityType.Builder.of(LEDTileEntity::new, block).build(null);
        BlockEntityType<LEDTileEntity> previous = zone.moddev.mc.baseelectric.init.Entities.LED;
        try {
            zone.moddev.mc.baseelectric.init.Entities.LED = type;
            assertNotNull(block.getTicker(null, block.defaultBlockState(), type));
            assertNull(block.getTicker(null, block.defaultBlockState(), BlockEntityType.CHEST));
        } finally {
            zone.moddev.mc.baseelectric.init.Entities.LED = previous;
        }
    }

    private static Block[] blocks() {
        return NativeMachineFixture.blocks();
    }
}
