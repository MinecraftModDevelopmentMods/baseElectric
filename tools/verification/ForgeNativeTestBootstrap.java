package zone.moddev.mc.baseelectric.verification;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/** Applies Forge's real event-bus plugin for native tier/harvest tests on a plain JVM. */
public final class ForgeNativeTestBootstrap {
    private ForgeNativeTestBootstrap() {}

    public static void premain(String options, Instrumentation instrumentation) {
        zone.moddev.mc.advantageworks.unit.ForgeCapabilityBootstrap.premain(options, instrumentation);
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type,
                    ProtectionDomain domain, byte[] bytes) {
                if (loader == null || name == null || !(name.equals("net/minecraftforge/fmllegacy/network/NetworkEvent")
                        || name.startsWith("net/minecraftforge/fmllegacy/network/NetworkEvent$"))) return null;
                try {
                    Class<?> readerType = Class.forName("org.objectweb.asm.ClassReader", true, loader);
                    Object reader = readerType.getConstructor(byte[].class).newInstance((Object) bytes);
                    Class<?> nodeType = Class.forName("org.objectweb.asm.tree.ClassNode", true, loader);
                    Object node = nodeType.getConstructor().newInstance();
                    Class<?> visitorType = Class.forName("org.objectweb.asm.ClassVisitor", true, loader);
                    readerType.getMethod("accept", visitorType, int.class).invoke(reader, node, 0);
                    Class<?> asmType = Class.forName("org.objectweb.asm.Type", true, loader);
                    Object classType = asmType.getMethod("getObjectType", String.class).invoke(null, name);
                    Class<?> phaseType = Class.forName("cpw.mods.modlauncher.serviceapi.ILaunchPluginService$Phase", true, loader);
                    Object phase = phaseType.getField("AFTER").get(null);
                    Class<?> pluginType = Class.forName("net.minecraftforge.eventbus.service.ModLauncherService", true, loader);
                    int flags = (Integer) pluginType.getMethod("processClassWithFlags", phaseType, nodeType, asmType, String.class)
                            .invoke(pluginType.getConstructor().newInstance(), phase, node, classType, "classloading");
                    if (flags == 0) throw new IllegalStateException("Native event-bus plugin did not transform " + name);
                    Class<?> writerType = Class.forName("org.objectweb.asm.ClassWriter", true, loader);
                    Object writer = writerType.getConstructor(int.class).newInstance(flags & 3);
                    nodeType.getMethod("accept", visitorType).invoke(node, writer);
                    return (byte[]) writerType.getMethod("toByteArray").invoke(writer);
                } catch (ReflectiveOperationException failure) {
                    throw new IllegalStateException("Cannot run Forge's native event-bus transform for " + name, failure);
                }
            }
        });
    }
}
