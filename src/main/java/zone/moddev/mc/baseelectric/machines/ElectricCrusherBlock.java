package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.level.Level;

public class ElectricCrusherBlock extends ElectricMachineBlock{
	// NOTE: If a block's meta value goes above 0xF, then the block turns into a different block when it state changes
	public ElectricCrusherBlock(){
		super();
	}

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new ElectricCrusherTileEntity(pos, state);
	}

}
