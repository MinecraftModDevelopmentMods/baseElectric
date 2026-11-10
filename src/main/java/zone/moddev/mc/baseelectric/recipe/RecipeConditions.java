package zone.moddev.mc.baseelectric.recipe;

import java.util.Locale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.basepower.PowerAdvantage;
import zone.moddev.mc.basepower.RecipeMode;

/** Registers the data-pack conditions used by Electric Advantage recipes. */
public final class RecipeConditions {
    private static final DeferredRegister<MapCodec<? extends ICondition>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.CONDITION_SERIALIZERS, ElectricAdvantage.MODID);
    private static final Codec<RecipeMode> MODE = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(RecipeMode.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    return DataResult.error(() -> "Unknown baseElectric recipe mode: " + name);
                }
            },
            mode -> mode.name().toLowerCase(Locale.ROOT));
    static final MapCodec<ModeCondition> MATCH = modeCodec(false);
    static final MapCodec<ModeCondition> NOT_MATCH = modeCodec(true);

    static {
        SERIALIZERS.register("recipe_mode", () -> MATCH);
        SERIALIZERS.register("not_recipe_mode", () -> NOT_MATCH);
    }

    private RecipeConditions() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }

    private static MapCodec<ModeCondition> modeCodec(boolean inverted) {
        return MODE.fieldOf("mode").xmap(mode -> new ModeCondition(mode, inverted), ModeCondition::mode);
    }

    record ModeCondition(RecipeMode mode, boolean inverted) implements ICondition {
        @Override public boolean test(IContext context, DynamicOps<?> ops) {
            return (PowerAdvantage.recipeMode == mode) != inverted;
        }

        @Override public MapCodec<? extends ICondition> codec() {
            return inverted ? NOT_MATCH : MATCH;
        }
    }
}
