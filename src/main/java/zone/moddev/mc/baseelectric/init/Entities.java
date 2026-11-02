package zone.moddev.mc.baseelectric.init;

import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.entities.*;
import zone.moddev.mc.baseelectric.graphics.*;
import zone.moddev.mc.baseelectric.machines.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.tileentity.TileEntity;
import zone.moddev.mc.basepower.compat.LegacyIdentity;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class Entities {

	private static boolean initDone = false;
	public static void init(){
		if(initDone) return;
		
		Blocks.init();

		registerTileEntity(ElectricFurnaceTileEntity.class);
		registerTileEntity(ElectricCrusherTileEntity.class);
		registerTileEntity(ElectricDrillTileEntity.class);
		registerTileEntity(ElectricFabricatorTileEntity.class);
		registerTileEntity(GrowthChamberTileEntity.class);
		registerTileEntity(GrowthChamberControllerTileEntity.class);
		registerTileEntity(ElectricOvenTileEntity.class);
		registerTileEntity(LaserTurretTileEntity.class);
		registerTileEntity(LEDTileEntity.class);

		registerTileEntity(HydroelectricGeneratorTileEntity.class);
		registerTileEntity(PhotovoltaicGeneratorTileEntity.class);
		registerTileEntity(SteamPoweredElectricGeneratorTileEntity.class);
		registerTileEntity(ElectricBatteryArrayTileEntity.class);
		

		registerTileEntity(ElectricPumpTileEntity.class);
		registerTileEntity(ElectricStillTileEntity.class);
		registerTileEntity(PlasticRefineryTileEntity.class);
		
		registerEntity(HydroturbineEntity.class);
		
		initDone = true;
	}
	
	

	static void registerTileEntity(Class<? extends TileEntity> tileEntityClass){
		String name = tileEntityClass.getSimpleName();
		if(name.endsWith("TileEntity")){
			name = name.substring(0, name.lastIndexOf("TileEntity"));
		}
		String suffix = "." + toUnderscoreStyle(name);
		LegacyIdentity.registerTileEntity(tileEntityClass, ElectricAdvantage.MODID + suffix,
				ElectricAdvantage.LEGACY_MODID + suffix);
	}

	private static int entityIndex = 0;
	private static void registerEntity(Class<? extends Entity> entityClass) {
		String name=entityClass.getSimpleName();
		if(name.endsWith("Entity")){
			name = name.substring(0, name.lastIndexOf("Entity"));
		} else if(name.startsWith("Entity")){
			name = name.substring("Entity".length(),name.length());
		}
		EntityRegistry.registerModEntity(entityClass, name, entityIndex++, ElectricAdvantage.INSTANCE, 64, 1, true);
		registerHydroturbineAliases();
	}

	static void registerHydroturbineAliases() {
		String[] oldIds = {"electricadvantage.electricadvantage.Hydroturbine",
				"electricadvantage.Hydroturbine"};
		for (String oldId : oldIds) {
			Class<? extends Entity> previous = EntityList.NAME_TO_CLASS.get(oldId);
			if (previous != null && previous != HydroturbineEntity.class) {
				throw new IllegalStateException("Legacy entity ID is already owned: " + oldId);
			}
		}
		// Read aliases only. Forge's canonical class-to-name entry controls new saves.
		for (String oldId : oldIds) EntityList.NAME_TO_CLASS.put(oldId, HydroturbineEntity.class);
	}
	
	private static String toUnderscoreStyle(String camelCase){
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toLowerCase(camelCase.charAt(0)));
		for(int i = 1; i < camelCase.length(); i++){
			char c = camelCase.charAt(i);
			if(Character.isUpperCase(c)){
				sb.append('_').append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	@SideOnly(Side.CLIENT)
	public static void registerRenderers(){
		RenderManager rm = Minecraft.getMinecraft().getRenderManager();
		
		ClientRegistry.bindTileEntitySpecialRenderer(LaserTurretTileEntity.class, new zone.moddev.mc.baseelectric.graphics.LaserTurretRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(ElectricDrillTileEntity.class, new zone.moddev.mc.baseelectric.graphics.LaserDrillRenderer());
		
		RenderingRegistry.registerEntityRenderingHandler(HydroturbineEntity.class,new HydroturbineRenderer(rm));
	}
}
