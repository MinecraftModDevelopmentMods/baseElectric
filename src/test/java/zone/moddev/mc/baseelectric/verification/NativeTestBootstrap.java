package zone.moddev.mc.baseelectric.verification;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.registries.GameData;
import net.neoforged.fml.loading.LoadingModList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Opens native registries only in the worldless test process. */
public final class NativeTestBootstrap {
    private static boolean ready;

    private NativeTestBootstrap() {}

    public static synchronized void initialize() {
        if (ready) return;
        SharedConstants.tryDetectVersion();
        if (LoadingModList.get() == null) {
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        Bootstrap.bootStrap();
        GameData.unfreezeData();
        ready = true;
    }

    public static RegistryAccess.Frozen registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized void registerVanillaFluidTypes() throws ReflectiveOperationException {
        initialize();
        if (NeoForgeRegistries.FLUID_TYPES.containsKey(net.minecraft.resources.ResourceLocation.withDefaultNamespace("empty"))) return;
        ((MappedRegistry<?>) BuiltInRegistries.REGISTRY).unfreeze();
        Registry.register((Registry) BuiltInRegistries.REGISTRY,
                NeoForgeRegistries.Keys.FLUID_TYPES.location(), NeoForgeRegistries.FLUID_TYPES);
        var field = NeoForgeMod.class.getDeclaredField("VANILLA_FLUID_TYPES");
        field.setAccessible(true);
        var constructor = RegisterEvent.class.getDeclaredConstructor(ResourceKey.class, Registry.class);
        constructor.setAccessible(true);
        var event = constructor.newInstance(NeoForgeRegistries.Keys.FLUID_TYPES, NeoForgeRegistries.FLUID_TYPES);
        var addEntries = DeferredRegister.class.getDeclaredMethod("addEntries", RegisterEvent.class);
        addEntries.setAccessible(true);
        addEntries.invoke(field.get(null), event);
    }
}
