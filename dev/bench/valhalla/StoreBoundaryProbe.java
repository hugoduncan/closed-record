import jdk.internal.value.ValueClass;

/** Checks whether a timing invocation actually changes each array element. */
public final class StoreBoundaryProbe {
    public static Object[] array(boolean flat, int length) {
        Object initial = new CapabilityProbe.Point(17);
        if (flat) {
            return ValueClass.newNullRestrictedAtomicArray(
                CapabilityProbe.Point.class, length, initial);
        }
        Object[] result = ValueClass.newReferenceArray(CapabilityProbe.Point.class, length);
        java.util.Arrays.fill(result, initial);
        return result;
    }

    public static Object replacement() {
        return new CapabilityProbe.Point(23);
    }

    // Return the previous scalar to expose repeated same-value stores. This is
    // a diagnostic operation, not the store expression intended for acceptance.
    public static int store(Object[] array, Object replacement) {
        int previous = ((CapabilityProbe.Point) array[0]).x;
        array[0] = replacement;
        return previous;
    }

    public static int changedStore(Object[] array, Object replacement) {
        int previous = store(array, replacement);
        if (previous != 17) throw new IllegalStateException("Store did not change 17 to 23");
        return previous;
    }

    public static int read(Object[] array) {
        return ((CapabilityProbe.Point) array[0]).x;
    }
}
