package zone.moddev.mc.baseelectric.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import org.junit.Test;
import zone.moddev.mc.baseelectric.verification.NativePackResourceProof;
import static org.junit.Assert.*;

public class NativePackRangeContractTest {
    @Test public void baseMetalsIsRequiredByNativeModInfoWithOptionalNegativeControl() throws Exception {
        var proof = NativePackResourceProof.read();
        assertEquals("net.minecraftforge.fml.loading.moddiscovery.ModInfo", proof.get("modInfoClass").getAsString());
        var dependencies = proof.getAsJsonObject("dependencies");
        assertEquals(java.util.Set.of("forge", "minecraft", "basepower", "basemetals"), dependencies.keySet());
        var metals = dependencies.getAsJsonObject("basemetals");
        assertTrue("Forge must require Base Metals on both sides", metals.get("mandatory").getAsBoolean());
        assertEquals("[3.1.2.121111,4.0.0)", metals.get("versionRange").getAsString());
        assertEquals("AFTER", metals.get("ordering").getAsString());
        assertEquals("BOTH", metals.get("side").getAsString());
        var control = proof.getAsJsonObject("optionalBaseMetalsControl");
        assertFalse("Native ModInfo must detect optional metadata", control.getAsJsonObject("basemetals")
                .get("mandatory").getAsBoolean());
        var expectedControl = dependencies.deepCopy();
        expectedControl.getAsJsonObject("basemetals").addProperty("mandatory", false);
        assertEquals("Only the control dependency requirement may differ", expectedControl, control);
    }

    @Test public void combinedPackRangeAcceptsBothNativePackTypes() throws Exception {
        var proof = NativePackResourceProof.read();
        assertEquals(0, proof.getAsJsonArray("warningsAndErrors").size());
        for (PackType type : PackType.values()) {
            var side = proof.getAsJsonObject(type.name());
            JsonObject pack = side.getAsJsonObject("nativeMetadata");
            var range = PackFormat.packCodec(type).codec().parse(JsonOps.INSTANCE, pack).getOrThrow();
            assertEquals(type.toString(), type == PackType.CLIENT_RESOURCES,
                    range.isValueInRange(PackFormat.of(75, 0)));
            assertEquals(type.toString(), type == PackType.SERVER_DATA,
                    range.isValueInRange(PackFormat.of(94, 1)));
            assertFalse(range.isValueInRange(PackFormat.of(74)));
            assertFalse(range.isValueInRange(PackFormat.of(95)));
        }
    }

    @Test public void omissionDoesNotSynthesizeMetadataInTheNativeModPathSupplier() throws Exception {
        var root = Path.of("build/native-pack-resources").toAbsolutePath();
        Files.createDirectories(root);
        var omitted = Files.createTempDirectory(root, "missing-metadata-");
        var info = new net.minecraft.server.packs.PackLocationInfo("mod:baseelectric",
                net.minecraft.network.chat.Component.literal("baseElectric"),
                net.minecraft.server.packs.repository.PackSource.DEFAULT, java.util.Optional.empty());
        var supplier = new net.minecraft.server.packs.PathPackResources.PathResourcesSupplier(omitted);
        try (var resources = supplier.openPrimary(info)) {
            assertNull(resources.getRootResource("pack.mcmeta"));
            for (PackType type : PackType.values()) {
                assertNull(type.toString(), resources.getMetadataSection(
                        net.minecraft.server.packs.metadata.pack.PackMetadataSection.forPackType(type)));
            }
        }
    }

    @Test public void dataOnlyRangeDoesNotQualifyClientResources() {
        JsonObject dataOnly = JsonParser.parseString("{\"min_format\":[94,1],\"max_format\":[94,1]}")
                .getAsJsonObject();
        var range = PackFormat.packCodec(PackType.CLIENT_RESOURCES).codec()
                .parse(JsonOps.INSTANCE, dataOnly).getOrThrow();
        assertFalse(range.isValueInRange(PackFormat.of(75, 0)));
        assertTrue(range.isValueInRange(PackFormat.of(94, 1)));
    }
}
