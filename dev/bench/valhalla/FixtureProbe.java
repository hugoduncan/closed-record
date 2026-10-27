import java.util.Arrays;
import jdk.internal.value.ValueClass;

/** Checks matched inventory data without a timing harness. */
public class FixtureProbe {
    public static void arrays(int length, int initialX, int replacementX, int index) {
        var initial = new CapabilityProbe.Point(initialX);
        var replacement = new CapabilityProbe.Point(replacementX);
        Object[] flat = ValueClass.newNullRestrictedAtomicArray(
            CapabilityProbe.Point.class, length, initial);
        Object[] references = ValueClass.newReferenceArray(CapabilityProbe.Point.class, length);
        Arrays.fill(references, initial);
        CapabilityProbe.require(flat.length == length && references.length == length, "length");
        CapabilityProbe.require(ValueClass.isFlatArray(flat), "flat inventory array");
        CapabilityProbe.require(!ValueClass.isFlatArray(references), "non-flat inventory array");
        for (int i = 0; i < length; i++) {
            CapabilityProbe.require(((CapabilityProbe.Point) flat[i]).x == initialX
                && ((CapabilityProbe.Point) references[i]).x == initialX, "construction/read");
        }
        // Reset between operations, not in the operation intended for measurement.
        for (int repeat = 0; repeat < 2; repeat++) {
            flat[index] = replacement;
            references[index] = replacement;
            for (int i = 0; i < length; i++) {
                int expected = i == index ? replacementX : initialX;
                CapabilityProbe.require(((CapabilityProbe.Point) flat[i]).x == expected
                    && ((CapabilityProbe.Point) references[i]).x == expected, "store isolation");
            }
            flat[index] = initial;
            references[index] = initial;
            CapabilityProbe.require(((CapabilityProbe.Point) flat[index]).x == initialX
                && ((CapabilityProbe.Point) references[index]).x == initialX, "reset");
        }
    }

    public static void fields(int initialX, int replacementX) {
        var initial = new CapabilityProbe.Point(initialX);
        var replacement = new CapabilityProbe.Point(replacementX);
        var before = new CapabilityProbe.Restricted(initial);
        var referenceBefore = new CapabilityProbe.Reference(initial);
        var after = new CapabilityProbe.Restricted(replacement);
        var referenceAfter = new CapabilityProbe.Reference(replacement);
        CapabilityProbe.require(before != after && referenceBefore != referenceAfter,
            "replacement allocates distinct holders");
        CapabilityProbe.require(before.point.x == initialX
            && ((CapabilityProbe.Point) referenceBefore.point).x == initialX, "source unchanged/read");
        CapabilityProbe.require(after.point.x == replacementX
            && ((CapabilityProbe.Point) referenceAfter.point).x == replacementX, "replacement/read");
    }
}
