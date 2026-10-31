package zone.moddev.mc.electricadvantage.blocks;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraftforge.common.ForgeHooks;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StaticTags;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.tags.TagContainer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class OreHarvestContractTest {
    @BeforeClass public static void bootstrapNativeRegistries() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        StaticTags.resetAllToEmpty();
        SerializationTags.bind(TagContainer.EMPTY);
        // A plain JVM does not run Forge's resource reload that orders vanilla tiers.
        java.lang.reflect.Method sort = net.minecraftforge.common.TierSortingRegistry.class
                .getDeclaredMethod("recalculateItemTiers");
        sort.setAccessible(true);
        sort.invoke(null);
        assertTrue(net.minecraftforge.common.TierSortingRegistry.getSortedTiers()
                .contains(net.minecraft.world.item.Tiers.STONE));
    }
    private TagContainer previousTags;
    @org.junit.Before public void saveTags() { previousTags = SerializationTags.getInstance(); }
    @org.junit.After public void restoreTags() { install(previousTags); }

    @Test public void lithiumRetainsStonePickaxeMinimumThroughNativeHarvestChecks() throws Exception {
        BlockState lithium = new Ore(1).defaultBlockState();
        bindHarvestTags(lithium.getBlock(), "electricadvantage:li_ore");
        assertTrue(lithium.requiresCorrectToolForDrops());
        assertTrue(lithium.is(BlockTags.MINEABLE_WITH_PICKAXE));
        assertTrue(lithium.is(BlockTags.NEEDS_STONE_TOOL));
        assertFalse(canHarvest(lithium, ItemStack.EMPTY));
        assertFalse(canHarvest(lithium, new ItemStack(Items.WOODEN_PICKAXE)));
        assertFalse(canHarvest(lithium, new ItemStack(Items.GOLDEN_PICKAXE)));
        assertFalse(canHarvest(lithium, new ItemStack(Items.STONE_AXE)));
        assertTrue(canHarvest(lithium, new ItemStack(Items.STONE_PICKAXE)));
        assertTrue(canHarvest(lithium, new ItemStack(Items.IRON_PICKAXE)));
    }

    @Test public void sulfurRetainsWoodenPickaxeMinimumThroughNativeHarvestChecks() throws Exception {
        BlockState sulfur = new Ore(new ItemStack(Items.GUNPOWDER), 0, 3).defaultBlockState();
        bindHarvestTags(sulfur.getBlock(), "electricadvantage:sulfur_ore");
        assertTrue(sulfur.requiresCorrectToolForDrops());
        assertTrue(sulfur.is(BlockTags.MINEABLE_WITH_PICKAXE));
        assertFalse(sulfur.is(BlockTags.NEEDS_STONE_TOOL));
        assertFalse(canHarvest(sulfur, ItemStack.EMPTY));
        assertFalse(canHarvest(sulfur, new ItemStack(Items.WOODEN_AXE)));
        assertTrue(canHarvest(sulfur, new ItemStack(Items.WOODEN_PICKAXE)));
        assertTrue(canHarvest(sulfur, new ItemStack(Items.STONE_PICKAXE)));
    }

    private static void bindHarvestTags(Block block, String id) throws Exception {
        Map<ResourceLocation, Tag<Block>> tags = new HashMap<>(BlockTags.getAllTags().getAllTags());
        for (String name : new String[]{"mineable/pickaxe", "needs_stone_tool"}) {
            java.nio.file.Path path = java.nio.file.Paths.get("src/main/resources/data/minecraft/tags/blocks", name + ".json");
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(java.nio.file.Files.readString(path)).getAsJsonObject();
            boolean present = false;
            for (com.google.gson.JsonElement value : json.getAsJsonArray("values")) present |= id.equals(value.getAsString());
            tags.put(new ResourceLocation(name), Tag.fromSet(present ? Set.of(block) : Set.of()));
        }
        tags.putIfAbsent(new ResourceLocation("needs_iron_tool"), Tag.fromSet(Set.of()));
        tags.putIfAbsent(new ResourceLocation("needs_diamond_tool"), Tag.fromSet(Set.of()));
        for (Tag.Named<Block> other : java.util.List.of(BlockTags.MINEABLE_WITH_AXE,
                BlockTags.MINEABLE_WITH_SHOVEL, BlockTags.MINEABLE_WITH_HOE)) {
            tags.putIfAbsent(other.getName(), Tag.fromSet(Set.of()));
        }
        for (net.minecraft.world.item.Tier tier : net.minecraftforge.common.TierSortingRegistry.getSortedTiers()) {
            if (tier.getTag() instanceof Tag.Named) {
                tags.putIfAbsent(((Tag.Named<Block>) tier.getTag()).getName(), Tag.fromSet(Set.of()));
            }
        }
        install(new TagContainer.Builder().add(Registry.BLOCK_REGISTRY, TagCollection.of(tags)).build());
    }

    private static void install(TagContainer tags) {
        SerializationTags.bind(tags);
        StaticTags.resetAll(tags);
    }

    private static boolean canHarvest(BlockState state, ItemStack held) throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field field = allocator.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        HarvestPlayer player = (HarvestPlayer) allocator.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), HarvestPlayer.class);
        player.held = held;
        return state.canHarvestBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, player);
    }

    // Only omit world construction. Harvest eligibility still executes Forge's native hook.
    private static final class HarvestPlayer extends Player {
        private ItemStack held;
        private HarvestPlayer() { super(null, BlockPos.ZERO, 0, (GameProfile) null); }
        @Override public ItemStack getMainHandItem() { return held; }
        @Override public boolean hasCorrectToolForDrops(BlockState state) {
            return !state.requiresCorrectToolForDrops() || held.isCorrectToolForDrops(state);
        }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }
}
