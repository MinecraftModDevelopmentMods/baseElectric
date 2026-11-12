package zone.moddev.mc.baseelectric.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

public class RecipeResourceTest {
    @Test public void allRecipesUseTheNativeOneFifteenDataPackLocationAndSerializers() throws Exception {
        assertNativeCodecShapes();
        Path recipes = Paths.get("src/main/resources/data/baseelectric/recipe");
        List<Path> files;
        try (Stream<Path> stream = Files.list(recipes)) {
            files = stream.filter(path -> path.toString().endsWith(".json"))
                    .sorted().collect(Collectors.toList());
        }
        assertEquals(58, files.size());
        int crafting = 0, smelting = 0, crushing = 0;
        for (Path file : files) {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            JsonObject json = new JsonParser().parse(text).getAsJsonObject();
            String type = json.get("type").getAsString();
            assertTrue(file + " must use the 26.1.2 result template codec", json.getAsJsonObject("result").has("id"));
            assertFalse(file + " still has pre-1.21.11 Forge conditions", json.has("conditions"));
            assertTrue(file + " has unsupported serializer " + type,
                    "minecraft:crafting_shaped".equals(type)
                            || "minecraft:crafting_shapeless".equals(type)
                            || "minecraft:smelting".equals(type)
                            || "basemetals:crushing".equals(type));
            if (type.startsWith("minecraft:crafting_")) crafting++;
            else if ("minecraft:smelting".equals(type)) smelting++;
            else crushing++;
            assertFalse(file + " still contains an ore dictionary ingredient", text.contains("ore_dict"));
            if (json.has("key")) for (var entry : json.getAsJsonObject("key").entrySet())
                assertNativeIngredient(file, entry.getValue());
            if (json.has("ingredient")) assertNativeIngredient(file, json.get("ingredient"));
            if (json.has("ingredients")) assertNativeIngredient(file, json.get("ingredients"));
        }
        assertEquals(46, crafting);
        assertEquals(4, smelting);
        assertEquals(8, crushing);
        assertFalse(Files.exists(Paths.get("src/main/resources/assets/baseelectric/recipes")));
    }

    private static void assertNativeIngredient(Path file, com.google.gson.JsonElement ingredient) {
        if (ingredient.isJsonArray()) {
            assertFalse(file.toString(), ingredient.getAsJsonArray().isEmpty());
            for (var value : ingredient.getAsJsonArray()) assertNativeIngredient(file, value);
        } else {
            assertTrue(file + " must use a native item or #tag string", ingredient.isJsonPrimitive()
                    && ingredient.getAsJsonPrimitive().isString());
            assertTrue(file.toString(), ingredient.getAsString().matches("#?[a-z0-9_.-]+:[a-z0-9_./-]+"));
        }
    }

    private static void assertNativeCodecShapes() throws ReflectiveOperationException {
        zone.moddev.mc.baseelectric.verification.NativeTestBootstrap.registerVanillaIngredientSerializers();
        // A private native registry scopes this syntax test; live mod IDs are checked by GameTests.
        var registry = new net.minecraft.core.MappedRegistry<net.minecraft.world.item.Item>(
                net.minecraft.core.registries.Registries.ITEM, com.mojang.serialization.Lifecycle.stable());
        var apple = net.minecraft.core.Registry.registerForHolder(registry,
                net.minecraft.resources.Identifier.parse("minecraft:apple"), net.minecraft.world.item.Items.APPLE);
        var carrot = net.minecraft.core.Registry.registerForHolder(registry,
                net.minecraft.resources.Identifier.parse("minecraft:carrot"), net.minecraft.world.item.Items.CARROT);
        registry.freeze();
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.Identifier.parse("baseelectric:codec_test_choices"));
        registry.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(
                net.minecraft.core.registries.Registries.ITEM, java.util.Map.of(tag,
                        java.util.List.<net.minecraft.core.Holder<net.minecraft.world.item.Item>>of(apple, carrot)))).apply();
        var ops = net.minecraft.core.HolderLookup.Provider.create(Stream.of(registry))
                .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        for (String encoded : List.of("\"minecraft:apple\"",
                "[\"minecraft:apple\",\"minecraft:carrot\"]", "\"#baseelectric:codec_test_choices\"")) {
            var ingredient = net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops,
                    JsonParser.parseString(encoded)).getOrThrow();
            assertEquals(encoded, encoded.equals("\"minecraft:apple\"")
                            ? List.of(net.minecraft.world.item.Items.APPLE)
                            : List.of(net.minecraft.world.item.Items.APPLE, net.minecraft.world.item.Items.CARROT),
                    ingredient.items().map(net.minecraft.core.Holder::value).toList());
            var roundTrip = net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops,
                    net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(ops, ingredient).getOrThrow()).getOrThrow();
            assertEquals(ingredient, roundTrip);
        }
        for (String obsolete : List.of("{\"item\":\"minecraft:apple\"}",
                "{\"tag\":\"baseelectric:codec_test_choices\"}")) {
            assertTrue("Forge 64 must reject old ingredient objects: " + obsolete,
                    net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops,
                            JsonParser.parseString(obsolete)).error().isPresent());
        }
    }
}
