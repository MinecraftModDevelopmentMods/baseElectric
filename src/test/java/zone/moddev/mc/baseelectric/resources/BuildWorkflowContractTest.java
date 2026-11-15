package zone.moddev.mc.baseelectric.resources;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BuildWorkflowContractTest {
    @Test
    public void workflowsUseTheDeclaredPowerVersion() throws Exception {
        for (String name : new String[] {"ci.yml", "codeql-analysis.yml"}) {
            String workflow = new String(Files.readAllBytes(Paths.get(".github/workflows", name)),
                    StandardCharsets.UTF_8);
            assertTrue(name, workflow.contains("power_version=$(sed -n 's/^basepower_version=//p' gradle.properties)"));
            assertTrue(name, workflow.contains("basePower-$power_version-deobf.jar"));
            assertFalse(name, workflow.contains("basePower-2.4."));
        }
    }
}
