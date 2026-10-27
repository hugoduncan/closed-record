import java.util.Arrays;
import jdk.internal.value.ValueClass;

/** Unchecked experimental operations. Fixture setup and reset belong to callers. */
public final class Operations {
    public static Object point(int x) {
        return new CapabilityProbe.Point(x);
    }

    public static Object[] valueArray(int length, Object initial) {
        return ValueClass.newNullRestrictedAtomicArray(
            CapabilityProbe.Point.class, length, initial);
    }

    public static Object[] referenceArray(int length, Object initial) {
        Object[] result = ValueClass.newReferenceArray(CapabilityProbe.Point.class, length);
        Arrays.fill(result, initial);
        return result;
    }

    public static int arrayRead(Object[] array, int index) {
        return ((CapabilityProbe.Point) array[index]).x;
    }

    public static Object arrayStore(Object[] array, int index, Object replacement) {
        array[index] = replacement;
        return array;
    }

    public static Object restrictedHolder(Object point) {
        return new CapabilityProbe.Restricted((CapabilityProbe.Point) point);
    }

    public static Object referenceHolder(Object point) {
        return new CapabilityProbe.Reference(point);
    }

    public static int restrictedRead(Object holder) {
        return ((CapabilityProbe.Restricted) holder).point.x;
    }

    public static int referenceRead(Object holder) {
        return ((CapabilityProbe.Point) ((CapabilityProbe.Reference) holder).point).x;
    }
}
