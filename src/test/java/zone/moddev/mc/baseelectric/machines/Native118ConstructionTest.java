package zone.moddev.mc.baseelectric.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
    @BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }

    @Test public void everyFactoryPreservesItsNativePositionAndState() {
        BlockPos pos = new BlockPos(-117, 83, 4917);
        for (Block block : blocks()) {
            BlockState state = block.defaultBlockState();
            BlockEntity tile = ((EntityBlock) block).newBlockEntity(pos, state);
            assertNotNull(block.getClass().getName(), tile);
            assertEquals(pos, tile.getBlockPos());
            assertSame(state, tile.getBlockState());
            CompoundTag saved = new CompoundTag();
            saved.putInt("x", 0); saved.putInt("y", 1); saved.putInt("z", 2);
            tile.load(saved);
            assertEquals("NBT coordinates cannot move a native block entity", pos, tile.getBlockPos());
            assertSame(state, tile.getBlockState());
        }
    }

    @Test public void turretPacketUsesConstructorCoordinatesBeforeWorldAttachment() {
        BlockPos pos = new BlockPos(1531, 91, -475);
        LaserTurretBlock block = new LaserTurretBlock(false);
        BlockEntityType<LaserTurretTileEntity> type = BlockEntityType.Builder.of(LaserTurretTileEntity::new, block).build(null);
        BlockEntityType<LaserTurretTileEntity> previous = zone.moddev.mc.baseelectric.init.Entities.LASER_TURRET;
        try {
            zone.moddev.mc.baseelectric.init.Entities.LASER_TURRET = type;
            LaserTurretTileEntity turret = new LaserTurretTileEntity(pos, block.defaultBlockState());
            var packet = turret.getUpdatePacket();
            assertEquals(pos, packet.getPos());
            assertSame(type, packet.getType());
            assertEquals(java.util.Set.of("energy", "lock"), packet.getTag().getAllKeys());
            assertTrue(turret.getUpdateTag().contains("player"));
            turret.setOpticPosition();
            assertEquals(pos, turret.getBlockPos());
        } finally {
            zone.moddev.mc.baseelectric.init.Entities.LASER_TURRET = previous;
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
        return new Block[]{new ElectricFurnaceBlock(), new ElectricCrusherBlock(), new ElectricDrillBlock(),
                new ElectricFabricatorBlock(), new GrowthChamberBlock(), new GrowthChamberControllerBlock(),
                new ElectricOvenBlock(), new LaserTurretBlock(false), new LaserTurretBlock(true), new LEDBlock(),
                new HydroelectricGeneratorBlock(), new PhotovoltaicGeneratorBlock(),
                new SteamPoweredElectricGeneratorBlock(), new ElectricBatteryArrayBlock(),
                new ElectricPumpBlock(), new ElectricStillBlock(), new PlasticRefineryBlock()};
    }
}
