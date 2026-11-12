package zone.moddev.mc.baseelectric.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.junit.BeforeClass;
import org.junit.Test;
import zone.moddev.mc.baseelectric.init.Power;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import static org.junit.Assert.*;

public class BatteryComponentTest {
    @BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }

    @Test public void vanillaItemUpgradePreservesDepletedChargeAndUnrelatedData() {
        CompoundTag custom = LegacyBatteryDataTest.upgradeBattery()
                .getCompound("components").orElseThrow().getCompound("minecraft:custom_data").orElseThrow();

        BatteryItem battery = new BatteryItem(100, new Item.Properties().setId(zone.moddev.mc.baseelectric.util.RegistryIds.item("lead_acid_battery")));
        ItemStack stack = new ItemStack(battery);
        assertEquals(100, battery.getEnergy(stack), 0);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        assertEquals(0, battery.getEnergy(stack), 0);
        assertEquals(100, battery.addEnergy(stack, 125, Power.ELECTRIC_POWER), 0);
        assertEquals("preserved", stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("probe").orElseThrow());
        assertEquals(-100, battery.addEnergy(stack, -125, Power.ELECTRIC_POWER), 0);
        assertEquals(0, battery.getEnergy(stack.copy()), 0);
    }

    @Test public void chargeMutationDoesNotLeakIntoAnItemCopy() {
        BatteryItem battery = new BatteryItem(100, new Item.Properties().setId(zone.moddev.mc.baseelectric.util.RegistryIds.item("lead_acid_battery")));
        ItemStack original = new ItemStack(battery);
        battery.setEnergy(original, 25);
        ItemStack copy = original.copy();
        battery.setEnergy(copy, 50);
        assertEquals(25, battery.getEnergy(original), 0);
        assertEquals(50, battery.getEnergy(copy), 0);
        assertFalse(ItemStack.isSameItemSameComponents(original, copy));
    }
}
