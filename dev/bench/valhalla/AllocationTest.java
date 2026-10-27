import jdk.internal.misc.Unsafe;

/** Real-counter boundary checks on the pinned VM. No timing thresholds. */
public final class AllocationTest {
    private static void rejects(boolean array, String operation, int length, int count) {
        try {
            Allocation.sample(true, array, operation, length, count);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Invalid site accepted");
    }

    public static void main(String[] args) {
        rejects(true, "construct", 4, 0);
        rejects(true, "read", 4, -1);
        rejects(true, "construct", 0, 1);
        rejects(true, "store", -1, 1);
        rejects(true, "replace", 4, 1);
        rejects(false, "store", 0, 1);
        rejects(false, "construct", 4, 1);
        rejects(false, null, 0, 1);
        rejects(true, "unknown", 4, 1);
        Object initial = Operations.point(17);
        Unsafe unsafe = Unsafe.getUnsafe();
        int count = 1024;
        for (boolean flat : new boolean[] {true, false}) {
            Object[] control = flat ? Operations.valueArray(4, initial)
                                    : Operations.referenceArray(4, initial);
            int stride = unsafe.arrayInstanceIndexScale(control);
            for (int warmup = 0; warmup < 20; warmup++) {
                Allocation.sample(flat, true, "construct", 4, count);
                Allocation.sample(flat, true, "construct", 1024, count);
            }
            for (int repeat = 0; repeat < 5; repeat++) {
                long small = Allocation.sample(flat, true, "construct", 4, count);
                long large = Allocation.sample(flat, true, "construct", 1024, count);
                // Length delta is alignment-neutral; shared overhead is not subtracted.
                CapabilityProbe.require(large - small == 1020L * stride * count,
                    "Allocation length delta must match physical array stride");
                System.out.printf("{:flat? %s :operations %d :stride %d :small %d :large %d}%n",
                    flat, count, stride, small, large);
            }
        }
        System.out.println("{:invalid-sites 9 :layout-controls 10 :status :passed}");
    }
}
