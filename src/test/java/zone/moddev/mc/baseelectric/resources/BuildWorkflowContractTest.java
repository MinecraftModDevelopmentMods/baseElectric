package zone.moddev.mc.baseelectric.resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

public class BuildWorkflowContractTest {
    @Test public void nativeScaffoldUsesOneJava21Toolchain() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("id 'net.neoforged.moddev' version '2.0.148'"));
        assertTrue(build.contains("JavaLanguageVersion.of(21)"));
        assertFalse(build.contains("net.minecraftforge.gradle"));
        assertFalse(build.contains("net.neoforged.gradle"));
        assertFalse(build.contains("mavenizer"));
        assertTrue(build.contains("tasks.named('sourcesJar', Jar)"));
        assertTrue(build.contains("filter(FixCrLfFilter, eol: FixCrLfFilter.CrLf.newInstance('lf'))"));
        String wrapper = Files.readString(Path.of("gradle/wrapper/gradle-wrapper.properties"));
        assertTrue(wrapper.contains("gradle-9.8.0-bin.zip"));
        assertTrue(wrapper.contains("bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c"));
    }

    @Test public void hostedBuildsRemainGatedOnCoordinatedDependencies() throws Exception {
        for (String workflow : new String[]{"ci.yml", "codeql-analysis.yml"}) {
            String source = Files.readString(Path.of(".github/workflows", workflow));
            assertTrue(source.contains("java-version: '21.0.7+6.0.LTS'"));
            assertTrue(source.contains("./.github/test-native-dependencies.ps1"));
            assertTrue(source.contains("./.github/verify-native-dependencies.ps1 -DownloadDirectory"));
            assertTrue(source.contains("-I .github/pinned-native-inputs.gradle"));
            assertTrue(source.contains("-PpowerAdvantageJar=$POWER_PLAYER"));
            assertTrue(source.contains("-PbaseMetalsJar=$BASE_METALS_JAR"));
            assertTrue(source.contains("-PmmdlibJar=$MMDLIB_JAR"));
            assertTrue(source.contains("-PoreSpawnJar=$ORESPAWN_JAR"));
            assertFalse(source.contains("verifyEclipseEcj"));
            assertFalse(source.contains("runGameTestServer"));
            assertFalse(source.contains("forge52Mavenizer"));
            assertFalse(source.contains("Install Java 8"));
        }
    }

    @Test public void candidateArchiveRetainsDispatcherPaths() throws Exception {
        String ci = Files.readString(Path.of(".github/workflows/ci.yml"));
        assertTrue(ci.contains("build/libs/baseElectric-${{ env.CHILD_VERSION }}.jar"));
        assertTrue(ci.contains("build/libs/baseElectric-${{ env.CHILD_VERSION }}-sources.jar"));
        assertTrue(ci.contains("build/libs/baseElectric-${{ env.CHILD_VERSION }}-javadoc.jar"));
        assertTrue(ci.contains("build/release/SHA256SUMS"));
        assertTrue(ci.contains("CHANGELOG.txt"));
        assertFalse(ci.contains("env.POWER_VERSION"));
        assertTrue(Files.readString(Path.of("build.gradle")).contains("buildDirectory.file('release/SHA256SUMS')"));
    }

    @Test public void wrapperAndTagValidationUseNativeReleaseBranch() throws Exception {
        String wrapper = Files.readString(Path.of(".github/workflows/validate-gradle-build.yml"));
        assertTrue(wrapper.contains("master-1.21.1-neo"));
        assertFalse(wrapper.contains("- master-1.21.1\n"));
        String tag = Files.readString(Path.of(".github/workflows/release-on-tag.yml"));
        assertTrue(tag.contains(".github/verify-native-dependencies.ps1"));
        assertTrue(tag.contains("neoforge:2)"));
        assertFalse(tag.contains("forge:1|"));
        assertTrue(tag.contains("actions/workflows/ci.yml/runs"));
        assertTrue(tag.contains("-f event=push"));
        assertTrue(tag.contains("compare/$GITHUB_SHA...$release_branch"));
        assertTrue(tag.contains("release_player_sha256"));
    }

    @Test public void publicPowerPinsAndFrozenPlayerAreReleaseBound() throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("gradle.properties"))) {
            properties.load(reader);
        }
        assertEquals("MERGED_SOURCE", properties.getProperty("poweradvantage_pin_state"));
        assertEquals("c5550ce133ce0f5d3fe3e5227f021b390cdc7953", properties.getProperty("poweradvantage_source_ref"));
        assertEquals(properties.getProperty("poweradvantage_version"), properties.getProperty("poweradvantage_source_tag"));
        assertEquals(properties.getProperty("poweradvantage_sha256"), properties.getProperty("poweradvantage_runtime_sha256"));
        assertEquals("61457EDF8B1C097AE17805DCC26CB7E0F4F661208B777A0B53C56BB383EEDA85",
                properties.getProperty("release_player_sha256"));
        assertTrue(Arrays.asList(properties.getProperty("cf_requirements").split(",")).contains("mmdlib"));
        assertTrue(Files.readString(Path.of("build.gradle")).contains("Player artifact differs from approved release_player_sha256"));
        assertTrue(Files.readString(Path.of(".github/verify-native-dependencies.ps1")).contains("Assert-NativeSourceTag $pin"));
    }
}
