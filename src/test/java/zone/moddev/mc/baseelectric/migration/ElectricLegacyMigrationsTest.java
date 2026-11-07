package zone.moddev.mc.baseelectric.migration;

import static org.junit.Assert.assertEquals;

import cyano.poweradvantage.api.migration.LegacyMigrationApi;
import net.minecraft.core.Direction;
import org.junit.Test;

public class ElectricLegacyMigrationsTest {
    @Test public void preservesAllSixDrillFacingsAndActiveFlag() {
        for (Direction facing : Direction.values()) {
            int metadata = facing.get3DDataValue() | 8;
            assertEquals(facing, ElectricLegacyMigrations.legacyDrillFacing(metadata));
            assertEquals(true, ElectricLegacyMigrations.legacyDrillActive(metadata));
        }
    }

    @Test public void aliasesLegacyTileAndHydroturbineIds() {
        ElectricLegacyMigrations.register();
        for (String namespace : new String[] {"electricadvantage", "baseelectric"}) {
            for (String prefix : new String[] {namespace + ":", namespace + ".", "minecraft:" + namespace + "."}) {
                assertEquals("baseelectric:led", LegacyMigrationApi.tileTarget(prefix + "l_e_d"));
                assertEquals("baseelectric:electric_furnace", LegacyMigrationApi.tileTarget(prefix + "electric_furnace"));
                assertEquals("baseelectric:hydroturbine", LegacyMigrationApi.entityTarget(prefix + "hydroturbine"));
            }
            String repeated = namespace + "." + namespace + ".hydroturbine";
            assertEquals("baseelectric:hydroturbine", LegacyMigrationApi.entityTarget(repeated));
            assertEquals("baseelectric:hydroturbine", LegacyMigrationApi.entityTarget("minecraft:" + repeated));
        }
        assertEquals("baseelectric:hydroturbine",
                LegacyMigrationApi.entityTarget("minecraft:electricadvantage.Hydroturbine"));
        assertEquals("baseelectric:hydroturbine",
                LegacyMigrationApi.entityTarget("electricadvantage.electricadvantage.Hydroturbine"));
    }
}
