import java.util.Arrays;

/** Clock-resolution diagnostic for the collector's actual StoreBatch boundary. */
public final class StoreTraversalClock {
    private static volatile Object sink;

    private static long sample(StoreBatch batch) {
        batch.reset();
        long before = System.nanoTime();
        Object[][] result = batch.store();
        long elapsed = System.nanoTime() - before;
        sink = result;
        return elapsed;
    }

    public static void main(String[] args) {
        for (int length : new int[] {4, 1024}) {
            for (boolean flat : new boolean[] {true, false}) {
                StoreBatch batch = new StoreBatch(flat, length, 1024);
                for (int i = 0; i < 2000; i++) sample(batch);
                long[] durations = new long[1000];
                int zeros = 0;
                for (int i = 0; i < durations.length; i++) {
                    durations[i] = sample(batch);
                    CapabilityProbe.require(durations[i] >= 0, "Negative duration");
                    if (durations[i] == 0) zeros++;
                }
                // Result checks stay outside all clock intervals.
                Object[][] arrays = (Object[][]) sink;
                for (Object[] array : arrays) {
                    for (int index = 0; index < length; index++) {
                        CapabilityProbe.require(Operations.arrayRead(array, index)
                            == (index == 0 ? 23 : 17), "Unexpected traversal result");
                    }
                }
                Arrays.sort(durations);
                System.out.printf(java.util.Locale.ROOT,
                    "{:layout :%s :length %d :stores 1024 :warmup 2000 "
                    + ":samples 1000 :zero-count %d :min-ns %d :median-ns %d "
                    + ":max-ns %d :verified true :acceptance false}%n",
                    flat ? "flat" : "reference", length, zeros,
                    durations[0], durations[500], durations[999]);
            }
        }
    }
}
