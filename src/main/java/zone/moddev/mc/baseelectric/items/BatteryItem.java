package zone.moddev.mc.baseelectric.items;

import zone.moddev.mc.baseelectric.common.IRechargeableItem;
import zone.moddev.mc.baseelectric.init.Power;
import cyano.poweradvantage.api.ConduitType;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

import java.text.NumberFormat;
import java.util.List;


public class BatteryItem extends net.minecraft.world.item.Item implements IRechargeableItem {
	private final float capacity;
	private final NumberFormat nf;
	
	public BatteryItem(float capacity, Item.Properties properties){
		super(properties.stacksTo(1));
		this.capacity = capacity;
		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(0);
		nf.setMinimumFractionDigits(0);
	}
	
	@Override
	public float getEnergy(ItemStack stack) {
		if(stack.getItem() instanceof IRechargeableItem){
			BatteryItem battery = (BatteryItem)stack.getItem();
			CompoundTag dataTag;
			if(!stack.hasTag()){
				return battery.capacity; // new battery starts with 100% charge
			} else {
				dataTag = stack.getTag();
				if(dataTag.contains("energy", 99)){
					return dataTag.getFloat("energy");
				} else {
					return battery.capacity; // new battery starts with 100% charge
				}
			}
		} else {
			return 0;
		}
	}
	@Override
	public void setEnergy(ItemStack stack, float energy){
		if(stack.getItem() instanceof BatteryItem){
			CompoundTag dataTag;
			dataTag = stack.getOrCreateTag();
			dataTag.putFloat("energy", energy);
			stack.setTag(dataTag);
		}
	}
	/**
	 * Adds energy to a battery item
	 * @param stack battery item stack
	 * @param energy energy to add
	 * @return Returns the actual amount of energy added
	 */
	@Override
	public float addEnergy(ItemStack stack, float energy, ConduitType type){
		if(ConduitType.areSameType(type, Power.ELECTRIC_POWER) && stack.getItem() instanceof BatteryItem){
			BatteryItem battery = (BatteryItem)stack.getItem();
			CompoundTag dataTag;
			dataTag = stack.getOrCreateTag();
			float currentEnergy;
			if(dataTag.contains("energy", 99)){
				currentEnergy = dataTag.getFloat("energy");
			} else {
				currentEnergy = battery.capacity;
			}
			float newEnergy = Math.max(0, Math.min(currentEnergy+energy, battery.capacity));
			dataTag.putFloat("energy", newEnergy);
			stack.setTag(dataTag);
			return newEnergy - currentEnergy;
		} else {
			return 0;
		}
	}
	
	
	@Override
	public float getMaxEnergy(ItemStack item){
		return this.capacity;
	}
	
	@Override
	public ConduitType getEnergyType(){
		return Power.ELECTRIC_POWER;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> list, TooltipFlag flag){
		super.appendHoverText(stack, world, list, flag);
		float max = getMaxEnergy(stack);
		if(max <= 0) return; // not a battery!
		float e = getEnergy(stack);
		list.add(new TextComponent(nf.format(100 * e / max) + "% ")
				.append(new TranslatableComponent("tooltip.battery.charge")));
		list.add(new TextComponent("(" + nf.format(max) + "kJ ")
				.append(new TranslatableComponent("tooltip.battery.capacity"))
				.append(")"));
	}
}
