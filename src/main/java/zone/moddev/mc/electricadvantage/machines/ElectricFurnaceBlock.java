package zone.moddev.mc.electricadvantage.machines;

import net.minecraft.world.level.Level;

public class ElectricFurnaceBlock extends ElectricMachineBlock{
	
	public ElectricFurnaceBlock(){
		super();
	}

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new ElectricFurnaceTileEntity(pos, state);
	}

}
