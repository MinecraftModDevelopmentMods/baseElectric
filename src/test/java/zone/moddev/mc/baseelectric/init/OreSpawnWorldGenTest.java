package zone.moddev.mc.baseelectric.init;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class OreSpawnWorldGenTest {
    @Test public void electricOwnsBothMaterialsWhenNoPreferredProviderIsLoaded() {
        assertDefaults(false, false, true, true);
    }

    @Test public void mineralogyOwnsSulfurButElectricStillOwnsLithium() {
        assertDefaults(true, false, false, true);
    }

    @Test public void baseMineralsOwnsBothFallbackMaterials() {
        assertDefaults(false, true, false, false);
    }

    @Test public void preferredProvidersDisableBothElectricAlternatives() {
        assertDefaults(true, true, false, false);
    }

    @Test public void constructorRegistersMigrationBeforeLifecycleAndFailsClosed() throws Exception {
        ClassNode entry = bytecode("zone/moddev/mc/baseelectric/ElectricAdvantage");
        List<String> constructor = calls(method(entry, "<init>"));
        assertTrue(constructor.contains("migrateLegacyOreSpawnConfiguration") && constructor.contains("registerConfig"));
        assertTrue(constructor.indexOf("migrateLegacyOreSpawnConfiguration")
                < constructor.indexOf("registerConfig"));
        MethodNode migration = method(entry, "migrateLegacyOreSpawnConfiguration");
        List<String> migrationCalls = calls(migration);
        assertTrue(migrationCalls.contains("migrate"));
        assertEquals(1, migrationCalls.stream().filter("registerMigrationDefaults"::equals).count());
        assertTrue(migrationCalls.indexOf("migrate") < migrationCalls.indexOf("registerMigrationDefaults"));
        assertTrue(migration.tryCatchBlocks.stream().anyMatch(block -> "java/io/IOException".equals(block.type)));
        assertTrue(java.util.stream.StreamSupport.stream(migration.instructions.spliterator(), false)
                .anyMatch(instruction -> instruction.getOpcode() == Opcodes.ATHROW));
        assertTrue(!migrationCalls.contains("error"));

        MethodNode registration = method(bytecode("zone/moddev/mc/baseelectric/init/OreSpawnWorldGen"),
                "registerMigrationDefaults");
        assertTrue((registration.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC))
                == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
        assertTrue(registration.exceptions.contains("java/io/IOException"));
        List<String> registrationCalls = calls(registration);
        assertEquals(1, registrationCalls.stream().filter("registerProvider"::equals).count());
        assertEquals(2, registrationCalls.stream().filter("isLoaded"::equals).count());
        assertTrue(registrationCalls.contains("createProvider") && registrationCalls.contains("toJson"));
        assertTrue(!registrationCalls.contains("enqueue"));
    }

    @Test public void migrationDefaultsKeepOwnedNativeRangesAndFallbackHierarchy() {
        JsonObject root = OreSpawnWorldGen.createProvider(false, false).toJson();
        assertEquals("baseelectric", root.get("provider_modid").getAsString());
        for (String material : new String[] {"lithium", "sulfur"}) {
            JsonObject rule = root.getAsJsonObject("ores").getAsJsonObject("baseelectric:ore/" + material);
            assertEquals("baseelectric:" + (material.equals("lithium") ? "li_ore" : "sulfur_ore"),
                    rule.get("block").getAsString());
            JsonObject dimension = rule.getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld");
            assertEquals(1, dimension.get("min_y").getAsInt());
            assertEquals(31, dimension.get("max_y").getAsInt());
            assertEquals(material.equals("lithium") ? 0.125 : 1.0, dimension.get("frequency").getAsDouble(), 0.0);
            assertEquals(material.equals("lithium") ? 6 : 8, dimension.get("min_quantity").getAsInt());
            assertEquals(material.equals("lithium") ? 9 : 23, dimension.get("max_quantity").getAsInt());
        }
    }

    private static ClassNode bytecode(String name) throws Exception {
        try (InputStream input = OreSpawnWorldGenTest.class.getResourceAsStream("/" + name + ".class")) {
            assertTrue("Missing actual compiled production class", input != null);
            ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, 0);
            return result;
        }
    }

    private static MethodNode method(ClassNode type, String name) {
        return type.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    private static List<String> calls(MethodNode method) {
        List<String> names = new ArrayList<>();
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) names.add(call.name);
        }
        return names;
    }

    private static void assertDefaults(boolean mineralogy, boolean baseMinerals,
            boolean sulfurEnabled, boolean lithiumEnabled) {
        JsonObject root = OreSpawnWorldGen.createProvider(mineralogy, baseMinerals).toJson();
        JsonObject ores = root.getAsJsonObject("ores");
        assertEquals(sulfurEnabled,
                ores.getAsJsonObject("baseelectric:ore/sulfur").get("enabled").getAsBoolean());
        assertEquals(lithiumEnabled,
                ores.getAsJsonObject("baseelectric:ore/lithium").get("enabled").getAsBoolean());
        assertEquals(2, ores.size());
    }
}
