package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.World;

public class ElectricOvenBlock extends ElectricMachineBlock{
	
	public ElectricOvenBlock(){
		super();
	}

	@Override
	public ElectricMachineTileEntity createNewTileEntity(World w, int m) {
		return new ElectricOvenTileEntity();
	}

}
