package zone.moddev.mc.baseelectric.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;

/** Keeps released machine fields at the root of native value input/output. */
public final class LegacyNbt {
    public static final MapCodec<CompoundTag> ROOT = MapCodec.assumeMapUnsafe(CompoundTag.CODEC);

    private LegacyNbt() {}
}
