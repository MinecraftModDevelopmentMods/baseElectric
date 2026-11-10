package zone.moddev.mc.baseelectric.verification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
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
        initializeEmptyModList();
        // The plain test JVM has no FML registration phase after vanilla bootstrap.
        GameData.unfreezeData();
        ready = true;
    }

    private static void initializeEmptyModList() {
        if (FMLLoader.getLoadingModList() != null) return;
        try {
            // Item components post native FML events even in a worldless JVM.
            Field discovered = FMLLoader.class.getDeclaredField("loadingModList");
            discovered.setAccessible(true);
            LoadingModList loading = LoadingModList.of(List.of(), List.of(), null);
            loading.setBrokenFiles(List.of());
            discovered.set(null, loading);
            Field layers = FMLLoader.class.getDeclaredField("moduleLayerManager");
            layers.setAccessible(true);
            layers.set(null, (IModuleLayerManager) ignored -> Optional.of(ModuleLayer.boot()));
            ModList mods = ModList.of(List.of(), List.of());
            Method load = ModList.class.getDeclaredMethod("setLoadedMods", List.class);
            load.setAccessible(true);
            load.invoke(mods, List.of());
            Field valid = ModLoader.class.getDeclaredField("loadingStateValid");
            valid.setAccessible(true);
            valid.setBoolean(ModLoader.get(), true);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot prepare the Forge 50 worldless test context", failure);
        }
    }

    public static RegistryAccess.Frozen registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    public static synchronized void registerVanillaFluidTypes() throws ReflectiveOperationException {
        initialize();
        if (ForgeRegistries.FLUID_TYPES.get() != null) return;

        // Run the same narrow Forge registry phases as Power's worldless entity fixtures.
        NewRegistryEvent event = new NewRegistryEvent();
        Field registries = ForgeRegistries.class.getDeclaredField("registries");
        registries.setAccessible(true);
        var deferred = ((List<?>) registries.get(null)).stream()
                .map(candidate -> (DeferredRegister<?>) candidate)
                .filter(candidate -> candidate.getRegistryKey().equals(ForgeRegistries.Keys.FLUID_TYPES))
                .findFirst().orElseThrow();
        // Dispatch the two native events directly; plain JUnit has no EventBus bytecode transform.
        Class<?> dispatcher = Class.forName(DeferredRegister.class.getName() + "$EventDispatcher");
        Constructor<?> listener = dispatcher.getDeclaredConstructor(DeferredRegister.class);
        listener.setAccessible(true);
        Method create = dispatcher.getDeclaredMethod("createRegistry", NewRegistryEvent.class);
        create.setAccessible(true);
        create.invoke(listener.newInstance(deferred), event);
        Method fill = NewRegistryEvent.class.getDeclaredMethod("fill");
        fill.setAccessible(true);
        fill.invoke(event);

        Constructor<RegisterEvent> registerEvent = RegisterEvent.class.getDeclaredConstructor(
                ResourceKey.class, ForgeRegistry.class, Registry.class);
        registerEvent.setAccessible(true);
        Field vanilla = ForgeMod.class.getDeclaredField("VANILLA_FLUID_TYPES");
        vanilla.setAccessible(true);
        Method handle = dispatcher.getDeclaredMethod("handleEvent", RegisterEvent.class);
        handle.setAccessible(true);
        handle.invoke(listener.newInstance(vanilla.get(null)), registerEvent.newInstance(
                ForgeRegistries.Keys.FLUID_TYPES, ForgeRegistries.FLUID_TYPES.get(), null));
    }
}
