package zone.moddev.mc.electricadvantage.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Audits real dependency recipe bytes, not a replacement recipe or a runtime job. */
public class BaseMetalsRawRecipeContractTest {
    @Test public void publishedOneSeventeenRecipesKeepRawAndOreOutputsDistinct() throws Exception {
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
                    assertEquals(input.equals("ore") ? 2 : 1, recipe.getAsJsonObject("result").get("count").getAsInt());
                }
            }
        }
    }
}
