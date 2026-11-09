package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.level.Level;

public class ElectricOvenBlock extends ElectricMachineBlock{
	
	public ElectricOvenBlock(){
		super();
	}

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new ElectricOvenTileEntity(pos, state);
	}

}
