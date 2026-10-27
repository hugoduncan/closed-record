import java.util.IdentityHashMap;

/** Checks whether identity-based graph accounting can count boxed value payloads. */
public final class ValueIdentityProbe {
    public static void main(String[] args) {
        Object payload = new CapabilityProbe.Point(17);
        Object[] references = new Object[] {payload, payload};
        var holder = new CapabilityProbe.Reference(payload);
        CapabilityProbe.require(((CapabilityProbe.Point) references[0]).x == 17
            && ((CapabilityProbe.Point) holder.point).x == 17, "Matched payloads");
        var seen = new IdentityHashMap<Object, Boolean>();
        seen.put(references, Boolean.TRUE);
        seen.put(holder, Boolean.TRUE);
        Object equalPayload = new CapabilityProbe.Point(17);
        seen.put(payload, Boolean.TRUE);
        seen.put(equalPayload, Boolean.TRUE);
        CapabilityProbe.require(seen.size() == 3,
            "Equal Point values collapse under identity-map semantics");
        Object changedPayload = new CapabilityProbe.Point(23);
        seen.put(changedPayload, Boolean.TRUE);
        CapabilityProbe.require(seen.size() == 4, "Different payload remains distinct");
        System.out.println("{:control :value-payload-identity :status :passed "
            + ":reference-containers :supported :equal-values-collapse true "
            + ":physical-box-deduplication :unverified :graph-metric :unavailable}");
    }
}
