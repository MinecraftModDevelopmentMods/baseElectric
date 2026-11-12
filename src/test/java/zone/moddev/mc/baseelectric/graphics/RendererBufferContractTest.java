package zone.moddev.mc.baseelectric.graphics;

import zone.moddev.mc.baseelectric.verification.NativeMachineFixture;
import zone.moddev.mc.baseelectric.machines.ElectricDrillBlock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import zone.moddev.mc.baseelectric.init.Power;
import zone.moddev.mc.baseelectric.machines.ElectricDrillTileEntity;
import zone.moddev.mc.baseelectric.machines.LaserTurretTileEntity;
import cyano.poweradvantage.api.ConduitType;
import cyano.poweradvantage.api.simple.TileEntitySimplePowerMachine;

import static org.junit.Assert.*;

public class RendererBufferContractTest {
    @org.junit.Rule public final NativeMachineFixture machines =
            new NativeMachineFixture();
    private static final int FULL_BRIGHT = 15 << 20 | 15 << 4;

    @BeforeClass public static void bootstrapNativeRegistries() {
        NativeTestBootstrap.initialize();
    }

    @Test public void quadNormalsUseWindingAndTheNativeNormalMatrix() {
        PoseStack matrices = new PoseStack();
        matrices.translate(3, 4, 5);
        matrices.rotate(Axis.YP.rotationDegrees(90));
        RecordingBuffer buffer = new RecordingBuffer();
        BufferedQuads quads = new BufferedQuads(matrices, buffer, 0x123456, 0x789abc);
        quad(quads, false);
        quad(quads, true);
        assertEquals(8, buffer.vertices.size());
        Vertex first = buffer.vertices.get(0);
        assertArrayEquals(new float[] {3, 4, 5}, first.position, 0.00001F);
        assertArrayEquals(new float[] {1, 0, 0}, first.normal, 0.00001F);
        assertArrayEquals(new float[] {-1, 0, 0}, buffer.vertices.get(4).normal, 0.00001F);
        assertArrayEquals(new float[] {0.25F, 0.75F}, first.uv, 0F);
        for (Vertex vertex : buffer.vertices) {
            assertEquals(0x123456, vertex.light);
            assertEquals(0x789abc, vertex.overlay);
            assertArrayEquals(new int[] {255, 255, 255, 255}, vertex.color);
        }
        assertNormals(buffer);
    }

    @Test public void drillRetainsSixDirectionsSixteenVerticesAndLegacyUvs() throws Exception {
        ElectricDrillTileEntity drill = new ElectricDrillTileEntity(net.minecraft.core.BlockPos.ZERO, NativeMachineFixture.state(ElectricDrillBlock.class));
        set(ElectricDrillTileEntity.class, drill, "laserLength", 5);
        LaserDrillRenderer renderer = new LaserDrillRenderer(null);
        for (Direction direction : Direction.values()) {
            set(ElectricDrillTileEntity.class, drill, "facingCache", direction);
            PoseStack matrices = new PoseStack();
            matrices.translate(11, -5, 7);
            PoseStack.Pose before = matrices.last();
            Matrix4f matrix = new Matrix4f(before.pose());
            RecordingBuffer buffer = new RecordingBuffer();
            renderer.render(drill, 0.5F, matrices, buffer, 0, 17);
            assertSame(before, matrices.last());
            assertEquals(matrix, matrices.last().pose());
            assertTrue(matrices.isEmpty());
            assertEquals(direction.toString(), 16, buffer.vertices.size());
            assertArrayEquals(new float[] {11.5F, -4.5F, 7.5F},
                    midpoint(buffer.vertices.get(0), buffer.vertices.get(3)), 0.00001F);
            assertArrayEquals(new float[] {11.5F + direction.getStepX() * 5,
                    -4.5F + direction.getStepY() * 5, 7.5F + direction.getStepZ() * 5},
                    midpoint(buffer.vertices.get(1), buffer.vertices.get(2)), 0.00001F);
            for (Vertex vertex : buffer.vertices) {
                assertEquals(FULL_BRIGHT, vertex.light);
                assertEquals(17, vertex.overlay);
                assertArrayEquals(new float[] {0, 1}, vertex.uv, 0F);
            }
            assertNormals(buffer);
            LaserDrillRenderer.State state = renderer.createRenderState();
            state.length = drill.getDrillLength();
            state.facing = drill.getFacing();
            RecordingBuffer deferred = new RecordingBuffer();
            renderer.submit(state, matrices, deferred.collector(), null);
            assertSame(before, matrices.last());
            assertEquals(matrix, matrices.last().pose());
            assertTrue(matrices.isEmpty());
            assertTrue(deferred.vertices.isEmpty());
            assertEquals(1, deferred.pending.size());
            state.length = 99;
            state.facing = direction.getOpposite();
            deferred.flush();
            assertEquals(16, deferred.vertices.size());
            for (int index = 0; index < buffer.vertices.size(); index++) {
                Vertex expected = buffer.vertices.get(index), actual = deferred.vertices.get(index);
                assertArrayEquals(expected.position, actual.position, 0.00001F);
                assertArrayEquals(expected.normal, actual.normal, 0.00001F);
                assertArrayEquals(expected.uv, actual.uv, 0F);
                assertArrayEquals(expected.color, actual.color);
                assertEquals(FULL_BRIGHT, actual.light);
                assertEquals(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, actual.overlay);
            }
        }
    }

    @Test public void idleDrillDoesNotTouchTheStackOrEmitVertices() {
        PoseStack matrices = new PoseStack();
        PoseStack.Pose before = matrices.last();
        RecordingBuffer buffer = new RecordingBuffer();
        new LaserDrillRenderer(null).render(new ElectricDrillTileEntity(net.minecraft.core.BlockPos.ZERO, NativeMachineFixture.state(ElectricDrillBlock.class)), 0, matrices, buffer, 0, 0);
        assertSame(before, matrices.last());
        assertTrue(matrices.isEmpty());
        assertTrue(buffer.vertices.isEmpty());
    }

    @Test public void turretRetainsBarrelUvsBeamTimingFullBrightAndStackBalance() throws Exception {
        LaserTurretTileEntity turret = turret();
        LaserTurretRenderer renderer = new LaserTurretRenderer(null);
        turret.laserBlastLength = 5;
        turret.targetLocked = true;
        turret.rotOldYaw = turret.rotYaw = -90;
        for (int charge : new int[] {0, 1, 10, 11}) {
            set(LaserTurretTileEntity.class, turret, "laserAttack", charge);
            PoseStack matrices = new PoseStack();
            PoseStack.Pose before = matrices.last();
            Matrix4f matrix = new Matrix4f(before.pose());
            RecordingBuffer buffer = new RecordingBuffer();
            renderer.render(turret, 0.5F, matrices, buffer, 0x340012, 23);
            boolean beam = charge == 1 || charge == 10;
            assertEquals(beam ? 40 : 24, buffer.vertices.size());
            assertSame(before, matrices.last());
            assertEquals(matrix, before.pose());
            assertTrue(matrices.isEmpty());
            assertArrayEquals(new float[] {0.5F, 0.5F}, buffer.vertices.get(0).uv, 0F);
            assertArrayEquals(new float[] {1, 1}, buffer.vertices.get(2).uv, 0F);
            for (int i = 0; i < buffer.vertices.size(); i++) {
                assertEquals(i < 24 ? 0x340012 : FULL_BRIGHT, buffer.vertices.get(i).light);
                assertEquals(23, buffer.vertices.get(i).overlay);
            }
            if (beam) {
                assertArrayEquals(new float[] {0.5F, 0.75F, 0.5F},
                        midpoint(buffer.vertices.get(24), buffer.vertices.get(27)), 0.00001F);
                assertArrayEquals(new float[] {0.5F, 0.75F, -4.5F},
                        midpoint(buffer.vertices.get(25), buffer.vertices.get(26)), 0.00001F);
                assertArrayEquals(new float[] {0, 1}, buffer.vertices.get(24).uv, 0F);
                assertArrayEquals(new float[] {0.5F, 0.5F}, buffer.vertices.get(26).uv, 0F);
            }
            assertNormals(buffer);
        }
        turret.setEnergy(1500, Power.ELECTRIC_POWER);
        RecordingBuffer active = new RecordingBuffer();
        renderer.render(turret, 0, new PoseStack(), active, 0, 0);
        assertArrayEquals(new float[] {0.5F, 0}, active.vertices.get(0).uv, 0F);
        assertArrayEquals(new float[] {1, 0.5F}, active.vertices.get(2).uv, 0F);
    }

    private static void quad(BufferedQuads quads, boolean reverse) {
        quads.pos(0, 0, 0).tex(0.25F, 0.75F).endVertex();
        quads.pos(reverse ? 0 : 1, reverse ? 1 : 0, 0).tex(0.5F, 0.75F).endVertex();
        quads.pos(1, 1, 0).tex(0.5F, 1).endVertex();
        quads.pos(reverse ? 1 : 0, reverse ? 0 : 1, 0).tex(0.25F, 1).endVertex();
    }

    private static float[] midpoint(Vertex a, Vertex b) {
        return new float[] {(a.position[0] + b.position[0]) / 2,
                (a.position[1] + b.position[1]) / 2, (a.position[2] + b.position[2]) / 2};
    }

    private static void assertNormals(RecordingBuffer buffer) {
        for (Vertex vertex : buffer.vertices) {
            float[] n = vertex.normal;
            assertEquals(1F, n[0] * n[0] + n[1] * n[1] + n[2] * n[2], 0.00001F);
        }
    }

    // Keep the real turret controls; only omit the world-dependent constructor.
    private static LaserTurretTileEntity turret() throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field unsafeField = allocator.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        LaserTurretTileEntity turret = (LaserTurretTileEntity)
                allocator.getMethod("allocateInstance", Class.class).invoke(unsafeField.get(null), LaserTurretTileEntity.class);
        set(TileEntitySimplePowerMachine.class, turret, "types", new ConduitType[] {Power.ELECTRIC_POWER});
        set(TileEntitySimplePowerMachine.class, turret, "energyBuffers", new float[] {0});
        set(TileEntitySimplePowerMachine.class, turret, "energyBufferSizes", new float[] {2000});
        return turret;
    }

    private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class Vertex {
        float[] position;
        float[] uv;
        float[] normal;
        int[] color;
        int light;
        int overlay;
    }

    private static final class RecordingBuffer implements VertexConsumer {
        final List<Vertex> vertices = new ArrayList<>();
        final List<Runnable> pending = new ArrayList<>();
        Vertex vertex = new Vertex();
        SubmitNodeCollector collector() {
            return (SubmitNodeCollector) java.lang.reflect.Proxy.newProxyInstance(
                    SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class},
                    (proxy, method, arguments) -> {
                        if (!method.getName().equals("submitCustomGeometry")) {
                            throw new AssertionError("Unexpected native submission: " + method);
                        }
                        assertNotNull((RenderType) arguments[1]);
                        PoseStack.Pose pose = ((PoseStack) arguments[0]).last().copy();
                        var geometry = (SubmitNodeCollector.CustomGeometryRenderer) arguments[2];
                        pending.add(() -> geometry.render(pose, this));
                        return null;
                    });
        }
        void flush() {
            pending.forEach(Runnable::run);
            pending.clear();
        }
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            vertex = new Vertex();
            vertex.position = new float[] {x, y, z};
            vertices.add(vertex);
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { vertex.color = new int[] {r, g, b, a}; return this; }
        @Override public VertexConsumer setColor(int color) { return setColor(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24); }
        @Override public VertexConsumer setUv(float u, float v) { vertex.uv = new float[] {u, v}; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { vertex.overlay = u | v << 16; return this; }
        @Override public VertexConsumer setUv2(int u, int v) { vertex.light = u | v << 16; return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { vertex.normal = new float[] {x, y, z}; return this; }
        @Override public VertexConsumer setLineWidth(float width) { throw new AssertionError("Entity quads must not emit line width"); }
    }
}
