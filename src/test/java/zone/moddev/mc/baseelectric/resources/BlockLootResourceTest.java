package zone.moddev.mc.baseelectric.resources;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlockLootResourceTest {
    @Test public void everyOrdinaryBlockKeepsItsOwnNativeItemDrop() throws Exception {
        Path root = Paths.get("src/main/resources/data/baseelectric/loot_table/blocks");
        try (Stream<Path> states = Files.list(Paths.get("src/main/resources/assets/baseelectric/blockstates"))) {
            for (Path state : (Iterable<Path>) states.filter(path -> path.toString().endsWith(".json"))::iterator) {
                String id = state.getFileName().toString().replace(".json", "");
                // Two retained asset-only models are not registered blocks.
                if ("electric_pcb_printer".equals(id) || "geothermal_generator".equals(id)) continue;
                if ("li_ore".equals(id) || "sulfur_ore".equals(id) || "pump_pipe_electric".equals(id)) continue;
                Path table = root.resolve(id + ".json");
                assertTrue("Missing native block drop for " + id, Files.isRegularFile(table));
                JsonObject json = read(table);
                assertEquals("minecraft:block", json.get("type").getAsString());
                JsonObject pool = json.getAsJsonArray("pools").get(0).getAsJsonObject();
                assertEquals(1, pool.get("rolls").getAsInt());
                JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
                assertEquals("minecraft:item", entry.get("type").getAsString());
                assertEquals("baseelectric:" + id, entry.get("name").getAsString());
                assertNativeLootCondition(json, pool);
            }
        }
        assertFalse(Files.exists(root.resolve("pump_pipe_electric.json")));
        assertNativeChestLoot();
    }

    @Test public void resourcePackDeclaresTheNativeTwentySixFormat() throws Exception {
        JsonObject pack = read(Paths.get("src/main/resources/pack.mcmeta")).getAsJsonObject("pack");
        assertEquals(JsonParser.parseString("[121,0]"), pack.get("min_format"));
        assertEquals(JsonParser.parseString("[121,0]"), pack.get("max_format"));
        assertFalse(pack.has("pack_format"));
        assertFalse(pack.has("supported_formats"));
        assertFalse(pack.has("forge:server_data_pack_format"));
        assertTrue(java.util.regex.Pattern.compile("(?m)^showAsResourcePack=false\\r?$")
                .matcher(Files.readString(Path.of("src/main/resources/META-INF/mods.toml"))).find());
        zone.moddev.mc.baseelectric.verification.NativeTestBootstrap.initialize();
        var version = net.minecraft.SharedConstants.getCurrentVersion();
        assertEquals(new net.minecraft.server.packs.metadata.pack.PackFormat(97, 1),
                version.packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES));
        assertEquals(new net.minecraft.server.packs.metadata.pack.PackFormat(121, 0),
                version.packVersion(net.minecraft.server.packs.PackType.SERVER_DATA));
        var proof = zone.moddev.mc.baseelectric.verification.NativePackResourceProof.read();
        assertEquals(0, proof.getAsJsonArray("warningsAndErrors").size());
        for (var type : net.minecraft.server.packs.PackType.values()) {
            var side = proof.getAsJsonObject(type.name());
            var metadata = side.getAsJsonObject("nativeMetadata");
            var range = net.minecraft.server.packs.metadata.pack.PackFormat.packCodec(type).codec()
                    .parse(com.mojang.serialization.JsonOps.INSTANCE, metadata).getOrThrow();
            assertTrue(type.toString(), range.isValueInRange(version.packVersion(type)));
            assertEquals("COMPATIBLE", side.get("currentCompatibility").getAsString());
            assertTrue(side.get("resourcesVerified").getAsInt() > 0);
            assertEquals(1, side.get("streamResourceCount").getAsInt());
            assertEquals("net.minecraft.server.packs.PathPackResources", side.get("streamResourceClass").getAsString());
            assertFalse(range.isValueInRange(new net.minecraft.server.packs.metadata.pack.PackFormat(83, 0)));
            assertFalse(range.isValueInRange(new net.minecraft.server.packs.metadata.pack.PackFormat(102, 0)));
            var codec = net.minecraft.server.packs.metadata.pack.PackMetadataSection.forPackType(type).codec();
            var decoded = codec.parse(com.mojang.serialization.JsonOps.INSTANCE, metadata).getOrThrow();
            assertEquals(metadata, codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, decoded).getOrThrow());
        }
        var client = proof.getAsJsonObject("CLIENT_RESOURCES");
        var server = proof.getAsJsonObject("SERVER_DATA");
        assertEquals("mod_resources", client.get("id").getAsString());
        assertEquals("net.minecraftforge.resource.DelegatingPackResources", client.get("resourcesClass").getAsString());
        assertEquals("mod:baseelectric", server.get("id").getAsString());
        assertEquals("net.minecraft.server.packs.PathPackResources", server.get("resourcesClass").getAsString());
        assertEquals("COMPATIBLE", client.get("loaderCompatibility").getAsString());
        assertEquals("COMPATIBLE", server.get("loaderCompatibility").getAsString());
        JsonObject insufficient = pack.deepCopy();
        insufficient.add("min_format", JsonParser.parseString("[97,1]"));
        insufficient.remove("pack_format");
        insufficient.remove("supported_formats");
        var broad = net.minecraft.server.packs.metadata.pack.PackFormat.packCodec(
                net.minecraft.server.packs.PackType.SERVER_DATA).codec()
                .parse(com.mojang.serialization.JsonOps.INSTANCE, insufficient).getOrThrow();
        assertTrue("Native 26 accepts the broad range, so reject it in the exact root contract above",
                broad.isValueInRange(version.packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)));
        assertTrue(broad.isValueInRange(version.packVersion(net.minecraft.server.packs.PackType.SERVER_DATA)));
    }

    @Test public void eachTrackStateUsesTheNativeCutoutRenderLayer() throws Exception {
        Path assets = Paths.get("src/main/resources/assets/baseelectric");
        var variants = read(assets.resolve("blockstates/electric_track.json")).getAsJsonObject("variants");
        assertEquals(64, variants.size());
        for (var variant : variants.entrySet()) {
            String model = variant.getValue().getAsJsonObject().get("model").getAsString();
            assertTrue(variant.getKey(), model.startsWith("baseelectric:block/electric_track_"));
            JsonObject modelJson = read(assets.resolve("models/" + model.substring("baseelectric:".length()) + ".json"));
            assertTrue(model + " does not declare a render layer", modelJson.has("render_type"));
            assertEquals(model, "minecraft:cutout", modelJson.get("render_type").getAsString());
        }
    }

    private static JsonObject read(Path path) throws Exception {
        return new JsonParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static void assertNativeLootCondition(JsonObject table, JsonObject pool) {
        zone.moddev.mc.baseelectric.verification.NativeTestBootstrap.initialize();
        assertFalse("Plural conditions are ignored by 26.3", pool.has("conditions"));
        JsonObject condition = pool.getAsJsonObject("condition");
        assertNotNull(condition);
        assertEquals("minecraft:survives_explosion", condition.get("type").getAsString());
        var codec = net.minecraft.world.level.storage.loot.predicates.LootItemCondition.DIRECT_CODEC;
        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        var predicate = codec.parse(ops, condition).getOrThrow();
        assertEquals(condition, codec.encodeStart(ops, predicate).getOrThrow());

        // Native syntax proof uses a vanilla item; registered Electric items remain a GameTest gate.
        JsonObject syntax = table.deepCopy();
        syntax.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                .get(0).getAsJsonObject().addProperty("name", "minecraft:stone");
        var tableCodec = net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC;
        var nativeTable = tableCodec.parse(ops, syntax).getOrThrow();
        var encoded = tableCodec.encodeStart(ops, nativeTable).getOrThrow().getAsJsonObject();
        assertEquals(condition, encoded.getAsJsonArray("pools").get(0).getAsJsonObject().get("condition"));

        JsonObject obsolete = syntax.deepCopy();
        var oldPool = obsolete.getAsJsonArray("pools").get(0).getAsJsonObject();
        var oldConditions = new com.google.gson.JsonArray();
        oldConditions.add(oldPool.remove("condition"));
        oldPool.add("conditions", oldConditions);
        var ignored = tableCodec.parse(ops, obsolete).getOrThrow();
        var ignoredWire = tableCodec.encodeStart(ops, ignored).getOrThrow().getAsJsonObject();
        assertFalse("Old plural field must not accidentally qualify explosion gating",
                ignoredWire.getAsJsonArray("pools").get(0).getAsJsonObject().has("condition"));
    }

    private static void assertNativeChestLoot() throws Exception {
        JsonObject table = read(Path.of("src/main/resources/data/baseelectric/loot_table/chests/advantage_parts.json"));
        JsonObject pool = table.getAsJsonArray("pools").get(0).getAsJsonObject();
        var entries = pool.getAsJsonArray("entries");
        assertEquals(10, entries.size());
        assertEquals(144, entries.get(0).getAsJsonObject().get("weight").getAsInt());
        assertEquals(48, entries.asList().stream().skip(1)
                .mapToInt(entry -> entry.getAsJsonObject().get("weight").getAsInt()).sum());
        JsonObject syntax = table.deepCopy();
        var syntaxEntries = syntax.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        for (int i = 1; i < entries.size(); i++) {
            JsonObject entry = entries.get(i).getAsJsonObject();
            assertFalse(entry.has("functions"));
            assertEquals("minecraft:set_count", entry.getAsJsonObject("modifier").get("type").getAsString());
            syntaxEntries.get(i).getAsJsonObject().addProperty("name", "minecraft:stone");
        }
        var codec = net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC;
        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        var decoded = codec.parse(ops, syntax).getOrThrow();
        var encoded = codec.encodeStart(ops, decoded).getOrThrow().getAsJsonObject()
                .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        for (int i = 1; i < entries.size(); i++) {
            assertEquals("Native chest modifiers must retain their count ranges", entries.get(i).getAsJsonObject().get("modifier"),
                    encoded.get(i).getAsJsonObject().get("modifier"));
            var wire = encoded.get(i).getAsJsonObject();
            int weight = wire.has("weight") ? wire.get("weight").getAsInt()
                    : net.minecraft.world.level.storage.loot.entries.UniformContainerBase.DEFAULT_WEIGHT;
            assertEquals(entries.get(i).getAsJsonObject().get("weight").getAsInt(), weight);
        }
    }
}
