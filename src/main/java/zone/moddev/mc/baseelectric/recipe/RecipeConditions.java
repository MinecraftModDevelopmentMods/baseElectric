package zone.moddev.mc.baseelectric.recipe;

import java.util.Locale;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.basepower.PowerAdvantage;
import zone.moddev.mc.basepower.RecipeMode;

/** Registers the data-pack conditions used by ElectricAdvantage recipes. */
public final class RecipeConditions {
    private static boolean registered;
    private RecipeConditions() {}

    public static synchronized void register() {
        if (registered) return;
        CraftingHelper.register(new ModeSerializer(false));
        CraftingHelper.register(new ModeSerializer(true));
        registered = true;
    }

    static final class ModeCondition implements ICondition {
        private final RecipeMode mode;
        private final boolean inverted;

        ModeCondition(RecipeMode mode, boolean inverted) {
            this.mode = mode;
            this.inverted = inverted;
        }

        @Override public ResourceLocation getID() {
            return new ResourceLocation(ElectricAdvantage.MODID,
                    inverted ? "not_recipe_mode" : "recipe_mode");
        }

        public boolean test() { return test(IContext.EMPTY); }

        @Override public boolean test(IContext context) {
            return (PowerAdvantage.recipeMode == mode) != inverted;
        }
    }

    static final class ModeSerializer implements IConditionSerializer<ModeCondition> {
        private final boolean inverted;

        ModeSerializer(boolean inverted) {
            this.inverted = inverted;
        }

        @Override public void write(JsonObject json, ModeCondition condition) {
            json.addProperty("mode", condition.mode.name().toLowerCase(Locale.ROOT));
        }

        @Override public ModeCondition read(JsonObject json) {
            return new ModeCondition(RecipeMode.valueOf(
                    GsonHelper.getAsString(json, "mode").toUpperCase(Locale.ROOT)), inverted);
        }

        @Override public ResourceLocation getID() {
            return new ResourceLocation(ElectricAdvantage.MODID,
                    inverted ? "not_recipe_mode" : "recipe_mode");
        }
    }
}
