package zone.moddev.mc.baseelectric.resources;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

public class BuildWorkflowContractTest {
    @Test public void hostedBuildsProvisionAllThreePinnedToolchainsBeforeGradle() throws Exception {
        for (String workflow : new String[] {"ci.yml", "codeql-analysis.yml"}) {
            String source = new String(Files.readAllBytes(Paths.get(".github/workflows", workflow)), StandardCharsets.UTF_8);
            int setup = source.indexOf("- name: Set up Gradle");
            for (String version : new String[] {"25.0.3+9.0.LTS", "16.0.2+7", "8.0.502+7", "17.0.1+12"}) {
                int pin = source.indexOf("java-version: '" + version + "'");
                assertTrue(workflow + " missing pinned JDK " + version, pin >= 0 && pin < setup);
            }
            assertTrue(source.contains("-Porg.gradle.java.installations.paths=$JAVA_HOME,$JAVA_HOME_16_X64,$JAVA_HOME_8_X64,$JAVA_HOME_25_X64"));
            assertTrue(source.contains("-Porg.gradle.java.installations.auto-detect=false"));
            assertTrue(source.contains("-Porg.gradle.java.installations.auto-download=false"));
            assertFalse(source.contains("-Dorg.gradle.java.installations."));
            for (String line : source.split("\\R")) {
                if (line.contains("./gradlew ") && !line.contains("chmod")) {
                    assertTrue(workflow + ": " + line, line.contains("./gradlew $MMD_GRADLE_TOOLCHAINS "));
                }
            }
            assertTrue(source.indexOf("- name: Use pinned build toolchains") < setup);
            assertFalse(source.contains("distribution: microsoft"));
        }
    }

    @Test public void dependencyPolicyPrecedesEveryPinnedCheckout() throws Exception {
        for (String workflow : new String[] {"ci.yml", "codeql-analysis.yml"}) {
            String source = new String(Files.readAllBytes(Paths.get(".github/workflows", workflow)), StandardCharsets.UTF_8);
            int policy = source.indexOf("git config --global core.autocrlf false");
            int pins = source.indexOf("- name: Require coordinated 1.17.1 dependency pins");
            assertTrue(policy > 0);
            assertTrue(policy < pins);
            for (String repository : new String[] {"OreSpawn", "BaseMetals", "basePower"}) {
                assertTrue(pins < source.indexOf("repository: MinecraftModDevelopmentMods/" + repository));
            }
            assertTrue(source.contains("ref: b56e2b62a1a6ea4d1dd96f8646a87801a817dbdf"));
            assertTrue(source.contains("BaseMetals-3.1.1.117011-deobf.jar"));
            assertTrue(source.contains("basePower-2.4.9.117011-deobf.jar"));
            assertTrue(source.contains("ref: ${{ steps.pins.outputs.basemetals_ref }}"));
            assertTrue(source.contains("ref: ${{ steps.pins.outputs.power_ref }}"));
            assertTrue(source.contains("\"$pin_state\" != \"LOCAL_CHECKPOINT\""));
            assertTrue(source.contains("! \"$power_ref\" =~ ^[a-f0-9]{40}$"));
            assertTrue(source.contains("! \"$bm_ref\" =~ ^[a-f0-9]{40}$"));
            for (String hash : new String[] {"power_sha", "bm_sha", "bootstrap_sha"}) {
                assertTrue(source.contains("! \"$" + hash + "\" =~ ^[A-F0-9]{64}$"));
            }
            assertTrue(source.contains("git -C \"$power\" rev-parse HEAD"));
            assertTrue(source.contains("git -C \"$basemetals\" rev-parse HEAD"));
            assertFalse(source.contains("5b9abe578b053d2ef59b1962cf9e04f6880c91b9"));
            assertFalse(source.contains("799B59B06EB2AC3FC4682AA64BF42C3A2A2C1D89D7FFFE9C72745DEC7DFBE0D8"));
            assertFalse(source.contains("dacfaa3af2e3310d5cb6b081da79f09260ae103d"));
            assertFalse(source.contains("098d4625abc8a849bd1a11dcf697b3fee0cade7b"));
            assertFalse(source.contains("78E58DD2AB04E2B9F3267E5735C64AAE7E93EAC97571CB04485038862C853ED3"));
            assertFalse(source.contains("ce90c24c302d0ff3565db2db0c6c94a3b5f21f83"));
            assertFalse(source.contains("8BBB03DD7E4AF08A7390A161ED5AA94E292BEE56EF60E550B5512E967E0E9174"));
            assertTrue(source.contains("branches: [master-1.17.1"));
            assertFalse(source.contains("master-1.14.4"));
        }
    }
}
