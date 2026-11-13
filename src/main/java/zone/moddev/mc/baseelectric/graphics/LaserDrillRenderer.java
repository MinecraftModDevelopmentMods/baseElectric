package zone.moddev.mc.baseelectric.graphics;

import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.machines.ElectricDrillTileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
public class LaserDrillRenderer implements BlockEntityRenderer<ElectricDrillTileEntity, LaserDrillRenderer.State>{

	
	private final Identifier texture = Identifier.parse(ElectricAdvantage.MODID+":textures/materials/laser_beam.png");
	private final RenderType beamType = ElectricRenderTypes.unshaded(texture, false);
	
	
	public LaserDrillRenderer(BlockEntityRendererProvider.Context context) {
	}

	
	public static final class State extends BlockEntityRenderState {
        float length;
        Direction facing = Direction.DOWN;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(ElectricDrillTileEntity entity, State state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breaking);
        state.length = entity.getDrillLength();
        state.facing = entity.getFacing();
    }

    @Override public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.length <= 0) return;
        transform(matrices, state.facing);
        float length = state.length;
        collector.submitCustomGeometry(matrices, beamType,
                (pose, vertices) -> draw(length, pose, vertices, OverlayTexture.NO_OVERLAY));
        matrices.popPose();
    }

	public void render(final ElectricDrillTileEntity e, final float partialTick, final PoseStack matrices,
            final VertexConsumer vertices, final int packedLight, final int packedOverlay) {
		float laserBlastLength = e.getDrillLength();

		if(laserBlastLength > 0){

			transform(matrices, e.getFacing());
            draw(laserBlastLength, matrices.last(), vertices, packedOverlay);
            matrices.popPose();
        }
    }

    private static void transform(PoseStack matrices, Direction dir) {
			float rotY = 0, rotX = 0;
			switch(dir){
			case NORTH:{
				// do nothing
				break;
			}
			case EAST:{
				rotY = -90;
				break;
			}
			case SOUTH:{
				rotY = 180;
				break;
			}
			case WEST:{
				rotY = 90;
				break;
			}
			case UP:{
				rotX = 90;
				break;
			}
			case DOWN:{
				rotX = -90;
				break;
			}
			default:{
				// do nothing
				break;
			}
			}
			
			matrices.pushPose();
			matrices.translate(0.5f, 0.5f, 0.5f);
			matrices.mulPose(Axis.YP.rotationDegrees(rotY));
			matrices.mulPose(Axis.XP.rotationDegrees(rotX));
    }

    private static void draw(float laserBlastLength, PoseStack.Pose pose,
            com.mojang.blaze3d.vertex.VertexConsumer vertices, int packedOverlay) {
            final float laserU0 = 0, laserU1 = 0, laserV0 = 1, laserV1 = 1;
            final float laserRadius = 0.125f;
            final BufferedQuads worldRenderer = new BufferedQuads(pose, vertices, 15 << 20 | 15 << 4, packedOverlay);

			final double x1, y1, z1, x2, y2, z2,  x3, y3, z3, x4, y4, z4;
			x1 = 0;
			y1 = -laserRadius;
			z1 = 0;
			x2 = 0;
			y2 = laserRadius;
			z2 = -laserBlastLength;

			x3 = -laserRadius;
			y3 = 0;
			z3 = z1;
			x4 = laserRadius;
			y4 = 0;
			z4 = z2;

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
