package zone.moddev.mc.baseelectric.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.GsonHelper;
import org.junit.Test;
import static org.junit.Assert.*;

/** Audits real dependency recipe bytes, not a replacement recipe or a runtime job. */
public class BaseMetalsRawRecipeContractTest {
    @Test public void nativeCrushingRecipesKeepRawAndOreOutputsDistinct() throws Exception {
        for (String metal : new String[]{"copper", "iron", "gold"}) {
            for (String input : new String[]{"ore", "raw"}) {
                String path = "/data/basemetals/recipes/" + metal + "_" + input + "_crushing.json";
                try (InputStream stream = getClass().getResourceAsStream(path)) {
                    assertNotNull(path, stream);
                    JsonObject recipe = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                    assertEquals("basemetals:crushing", recipe.get("type").getAsString());
                    assertEquals("forge:" + (input.equals("ore") ? "ores/" : "raw_materials/") + metal,
                            recipe.getAsJsonObject("ingredient").get("tag").getAsString());
                    assertEquals("basemetals:" + metal + "_powder", recipe.getAsJsonObject("result").get("item").getAsString());
                    // Base Metals delegates to CraftingHelper, whose omitted count defaults to one.
                    assertEquals(input.equals("ore") ? 2 : 1, GsonHelper.getAsInt(recipe.getAsJsonObject("result"), "count", 1));
                }
            }
        }
    }
}
