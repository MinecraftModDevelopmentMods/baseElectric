package zone.moddev.mc.baseelectric.gametest;

import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.init.Blocks;
import zone.moddev.mc.baseelectric.init.Items;
import zone.moddev.mc.baseelectric.init.Power;
import zone.moddev.mc.baseelectric.items.BatteryItem;
import zone.moddev.mc.baseelectric.machines.ElectricDrillTileEntity;
import zone.moddev.mc.baseelectric.machines.LaserTurretTileEntity;
import zone.moddev.mc.baseelectric.machines.ElectricCrusherTileEntity;
import zone.moddev.mc.baseelectric.compat.BaseMetalsCompat;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.ModList;

@GameTestHolder(ElectricAdvantage.MODID)
@PrefixGameTestTemplate(false)
public final class ElectricGameTests {
    private ElectricGameTests() {}

    @GameTest(batch = ElectricAdvantage.MODID, template = "empty")
    public static void exactNativeModsAreLoaded(GameTestHelper helper) {
        Map<String, String> expected = Map.of(
                "baseelectric", "2.2.20.121012",
                "baseelectric_tests", "1.0.0",
                "basepower", "2.4.19.121012",
                "basemetals", "4.0.1.121012+Neo",
                "mmdlib", "2.0.2.121012+Neo",
                "orespawn", "4.1.1.121012",
                "minecraft", "1.21.1",
                "neoforge", "21.1.265");
        Map<String, String> actual = new TreeMap<>();
        ModList.get().getMods().forEach(mod -> actual.put(mod.getModId(), mod.getVersion().toString()));
        helper.assertTrue(actual.equals(expected), "Wrong native GameTest mod set: " + actual);
        com.mojang.logging.LogUtils.getLogger().info("ELECTRIC_GAMETEST_LOADED_MODS {}", actual);
        helper.succeed();
    }

    @GameTest(batch = "baseelectric_shared_crushing", template = "empty")
    public static void sharedCrusherHonorsRecipeRemovalAndCopiesOutputs(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var recipes = java.util.List.copyOf(manager.getRecipes());
        ItemStack input = new ItemStack(net.minecraft.world.item.Items.STONE);
        var first = BaseMetalsCompat.getCrusherRecipeOutput(level, input);
        helper.assertTrue(first != null && first.is(net.minecraft.world.item.Items.COBBLESTONE),
                "The shared vanilla stone recipe is missing");
        first.setCount(63);
        var second = BaseMetalsCompat.getCrusherRecipeOutput(level, input);
        helper.assertTrue(second != null && second.getCount() == 1,
                "Crusher output aliases the recipe result");
        helper.assertTrue(input.getCount() == 1, "Recipe lookup changed its input");
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.rock_crusher);
        var crusher = (ElectricCrusherTileEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        crusher.setInputSlot(0, input.copy());
        for (int tick = 0; tick < 200; tick++) {
            crusher.setEnergy(500.0F, Power.ELECTRIC_POWER);
            crusher.tickUpdate(true);
        }
        helper.assertTrue(crusher.getInputSlot(0) == null
                && crusher.getOutputSlot(0) != null
                && crusher.getOutputSlot(0).is(net.minecraft.world.item.Items.COBBLESTONE),
                "The machine did not process the shared recipe with its existing timing");
        try {
            manager.replaceRecipes(recipes.stream().filter(holder -> !(holder.value()
                    instanceof zone.moddev.mc.lib.common.recipes.CrushingRecipe)).toList());
            helper.assertTrue(BaseMetalsCompat.getCrusherRecipeOutput(level, input) == null,
                    "A hard-coded fallback bypassed recipe deletion");
            helper.assertTrue(BaseMetalsCompat.getCrusherRecipeOutput(level,
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()) == null,
                    "The block-state lookup bypassed recipe deletion");
            crusher.setInputSlot(0, input.copy());
            crusher.setEnergy(500.0F, Power.ELECTRIC_POWER);
            for (int tick = 0; tick < 201; tick++) crusher.tickUpdate(true);
            helper.assertTrue(crusher.getInputSlot(0).getCount() == 1
                    && crusher.getEnergy(Power.ELECTRIC_POWER) == 500.0F,
                    "A removed recipe consumed input or energy");
        } finally {
            manager.replaceRecipes(recipes);
        }
        helper.assertTrue(BaseMetalsCompat.hasCrusherRecipe(level, input), "Restored recipe was not reloaded");
        helper.succeed();
    }

    @GameTest(batch = ElectricAdvantage.MODID, template = "empty")
    public static void nativeSidedCapabilitiesExposeElectricMachines(GameTestHelper helper) {
        BlockPos pump = new BlockPos(1, 1, 1);
        BlockPos crusher = new BlockPos(2, 1, 1);
        helper.setBlock(pump, Blocks.electric_pump);
        helper.setBlock(crusher, Blocks.rock_crusher);
        var level = helper.getLevel();
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(pump), Direction.UP) != null, "Pump fluid capability missing");
        helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(crusher), Direction.NORTH) != null, "Crusher item capability missing");
        helper.succeed();
    }

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
        int crushing = 0;
        var registries = helper.getLevel().registryAccess();
        for (var holder : server.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(ElectricAdvantage.MODID)) continue;
            if (holder.value() instanceof zone.moddev.mc.lib.common.recipes.CrushingRecipe) crushing++;
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
        int expectedCrushing = zone.moddev.mc.basepower.PowerAdvantage.recipeMode
                == zone.moddev.mc.basepower.RecipeMode.APOCALYPTIC ? 8 : 3;
        helper.assertTrue(crushing == expectedCrushing,
                "Wrong Electric MMDLib crushing recipe count for the selected mode: " + crushing);
        helper.succeed();
    }

    @GameTest(batch = ElectricAdvantage.MODID, template = "empty")
    public static void siliconAndSolderResolveRealMaterialTags(GameTestHelper helper) {
        assertLoadedRecipe(helper, "silicon_blend", Items.silicon_blend, 1);
        assertLoadedRecipe(helper, "silicon_ingot_from_blend", Items.silicon_ingot, 1);
        assertLoadedRecipe(helper, "solder_blend_tin_lead", Items.solder_blend, 3);
        assertLoadedRecipe(helper, "solder_blend_tin_silver", Items.solder_blend, 3);
        assertLoadedRecipe(helper, "solder_from_blend", Items.solder, 1);
        helper.succeed();
    }

    private static void assertLoadedRecipe(GameTestHelper helper, String path, Item item, int count) {
        var id = ResourceLocation.fromNamespaceAndPath(ElectricAdvantage.MODID, path);
        var level = helper.getLevel();
        var holder = level.getRecipeManager().byKey(id);
        helper.assertTrue(holder.isPresent(), "Recipe was gated despite valid materials: " + id);
        var recipe = holder.orElseThrow().value();
        helper.assertTrue(recipe.getIngredients().stream().allMatch(ingredient -> ingredient.getItems().length > 0),
                "Recipe loaded with an empty ingredient: " + id);
        helper.assertTrue(ItemStack.matches(new ItemStack(item, count), recipe.getResultItem(level.registryAccess())),
                "Unexpected output for " + id);
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
