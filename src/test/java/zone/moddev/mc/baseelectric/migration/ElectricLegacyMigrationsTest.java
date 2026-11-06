package zone.moddev.mc.baseelectric.migration;

import static org.junit.Assert.assertEquals;

import cyano.poweradvantage.api.migration.LegacyMigrationApi;
import net.minecraft.util.Direction;
import org.junit.Test;

public class ElectricLegacyMigrationsTest {
    @Test public void preservesAllSixDrillFacingsAndActiveFlag() {
        for (Direction facing : Direction.values()) {
            int metadata = facing.getIndex() | 8;
            assertEquals(facing, ElectricLegacyMigrations.legacyDrillFacing(metadata));
            assertEquals(true, ElectricLegacyMigrations.legacyDrillActive(metadata));
        }
    }

    @Test public void aliasesLegacyTileAndHydroturbineIds() {
        ElectricLegacyMigrations.register();
        assertEquals("baseelectric:led", LegacyMigrationApi.tileTarget("baseelectric:l_e_d"));
        assertEquals("baseelectric:led", LegacyMigrationApi.tileTarget("baseelectric.l_e_d"));
        assertEquals("baseelectric:electric_furnace",
                LegacyMigrationApi.tileTarget("minecraft:baseelectric.electric_furnace"));
        assertEquals("baseelectric:hydroturbine",
                LegacyMigrationApi.entityTarget("minecraft:baseelectric.Hydroturbine"));
        assertEquals("baseelectric:hydroturbine",
                LegacyMigrationApi.entityTarget("baseelectric.baseelectric.Hydroturbine"));
        assertEquals("baseelectric:hydroturbine",
                LegacyMigrationApi.entityTarget("minecraft:electricadvantage.Hydroturbine"));
        assertEquals("baseelectric:led", LegacyMigrationApi.tileTarget("electricadvantage:l_e_d"));
    }
}
