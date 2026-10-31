package zone.moddev.mc.electricadvantage.blocks;

import java.util.Random;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Legacy ElectricAdvantage ore block retaining its original drops and harvest level. */
public class Ore extends Block {
    private final ItemStack dropItem;
    private final int dropRange;

    public Ore(ItemStack oreDrop, int pickLevel, int variation) {
        super(Properties.of(Material.STONE).requiresCorrectToolForDrops().sound(SoundType.STONE)
                .strength(1.5F + pickLevel, 5.0F * (1 + pickLevel)));
        dropItem = oreDrop.copy();
        dropRange = variation + 1;
    }

    public Ore(int pickLevel) {
        super(Properties.of(Material.STONE).requiresCorrectToolForDrops().sound(SoundType.STONE)
                .strength(1.5F + pickLevel, 5.0F * (1 + pickLevel)));
        dropItem = ItemStack.EMPTY;
        dropRange = 0;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) {
        ItemStack tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (dropItem.isEmpty() || (tool != null
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0)) {
            return Collections.singletonList(new ItemStack(this));
        }
        Random random = context.getLevel().random;
        ItemStack result = dropItem.copy();
        result.setCount(dropItem.getCount() + (dropRange > 1 ? random.nextInt(dropRange) : 0));
        return Collections.singletonList(result);
    }
}
