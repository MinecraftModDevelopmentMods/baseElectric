package zone.moddev.mc.baseelectric.items;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import org.junit.Test;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import static org.junit.Assert.*;

public class LegacyBatteryDataTest {
    @Test public void nativeDataFixerKeepsTheModdedItemAndMovesItsLegacyCharge() {
        CompoundTag upgraded = upgradeBattery();
        assertEquals("baseelectric:lead_acid_battery", upgraded.getString("id"));
        assertFalse(upgraded.contains("tag"));
        CompoundTag custom = upgraded.getCompound("components").getCompound("minecraft:custom_data");
        assertTrue("Zero charge must be explicitly preserved", custom.contains("energy"));
        assertEquals(0, custom.getFloat("energy"), 0);
        assertEquals("preserved", custom.getString("probe"));
    }

    static CompoundTag upgradeBattery() {
        NativeTestBootstrap.initialize();
        CompoundTag item = new CompoundTag();
        item.putString("id", "baseelectric:lead_acid_battery");
        item.putByte("Count", (byte) 1);
        CompoundTag tag = new CompoundTag();
        tag.putFloat("energy", 0);
        tag.putString("probe", "preserved");
        item.put("tag", tag);
        return (CompoundTag) DataFixers.getDataFixer().update(References.ITEM_STACK,
                new Dynamic<>(NbtOps.INSTANCE, item), 3465,
                SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
    }
}
