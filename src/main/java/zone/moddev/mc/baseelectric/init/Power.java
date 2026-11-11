package zone.moddev.mc.baseelectric.init;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import cyano.poweradvantage.api.ConduitType;
import zone.moddev.mc.baseelectric.ElectricAdvantage;

public abstract class Power {

	public static final ConduitType ELECTRIC_POWER = new ConduitType("electricity");
	
	public static final ConduitType GROWTHCHAMBER_POWER = new ConduitType("greenhouse");
	
	// 1 unit of electricity is 1 kJ of energy, 1 piece of coal has 50 MJ in it
	public static final float STEAM_TO_ELECTRICITY = 31.25f;
	public static final float ELECTRICITY_TO_STEAM = 1.0f / STEAM_TO_ELECTRICITY;

	// made from lithium
	public static final float LITHIUM_BATTERY_CAPACITY = 8000f;
	// made from manganese and zinc
	public static final float ALKALINE_BATTERY_CAPACITY = 4000f;
	// made from nickel and manganese
	public static final float NICKEL_HYDRIDE_BATTERY_CAPACITY = 2000f;
	// made from lead and sulfur
	public static final float LEAD_ACID_BATTERY_CAPACITY = 1000f;
	
	public static final ResourceKey<DamageType> LASER_DAMAGE = damageType("laser");
	public static final ResourceKey<DamageType> PROPELLER_DAMAGE = damageType("propeller");

	private static ResourceKey<DamageType> damageType(String id) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(ElectricAdvantage.MODID, id));
	}

	public static DamageSource damageSource(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type));
	}
}
