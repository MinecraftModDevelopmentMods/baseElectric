package zone.moddev.mc.electricadvantage.graphics;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;

/** Scoped render state replaces the old global lighting/blending mutations. */
final class ElectricRenderTypes extends RenderStateShard {
    static final ResourceLocation UNSHADED = new ResourceLocation(
            zone.moddev.mc.electricadvantage.ElectricAdvantage.MODID, "unshaded");
    private static ShaderInstance unshadedShader;
    private ElectricRenderTypes() {
        super("electricadvantage", () -> {}, () -> {});
    }

    static void setUnshadedShader(ShaderInstance shader) {
        unshadedShader = shader;
    }

    static RenderType unshaded(ResourceLocation texture, boolean translucent) {
        return RenderType.create("electricadvantage_unshaded", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, translucent, RenderType.CompositeState.builder()
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                        .setShaderState(new ShaderStateShard(() -> unshadedShader)).setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
    }
}
