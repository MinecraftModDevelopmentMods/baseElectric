package zone.moddev.mc.baseelectric.init;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
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
                .getOffer(null, null, random);
        CompoundTag nbt = (CompoundTag) MerchantOffer.CODEC.encodeStart(
                NativeTestBootstrap.registries().createSerializationContext(NbtOps.INSTANCE), offer).getOrThrow();
        assertEquals("minecraft:emerald", nbt.getCompound("buy").orElseThrow().getString("id").orElseThrow());
        CompoundTag payment = nbt.getCompound("buy").orElseThrow();
        assertEquals(emeralds, payment.contains("count") ? payment.getInt("count").orElseThrow() : 1);
        assertEquals("minecraft:apple", nbt.getCompound("sell").orElseThrow().getString("id").orElseThrow());
        CompoundTag sale = nbt.getCompound("sell").orElseThrow();
        assertEquals(goods, sale.contains("count") ? sale.getInt("count").orElseThrow() : 1);
        assertEquals(7, nbt.getInt("maxUses").orElseThrow().intValue());
        assertEquals(0.0F, offer.getPriceMultiplier(), 0.0F);
        // Native MerchantOffer.CODEC omits its zero price-multiplier default.
        assertEquals(0.0F, nbt.getFloatOr("priceMultiplier", 0.0F), 0.0F);
    }
}
