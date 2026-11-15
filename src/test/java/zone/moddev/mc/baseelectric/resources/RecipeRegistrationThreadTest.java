package zone.moddev.mc.baseelectric.resources;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecipeRegistrationThreadTest {
    @Test public void sharedForgeConditionsAreRegisteredAfterParallelSetup() throws Exception {
        Path root = Path.of("src/main/java/zone/moddev/mc/baseelectric");
        String entry = Files.readString(root.resolve("ElectricAdvantage.java"));
        String conditions = Files.readString(root.resolve("recipe/RecipeConditions.java"));
        assertTrue(entry.contains("RecipeConditions.register(bus)"));
        assertFalse(entry.contains("RecipeConditions::register"));
        assertTrue(conditions.contains("DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS"));
        assertTrue(conditions.contains("SERIALIZERS.register(bus)"));
        assertFalse(conditions.contains("CraftingHelper"));
    }
}
