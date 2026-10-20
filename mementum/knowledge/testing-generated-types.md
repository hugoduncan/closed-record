---
title: Tests of eval'ed forms and compiler-generated types
status: active
category: testing
tags: [scry, eval, deftype*, macroexpand, class-loader, ns, clj-kondo, emitValue, print-dup, reader-literal, mutation, type-hints, differential, call-site, equality, hash, withMeta, extmap, iterator, test.check, property, keyword-invoke, map-views]
related: [mementum/knowledge/dev-tooling.md, test/closed_record/compiler_rules_test.clj, test/closed_record/defclosedrecord_test.clj, test/closed_record/literal_test.clj, test/closed_record/field_value_test.clj, test/closed_record/equality_test.clj, test/closed_record/hidden_key_test.clj, test/closed_record/property_test.clj, test/closed_record/differential_test.clj, test/closed_record/malli_test.clj, ramora/defclosedrecord/invariants.md]
depends-on: [mementum/knowledge/dev-tooling.md]
---

💡 Rules for tests that `eval` or `macroexpand-1` quoted forms, or that inspect generated classes.
Observed in task 002 on Clojure 1.11.4, 1.12.6 and 1.13.0-alpha9. The helpers are in
`test/closed_record/compiler_rules_test.clj`.

## The scry CLI is not the REPL

❌ A test that passes in the psi REPL can fail in the scry CLI. Check each new test-form choice in the CLI
(`clojure -M:test -m scry.cli`) on the three versions, not only in the REPL or with `-e`.

- `*ns*` is `user` in the CLI. An unqualified macro or var in a quoted form does not resolve
  ("Unable to resolve symbol"). Some forms pass only because `user` refers `clojure.core`.
  Fix: `(binding [*ns* (the-ns '<test-ns>)] (eval form))`. The emitter reads `*ns*` (class name, type hash,
  protocol resolution), so the binding also sets those values.
- `Compiler/LOADER` is not bound in the CLI. Then `(Class/forName name true (RT/baseLoader))` does not find an
  `eval`ed class (`ClassNotFoundException`). Fix: wrap the lookup in
  `(with-bindings {clojure.lang.Compiler/LOADER (clojure.lang.DynamicClassLoader.)} ...)`.
- `read-string` of a record literal inside the `*ns*` binding works without the loader binding.
- The CLI hides test output. In a reproduction, `spit` the results to a file.

## Class verification

- `eval` of a `deftype*` form defines the class but does not initialise it. Bad bytecode shows only at
  initialisation (`VerifyError`, no cause). Load with `Class/forName` and initialise `true` to test it.
- `RT/classForName` finds the class in memory without initialisation, so it gives no `VerifyError`.
- Read constructors with `.getConstructors` and `.getParameterTypes`. Do not depend on their order: it differs
  between Clojure versions.

## Values in forms

- ❌ `eval` of a form that contains an object embeds it as a constant. That runs `Compiler.emitValue` (K-08),
  which can fail or call other code. Read fields with `clojure.lang.Reflector/getInstanceField`, not
  `(eval '(.-a o))`. Test constants on purpose with `((eval (list 'fn [] v)))`.
- `^:unsynchronized-mutable` fields (`__hash`, `__hasheq`) are private. `.-__hash` throws. Do not read them.

## Record literals and code constants

Observed in task 003 (Clojure 1.11.4; suite passes on 1.12 and 1.13). Pattern: `test/closed_record/literal_test.clj`.

- ❌ A record literal in test source (for example in a `deftest` body) is a code constant. `emitValue` rebuilds it
  and drops `__extmap`. To test a value with extra keys, `read-string` the literal at run time (with `*ns*` bound).
  First assert that `check` throws `extra-keys`. Without this assertion, the test can pass when it tests nothing.
- To test a compiled constant: a macro reads a literal string at expansion time and returns the object. Use the
  macro in a function body. Do not `eval` in the macro: `eval` of a form that holds the object goes through
  `emitValue`, so `__extmap` is dropped before the macro returns. `macroexpand-1` of the macro gives the object
  before compilation.
- Only a constant that the compiler emits into a class goes through `emitValue`. The init of a top-level
  `(def d (m))` keeps the object (`DefExpr.eval`, `ConstantExpr.eval`). Other top-level forms compile into a
  function. See I-07.
- For `IRecord`, `emitValue` reads `entrySet`, not `iterator`. The printer iterates `seq`. Use this to choose
  mutations.
- With `*print-dup*` true, a record with metadata prints `^#=(...)` before `#ns.T[`. A prefix assertion needs a
  value without metadata.

## Field hints and differential tests

Observed in task 004 (Clojure 1.11.4; suite passes on 1.12 and 1.13). Pattern: `test/closed_record/field_value_test.clj`.

- 💡 The emitter has no cast code. The compiler emits the casts from the `deftype*` field hints. `new` (`->Name`,
  `assoc`) casts each argument to the field type (`^long` ← `2.7` gives `2`). `create` (`map->Name`, map literals)
  casts each value to the box class, then unboxes: a `Long` for `^int` throws `ClassCastException`. A nil or missing
  primitive throws `NullPointerException` on both paths (D-19). An object hint (`^String`) gives an `Object` field:
  no cast.
- ❌ A second bad field hides the case under test. In a `map->Name` input, a `Long` `:i` throws first. Give only the
  field under test a bad or missing value.
- 🔁 A mutation for a cast path changes how the emitter reaches the compiler: drop the hints in the `deftype*` field
  vector, or use `create` in place of `new` (or the reverse). Drop-hints fails a test only when the input needs a
  cast (`Long` for `^double`, `2.7` for `^long`, `(int 2)` read as `Long`). An input of the field type hides it.
- ❌ A direct `(. ~tname create ...)` in a method body fails at load ("No matching method create ...
  compile__stub"). In a mutation, use `clojure.lang.Reflector/invokeStaticMethod`. `Reflector/invokeConstructor`
  casts as `new` does (`9.9` gives `9`, nil gives NPE).
- No emitter code, so no mutation: `.-field` (a compiled field read), and a `getLookupThunk` that gives nil (the call
  site falls back to `valAt`, with the same result).
- Differential helper (closed type vs `defrecord`): give `[:ok v]` or `[:ex class]`. Pair each value with its class:
  `(= (int 5) 5)` is true, so `=` alone misses a wrong box class. Compare a record as `(into {} x)`: `=` of two types
  is false. Do not compare `hash` across types: `hasheq` includes the type hash (D-07). Compare exception classes,
  not messages.
- Assert with a macro that expands to `(is (= closed-outcome record-outcome))`. A boolean helper reports only
  `actual: false`. In the macro, bind the two outcomes to readable locals (`~'closed`, `~'record`), not `x#`:
  `expected:` then shows `(= closed record)`, not a gensym. Return a map keyed by operation
  (`{:= .. :hash= .. :equals ..}`), not a vector of booleans, so a failure names the operation that differs.
- A keyword call site caches the types that it sees. Give each call-site test its own site (a local `fn` in the
  `deftest`). A shared top-level site makes the site state depend on the test order.
- Run one namespace: `clojure -M:test -m scry.cli -n <ns>`. A scripted mutation run: replace a string in
  `src/closed_record/impl/jvm.clj`, run the namespace, read `.scry-results/`, revert with `git checkout`.

## Equality and hash tests

Observed in task 005 (Clojure 1.11.4; facts also hold on 1.13). Pattern: `test/closed_record/equality_test.clj`.

- 💡 For most field values, `=` (`Util.equiv`) and `.equals` (`Util.equals`) give the same result, so a test does
  not see a wrong comparison or a wrong map hash. Use field pairs that make them differ: `1`/`1N` and
  `(int 1)`/`1` (`=` true, `.equals` false), `0.0`/`-0.0` (also `.hashCode` differs), `1`/`1.0` (all false).
  `##NaN` gives no signal: `=` of a value with itself is true by identity.
- 💡 Call paths from collections to the emitter methods of a record `r` (plain map `m`):
  - `(= m r)` calls no method of `r`. `APersistentMap.equiv` returns false because `r` is not a `MapEquivalence`.
    Only a change in `base-interfaces` (add `MapEquivalence`) can change the result.
  - `(.equals m r)` and `HashMap.equals` call `size`, `containsKey` and `get` of `r`, not `equals`.
  - `(contains? #{m} r)` calls `hasheq` of `r`, then `(.equiv r m)`. `HashSet.contains` calls `.hashCode`
    and `.equals` of `r`.
  - Read the Clojure source before you say that a call is in emitter code. One guess in task 005 was wrong.
- ❌ `withMeta` copies the `__hash` and `__hasheq` caches. If a test calls `hash` before `with-meta`, the copied
  cache hides a change that puts `__meta` into a hash. Make all values first, then call `hash` or `.hashCode`.
- Do not compare `hash` of a closed value with `hash` of a `defrecord` value (type hash, D-07). Compare `hash`
  equality inside each type, then compare the two results.

## Hidden keys and call paths

Observed in task 006 (Clojure 1.11.4; suite passes on 1.12 and 1.13). Pattern: `test/closed_record/hidden_key_test.clj`.

- 💡 Call paths from Clojure functions to the emitter methods of a closed record `x`. Mutation runs confirmed each
  path. A closed record is `Iterable` and not `IReduceInit`:
  - `iterator` (`RecordIterator`): `reduce`, `(into {} x)`, `reduce-kv`, `iterator-seq`, `hash` (`hasheq`).
  - `seq`: `keys`, `vals`, `pr-str` (`print-map`), `.hashCode`, `.keySet`, `.values`, `.entrySet`, `.containsValue`.
  - `count`: `.size`. `valAt`: `get`, keyword call, `.get`. `containsKey`: `contains?`. `entryAt`: `find`.
  - `src` has no print code. A `seq` mutation is the print path.
- ❌ `=` of two `.iterator` results is false. Compare them through `iterator-seq`.
- 🔁 Put the guard (`check` throws `extra-keys`) in the shared assertion helper, so that the helper reads the
  value, asserts the guard, then compares each row. A guard in each `deftest` holds only by convention. Take the
  expected value from the same value without extra keys (`plain`), not from fixed values.
- ❌ A variadic helper with `(partition 2 label-fs)` drops a last label with no fn, with no signal. Take a vector of
  `[label f]` pairs. Use the label in `testing`, so a mutation failure names the operation.

## Property tests (test.check)

Observed in task 007 (test.check 1.1.3, Clojure 1.11.4; suite passes on 1.12 and 1.13). Pattern:
`test/closed_record/property_test.clj`.

- 🎯 The property body returns `true` or a failure `Result`. Do not use `is` in the body: each trial and each shrink
  step then adds assertions. The `deftest` has one `(is (:pass? result) msg)`. `msg` shows `:seed`,
  `[:shrunk :smallest]` and `[:shrunk :result-data]`. scry counts the test as passed, not "unknown".
- 💡 A failure that names its check: `(reify clojure.test.check.results/Result (pass? [_] false)
  (result-data [_] {:check id}))`. Its data is at `[:shrunk :result-data]`. A throw in the body gives only
  `{:clojure.test.check.properties/error e}`, with no check name. Thus each check catches any `Throwable` from its
  own operation and fails with its name and `:ex`. Do not catch around setup (factory call, model update).
- ❌ Also run the comparison (`=`, the `ok?` predicate) in the catch, not only the operation. `(seq x)` and
  `(.values x)` can give a lazy seq, and `=` realizes it. A fault that throws during realization then escapes the
  check with no name (task 008 implementation review).
- ❌ Shrinking keeps any failure, not the first one. `[:shrunk :result-data]` can name a different check than the
  first failed trial. Read the check name from the shrunk result.
- ❌ To find a failure among other return values, use `(instance? clojure.test.check.results.Result r)`. Do not use
  `satisfies?`: test.check extends `Result` to `Object` and `nil`, so it is true for all values.
- ❌ `gen/any` can give `##NaN`. `(= ##NaN ##NaN)` is false, so a model comparison fails with no fault. Use
  `gen/small-integer` with `nil`, or `gen/any-equatable`.
- 🔁 Run an operation once (`{:value v}` or `{:ex e}`), then give that outcome to each check. Do not let one check
  keep a result (for example in a `volatile!`) for a later check.
- Mutation run: in a new JVM, call `quick-check` on the property var (`@#'ns/var` for a private var) and print
  `[:shrunk :result-data]`. The property stops at the first failed check, so the change must show first in the check
  under test. Change the body of a view method (`size`, `keySet`, `values`, `entrySet`), not `seq` or `count`.

## Differential property (closed type vs `defrecord`)

Observed in task 008 (Clojure 1.11.4; suite passes on 1.12 and 1.13). Pattern:
`test/closed_record/differential_test.clj` (V-02). It uses the property model above, with a real `defrecord` in
place of a model map, and compares the two values after the start and after each generated step.

- 💡 Keyword lookup with no call site: `(k x nf)` with `k` a local calls `Keyword.invoke`, not `getLookupThunk`.
  A property can then use it in each trial, and no call-site state carries between trials.
- 💡 The `java.util.Map` views of the two types have the same classes (`.keySet` `PersistentHashSet`, `.values`
  `APersistentMap$ValSeq`). Compare them with `=`, with no conversion. `=` of a Clojure set and a `HashSet`, or of a
  seq and an `ArrayList`, is true in both orders.
- `(.containsValue x nil)` is false on both types, also when a field is nil (`defrecord` uses
  `(some #{v} (vals x))`). It is not a difference.
- `defrecord` `hash` is also type hash XOR `mapHasheq`. Compare `hash` only inside each type (D-07).

## Expansion-time errors

- `macroexpand-1` wraps a macro error in `Compiler$CompilerException`. Test the `ex-cause` class.
- Do not test `:clojure.error/phase`: it is `:macroexpansion` for an `Error` and `:macro-syntax-check` for an
  `Exception`.
- Capture exceptions with a `thrown` helper (`(try (f) nil (catch Throwable t t))`), then assert on the result.

## clj-kondo and test forms

- A literal `deftype*` form in a linted file gives one "Unresolved symbol" error for each field, method name and
  argument. Build the form as quoted data and give it to `eval`. clj-kondo does not lint runtime data.
- A namespace that uses a `:refer`red macro only in quoted forms gets two warnings (namespace and var not used).
  Use the macro unquoted at least once in that namespace.
- With `:lint-as` `defrecord`, a direct wrong-arity call of `->Name` is an error. Use `apply`.

## Test design

- 🔁 Mutation check: after a new test passes, break the code that it covers, see the test fail, then revert.
  Task 002 used this for each review follow-up test.
- 🔁 A mutation counts only if it breaks the path or property that the test asserts. A change that breaks `=` or
  `check` for all values does not count. A change that fails at namespace load (`VerifyError`, NPE) gives no
  signal for each test. Record, for each test, the change and the assertion that failed.
- 🔁 A mutation that changes more than one method counts as one change only if each part alone does not make the
  item fail. Run and record each part alone.
- ❌ A hash method body must have type `int`: wrap a `bit-xor` in `(int ...)`. A `long` body fails at `deftype*`
  compile (`ArrayIndexOutOfBoundsException`) and gives no signal.
- To run a mutation without risk to the work tree, `cp -R` the repository to `/tmp` and change the copy.
- ❌ Run each mutation in a new JVM. Do not reload the emitter and the test namespace in one JVM: `read-string` of a
  record literal resolves the class by name and gets the old class, so the results stay the same after the first
  mutation. Between runs, `git checkout src` in the copy (task 006).
- 🔁 When the docs give a fix for a failure case, test the fix too. Put the failure test and the fix test on one
  case table (`[label input-setup expected-failure expected-fix]`), so each failure case has a fix check and both
  checks use the same inputs. Make variant inputs from a base value (for example a closed schema from the open
  schema), not a second literal. Pattern: `throwing-cases` in `test/closed_record/malli_test.clj` (task 009).
- 🎯 Test source must not refer to task design sections ("Scope 3", "the first Context table"). The reader cannot
  find them after the task closes. Describe each case in the source. Keep durable ids (V-nn, I-nn, D-nn).
- ❌ `(is (.startsWith s p))` reports only `actual: false`: `clojure.test` shows the arguments only of a var call.
  For a change-signal assertion, use `=` (for example on a prefix from `subs`), so a failure shows the new output.
- A `defrecord` `Object/toString` that calls `str` on the record recurses (`StackOverflowError`). Use a protocol
  method to test that a method body can call a factory.
