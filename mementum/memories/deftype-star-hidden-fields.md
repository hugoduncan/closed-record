💡 deftype* hidden fields: rules for a defrecord-like type

Observed on Clojure 1.11.4 tests and 1.13.0-alpha6 source (Compiler.java, LispReader.java).

- The compiler generates static `create(IPersistentMap)` and the n-argument
  constructor only when the field list ends with exactly
  `__meta __extmap __hash __hasheq`.
- With only 3 of these fields, the type does not compile.
- The `deftype` macro forbids these names. Use the `deftype*` special form, as `defrecord` does.
- Map literal `#ns.T{...}` calls `create`. Positional literal `#ns.T[...]` calls the n-argument constructor.
- `create` puts extra keys in `__extmap`, and missing keys become nil. It calls no user code,
  so a read-time key check is not possible.
- `Compiler.emitValue` rebuilds record constants in code with `create`. Without `create`, code that
  embeds a record value does not compile.
- Record literals require `*read-eval*` true.

Related: ramora/defclosedrecord/context.md (K-06, K-07), decisions.md (D-01, D-09).
