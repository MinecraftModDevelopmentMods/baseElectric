package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.init.Power;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class PhotovoltaicGeneratorTileEntity extends ElectricGeneratorTileEntity {

	private static final float PI = (float)Math.PI;
	private static final float SUM_OF_SINE_TICKS = 24000 / PI / 8;
	public static final float SOLAR_PER_DAY = 12500; // quarter the energy of a piece of coal per day
	public static final float MAX_SOLAR_OUTPUT = PI * SOLAR_PER_DAY / SUM_OF_SINE_TICKS;
	public static final float BLOCKLIGHT_FACTOR = 0.1f; // light from blocks less effective than sunlight
	private float powerOutput = 0;
	
	public PhotovoltaicGeneratorTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(zone.moddev.mc.baseelectric.init.Entities.PHOTOVOLTAIC_GENERATOR, pos, state, "tile.baseelectric.photovoltaic_generator.name", 0);
	}

	@Override
	public float getPowerOutput() {
		return powerOutput;
	}
	
	@Override
	public int getComparatorOutput(){
		return (int)(15 * powerOutput);
	}


	@Override
	public boolean isActive(){
		return (hasRedstoneSignal() == false) && powerOutput > 0.0001;
	}
	
	@Override
	protected boolean isValidInputItem(ItemStack item) {
		return false;
	}

	@Override
	protected void saveTo(CompoundTag tagRoot) {
		// do nothing
	}

	@Override
	protected void loadFrom(CompoundTag tagRoot) {
		// do nothing
	}

	final int[] dataArray = new int[1];
	@Override
	public int[] getDataFieldArray() {
		return dataArray;
	}

	@Override
	public void onDataFieldUpdate() {
		powerOutput = Float.intBitsToFloat(dataArray[0]);
	}

	@Override
	public void prepareDataFieldsForSync() {
		dataArray[0] = Float.floatToIntBits(powerOutput);
	}

	@Override
	public void tickUpdate(boolean isServer) {
		// do nothing
	}
	float oldPower = 0;
	@Override
	public void powerUpdate(){
		float power;
		if(hasRedstoneSignal() == false){
			float solar = getLightIntensityAt(getBlockPos(),getLevel());
			power = Math.max(0,MAX_SOLAR_OUTPUT * solar);
			this.addEnergy(power, Power.ELECTRIC_POWER);
		} else {
			power = 0f;
		}
		powerOutput = power / MAX_SOLAR_OUTPUT;
		if(powerOutput != oldPower){
			oldPower = powerOutput;
			this.sync();
		}
		
		super.powerUpdate();
	}
	
	public static float getLightIntensityAt(BlockPos pos, Level w){
		// The panel is solid; sample incident light above its top surface, not inside it.
		BlockPos lightPos = pos.above();
		float light;
		if(w.canSeeSky(lightPos) && w.dimensionType().hasSkyLight()){
			// outdoor light imposes no penalty
			light = Mth.sin((float)w.getDayTime() * 2.61799E-4f); // (worldTime * pi / 12000);
			// handle weather
			if(w.isThundering()){
				light *= 0.2f;
			} else if(w.isRaining()){
				light *= 0.35f;
			}
		} else {
			float blockLight = w.getMaxLocalRawBrightness(lightPos) * 6.66666E-2f; // block light / 15
			light = BLOCKLIGHT_FACTOR * blockLight;
		}
		return light;
	}

}
