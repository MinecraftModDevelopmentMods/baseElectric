package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.level.Level;

public class ElectricFabricatorBlock extends ElectricMachineBlock{

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new ElectricFabricatorTileEntity(pos, state);
	}

}
