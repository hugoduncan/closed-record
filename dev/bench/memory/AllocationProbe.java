import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/** Real counter controls, separate from timing and graph measurement. */
public final class AllocationProbe {
    private static volatile Object sink;
    private static volatile int scalar;

    private static long sample(ThreadMXBean bean, Object[] roots, int length) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < roots.length; i++) {
            if (length >= 0) {
                roots[i] = new byte[length];
            } else {
                scalar = i;
            }
        }
        long after = bean.getThreadAllocatedBytes(thread);
        sink = roots;
        if (before < 0 || after < before) {
            throw new AssertionError("Invalid allocation counter");
        }
        return after - before;
    }

    private static final class CrossThreadControl {
        volatile int stage;
        long workerBytes;
    }

    private static long crossThreadSample(ThreadMXBean bean, long expected)
            throws InterruptedException {
        Object[] roots = new Object[4096];
        CrossThreadControl control = new CrossThreadControl();
        Thread child = new Thread(() -> {
            control.stage = 1;
            while (control.stage != 2) Thread.onSpinWait();
            control.workerBytes = sample(bean, roots, 64);
            control.stage = 3;
        });
        child.start();
        while (control.stage != 1) Thread.onSpinWait();
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        control.stage = 2;
        while (control.stage != 3) Thread.onSpinWait();
        long after = bean.getThreadAllocatedBytes(thread);
        // Joining and assertions are outside the parent counter interval.
        child.join();
        require(control.workerBytes == expected, "Worker allocation missing during exclusion control");
        require(after >= before && before >= 0, "Invalid parent counter");
        require(after == before, "Worker allocation entered parent counter");
        require(((byte[]) roots[0]).length == 64, "Worker results not retained");
        return after - before;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        require(bean.isThreadAllocatedMemorySupported(), "Counter unsupported");
        bean.setThreadAllocatedMemoryEnabled(true);
        Object[] roots = new Object[4096];
        for (int i = 0; i < 100; i++) {
            sample(bean, roots, -1);
            sample(bean, roots, 64);
            sample(bean, roots, 128);
        }
        long empty = sample(bean, roots, -1);
        long small = sample(bean, roots, 64);
        long large = sample(bean, roots, 128);
        require(empty == 0, "Control boundary allocates");
        require(small >= 64L * roots.length, "Escaping allocations missing");
        require(large - small == 64L * roots.length, "Wrong operation divisor or size delta");
        require(((byte[]) roots[0]).length == 128, "Results not retained");

        // Unequal batches expose a fixed or incorrect normalisation divisor.
        Object[] shortRoots = new Object[257];
        Object[] noRoots = new Object[0];
        for (int i = 0; i < 100; i++) {
            sample(bean, shortRoots, 64);
            sample(bean, noRoots, 64);
        }
        long shortBytes = sample(bean, shortRoots, 64);
        long zeroBytes = sample(bean, noRoots, 64);
        require(shortBytes * roots.length == small * shortRoots.length,
            "Allocation does not scale with actual operation count");
        require(zeroBytes == 0, "Zero-operation boundary allocates");
        require(((byte[]) shortRoots[256]).length == 64, "Short batch results missing");

        // Worker setup and joining are outside its own measured interval.
        long[] worker = new long[1];
        Thread child = new Thread(() -> worker[0] = sample(bean, roots, 64));
        child.start();
        child.join();
        require(worker[0] == small, "Per-thread control differs");
        for (int i = 0; i < 20; i++) crossThreadSample(bean, small);
        long parentBytes = crossThreadSample(bean, small);
        System.out.printf(java.util.Locale.ROOT,
            "{:status :passed :tool :hotspot-thread-mxbean :unit :bytes "
            + ":operations %d :empty-boundary-bytes %d :array-64-bytes %d "
            + ":array-128-bytes %d :worker-array-64-bytes %d "
            + ":cross-thread-parent-bytes %d :cross-thread-worker-bytes %d "
            + ":short-batch {:operations %d :bytes %d} "
            + ":zero-batch {:operations 0 :bytes %d :normalisation :unavailable} "
            + ":overhead-subtracted false :graph-support :unvalidated}%n",
            roots.length, empty, small, large, worker[0], parentBytes, small,
            shortRoots.length, shortBytes, zeroBytes);
    }
}
