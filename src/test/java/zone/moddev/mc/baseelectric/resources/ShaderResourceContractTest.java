package zone.moddev.mc.baseelectric.resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Resource contracts do not prove GPU shader compilation or visual acceptance. */
public class ShaderResourceContractTest {
    private static final Path PIPELINES = Path.of(
            "src/main/java/zone/moddev/mc/baseelectric/graphics/ElectricRenderTypes.java");

    @Test public void nativeShaderRetainsTheEntityVertexLayoutAndVanillaFogFragment() throws Exception {
        String pipeline = Files.readString(PIPELINES);
        assertTrue(pipeline.contains("RenderPipelines.ENTITY_SNIPPET"));
        assertTrue(pipeline.contains("withShaderDefine(\"NO_CARDINAL_LIGHTING\")"));
        assertTrue(pipeline.contains("withSampler(\"Sampler1\")"));
        assertTrue(pipeline.contains("withCull(false)"));
        assertTrue(pipeline.contains("BlendFunction.TRANSLUCENT"));
        assertFalse(pipeline.contains("ShaderInstance"));
        assertFalse(pipeline.contains("EMISSIVE"));
        assertNotNull(getClass().getResource("/assets/minecraft/shaders/core/entity.fsh"));
        String vertex = vanilla("entity.vsh");
        for (String attribute : Arrays.asList("Position", "Color", "UV0", "UV1", "UV2", "Normal"))
            assertTrue(attribute, vertex.contains(" " + attribute + ";"));
        assertTrue(vertex.contains("dynamictransforms.glsl"));
        assertTrue(vertex.contains("projection.glsl"));
        assertFalse(vertex.contains("IViewRotMat"));
        for (String renderer : Arrays.asList("HydroturbineRenderer", "LaserDrillRenderer", "LaserTurretRenderer")) {
            String source = Files.readString(PIPELINES.resolveSibling(renderer + ".java"));
            assertFalse("Forge 64 warns about mod-owned class annotations: " + renderer, source.contains("@OnlyIn"));
        }
        String registration = Files.readString(PIPELINES.resolveSibling("ClientRegistration.java"));
        assertTrue(registration.contains("value = Dist.CLIENT"));
    }

    @Test public void noDiffuseLightingDoesNotRemoveLightmapOrOverlaySampling() throws Exception {
        String vertex = vanilla("entity.vsh");
        assertTrue(vertex.contains("vertexColor = Color;"));
        assertTrue(vertex.contains("#moj_import <minecraft:sample_lightmap.glsl>"));
        assertTrue(vertex.contains("lightMapColor = sample_lightmap(Sampler2, UV2);"));
        assertTrue(vertex.contains("overlayColor = texelFetch(Sampler1, UV1, 0);"));
        assertTrue(vertex.contains("#elif defined(NO_CARDINAL_LIGHTING)\n    vertexColor = Color;"));
        assertTrue(vertex.contains("#moj_import <minecraft:fog.glsl>"));
        assertTrue(vertex.contains("sphericalVertexDistance = fog_spherical_distance(Position);"));
        assertTrue(vertex.contains("cylindricalVertexDistance = fog_cylindrical_distance(Position);"));
        String fragment = vanilla("entity.fsh");
        assertTrue(fragment.contains("color *= lightMapColor;"));
        assertTrue(fragment.contains("mix(overlayColor.rgb, color.rgb, overlayColor.a)"));
        assertTrue(fragment.contains("apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance"));
        String pipeline = Files.readString(PIPELINES);
        assertTrue(pipeline.contains(".useLightmap().useOverlay()"));
        assertFalse(pipeline.contains("NO_OVERLAY"));
    }

    private static String vanilla(String name) throws Exception {
        try (var source = ShaderResourceContractTest.class.getResourceAsStream("/assets/minecraft/shaders/core/" + name)) {
            assertNotNull(source);
            return new String(source.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
