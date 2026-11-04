package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.init.Power;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class GrowthChamberBlock extends ElectricMachineBlock{

	public GrowthChamberBlock(){
		super(Material.PISTON,Power.GROWTHCHAMBER_POWER);
	}

	@Override
	public ElectricMachineTileEntity createNewTileEntity(World w, int m) {
		return new GrowthChamberTileEntity();
	}
}
