package zone.moddev.mc.baseelectric.gui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.machines.GrowthChamberControllerTileEntity;
import cyano.poweradvantage.api.simple.SimpleMachineGUI;
import zone.moddev.mc.basepower.math.Integer2D;
import net.minecraft.resources.ResourceLocation;

public class GrowthChamberControllerGUI extends SimpleMachineGUI{

	public GrowthChamberControllerGUI() {
		super(new ResourceLocation(ElectricAdvantage.MODID+":textures/gui/container/growth_chamber_controller_gui.png"), 
				Integer2D.fromCoordinates(100,74));
	}
	
	/**
	 * Override this method to draw on the GUI window.
	 * <br><br>
	 * This method is invoked when drawing the GUI so that you can draw 
	 * animations and other foreground decorations to the GUI.
	 * @param srcEntity This is the TileEntity (or potentially a LivingEntity) 
	 * for whom we are drawing this interface
	 * @param guiContainer This is the instance of GUIContainer that is drawing 
	 * the GUI. You need to use it to draw on the screen. For example:<br>
	   <pre>
guiContainer.getMinecraft().getTextureManager().bindTexture(arrowTexture);
guiContainer.blit(x+79, y+35, 0, 0, arrowLength, 17); // x, y, textureOffsetX, textureOffsetY, width, height)
	   </pre>
	 * @param x This is the x coordinate (in pixels) from the top-left corner of 
	 * the GUI
	 * @param y This is the y coordinate (in pixels) from the top-left corner of 
	 * the GUI
	 * @param z This is the z coordinate (no units) into the depth of the screen
	 */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void drawGUIDecorations(Object srcEntity, GUIContainer guiContainer, int x, int y, float  z){
		if(srcEntity instanceof GrowthChamberControllerTileEntity){
			GrowthChamberControllerTileEntity te = (GrowthChamberControllerTileEntity)srcEntity;
			boolean power = te.isPowered();
			GUIHelper.drawIndicatorLight(power, guiContainer,x,y);
			float water = te.getWaterLevel();
			GUIHelper.drawProgressBar(x+97, y+60, water, guiContainer);
		}
	}

}
