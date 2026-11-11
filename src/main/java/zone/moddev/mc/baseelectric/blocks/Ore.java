package zone.moddev.mc.baseelectric.blocks;

import net.minecraft.util.RandomSource;
import net.minecraft.core.registries.Registries;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Legacy ElectricAdvantage ore block retaining its original drops and harvest level. */
public class Ore extends Block {
    private final Supplier<ItemStack> drop;
    private final int dropRange;

    public Ore(ItemStack oreDrop, int pickLevel, int variation) {
        this(fixedDrop(oreDrop), pickLevel, variation);
    }

    public static Ore withDeferredDrop(Supplier<ItemStack> drop, int pickLevel, int variation) {
        return new Ore(drop, pickLevel, variation);
    }

    private static Supplier<ItemStack> fixedDrop(ItemStack oreDrop) {
        ItemStack copy = oreDrop.copy();
        return () -> copy;
    }

    private Ore(Supplier<ItemStack> drop, int pickLevel, int variation) {
        super(Properties.of().setId(zone.moddev.mc.baseelectric.util.RegistryIds.block("sulfur_ore"))
                .mapColor(MapColor.STONE).requiresCorrectToolForDrops().sound(SoundType.STONE)
                .strength(1.5F + pickLevel, 5.0F * (1 + pickLevel)));
        this.drop = drop;
        dropRange = variation + 1;
    }

    public Ore(int pickLevel) {
        super(Properties.of().setId(zone.moddev.mc.baseelectric.util.RegistryIds.block("li_ore"))
                .mapColor(MapColor.STONE).requiresCorrectToolForDrops().sound(SoundType.STONE)
                .strength(1.5F + pickLevel, 5.0F * (1 + pickLevel)));
        drop = () -> ItemStack.EMPTY;
        dropRange = 0;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        ItemStack tool = context.getOptionalParameter(LootContextParams.TOOL);
        ItemStack dropItem = drop.get();
        if (dropItem.isEmpty() || (tool != null
                && EnchantmentHelper.getItemEnchantmentLevel(context.getLevel().registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0)) {
            return Collections.singletonList(new ItemStack(this));
        }
        RandomSource random = context.getLevel().random;
        ItemStack result = dropItem.copy();
        result.setCount(dropItem.getCount() + (dropRange > 1 ? random.nextInt(dropRange) : 0));
        return Collections.singletonList(result);
    }
}
