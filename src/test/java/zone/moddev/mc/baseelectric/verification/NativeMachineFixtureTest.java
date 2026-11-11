package zone.moddev.mc.baseelectric.verification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import net.minecraft.core.BlockPos;
import zone.moddev.mc.baseelectric.init.Entities;
import zone.moddev.mc.baseelectric.machines.ElectricDrillBlock;
import zone.moddev.mc.baseelectric.machines.ElectricDrillTileEntity;
import static org.junit.Assert.*;

public class NativeMachineFixtureTest {
    @Test public void fixtureRestoresAllGlobalTypesEvenWhenTheTestFails() throws Throwable {
        NativeTestBootstrap.initialize();
        Map<Field, Object> before = new LinkedHashMap<>();
        for (Field field : Entities.class.getFields()) before.put(field, field.get(null));
        AssertionError expected = new AssertionError("fixture failure probe");
        var fixture = new NativeMachineFixture();
        try {
            fixture.apply(new Statement() {
                @Override public void evaluate() {
                    assertNotNull(Entities.ELECTRIC_DRILL);
                    assertTrue(Entities.ELECTRIC_DRILL.isValid(NativeMachineFixture.state(ElectricDrillBlock.class)));
                    throw expected;
                }
            }, Description.EMPTY).evaluate();
            fail("The fixture swallowed its test failure");
        } catch (AssertionError failure) {
            assertSame(expected, failure);
        }
        for (var entry : before.entrySet()) assertSame(entry.getKey().getName(), entry.getValue(), entry.getKey().get(null));
        assertThrows(IllegalStateException.class, () -> NativeMachineFixture.state(ElectricDrillBlock.class));
    }

    @Test public void constructorStillRejectsAnInvalidNativeBlockState() throws Throwable {
        new NativeMachineFixture().apply(new Statement() {
            @Override public void evaluate() {
                var state = NativeMachineFixture.state(ElectricDrillBlock.class);
                assertSame(Entities.ELECTRIC_DRILL, new ElectricDrillTileEntity(BlockPos.ZERO, state).getType());
                assertFalse(Entities.ELECTRIC_DRILL.isValid(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
                assertThrows(IllegalStateException.class, () -> new ElectricDrillTileEntity(BlockPos.ZERO,
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
            }
        }, Description.EMPTY).evaluate();
    }
}
