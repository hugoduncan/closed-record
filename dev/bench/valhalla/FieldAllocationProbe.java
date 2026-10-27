import com.sun.management.ThreadMXBean;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import jdk.internal.misc.Unsafe;

/** Real allocation and shallow-size controls, not retained-graph measurements. */
public final class FieldAllocationProbe {
    private static Instrumentation instrumentation;
    private static volatile Object sink;

    public static void premain(String arguments, Instrumentation agent) {
        instrumentation = agent;
    }

    private static long restrictedSample(ThreadMXBean bean, Object[] roots,
                                         CapabilityProbe.Point payload) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < roots.length; i++) {
            roots[i] = new CapabilityProbe.Restricted(payload);
        }
        long after = bean.getThreadAllocatedBytes(thread);
        sink = roots;
        CapabilityProbe.require(before >= 0 && after >= before, "Invalid counter");
        return after - before;
    }

    private static long referenceSample(ThreadMXBean bean, Object[] roots,
                                        CapabilityProbe.Point payload) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < roots.length; i++) {
            roots[i] = new CapabilityProbe.Reference(payload);
        }
        long after = bean.getThreadAllocatedBytes(thread);
        sink = roots;
        CapabilityProbe.require(before >= 0 && after >= before, "Invalid counter");
        return after - before;
    }

    private static long preboxedReferenceSample(ThreadMXBean bean, Object[] roots,
                                        Object payload) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < roots.length; i++) {
            roots[i] = new CapabilityProbe.Reference(payload);
        }
        long after = bean.getThreadAllocatedBytes(thread);
        sink = roots;
        CapabilityProbe.require(before >= 0 && after >= before, "Invalid counter");
        return after - before;
    }

    public static void main(String[] args) throws Exception {
        CapabilityProbe.require(instrumentation != null, "Launch with size agent");
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        CapabilityProbe.require(bean.isThreadAllocatedMemorySupported(), "Counter unsupported");
        bean.setThreadAllocatedMemoryEnabled(true);
        CapabilityProbe.Point payload = new CapabilityProbe.Point(17);
        Object[] restricted = new Object[4096];
        Object[] references = new Object[4096];
        Object boxedPayload = payload;
        Object[] preboxed = new Object[4096];
        for (int i = 0; i < 100; i++) {
            restrictedSample(bean, restricted, payload);
            referenceSample(bean, references, payload);
            preboxedReferenceSample(bean, preboxed, boxedPayload);
        }
        long flatBytes = restrictedSample(bean, restricted, payload);
        long referenceBytes = referenceSample(bean, references, payload);
        long preboxedBytes = preboxedReferenceSample(bean, preboxed, boxedPayload);
        long flatSize = instrumentation.getObjectSize(restricted[0]);
        long referenceSize = instrumentation.getObjectSize(references[0]);
        CapabilityProbe.require(Unsafe.getUnsafe().isFlatField(
            CapabilityProbe.Restricted.class.getDeclaredField("point")), "Expected flat field");
        CapabilityProbe.require(!Unsafe.getUnsafe().isFlatField(
            CapabilityProbe.Reference.class.getDeclaredField("point")), "Expected reference field");
        CapabilityProbe.require(flatSize > 0 && referenceSize > 0, "Invalid shallow size");
        CapabilityProbe.require(flatBytes == flatSize * restricted.length,
            "Flat holder allocation differs from shallow size");
        long payloadSize = instrumentation.getObjectSize(
            ((CapabilityProbe.Reference) references[0]).point);
        CapabilityProbe.require(referenceBytes == (referenceSize + payloadSize) * references.length,
            "Typed reference construction must account for holder and payload box");
        CapabilityProbe.require(instrumentation.getObjectSize(preboxed[0]) == referenceSize
            && preboxedBytes == referenceSize * preboxed.length,
            "Preboxed construction must allocate only the holder");
        for (int i = 0; i < restricted.length; i++) {
            CapabilityProbe.require(((CapabilityProbe.Point)
                ((CapabilityProbe.Reference) preboxed[i]).point).x == 17,
                "Preboxed payload differs");
            CapabilityProbe.require(((CapabilityProbe.Restricted) restricted[i]).point.x == 17
                && ((CapabilityProbe.Point) ((CapabilityProbe.Reference) references[i]).point).x == 17,
                "Escaping payload differs");
            if (i > 0) {
                CapabilityProbe.require(restricted[i] != restricted[i - 1]
                    && references[i] != references[i - 1]
                    && preboxed[i] != preboxed[i - 1], "Holders must be distinct");
            }
        }
        System.out.printf(java.util.Locale.ROOT,
            "{:control :field-allocation :status :passed :unit :bytes :operations %d "
            + ":flat-holder-bytes %d :reference-holder-bytes %d "
            + ":flat-shallow-bytes %d :reference-shallow-bytes %d "
            + ":preboxed-reference-bytes %d :preboxed-argument-outside-counter true "
            + ":reference-extra-bytes %d :size-tool :jdk-instrumentation :overhead-subtracted false "
            + ":graph-support :unvalidated}%n",
            restricted.length, flatBytes, referenceBytes, flatSize, referenceSize,
            preboxedBytes, referenceBytes - referenceSize * references.length);
    }
}
