package zone.moddev.mc.electricadvantage.resources;

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
        for (String helper : new String[] {"tools/verify-native-port.js", "tools/verification/Verify-ElectricEcj.ps1"}) {
            String source = new String(Files.readAllBytes(Paths.get(helper)), StandardCharsets.UTF_8);
            assertTrue(helper, source.contains("API_PIN_PROVISIONAL"));
            assertTrue(helper, source.contains("28247f936ac99e7a417da1bc1c89bc8394773a26"));
            assertTrue(helper, source.contains("7B2B2676CDB9A0B933DA04CC743787803C68E1466FD44E2EFBAEFB3ED47ABB3D"));
            assertFalse(helper, source.contains("2116a35a40dd150d79e7dfdcdc891637793178b7"));
            assertFalse(helper, source.contains("F58B93DAF03CF303F32B18CCCA140E55E886A2BA82732A3F286ADF3E31194B6A"));
        }
        for (String workflow : new String[] {"ci.yml", "codeql-analysis.yml"}) {
            String source = new String(Files.readAllBytes(Paths.get(".github/workflows", workflow)), StandardCharsets.UTF_8);
            int policy = source.indexOf("git config --global core.autocrlf false");
            assertTrue(policy > 0);
            assertTrue(policy < source.indexOf("repository: MinecraftModDevelopmentMods/OreSpawn"));
            assertTrue(policy < source.indexOf("repository: MinecraftModDevelopmentMods/BaseMetals"));
            assertTrue(policy < source.indexOf("repository: MinecraftModDevelopmentMods/PowerAdvantage"));
            assertTrue(source.contains("ref: b56e2b62a1a6ea4d1dd96f8646a87801a817dbdf"));
            assertTrue(source.contains("ref: dacfaa3af2e3310d5cb6b081da79f09260ae103d"));
            assertTrue(source.contains("BaseMetals-3.1.0.117011-deobf.jar"));
            assertTrue(source.contains("ref: ${{ steps.pins.outputs.power_ref }}"));
            assertTrue(source.contains("API_PIN_PROVISIONAL"));
            assertTrue(source.contains("28247f936ac99e7a417da1bc1c89bc8394773a26"));
            assertTrue(source.contains("7B2B2676CDB9A0B933DA04CC743787803C68E1466FD44E2EFBAEFB3ED47ABB3D"));
            assertFalse(source.contains("098d4625abc8a849bd1a11dcf697b3fee0cade7b"));
            assertFalse(source.contains("78E58DD2AB04E2B9F3267E5735C64AAE7E93EAC97571CB04485038862C853ED3"));
            assertFalse(source.contains("ce90c24c302d0ff3565db2db0c6c94a3b5f21f83"));
            assertFalse(source.contains("8BBB03DD7E4AF08A7390A161ED5AA94E292BEE56EF60E550B5512E967E0E9174"));
            assertTrue(source.indexOf("- name: Require final PowerAdvantage pin") < source.indexOf("- name: Check out pinned OreSpawn"));
            assertTrue(source.contains("branches: [master-1.17.1"));
            assertFalse(source.contains("master-1.14.4"));
        }
    }
}
