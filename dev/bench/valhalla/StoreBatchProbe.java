import java.util.Arrays;

/** Diagnostic changed-store batches; not an acceptance timing collector. */
public final class StoreBatchProbe {
    private static volatile Object sink;

    private static long sample(Object[][] arrays, Object initial, Object replacement) {
        for (Object[] array : arrays) Arrays.fill(array, initial);
        long before = System.nanoTime();
        for (Object[] array : arrays) Operations.arrayStore(array, 1, replacement);
        long elapsed = System.nanoTime() - before;
        sink = arrays;
        return elapsed;
    }

    private static void verify(Object[][] arrays, int length) {
        for (int i = 0; i < arrays.length; i++) {
            CapabilityProbe.require(arrays[i].length == length, "Wrong length");
            if (i > 0) CapabilityProbe.require(arrays[i] != arrays[i - 1],
                                               "Arrays must be independent");
            for (int j = 0; j < length; j++) {
                CapabilityProbe.require(Operations.arrayRead(arrays[i], j)
                    == (j == 1 ? 23 : 17), "Store or reset result differs");
            }
        }
    }

    public static void main(String[] args) {
        Object initial = Operations.point(17);
        Object replacement = Operations.point(23);
        for (int length : new int[] {4, 1024}) {
            for (boolean flat : new boolean[] {true, false}) {
                for (int count : new int[] {1, 64, 1024}) {
                    Object[][] arrays = new Object[count][];
                    for (int i = 0; i < count; i++) {
                        arrays[i] = flat ? Operations.valueArray(length, initial)
                                         : Operations.referenceArray(length, initial);
                    }
                    for (int i = 0; i < 2000; i++) sample(arrays, initial, replacement);
                    verify(arrays, length);
                    long[] durations = new long[1000];
                    int zeros = 0;
                    for (int i = 0; i < durations.length; i++) {
                        durations[i] = sample(arrays, initial, replacement);
                        CapabilityProbe.require(durations[i] >= 0, "Negative duration");
                        if (durations[i] == 0) zeros++;
                        verify(arrays, length);
                    }
                    Arrays.sort(durations);
                    System.out.printf(java.util.Locale.ROOT,
                        "{:layout :%s :length %d :stores %d :samples %d "
                        + ":zero-count %d :min-ns %d :median-ns %d :max-ns %d "
                        + ":verified true :acceptance false}%n",
                        flat ? "flat" : "reference", length, count, durations.length,
                        zeros, durations[0], durations[500], durations[999]);
                }
            }
        }
    }
}
