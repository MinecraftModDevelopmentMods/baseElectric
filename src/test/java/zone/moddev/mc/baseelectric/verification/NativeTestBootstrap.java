package zone.moddev.mc.baseelectric.verification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;

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

    public static synchronized void registerVanillaFluidTypes() throws ReflectiveOperationException {
        initialize();
        if (ForgeRegistries.FLUID_TYPES.get() != null) return;

        // Run the same narrow Forge registry phases as Power's worldless entity fixtures.
        NewRegistryEvent event = new NewRegistryEvent();
        Field types = ForgeRegistries.class.getDeclaredField("DEFERRED_FLUID_TYPES");
        types.setAccessible(true);
        Method create = DeferredRegister.class.getDeclaredMethod("createRegistry", NewRegistryEvent.class);
        create.setAccessible(true);
        create.invoke(types.get(null), event);
        Method fill = NewRegistryEvent.class.getDeclaredMethod("fill");
        fill.setAccessible(true);
        fill.invoke(event);

        Field vanilla = ForgeMod.class.getDeclaredField("VANILLA_FLUID_TYPES");
        vanilla.setAccessible(true);
        Method register = DeferredRegister.class.getDeclaredMethod("addEntries", RegisterEvent.class);
        register.setAccessible(true);
        Constructor<RegisterEvent> registerEvent = RegisterEvent.class.getDeclaredConstructor(
                ResourceKey.class, ForgeRegistry.class, Registry.class);
        registerEvent.setAccessible(true);
        register.invoke(vanilla.get(null), registerEvent.newInstance(
                ForgeRegistries.Keys.FLUID_TYPES, ForgeRegistries.FLUID_TYPES.get(), null));
    }
}
