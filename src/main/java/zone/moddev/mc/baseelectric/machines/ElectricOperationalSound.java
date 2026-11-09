package zone.moddev.mc.baseelectric.machines;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

final class ElectricOperationalSound {
    private ElectricOperationalSound() {}

    static void broadcast(Level level, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
        if (level.isClientSide) return;
        final double range = 16;
        AABB bounds = new AABB(x - range, y - range, z - range, x + range, y + range, z + range);
        ClientboundSoundPacket packet = null;
        // Keep the historical recipient cube without a chunk-backed entity query.
        for (Player candidate : level.players()) {
            if (!(candidate instanceof ServerPlayer player) || player.connection == null
                    || player.isSpectator() || !player.getBoundingBox().intersects(bounds)) continue;
            if (packet == null) {
                packet = new ClientboundSoundPacket(Holder.direct(sound), SoundSource.BLOCKS,
                        x, y, z, volume, pitch, ThreadLocalRandom.current().nextLong());
            }
            player.connection.send(packet);
        }
    }
}
