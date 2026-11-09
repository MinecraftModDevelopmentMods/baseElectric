package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

public class PhotovoltaicGeneratorBlock extends ElectricGeneratorBlock{

	public PhotovoltaicGeneratorBlock(){
		super(net.minecraft.world.level.block.Block.Properties.of(Material.STONE)
				.strength(0.75F).sound(SoundType.GLASS).dynamicShape()
                .isRedstoneConductor((state, world, pos) -> true));
	}
	
	@Override
	public ElectricGeneratorTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new PhotovoltaicGeneratorTileEntity(pos, state);
	}
	


}
