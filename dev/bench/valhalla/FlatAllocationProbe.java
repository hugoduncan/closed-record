import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import jdk.internal.misc.Unsafe;
import jdk.internal.value.ValueClass;

/** Allocation controls for real flat arrays; not a timing or graph benchmark. */
public final class FlatAllocationProbe {
    private static volatile Object sink;

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static long sample(ThreadMXBean bean, Object[] roots, int length,
                               CapabilityProbe.Point initial) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < roots.length; i++) {
            roots[i] = ValueClass.newNullRestrictedAtomicArray(
                CapabilityProbe.Point.class, length, initial);
        }
        long after = bean.getThreadAllocatedBytes(thread);
        sink = roots;
        require(before >= 0 && after >= before, "Invalid allocation counter");
        return after - before;
    }

    // Keep the already boxed payload opaque across the native Object argument boundary.
    private static long boxedSample(ThreadMXBean bean, Object[] roots, int length,
                                    Object initial) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < roots.length; i++) {
            roots[i] = ValueClass.newNullRestrictedAtomicArray(
                CapabilityProbe.Point.class, length, initial);
        }
        long after = bean.getThreadAllocatedBytes(thread);
        sink = roots;
        require(before >= 0 && after >= before, "Invalid boxed allocation counter");
        return after - before;
    }

    public static void main(String[] args) {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        require(bean.isThreadAllocatedMemorySupported(), "Counter unsupported");
        bean.setThreadAllocatedMemoryEnabled(true);
        CapabilityProbe.Point initial = new CapabilityProbe.Point(17);
        Object[] roots = new Object[4096];
        for (int i = 0; i < 100; i++) {
            sample(bean, roots, 4, initial);
            sample(bean, roots, 1024, initial);
        }
        long small = sample(bean, roots, 4, initial);
        Object[] smallArray = (Object[]) roots[0];
        long large = sample(bean, roots, 1024, initial);
        Object[] largeArray = (Object[]) roots[0];
        require(ValueClass.isFlatArray(smallArray) && ValueClass.isFlatArray(largeArray),
                "Controls must use flat storage");
        Unsafe unsafe = Unsafe.getUnsafe();
        int scale = unsafe.arrayInstanceIndexScale(largeArray);
        long base = unsafe.arrayInstanceBaseOffset(largeArray);
        require(scale > 0 && base > 0, "Invalid array layout");
        require(small >= (base + 4L * scale) * roots.length, "Flat allocation missing");
        // The length difference times the stride is alignment-neutral on this pinned VM.
        require(large - small == 1020L * scale * roots.length,
                "Flat allocation delta differs from runtime layout");
        require(smallArray.length == 4 && largeArray.length == 1024, "Wrong lengths");
        for (Object root : roots) {
            Object[] array = (Object[]) root;
            require(((CapabilityProbe.Point) array[0]).x == 17
                    && ((CapabilityProbe.Point) array[1023]).x == 17,
                    "Escaping array payload differs");
        }
        Object boxed = initial;
        for (int i = 0; i < 100; i++) {
            boxedSample(bean, roots, 4, boxed);
            boxedSample(bean, roots, 1024, boxed);
        }
        long boxedSmall = boxedSample(bean, roots, 4, boxed);
        long boxedLarge = boxedSample(bean, roots, 1024, boxed);
        require(boxedSmall == (base + 4L * scale) * roots.length,
                "Preboxed small array allocation differs from layout");
        require(boxedLarge == (base + 1024L * scale) * roots.length,
                "Preboxed large array allocation differs from layout");
        require(small - boxedSmall == large - boxedLarge,
                "Argument adaptation cost depends on array length");
        for (Object root : roots) {
            Object[] array = (Object[]) root;
            require(ValueClass.isFlatArray(array)
                    && ((CapabilityProbe.Point) array[0]).x == 17
                    && ((CapabilityProbe.Point) array[1023]).x == 17,
                    "Preboxed control layout or payload differs");
        }
        System.out.printf(java.util.Locale.ROOT,
            "{:control :preboxed-native-argument :status :passed :unit :bytes "
            + ":operations %d :array-4-bytes %d :array-1024-bytes %d "
            + ":typed-argument-extra-bytes %d :overhead-subtracted false}%n",
            roots.length, boxedSmall, boxedLarge, small - boxedSmall);
        System.out.printf(java.util.Locale.ROOT,
            "{:status :passed :tool :hotspot-thread-mxbean :unit :bytes "
            + ":operations %d :flat true :array-base-bytes %d :element-stride-bytes %d "
            + ":array-4-bytes %d :array-1024-bytes %d :overhead-subtracted false "
            + ":graph-support :unvalidated}%n", roots.length, base, scale, small, large);
    }
}
