/** Matched changed-store working set. Reset and creation are untimed setup. */
public final class StoreBatch {
    private final Object[][] arrays;
    private final Object initial;
    private final Object replacement;

    public StoreBatch(boolean flat, int length, int count) {
        if (length < 1 || count < 1) {
            throw new IllegalArgumentException("Positive length and count required");
        }
        initial = Operations.point(17);
        replacement = Operations.point(23);
        arrays = new Object[count][];
        for (int i = 0; i < count; i++) {
            arrays[i] = flat ? Operations.valueArray(length, initial)
                             : Operations.referenceArray(length, initial);
        }
    }

    public void reset() {
        // Only the changed slot is reset. Do not scan the untouched payload.
        for (Object[] array : arrays) Operations.arrayStore(array, 0, initial);
    }

    public Object[][] store() {
        for (Object[] array : arrays) Operations.arrayStore(array, 0, replacement);
        return arrays;
    }
}
