package zone.moddev.mc.baseelectric.machines;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;

public class ElectricDrillPersistenceTest {
    @BeforeClass public static void bootstrapNativeRegistries() { NativeTestBootstrap.initialize(); }

    @Test public void retainsLegacyProgressUntilWorldIsAttached() {
        ElectricDrillTileEntity drill = new ElectricDrillTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.ElectricDrillBlock().defaultBlockState());
        drill.loadWithComponents(target(94), NativeTestBootstrap.registries());
        drill.prepareDataFieldsForSync();
        assertEquals(94, drill.getDataFieldArray()[1]);
    }

    @Test public void clearsPreviousProgressOnReadingAnUntargetedTile() {
        ElectricDrillTileEntity drill = new ElectricDrillTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.ElectricDrillBlock().defaultBlockState());
        drill.loadWithComponents(target(94), NativeTestBootstrap.registries());
        drill.loadWithComponents(new CompoundTag(), NativeTestBootstrap.registries());
        drill.prepareDataFieldsForSync();
        assertEquals(0, drill.getDataFieldArray()[1]);
    }

    @Test public void preservesPendingTargetSnapshotAcrossRepeatedReadSave() {
        CompoundTag source = target(94);
        CompoundTag state = new CompoundTag();
        state.putString("Name", "minecraft:stone");
        ListTag states = new ListTag();
        for (int i = 0; i < 9; i++) states.add(state.copy());
        source.put("TargetStates", states);
        for (int pass = 0; pass < 3; pass++) {
            ElectricDrillTileEntity drill = new ElectricDrillTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.ElectricDrillBlock().defaultBlockState());
            drill.loadFrom(source);
            CompoundTag saved = new CompoundTag();
            drill.saveTo(saved);
            assertEquals(source, saved);
            source = saved;
        }
    }

    @Test public void targetPlaneRemainsNineUniqueBlocksForEveryDrillDirection() throws Exception {
        ElectricDrillTileEntity drill = new ElectricDrillTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.ElectricDrillBlock().defaultBlockState());
        Field facing = ElectricDrillTileEntity.class.getDeclaredField("facingCache");
        facing.setAccessible(true);
        Method area = ElectricDrillTileEntity.class.getDeclaredMethod("getArea", BlockPos.class);
        area.setAccessible(true);
        BlockPos center = new BlockPos(17, 80, 21);
        for (Direction direction : Direction.values()) {
            facing.set(drill, direction);
            BlockPos[] targets = (BlockPos[]) area.invoke(drill, center);
            Set<BlockPos> unique = new HashSet<>();
            assertEquals(center, targets[0]);
            for (BlockPos target : targets) {
                unique.add(target);
                assertEquals(0, (target.getX() - center.getX()) * direction.getStepX()
                        + (target.getY() - center.getY()) * direction.getStepY()
                        + (target.getZ() - center.getZ()) * direction.getStepZ());
            }
            assertEquals(direction.toString(), 9, unique.size());
        }
    }

    @Test public void nativeDataFieldsRetainEnergyAndMiningProgress() {
        ElectricDrillTileEntity drill = new ElectricDrillTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.ElectricDrillBlock().defaultBlockState());
        drill.loadWithComponents(target(94), NativeTestBootstrap.registries());
        drill.setEnergy(416.0F, drill.getEnergyType());
        drill.prepareDataFieldsForSync();
        assertEquals(416.0F, Float.intBitsToFloat(drill.getDataFieldArray()[0]), 0.0F);
        assertEquals(94, drill.getDataFieldArray()[1]);
        assertEquals(250.0F, ElectricDrillTileEntity.ENERGY_COST_MOVE, 0.0F);
        assertEquals(24.0F, ElectricDrillTileEntity.ENERGY_COST_PROGRESS_TICK, 0.0F);
        assertEquals(6, drill.numberOfOutputSlots());

        // Mod inventories are not visited by vanilla's block-entity item-stack walker.
        CompoundTag oldStack = new CompoundTag();
        oldStack.putByte("Slot", (byte) 0);
        oldStack.putString("id", "minecraft:diamond");
        oldStack.putByte("Count", (byte) 3);
        CompoundTag display = new CompoundTag();
        display.putString("Name", "{\"text\":\"Track transfer payload\"}");
        CompoundTag oldData = new CompoundTag();
        oldData.put("display", display);
        oldStack.put("tag", oldData);
        ListTag oldItems = new ListTag();
        oldItems.add(oldStack);
        CompoundTag legacy = target(94);
        legacy.put("Items", oldItems);
        drill.loadWithComponents(legacy, NativeTestBootstrap.registries());
        drill.setEnergy(416.0F, drill.getEnergyType());
        ItemStack payload = drill.getOutputSlot(0);
        assertEquals(Items.DIAMOND, payload.getItem());
        assertEquals(3, payload.getCount());
        assertEquals(Component.literal("Track transfer payload"), payload.get(DataComponents.CUSTOM_NAME));
        CompoundTag transfer = drill.saveWithoutMetadata(NativeTestBootstrap.registries());
        assertEquals(94, transfer.getShort("progress"));
        ElectricDrillTileEntity placed = new ElectricDrillTileEntity(new BlockPos(5, 70, 9), drill.getBlockState());
        placed.loadWithComponents(transfer, NativeTestBootstrap.registries());
        assertEquals(416.0F, placed.getEnergy(placed.getEnergyType()), 0.0F);
        assertEquals(new BlockPos(5, 70, 9), placed.getBlockPos());
        assertEquals(3, placed.getOutputSlot(0).getCount());
        org.junit.Assert.assertTrue(ItemStack.isSameItemSameComponents(payload, placed.getOutputSlot(0)));
    }

    private static CompoundTag target(int progress) {
        CompoundTag tag = new CompoundTag();
        tag.putShort("progress", (short) progress);
        tag.putInt("targetX", 2048);
        tag.putInt("targetY", 80);
        tag.putInt("targetZ", 2049);
        return tag;
    }
}
