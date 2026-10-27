import java.lang.instrument.Instrumentation;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;

/** Array-only reachable-size control. Does not support record or value layouts. */
public final class ArrayGraphProbe {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation agent) {
        instrumentation = agent;
    }

    // Fail closed: arbitrary objects need a separately validated field walker.
    static long graphBytes(Object... roots) {
        var seen = new IdentityHashMap<Object, Boolean>();
        var pending = new ArrayDeque<Object>();
        for (Object root : roots) {
            if (root != null) pending.add(root);
        }
        long bytes = 0;
        while (!pending.isEmpty()) {
            Object object = pending.removeLast();
            if (seen.put(object, Boolean.TRUE) != null) continue;
            Class<?> type = object.getClass();
            if (!type.isArray() || (!type.getComponentType().isPrimitive()
                    && type != Object[].class)) {
                throw new IllegalArgumentException("Unsupported graph type: " + type);
            }
            bytes += instrumentation.getObjectSize(object);
            if (object instanceof Object[] array) {
                for (Object child : array) {
                    if (child != null) pending.add(child);
                }
            }
        }
        return bytes;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        require(instrumentation != null, "Launch with the size agent");
        byte[] shared = new byte[64];
        byte[] oldValue = new byte[64];
        byte[] replacement = new byte[64];
        Object[] source = {shared, oldValue, shared, null};
        Object[] result = {shared, replacement, shared, null};
        long payload = instrumentation.getObjectSize(shared);
        long container = instrumentation.getObjectSize(source);
        long sourceBytes = graphBytes((Object) source);
        long resultBytes = graphBytes((Object) result);
        long unionBytes = graphBytes(source, result);
        require(sourceBytes == container + 2 * payload, "Shared source counted twice");
        require(resultBytes == container + 2 * payload, "Result graph mismatch");
        require(unionBytes == 2 * container + 3 * payload, "Union must deduplicate identities");
        require(unionBytes - sourceBytes == container + payload, "Incremental accounting mismatch");
        require(graphBytes(source, source) == sourceBytes, "Duplicate roots counted twice");
        require(graphBytes(new Object[] {shared, oldValue, replacement}) == 3 * payload,
            "Distinct equal arrays must not be deduplicated by value");
        Object[] cycle = new Object[2];
        cycle[0] = cycle;
        cycle[1] = shared;
        require(graphBytes((Object) cycle) == instrumentation.getObjectSize(cycle) + payload,
            "Cycle must terminate and count each identity once");
        require(graphBytes((Object) null) == 0, "Null has no reachable bytes");
        boolean rejected = false;
        try {
            graphBytes(new Object());
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "Unsupported objects must not produce partial totals");
        System.out.printf(java.util.Locale.ROOT,
            "{:control :array-graph :status :passed :unit :bytes "
            + ":source %d :result %d :union %d :incremental %d "
            + ":container-shallow %d :payload-shallow %d "
            + ":identity-deduplication true :cycles true :unsupported-rejected true "
            + ":scope :ordinary-arrays-only :size-tool :jdk-instrumentation}%n",
            sourceBytes, resultBytes, unionBytes, unionBytes - sourceBytes, container, payload);
    }
}
