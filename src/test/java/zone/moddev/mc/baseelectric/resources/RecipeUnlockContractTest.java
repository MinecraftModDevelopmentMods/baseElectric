package zone.moddev.mc.baseelectric.resources;

import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.world.item.Items;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecipeUnlockContractTest {
    private static final String MOD = "baseelectric";
    private static final Path DATA = Paths.get("src/main/resources/data", MOD);
    private static final Set<String> IGNORED = new LinkedHashSet<>(java.util.Arrays.asList(
            "item:minecraft:stick", "item:minecraft:string", "item:minecraft:feather", "tag:forge:rods/wooden"));

    @Test public void everyCraftingRecipeHasExactlyOneUnlockAndProcessingHasNone() throws IOException {
        Set<String> expected = new LinkedHashSet<>();
        for (Path file : files(DATA.resolve("recipe"))) {
            if (crafting(read(file))) expected.add(file.getFileName().toString());
        }
        Set<String> actual = files(DATA.resolve("advancement/recipes")).stream()
                .map(file -> file.getFileName().toString()).collect(Collectors.toSet());
        assertFalse("No crafting recipes found", expected.isEmpty());
        assertEquals("Missing or orphaned recipe unlocks", expected, actual);
        Set<String> tags = files(DATA.resolve("tags/recipe/discovery")).stream()
                .map(file -> file.getFileName().toString()).collect(Collectors.toSet());
        assertEquals("Every discovery must have exactly one optional recipe tag", expected, tags);
    }

    @Test public void discoveriesAreSingleIngredientAlternativesNotAnEntireCraftingGrid() throws IOException {
        for (Path file : files(DATA.resolve("recipe"))) {
            JsonObject recipe = read(file);
            if (!crafting(recipe)) continue;
            JsonObject unlock = read(DATA.resolve("advancement/recipes").resolve(file.getFileName()));
            assertEquals("minecraft:recipes/root", unlock.get("parent").getAsString());
            assertFalse("Recipe unlocks should not create visible advancement tabs", unlock.has("display"));
            JsonObject criteria = unlock.getAsJsonObject("criteria");
            Set<String> actual = new LinkedHashSet<>();
            for (Map.Entry<String, JsonElement> entry : criteria.entrySet()) {
                if (entry.getKey().equals("has_recipe")) continue;
                JsonObject criterion = entry.getValue().getAsJsonObject();
                assertEquals(file.toString(), "minecraft:inventory_changed", criterion.get("trigger").getAsString());
                JsonArray items = criterion.getAsJsonObject("conditions").getAsJsonArray("items");
                assertEquals("Finding ONE ingredient must be enough: " + file, 1, items.size());
                JsonObject predicate = items.get(0).getAsJsonObject();
                assertEquals("Only an item or tag predicate is expected", 1, predicate.size());
                actual.add(unlockPredicateKey(predicate));
            }
            List<JsonObject> choices = new ArrayList<>();
            if (recipe.has("key")) {
                for (Map.Entry<String, JsonElement> entry : recipe.getAsJsonObject("key").entrySet())
                    collect(entry.getValue(), choices);
            } else collect(recipe.has("ingredients") ? recipe.get("ingredients") : recipe.get("ingredient"), choices);
            Set<String> expected = choices.stream().map(RecipeUnlockContractTest::ingredientKey)
                    .filter(key -> !IGNORED.contains(key)).collect(Collectors.toCollection(LinkedHashSet::new));
            if (expected.isEmpty()) for (JsonObject choice : choices) expected.add(ingredientKey(choice));
            assertEquals("Trigger must correspond to a relevant recipe ingredient: " + file, expected, actual);
            JsonArray requirements = unlock.getAsJsonArray("requirements");
            assertEquals("All criteria must form one OR group", 1, requirements.size());
            Set<String> alternatives = new LinkedHashSet<>();
            for (JsonElement key : requirements.get(0).getAsJsonArray()) alternatives.add(key.getAsString());
            assertEquals(criteria.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet()), alternatives);
        }
    }

    @Test public void nativeParserRestrictsItemArraysButSilentlyIgnoresTheLegacySingularKey() {
        NativeTestBootstrap.initialize();
        JsonObject nativeShape = new JsonParser().parse("{\"items\":[\"minecraft:apple\"]}").getAsJsonObject();
        ItemPredicate nativePredicate = ItemPredicate.CODEC.parse(NativeTestBootstrap.registries().createSerializationContext(JsonOps.INSTANCE), nativeShape).getOrThrow();
        assertTrue(nativePredicate.items().orElseThrow().contains(Items.APPLE.builtInRegistryHolder()));
        assertFalse("An unrelated item must not unlock an apple recipe",
                nativePredicate.items().orElseThrow().contains(Items.DIRT.builtInRegistryHolder()));
        assertEquals("item:minecraft:apple", unlockPredicateKey(nativeShape));

        JsonObject legacyShape = new JsonParser().parse("{\"item\":\"minecraft:apple\"}").getAsJsonObject();
        assertEquals("The old singular spelling remains valid for recipe ingredients",
                "item:minecraft:apple", ingredientKey(legacyShape));
        ItemPredicate legacyPredicate = ItemPredicate.CODEC.parse(NativeTestBootstrap.registries().createSerializationContext(JsonOps.INSTANCE), legacyShape).getOrThrow();
        assertTrue("Negative control: the native parser silently loses this item's restriction",
                legacyPredicate.items().isEmpty());
        assertThrows("The resource contract must reject that silently unrestricted trigger",
                AssertionError.class, () -> unlockPredicateKey(legacyShape));
        for (String malformed : new String[] {
                "{\"items\":[]}", "{\"items\":[\"minecraft:apple\",\"minecraft:dirt\"]}",
                "{\"items\":\"minecraft:apple\"}", "{\"items\":[1]}",
                "{\"items\":[\"minecraft:apple\"],\"item\":\"minecraft:dirt\"}"}) {
            JsonObject predicate = new JsonParser().parse(malformed).getAsJsonObject();
            assertThrows(malformed, AssertionError.class, () -> unlockPredicateKey(predicate));
        }
    }

    @Test public void rewardAndRecipeCommandCriterionReferToTheExactVariant() throws IOException {
        NativeTestBootstrap.initialize();
        var loaded = nativeRecipeRegistry(true);
        var inactive = nativeRecipeRegistry(false);
        for (Path file : files(DATA.resolve("advancement/recipes"))) {
            String id = MOD + ":" + file.getFileName().toString().replace(".json", "");
            JsonObject unlock = read(file);
            JsonArray recipes = unlock.getAsJsonObject("rewards").getAsJsonArray("recipes");
            assertEquals(1, recipes.size());
            assertEquals(id, recipes.get(0).getAsString());
            JsonObject criterion = unlock.getAsJsonObject("criteria").getAsJsonObject("has_recipe");
            assertEquals("minecraft:recipe_unlocked", criterion.get("trigger").getAsString());
            assertEquals(id, recipeCriterionId(criterion.getAsJsonObject("conditions")));
            assertNativeRecipeCriterion(criterion.getAsJsonObject("conditions"), id, loaded, true);
            assertNativeRecipeCriterion(criterion.getAsJsonObject("conditions"), id, inactive, false);
            JsonObject recipe = read(DATA.resolve("recipe").resolve(file.getFileName()));
            assertConditionParity(recipe, unlock);
            JsonObject tag = read(DATA.resolve("tags/recipe/discovery").resolve(file.getFileName()));
            assertOptionalDiscoveryTag(tag, id);
            for (String mutation : new String[] {"required", "missing_required", "identity"}) {
                var changed = tag.deepCopy();
                var value = changed.getAsJsonArray("values").get(0).getAsJsonObject();
                if (mutation.equals("required")) value.addProperty("required", true);
                else if (mutation.equals("missing_required")) value.remove("required");
                else value.addProperty("id", MOD + ":unrelated_codec_recipe");
                assertThrows("Optional tag identity/required guard must reject " + mutation,
                        AssertionError.class, () -> assertOptionalDiscoveryTag(changed, id));
            }
            if (recipe.has("forge:condition")) {
                JsonObject conditional = unlock.deepCopy();
                conditional.add("forge:condition", recipe.get("forge:condition"));
                assertThrows("Mode changes must not remove future discovery advancements", AssertionError.class,
                        () -> assertConditionParity(recipe, conditional));
            }
            var ops = net.minecraft.core.HolderLookup.Provider.create(Stream.of(inactive)).createSerializationContext(JsonOps.INSTANCE);
            var rewards = net.minecraft.advancements.AdvancementRewards.CODEC.parse(ops, unlock.get("rewards")).getOrThrow();
            assertEquals("Native reward keys need not name a currently loaded recipe", id,
                    rewards.recipes().get(0).identifier().toString());
            assertEquals(unlock.get("rewards"), net.minecraft.advancements.AdvancementRewards.CODEC.encodeStart(ops, rewards).getOrThrow());
            assertFalse(unlock.has("conditions"));
        }
        assertTrue("The material-gated control must remain conditional",
                read(DATA.resolve("recipe/alkaline_battery.json")).has("forge:condition"));
        for (String malformed : new String[] {"{\"recipe\":\"baseelectric:alkaline_battery\"}",
                "{\"recipes\":[]}", "{\"recipes\":[\"baseelectric:alkaline_battery\",\"baseelectric:unrelated_codec_recipe\"]}",
                "{\"recipes\":\"baseelectric:alkaline_battery\"}",
                "{\"recipes\":\"#other:discovery/alkaline_battery\"}",
                "{\"recipes\":\"#baseelectric:discovery/alkaline_battery\",\"recipe\":\"baseelectric:unrelated_codec_recipe\"}"}) {
            var conditions = JsonParser.parseString(malformed).getAsJsonObject();
            assertThrows(malformed, AssertionError.class, () -> recipeCriterionId(conditions));
        }
    }

    private static String recipeCriterionId(JsonObject conditions) {
        assertEquals("A recipe criterion must constrain exactly its own discovery tag", 1, conditions.size());
        assertFalse(conditions.has("recipe"));
        assertTrue(conditions.has("recipes") && conditions.get("recipes").isJsonPrimitive()
                && conditions.getAsJsonPrimitive("recipes").isString());
        String id = conditions.get("recipes").getAsString();
        String prefix = "#" + MOD + ":discovery/";
        assertTrue("Discovery criteria must use the owned optional recipe tag", id.startsWith(prefix));
        String stem = id.substring(prefix.length());
        assertTrue(stem.matches("[a-z0-9_./-]+"));
        return MOD + ":" + stem;
    }

    private static void assertConditionParity(JsonObject recipe, JsonObject unlock) {
        assertFalse("Discovery must survive recipe mode/material availability changes", unlock.has("forge:condition"));
        assertNotNull("Recipe input must remain untouched", recipe.get("type"));
    }

    private static void assertOptionalDiscoveryTag(JsonObject tag, String id) {
        assertEquals(1, tag.size());
        assertTrue(tag.has("values") && tag.get("values").isJsonArray());
        assertEquals(1, tag.getAsJsonArray("values").size());
        var entry = tag.getAsJsonArray("values").get(0).getAsJsonObject();
        assertEquals(2, entry.size());
        assertEquals(id, entry.get("id").getAsString());
        assertTrue("Missing required defaults to true in the native codec", entry.has("required"));
        assertTrue(entry.get("required").isJsonPrimitive() && entry.getAsJsonPrimitive("required").isBoolean());
        assertFalse(entry.get("required").getAsBoolean());
        var decoded = net.minecraft.tags.TagFile.CODEC.parse(JsonOps.INSTANCE, tag).getOrThrow();
        assertEquals(tag, net.minecraft.tags.TagFile.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow());
    }

    private static net.minecraft.core.Registry<net.minecraft.world.item.crafting.Recipe<?>> nativeRecipeRegistry(boolean available) throws IOException {
        var registry = new net.minecraft.core.MappedRegistry<net.minecraft.world.item.crafting.Recipe<?>>(
                net.minecraft.core.registries.Registries.RECIPE, com.mojang.serialization.Lifecycle.stable());
        var ids = new ArrayList<String>();
        if (available) for (Path file : files(DATA.resolve("advancement/recipes")))
            ids.add(MOD + ":" + file.getFileName().toString().replace(".json", ""));
        ids.add(MOD + ":unrelated_codec_recipe");
        for (String id : ids) {
            var recipe = new net.minecraft.world.item.crafting.ShapelessRecipe(
                    new net.minecraft.world.item.crafting.Recipe.CommonInfo(true),
                    new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(
                            net.minecraft.world.item.crafting.CraftingBookCategory.MISC, "unlock-codec"),
                    new net.minecraft.world.item.ItemStackTemplate(Items.BREAD, 1),
                    List.of(net.minecraft.world.item.crafting.Ingredient.of(Items.APPLE)));
            net.minecraft.core.Registry.registerForHolder(registry, net.minecraft.resources.Identifier.parse(id), recipe);
        }
        registry.freeze();
        var tags = new java.util.HashMap<net.minecraft.tags.TagKey<net.minecraft.world.item.crafting.Recipe<?>>,
                List<net.minecraft.core.Holder<net.minecraft.world.item.crafting.Recipe<?>>>>();
        for (Path file : files(DATA.resolve("advancement/recipes"))) {
            String stem = file.getFileName().toString().replace(".json", "");
            String id = MOD + ":" + stem;
            var raw = read(DATA.resolve("tags/recipe/discovery").resolve(file.getFileName()));
            assertOptionalDiscoveryTag(raw, id);
            var parsed = net.minecraft.tags.TagFile.CODEC.parse(JsonOps.INSTANCE, raw).getOrThrow();
            var members = new ArrayList<net.minecraft.core.Holder<net.minecraft.world.item.crafting.Recipe<?>>>();
            var lookup = new net.minecraft.tags.TagEntry.Lookup<net.minecraft.core.Holder<net.minecraft.world.item.crafting.Recipe<?>>>() {
                            @Override public net.minecraft.core.Holder<net.minecraft.world.item.crafting.Recipe<?>> element(
                                    net.minecraft.resources.Identifier key, boolean required) {
                                return registry.get(key).orElse(null);
                            }
                            @Override public java.util.Collection<net.minecraft.core.Holder<net.minecraft.world.item.crafting.Recipe<?>>> tag(
                                    net.minecraft.resources.Identifier key) { return null; }
                        };
            for (var entry : parsed.entries()) assertTrue("An absent optional recipe must not fail native tag loading",
                    entry.build(lookup, members::add));
            if (!available) assertFalse("Native required=true must reject an absent recipe",
                    net.minecraft.tags.TagEntry.element(net.minecraft.resources.Identifier.parse(id)).build(lookup, members::add));
            assertEquals(available ? 1 : 0, members.size());
            tags.put(discoveryTag(stem), members);
        }
        var unrelated = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.parse(MOD + ":unrelated_codec_recipe"));
        tags.put(discoveryTag("unrelated_codec_recipe"), List.of(registry.get(unrelated).orElseThrow()));
        registry.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(
                net.minecraft.core.registries.Registries.RECIPE, tags)).apply();
        return registry;
    }

    private static net.minecraft.tags.TagKey<net.minecraft.world.item.crafting.Recipe<?>> discoveryTag(String stem) {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.parse(MOD + ":discovery/" + stem));
    }

    private static void assertNativeRecipeCriterion(JsonObject conditions, String id,
            net.minecraft.core.Registry<net.minecraft.world.item.crafting.Recipe<?>> registry,
            boolean available) {
        var ops = net.minecraft.core.HolderLookup.Provider.create(Stream.of(registry)).createSerializationContext(JsonOps.INSTANCE);
        var codec = net.minecraft.advancements.triggers.RecipeUnlockedTrigger.TriggerInstance.CODEC;
        var decoded = codec.parse(ops, conditions).getOrThrow();
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.parse(id));
        var unrelatedKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.parse(MOD + ":unrelated_codec_recipe"));
        var unrelated = new net.minecraft.world.item.crafting.RecipeHolder<>(unrelatedKey, registry.getValueOrThrow(unrelatedKey));
        var matching = new net.minecraft.world.item.crafting.RecipeHolder<>(key,
                available ? registry.getValueOrThrow(key) : unrelated.value());
        assertEquals(available ? 1 : 0, decoded.recipes().size());
        assertEquals(discoveryTag(id.substring((MOD + ":").length())), decoded.recipes().unwrapKey().orElseThrow());
        assertEquals("Only an available exact recipe variant may match", available, decoded.matches(matching));
        assertFalse("Unrelated recipe must not unlock this variant", decoded.matches(unrelated));
        var wire = codec.encodeStart(ops, decoded).getOrThrow();
        assertEquals(conditions, wire);
        var again = codec.parse(ops, wire).getOrThrow();
        assertEquals(available, again.matches(matching));
        assertFalse(again.matches(unrelated));
        var wrongTag = new JsonObject();
        wrongTag.addProperty("recipes", "#" + MOD + ":discovery/unrelated_codec_recipe");
        var wrong = codec.parse(ops, wrongTag).getOrThrow();
        assertFalse("An unrelated discovery tag cannot match this variant", wrong.matches(matching));
        assertTrue("The unrelated-tag control must itself be populated", wrong.matches(unrelated));
        var old = new JsonObject();
        old.addProperty("recipe", id);
        var obsolete = codec.parse(ops, old);
        assertTrue("Native 26.3 must reject the old singular recipe key", obsolete.error().isPresent());
        assertTrue(obsolete.error().orElseThrow().message().contains("recipes"));
        var missing = new JsonObject();
        missing.addProperty("recipes", MOD + ":missing_codec_recipe");
        assertTrue("Unavailable recipe holders cannot qualify an unconditional unlock",
                codec.parse(ops, missing).error().isPresent());
        missing.addProperty("recipes", "#" + MOD + ":discovery/missing_codec_tag");
        assertTrue("Undefined tags cannot substitute for defined-empty discovery tags",
                codec.parse(ops, missing).error().isPresent());
    }

    @Test public void recipeBookGroupsIdentifyTheOutputNotABroadMachineCategory() throws IOException {
        for (Path file : files(DATA.resolve("recipe"))) {
            JsonObject recipe = read(file);
            if (!crafting(recipe)) continue;
            JsonElement result = recipe.get("result");
            String output = result.isJsonPrimitive() ? result.getAsString()
                    : result.getAsJsonObject().get("id").getAsString();
            assertTrue("Missing output-specific group: " + file, recipe.has("group"));
            assertEquals("Different items must not share a recipe-book cell: " + file,
                    output, recipe.get("group").getAsString());
        }
    }

    private static boolean crafting(JsonObject recipe) {
        String type = recipe.get("type").getAsString();
        return type.equals("minecraft:crafting_shaped") || type.equals("minecraft:crafting_shapeless")
                || type.equals("minecraft:smelting");
    }

    private static String ingredientKey(JsonObject predicate) {
        String type = predicate.has("tag") ? "tag" : "item";
        return type + ":" + predicate.get(type).getAsString();
    }

    private static String unlockPredicateKey(JsonObject predicate) {
        assertEquals("Unlock predicates must constrain exactly one item or tag", 1, predicate.size());
        assertFalse("The native parser ignores the singular item key in advancement predicates", predicate.has("item"));
        if (predicate.has("items") && predicate.get("items").isJsonPrimitive()) {
            assertTrue(predicate.getAsJsonPrimitive("items").isString());
            String tag = predicate.get("items").getAsString();
            assertTrue("A tag predicate must use a hash prefix", tag.startsWith("#") && tag.length() > 1);
            return "tag:" + tag.substring(1);
        }
        assertTrue("A native item predicate requires the items array", predicate.has("items")
                && predicate.get("items").isJsonArray());
        JsonArray items = predicate.getAsJsonArray("items");
        assertEquals("Each unlock alternative must name one ingredient", 1, items.size());
        assertTrue("Item IDs must be strings", items.get(0).isJsonPrimitive()
                && items.get(0).getAsJsonPrimitive().isString());
        return "item:" + items.get(0).getAsString();
    }

    private static void collect(JsonElement element, List<JsonObject> result) {
        if (element.isJsonArray()) for (JsonElement child : element.getAsJsonArray()) collect(child, result);
        else if (element.isJsonPrimitive()) {
            String id = element.getAsString();
            JsonObject choice = new JsonObject();
            choice.addProperty(id.startsWith("#") ? "tag" : "item", id.startsWith("#") ? id.substring(1) : id);
            result.add(choice);
        } else result.add(element.getAsJsonObject());
    }

    private static JsonObject read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    private static List<Path> files(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return java.util.Collections.emptyList();
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(path -> path.toString().endsWith(".json")).sorted().collect(Collectors.toList());
        }
    }
}
