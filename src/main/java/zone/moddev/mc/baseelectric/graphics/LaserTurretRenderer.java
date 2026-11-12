package zone.moddev.mc.baseelectric.graphics;

import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.machines.LaserTurretTileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;
public class LaserTurretRenderer implements BlockEntityRenderer<LaserTurretTileEntity, LaserTurretRenderer.State>{

	
	private final Identifier texture = Identifier.parse(ElectricAdvantage.MODID+":textures/entity/laser_turret.png");
	private final RenderType renderType = ElectricRenderTypes.unshaded(texture, true);
	
	public LaserTurretRenderer(BlockEntityRendererProvider.Context context) {
	}

	
	public static final class State extends BlockEntityRenderState {
        boolean active;
        boolean showBeam;
        double length;
        float yaw;
        float pitch;
    }

    @Override public State createRenderState() { return new State(); }

    private static void capture(LaserTurretTileEntity entity, State state, float partialTick) {
        state.active = entity.isActive();
        state.showBeam = entity.showLaserLine();
        state.length = entity.laserBlastLength;
        state.yaw = -((1 - partialTick) * entity.rotOldYaw + partialTick * entity.rotYaw) - 90;
        state.pitch = (1 - partialTick) * entity.rotOldPitch + partialTick * entity.rotPitch;
    }

    @Override public void extractRenderState(LaserTurretTileEntity entity, State state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breaking);
        capture(entity, state, partialTick);
    }

    @Override public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
        State snapshot = new State();
        snapshot.active = state.active;
        snapshot.showBeam = state.showBeam;
        snapshot.length = state.length;
        transform(matrices, state.yaw, state.pitch);
        int light = state.lightCoords;
        collector.submitCustomGeometry(matrices, renderType,
                (pose, vertices) -> draw(snapshot, pose, vertices, light, OverlayTexture.NO_OVERLAY));
        matrices.popPose();
    }

	public void render(final LaserTurretTileEntity e, final float partialTick, final PoseStack matrices,
            final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
        State state = new State();
        capture(e, state, partialTick);
        transform(matrices, state.yaw, state.pitch);
        draw(state, matrices.last(), buffers.getBuffer(renderType), packedLight, packedOverlay);
        matrices.popPose();
    }

    private static void transform(PoseStack matrices, float yaw, float pitch) {
        matrices.pushPose();
        matrices.translate(0.5f, 0.75f, 0.5f);
        matrices.mulPose(Axis.YP.rotationDegrees(yaw));
        matrices.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    private static void draw(State state, PoseStack.Pose pose,
            com.mojang.blaze3d.vertex.VertexConsumer vertices, int packedLight, int packedOverlay) {
		final float sideU0 = 0;
		final float sideU1 = 0.5f;
		final float sideV0 = 0;
		final float sideV1 = 0.5f;
		final float endU0 = 0.5f;
		final float endU1 = 1;
		final float endV0;
		final float endV1;
		if(state.active){
			endV0 = 0;
			endV1 = 0.5f;
		}else{
			endV0 = 0.5f;
			endV1 = 1.0f;
		}

		final float laserU0 = 0;
		final float laserU1 = 0.5f;
		final float laserV0 = 0.5f;
		final float laserV1 = 1.0f;

		final float radius = 0.25f;
		final float laserRadius = 0.125f;
		

        BufferedQuads worldRenderer = new BufferedQuads(pose, vertices, packedLight, packedOverlay);
		
		worldRenderer.pos( radius, radius, -radius).tex(endU0, endV0).endVertex();
		worldRenderer.pos( radius,-radius, -radius).tex(endU0, endV1).endVertex();
		worldRenderer.pos(-radius,-radius, -radius).tex(endU1, endV1).endVertex();
		worldRenderer.pos(-radius, radius, -radius).tex(endU1, endV0).endVertex();

		worldRenderer.pos(-radius, radius,  radius).tex(sideU0, sideV0).endVertex();
		worldRenderer.pos(-radius,-radius,  radius).tex(sideU0, sideV1).endVertex();
		worldRenderer.pos( radius,-radius,  radius).tex(sideU1, sideV1).endVertex();
		worldRenderer.pos( radius, radius,  radius).tex(sideU1, sideV0).endVertex();

		worldRenderer.pos(-radius, radius, -radius).tex(sideU0, sideV0).endVertex();
		worldRenderer.pos(-radius,-radius, -radius).tex(sideU0, sideV1).endVertex();
		worldRenderer.pos(-radius,-radius,  radius).tex(sideU1, sideV1).endVertex();
		worldRenderer.pos(-radius, radius,  radius).tex(sideU1, sideV0).endVertex();

		worldRenderer.pos( radius, radius,  radius).tex(sideU0, sideV0).endVertex();
		worldRenderer.pos( radius,-radius,  radius).tex(sideU0, sideV1).endVertex();
		worldRenderer.pos( radius,-radius, -radius).tex(sideU1, sideV1).endVertex();
		worldRenderer.pos( radius, radius, -radius).tex(sideU1, sideV0).endVertex();

		worldRenderer.pos(-radius, radius, -radius).tex(sideU0, sideV0).endVertex();
		worldRenderer.pos(-radius, radius,  radius).tex(sideU0, sideV1).endVertex();
		worldRenderer.pos( radius, radius,  radius).tex(sideU1, sideV1).endVertex();
		worldRenderer.pos( radius, radius, -radius).tex(sideU1, sideV0).endVertex();

		worldRenderer.pos(-radius,-radius,  radius).tex(sideU0, sideV0).endVertex();
		worldRenderer.pos(-radius,-radius, -radius).tex(sideU0, sideV1).endVertex();
		worldRenderer.pos( radius,-radius, -radius).tex(sideU1, sideV1).endVertex();
		worldRenderer.pos( radius,-radius,  radius).tex(sideU1, sideV0).endVertex();

		if(state.showBeam && state.length > 0){
			final double x1, y1, z1, x2, y2, z2,  x3, y3, z3, x4, y4, z4;
			x1 = 0;
			y1 = -laserRadius;
			z1 = 0;
			x2 = 0;
			y2 = laserRadius;
			z2 = -state.length;
			
			x3 = -laserRadius;
			y3 = 0;
			z3 = z1;
			x4 = laserRadius;
			y4 = 0;
			z4 = z2;
			
            worldRenderer = new BufferedQuads(pose, vertices, 15 << 20 | 15 << 4, packedOverlay);
			
			worldRenderer.pos( x1, y1, z1).tex(laserU0, laserV1).endVertex();
			worldRenderer.pos( x2, y1, z2).tex(laserU1, laserV1).endVertex();
			worldRenderer.pos( x2, y2, z2).tex(laserU1, laserV0).endVertex();
			worldRenderer.pos( x1, y2, z1).tex(laserU0, laserV0).endVertex();
			
			worldRenderer.pos( x1, y2, z1).tex(laserU0, laserV1).endVertex();
			worldRenderer.pos( x2, y2, z2).tex(laserU1, laserV1).endVertex();
			worldRenderer.pos( x2, y1, z2).tex(laserU1, laserV0).endVertex();
			worldRenderer.pos( x1, y1, z1).tex(laserU0, laserV0).endVertex();


			worldRenderer.pos( x3, y3, z3).tex(laserU0, laserV1).endVertex();
			worldRenderer.pos( x3, y4, z4).tex(laserU1, laserV1).endVertex();
			worldRenderer.pos( x4, y4, z4).tex(laserU1, laserV0).endVertex();
			worldRenderer.pos( x4, y3, z3).tex(laserU0, laserV0).endVertex();
			
			worldRenderer.pos( x4, y3, z3).tex(laserU0, laserV1).endVertex();
			worldRenderer.pos( x4, y4, z4).tex(laserU1, laserV1).endVertex();
			worldRenderer.pos( x3, y4, z4).tex(laserU1, laserV0).endVertex();
			worldRenderer.pos( x3, y3, z3).tex(laserU0, laserV0).endVertex();
			
		}

	}

	
}
