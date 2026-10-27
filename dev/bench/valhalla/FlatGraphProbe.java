import java.lang.instrument.Instrumentation;
import java.util.IdentityHashMap;
import jdk.internal.misc.Unsafe;
import jdk.internal.value.ValueClass;

/** Bounded graph control for containers of the primitive-only Point value. */
public final class FlatGraphProbe {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation agent) {
        instrumentation = agent;
    }

    /** Identify the loaded artifact; refuse a classpath-only, inactive probe. */
    public static java.util.Map<String, String> identity() throws Exception {
        if (instrumentation == null) {
            throw new IllegalStateException("Flat graph agent is not active");
        }
        var location = FlatGraphProbe.class.getProtectionDomain().getCodeSource().getLocation();
        var path = java.nio.file.Path.of(location.toURI());
        if (!java.nio.file.Files.isRegularFile(path)) {
            throw new IllegalStateException("Flat graph agent must load from a JAR");
        }
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        String jarHash = java.util.HexFormat.of().formatHex(
            digest.digest(java.nio.file.Files.readAllBytes(path)));
        try (var stream = FlatGraphProbe.class.getResourceAsStream("/FlatGraphProbe.class")) {
            String classHash = java.util.HexFormat.of().formatHex(digest.digest(stream.readAllBytes()));
            return java.util.Map.of("jar-sha256", jarHash, "class-sha256", classHash,
                "location", location.toExternalForm());
        }
    }

    // Do not read flat elements or fields: doing so materializes boxes not in the graph.
    static long graphBytes(Object... roots) throws Exception {
        var seen = new IdentityHashMap<Object, Boolean>();
        long bytes = 0;
        for (Object root : roots) {
            if (root == null) continue;
            boolean array = root.getClass() == CapabilityProbe.Point[].class
                && ValueClass.isFlatArray(root);
            boolean holder = root.getClass() == CapabilityProbe.Restricted.class
                && Unsafe.getUnsafe().isFlatField(
                    CapabilityProbe.Restricted.class.getDeclaredField("point"));
            if (!array && !holder) {
                throw new IllegalArgumentException("Unsupported graph type: " + root.getClass());
            }
            if (seen.put(root, Boolean.TRUE) == null) {
                bytes += instrumentation.getObjectSize(root);
            }
        }
        return bytes;
    }

    public static void main(String[] args) throws Exception {
        CapabilityProbe.require(instrumentation != null, "Launch with size agent");
        Object initial = new CapabilityProbe.Point(17);
        Object replacement = new CapabilityProbe.Point(23);
        for (int length : new int[] {4, 1024}) {
            Object source = ValueClass.newNullRestrictedAtomicArray(
                CapabilityProbe.Point.class, length, initial);
            Object result = ValueClass.newNullRestrictedAtomicArray(
                CapabilityProbe.Point.class, length, replacement);
            var unsafe = Unsafe.getUnsafe();
            long layout = unsafe.arrayInstanceBaseOffset((Object[]) source)
                + (long) length * unsafe.arrayInstanceIndexScale((Object[]) source);
            CapabilityProbe.require(layout % 8 == 0, "Control requires aligned array layout");
            long sourceBytes = graphBytes(source);
            long resultBytes = graphBytes(result);
            long union = graphBytes(source, result);
            CapabilityProbe.require(sourceBytes == layout && resultBytes == layout,
                "Instrumentation must include inline elements exactly once");
            CapabilityProbe.require(union == 2 * layout && union - sourceBytes == layout,
                "Flat arrays must not share an invented payload box");
            CapabilityProbe.require(graphBytes(source, source, null) == layout,
                "Duplicate roots and nulls must not add bytes");
            System.out.printf(java.util.Locale.ROOT,
                "{:control :flat-array-graph :status :passed :unit :bytes :length %d "
                + ":source %d :result %d :union %d :incremental %d :unsafe-layout %d}%n",
                length, sourceBytes, resultBytes, union, union - sourceBytes, layout);
        }
        Object source = new CapabilityProbe.Restricted((CapabilityProbe.Point) initial);
        Object result = new CapabilityProbe.Restricted((CapabilityProbe.Point) replacement);
        long shallow = instrumentation.getObjectSize(source);
        long union = graphBytes(source, result);
        CapabilityProbe.require(graphBytes(source) == shallow && union == 2 * shallow
            && graphBytes(source, source) == shallow, "Flat holder accounting mismatch");
        boolean rejected = false;
        try {
            graphBytes(new CapabilityProbe.Reference(initial));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        CapabilityProbe.require(rejected, "Reference containers require traversal");
        System.out.printf(java.util.Locale.ROOT,
            "{:control :flat-field-graph :status :passed :unit :bytes :source %d "
            + ":result %d :union %d :incremental %d :reference-container-rejected true "
            + ":scope :primitive-only-point-containers}%n",
            shallow, shallow, union, union - shallow);
    }
}
