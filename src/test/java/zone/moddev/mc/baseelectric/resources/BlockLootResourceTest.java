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
            }
        }
        assertFalse(Files.exists(root.resolve("pump_pipe_electric.json")));
    }

    @Test public void resourcePackDeclaresTheNativeOneTwentyOneFormat() throws Exception {
        JsonObject pack = read(Paths.get("src/main/resources/pack.mcmeta")).getAsJsonObject("pack");
        assertEquals(JsonParser.parseString("[94,1]"), pack.get("min_format"));
        assertEquals(JsonParser.parseString("[94,1]"), pack.get("max_format"));
        assertFalse(pack.has("pack_format"));
        assertFalse(pack.has("supported_formats"));
        assertFalse(pack.has("forge:server_data_pack_format"));
        assertTrue(java.util.regex.Pattern.compile("(?m)^showAsResourcePack=false\\r?$")
                .matcher(Files.readString(Path.of("src/main/resources/META-INF/mods.toml"))).find());
        zone.moddev.mc.baseelectric.verification.NativeTestBootstrap.initialize();
        var version = net.minecraft.SharedConstants.getCurrentVersion();
        assertEquals(new net.minecraft.server.packs.metadata.pack.PackFormat(75, 0),
                version.packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES));
        assertEquals(new net.minecraft.server.packs.metadata.pack.PackFormat(94, 1),
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
            assertFalse(range.isValueInRange(new net.minecraft.server.packs.metadata.pack.PackFormat(74, 0)));
            assertFalse(range.isValueInRange(new net.minecraft.server.packs.metadata.pack.PackFormat(95, 0)));
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
        assertEquals("TOO_NEW", server.get("loaderCompatibility").getAsString());
        JsonObject insufficient = pack.deepCopy();
        insufficient.add("min_format", JsonParser.parseString("[75,0]"));
        insufficient.remove("pack_format");
        insufficient.remove("supported_formats");
        assertTrue(net.minecraft.server.packs.metadata.pack.PackFormat.packCodec(
                net.minecraft.server.packs.PackType.SERVER_DATA).codec()
                .parse(com.mojang.serialization.JsonOps.INSTANCE, insufficient).error().isPresent());
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
}
