package zone.moddev.mc.baseelectric.init;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.*;

public class LegacyOreSpawnMigrationTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void legacyMineralsBecomeExactlyTwoRetainedRulesAndKeepAuthority() throws Exception {
        JsonObject legacy;
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/orespawn-legacy/electricadvantage.json"),
                StandardCharsets.UTF_8)) {
            legacy = new JsonParser().parse(reader).getAsJsonObject();
        }
        Class<?> bridge = Class.forName("com.mcmoddev.orespawn.compat.LegacyOs3Bridge");
        Method translate = bridge.getDeclaredMethod("translateOs1ForTests", String.class, JsonObject.class);
        translate.setAccessible(true);
        JsonObject converted = (JsonObject) translate.invoke(null, "electricadvantage", legacy);
        assertEquals(2, converted.getAsJsonObject("spawns").entrySet().size());
        assertEquals(2, OreSpawnWorldGen.createProvider(false, false).toJson()
                .getAsJsonObject("ores").entrySet().size());

        Path config = temporary.newFolder("config").toPath();
        Path legacyDirectory = Files.createDirectories(config.resolve("orespawn"));
        Path old = legacyDirectory.resolve("electricadvantage.json");
        copyOriginalOs1(old);
        byte[] original = Files.readAllBytes(old);
        assertTrue(OreSpawnWorldGen.migrateConfiguration(config) > 0);
        Path target = legacyDirectory.resolve("baseelectric.json");
        assertFalse(Files.exists(old));
        assertEquals("config/orespawn/baseelectric.json (legacy importer authority)",
                OreSpawnWorldGen.describeOverride(config));
        assertBackupContains(old, original);

        JsonObject expected = new JsonParser().parse(legacy.toString()).getAsJsonObject();
        for (JsonElement dimension : expected.getAsJsonArray("dimensions")) {
            for (JsonElement ore : dimension.getAsJsonObject().getAsJsonArray("ores")) {
                JsonObject rule = ore.getAsJsonObject();
                rule.addProperty("blockID", rule.get("blockID").getAsString()
                        .replace("electricadvantage:", "baseelectric:"));
            }
        }
        JsonObject migrated = read(target);
        assertEquals("Only owned block references change; placement and restrictions are retained",
                expected, migrated);
        JsonObject reimported = (JsonObject) translate.invoke(null, "baseelectric", migrated);
        assertEquals(2, reimported.getAsJsonObject("spawns").entrySet().size());
        assertFalse(Files.exists(config.resolve("electricadvantage-orespawn.json")));
        assertEquals(0, OreSpawnWorldGen.migrateConfiguration(config));
        assertEquals(migrated, read(target));
    }

    @Test public void explicitModernOverrideRetainsItsSettingsAndRetiresTheOS1Input() throws Exception {
        Path config = temporary.newFolder("modern").toPath();
        Path legacyOs1 = config.resolve("orespawn/electricadvantage.json");
        copyOriginalOs1(legacyOs1);
        byte[] originalOs1 = Files.readAllBytes(legacyOs1);
        Path legacyProvider = config.resolve("electricadvantage-orespawn.json");
        JsonObject provider = legacyProvider();
        provider.addProperty("provider_revision", 17);
        provider.addProperty("notes", "Retain electricadvantage:free_text literally");
        provider.getAsJsonObject("ores").getAsJsonObject("electricadvantage:ore/lithium")
                .addProperty("enabled", false);
        byte[] originalProvider = provider.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(legacyProvider, originalProvider);

        assertTrue(OreSpawnWorldGen.migrateConfiguration(config) > 0);
        JsonObject migrated = read(config.resolve("baseelectric-orespawn.json"));
        assertEquals("baseelectric", migrated.get("provider_modid").getAsString());
        assertEquals(17, migrated.get("provider_revision").getAsInt());
        assertEquals(provider.get("notes"), migrated.get("notes"));
        JsonObject ores = migrated.getAsJsonObject("ores");
        assertEquals(2, ores.entrySet().size());
        assertFalse(ores.getAsJsonObject("baseelectric:ore/lithium").get("enabled").getAsBoolean());
        assertEquals("baseelectric:li_ore", ores.getAsJsonObject("baseelectric:ore/lithium").get("block").getAsString());
        assertEquals("baseelectric:sulfur_ore", ores.getAsJsonObject("baseelectric:ore/sulfur").get("block").getAsString());
        assertFalse(Files.exists(legacyProvider));
        assertFalse(Files.exists(legacyOs1));
        assertFalse("An explicit provider must not leave another owned input for the OS1 importer",
                Files.exists(config.resolve("orespawn/baseelectric.json")));
        assertBackupContains(legacyProvider, originalProvider);
        assertBackupContains(legacyOs1, originalOs1);
        assertEquals("config/baseelectric-orespawn.json (authoritative)",
                OreSpawnWorldGen.describeOverride(config));
        assertEquals(0, OreSpawnWorldGen.migrateConfiguration(config));
    }

    @Test public void conflictingOldAndNewAuthoritiesAbortWithoutChangingEitherFile() throws Exception {
        Path config = temporary.newFolder("conflicting").toPath();
        Path old = config.resolve("electricadvantage-orespawn.json");
        byte[] original = legacyProvider().toString().getBytes(StandardCharsets.UTF_8);
        Files.write(old, original);
        JsonObject custom = OreSpawnWorldGen.createProvider(false, false).toJson();
        custom.getAsJsonObject("ores").getAsJsonObject("baseelectric:ore/lithium")
                .addProperty("enabled", false);
        Path canonical = config.resolve("baseelectric-orespawn.json");
        byte[] customBytes = custom.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(canonical, customBytes);
        assertMigrationAborts(config);
        assertArrayEquals(original, Files.readAllBytes(old));
        assertArrayEquals(customBytes, Files.readAllBytes(canonical));
    }

    @Test public void completedBridgeMarkerCannotSilentlyDiscardAnUnimportedOS1File() throws Exception {
        Path config = temporary.newFolder("incomplete").toPath();
        Path old = config.resolve("orespawn/electricadvantage.json");
        copyOriginalOs1(old);
        byte[] original = Files.readAllBytes(old);
        Files.write(config.resolve(".orespawn-legacy-bridge-migrated"), new byte[0]);
        assertMigrationAborts(config);
        assertArrayEquals(original, Files.readAllBytes(old));
        assertFalse(Files.exists(config.resolve("orespawn/baseelectric.json")));
    }

    private static void assertMigrationAborts(Path config) {
        try {
            OreSpawnWorldGen.migrateConfiguration(config);
            fail("Migration failure must abort startup before enqueueing defaults");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getCause() instanceof IOException);
        }
    }

    private static JsonObject legacyProvider() {
        JsonObject provider = OreSpawnWorldGen.createProvider(false, false).toJson();
        provider.addProperty("provider_modid", "electricadvantage");
        JsonObject oldOres = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : provider.getAsJsonObject("ores").entrySet()) {
            JsonObject ore = new JsonParser().parse(entry.getValue().toString()).getAsJsonObject();
            ore.addProperty("block", ore.get("block").getAsString().replace("baseelectric:", "electricadvantage:"));
            oldOres.add(entry.getKey().replace("baseelectric:", "electricadvantage:"), ore);
        }
        provider.add("ores", oldOres);
        return provider;
    }

    private void copyOriginalOs1(Path target) throws Exception {
        Files.createDirectories(target.getParent());
        try (InputStream source = getClass().getResourceAsStream("/orespawn-legacy/electricadvantage.json")) {
            Files.copy(source, target);
        }
    }

    private static JsonObject read(Path path) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    private static void assertBackupContains(Path source, byte[] original) throws IOException {
        int count = 0;
        try (DirectoryStream<Path> backups = Files.newDirectoryStream(source.getParent(),
                source.getFileName() + ".before-baseelectric-*.bak")) {
            for (Path backup : backups) {
                assertArrayEquals(original, Files.readAllBytes(backup));
                count++;
            }
        }
        assertEquals("Exactly one immutable byte-preserving backup per source", 1, count);
    }
}
