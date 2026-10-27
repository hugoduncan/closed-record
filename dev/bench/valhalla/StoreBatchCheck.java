import java.util.IdentityHashMap;

/** State-based batch contract checks. No timing thresholds. */
public final class StoreBatchCheck {
    public static void main(String[] args) {
        int checks = 0;
        for (boolean flat : new boolean[] {false, true}) {
            for (int length : new int[] {4, 1024}) {
                for (int count : new int[] {1, 1024}) {
                    StoreBatch batch = new StoreBatch(flat, length, count);
                    Object[][] previous = null;
                    for (int repeat = 0; repeat < 3; repeat++) {
                        batch.reset();
                        if (previous != null) verify(previous, length, count, 17);
                        Object[][] result = batch.store();
                        verify(result, length, count, 23);
                        CapabilityProbe.require(previous == null || previous == result,
                                                "Store must reuse the working set");
                        previous = result;
                        checks++;
                    }
                }
            }
        }
        for (int[] invalid : new int[][] {{0, 1}, {1, 0}, {-1, 1}, {1, -1}}) {
            try {
                new StoreBatch(false, invalid[0], invalid[1]);
                throw new AssertionError("Invalid dimensions accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
        }
        System.out.println("{:checks " + checks + " :pass true}");
    }

    private static void verify(Object[][] arrays, int length, int count, int changed) {
        CapabilityProbe.require(arrays.length == count, "Wrong operation count");
        IdentityHashMap<Object[], Boolean> identities = new IdentityHashMap<>();
        for (Object[] array : arrays) {
            CapabilityProbe.require(identities.put(array, true) == null,
                                    "Each store needs a distinct array");
            CapabilityProbe.require(array.length == length, "Wrong array length");
            for (int index = 0; index < length; index++) {
                CapabilityProbe.require(Operations.arrayRead(array, index)
                    == (index == 0 ? changed : 17), "Wrong store/reset contents");
            }
        }
    }
}
