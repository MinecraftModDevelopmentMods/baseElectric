package zone.moddev.mc.baseelectric.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import zone.moddev.mc.basepower.compat.CrushingRecipes;

/** Legacy entry points backed exclusively by the reloadable shared crushing recipes. */
public final class BaseMetalsCompat {
    private BaseMetalsCompat() {}

    public static boolean isLoaded() { return ModList.get().isLoaded("basemetals"); }

    /** Historical registration calls are now supplied by JSON recipes and common tags. */
    public static void addCrusherRecipe(String input, ItemStack output) {}
    public static void addCrusherRecipe(Item input, ItemStack output) {}
    public static void addCrusherRecipe(Block input, ItemStack output) {}
    public static void registerOreDictionaryCopies(String target, String... sources) {}

    public static boolean hasCrusherRecipe(Level world, ItemStack input) {
        return getCrusherRecipeOutput(world, input) != null;
    }

    public static ItemStack getCrusherRecipeOutput(Level world, ItemStack input) {
        return CrushingRecipes.find(world, input);
    }

    public static ItemStack getCrusherRecipeOutput(Level world, BlockState input) {
        return CrushingRecipes.find(world, input);
    }
}
