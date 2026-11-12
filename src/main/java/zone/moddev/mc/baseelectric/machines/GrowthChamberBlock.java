package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.init.Power;
import net.minecraft.world.level.Level;

public class GrowthChamberBlock extends ElectricMachineBlock{

	public GrowthChamberBlock(){
		super(machineProperties(0.75f).setId(zone.moddev.mc.baseelectric.util.RegistryIds.block("growth_chamber")), Power.GROWTHCHAMBER_POWER);
	}

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new GrowthChamberTileEntity(pos, state);
	}
}
