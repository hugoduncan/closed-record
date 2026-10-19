# Invariants

Assertion records in the shape of the architecture metamodel.
`T` is a type defined by `defclosedrecord`. `D(T)` is the set of declared keys of `T`. `r` is an instance of `T`.

```text
id: I-01
status: intended
subject: r
predicate: keys equal
object: D(T)
scope: all instances made by ->Name, map->Name, assoc, cons, withMeta, or create without extra keys
basis: D-02, D-08
enforced-by: assoc, cons, map->Name (run-time check); ->Name (fixed arity)
confidence: high
```

```text
id: I-02
status: intended
subject: every operation on r except extraKeys
predicate: does not read
object: __extmap
scope: all methods of T
basis: D-09
enforced-by: code review; tests that read a literal with extra keys and check count, seq, =, hash, print
confidence: high
```

```text
id: I-03
status: intended
subject: (assoc r k v), k ∈ D(T)
predicate: has type
object: T
scope: all instances
basis: C-03
enforced-by: assoc emits (new T ...)
confidence: high
```

```text
id: I-04
status: intended
subject: (dissoc r k)
predicate: throws if k ∈ D(T), else returns
object: r
scope: all instances
basis: D-04, C-05
enforced-by: without
confidence: high
```

```text
id: I-05
status: intended
subject: (= r1 r2)
predicate: true iff
object: same class ∧ equal declared fields
scope: all instances; metadata and __extmap ignored
basis: D-07, I-02
enforced-by: equiv, equals
confidence: high
```

```text
id: I-06
status: intended
subject: (read-string (pr-str r))
predicate: equals
object: r
scope: *read-eval* true; fields with readable values
basis: D-09
enforced-by: print-method for IRecord; create
confidence: high
```

```text
id: I-07
status: intended
subject: __extmap
predicate: non-empty only if made by
object: create from a map literal with extra keys
scope: all instances
basis: K-07; Compiler.emitValue passes only the entries of r to create
enforced-by: no method writes new keys to __extmap
confidence: high
```

```text
id: I-08
status: intended (explicitly unenforced at read time)
subject: map literal #ns.T{...} with keys outside D(T)
predicate: is detected by
object: (check r)
scope: reader input with *read-eval* true
basis: D-09, K-07
enforced-by: check, called by the user; not automatic
confidence: high
```
