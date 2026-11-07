package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.init.Power;
import cyano.poweradvantage.api.ConduitType;
import cyano.poweradvantage.api.PowerConnectorContext;

public class SteamPoweredElectricGeneratorBlock extends ElectricGeneratorBlock{

	public static final ConduitType STEAM_POWER = new ConduitType("steam");

	@Override
	public ElectricGeneratorTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new SteamPoweredElectricGeneratorTileEntity(pos, state);
	}

	@Override
	public boolean canAcceptConnection(PowerConnectorContext c){
		return ConduitType.areSameType(Power.ELECTRIC_POWER,c.powerType)
				|| ConduitType.areSameType(STEAM_POWER,c.powerType);
	}
	private final ConduitType[] types = {Power.ELECTRIC_POWER,STEAM_POWER};
	@Override
	public ConduitType[] getTypes(){
		return types;
	}

	@Override
	public boolean isPowerSink(ConduitType pt){
		return ConduitType.areSameType(STEAM_POWER,pt);
	}
	@Override
	public boolean isPowerSource(ConduitType pt){
		return ConduitType.areSameType(Power.ELECTRIC_POWER,pt);
	}

	///// end multi-type overrides /////

}
