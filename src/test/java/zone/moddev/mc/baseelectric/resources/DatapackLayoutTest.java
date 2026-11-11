package zone.moddev.mc.baseelectric.resources;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class DatapackLayoutTest {
    @Test public void onlyRegistryDirectoriesBecomeSingular() throws Exception {
        Path data = Path.of("src/main/resources/data");
        try (var namespaces = Files.list(data)) {
            for (Path namespace : namespaces.toList()) {
                for (String obsolete : new String[] {"recipes", "advancements", "loot_tables", "structures",
                        "tags/items", "tags/blocks"}) {
                    Path old = namespace.resolve(obsolete);
                    if (Files.exists(old)) try (var files = Files.walk(old)) {
                        assertFalse(old.toString(), files.anyMatch(Files::isRegularFile));
                    }
                }
            }
        }
        assertTrue(Files.isRegularFile(data.resolve("baseelectric/advancement/recipes/alkaline_battery.json")));
        assertTrue(Files.isRegularFile(data.resolve("baseelectric/loot_table/blocks/electric_drill.json")));
        assertTrue(Files.isRegularFile(data.resolve("forge/tags/item/ores/sulphur.json")));
        assertTrue(Files.isRegularFile(data.resolve("forge/tags/block/ores/sulphur.json")));
    }

    @Test public void pumpHelperHasNoLootAndRemovesItsColumnWithoutDrops() throws Exception {
        Path resources = Path.of("src/main/resources");
        assertFalse(Files.exists(resources.resolve("data/baseelectric/loot_table/blocks/pump_pipe_electric.json")));
        String block = Files.readString(Path.of("src/main/java/zone/moddev/mc/baseelectric/blocks/PumpPipeBlock.java"));
        assertTrue(block.contains("noLootTable()"));
        assertTrue(block.contains("world.setBlock(cursor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3)"));
        assertFalse(block.contains("destroyBlock("));
    }
}
