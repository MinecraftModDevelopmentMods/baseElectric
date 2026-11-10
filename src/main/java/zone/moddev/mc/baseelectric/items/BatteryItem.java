package zone.moddev.mc.baseelectric.items;

import zone.moddev.mc.baseelectric.common.IRechargeableItem;
import zone.moddev.mc.baseelectric.init.Power;
import cyano.poweradvantage.api.ConduitType;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.CustomData;

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
		if (!(stack.getItem() instanceof BatteryItem battery)) return 0;
		// Vanilla moves unknown 1.20.1 item tags to CUSTOM_DATA on world upgrade.
		CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		if (!data.contains("energy")) return battery.capacity;
		CompoundTag tag = data.copyTag();
		return tag.contains("energy", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("energy") : battery.capacity;
	}
	@Override
	public void setEnergy(ItemStack stack, float energy){
		if(stack.getItem() instanceof BatteryItem){
			CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat("energy", energy));
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
			float currentEnergy = getEnergy(stack);
			float newEnergy = Math.max(0, Math.min(currentEnergy+energy, battery.capacity));
			setEnergy(stack, newEnergy);
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
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag){
		super.appendHoverText(stack, context, list, flag);
		float max = getMaxEnergy(stack);
		if(max <= 0) return; // not a battery!
		float e = getEnergy(stack);
		list.add(Component.literal(nf.format(100 * e / max) + "% ")
				.append(Component.translatable("tooltip.battery.charge")));
		list.add(Component.literal("(" + nf.format(max) + "kJ ")
				.append(Component.translatable("tooltip.battery.capacity"))
				.append(")"));
	}
}
