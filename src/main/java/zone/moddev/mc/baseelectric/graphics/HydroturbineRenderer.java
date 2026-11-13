package zone.moddev.mc.baseelectric.graphics;

import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.entities.HydroturbineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
public class HydroturbineRenderer extends EntityRenderer<HydroturbineEntity, HydroturbineRenderer.State>{
	private static final Identifier texture = Identifier.parse(ElectricAdvantage.MODID+":textures/entity/hydroturbine.png");

	private static final float DEGREES_TO_RADIANS = (float)(Math.PI / 180);

	public HydroturbineRenderer(final EntityRendererProvider.Context rm) {
		super(rm);
	}

    public static final class State extends EntityRenderState {
        float yaw;
        float rotation;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(HydroturbineEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yaw = entity.getYRot();
        state.rotation = entity.rotation + (entity.isSpinning ? HydroturbineEntity.DEGREES_PER_TICK * partialTick : 0);
    }

    @Override public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
        matrices.pushPose();
        matrices.translate(0f, 0.5f, 0f);
        matrices.rotate(Axis.YP.rotationDegrees(state.yaw));
        float rotation = state.rotation;
        int light = state.lightCoords;
        collector.submitCustomGeometry(matrices, RenderTypes.entitySolid(texture),
                (pose, vertices) -> draw(rotation, pose, vertices, light));
        matrices.popPose();
        super.submit(state, matrices, collector, camera);
    }

    private static void draw(float rotation, PoseStack.Pose pose,
            com.mojang.blaze3d.vertex.VertexConsumer vertices, int packedLight) {
		{
			final float pixel = 1.0f / 16;
			final float topHeight = 0.5f;
			final float bottomHeight = 0.375f-0.5f;
			final float radius = pixel;
			final float shaftMinU = 0.5f;
			final float shaftMinV = 0.5f;
			final float shaftMaxU = 0.5f+2*pixel;
			final float shaftMaxV = 1.0f;
			
            final BufferedQuads worldRenderer = new BufferedQuads(pose, vertices, packedLight, OverlayTexture.NO_OVERLAY);

			// render shaft
			worldRenderer.pos(+radius, bottomHeight, -radius).tex(shaftMinU,shaftMinV).endVertex();
			worldRenderer.pos(-radius, bottomHeight, -radius).tex(shaftMaxU,shaftMinV).endVertex();
			worldRenderer.pos(-radius, topHeight   , -radius).tex(shaftMaxU,shaftMaxV).endVertex();
			worldRenderer.pos(+radius, topHeight   , -radius).tex(shaftMinU,shaftMaxV).endVertex();

			worldRenderer.pos(-radius, bottomHeight, +radius).tex(shaftMinU,shaftMinV).endVertex();
			worldRenderer.pos(+radius, bottomHeight, +radius).tex(shaftMaxU,shaftMinV).endVertex();
			worldRenderer.pos(+radius, topHeight   , +radius).tex(shaftMaxU,shaftMaxV).endVertex();
			worldRenderer.pos(-radius, topHeight   , +radius).tex(shaftMinU,shaftMaxV).endVertex();

			worldRenderer.pos(-radius, bottomHeight, -radius).tex(shaftMinU,shaftMinV).endVertex();
			worldRenderer.pos(-radius, bottomHeight, +radius).tex(shaftMaxU,shaftMinV).endVertex();
			worldRenderer.pos(-radius, topHeight   , +radius).tex(shaftMaxU,shaftMaxV).endVertex();
			worldRenderer.pos(-radius, topHeight   , -radius).tex(shaftMinU,shaftMaxV).endVertex();

			worldRenderer.pos(+radius, bottomHeight, +radius).tex(shaftMinU,shaftMinV).endVertex();
			worldRenderer.pos(+radius, bottomHeight, -radius).tex(shaftMaxU,shaftMinV).endVertex();
			worldRenderer.pos(+radius, topHeight   , -radius).tex(shaftMaxU,shaftMaxV).endVertex();
			worldRenderer.pos(+radius, topHeight   , +radius).tex(shaftMinU,shaftMaxV).endVertex();

			worldRenderer.pos(-radius, bottomHeight, +radius).tex(shaftMinU,shaftMinV).endVertex();
			worldRenderer.pos(-radius, bottomHeight, -radius).tex(shaftMinU+2*pixel,shaftMinV).endVertex();
			worldRenderer.pos(+radius, bottomHeight, -radius).tex(shaftMinU+2*pixel,shaftMaxV).endVertex();
			worldRenderer.pos(+radius, bottomHeight, +radius).tex(shaftMinU,shaftMaxV).endVertex();
			
			// render axel
			final float axelLength = 4 * pixel;
			final float axelRadius = 1 * pixel;
			final float axelMinU = 0;
			final float axelMaxU = 4 * pixel;
			final float axelMaxU2 = 2 * pixel;
			final float axelMinV = 0.5f;
			final float axelMaxV = 0.5f + 2 * pixel;
			final float axelMaxV2 = 0.5f + 4 * pixel;
			
			worldRenderer.pos(+axelRadius, -axelRadius, -pixel           ).tex(axelMinU,axelMinV).endVertex();
			worldRenderer.pos(+axelRadius, -axelRadius, -pixel-axelLength).tex(axelMaxU,axelMinV).endVertex();
			worldRenderer.pos(+axelRadius, +axelRadius, -pixel-axelLength).tex(axelMaxU,axelMaxV).endVertex();
			worldRenderer.pos(+axelRadius, +axelRadius, -pixel           ).tex(axelMinU,axelMaxV).endVertex();
			
			worldRenderer.pos(-axelRadius, +axelRadius, -pixel           ).tex(axelMinU,axelMinV).endVertex();
			worldRenderer.pos(-axelRadius, +axelRadius, -pixel-axelLength).tex(axelMaxU,axelMinV).endVertex();
			worldRenderer.pos(-axelRadius, -axelRadius, -pixel-axelLength).tex(axelMaxU,axelMaxV).endVertex();
			worldRenderer.pos(-axelRadius, -axelRadius, -pixel           ).tex(axelMinU,axelMaxV).endVertex();
			
			worldRenderer.pos(+axelRadius, +axelRadius, -pixel           ).tex(axelMinU,axelMaxV2).endVertex();
			worldRenderer.pos(+axelRadius, +axelRadius, -pixel-axelLength).tex(axelMinU,axelMinV).endVertex();
			worldRenderer.pos(-axelRadius, +axelRadius, -pixel-axelLength).tex(axelMaxU2,axelMinV).endVertex();
			worldRenderer.pos(-axelRadius, +axelRadius, -pixel           ).tex(axelMaxU2,axelMaxV2).endVertex();
			
			worldRenderer.pos(-axelRadius, -axelRadius, -pixel           ).tex(axelMinU,axelMaxV2).endVertex();
			worldRenderer.pos(-axelRadius, -axelRadius, -pixel-axelLength).tex(axelMinU,axelMinV).endVertex();
			worldRenderer.pos(+axelRadius, -axelRadius, -pixel-axelLength).tex(axelMaxU2,axelMinV).endVertex();
			worldRenderer.pos(+axelRadius, -axelRadius, -pixel           ).tex(axelMaxU2,axelMaxV2).endVertex();
			
			worldRenderer.pos(-axelRadius, -axelRadius, -pixel-axelLength).tex(axelMinU,axelMaxV2).endVertex();
			worldRenderer.pos(-axelRadius, +axelRadius, -pixel-axelLength).tex(axelMinU,axelMinV).endVertex();
			worldRenderer.pos(+axelRadius, +axelRadius, -pixel-axelLength).tex(axelMaxU2,axelMinV).endVertex();
			worldRenderer.pos(+axelRadius, -axelRadius, -pixel-axelLength).tex(axelMaxU2,axelMaxV2).endVertex();
			
			// render spinning (or not) blades
			float spin1, spin2, spin3;
            spin1 = rotation;
            spin2 = spin1 + 120;
            spin3 = spin1 - 120;
			final float bladeWidth = 3*pixel;
			final float bladeLength = 6*pixel;
			final float bladeOffset = 1*pixel;
			final float bladeMinU = 0.0f;
			final float bladeMinV = 0.0f;
			final float bladeMaxU = bladeWidth;
			final float bladeMaxV = bladeLength;
			final float depthDelta = pixel;

			final float rotaryWidth = DEGREES_TO_RADIANS*45;
			final float t1 = DEGREES_TO_RADIANS*spin1, t2 = DEGREES_TO_RADIANS*spin2, t3 = DEGREES_TO_RADIANS*spin3;
			final float sin1a = Mth.sin(t1);
			final float sin1b = Mth.sin(t1+rotaryWidth);
			final float cos1a = Mth.cos(t1);
			final float cos1b = Mth.cos(t1+rotaryWidth);
			final float sin2a = Mth.sin(t2);
			final float sin2b = Mth.sin(t2+rotaryWidth);
			final float cos2a = Mth.cos(t2);
			final float cos2b = Mth.cos(t2+rotaryWidth);
			final float sin3a = Mth.sin(t3);
			final float sin3b = Mth.sin(t3+rotaryWidth);
			final float cos3a = Mth.cos(t3);
			final float cos3b = Mth.cos(t3+rotaryWidth);

			worldRenderer.pos(cos1b*bladeOffset, sin1b*bladeOffset, depthDelta-axelLength).tex(bladeMinU,bladeMinV).endVertex(); // inner, leading
			worldRenderer.pos(cos1b*bladeLength, sin1b*bladeLength, depthDelta-axelLength).tex(bladeMinU,bladeMaxV).endVertex(); // outer, leading
			worldRenderer.pos(cos1a*bladeLength, sin1a*bladeLength,           -axelLength).tex(bladeMaxU,bladeMaxV).endVertex(); // outer, trailing
			worldRenderer.pos(cos1a*bladeOffset, sin1a*bladeOffset,           -axelLength).tex(bladeMaxU,bladeMinV).endVertex(); // inner, trailing

			worldRenderer.pos(cos1a*bladeOffset, sin1a*bladeOffset,           -axelLength).tex(bladeMaxU,bladeMinV).endVertex(); // inner, trailing
			worldRenderer.pos(cos1a*bladeLength, sin1a*bladeLength,           -axelLength).tex(bladeMaxU,bladeMaxV).endVertex(); // outer, trailing
			worldRenderer.pos(cos1b*bladeLength, sin1b*bladeLength, depthDelta-axelLength).tex(bladeMinU,bladeMaxV).endVertex(); // outer, leading
			worldRenderer.pos(cos1b*bladeOffset, sin1b*bladeOffset, depthDelta-axelLength).tex(bladeMinU,bladeMinV).endVertex(); // inner, leading


			worldRenderer.pos(cos2b*bladeOffset, sin2b*bladeOffset, depthDelta-axelLength).tex(bladeMinU,bladeMinV).endVertex(); // inner, leading
			worldRenderer.pos(cos2b*bladeLength, sin2b*bladeLength, depthDelta-axelLength).tex(bladeMinU,bladeMaxV).endVertex(); // outer, leading
			worldRenderer.pos(cos2a*bladeLength, sin2a*bladeLength,           -axelLength).tex(bladeMaxU,bladeMaxV).endVertex(); // outer, trailing
			worldRenderer.pos(cos2a*bladeOffset, sin2a*bladeOffset,           -axelLength).tex(bladeMaxU,bladeMinV).endVertex(); // inner, trailing

			worldRenderer.pos(cos2a*bladeOffset, sin2a*bladeOffset,           -axelLength).tex(bladeMaxU,bladeMinV).endVertex(); // inner, trailing
			worldRenderer.pos(cos2a*bladeLength, sin2a*bladeLength,           -axelLength).tex(bladeMaxU,bladeMaxV).endVertex(); // outer, trailing
			worldRenderer.pos(cos2b*bladeLength, sin2b*bladeLength, depthDelta-axelLength).tex(bladeMinU,bladeMaxV).endVertex(); // outer, leading
			worldRenderer.pos(cos2b*bladeOffset, sin2b*bladeOffset, depthDelta-axelLength).tex(bladeMinU,bladeMinV).endVertex(); // inner, leading


			worldRenderer.pos(cos3b*bladeOffset, sin3b*bladeOffset, depthDelta-axelLength).tex(bladeMinU,bladeMinV).endVertex(); // inner, leading
			worldRenderer.pos(cos3b*bladeLength, sin3b*bladeLength, depthDelta-axelLength).tex(bladeMinU,bladeMaxV).endVertex(); // outer, leading
			worldRenderer.pos(cos3a*bladeLength, sin3a*bladeLength,           -axelLength).tex(bladeMaxU,bladeMaxV).endVertex(); // outer, trailing
			worldRenderer.pos(cos3a*bladeOffset, sin3a*bladeOffset,           -axelLength).tex(bladeMaxU,bladeMinV).endVertex(); // inner, trailing

			worldRenderer.pos(cos3a*bladeOffset, sin3a*bladeOffset,           -axelLength).tex(bladeMaxU,bladeMinV).endVertex(); // inner, trailing
			worldRenderer.pos(cos3a*bladeLength, sin3a*bladeLength,           -axelLength).tex(bladeMaxU,bladeMaxV).endVertex(); // outer, trailing
			worldRenderer.pos(cos3b*bladeLength, sin3b*bladeLength, depthDelta-axelLength).tex(bladeMinU,bladeMaxV).endVertex(); // outer, leading
			worldRenderer.pos(cos3b*bladeOffset, sin3b*bladeOffset, depthDelta-axelLength).tex(bladeMinU,bladeMinV).endVertex(); // inner, leading
			
			
		}
	}


}
