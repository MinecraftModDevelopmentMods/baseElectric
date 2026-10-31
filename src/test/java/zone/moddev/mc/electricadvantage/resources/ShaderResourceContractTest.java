package zone.moddev.mc.electricadvantage.resources;

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
    private static final Path ROOT = Path.of("src/main/resources/assets/electricadvantage/shaders/core");

    @Test public void nativeShaderRetainsTheEntityVertexLayoutAndVanillaFogFragment() throws Exception {
        JsonObject json = new JsonParser().parse(Files.readString(ROOT.resolve("unshaded.json"))).getAsJsonObject();
        assertEquals("electricadvantage:unshaded", json.get("vertex").getAsString());
        assertEquals("minecraft:rendertype_entity_translucent", json.get("fragment").getAsString());
        assertEquals(Arrays.asList("Position", "Color", "UV0", "UV1", "UV2", "Normal"),
                java.util.stream.StreamSupport.stream(json.getAsJsonArray("attributes").spliterator(), false)
                        .map(value -> value.getAsString()).collect(Collectors.toList()));
        assertNotNull(getClass().getResource("/assets/minecraft/shaders/core/rendertype_entity_translucent.fsh"));
    }

    @Test public void noDiffuseLightingDoesNotRemoveLightmapOrOverlaySampling() throws Exception {
        String vertex = Files.readString(ROOT.resolve("unshaded.vsh"));
        assertTrue(vertex.contains("vertexColor = Color;"));
        assertTrue(vertex.contains("lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);"));
        assertTrue(vertex.contains("overlayColor = texelFetch(Sampler1, UV1, 0);"));
        assertFalse(vertex.contains("minecraft_mix_light"));
        assertTrue(vertex.contains("vertexDistance = length(viewPosition.xyz);"));
    }
}
