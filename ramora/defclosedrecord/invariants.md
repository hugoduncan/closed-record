# Invariants

Assertion records in the shape of the architecture metamodel. `enforced-by` names the enforcement points.
`verified-by` names the verification plan items in [qualities](qualities.md#verification-plan).
`T` is a type defined by `defclosedrecord`. `D(T)` is the set of declared keys of `T`. `r` is an instance of `T`.

```text
id: I-01
subject: observable keys of r (seq, count, contains?, java.util.Map views)
predicate: equal
object: D(T)
scope: all instances
basis: D-02, D-08, D-09
enforced-by: assoc, cons, map->Name (run-time check); without (I-04); ->Name (fixed arity);
             create (extra keys only into __extmap, I-02)
verified-by: V-01, V-03, V-14
confidence: high
```

```text
id: I-02
subject: every operation on r except check
predicate: result does not depend on
object: __extmap
scope: all methods of T. assoc and withMeta copy __extmap but do not expose it. cons (conj, merge) copies __extmap
  through assoc. without (dissoc) of a key outside D(T) returns the same instance.
basis: D-09
enforced-by: code review; only -extra-keys returns __extmap
verified-by: V-14
confidence: high
```

```text
id: I-03
subject: (assoc r k v), k ∈ D(T)
predicate: has type
object: T
scope: all instances
basis: C-03
enforced-by: assoc emits (new T ...)
verified-by: V-01, V-03
confidence: high
```

```text
id: I-04
subject: (dissoc r k)
predicate: throws if k ∈ D(T), else returns
object: r
scope: all instances
basis: D-04, C-05
enforced-by: without
verified-by: V-01, V-03
confidence: high
```

```text
id: I-05
subject: equality and hash of r
predicate: equal to
object: the defrecord result for the same declared entries
scope: all instances; metadata and __extmap have no effect
basis: D-07, I-02
enforced-by: equiv, equals, hasheq, hashCode
verified-by: V-02, V-12
confidence: high
```

```text
id: I-06
subject: (read-string (pr-str r))
predicate: equals
object: r
scope: *read-eval* true; fields with readable values; class T loaded
basis: D-09
enforced-by: print-method for IRecord; create
verified-by: V-04
confidence: high
```

```text
id: I-07
subject: __extmap
predicate: non-empty only if made by
object: create from a map literal with extra keys, or assoc and withMeta of such an instance (cons, so conj and
  merge, copies __extmap through assoc; without of a key outside D(T) returns the same instance)
scope: all instances. Print, code constants and positional reads drop __extmap; after that, check passes. A code
  constant is a constant that the compiler emits into a class (Compiler.emitValue). A constant that Clojure evaluates
  without a class keeps the object (observed: only the init of a top-level def, when the init is not compiled into a
  function).
basis: K-07; E-03 (emitValue uses the java.util.Map view, I-09); Clojure 1.11.4 evaluation behaviour;
  Compiler.eval path (DefExpr.eval evaluates the init, ConstantExpr.eval returns the object; other top-level forms
  compile into a function)
enforced-by: no method writes new keys to __extmap
verified-by: V-04, V-05, V-14
confidence: high
```

```text
id: I-08
subject: map literal #ns.T{...} with keys outside D(T)
predicate: is detected by
object: (check r)
scope: reader input with *read-eval* true
basis: D-09, K-07
enforced-by: none at read time (unenforced); check, when the user calls it
verified-by: V-04, V-15
confidence: high
```

```text
id: I-09
subject: java.util.Map views of r
predicate: contain only
object: the declared entries
scope: all instances
basis: D-06, E-03. Code constants are built from this view.
enforced-by: size, get, containsKey, keySet, values, entrySet
verified-by: V-05, V-14
confidence: high
```
