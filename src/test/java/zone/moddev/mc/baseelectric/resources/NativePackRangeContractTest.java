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
    @Test public void pipedDumbTerminalChildUsesOfficialNonInteractiveFlag() throws Exception {
        Path root = Path.of("build/native-pack-resources").toAbsolutePath();
        Files.createDirectories(root);
        Path evidence = Files.createTempDirectory(root, "piped-dumb-");
        var builder = NativePackResourceProof.childProcess(evidence);
        assertEquals("dumb", builder.environment().get("TERM"));
        assertTrue(builder.command().contains("-XX:ActiveProcessorCount=4"));
        for (String name : java.util.List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) {
            assertFalse(builder.environment().containsKey(name));
        }
        assertEquals("terminal.jline", net.minecrell.terminalconsole.TerminalConsoleAppender.JLINE_OVERRIDE_PROPERTY);
        assertEquals(ProcessBuilder.Redirect.PIPE, builder.redirectInput());
        assertEquals(ProcessBuilder.Redirect.PIPE, builder.redirectOutput());
        assertTrue(builder.redirectErrorStream());
        Process child = builder.start();
        child.getOutputStream().close();
        var output = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }
        });
        try {
            assertTrue("Piped native proof timed out", child.waitFor(90, java.util.concurrent.TimeUnit.SECONDS));
            String log = output.get(10, java.util.concurrent.TimeUnit.SECONDS);
            Path logPath = evidence.resolve("piped-resources.log");
            Files.writeString(logPath, log);
            assertEquals(log, 0, child.exitValue());
            NativePackResourceProof.requireNoDiagnostics(logPath, log);
            JsonObject proof = JsonParser.parseString(Files.readString(evidence.resolve("receipt.json"))).getAsJsonObject();
            var property = proof.get("terminalJline");
            assertEquals("Native child must disable JLine using the official property", "false",
                    property.isJsonNull() ? null : property.getAsString());
            assertFalse(proof.get("terminalPresent").getAsBoolean());
            assertTrue(proof.get("interactiveTerminalDisabled").getAsBoolean());
            assertTrue(proof.get("environmentJvmOptionsAbsent").getAsBoolean());
            assertEquals(4, proof.get("availableProcessors").getAsInt());
            assertTrue(proof.getAsJsonArray("jvmArguments").asList().stream()
                    .anyMatch(value -> value.getAsString().equals("-XX:ActiveProcessorCount=4")));
            assertEquals(0, proof.getAsJsonArray("warningsAndErrors").size());
            assertTrue(proof.getAsJsonObject("CLIENT_RESOURCES").get("resourcesVerified").getAsInt() > 0);
            assertTrue(proof.getAsJsonObject("SERVER_DATA").get("resourcesVerified").getAsInt() > 0);
        } finally {
            if (child.isAlive()) child.destroyForcibly().waitFor();
        }
    }

    @Test public void nonInteractiveModeDoesNotWhitelistAnyDiagnostic() throws Exception {
        Path log = Path.of("negative-control.log");
        for (String diagnostic : new String[] {
                "[main/WARN] real resource warning", "[main/ERROR] real resource error", "[main/FATAL] real fatal",
                "2026-11-11T00:00:00Z main WARN Advanced terminal features are not available in this environment",
                "2026-11-11T00:00:00Z main ERROR real bootstrap error", "2026-11-11T00:00:00Z main FATAL fatal bootstrap"
        }) {
            assertThrows(diagnostic, AssertionError.class,
                    () -> NativePackResourceProof.requireNoDiagnostics(log, diagnostic));
        }
        NativePackResourceProof.requireNoDiagnostics(log, "[main/INFO] valid resource receipt");
        Path root = Path.of("build/native-pack-resources").toAbsolutePath();
        Files.createDirectories(root);
        for (String name : java.util.List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) {
            Path evidence = Files.createTempDirectory(root, "inherited-options-negative-");
            Path output = evidence.resolve("rejected.log");
            var command = NativePackResourceProof.childProcess(evidence);
            command.environment().put(name, "-XX:ActiveProcessorCount=3");
            Process child = command.redirectOutput(output.toFile()).start();
            try {
                assertTrue(child.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
                assertNotEquals(0, child.exitValue());
                assertTrue(Files.readString(output), Files.readString(output).contains(
                        "Inherited JVM environment options are forbidden in native proof: " + name));
                assertFalse("Rejected environment must not emit a success receipt", Files.exists(evidence.resolve("receipt.json")));
            } finally {
                if (child.isAlive()) child.destroyForcibly().waitFor();
            }
        }
    }

    @Test public void baseMetalsIsRequiredByNativeModInfoWithOptionalNegativeControl() throws Exception {
        var proof = NativePackResourceProof.read();
        assertEquals("net.minecraftforge.fml.loading.moddiscovery.ModInfo", proof.get("modInfoClass").getAsString());
        var dependencies = proof.getAsJsonObject("dependencies");
        assertEquals(java.util.Set.of("forge", "minecraft", "basepower", "basemetals"), dependencies.keySet());
        var metals = dependencies.getAsJsonObject("basemetals");
        assertTrue("Forge must require Base Metals on both sides", metals.get("mandatory").getAsBoolean());
        assertEquals("[3.1.2.2602001,4.0.0)", metals.get("versionRange").getAsString());
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
                    range.isValueInRange(PackFormat.of(88, 0)));
            assertEquals(type.toString(), type == PackType.SERVER_DATA,
                    range.isValueInRange(PackFormat.of(107, 1)));
            assertFalse(range.isValueInRange(PackFormat.of(83)));
            assertFalse(range.isValueInRange(PackFormat.of(102)));
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
        JsonObject dataOnly = JsonParser.parseString("{\"min_format\":[107,1],\"max_format\":[107,1]}")
                .getAsJsonObject();
        var range = PackFormat.packCodec(PackType.CLIENT_RESOURCES).codec()
                .parse(JsonOps.INSTANCE, dataOnly).getOrThrow();
        assertFalse(range.isValueInRange(PackFormat.of(88, 0)));
        assertTrue(range.isValueInRange(PackFormat.of(107, 1)));
    }
}
