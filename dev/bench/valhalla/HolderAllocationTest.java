import java.lang.instrument.Instrumentation;

/** Checks the real allocation boundary against independently measured holder sizes. */
public final class HolderAllocationTest {
    private static Instrumentation instrumentation;

    public static void premain(String arguments, Instrumentation agent) {
        instrumentation = agent;
    }

    public static void main(String[] args) {
        CapabilityProbe.require(instrumentation != null, "Launch with size agent");
        Object payload = Operations.point(17);
        int count = 1024;
        for (boolean flat : new boolean[] {true, false}) {
            Object holder = flat ? Operations.restrictedHolder(payload)
                                 : Operations.referenceHolder(payload);
            long shallow = instrumentation.getObjectSize(holder);
            CapabilityProbe.require(shallow > 0, "Positive holder size");
            for (String operation : new String[] {"construct", "replace"}) {
                for (int warmup = 0; warmup < 20; warmup++) {
                    Allocation.sample(flat, false, operation, 0, count);
                }
                for (int repeat = 0; repeat < 5; repeat++) {
                    long bytes = Allocation.sample(flat, false, operation, 0, count);
                    // Payloads are prebuilt: only the escaping holder belongs in this count.
                    CapabilityProbe.require(bytes == shallow * count,
                        "Holder allocation must match independent shallow size");
                    System.out.printf(java.util.Locale.ROOT,
                        "{:flat? %s :operation :%s :sample %d :operations %d "
                        + ":allocated-bytes %d :shallow-bytes %d}%n",
                        flat, operation, repeat, count, bytes, shallow);
                }
            }
        }
        System.out.println("{:holder-controls 20 :status :passed}");
    }
}
