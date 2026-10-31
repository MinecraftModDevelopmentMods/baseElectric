package zone.moddev.mc.electricadvantage.machines;

import zone.moddev.mc.electricadvantage.init.Power;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.Level;

public class GrowthChamberBlock extends ElectricMachineBlock{

	public GrowthChamberBlock(){
		super(Material.PISTON,Power.GROWTHCHAMBER_POWER);
	}

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new GrowthChamberTileEntity(pos, state);
	}
}
