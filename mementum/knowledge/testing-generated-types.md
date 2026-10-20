---
title: Tests of eval'ed forms and compiler-generated types
status: active
category: testing
tags: [scry, eval, deftype*, macroexpand, class-loader, ns, clj-kondo, emitValue, print-dup, reader-literal, mutation]
related: [mementum/knowledge/dev-tooling.md, test/closed_record/compiler_rules_test.clj, test/closed_record/defclosedrecord_test.clj, test/closed_record/literal_test.clj, ramora/defclosedrecord/invariants.md]
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
- ❌ `(is (.startsWith s p))` reports only `actual: false`: `clojure.test` shows the arguments only of a var call.
  For a change-signal assertion, use `=` (for example on a prefix from `subs`), so a failure shows the new output.
- A `defrecord` `Object/toString` that calls `str` on the record recurses (`StackOverflowError`). Use a protocol
  method to test that a method body can call a factory.
