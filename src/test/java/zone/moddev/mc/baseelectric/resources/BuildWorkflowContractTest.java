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
            for (String version : new String[] {"25.0.3+9.0.LTS", "8.0.502+7", "21.0.7+6.0.LTS"}) {
                int pin = source.indexOf("java-version: '" + version + "'");
                assertTrue(workflow + " missing pinned JDK " + version, pin >= 0 && pin < setup);
            }
            assertTrue(source.contains("-Porg.gradle.java.installations.paths=$JAVA_HOME,$JAVA_HOME_8_X64,$JAVA_HOME_25_X64"));
            assertTrue(source.contains("-Porg.gradle.java.installations.auto-detect=false"));
            assertTrue(source.contains("-Porg.gradle.java.installations.auto-download=false"));
            assertFalse(source.contains("-Dorg.gradle.java.installations."));
            for (String line : source.split("\\R")) {
                if (line.contains("./gradlew ") && !line.contains("chmod")) {
                    assertTrue(workflow + ": " + line, line.contains("./gradlew $MMD_GRADLE_TOOLCHAINS "));
                }
            }
            assertTrue(source.indexOf("- name: Use pinned build toolchains") < setup);
            assertFalse(source.contains("JAVA_HOME_16_X64"));
            assertFalse(source.contains("distribution: microsoft"));
        }
    }

    @Test public void dependencyPolicyPrecedesEveryPinnedCheckout() throws Exception {
        for (String workflow : new String[] {"ci.yml", "codeql-analysis.yml"}) {
            String source = new String(Files.readAllBytes(Paths.get(".github/workflows", workflow)), StandardCharsets.UTF_8);
            int policy = source.indexOf("git config --global core.autocrlf false");
            int published = source.indexOf("bash .github/verify-published-pins.sh");
            assertTrue(policy > 0);
            assertTrue(policy < published);
            for (String repository : new String[] {"OreSpawn", "basePower"}) {
                assertTrue(published < source.indexOf("repository: MinecraftModDevelopmentMods/" + repository));
            }
            assertTrue(source.contains("ref: ${{ steps.public-pins.outputs.orespawn_ref }}"));
            assertTrue(source.contains("ref: ${{ steps.public-pins.outputs.power_ref }}"));
            assertTrue(source.contains("branches: [master-1.21.11"));
            assertTrue(source.contains("-Pforge61MavenizerJar=$MAVENIZER"));
            assertTrue(source.contains("-PpowerAdvantageDeobfJar=$POWER_PLAYER"));
            assertFalse(source.contains("forge47"));
            if (workflow.equals("ci.yml")) {
                assertTrue(source.contains("-PforgeInstallerJar=$FORGE_INSTALLER"));
            }
        }
        String script = Files.readString(Paths.get(".github/verify-published-pins.sh"));
        assertTrue(script.contains("=~ ^[a-f0-9]{40}$") && script.contains("=~ ^[A-F0-9]{64}$"));
        for (String dependency : new String[] {"poweradvantage", "basemetals", "orespawn"}) {
            assertTrue(script.contains("$(value " + dependency + "_public_file_id)"));
        }
        assertTrue(script.contains("sha256sum -c -"));
        assertTrue(script.contains("$(value poweradvantage_pin_state)"));
        assertTrue(script.contains("!= 'MERGED_SOURCE'"));
        assertTrue(script.indexOf("!= 'MERGED_SOURCE'") < script.indexOf("require_merged()"));
        assertTrue(script.contains("_public_file_id\")\""));
    }
}
