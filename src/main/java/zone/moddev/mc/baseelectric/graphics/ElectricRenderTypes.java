package zone.moddev.mc.baseelectric.graphics;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** Native entity shaders retain overlay, lightmap and fog without diffuse shading. */
final class ElectricRenderTypes {
    private static final RenderPipeline OPAQUE = pipeline(false);
    private static final RenderPipeline TRANSLUCENT = pipeline(true);

    private ElectricRenderTypes() {}

    private static RenderPipeline pipeline(boolean translucent) {
        var builder = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("baseelectric",
                        translucent ? "pipeline/unshaded_translucent" : "pipeline/unshaded"))
                .withShaderDefine("NO_CARDINAL_LIGHTING")
                .withBindGroupLayout(BindGroupLayouts.SAMPLER1).withCull(false);
        if (translucent) builder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        RenderPipeline pipeline = builder.build();
        RenderPipelines.PIPELINES_BY_LOCATION.put(pipeline.getLocation(), pipeline);
        return pipeline;
    }

    static RenderType unshaded(Identifier texture, boolean translucent) {
        var setup = RenderSetup.builder(translucent ? TRANSLUCENT : OPAQUE)
                .withTexture("Sampler0", texture).useLightmap().useOverlay();
        if (translucent) setup.sortOnUpload();
        return RenderType.create("baseelectric_unshaded", setup.createRenderSetup());
    }
}
