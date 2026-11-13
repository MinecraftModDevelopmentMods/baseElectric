package zone.moddev.mc.baseelectric.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.Test;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import static org.junit.Assert.*;

public class AcceptedLootBaselineTest {
    private static final Path FIXTURE = Path.of("src/test/resources/loot-baseline-262");
    private static final String PREFIX = "data/baseelectric/loot_table/";

    @Test public void everyWholeTablePreservesTheAcceptedJarSemantics() throws Exception {
        NativeTestBootstrap.initialize();
        byte[] manifestBytes = Files.readAllBytes(FIXTURE.resolve("manifest.json"));
        assertEquals("D4F3187FE5A46652885D66C2E68C9B083EF6A0E1076BD20BD6462F56F6BF1132", sha(manifestBytes));
        var manifest = JsonParser.parseString(new String(manifestBytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("A7D12A66CA91A6BACCFEFD2CED9D3EC962F5FEB73BF632E1683F99084384F938",
                manifest.get("sourceJarSha256").getAsString());
        assertEquals("baseElectric-2.2.18.2602001.jar", manifest.get("artifact").getAsString());
        var golden = new TreeMap<String, JsonObject>();
        var current = new TreeMap<String, JsonObject>();
        var itemIds = new TreeSet<String>();
        for (var element : manifest.getAsJsonArray("tables")) {
            var entry = element.getAsJsonObject();
            String path = entry.get("path").getAsString();
            assertTrue(path, path.startsWith(PREFIX) && !path.contains(".."));
            byte[] bytes = Files.readAllBytes(FIXTURE.resolve(path));
            assertEquals(path, entry.get("bytes").getAsLong(), bytes.length);
            assertEquals(path, entry.get("sha256").getAsString(), sha(bytes));
            var accepted = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var expected = translateContainer(accepted);
            var production = JsonParser.parseString(Files.readString(Path.of("src/main/resources").resolve(path))).getAsJsonObject();
            assertEquals(path, normalize(expected, "table", false), normalize(production, "table", false));
            assertNull("Duplicate accepted table", golden.put(path, expected));
            current.put(path, production);
            visitItems(expected, item -> itemIds.add(item.get("name").getAsString()));
        }
        assertEquals(21, golden.size());
        try (var paths = Files.walk(Path.of("src/main/resources").resolve(PREFIX))) {
            var actual = new TreeSet<String>();
            paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> actual.add(
                    Path.of("src/main/resources").relativize(path).toString().replace('\\', '/')));
            assertEquals(golden.keySet(), actual);
        }
        var registryBefore = new TreeSet<>(BuiltInRegistries.ITEM.keySet());
        var available = registryBefore.stream().map(Object::toString)
                .filter(id -> id.startsWith("minecraft:") && !id.equals("minecraft:air") && !itemIds.contains(id)).sorted().toList();
        var forward = new TreeMap<String, String>();
        var reverse = new TreeMap<String, String>();
        for (String id : itemIds) {
            if (id.startsWith("minecraft:")) continue;
            String standIn = available.get(forward.size());
            forward.put(id, standIn);
            assertNull("Stand-ins must be bijective", reverse.put(standIn, id));
        }
        assertEquals(forward.size(), reverse.size());
        for (var entry : golden.entrySet()) {
            var encoded = wholeTable(current.get(entry.getKey()), forward, reverse);
            assertEquals(entry.getKey(), normalize(entry.getValue(), "table", true), normalize(encoded, "table", true));
        }
        String chest = PREFIX + "chests/advantage_parts.json";
        for (String mutation : new String[] {"count", "weight", "identity", "plural_modifier"}) {
            var changed = current.get(chest).deepCopy();
            var entries = changed.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
            var item = entries.get(1).getAsJsonObject();
            switch (mutation) {
                case "count" -> {
                    var count = item.getAsJsonObject("modifier").getAsJsonObject("count");
                    count.addProperty("max", count.get("max").getAsInt() + 1);
                }
                case "weight" -> item.addProperty("weight", item.get("weight").getAsInt() + 1);
                case "identity" -> item.add("name", entries.get(2).getAsJsonObject().get("name"));
                case "plural_modifier" -> {
                    var old = new JsonArray();
                    old.add(item.remove("modifier"));
                    item.add("functions", old);
                }
                default -> throw new AssertionError(mutation);
            }
            assertNotEquals("Golden whole-table proof must reject " + mutation,
                    normalize(golden.get(chest), "table", true), normalize(wholeTable(changed, forward, reverse), "table", true));
        }
        String block = golden.firstKey();
        var changed = current.get(block).deepCopy();
        var pool = changed.getAsJsonArray("pools").get(0).getAsJsonObject();
        var old = new JsonArray();
        old.add(pool.remove("condition"));
        pool.add("conditions", old);
        assertNotEquals("Golden whole-table proof must reject lost explosion gating",
                normalize(golden.get(block), "table", true), normalize(wholeTable(changed, forward, reverse), "table", true));
        assertEquals("Codec stand-ins must not mutate the real item registry", registryBefore, BuiltInRegistries.ITEM.keySet());
        System.out.println("ACCEPTED_262_WHOLE_LOOT tables=21 bijectiveItems=" + forward.size()
                + " countWeightIdentityPluralControls=5 sourceJar=A7D12A66CA91A6BACCFEFD2CED9D3EC962F5FEB73BF632E1683F99084384F938");
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static JsonObject wholeTable(JsonObject source, Map<String, String> forward, Map<String, String> reverse) {
        var input = source.deepCopy();
        remap(input, forward);
        var decoded = LootTable.DIRECT_CODEC.parse(JsonOps.INSTANCE, input).getOrThrow();
        var encoded = LootTable.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow().getAsJsonObject();
        assertEquals(encoded, LootTable.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE,
                LootTable.DIRECT_CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow()).getOrThrow());
        remap(encoded, reverse);
        return encoded;
    }

    private static void remap(JsonObject table, Map<String, String> identities) {
        visitItems(table, item -> {
            String replacement = identities.get(item.get("name").getAsString());
            if (replacement != null) item.addProperty("name", replacement);
        });
    }

    private static void visitItems(JsonElement value, Consumer<JsonObject> visitor) {
        if (value.isJsonArray()) value.getAsJsonArray().forEach(child -> visitItems(child, visitor));
        else if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            if (object.has("type") && qualify(object.get("type").getAsString()).equals("minecraft:item")) visitor.accept(object);
            object.entrySet().forEach(entry -> visitItems(entry.getValue(), visitor));
        }
    }

    private static JsonObject translateContainer(JsonObject source) {
        var result = source.deepCopy();
        if (result.has("functions")) {
            var modifiers = new JsonArray();
            for (var value : result.remove("functions").getAsJsonArray()) {
                var function = value.getAsJsonObject().deepCopy();
                function.add("type", function.remove("function"));
                if (function.has("count") && function.get("count").isJsonObject()) {
                    var count = function.getAsJsonObject("count");
                    if (!count.has("type") && count.has("min") && count.has("max")) count.addProperty("type", "minecraft:uniform");
                }
                modifiers.add(translateContainer(function));
            }
            result.add("modifier", modifiers);
        }
        if (result.has("conditions")) {
            var terms = new JsonArray();
            for (var value : result.remove("conditions").getAsJsonArray()) {
                var condition = value.getAsJsonObject().deepCopy();
                condition.add("type", condition.remove("condition"));
                terms.add(condition);
            }
            if (terms.size() == 1) result.add("condition", terms.get(0));
            else {
                var all = new JsonObject();
                all.addProperty("type", "minecraft:all_of");
                all.add("terms", terms);
                result.add("condition", all);
            }
        }
        for (String list : new String[] {"pools", "entries", "children"}) {
            if (!result.has(list)) continue;
            var translated = new JsonArray();
            result.getAsJsonArray(list).forEach(value -> translated.add(translateContainer(value.getAsJsonObject())));
            result.add(list, translated);
        }
        return result;
    }

    private static JsonElement normalize(JsonElement value, String role, boolean codecProjection) {
        if (value.isJsonArray()) {
            var array = new JsonArray();
            value.getAsJsonArray().forEach(child -> array.add(normalize(child, role, codecProjection)));
            return array;
        }
        if (!value.isJsonObject()) return value.deepCopy();
        var result = new JsonObject();
        for (var entry : value.getAsJsonObject().entrySet()) {
            String field = entry.getKey();
            JsonElement child = entry.getValue();
            if (role.equals("entry") && ((field.equals("weight") && number(child, 1)) || (field.equals("quality") && number(child, 0)))) continue;
            if (role.equals("pool") && field.equals("bonus_rolls") && number(child, 0)) continue;
            if (role.equals("function") && field.equals("add") && child.isJsonPrimitive()
                    && child.getAsJsonPrimitive().isBoolean() && !child.getAsBoolean()) continue;
            if (role.equals("table") && field.equals("pools") && child.isJsonArray() && child.getAsJsonArray().isEmpty()) continue;
            // The accepted chest's comment is compared before the codec, which does not encode metadata.
            if (codecProjection && role.equals("pool") && field.equals("__comment")) continue;
            String next = switch (field) {
                case "pools" -> "pool";
                case "entries", "children" -> "entry";
                case "modifier" -> "function";
                case "condition", "term", "terms" -> "predicate";
                case "count", "rolls", "bonus_rolls" -> "provider";
                default -> "other";
            };
            JsonElement normalized = normalize(child, next, codecProjection);
            if (field.equals("type") && !role.equals("other")) normalized = new com.google.gson.JsonPrimitive(qualify(child.getAsString()));
            if (field.equals("modifier") && normalized.isJsonArray() && normalized.getAsJsonArray().size() == 1) normalized = normalized.getAsJsonArray().get(0);
            result.add(field, normalized);
        }
        return result;
    }

    private static boolean number(JsonElement value, double expected) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() && value.getAsDouble() == expected;
    }

    private static String qualify(String type) { return type.contains(":") ? type : "minecraft:" + type; }
}
