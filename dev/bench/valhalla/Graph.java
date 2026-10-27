/** Retained roots for supported primitive-only flat experimental containers. */
public final class Graph {
    public record Counts(long source, long result, long union) {
        public long incremental() { return union - source; }
    }

    public static Counts sample(boolean array, String operation, int length) throws Exception {
        if ((array ? length <= 0 : length != 0) || operation == null
            || !(operation.equals("construct") || operation.equals("read")
                 || (array ? operation.equals("store") : operation.equals("replace")))) {
            throw new IllegalArgumentException("Invalid graph site");
        }
        Object initial = Operations.point(17);
        Object replacement = Operations.point(23);
        Object source = array ? Operations.valueArray(length, initial)
            : Operations.restrictedHolder(initial);
        Object result;
        switch (operation) {
            case "construct" -> result = array ? Operations.valueArray(length, initial)
                : Operations.restrictedHolder(initial);
            case "read" -> {
                int value = array ? Operations.arrayRead((Object[]) source, 0)
                    : Operations.restrictedRead(source);
                CapabilityProbe.require(value == 17, "Read result");
                // Scalar reads retain the input container, not a materialized scalar box.
                result = source;
            }
            case "store" -> result = Operations.arrayStore((Object[]) source, 0, replacement);
            case "replace" -> result = Operations.restrictedHolder(replacement);
            default -> throw new IllegalArgumentException("Invalid graph operation");
        }
        // Object-typed roots prevent Java varargs expansion of array elements.
        return new Counts(FlatGraphProbe.graphBytes(source), FlatGraphProbe.graphBytes(result),
            FlatGraphProbe.graphBytes(source, result));
    }
}
