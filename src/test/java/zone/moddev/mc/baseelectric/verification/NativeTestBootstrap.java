package zone.moddev.mc.baseelectric.verification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
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
        bindNativeComponents();
        // The plain test JVM has no FML registration phase after vanilla bootstrap.
        GameData.unfreezeData();
        ready = true;
        try {
            registerHolderSetTypes();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not initialize native Forge holder-set types", failure);
        }
    }

    public static RegistryAccess.Frozen registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    public static void bindNativeComponents() {
        var lookup = net.minecraft.data.registries.VanillaRegistries.createWorldLookup();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(
                net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
    }

    public static net.minecraft.world.level.storage.ValueInput input(net.minecraft.nbt.CompoundTag data) {
        return net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
                registries(), data);
    }

    public static synchronized void registerVanillaFluidTypes() throws ReflectiveOperationException {
        initialize();
        registerNativeForgeRegistry(ForgeRegistries.FLUID_TYPES, "VANILLA_FLUID_TYPES");
    }

    public static synchronized void registerVanillaIngredientSerializers() throws ReflectiveOperationException {
        initialize();
        registerHolderSetTypes();
        registerNativeForgeRegistry(ForgeRegistries.INGREDIENT_SERIALIZERS, "INGREDIENT_SERIALIZERS");
    }

    public static synchronized void registerHolderSetTypes() throws ReflectiveOperationException {
        initialize();
        registerNativeForgeRegistry(ForgeRegistries.HOLDER_SET_TYPES, "HOLDER_SET_TYPES");
    }

    private static void registerNativeForgeRegistry(DeferredRegister.RegistryHolder<?> registry, String contentsField)
            throws ReflectiveOperationException {
        initialize();
        if (registry.get() != null) return;
        ResourceKey<?> key = registry.getKey();

        // Run the same narrow Forge registry phases as Power's worldless entity fixtures.
        NewRegistryEvent event = new NewRegistryEvent();
        Field registries = ForgeRegistries.class.getDeclaredField("registries");
        registries.setAccessible(true);
        var deferred = ((List<?>) registries.get(null)).stream()
                .map(candidate -> (DeferredRegister<?>) candidate)
                .filter(candidate -> candidate.getRegistryKey().equals(key))
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
        Field vanilla = ForgeMod.class.getDeclaredField(contentsField);
        vanilla.setAccessible(true);
        Method handle = dispatcher.getDeclaredMethod("handleEvent", RegisterEvent.class);
        handle.setAccessible(true);
        handle.invoke(listener.newInstance(vanilla.get(null)), registerEvent.newInstance(
                key, registry.get(), null));
    }
}
