package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.World;

public class ElectricFurnaceBlock extends ElectricMachineBlock{
	
	public ElectricFurnaceBlock(){
		super();
	}

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.world.IBlockReader world) {
		return new ElectricFurnaceTileEntity();
	}

}
