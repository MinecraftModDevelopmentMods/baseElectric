package zone.moddev.mc.baseelectric.graphics;

import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.machines.ElectricDrillTileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT) // This is needed for classes that extend client-only classes
public class LaserDrillRenderer implements BlockEntityRenderer<ElectricDrillTileEntity>{

	
	private final ResourceLocation texture = ResourceLocation.parse(ElectricAdvantage.MODID+":textures/materials/laser_beam.png");
	private final RenderType beamType = ElectricRenderTypes.unshaded(texture, false);
	
	
	public LaserDrillRenderer(BlockEntityRendererProvider.Context context) {
	}

	
	@Override
	public void render(final ElectricDrillTileEntity e, final float partialTick, final PoseStack matrices,
            final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
		float laserBlastLength = e.getDrillLength();

		if(laserBlastLength > 0){

			final float laserU0 = 0;
			final float laserU1 = 0;
			final float laserV0 = 1.0f;
			final float laserV1 = 1.0f;

			final float laserRadius = 0.125f;


			Direction dir = e.getFacing();

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
            final BufferedQuads worldRenderer = new BufferedQuads(matrices,
                    buffers.getBuffer(beamType), 15 << 20 | 15 << 4, packedOverlay);

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

			matrices.popPose();

		}
	}

	
}
