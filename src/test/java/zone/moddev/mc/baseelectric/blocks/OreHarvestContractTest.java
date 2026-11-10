package zone.moddev.mc.baseelectric.blocks;

import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class OreHarvestContractTest {
    @BeforeClass public static void bootstrapNativeRegistries() { NativeTestBootstrap.initialize(); }
    @Test public void lithiumRetainsStonePickaxeMinimumThroughNativeHarvestChecks() throws Exception {
        BlockState lithium = new Ore(1).defaultBlockState();
        bindHarvestTags(lithium.getBlock(), "baseelectric:li_ore");
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
        BlockState sulfur = Ore.withDeferredDrop(() -> {
            throw new AssertionError("Harvest eligibility must not require the drop item to be registered");
        }, 0, 3).defaultBlockState();
        bindHarvestTags(sulfur.getBlock(), "baseelectric:sulfur_ore");
        assertTrue(sulfur.requiresCorrectToolForDrops());
        assertTrue(sulfur.is(BlockTags.MINEABLE_WITH_PICKAXE));
        assertFalse(sulfur.is(BlockTags.NEEDS_STONE_TOOL));
        assertFalse(canHarvest(sulfur, ItemStack.EMPTY));
        assertFalse(canHarvest(sulfur, new ItemStack(Items.WOODEN_AXE)));
        assertTrue(canHarvest(sulfur, new ItemStack(Items.WOODEN_PICKAXE)));
        assertTrue(canHarvest(sulfur, new ItemStack(Items.STONE_PICKAXE)));
    }

    private static void bindHarvestTags(Block block, String id) throws Exception {
        Set<TagKey<Block>> tags = new HashSet<>();
        for (TagKey<Block> tag : java.util.List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)) {
            java.nio.file.Path path = java.nio.file.Paths.get("src/main/resources/data/minecraft/tags/blocks",
                    tag.location().getPath() + ".json");
            var json = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();
            for (var value : json.getAsJsonArray("values")) {
                if (id.equals(value.getAsString())) tags.add(tag);
            }
        }
        // Vanilla's wooden and golden incorrect-tool tags both include needs_stone_tool.
        if (tags.contains(BlockTags.NEEDS_STONE_TOOL)) {
            tags.add(net.minecraft.world.item.Tiers.WOOD.getIncorrectBlocksForDrops());
            tags.add(net.minecraft.world.item.Tiers.GOLD.getIncorrectBlocksForDrops());
        }
        // Bind only the fresh test block's holder; leave the global registry alone.
        block.builtInRegistryHolder().bindTags(tags);
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
