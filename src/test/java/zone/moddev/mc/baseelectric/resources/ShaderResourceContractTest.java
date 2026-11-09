package zone.moddev.mc.baseelectric.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

/** Resource contracts do not prove GPU shader compilation or visual acceptance. */
public class ShaderResourceContractTest {
    private static final Path ROOT = Path.of("src/main/resources/assets/baseelectric/shaders/core");

    @Test public void nativeShaderRetainsTheEntityVertexLayoutAndVanillaFogFragment() throws Exception {
        JsonObject json = new JsonParser().parse(Files.readString(ROOT.resolve("unshaded.json"))).getAsJsonObject();
        assertEquals("baseelectric:unshaded", json.get("vertex").getAsString());
        assertEquals("minecraft:rendertype_entity_translucent", json.get("fragment").getAsString());
        assertEquals(Arrays.asList("Position", "Color", "UV0", "UV1", "UV2", "Normal"),
                java.util.stream.StreamSupport.stream(json.getAsJsonArray("attributes").spliterator(), false)
                        .map(value -> value.getAsString()).collect(Collectors.toList()));
        assertNotNull(getClass().getResource("/assets/minecraft/shaders/core/rendertype_entity_translucent.fsh"));
        var uniformNames = java.util.stream.StreamSupport.stream(json.getAsJsonArray("uniforms").spliterator(), false)
                .map(value -> value.getAsJsonObject().get("name").getAsString()).collect(Collectors.toSet());
        assertTrue(uniformNames.containsAll(java.util.Set.of("IViewRotMat", "FogShape")));
    }

    @Test public void noDiffuseLightingDoesNotRemoveLightmapOrOverlaySampling() throws Exception {
        String vertex = Files.readString(ROOT.resolve("unshaded.vsh"));
        assertTrue(vertex.contains("vertexColor = Color;"));
        assertTrue(vertex.contains("lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);"));
        assertTrue(vertex.contains("overlayColor = texelFetch(Sampler1, UV1, 0);"));
        assertFalse(vertex.contains("minecraft_mix_light"));
        assertTrue(vertex.contains("#moj_import <fog.glsl>"));
        String fog = "vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);";
        assertTrue(vertex.contains(fog));
        try (var vanilla = getClass().getResourceAsStream("/assets/minecraft/shaders/core/rendertype_entity_translucent.vsh")) {
            assertNotNull(vanilla);
            assertTrue(new String(vanilla.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains(fog));
        }
    }
}
