import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/** Experimental allocation boundary. Setup, resets, and checks are outside counters. */
public final class Allocation {
    private static volatile Object sink;
    private static volatile int scalarSink;

    public static long sample(boolean flat, boolean array, String operation,
                              int length, int count) {
        if (count <= 0 || (array ? length <= 0 : length != 0) || operation == null
            || !(operation.equals("construct") || operation.equals("read")
                 || (array ? operation.equals("store") : operation.equals("replace")))) {
            throw new IllegalArgumentException("Invalid allocation site");
        }
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("Thread allocation counter unavailable");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        Object initial = Operations.point(17);
        Object replacement = Operations.point(23);
        Object[] results = new Object[count];
        int[] scalars = new int[count];
        Object[] sources = new Object[count];
        for (int i = 0; i < count; i++) {
            sources[i] = array
                ? (flat ? Operations.valueArray(length, initial)
                        : Operations.referenceArray(length, initial))
                : (flat ? Operations.restrictedHolder(initial)
                        : Operations.referenceHolder(initial));
        }
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < count; i++) {
            if (array) {
                Object[] source = (Object[]) sources[i];
                switch (operation) {
                    case "construct" -> results[i] = flat
                        ? Operations.valueArray(length, initial)
                        : Operations.referenceArray(length, initial);
                    case "read" -> scalars[i] = Operations.arrayRead(source, 0);
                    case "store" -> results[i] = Operations.arrayStore(source, 0, replacement);
                }
            } else {
                switch (operation) {
                    case "construct" -> results[i] = flat
                        ? Operations.restrictedHolder(initial) : Operations.referenceHolder(initial);
                    case "read" -> scalars[i] = flat
                        ? Operations.restrictedRead(sources[i]) : Operations.referenceRead(sources[i]);
                    case "replace" -> results[i] = flat
                        ? Operations.restrictedHolder(replacement) : Operations.referenceHolder(replacement);
                }
            }
        }
        long after = bean.getThreadAllocatedBytes(thread);
        sink = results;
        for (int i = 0; i < count; i++) {
            if (operation.equals("read")) {
                CapabilityProbe.require(scalars[i] == 17, "Scalar read result");
                scalarSink = scalars[i];
            } else if (array) {
                Object[] result = (Object[]) results[i];
                CapabilityProbe.require(result.length == length, "Array length");
                for (int j = 0; j < length; j++) {
                    CapabilityProbe.require(Operations.arrayRead(result, j)
                        == (operation.equals("store") && j == 0 ? 23 : 17), "Array contents");
                }
                CapabilityProbe.require(operation.equals("store")
                    ? result == sources[i] : result != sources[i], "Array ownership");
            } else {
                int expected = operation.equals("replace") ? 23 : 17;
                CapabilityProbe.require((flat ? Operations.restrictedRead(results[i])
                    : Operations.referenceRead(results[i])) == expected, "Holder result");
                CapabilityProbe.require(results[i] != sources[i], "Fresh holder");
                CapabilityProbe.require((flat ? Operations.restrictedRead(sources[i])
                    : Operations.referenceRead(sources[i])) == 17, "Source unchanged");
            }
        }
        CapabilityProbe.require(before >= 0 && after >= before, "Allocation counter");
        return after - before;
    }

    public static void main(String[] args) {
        int count = 1024;
        for (boolean array : new boolean[] {true, false}) {
            for (int length : array ? new int[] {4, 1024} : new int[] {0}) {
                for (String operation : array ? new String[] {"construct", "read", "store"}
                                             : new String[] {"construct", "read", "replace"}) {
                    for (boolean flat : new boolean[] {true, false}) {
                        for (int warmup = 0; warmup < 20; warmup++) {
                            sample(flat, array, operation, length, count);
                        }
                        for (int repeat = 0; repeat < 5; repeat++) {
                            long bytes = sample(flat, array, operation, length, count);
                            System.out.printf(java.util.Locale.ROOT,
                                "{:array? %s :flat? %s :operation :%s :length %d "
                                + ":sample %d :operations %d :allocated-bytes %d}%n",
                                array, flat, operation, length, repeat, count, bytes);
                        }
                    }
                }
            }
        }
    }
}
