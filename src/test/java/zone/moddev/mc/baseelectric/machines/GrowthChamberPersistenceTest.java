package zone.moddev.mc.baseelectric.machines;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;

public class GrowthChamberPersistenceTest {
    @org.junit.BeforeClass public static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }
    @Test public void legacyGrowthAndProgressSurviveReadSaveBeforeWorldAttachment() {
        CompoundTag source = new CompoundTag();
        source.putByteArray("growth", new byte[] {2, 4, 6});
        source.putIntArray("prog", new int[] {245, 468, 627});
        for (int pass = 0; pass < 3; pass++) {
            GrowthChamberTileEntity chamber = new GrowthChamberTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.GrowthChamberBlock().defaultBlockState());
            chamber.loadFrom(source);
            CompoundTag saved = new CompoundTag();
            chamber.saveTo(saved);
            assertArrayEquals(new byte[] {2, 4, 6}, saved.getByteArray("growth"));
            assertArrayEquals(new int[] {245, 468, 627}, saved.getIntArray("prog"));
            source = saved;
        }
    }

    @Test public void shortArraysArePaddedAndNewNbtClearsOldPendingWork() {
        GrowthChamberTileEntity chamber = new GrowthChamberTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.GrowthChamberBlock().defaultBlockState());
        CompoundTag source = new CompoundTag();
        source.putByteArray("growth", new byte[] {2});
        source.putIntArray("prog", new int[] {245});
        chamber.loadFrom(source);
        CompoundTag saved = new CompoundTag();
        chamber.saveTo(saved);
        assertArrayEquals(new byte[] {2, 0, 0}, saved.getByteArray("growth"));
        assertArrayEquals(new int[] {245, 0, 0}, saved.getIntArray("prog"));
        chamber.loadFrom(new CompoundTag());
        chamber.saveTo(saved);
        assertArrayEquals(new byte[] {0, 0, 0}, saved.getByteArray("growth"));
        assertArrayEquals(new int[] {0, 0, 0}, saved.getIntArray("prog"));
    }
}
