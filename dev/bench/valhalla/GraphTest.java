import jdk.internal.misc.Unsafe;

/** Real-agent checks of the collector boundary, independent of saved reporting. */
public final class GraphTest {
    public static void main(String[] args) throws Exception {
        int checks = 0;
        for (boolean array : new boolean[] {true, false}) {
            for (int length : array ? new int[] {4, 1024} : new int[] {0}) {
                Object root = array ? Operations.valueArray(length, Operations.point(17))
                    : Operations.restrictedHolder(Operations.point(17));
                long expected = array
                    ? Unsafe.getUnsafe().arrayInstanceBaseOffset((Object[]) root)
                        + (long) length * Unsafe.getUnsafe().arrayInstanceIndexScale((Object[]) root)
                    : FlatGraphProbe.graphBytes(root);
                for (String operation : array ? new String[] {"construct", "read", "store"}
                                             : new String[] {"construct", "read", "replace"}) {
                    var counts = Graph.sample(array, operation, length);
                    boolean alias = operation.equals("read") || operation.equals("store");
                    CapabilityProbe.require(counts.source() == expected && counts.result() == expected
                        && counts.union() == (alias ? expected : 2 * expected)
                        && counts.incremental() == (alias ? 0 : expected), "Graph root counts");
                    checks++;
                }
            }
        }
        for (Object[] invalid : new Object[][] {
                {true, null, 4}, {true, "replace", 4}, {true, "read", 0},
                {true, "construct", -1}, {false, "store", 0}, {false, "read", 4},
                {false, null, 0}, {false, "unknown", 0}}) {
            boolean rejected = false;
            try {
                Graph.sample((boolean) invalid[0], (String) invalid[1], (int) invalid[2]);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            CapabilityProbe.require(rejected, "Invalid graph site rejected");
            checks++;
        }
        System.out.printf("{:graph-boundary-checks %d :status :passed}%n", checks);
    }
}
