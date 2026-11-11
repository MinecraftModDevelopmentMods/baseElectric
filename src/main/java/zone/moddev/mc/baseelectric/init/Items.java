package zone.moddev.mc.baseelectric.init;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.items.BatteryItem;
import zone.moddev.mc.basepower.init.ItemGroups;

/** Stable item registry for Electric Advantage. */
@Mod.EventBusSubscriber(modid = ElectricAdvantage.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Items {
    public static final Map<String, Item> allItems = new LinkedHashMap<>();
    public static Item blank_circuit_board;
    public static Item control_circuit;
    public static Item integrated_circuit;
    public static Item power_supply_unit;
    public static Item silicon_ingot;
    public static Item silicon_blend;
    public static Item solder_blend;
    public static Item solder;
    public static Item lead_acid_battery;
    public static Item nickel_hydride_battery;
    public static Item alkaline_battery;
    public static Item lithium_battery;
    public static Item lithium_ingot;
    public static Item lithium_powder;
    public static Item lithium_nugget;
    public static Item sulfur_powder;
    public static Item petrolplastic_ingot;
    private static boolean initialized;

    private Items() {}

    /** Kept for callers of the legacy bootstrap; Forge owns item construction. */
    public static void init() {}

    private static synchronized void constructItems() {
        if (initialized) return;
        blank_circuit_board = add("blank_circuit_board", new Item(properties("blank_circuit_board")));
        control_circuit = add("control_circuit", new Item(properties("control_circuit")));
        integrated_circuit = add("integrated_circuit", new Item(properties("integrated_circuit")));
        power_supply_unit = add("psu", new Item(properties("psu")));
        silicon_ingot = add("silicon_ingot", new Item(properties("silicon_ingot")));
        silicon_blend = add("silicon_mix", new Item(properties("silicon_mix")));
        solder_blend = add("solder_mix", new Item(properties("solder_mix")));
        solder = add("solder", new Item(properties("solder")));
        lithium_ingot = add("li_ingot", new Item(properties("li_ingot")));
        lithium_powder = add("li_powder", new Item(properties("li_powder")));
        lithium_nugget = add("li_nugget", new Item(properties("li_nugget")));
        sulfur_powder = add("sulfur_powder", new Item(properties("sulfur_powder")));
        petrolplastic_ingot = add("petrolplastic_ingot", new Item(properties("petrolplastic_ingot")));
        lead_acid_battery = add("lead_acid_battery", new BatteryItem(Power.LEAD_ACID_BATTERY_CAPACITY, properties("lead_acid_battery")));
        nickel_hydride_battery = add("nickel_hydride_battery", new BatteryItem(Power.NICKEL_HYDRIDE_BATTERY_CAPACITY, properties("nickel_hydride_battery")));
        alkaline_battery = add("alkaline_battery", new BatteryItem(Power.ALKALINE_BATTERY_CAPACITY, properties("alkaline_battery")));
        lithium_battery = add("lithium_battery", new BatteryItem(Power.LITHIUM_BATTERY_CAPACITY, properties("lithium_battery")));
        initialized = true;
    }

    private static Item.Properties properties(String id) { return new Item.Properties().setId(zone.moddev.mc.baseelectric.util.RegistryIds.item(id)); }
    private static Item add(String id, Item item) {
        allItems.put(id, item);
        return item;
    }

    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, registry -> {
            constructItems();
            allItems.forEach((id, item) ->
                    registry.register(Identifier.fromNamespaceAndPath(ElectricAdvantage.MODID, id), ItemGroups.show(item)));
        });
    }

    public static Item getItemByName(String id) { return allItems.get(id); }
    public static Set<Map.Entry<String, Item>> getAllRegisteredItems() { return allItems.entrySet(); }
}
