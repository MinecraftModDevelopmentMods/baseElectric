package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.verification.NativeMachineFixture;

import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;

public class GrowthChamberPersistenceTest {
    @org.junit.Rule public final NativeMachineFixture machines =
            new NativeMachineFixture();
    @org.junit.BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }
    @Test public void legacyGrowthAndProgressSurviveReadSaveBeforeWorldAttachment() {
        CompoundTag source = new CompoundTag();
        source.putByteArray("growth", new byte[] {2, 4, 6});
        source.putIntArray("prog", new int[] {245, 468, 627});
        for (int pass = 0; pass < 3; pass++) {
            GrowthChamberTileEntity chamber = new GrowthChamberTileEntity(net.minecraft.core.BlockPos.ZERO, NativeMachineFixture.state(GrowthChamberBlock.class));
            chamber.loadWithComponents(NativeTestBootstrap.input(source));
            CompoundTag saved = chamber.saveWithoutMetadata(NativeTestBootstrap.registries());
            assertArrayEquals(new byte[] {2, 4, 6}, saved.getByteArray("growth").orElseThrow());
            assertArrayEquals(new int[] {245, 468, 627}, saved.getIntArray("prog").orElseThrow());
            source = saved;
        }
    }

    @Test public void shortArraysArePaddedAndNewNbtClearsOldPendingWork() {
        GrowthChamberTileEntity chamber = new GrowthChamberTileEntity(net.minecraft.core.BlockPos.ZERO, NativeMachineFixture.state(GrowthChamberBlock.class));
        CompoundTag source = new CompoundTag();
        source.putByteArray("growth", new byte[] {2});
        source.putIntArray("prog", new int[] {245});
        chamber.loadFrom(source);
        CompoundTag saved = new CompoundTag();
        chamber.saveTo(saved);
        assertArrayEquals(new byte[] {2, 0, 0}, saved.getByteArray("growth").orElseThrow());
        assertArrayEquals(new int[] {245, 0, 0}, saved.getIntArray("prog").orElseThrow());
        chamber.loadFrom(new CompoundTag());
        chamber.saveTo(saved);
        assertArrayEquals(new byte[] {0, 0, 0}, saved.getByteArray("growth").orElseThrow());
        assertArrayEquals(new int[] {0, 0, 0}, saved.getIntArray("prog").orElseThrow());
    }
}
