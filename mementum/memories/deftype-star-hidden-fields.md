💡 deftype* hidden fields: rules for a defrecord-like type

Observed on Clojure 1.11.4, 1.12.6 and 1.13.0-alpha9. V-13 (`test/closed_record/compiler_rules_test.clj`) fixes them.

- The compiler generates static `create(IPersistentMap)` and the alternative constructors (declared fields;
  declared fields + `__meta __extmap`) only when the field list ends with exactly
  `__meta __extmap ^int __hash ^int __hasheq`. `getBasis` is always generated.
- ❌ `__hash` and `__hasheq` need `^int`. With 3 of the 4 fields, or without `^int`, the form compiles but the
  class fails JVM verification at load (`VerifyError`). A test must load the class, not only `eval` the form.
- The `deftype` macro forbids these names. Use `deftype*`, as `defrecord` does.
- Map literal `#ns.T{...}` calls `create`. Positional literal `#ns.T[...]` calls the declared-fields constructor.
  Both require `*read-eval*` true.
- `create` puts extra keys in `__extmap`, and missing keys become nil. It calls no method of the type,
  so a read-time key check is not possible.
- `Compiler.emitValue` checks `IType` before `IRecord`. Do not add `IType`, or constants skip `create`.
  The `IRecord` path builds the constant from the `java.util.Map` view, so that view must show declared keys only.

Related: ramora/defclosedrecord/context.md (K-06 to K-08, E-02, E-03), embodiment.md (Generated type).
