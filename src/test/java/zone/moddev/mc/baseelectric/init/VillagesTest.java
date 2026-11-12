package zone.moddev.mc.baseelectric.init;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import org.junit.BeforeClass;
import org.junit.Test;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import static org.junit.Assert.*;

public class VillagesTest {
    private static final Path TRADES = Path.of("src/main/resources/data/baseelectric/villager_trade/librarian");
    private static Map<String, Item> GOODS;
    private static HolderLookup.Provider registries;

    @BeforeClass public static void bootstrapVanillaItems() {
        NativeTestBootstrap.initialize();
        // Native Item.CODEC resolves the built-in registry, not a private RegistryOps substitute.
        GOODS = Stream.of("electric_conduit", "solder", "integrated_circuit", "blank_circuit_board", "petrolplastic_ingot", "psu")
                .collect(java.util.stream.Collectors.toMap(id -> id, id -> {
                    var key = ResourceKey.create(Registries.ITEM, Identifier.parse("baseelectric:" + id));
                    Item item = new Item(new Item.Properties().setId(key));
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.register(key.identifier(), item);
                    return item;
                }));
        NativeTestBootstrap.bindNativeComponents();
        registries = NativeTestBootstrap.registries();
    }

    @Test public void nativeNegativePricesKeepBulkGoodsForOneEmerald() throws Exception {
        for (var bounds : List.of(new Object[] {"electric_conduit", 3, 6}, new Object[] {"solder", 5, 10},
                new Object[] {"integrated_circuit", 3, 6}, new Object[] {"blank_circuit_board", 3, 6},
                new Object[] {"petrolplastic_ingot", 4, 8})) {
            assertOffer((String)bounds[0], false, 1, (int)bounds[1]);
            assertOffer((String)bounds[0], true, 1, (int)bounds[2]);
        }
    }

    @Test public void nativePositivePricesKeepOneGoodForOneToThreeEmeralds() throws Exception {
        assertOffer("psu", false, 1, 1);
        assertOffer("psu", true, 3, 1);
    }

    @Test public void allSixOffersExtendOnlyTheirHistoricalLibrarianTiers() throws Exception {
        try (var files = Files.list(TRADES)) {
            assertEquals(GOODS.keySet(), files.map(file -> file.getFileName().toString().replace(".json", ""))
                    .collect(java.util.stream.Collectors.toSet()));
        }
        for (int level : List.of(1, 2)) {
            JsonObject tag = JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/data/minecraft/tags/villager_trade/librarian/level_" + level + ".json")))
                    .getAsJsonObject();
            assertFalse("Vanilla offers must remain available", tag.get("replace").getAsBoolean());
            Set<String> expected = level == 1 ? Set.of("electric_conduit", "solder", "integrated_circuit", "blank_circuit_board")
                    : Set.of("petrolplastic_ingot", "psu");
            assertEquals(expected.stream().map(id -> "baseelectric:librarian/" + id).collect(java.util.stream.Collectors.toSet()),
                    tag.getAsJsonArray("values").asList().stream().map(value -> value.getAsString())
                            .collect(java.util.stream.Collectors.toSet()));
            assertEquals(expected.size(), tag.getAsJsonArray("values").size());
        }
    }

    private static void assertOffer(String id, boolean upper, int emeralds, int goods) throws Exception {
        var json = JsonParser.parseString(Files.readString(TRADES.resolve(id + ".json")));
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        VillagerTrade trade = VillagerTrade.CODEC.parse(ops, json).getOrThrow();
        var roundTrip = VillagerTrade.CODEC.parse(ops, VillagerTrade.CODEC.encodeStart(ops, trade).getOrThrow()).getOrThrow();
        RandomSource random = new LegacyRandomSource(0L) {
            @Override public int nextInt(int bound) { return upper ? bound - 1 : 0; }
        };
        // Constant/uniform providers need no world; use the native context constructor directly.
        var constructor = LootContext.class.getDeclaredConstructor(LootParams.class, RandomSource.class, HolderGetter.Provider.class);
        constructor.setAccessible(true);
        LootContext context = constructor.newInstance(new LootParams(null, new ContextMap.Builder().create(), Map.of(), 0),
                random, registries);
        for (VillagerTrade definition : List.of(trade, roundTrip)) {
            MerchantOffer offer = definition.getOffer(context);
            assertNotNull(id, offer);
            assertTrue(offer.getBaseCostA().is(Items.EMERALD));
            assertEquals(id, emeralds, offer.getBaseCostA().getCount());
            assertTrue(id, offer.getResult().is(GOODS.get(id)));
            assertEquals(id, goods, offer.getResult().getCount());
            assertTrue(offer.getCostB().isEmpty());
            assertEquals(7, offer.getMaxUses());
            assertEquals(1, offer.getXp());
            assertEquals(0.0F, offer.getPriceMultiplier(), 0.0F);
        }
    }
}
