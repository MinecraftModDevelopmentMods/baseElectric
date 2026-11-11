package zone.moddev.mc.baseelectric.graphics;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;

/** Scoped render state replaces the old global lighting/blending mutations. */
final class ElectricRenderTypes extends RenderStateShard {
    static final ResourceLocation UNSHADED = ResourceLocation.fromNamespaceAndPath(zone.moddev.mc.baseelectric.ElectricAdvantage.MODID, "unshaded");
    private static ShaderInstance unshadedShader;
    private ElectricRenderTypes() {
        super("baseelectric", () -> {}, () -> {});
    }

    static void setUnshadedShader(ShaderInstance shader) {
        unshadedShader = shader;
    }

    static RenderType unshaded(ResourceLocation texture, boolean translucent) {
        return RenderType.create("baseelectric_unshaded", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, translucent, RenderType.CompositeState.builder()
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                        .setShaderState(new ShaderStateShard(() -> unshadedShader)).setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
    }
}
