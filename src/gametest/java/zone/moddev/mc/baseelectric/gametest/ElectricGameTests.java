package zone.moddev.mc.baseelectric.gametest;

import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestHolder;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.init.Blocks;
import zone.moddev.mc.baseelectric.init.Items;
import zone.moddev.mc.baseelectric.init.Power;
import zone.moddev.mc.baseelectric.items.BatteryItem;
import zone.moddev.mc.baseelectric.machines.ElectricDrillTileEntity;
import zone.moddev.mc.baseelectric.machines.LaserTurretTileEntity;

@GameTestHolder(value = ElectricAdvantage.MODID, namespace = ElectricAdvantage.MODID)
@GameTestDontPrefix
public final class ElectricGameTests {
    private ElectricGameTests() {}

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());
        NbtIo.writeCompressed(NbtUtils.snbtToStructure(Files.readString(Path.of(args[0]))), output);
    }

    @GameTest(batch = ElectricAdvantage.MODID, template = "empty")
    public static void drillUpgradesAndTransfersLegacyBattery(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 1);
        BlockPos destination = source.east();
        helper.setBlock(source, Blocks.laser_drill);
        helper.setBlock(destination, Blocks.laser_drill);
        var drill = (ElectricDrillTileEntity) helper.getBlockEntity(source);
        var relocated = (ElectricDrillTileEntity) helper.getBlockEntity(destination);
        var registries = helper.getLevel().registryAccess();
        CompoundTag batteryData = new CompoundTag();
        batteryData.putFloat("energy", 0.0F);
        batteryData.putString("marker", "old-drill-slot");
        CompoundTag oldStack = new CompoundTag();
        oldStack.putString("id", "baseelectric:lead_acid_battery");
        oldStack.putByte("Slot", (byte) 0);
        oldStack.putByte("Count", (byte) 1);
        oldStack.put("tag", batteryData);
        ListTag slots = new ListTag();
        slots.add(oldStack);
        CompoundTag oldInventory = new CompoundTag();
        oldInventory.put("Items", slots);
        drill.loadWithComponents(oldInventory, registries);
        ItemStack stack = drill.getOutputSlot(0);
        var battery = (BatteryItem) Items.lead_acid_battery;
        helper.assertTrue(stack != null && stack.is(battery), "The legacy slot did not load as a battery");
        helper.assertTrue(battery.getEnergy(stack) == 0.0F, "The depleted battery became charged");
        helper.assertTrue(battery.addEnergy(stack, 25.0F, Power.ELECTRIC_POWER) == 25.0F,
                "The loaded battery cannot receive charge");
        relocated.loadWithComponents(drill.saveWithoutMetadata(registries), registries);
        ItemStack moved = relocated.getOutputSlot(0);
        helper.assertTrue(moved != null && ItemStack.matches(stack, moved),
                "Drill transfer did not preserve the battery components");
        helper.assertTrue("old-drill-slot".equals(moved.get(DataComponents.CUSTOM_DATA).copyTag().getString("marker")),
                "Charging the battery erased unrelated data");
        helper.assertTrue(relocated.getBlockPos().equals(helper.absolutePos(destination)),
                "Drill transfer changed the destination position");
        helper.succeed();
    }

    @GameTest(batch = ElectricAdvantage.MODID, template = "empty")
    public static void recipeAvailabilityDoesNotRemoveDiscovery(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var advancement = ResourceLocation.fromNamespaceAndPath(ElectricAdvantage.MODID, "recipes/alkaline_battery");
        helper.assertTrue(server.getAdvancements().get(advancement) != null,
                "A material-gated recipe lost its discovery advancement");
        int checked = 0;
        var registries = helper.getLevel().registryAccess();
        for (var holder : server.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(ElectricAdvantage.MODID)) continue;
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                Recipe.STREAM_CODEC.encode(buffer, holder.value());
                var decoded = Recipe.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.getSerializer() == holder.value().getSerializer()
                        && ItemStack.matches(decoded.getResultItem(registries), holder.value().getResultItem(registries)),
                        "Electric recipe changed over the native network codec: " + holder.id());
            } finally {
                buffer.release();
            }
            checked++;
        }
        helper.assertTrue(checked > 0, "No Electric recipes were present for the network check");
        helper.succeed();
    }

    @GameTest(batch = ElectricAdvantage.MODID, template = "empty")
    public static void turretUsesNativeUpdatePacket(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, Blocks.laser_turret);
        var turret = (LaserTurretTileEntity) helper.getBlockEntity(relative);
        var packet = turret.getUpdatePacket();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ClientboundBlockEntityDataPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = ClientboundBlockEntityDataPacket.STREAM_CODEC.decode(buffer);
            helper.assertTrue(decoded.getPos().equals(helper.absolutePos(relative))
                    && decoded.getType() == turret.getType(), "The turret packet changed its native target");
            helper.assertTrue(decoded.getTag().contains("lock") && decoded.getTag().contains("energy"),
                    "The turret packet lost its render state");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
