import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Set;

/** Bounded graph control for explicitly selected identity record classes. */
public final class RecordGraphProbe {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation agent) {
        instrumentation = agent;
    }

    /** Identify the loaded artifact; refuse a classpath-only, inactive probe. */
    public static java.util.Map<String, String> identity() throws Exception {
        if (instrumentation == null) {
            throw new IllegalStateException("Record graph agent is not active");
        }
        var location = RecordGraphProbe.class.getProtectionDomain().getCodeSource().getLocation();
        var path = java.nio.file.Path.of(location.toURI());
        if (!java.nio.file.Files.isRegularFile(path)) {
            throw new IllegalStateException("Record graph agent must load from a JAR");
        }
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        String jarHash = java.util.HexFormat.of().formatHex(
            digest.digest(java.nio.file.Files.readAllBytes(path)));
        try (var stream = RecordGraphProbe.class.getResourceAsStream("/RecordGraphProbe.class")) {
            String classHash = java.util.HexFormat.of().formatHex(digest.digest(stream.readAllBytes()));
            return java.util.Map.of("jar-sha256", jarHash, "class-sha256", classHash,
                "location", location.toExternalForm());
        }
    }

    public static long shallow(Object object) {
        return instrumentation.getObjectSize(object);
    }

    public static long graphBytes(Set<Class<?>> recordTypes, Object[] roots)
            throws IllegalAccessException {
        var seen = new IdentityHashMap<Object, Boolean>();
        var pending = new ArrayDeque<Object>();
        for (Object root : roots) if (root != null) pending.add(root);
        long bytes = 0;
        while (!pending.isEmpty()) {
            Object object = pending.removeLast();
            if (seen.put(object, Boolean.TRUE) != null) continue;
            Class<?> type = object.getClass();
            if (type == Long.class || (type.isArray() && type.getComponentType().isPrimitive())) {
                bytes += shallow(object);
            } else if (recordTypes.contains(type) && type.getSuperclass() == Object.class) {
                bytes += shallow(object);
                // Include metadata and extension maps, not only logical map entries.
                // Static keyword caches are class state, not instance graph edges.
                for (var field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                    field.setAccessible(true);
                    Object child = field.get(object);
                    if (child != null) pending.add(child);
                }
            } else {
                throw new IllegalArgumentException("Unsupported graph type: " + type);
            }
        }
        return bytes;
    }
}
