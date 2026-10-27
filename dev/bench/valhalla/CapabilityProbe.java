import jdk.internal.value.ValueClass;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.NullRestricted;

/** Executable assertions for the pinned experimental runtime, not a benchmark. */
public class CapabilityProbe {
    public static value class Point {
        public final int x;
        public Point(int x) { this.x = x; }
    }
    static class Restricted {
        @NullRestricted final Point point;
        Restricted(Point point) { this.point = point; super(); }
    }
    static class Nullable {
        final Point point;
        Nullable(Point point) { this.point = point; }
    }
    // Object prevents value-type flattening without changing the stored logical value.
    static class Reference {
        final Object point;
        Reference(Point point) { this.point = point; }
        Reference(Object point) { this.point = point; }
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Point initial = new Point(17);
        Point replacement = new Point(23);
        Object[] array = ValueClass.newNullRestrictedAtomicArray(Point.class, 4, initial);
        require(((Point) array[0]).x == 17, "array initialization");
        array[0] = replacement;
        require(((Point) array[0]).x == 23, "array replacement");
        boolean arrayRejectsNull = false;
        try { array[0] = null; } catch (NullPointerException expected) { arrayRejectsNull = true; }
        require(arrayRejectsNull, "array must reject null");
        Object[] references = ValueClass.newReferenceArray(Point.class, 4);
        references[0] = initial;
        require(((Point) references[0]).x == 17, "reference read");
        references[0] = null;
        require(!ValueClass.isFlatArray(references), "reference control must not be flat");
        Restricted before = new Restricted(initial);
        Restricted after = new Restricted(replacement);
        require(before.point.x == 17 && after.point.x == 23, "field replacement copies holder");
        boolean fieldRejectsNull = false;
        try { new Restricted(null); } catch (NullPointerException expected) { fieldRejectsNull = true; }
        require(fieldRejectsNull, "field must reject null");
        require(new Nullable(null).point == null, "nullable control");
        var restricted = Restricted.class.getDeclaredField("point");
        var nullable = Nullable.class.getDeclaredField("point");
        require(ValueClass.isNullRestrictedField(restricted), "restriction metadata");
        require(!ValueClass.isNullRestrictedField(nullable), "nullable metadata");
        require(ValueClass.isAtomicArray(array), "atomic array layout");
        require(ValueClass.isNullRestrictedArray(array), "array restriction metadata");
        require(ValueClass.isFlatArray(array), "selected value array must be flat");
        require(ValueClass.isAtomicArray(references), "reference array atomic accesses");
        require(!ValueClass.isNullRestrictedArray(references), "reference array nullable metadata");
        require(Unsafe.getUnsafe().isFlatField(restricted), "selected restricted field must be flat");
        Reference referenceBefore = new Reference(initial);
        Reference referenceAfter = new Reference(replacement);
        require(((Point) referenceBefore.point).x == 17
            && ((Point) referenceAfter.point).x == 23, "reference replacement copies holder");
        require(referenceBefore != referenceAfter, "reference holders are distinct");
        require(new Reference(null).point == null, "reference accepts null");
        var reference = Reference.class.getDeclaredField("point");
        require(!ValueClass.isNullRestrictedField(reference), "reference restriction metadata");
        require(!Unsafe.getUnsafe().isFlatField(reference), "reference field must not be flat");
        System.out.printf("{:status :measured :array {:null-restricted true :rejects-null true :atomic true :flat %s :read 17 :replacement 23} :reference-array {:flat false :accepts-null true :atomic true :null-restricted false} :field {:null-restricted true :rejects-null true :flat %s :read 17 :replacement 23} :nullable-field {:null-restricted false :accepts-null true :flat %s} :reference-field {:declared-type :object :null-restricted false :accepts-null true :flat false :read 17 :replacement 23 :distinct-holders true}}%n",
            ValueClass.isFlatArray(array), Unsafe.getUnsafe().isFlatField(restricted), Unsafe.getUnsafe().isFlatField(nullable));
    }
}
