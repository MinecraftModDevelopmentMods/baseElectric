package zone.moddev.mc.baseelectric.machines;

import zone.moddev.mc.baseelectric.init.Power;
import cyano.poweradvantage.api.ConduitType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import zone.moddev.mc.baseelectric.ElectricAdvantage;

import java.util.Arrays;

public abstract class ElectricGeneratorTileEntity extends cyano.poweradvantage.api.simple.TileEntitySimplePowerMachine {

	private final ItemStack[] inventory;
	private final int[] inputSlots;
	
	
	public ElectricGeneratorTileEntity(BlockEntityType<?> tileType, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, String name,int numInputSlots) {
		super(tileType, pos, state, Power.ELECTRIC_POWER, 1000, name);
		inventory = new ItemStack[numInputSlots];
		inputSlots = new int[numInputSlots];
		for(int i = 0; i < inventory.length; i++){
			if(i < inputSlots.length)inputSlots[i] = i;
		}
	}

	public ElectricGeneratorTileEntity(BlockEntityType<?> tileType, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, String name,int numInputSlots, ConduitType[] types, float[] energyBuffers) {
		super(tileType, pos, state, types, energyBuffers, name);
		inventory = new ItemStack[numInputSlots];
		inputSlots = new int[numInputSlots];
		for(int i = 0; i < inventory.length; i++){
			if(i < inputSlots.length)inputSlots[i] = i;
		}
	}


	private boolean redstone = false;
	private boolean redstoneInitialized = false;
	private float oldEnergy = 0f;
	@Override
	public void powerUpdate(){
		super.powerUpdate();
		
		redstone = getLevel().hasNeighborSignal(getBlockPos());
		redstoneInitialized = true;
		
		if(oldEnergy != this.getEnergy(Power.ELECTRIC_POWER)){
			this.sync();
		}
	}
	
	protected boolean hasRedstoneSignal(){
		// Generators can check the interlock before their first power-update refresh.
		if(!redstoneInitialized && getLevel() != null && !getLevel().isClientSide){
			redstone = getLevel().hasNeighborSignal(getBlockPos());
			redstoneInitialized = true;
		}
		return redstone;
	}

	
	protected void setActive(boolean active){
		BlockState old = getLevel().getBlockState(getBlockPos());
		if(old.getBlock() instanceof ElectricGeneratorBlock 
				&& (Boolean)old.getValue(ElectricGeneratorBlock.ACTIVE) != active){
			getLevel().setBlock(getBlockPos(), old.setValue(ElectricGeneratorBlock.ACTIVE, active), 3);
		}
	}

	protected void playSoundEffect(double x, double y, double z, SoundEvent sound, float volume, float pitch){
		ElectricOperationalSound.broadcast(getLevel(), x, y, z, sound, volume, pitch);
	}

	public boolean isActive(){
		return (Boolean)getLevel().getBlockState(getBlockPos()).getValue(ElectricGeneratorBlock.ACTIVE);
	}

	@Override
	public boolean isPowerSink(ConduitType e){
		return false;
	}

	@Override
	public boolean isPowerSource(ConduitType e){
		return true;
	}
	
	public abstract float getPowerOutput();

	public ItemStack getInputSlot(int i){
		if(i < inputSlots.length){
			return inventory[inputSlots[i]];
		} else {
			ElectricAdvantage.LOGGER.warn("Invalid slot access in {}. Stack trace: {}",
					getClass().getName(), Arrays.toString(Thread.currentThread().getStackTrace()).replace(',', '\n'));
			return null;
		}
	}
	

	public int numberOfInputSlots(){return inputSlots.length;}
	
	
	@Override
	protected ItemStack[] getInventory() {
		return inventory;
	}

	
	@Override
	public boolean canPlaceItemThroughFace(final int slot, final ItemStack srcItem, final Direction side) {
		return isValidInputItem(srcItem) && isInArray(slot,inputSlots);
	}

	@Override
	public boolean canPlaceItem(final int slot, final ItemStack srcItem) {
		return isValidInputItem(srcItem) && isInArray(slot,inputSlots);
	}
	
	protected abstract boolean isValidInputItem(ItemStack item);
	
	protected final boolean isInArray(int i, int[] arr){
		for(int n = 0; n < arr.length; n++){
			if(arr[n] == i)return true;
		}
		return false;
	}



	public float getEnergy(){
		return getEnergy(Power.ELECTRIC_POWER);
	}

	public float getEnergyCapacity(){
		return getEnergyCapacity(Power.ELECTRIC_POWER);
	}

	public float addEnergy(float amount){
		return addEnergy(amount,Power.ELECTRIC_POWER);
	}

	public float subtractEnergy(float amount){
		return subtractEnergy(amount,Power.ELECTRIC_POWER);
	}

	public void setEnergy(float amount){
		setEnergy(amount,Power.ELECTRIC_POWER);
	}

	public ConduitType getEnergyType() {return Power.ELECTRIC_POWER;}

	public int getComparatorOutput(){
		return (int)(15 * this.getEnergy(Power.ELECTRIC_POWER) / this.getEnergyCapacity(Power.ELECTRIC_POWER));
	}

	
	@Override
	protected void saveAdditional(CompoundTag tagRoot){
		super.saveAdditional(tagRoot);
		saveTo(tagRoot);
	}
	
	@Override
	public void load(CompoundTag tagRoot){
		super.load(tagRoot);
		loadFrom(tagRoot);
	}
	
	protected abstract void saveTo(CompoundTag tagRoot);
	protected abstract void loadFrom(CompoundTag tagRoot);

	@Override public int getMaxStackSize(){return 64;}
}
