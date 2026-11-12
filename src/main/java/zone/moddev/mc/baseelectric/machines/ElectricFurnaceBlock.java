package zone.moddev.mc.baseelectric.machines;

import net.minecraft.world.level.Level;

public class ElectricFurnaceBlock extends ElectricMachineBlock{
	
	public ElectricFurnaceBlock(){
		super(machineProperties(0.75F).setId(zone.moddev.mc.baseelectric.util.RegistryIds.block("electric_furnace")));
	}

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new ElectricFurnaceTileEntity(pos, state);
	}

}
