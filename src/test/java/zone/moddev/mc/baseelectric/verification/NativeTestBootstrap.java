package zone.moddev.mc.baseelectric.verification;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.registries.GameData;

/** Opens the native Forge registration phase for worldless test fixtures. */
public final class NativeTestBootstrap {
    private static boolean ready;

    private NativeTestBootstrap() {}

    public static synchronized void initialize() {
        if (ready) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // The plain test JVM has no FML registration phase after vanilla bootstrap.
        GameData.unfreezeData();
        ready = true;
    }
}
