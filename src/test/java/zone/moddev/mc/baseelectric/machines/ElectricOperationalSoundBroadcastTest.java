package zone.moddev.mc.baseelectric.machines;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import zone.moddev.mc.baseelectric.verification.NativeTestBootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/** Real broadcast methods with an explicit player list and forbidden chunk-backed entity queries. */
public class ElectricOperationalSoundBroadcastTest {
    @BeforeClass public static void bootstrap() { NativeTestBootstrap.initialize(); }

    @Test public void machineBroadcastUsesOnlyExistingIntersectingConnectedPlayers() throws Exception {
        QueryRejectingWorld world = fixture();
        ElectricOvenTileEntity machine = new ElectricOvenTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.ElectricOvenBlock().defaultBlockState()) {
            @Override public Level getLevel() { return world; }
        };
        machine.playSoundEffect(100, 80, -20, SoundEvents.ANVIL_LAND, 0.25f, 0.75f);
        assertRecipients(world);
    }

    @Test public void generatorBroadcastUsesOnlyExistingIntersectingConnectedPlayers() throws Exception {
        QueryRejectingWorld world = fixture();
        PhotovoltaicGeneratorTileEntity generator = new PhotovoltaicGeneratorTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.PhotovoltaicGeneratorBlock().defaultBlockState()) {
            @Override public Level getLevel() { return world; }
        };
        generator.playSoundEffect(100, 80, -20, SoundEvents.ANVIL_LAND, 0.25f, 0.75f);
        assertRecipients(world);
    }

    @Test public void emptyPlayerListNeverQueriesChunks() throws Exception {
        QueryRejectingWorld world = withoutConstructor(QueryRejectingWorld.class);
        world.existingPlayers = Collections.emptyList();
        ElectricOvenTileEntity machine = new ElectricOvenTileEntity(net.minecraft.core.BlockPos.ZERO, new zone.moddev.mc.baseelectric.machines.ElectricOvenBlock().defaultBlockState()) {
            @Override public Level getLevel() { return world; }
        };
        machine.playSoundEffect(50000, 82, 50000, SoundEvents.ANVIL_LAND, 1, 1);
        assertEquals(1, world.playerListReads);
    }

    private static QueryRejectingWorld fixture() throws Exception {
        QueryRejectingWorld world = withoutConstructor(QueryRejectingWorld.class);
        world.existingPlayers = new ArrayList<>();
        world.existingPlayers.add(player(new AABB(99.7, 80, -20.3, 100.3, 81.8, -19.7), false, true));
        // Player center can be outside 16 blocks while its box still intersects the original cube.
        world.existingPlayers.add(player(new AABB(115.9, 80, -20.3, 116.5, 81.8, -19.7), false, true));
        // Cube corner is outside a spherical range but was included by the original AABB query.
        world.existingPlayers.add(player(new AABB(115, 80, -5, 115.6, 81.8, -4.4), false, true));
        world.existingPlayers.add(player(new AABB(116, 80, -20.3, 116.6, 81.8, -19.7), false, true));
        world.existingPlayers.add(player(new AABB(99.7, 96, -20.3, 100.3, 97.8, -19.7), false, true));
        world.existingPlayers.add(player(new AABB(99.7, 62.2, -20.3, 100.3, 64, -19.7), false, true));
        world.existingPlayers.add(player(new AABB(99.7, 80, -20.3, 100.3, 81.8, -19.7), true, true));
        world.existingPlayers.add(player(new AABB(99.7, 80, -20.3, 100.3, 81.8, -19.7), false, false));
        return world;
    }

    private static StubPlayer player(AABB bounds, boolean spectator, boolean connected) throws Exception {
        StubPlayer player = withoutConstructor(StubPlayer.class);
        player.setBoundingBox(bounds);
        player.spectator = spectator;
        if (connected) player.connection = withoutConstructor(RecordingConnection.class);
        return player;
    }

    private static void assertRecipients(QueryRejectingWorld world) {
        assertEquals("Broadcast reads the existing player list once", 1, world.playerListReads);
        for (int i = 0; i < world.existingPlayers.size(); i++) {
            ServerPlayer player = world.existingPlayers.get(i);
            if (player.connection == null) continue;
            RecordingConnection connection = (RecordingConnection)player.connection;
            assertEquals("Player " + i, i < 3 ? 1 : 0, connection.sent);
            if (i < 3) {
                assertTrue(connection.last instanceof ClientboundSoundPacket);
                ClientboundSoundPacket packet = (ClientboundSoundPacket) connection.last;
                assertSame(SoundEvents.ANVIL_LAND, packet.getSound().value());
                assertEquals(100, packet.getX(), 0.0);
                assertEquals(80, packet.getY(), 0.0);
                assertEquals(-20, packet.getZ(), 0.0);
                assertEquals(0.25f, packet.getVolume(), 0.0f);
                assertEquals(0.75f, packet.getPitch(), 0.0f);
            }
        }
    }

    private static <T> T withoutConstructor(Class<T> type) throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field field = allocator.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(allocator.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static final class QueryRejectingWorld extends ServerLevel {
        List<ServerPlayer> existingPlayers;
        int playerListReads;

        private QueryRejectingWorld() {
            super(null, null, null, null, null, null, null, false, 0L, Collections.emptyList(), false);
        }

        @Override public List<ServerPlayer> players() {
            playerListReads++;
            return existingPlayers;
        }

        @Override public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type,
                AABB bounds, Predicate<? super T> predicate) {
            throw new AssertionError("Sound broadcast must not use chunk-backed entity queries");
        }
    }

    private static final class StubPlayer extends ServerPlayer {
        boolean spectator;
        private StubPlayer() { super(null, null, null); }
        @Override public boolean isSpectator() { return spectator; }
    }

    private static final class RecordingConnection extends ServerGamePacketListenerImpl {
        int sent;
        Packet<?> last;
        private RecordingConnection() { super(null, null, null); }
        @Override public void send(Packet<?> packet) { sent++; last = packet; }
    }
}
