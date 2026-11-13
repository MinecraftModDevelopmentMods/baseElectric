package zone.moddev.mc.baseelectric.util.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

/** Recursive crafting solver used by the Electric Fabricator. */
public final class RecipeDeconstructor {
    private static final RecipeDeconstructor INSTANCE = new RecipeDeconstructor();
    public static int RECURSION_LIMIT = 5;

    private final Map<ItemRecord, List<RecipePlan>> recipeCache = new HashMap<>();
    private Collection<RecipeHolder<?>> cachedRecipes;

    private RecipeDeconstructor() {}

    public static RecipeDeconstructor getInstance() {
        return INSTANCE;
    }

    public void invalidateRecipeCache() {
        recipeCache.clear();
        cachedRecipes = null;
    }

    private void initializeRecipeCache(Level world) {
        if (!(world instanceof ServerLevel server)) return;
        Collection<RecipeHolder<?>> recipes = server.recipeAccess().getRecipes();
        if (cachedRecipes == recipes) return;
        recipeCache.clear();
        for (RecipeHolder<?> holder : recipes) {
            Recipe<?> recipe = holder.value();
            for (RecipePlan plan : describe(recipe, world.registryAccess())) {
                recipeCache.computeIfAbsent(new ItemRecord(plan.result()), ignored -> new ArrayList<>()).add(plan);
            }
        }
        cachedRecipes = recipes;
    }

    record RecipePlan(ItemStack result, List<Ingredient> ingredients) {}

    static List<RecipePlan> describe(Recipe<?> recipe, RegistryAccess registries) {
        List<Ingredient> ingredients = new ArrayList<>(recipe.placementInfo().ingredients());
        ContextMap context = ContextMap.builder().set(SlotDisplayContext.REGISTRIES, registries)
                .buildAndValidate(SlotDisplayContext.CONTEXT);
        List<ItemStack> displayedResults = recipe.display().stream()
                .flatMap(display -> display.result().resolveForStacks(context).stream())
                .filter(result -> !result.isEmpty()).toList();
        if (!ingredients.isEmpty() && !displayedResults.isEmpty()) {
            return displayedResults.stream().map(result -> new RecipePlan(result.copy(), List.copyOf(ingredients))).toList();
        }
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var encoded = Recipe.DIRECT_CODEC.encodeStart(ops, recipe).getOrThrow().getAsJsonObject();
        // Special processing recipes, including Base Metals crushing, are not recipe-book placeable.
        if (ingredients.isEmpty() && encoded.has("ingredient")) {
            ingredients.add(Ingredient.CODEC.parse(ops, encoded.get("ingredient")).getOrThrow());
        } else if (ingredients.isEmpty() && encoded.has("ingredients")) {
            for (JsonElement input : encoded.getAsJsonArray("ingredients")) {
                ingredients.add(Ingredient.CODEC.parse(ops, input).getOrThrow());
            }
        }
        if (ingredients.isEmpty()) return List.of();
        if (encoded.has("result")) {
            ItemStack result = ItemStack.CODEC.parse(ops, encoded.get("result")).getOrThrow();
            return result.isEmpty() ? List.of() : List.of(new RecipePlan(result, List.copyOf(ingredients)));
        }
        return displayedResults.stream().map(result -> new RecipePlan(result.copy(), List.copyOf(ingredients))).toList();
    }

    public SerializedInventory attemptToCraft(Level world, ItemStack target,
            SerializedInventory inventory, AtomicReference<ItemStack> output) {
        if (!(world instanceof ServerLevel)) return null;
        initializeRecipeCache(world);
        return attemptToCraft(world, target, inventory, output, 0);
    }

    private SerializedInventory attemptToCraft(Level world, ItemStack target,
            SerializedInventory inventory, AtomicReference<ItemStack> output, int depth) {
        if (depth > RECURSION_LIMIT || target == null || target.isEmpty()) return null;
        List<RecipePlan> recipes = recipeCache.get(new ItemRecord(target));
        if (recipes == null || recipes.isEmpty()) return null;

        for (RecipePlan recipe : recipes) {
            SerializedInventory candidate = inventory.copy();
            boolean failed = false;
            for (Ingredient ingredient : recipe.ingredients()) {
                List<ItemStack> matching = ingredient.items().map(holder -> new ItemStack(holder.value())).toList();
                if (matching.isEmpty()) {
                    failed = true;
                    break;
                }
                ItemMatcher matcher = new ItemMatcher(matching);
                if (candidate.decrement(matcher)) continue;

                boolean craftedIngredient = false;
                for (ItemStack choice : matcher.getValidItems()) {
                    AtomicReference<ItemStack> nestedOutput = new AtomicReference<>();
                    SerializedInventory nested = attemptToCraft(
                            world, choice, candidate, nestedOutput, depth + 1);
                    if (nested != null) {
                        ItemStack surplus = nestedOutput.get();
                        if (surplus != null && surplus.getCount() > 1) {
                            surplus = surplus.copy();
                            surplus.shrink(1);
                            nested.add(surplus);
                        }
                        candidate = nested;
                        craftedIngredient = true;
                        break;
                    }
                }
                if (!craftedIngredient) {
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                output.set(recipe.result().copy());
                return candidate;
            }
        }
        return null;
    }
}
