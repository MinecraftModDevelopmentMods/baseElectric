package zone.moddev.mc.baseelectric.init;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.nbt.CompoundTag;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class VillagesTest {
    @BeforeClass public static void bootstrapVanillaItems() {
        NativeTestBootstrap.initialize();
    }

    @Test public void nativeNegativePricesKeepBulkGoodsForOneEmerald() {
        assertOffer(-8, -4, false, 1, 8);
        assertOffer(-8, -4, true, 1, 4);
        assertOffer(-6, -3, false, 1, 6);
        assertOffer(-6, -3, true, 1, 3);
        assertOffer(-10, -5, false, 1, 10);
        assertOffer(-10, -5, true, 1, 5);
    }

    @Test public void nativePositivePricesKeepOneGoodForOneToThreeEmeralds() {
        assertOffer(1, 3, false, 1, 1);
        assertOffer(1, 3, true, 3, 1);
    }

    private static void assertOffer(int minimum, int maximum, boolean upper, int emeralds, int goods) {
        RandomSource random = new LegacyRandomSource(0L) {
            @Override public int nextInt(int bound) { return upper ? bound - 1 : 0; }
        };
        MerchantOffer offer = Villages.sell(net.minecraft.world.item.Items.APPLE, minimum, maximum)
                .getOffer(null, random);
        CompoundTag nbt = offer.createTag();
        assertEquals("minecraft:emerald", nbt.getCompound("buy").getString("id"));
        assertEquals(emeralds, nbt.getCompound("buy").getByte("Count"));
        assertEquals("minecraft:apple", nbt.getCompound("sell").getString("id"));
        assertEquals(goods, nbt.getCompound("sell").getByte("Count"));
        assertEquals(7, nbt.getInt("maxUses"));
        assertEquals(0.0F, nbt.getFloat("priceMultiplier"), 0.0F);
    }
}
