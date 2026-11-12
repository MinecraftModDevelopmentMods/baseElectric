package zone.moddev.mc.baseelectric.gametest;

import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraftforge.gametest.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestNamespace;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.init.Blocks;
import zone.moddev.mc.baseelectric.init.Items;
import zone.moddev.mc.baseelectric.init.Power;
import zone.moddev.mc.baseelectric.items.BatteryItem;
import zone.moddev.mc.baseelectric.machines.ElectricDrillTileEntity;
import zone.moddev.mc.baseelectric.machines.LaserTurretTileEntity;

@GameTestNamespace(ElectricAdvantage.MODID)
@GameTestDontPrefix
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = ElectricAdvantage.MODID,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class ElectricGameTests {
    private ElectricGameTests() {}

    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void registerFunctions(net.minecraftforge.registries.RegisterEvent event) {
        if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.TEST_FUNCTION)) {
            recordRuntimeBoundary();
        }
        event.register(net.minecraft.core.registries.Registries.TEST_FUNCTION, registry -> {
            registry.register(Identifier.parse("baseelectric:drillupgradesandtransferslegacybattery"),
                    ElectricGameTests::drillUpgradesAndTransfersLegacyBattery);
            registry.register(Identifier.parse("baseelectric:recipeavailabilitydoesnotremovediscovery"),
                    ElectricGameTests::recipeAvailabilityDoesNotRemoveDiscovery);
            registry.register(Identifier.parse("baseelectric:siliconandsolderresolverealmaterialtags"),
                    ElectricGameTests::siliconAndSolderResolveRealMaterialTags);
            registry.register(Identifier.parse("baseelectric:turretusesnativeupdatepacket"),
                    ElectricGameTests::turretUsesNativeUpdatePacket);
        });
    }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());
        NbtIo.writeCompressed(NbtUtils.snbtToStructure(Files.readString(Path.of(args[0]))), output);
    }

    private static void recordRuntimeBoundary() {
        var arguments = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String name : java.util.List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) {
            if (System.getenv(name) != null) throw new AssertionError("Inherited native GameTest JVM options: " + name);
        }
        if (!arguments.contains("-XX:ActiveProcessorCount=4") || Runtime.getRuntime().availableProcessors() != 4) {
            throw new AssertionError("Native GameTests require an explicit four-CPU JVM argument");
        }
        String destination = System.getProperty("baseelectric.gameTestRuntimeReceipt");
        if (destination == null) throw new AssertionError("Native GameTest runtime receipt path is missing");
        var result = new com.google.gson.JsonObject();
        result.addProperty("scope", "SOURCE_GAMETEST_NOT_FROZEN_PLAYER_JAR");
        result.addProperty("javaRuntimeVersion", System.getProperty("java.runtime.version"));
        result.addProperty("availableProcessors", Runtime.getRuntime().availableProcessors());
        result.addProperty("environmentJvmOptionsAbsent", true);
        result.addProperty("classpath", System.getProperty("java.class.path"));
        result.addProperty("electricCodeSource", ElectricAdvantage.class.getProtectionDomain().getCodeSource().getLocation().toString());
        var inputs = new com.google.gson.JsonArray();
        arguments.forEach(inputs::add);
        result.add("jvmArguments", inputs);
        try {
            Files.writeString(Path.of(destination), result.toString(), java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (java.io.IOException failure) {
            throw new AssertionError("Cannot preserve native GameTest runtime receipt", failure);
        }
    }

    @GameTest(name = "drillupgradesandtransferslegacybattery", structure = "empty")
    public static void drillUpgradesAndTransfersLegacyBattery(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 1);
        BlockPos destination = source.east();
        helper.setBlock(source, Blocks.laser_drill);
        helper.setBlock(destination, Blocks.laser_drill);
        var drill = helper.getBlockEntity(source, ElectricDrillTileEntity.class);
        var relocated = helper.getBlockEntity(destination, ElectricDrillTileEntity.class);
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
        load(drill, oldInventory, registries);
        ItemStack stack = drill.getOutputSlot(0);
        var battery = (BatteryItem) Items.lead_acid_battery;
        helper.assertTrue(stack != null && stack.is(battery), "The legacy slot did not load as a battery");
        helper.assertTrue(battery.getEnergy(stack) == 0.0F, "The depleted battery became charged");
        helper.assertTrue(battery.addEnergy(stack, 25.0F, Power.ELECTRIC_POWER) == 25.0F,
                "The loaded battery cannot receive charge");
        load(relocated, drill.saveWithoutMetadata(registries), registries);
        ItemStack moved = relocated.getOutputSlot(0);
        helper.assertTrue(moved != null && ItemStack.matches(stack, moved),
                "Drill transfer did not preserve the battery components");
        helper.assertTrue("old-drill-slot".equals(moved.get(DataComponents.CUSTOM_DATA).copyTag().getString("marker").orElseThrow()),
                "Charging the battery erased unrelated data");
        helper.assertTrue(relocated.getBlockPos().equals(helper.absolutePos(destination)),
                "Drill transfer changed the destination position");
        helper.succeed();
    }

    @GameTest(name = "recipeavailabilitydoesnotremovediscovery", structure = "empty")
    public static void recipeAvailabilityDoesNotRemoveDiscovery(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var itemIds = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys().stream()
                .filter(id -> id.getNamespace().equals(ElectricAdvantage.MODID))
                .map(Object::toString).sorted().toList();
        helper.assertTrue(!itemIds.contains("baseelectric:battery_base"), "Abstract battery parent became a registered item");
        for (String battery : java.util.List.of("alkaline_battery", "lead_acid_battery", "lithium_battery", "nickel_hydride_battery")) {
            helper.assertTrue(itemIds.contains("baseelectric:" + battery), "Concrete battery is not registered: " + battery);
        }
        var census = new com.google.gson.JsonObject();
        census.addProperty("scope", "ACTUAL_NATIVE_COMMON_ITEM_REGISTRY_SOURCE_GAMETEST_NOT_CLIENT_PIXELS");
        census.addProperty("abstractBatteryRegistered", false);
        var registryEntries = new com.google.gson.JsonArray();
        itemIds.forEach(registryEntries::add);
        census.add("registeredElectricItems", registryEntries);
        try {
            Files.writeString(Path.of(System.getProperty("baseelectric.gameTestRuntimeReceipt"))
                    .resolveSibling("electric-item-registry-census.json"), census.toString(),
                    java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (java.io.IOException failure) {
            throw new AssertionError("Cannot retain actual item registry census", failure);
        }
        var advancement = Identifier.fromNamespaceAndPath(ElectricAdvantage.MODID, "recipes/alkaline_battery");
        helper.assertTrue(server.getAdvancements().get(advancement) != null,
                "A material-gated recipe lost its discovery advancement");
        int checked = 0;
        var registries = helper.getLevel().registryAccess();
        var entries = new java.util.ArrayList<net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket.Entry>();
        for (var holder : server.getRecipeManager().getRecipes()) {
            if (!holder.id().identifier().getNamespace().equals(ElectricAdvantage.MODID)) continue;
            int before = entries.size();
            server.getRecipeManager().listDisplaysForRecipe(holder.id(), display -> entries.add(
                    new net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket.Entry(
                            display, holder.value().showNotification(), true)));
            helper.assertTrue(entries.size() - before == holder.value().display().size(),
                    "Electric recipe lost its native client displays: " + holder.id());
            checked++;
        }
        helper.assertTrue(checked > 0, "No Electric recipes were present for the network check");
        helper.assertTrue(!entries.isEmpty(), "No Electric recipe-book entries were present");
        var packet = new net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket(entries, true);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        RegistryFriendlyByteBuf roundTrip = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket.STREAM_CODEC.encode(buffer, packet);
            byte[] originalBytes = io.netty.buffer.ByteBufUtil.getBytes(buffer);
            var decoded = net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket.STREAM_CODEC.decode(buffer);
            net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket.STREAM_CODEC.encode(roundTrip, decoded);
            helper.assertTrue(decoded.replace() && decoded.entries().size() == entries.size()
                            && java.util.Arrays.equals(originalBytes, io.netty.buffer.ByteBufUtil.getBytes(roundTrip)),
                    "Electric recipe displays changed over the actual native recipe-book packet");
        } finally {
            buffer.release();
            roundTrip.release();
        }
        assertEveryPackagedRecipeUsesTheNativeCodec(helper);
        assertEveryPackagedLootTableUsesTheNativeCodec(helper);
        helper.succeed();
    }

    private static void assertEveryPackagedRecipeUsesTheNativeCodec(GameTestHelper helper) {
        var resources = helper.getLevel().getServer().getResourceManager().listResources("recipe",
                id -> id.getNamespace().equals(ElectricAdvantage.MODID) && id.getPath().endsWith(".json"));
        helper.assertTrue(resources.size() == 58, "Expected all 58 packaged Electric recipes, found " + resources.size());
        var ops = helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        for (var entry : resources.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var json = com.google.gson.JsonParser.parseReader(reader);
                var decoded = Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow();
                var encoded = Recipe.DIRECT_CODEC.encodeStart(ops, decoded).getOrThrow();
                var again = Recipe.DIRECT_CODEC.parse(ops, encoded).getOrThrow();
                helper.assertTrue(decoded.getSerializer() == again.getSerializer()
                                && encoded.equals(Recipe.DIRECT_CODEC.encodeStart(ops, again).getOrThrow()),
                        "Packaged recipe changed over its native codec: " + entry.getKey());
            } catch (Exception failure) {
                throw new AssertionError("Native recipe codec rejected " + entry.getKey(), failure);
            }
        }
    }

    private static void assertEveryPackagedLootTableUsesTheNativeCodec(GameTestHelper helper) {
        var resources = helper.getLevel().getServer().getResourceManager().listResources("loot_table",
                id -> id.getNamespace().equals(ElectricAdvantage.MODID) && id.getPath().endsWith(".json"));
        helper.assertTrue(resources.size() == 21, "Expected all 21 packaged Electric loot tables, found " + resources.size());
        var ops = helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var codec = net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC;
        for (var entry : resources.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                var decoded = codec.parse(ops, json).getOrThrow();
                var encoded = codec.encodeStart(ops, decoded).getOrThrow().getAsJsonObject();
                var condition = json.getAsJsonArray("pools").get(0).getAsJsonObject().get("condition");
                if (json.has("type") && json.get("type").getAsString().equals("minecraft:block")) {
                    helper.assertTrue(condition != null && condition.equals(
                            encoded.getAsJsonArray("pools").get(0).getAsJsonObject().get("condition")),
                            "Packaged explosion condition was lost: " + entry.getKey());
                } else {
                    var before = json.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
                    var after = encoded.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
                    helper.assertTrue(before.size() == 10 && after.size() == 10, "Chest entry inventory changed");
                    for (int i = 1; i < before.size(); i++) {
                        helper.assertTrue(before.get(i).getAsJsonObject().get("modifier").equals(
                                after.get(i).getAsJsonObject().get("modifier")), "Packaged chest count was lost: " + i);
                        var wire = after.get(i).getAsJsonObject();
                        int weight = wire.has("weight") ? wire.get("weight").getAsInt()
                                : net.minecraft.world.level.storage.loot.entries.UniformContainerBase.DEFAULT_WEIGHT;
                        helper.assertTrue(before.get(i).getAsJsonObject().get("weight").getAsInt() == weight,
                                "Packaged chest weight changed: " + i);
                    }
                }
            } catch (Exception failure) {
                throw new AssertionError("Native loot codec rejected registered Electric drops: " + entry.getKey(), failure);
            }
        }
    }

    @GameTest(name = "siliconandsolderresolverealmaterialtags", structure = "empty")
    public static void siliconAndSolderResolveRealMaterialTags(GameTestHelper helper) {
        assertLoadedRecipe(helper, "silicon_blend", Items.silicon_blend, 1);
        assertLoadedRecipe(helper, "silicon_ingot_from_blend", Items.silicon_ingot, 1);
        assertLoadedRecipe(helper, "solder_blend_tin_lead", Items.solder_blend, 3);
        assertLoadedRecipe(helper, "solder_blend_tin_silver", Items.solder_blend, 3);
        assertLoadedRecipe(helper, "solder_from_blend", Items.solder, 1);
        helper.succeed();
    }

    private static void assertLoadedRecipe(GameTestHelper helper, String path, Item item, int count) {
        var id = Identifier.fromNamespaceAndPath(ElectricAdvantage.MODID, path);
        var level = helper.getLevel();
        var holder = level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id));
        helper.assertTrue(holder.isPresent(), "Recipe was gated despite valid materials: " + id);
        var recipe = holder.orElseThrow().value();
        helper.assertTrue(!recipe.placementInfo().ingredients().isEmpty()
                        && recipe.placementInfo().ingredients().stream().allMatch(ingredient -> !ingredient.isEmpty()),
                "Recipe loaded with an empty ingredient: " + id);
        var encoded = Recipe.DIRECT_CODEC.encodeStart(level.registryAccess().createSerializationContext(
                com.mojang.serialization.JsonOps.INSTANCE), recipe).getOrThrow().getAsJsonObject();
        var result = ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(
                com.mojang.serialization.JsonOps.INSTANCE), encoded.get("result")).getOrThrow();
        helper.assertTrue(ItemStack.matches(new ItemStack(item, count), result),
                "Unexpected output for " + id);
    }

    @GameTest(name = "turretusesnativeupdatepacket", structure = "empty")
    public static void turretUsesNativeUpdatePacket(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, Blocks.laser_turret);
        var turret = helper.getBlockEntity(relative, LaserTurretTileEntity.class);
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

    private static void load(net.minecraft.world.level.block.entity.BlockEntity tile, CompoundTag data,
            net.minecraft.core.HolderLookup.Provider registries) {
        try (var problems = new net.minecraft.util.ProblemReporter.ScopedCollector(org.slf4j.LoggerFactory.getLogger("baseelectric-gametest"))) {
            tile.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(problems, registries, data));
        }
    }
}
