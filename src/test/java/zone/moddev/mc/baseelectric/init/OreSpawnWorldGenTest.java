package zone.moddev.mc.baseelectric.init;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OreSpawnWorldGenTest {
    @Test
    public void electricOwnsBothMineralsWhenNoPreferredProviderIsLoaded() {
        assertOwnership(false, false, true, true);
    }

    @Test
    public void mineralogyOwnsSulfurButElectricStillOwnsLithium() {
        assertOwnership(true, false, false, true);
    }

    @Test
    public void baseMineralsOwnsBothMineralsWithoutOmittingElectricRules() {
        assertOwnership(false, true, false, false);
    }

    @Test
    public void allProvidersStillSerializeBothDisabledFallbackRules() {
        assertOwnership(true, true, false, false);
    }

    private static void assertOwnership(boolean mineralogy, boolean baseMinerals,
            boolean sulfurEnabled, boolean lithiumEnabled) {
        JsonObject root = OreSpawnWorldGen.createProvider(mineralogy, baseMinerals).toJson();
        assertEquals(4, root.get("schema_version").getAsInt());
        assertEquals("baseelectric", root.get("provider_modid").getAsString());
        JsonObject ores = root.getAsJsonObject("ores");
        assertEquals(2, ores.entrySet().size());
        assertFalse(ores.has("electricadvantage:ore/lithium"));
        assertFalse(ores.has("electricadvantage:ore/sulfur"));
        JsonObject lithium = ores.getAsJsonObject("baseelectric:ore/lithium");
        JsonObject sulfur = ores.getAsJsonObject("baseelectric:ore/sulfur");
        assertEquals("baseelectric:li_ore", lithium.get("block").getAsString());
        assertEquals("baseelectric:sulfur_ore", sulfur.get("block").getAsString());
        assertEquals(lithiumEnabled, lithium.get("enabled").getAsBoolean());
        assertEquals(sulfurEnabled, sulfur.get("enabled").getAsBoolean());
        assertLegacyPlacement(lithium, 0.125D, 6, 9);
        assertLegacyPlacement(sulfur, 1.0D, 8, 23);
        if (baseMinerals) {
            assertFalse(lithium.get("enabled").getAsBoolean());
            assertFalse(sulfur.get("enabled").getAsBoolean());
        }
    }

    private static void assertLegacyPlacement(JsonObject ore, double frequency,
            int minQuantity, int maxQuantity) {
        assertFalse(ore.get("retrogen").getAsBoolean());
        JsonObject placement = ore.getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld");
        assertTrue(placement.get("enabled").getAsBoolean());
        assertEquals(1, placement.get("min_y").getAsInt());
        assertEquals(31, placement.get("max_y").getAsInt());
        assertEquals(frequency, placement.get("frequency").getAsDouble(), 0.0D);
        assertEquals(minQuantity, placement.get("min_quantity").getAsInt());
        assertEquals(maxQuantity, placement.get("max_quantity").getAsInt());
        assertEquals(4, placement.getAsJsonArray("host_families").size());
    }
}
