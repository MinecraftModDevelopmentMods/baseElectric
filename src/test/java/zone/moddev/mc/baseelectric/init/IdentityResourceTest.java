package zone.moddev.mc.baseelectric.init;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraftforge.fml.common.Mod;
import org.junit.Test;
import zone.moddev.mc.baseelectric.ElectricAdvantage;

import static org.junit.Assert.*;

public class IdentityResourceTest {
    @Test public void entryPointAndPackagedMetadataAgreeOnTheNewIdentity() throws Exception {
        Mod mod = ElectricAdvantage.class.getAnnotation(Mod.class);
        assertEquals("baseelectric", mod.modid());
        assertEquals("baseElectric", mod.name());
        assertEquals("2.2.6.110021", mod.version());
        assertEquals("required-after:orespawn@[4.0.6,5.0.0);required-after:basepower@[2.4.4.110021,);after:basemetals",
                mod.dependencies());
        try (InputStream stream = getClass().getResourceAsStream("/mcmod.info");
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject metadata = new JsonParser().parse(reader).getAsJsonArray().get(0).getAsJsonObject();
            assertEquals(mod.modid(), metadata.get("modid").getAsString());
            assertEquals(mod.name(), metadata.get("name").getAsString());
        }
    }

    @Test public void assetsUseTheNewDomainWithoutKeepingAnOldDuplicateTree() throws Exception {
        for (String path : new String[] {"lang/en_US.lang", "models/item/psu.json",
                "blockstates/electric_drill.json", "textures/entity/hydroturbine.png"}) {
            try (InputStream current = getClass().getResourceAsStream("/assets/baseelectric/" + path);
                    InputStream obsolete = getClass().getResourceAsStream("/assets/electricadvantage/" + path)) {
                assertNotNull(path, current);
                assertNull(path, obsolete);
            }
        }
        try (InputStream stream = getClass().getResourceAsStream("/assets/baseelectric/models/item/psu.json");
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject model = new JsonParser().parse(reader).getAsJsonObject();
            assertEquals("baseelectric:item/generic_item", model.get("parent").getAsString());
            assertEquals("baseelectric:items/psu", model.getAsJsonObject("textures").get("item_texture").getAsString());
        }
    }
}
