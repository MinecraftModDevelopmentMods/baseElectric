package zone.moddev.mc.electricadvantage.machines;

import cyano.poweradvantage.api.ConduitType;
import net.minecraft.world.level.Level;

public class ElectricBatteryArrayBlock extends ElectricGeneratorBlock {

	public ElectricBatteryArrayBlock(){
		super();
	}
	
	@Override
	public ElectricGeneratorTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new ElectricBatteryArrayTileEntity(pos, state);
	}
	@Override
	public boolean isPowerSink(ConduitType e){
		return true;
	}

}
