package zone.moddev.mc.baseelectric.recipe;

import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.basepower.PowerAdvantage;
import zone.moddev.mc.basepower.RecipeMode;

/** Registers the data-pack conditions used by ElectricAdvantage recipes. */
public final class RecipeConditions {
    private static boolean registered;
    private RecipeConditions() {}

    public static synchronized void register() {
        if (registered) return;
        CraftingHelper.register(new ResourceLocation(ElectricAdvantage.MODID, "recipe_mode"), json -> {
            RecipeMode expected = RecipeMode.valueOf(JsonUtils.getString(json, "mode").toUpperCase());
            return () -> PowerAdvantage.recipeMode == expected;
        });
        CraftingHelper.register(new ResourceLocation(ElectricAdvantage.MODID, "not_recipe_mode"), json -> {
            RecipeMode excluded = RecipeMode.valueOf(JsonUtils.getString(json, "mode").toUpperCase());
            return () -> PowerAdvantage.recipeMode != excluded;
        });
        registered = true;
    }
}
