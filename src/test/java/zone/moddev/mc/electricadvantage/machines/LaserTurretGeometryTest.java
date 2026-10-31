package zone.moddev.mc.electricadvantage.machines;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class LaserTurretGeometryTest {
    @Test public void nativeRayGeometryKeepsHitsInAllSixDirections() {
        Vec3 origin = new Vec3(0.5D, 0.5D, 0.5D);
        for (Direction facing : Direction.values()) {
            Vec3 direction = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
            Vec3 center = origin.add(direction.scale(5.0D));
            AABB target = new AABB(center.x - 0.25D, center.y - 0.25D, center.z - 0.25D,
                    center.x + 0.25D, center.y + 0.25D, center.z + 0.25D);
            assertTrue(facing.toString(), LaserTurretTileEntity.rayIntersectsBoundingBox(origin, direction, target));
            assertFalse(facing.toString(), LaserTurretTileEntity.rayIntersectsBoundingBox(origin, direction.scale(-1.0D), target));
        }
    }

    @Test public void anOffsetTargetIsNotHitAndMissingBoundsAreIgnored() {
        Vec3 origin = new Vec3(0.5D, 0.5D, 0.5D);
        Vec3 direction = new Vec3(1.0D, 0.0D, 0.0D);
        assertFalse(LaserTurretTileEntity.rayIntersectsBoundingBox(origin, direction,
                new AABB(5.0D, 4.0D, 4.0D, 6.0D, 5.0D, 5.0D)));
        assertFalse(LaserTurretTileEntity.rayIntersectsBoundingBox(origin, direction, null));
    }
}
