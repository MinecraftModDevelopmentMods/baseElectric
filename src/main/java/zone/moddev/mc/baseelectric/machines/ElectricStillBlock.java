package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.init.Power;
import cyano.poweradvantage.api.ConduitType;
import cyano.poweradvantage.api.PowerConnectorContext;
import zone.moddev.mc.basepower.init.Fluids;
import net.minecraft.world.level.Level;

public class ElectricStillBlock extends ElectricMachineBlock{

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new ElectricStillTileEntity(pos, state);
	}

///// Overrides to make this a multi-type block /////
	

	@Override
	public boolean canAcceptConnection(PowerConnectorContext connection){
		return ConduitType.areSameType(Power.ELECTRIC_POWER, connection.powerType)
				|| Fluids.isFluidType(connection.powerType);
	}

	private final ConduitType[] types = {Power.ELECTRIC_POWER, Fluids.fluidConduit_general};

	@Override
	public ConduitType[] getTypes(){
		return types;
	}/**
	 * Determines whether this block/entity should receive energy
	 * @return true if this block/entity should receive energy
	 */
	@Override
	public boolean isPowerSink(ConduitType p){
		return ConduitType.areSameType(Power.ELECTRIC_POWER,p) || Fluids.isFluidType(p);
	}
	/**
	 * Determines whether this block/entity can provide energy
	 * @return true if this block/entity can provide energy
	 */
	@Override
	public boolean isPowerSource(ConduitType p){
		return Fluids.isFluidType(p);
	}
	///// end multi-type overrides /////
	
}
