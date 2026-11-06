package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.World;

public class ElectricFabricatorBlock extends ElectricMachineBlock{

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.world.IBlockReader world) {
		return new ElectricFabricatorTileEntity();
	}

}
