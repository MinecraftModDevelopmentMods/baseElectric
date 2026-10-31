package zone.moddev.mc.electricadvantage.machines;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

public class HydroelectricGeneratorBlock extends ElectricGeneratorBlock {

	public HydroelectricGeneratorBlock(){
		super();
	}

	@Override
	public ElectricGeneratorTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new HydroelectricGeneratorTileEntity(pos, state);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos)
	{
		return super.canSurvive(state, worldIn, pos)
				&& !worldIn.getBlockState(pos.below()).getMaterial().isSolid();
	}
}
