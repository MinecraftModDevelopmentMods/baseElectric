package zone.moddev.mc.baseelectric.util.crafting;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.junit.BeforeClass;
import org.junit.Test;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import static org.junit.Assert.*;

public class RecipeDeconstructorTest {
    @BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }

    @Test public void shapedPlansKeepRepeatedIngredientsAndResultCountWithoutEmptySlots() {
        var recipe = new ShapedRecipe("native-plan", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(Map.of('A', Ingredient.of(Items.APPLE)), " A ", "AA "),
                new ItemStack(Items.GOLDEN_APPLE, 3));
        var plans = RecipeDeconstructor.describe(recipe, NativeTestBootstrap.registries());
        assertEquals(1, plans.size());
        var plan = plans.getFirst();
        assertTrue(plan.result().is(Items.GOLDEN_APPLE));
        assertEquals(3, plan.result().getCount());
        assertEquals(3, plan.ingredients().size());
        for (Ingredient ingredient : plan.ingredients()) {
            assertTrue(ingredient.test(new ItemStack(Items.APPLE)));
            assertFalse(ingredient.test(new ItemStack(Items.DIRT)));
        }
    }

    @Test public void shapelessPlansKeepAlternativesAndDoNotInventFreeIngredients() {
        var recipe = new ShapelessRecipe("native-plan", CraftingBookCategory.MISC,
                new ItemStack(Items.BREAD, 2), List.of(Ingredient.of(Items.APPLE, Items.CARROT), Ingredient.of(Items.WHEAT)));
        var plan = RecipeDeconstructor.describe(recipe, NativeTestBootstrap.registries()).getFirst();
        assertEquals(2, plan.ingredients().size());
        assertEquals(2, plan.result().getCount());
        assertTrue(plan.result().is(Items.BREAD));
        assertTrue(plan.ingredients().getFirst().test(new ItemStack(Items.APPLE)));
        assertTrue(plan.ingredients().getFirst().test(new ItemStack(Items.CARROT)));
        assertFalse(plan.ingredients().getFirst().test(new ItemStack(Items.WHEAT)));
        assertTrue(plan.ingredients().get(1).test(new ItemStack(Items.WHEAT)));
        var registries = NativeTestBootstrap.registries();
        var original = new net.minecraft.nbt.CompoundTag();
        original.putInt("progress", 127);
        original.putIntArray("parentPos", new int[] {31, 72, -18});
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, registries);
        output.store(zone.moddev.mc.baseelectric.util.LegacyNbt.ROOT, original);
        assertEquals(original, output.buildResult());
        var input = net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, registries, output.buildResult());
        assertEquals(original, input.read(zone.moddev.mc.baseelectric.util.LegacyNbt.ROOT).orElseThrow());
    }
}
