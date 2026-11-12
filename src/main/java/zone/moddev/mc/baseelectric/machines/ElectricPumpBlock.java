package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.init.Blocks;
import zone.moddev.mc.baseelectric.init.Power;
import cyano.poweradvantage.api.ConduitType;
import cyano.poweradvantage.api.PowerConnectorContext;
import zone.moddev.mc.basepower.conduitnetwork.ConduitRegistry;
import zone.moddev.mc.basepower.init.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

public class ElectricPumpBlock extends ElectricMachineBlock{
    public ElectricPumpBlock() {
        super(machineProperties(0.75F).setId(zone.moddev.mc.baseelectric.util.RegistryIds.block("electric_pump")));
    }

	@Override
	public ElectricMachineTileEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		return new ElectricPumpTileEntity(pos, state);
	}
	
///// Overrides to make this a multi-type block /////
	
	/**
	 * This method is called whenever the block is placed into the world
	 */
	@Override
	public void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel w, BlockPos coord, boolean isMoving) {
		destroyPipe(w, coord);
		super.affectNeighborsAfterRemoval(state, w, coord, isMoving);
	}
	/**
	 * This method is called when the block is destroyed by an explosion.
	 */
	@Override
	public void wasExploded(Level w, BlockPos coord, Explosion boom){
		destroyPipe(w,coord);
		super.wasExploded(w, coord, boom);
	}
	
	
	private void destroyPipe(Level w, BlockPos coord) {
		if(w.isClientSide()) return;
		// destroy connected drill bits
		BlockPos c = coord.below();
		while(c.getY() > 0 && w.getBlockState(c).getBlock() == Blocks.pump_pipe_electric){
			w.setBlockAndUpdate(c, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
			c = c.below();
		}
	}


	@Override
	public boolean canAcceptConnection(PowerConnectorContext connection){
		return  ConduitType.areSameType(getType(), connection.powerType) || ConduitType.areSameType(Fluids.fluidConduit_general, connection.powerType);
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
		return ConduitType.areSameType(Power.ELECTRIC_POWER,p);
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
