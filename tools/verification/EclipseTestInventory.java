package zone.moddev.mc.baseelectric.verification;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/** Compares fresh compiled JUnit methods with exact Gradle XML inventory, without loading test classes. */
public final class EclipseTestInventory {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("classes, inventory file, expected test count");
        List<String> lines = Files.readAllLines(Path.of(args[1]), StandardCharsets.UTF_8);
        Set<String> declared = new TreeSet<>(lines);
        if (declared.isEmpty() || declared.contains("") || declared.size() != lines.size()) {
            throw new AssertionError("Empty or duplicate Gradle test inventory");
        }
        Set<String> compiled = new TreeSet<>();
        Set<String> suites = new TreeSet<>();
        try (var paths = Files.walk(Path.of(args[0]))) {
            for (Path file : paths.filter(path -> path.toString().endsWith(".class")).sorted().toList()) {
                ClassNode type = new ClassNode();
                new ClassReader(Files.readAllBytes(file)).accept(type,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                if (has(type.visibleAnnotations, "Lorg/junit/runner/RunWith;")
                        || "junit/framework/TestCase".equals(type.superName)) {
                    throw new AssertionError("Review custom JUnit discovery before qualification: " + type.name);
                }
                for (var method : type.methods) {
                    if (!has(method.visibleAnnotations, "Lorg/junit/Test;")) continue;
                    if (type.name.contains("$") || !"java/lang/Object".equals(type.superName)
                            || (type.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0
                            || (method.access & Opcodes.ACC_PUBLIC) == 0
                            || (method.access & Opcodes.ACC_STATIC) != 0 || !"()V".equals(method.desc)
                            || has(type.visibleAnnotations, "Lorg/junit/Ignore;")
                            || has(method.visibleAnnotations, "Lorg/junit/Ignore;")) {
                        throw new AssertionError("Review excluded, nested, inherited or nonstandard JUnit test: " + type.name + "#" + method.name);
                    }
                    String suite = type.name.replace('/', '.');
                    suites.add(suite);
                    if (!compiled.add(suite + "#" + method.name)) throw new AssertionError("Duplicate compiled test");
                }
            }
        }
        if (!compiled.equals(declared) || compiled.size() != Integer.parseInt(args[2])) {
            Set<String> omitted = new TreeSet<>(compiled);
            omitted.removeAll(declared);
            Set<String> stale = new TreeSet<>(declared);
            stale.removeAll(compiled);
            throw new AssertionError("Incomplete or stale Gradle method inventory: omitted=" + omitted
                    + ", stale=" + stale + ", compiled tests=" + compiled.size() + ", declared tests=" + args[2]);
        }
        System.out.println("PASS: exact compiled JUnit inventory; " + suites.size() + " suites, " + compiled.size() + " methods");
    }

    private static boolean has(List<AnnotationNode> annotations, String descriptor) {
        return annotations != null && annotations.stream().anyMatch(annotation -> descriptor.equals(annotation.desc));
    }
}
