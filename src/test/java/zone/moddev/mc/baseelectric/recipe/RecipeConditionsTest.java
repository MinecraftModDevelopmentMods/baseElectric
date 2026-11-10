package zone.moddev.mc.baseelectric.recipe;

import java.util.Locale;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.Test;
import net.minecraftforge.common.crafting.conditions.ICondition;
import zone.moddev.mc.basepower.PowerAdvantage;
import zone.moddev.mc.basepower.RecipeMode;
import static org.junit.Assert.*;

public class RecipeConditionsTest {
    @Test public void nativeConditionsRoundTripAndEvaluateEveryRecipeMode() {
        RecipeMode original = PowerAdvantage.recipeMode;
        try {
            for (boolean inverted : new boolean[] {false, true}) {
                var codec = inverted ? RecipeConditions.NOT_MATCH : RecipeConditions.MATCH;
                for (RecipeMode expected : RecipeMode.values()) {
                    JsonObject source = new JsonObject();
                    source.addProperty("mode", expected.name().toLowerCase(Locale.ROOT));
                    var condition = codec.codec().parse(JsonOps.INSTANCE, source).getOrThrow();
                    assertSame(codec, condition.codec());
                    assertEquals(source, codec.codec().encodeStart(JsonOps.INSTANCE, condition).getOrThrow());
                    for (RecipeMode selected : RecipeMode.values()) {
                        PowerAdvantage.recipeMode = selected;
                        assertEquals((selected == expected) != inverted,
                                condition.test(ICondition.IContext.EMPTY, JsonOps.INSTANCE));
                    }
                }
            }
        } finally {
            PowerAdvantage.recipeMode = original;
        }
    }

    @Test public void modeNamesRemainCaseInsensitiveUnderTurkishLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            JsonObject json = new JsonObject();
            json.addProperty("mode", "apocalyptic");
            assertEquals(RecipeMode.APOCALYPTIC,
                    RecipeConditions.MATCH.codec().parse(JsonOps.INSTANCE, json).getOrThrow().mode());
        } finally {
            Locale.setDefault(original);
        }
    }
}
