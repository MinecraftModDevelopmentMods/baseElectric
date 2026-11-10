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
            for (String version : new String[] {"25.0.3+9.0.LTS", "8.0.502+7", "17.0.1+12"}) {
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
            int pins = source.indexOf("bash .github/scripts/forge47-dependencies.sh pins");
            int dependencyBuild = source.indexOf("bash .github/scripts/forge47-dependencies.sh build");
            assertTrue(policy > 0);
            assertTrue(policy < published && published < pins);
            for (String repository : new String[] {"OreSpawn", "BaseMetals", "basePower"}) {
                assertTrue(pins < source.indexOf("repository: MinecraftModDevelopmentMods/" + repository));
            }
            assertTrue(source.contains("ref: ${{ steps.pins.outputs.orespawn_ref }}"));
            assertTrue(source.contains("ref: ${{ steps.pins.outputs.basemetals_ref }}"));
            assertTrue(source.contains("ref: ${{ steps.pins.outputs.power_ref }}"));
            assertTrue(dependencyBuild > source.indexOf("ref: ${{ steps.pins.outputs.power_ref }}"));
            assertTrue(source.indexOf("\"-Pforge47MavenizerJar=$MAVENIZER\"") > dependencyBuild);
            assertTrue(source.contains("MAVENIZER: ${{ steps.dependencies.outputs.mavenizer }}"));
            assertTrue(source.contains("branches: [master-1.20.1"));
            assertFalse(source.contains("1.17.1"));
            if (workflow.equals("ci.yml")) {
                assertTrue(source.contains("POWER_PLAYER: ${{ steps.public-pins.outputs.power_player }}"));
            }
        }
        String script = Files.readString(Paths.get(".github/scripts/forge47-dependencies.sh"));
        assertTrue(script.contains("$(value poweradvantage_pin_state)") && script.contains("'LOCAL_CHECKPOINT'"));
        assertTrue(script.contains("=~ ^[a-f0-9]{40}$") && script.contains("=~ ^[A-F0-9]{64}$"));
        for (String dependency : new String[] {"orespawn", "basemetals", "power"}) {
            assertTrue(script.contains("git -C \"$" + dependency + "\" rev-parse HEAD"));
        }
        assertTrue(script.contains("$orespawn/ci-fixtures/tools/minecraft-mavenizer-0.5.21-orespawn-compat.jar"));
        assertTrue(script.contains("check_hash \"$mavenizer\" '416DCA4797BC55826F0DDCF03FDC405F1990FC15C2133FA27A1F143BF7976177'"));
        int metalsBuild = script.indexOf("(cd \"$basemetals\" && ./gradlew");
        int powerBuild = script.indexOf("(cd \"$power\" && ./gradlew");
        assertTrue(metalsBuild > 0 && powerBuild > metalsBuild);
        String metals = script.substring(metalsBuild, powerBuild);
        assertFalse(metals.contains("-I ") || metals.contains("-Pforge47Mavenizer"));
        String power = script.substring(powerBuild);
        assertFalse(power.contains("-I ") || power.contains("MavenizerScript"));
        assertTrue(power.contains("\"-Pforge47MavenizerJar=$mavenizer\""));
        assertTrue(script.contains("git -C \"$basemetals\" diff --exit-code HEAD --"));
        assertTrue(script.contains("git -C \"$power\" diff --exit-code HEAD --"));
    }
}
