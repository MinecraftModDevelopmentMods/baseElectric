package zone.moddev.mc.baseelectric.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import org.junit.Test;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.Assert.*;

public class ClientResourceContractTest {
    private static final Path ROOT = Paths.get("src/main/resources");
    private static final Path ASSETS = ROOT.resolve("assets/baseelectric");

    @Test public void blockstatesUseLiteralModelPaths() throws Exception {
        try (Stream<Path> files = Files.walk(ASSETS.resolve("blockstates"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                verifyModels(read(file), file);
            }
        }
    }

    @Test public void obsoleteSelectorsAreAbsent() throws Exception {
        try (Stream<Path> files = Files.walk(ASSETS.resolve("blockstates"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                JsonObject state = read(file);
                assertFalse(file.toString(), state.has("forge_marker"));
                if (state.has("variants")) {
                    assertFalse(file.toString(), state.getAsJsonObject("variants").has("normal"));
                    assertFalse(file.toString(), state.getAsJsonObject("variants").has("fluid"));
                }
            }
        }
    }

    @Test public void parentsAndTexturesExistInActualPacks() throws Exception {
        SpriteSources.bootstrap();
        var sprites = new java.util.HashSet<String>();
        for (String atlas : new String[]{"blocks", "items"}) {
            var decoded = SpriteSources.FILE_CODEC.parse(JsonOps.INSTANCE,
                    read(ROOT.resolve("assets/minecraft/atlases/" + atlas + ".json"))).getOrThrow();
            assertEquals(atlas, "blocks".equals(atlas) ? 49 : 17, decoded.size());
            for (var source : decoded) {
                assertTrue("No global directories or order-dependent filters", source instanceof SingleFile);
                var single = (SingleFile) source;
                assertEquals("baseelectric", single.resourceId().getNamespace());
                assertTrue(single.spriteId().isEmpty());
                assertTrue("Duplicate sprite across native atlases", sprites.add(single.resourceId().toString()));
            }
        }
        assertTrue(sprites.contains("baseelectric:entity/laser_turret"));
        assertTrue(sprites.contains("baseelectric:materials/machine_case"));
        assertFalse(sprites.contains("baseelectric:materials/laser_beam"));
        assertTrue(SpriteSources.FILE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"sources\":[{\"type\":\"minecraft:single\",\"resource\":5}]}" )).error().isPresent());
        assertFalse("Abstract parent is not a standalone registered item",
                Files.exists(ASSETS.resolve("items/battery_base.json")));
        assertTrue(Files.isRegularFile(ASSETS.resolve("models/item/battery_base.json")));
        for (String battery : new String[]{"alkaline_battery", "lead_acid_battery", "lithium_battery", "nickel_hydride_battery"}) {
            JsonObject child = read(ASSETS.resolve("models/item/" + battery + ".json"));
            assertEquals("baseelectric:item/battery_base", child.get("parent").getAsString());
            assertEquals("baseelectric:items/" + battery, child.getAsJsonObject("textures").get("item_texture").getAsString());
            assertEquals("baseelectric:item/" + battery, read(ASSETS.resolve("items/" + battery + ".json"))
                    .getAsJsonObject("model").get("model").getAsString());
        }
        try (Stream<Path> files = Files.walk(ASSETS.resolve("models"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                JsonObject model = read(file);
                if (model.has("parent")) reference(model.get("parent").getAsString(), "models", ".json", file);
                if (model.has("textures")) for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet())
                    reference(entry.getValue().getAsString(), "textures", ".png", file);
            }
        }
    }

    @Test public void modernAliasesRetainEveryLegacyTranslation() throws Exception {
        try (Stream<Path> files = Files.walk(ASSETS.resolve("lang"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                JsonObject locale = read(file);
                for (Map.Entry<String, JsonElement> entry : locale.entrySet()) {
                    String key = entry.getKey();
                    if (!(key.startsWith("tile.baseelectric.") || key.startsWith("item.baseelectric."))
                            || !key.endsWith(".name")) continue;
                    String alias = key.substring(0, key.length() - 5);
                    if (alias.startsWith("tile.")) alias = "block." + alias.substring(5);
                    assertEquals(file + " lost alias " + alias, entry.getValue(), locale.get(alias));
                }
            }
        }
    }

    private static void verifyModels(JsonElement node, Path file) {
        if (node.isJsonObject()) for (Map.Entry<String, JsonElement> entry : node.getAsJsonObject().entrySet()) {
            if ("model".equals(entry.getKey())) reference(entry.getValue().getAsString(), "models", ".json", file);
            else verifyModels(entry.getValue(), file);
        }
        else if (node.isJsonArray()) for (JsonElement child : node.getAsJsonArray()) verifyModels(child, file);
    }

    private static void reference(String id, String directory, String suffix, Path file) {
        if (id.startsWith("#")) return;
        String[] parts = id.split(":", 2);
        String namespace = parts.length == 2 ? parts[0] : "minecraft";
        String path = parts.length == 2 ? parts[1] : parts[0];
        if ("minecraft".equals(namespace) && path.startsWith("builtin/")) return;
        String resource = "assets/" + namespace + "/" + directory + "/" + path + suffix;
        boolean exists = "baseelectric".equals(namespace) ? Files.isRegularFile(ROOT.resolve(resource))
                : ClientResourceContractTest.class.getClassLoader().getResource(resource) != null;
        assertTrue(file + " references missing " + resource, exists);
    }

    private static JsonObject read(Path file) throws Exception {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }
}
