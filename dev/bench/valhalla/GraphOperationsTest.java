import jdk.internal.misc.Unsafe;

/** Real operation/root controls for the primitive-only flat Point containers. */
public final class GraphOperationsTest {
    public static void main(String[] args) throws Exception {
        Object initial = Operations.point(17);
        Object replacement = Operations.point(23);
        int checks = 0;
        for (int length : new int[] {4, 1024}) {
            Object[] array = Operations.valueArray(length, initial);
            var unsafe = Unsafe.getUnsafe();
            long expected = unsafe.arrayInstanceBaseOffset(array)
                + (long) length * unsafe.arrayInstanceIndexScale(array);
            CapabilityProbe.require(expected % 8 == 0, "Aligned physical layout");
            for (String operation : new String[] {"construct", "read", "store"}) {
                if (operation.equals("read")) {
                    CapabilityProbe.require(Operations.arrayRead(array, 0) == 17,
                        "Read retains the input container, not a boxed scalar");
                }
                if (operation.equals("store")) {
                    CapabilityProbe.require(Operations.arrayStore(array, 0, replacement) == array,
                        "Store retains the same array");
                    CapabilityProbe.require(Operations.arrayRead(array, 0) == 23
                        && Operations.arrayRead(array, length - 1) == 17, "Changed isolated store");
                }
                long bytes = FlatGraphProbe.graphBytes((Object) array);
                CapabilityProbe.require(bytes == expected, "Flat array includes inline payload once");
                System.out.printf("{:operation :%s :length %d :root :array :bytes %d}%n",
                    operation, length, bytes);
                checks++;
            }
        }
        for (String operation : new String[] {"construct", "read", "replace"}) {
            Object source = Operations.restrictedHolder(initial);
            // A read retains its input; construction/replacement retains a fresh holder.
            Object result = operation.equals("read") ? source
                : Operations.restrictedHolder(operation.equals("replace") ? replacement : initial);
            CapabilityProbe.require(Operations.restrictedRead(source) == 17
                && Operations.restrictedRead(result) == (operation.equals("replace") ? 23 : 17),
                "Source-preserving holder operation");
            long sourceBytes = FlatGraphProbe.graphBytes(source);
            long resultBytes = FlatGraphProbe.graphBytes(result);
            long union = FlatGraphProbe.graphBytes(source, result);
            CapabilityProbe.require(sourceBytes > 0 && resultBytes == sourceBytes
                && union == (operation.equals("read") ? sourceBytes : 2 * sourceBytes),
                "Holder root identity and inline payload accounting");
            System.out.printf("{:operation :%s :source %d :result %d :union %d :incremental %d}%n",
                operation, sourceBytes, resultBytes, union, union - sourceBytes);
            checks++;
        }
        for (Object unsupported : new Object[] {
                Operations.referenceArray(4, initial), Operations.referenceHolder(initial)}) {
            boolean rejected = false;
            try {
                FlatGraphProbe.graphBytes(unsupported);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            CapabilityProbe.require(rejected, "Shallow reference size is not a total graph");
            checks++;
        }
        System.out.printf("{:operation-root-controls %d :status :passed}%n", checks);
    }
}
