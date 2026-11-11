package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.init.Power;
import cyano.poweradvantage.api.ConduitType;
import cyano.poweradvantage.api.PowerConnectorContext;
import cyano.poweradvantage.api.PoweredEntity;
import zone.moddev.mc.basepower.init.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;


/**
 * The coal boiler is a bit more complicated because it consumes fluids (water) and produces steam,
 * making it a multi-type powered block.
 * @author DrCyano
 *
 */
public class GrowthChamberControllerBlock extends cyano.poweradvantage.api.simple.BlockSimplePowerMachine{


	public GrowthChamberControllerBlock() {
		super(machineProperties(0.75f).setId(zone.moddev.mc.baseelectric.util.RegistryIds.block("growth_chamber_controller")),
				Power.GROWTHCHAMBER_POWER, Power.ELECTRIC_POWER, Fluids.fluidConduit_general);
	}

	@Override
	public PoweredEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new GrowthChamberControllerTileEntity(pos, state);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState bs) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState bs, Level world, BlockPos coord) {
		if(world.getBlockEntity(coord) instanceof GrowthChamberControllerTileEntity){
			return ((GrowthChamberControllerTileEntity)world.getBlockEntity(coord)).getComparatorOutput();
		}
		return 0;
	}



	@Override
	public boolean isPowerSink(ConduitType pt){
		return ConduitType.areSameType(Power.ELECTRIC_POWER,pt)
				|| Fluids.isFluidType(pt);
	}
	@Override
	public boolean isPowerSource(ConduitType pt){
		return ConduitType.areSameType(Power.GROWTHCHAMBER_POWER,pt);
	}

	@Override
	public boolean canAcceptConnection(PowerConnectorContext c){
		return ConduitType.areSameType(Power.ELECTRIC_POWER,c.powerType)
				|| ConduitType.areSameType(Power.GROWTHCHAMBER_POWER, c.powerType)
				|| Fluids.isFluidType(c.powerType);
	}

	///// end multi-type overrides /////
}
