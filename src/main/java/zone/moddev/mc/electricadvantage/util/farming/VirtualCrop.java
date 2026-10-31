package zone.moddev.mc.electricadvantage.util.farming;

import zone.moddev.mc.electricadvantage.util.crafting.ItemRecord;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IPlantable;

import java.lang.reflect.Field;
import java.util.*;

public class VirtualCrop {

	private static final Map<ItemRecord,VirtualCrop> customRecipes = new HashMap<>();

	public static void addCustomCrop(ItemStack seed, int numberGrowthStages, Collection<ItemStack> harvest){
		ItemStack single = seed.copy();
		single.setCount(1);
		customRecipes.put(new ItemRecord(seed), new VirtualCrop(single,numberGrowthStages,harvest));
	}
	
	public static void removeCustomCrop(ItemStack seed){
		customRecipes.remove(new ItemRecord(seed));
	}
	
	public static VirtualCrop createVirtualCrop(ItemStack stack, Level w, BlockPos pos){
		if(stack == null || stack.getItem() == null) return null;
		ItemRecord r = new ItemRecord(stack);
		ItemStack seed = stack.copy();
		seed.setCount(1);
		Item i = stack.getItem();
		Block b;
		if(i instanceof BlockItem){
			b = ((BlockItem)i).getBlock();
		} else {
			b = null;
		}
		if(customRecipes.get(r) != null){
			return customRecipes.get(r).copy();
		} else if(i == net.minecraft.world.level.block.Blocks.SUGAR_CANE.asItem()
				|| b instanceof net.minecraft.world.level.block.MushroomBlock
				|| b instanceof net.minecraft.world.level.block.CactusBlock
				|| (b instanceof net.minecraft.world.level.block.BushBlock
						&& !(b instanceof net.minecraft.world.level.block.CropBlock) && !(b instanceof StemBlock))
				|| b instanceof net.minecraft.world.level.block.GrassBlock
				|| b instanceof net.minecraft.world.level.block.VineBlock
				|| b instanceof net.minecraft.world.level.block.WaterlilyBlock){
			// duplicate
			ItemStack product = seed.copy();
			product.setCount(2);
			return new VirtualCrop(seed,8,Arrays.asList(product));
		} else if(i instanceof IPlantable || b instanceof IPlantable){
			IPlantable plantable = i instanceof IPlantable ? (IPlantable)i : (IPlantable)b;
			BlockState startingState = plantable.getPlant(w, pos);
			Block block = startingState.getBlock();
			int numStages = getNumberOfGrowthStages(startingState);
			if(block instanceof StemBlock){
				Block product = ((StemBlock)block).getFruit();
				if(product == null) {
					return null;
				}
				return new VirtualCrop(seed,numStages,Arrays.asList(new ItemStack(product)));
			} else {
				BlockState agedState = ageToMax(startingState);
				return new VirtualCrop(getDrops(startingState, w, pos),
						getNumberOfGrowthStages(startingState),
						getDrops(agedState, w, pos));
			}
		}
		return null;
	}
	
	
	public VirtualCrop copy(){
		return new VirtualCrop(this.prematureHarvest,this.numberOfStages, this.harvest);
	}
	

	private static int getNumberOfGrowthStages(BlockState startingState) {
		for(Property<?> property : startingState.getProperties()){
			if(property instanceof IntegerProperty){
				IntegerProperty prop = (IntegerProperty)property;
				if("age".equals(prop.getName())){
					return prop.getPossibleValues().size();
				}
			}
		}
		return 8;
	}
	

	private static BlockState ageToMax(BlockState startingState) {
		for(Property<?> property : startingState.getProperties()){
			if(property instanceof IntegerProperty){
				IntegerProperty prop = (IntegerProperty)property;
				if("age".equals(prop.getName())){
					int max = 0;
					for(Object i : prop.getPossibleValues()){
						if((Integer)i > max){max = (Integer)i;}
					}
					return startingState.setValue(prop, Integer.valueOf(max));
				}
			}
		}
		return startingState;
	}

	private static List<ItemStack> getDrops(BlockState state, Level world, BlockPos pos) {
		if (!(world instanceof net.minecraft.server.level.ServerLevel)) return Collections.emptyList();
		return Block.getDrops(state, (net.minecraft.server.level.ServerLevel) world, pos, null);
	}
	
	private final byte numberOfStages;
	private byte stage = 0;
	private final List<ItemStack> harvest;
	private final List<ItemStack> prematureHarvest;

	public VirtualCrop(ItemStack item, int numGrowthStages, Collection<ItemStack> harvest){
		this.numberOfStages = (byte)(numGrowthStages & 0x7F);
		this.prematureHarvest = Arrays.asList(item);
		this.harvest = removeInvalidItemStacks(harvest);
		if(this.harvest.isEmpty()){
			ItemStack pity = item.copy();
			pity.setCount(2);
			this.harvest.add(pity);
		}
	}
	
	public VirtualCrop(Collection<ItemStack> earlyHarvest, int numGrowthStages, Collection<ItemStack> harvest){
		this.numberOfStages = (byte)(numGrowthStages & 0x7F);
		this.prematureHarvest = new ArrayList<>(earlyHarvest);
		this.harvest = removeInvalidItemStacks(harvest);
	}
	
	public List<ItemStack> getHarvest(){
		if(stage >= numberOfStages){
			return harvest;
		} else {
			return prematureHarvest;
		}
	}
	/**
	 * Grows this crop by 1 growth stage
	 * @return True if the crop is ready for harvest, false otherwise
	 */
	public boolean grow(){
		stage++;
		return stage >= numberOfStages;
	}

	public byte getCurrentGrowth(){
		return this.stage;
	}
	public void setCurrentGrowth(byte newValue){
		this.stage = (newValue > getMaxGrowth() ? getMaxGrowth() : newValue);
	}
	
	public byte getMaxGrowth(){
		return this.numberOfStages;
	}
	
	private static List<ItemStack> removeInvalidItemStacks(Collection<ItemStack> items){
		/// CURSE YOU, MASTERCHEF MOD! DON'T YOU KNOW THAT ITEMSTACKS CAN'T HAVE NULL ITEMS?!?!
		List<ItemStack> valid = new ArrayList<>(items.size());
		for(ItemStack i : items){
			if(i.getItem() != null && i.getCount() > 0){
				valid.add(i);
			}
		}
		return valid;
	}
	
}
