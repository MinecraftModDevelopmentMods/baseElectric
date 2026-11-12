package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

public class HydroelectricGeneratorBlock extends ElectricGeneratorBlock {

	public HydroelectricGeneratorBlock(){
		super(machineProperties(0.75F).setId(zone.moddev.mc.baseelectric.util.RegistryIds.block("hydroelectric_generator")));
	}

	@Override
	public ElectricGeneratorTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new HydroelectricGeneratorTileEntity(pos, state);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos)
	{
		if (!super.canSurvive(state, worldIn, pos)) return false;
		BlockState below = worldIn.getBlockState(pos.below());
		// Preserve the removed blocksMotion predicate, including its vanilla exceptions.
		return below.is(net.minecraft.world.level.block.Blocks.COBWEB)
				|| below.is(net.minecraft.world.level.block.Blocks.BAMBOO_SAPLING) || !below.isSolid();
	}
}
