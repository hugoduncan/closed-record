# 022 Closed shape for value-type records

## Intent

Let a `:value-type true` closed record flatten on the JVM. On value output,
the record must have only its declared fields: no `__meta` and no
`__extmap`.

```clojure
(defclosedrecord Cell [^long v] :value-type true)

(require '[value-type.array :as va])
(:flat? (va/array-info (va/into-array Cell [(->Cell 1)])))  ;=> true
```

In value mode, the record gives up metadata and hidden literal keys. The
closed key set does not change.

The design is in Ramora: `ramora/value-type-records.md` and
`ramora/value-type-records/`, decisions VT-D13 to VT-D15, invariants VT-I7
to VT-I9, checks VT-V11 and VT-V12, risks VT-R6 and VT-R7. This task brings
the implementation, the tests and the user documentation into agreement with
that design. Ramora is the authority. If this file and Ramora differ, Ramora
applies; record the difference in `implementation.md`.

## Problem

On value output, a closed record keeps two reference fields, `__meta` and
`__extmap`. With compressed references they take 64 bits. On Valhalla
`27-jep401ea3+1-1`, a null-restricted atomic array is flat only when the
instance fields fit in 64 bits (VT-K8). Thus no closed value record
flattens, also with one `long` field, but a `defvaluetype` with the same
field does. Outcome O-VT1 ("the JVM can flatten it") is not met.

The Clojure compiler needs the four hidden fields in the `deftype*` form to
generate `create` and the record constructors (K-06). Thus the fields cannot
leave the form. value-type already removes `__hash` and `__hasheq` from value
output; VT-D13 extends this to `__meta` and `__extmap`.

## Prerequisite

value-type must provide the closed record shape of VT-D13. The pinned
value-type (`edc3f79`) does not. value-type task 022
(`munera/open/022-closed-record-shape` in value-type) implements it, under
value-type deftype* D9 and D10. The contract that closed-record needs:

- the option `:value-type/closed true` in the `defvaluetype*` form, with the
  record field shape (`__meta __extmap ^int __hash ^int __hasheq`);
- value output removes all four hidden fields; the record constructors,
  `create` and `getBasis` keep their descriptors;
- identity output keeps all four; `__meta` and `__extmap` hold nil;
- in both outputs, the main constructor rejects a non-nil `__meta` or
  `__extmap` argument with `IllegalArgumentException`, before the non-nil
  field checks.

This work is not part of this task. Start this task only when a
value-type commit on GitHub `master` provides the contract. If the value-type
contract differs from VT-D13, change Ramora first (VT-R6), then this design.

## Context

### Facts

- VT-K7: the compiler-generated `create` passes `null` for `__meta` and
  `RT.seqOrElse(rest)` for `__extmap` (`rest`: the map without the declared
  keys). The constructor with the declared fields only passes nil for both.
  Thus the value-type constructor check rejects undeclared keys on all
  `create` paths, and accepts all other paths.
- Compiled record constants come from `create` with the `java.util.Map` view
  (E-03, I-09). That view has only declared keys, so constants pass the
  check.
- `Compiler.emitValue` applies constant metadata only to an `IObj`. A type
  without `IObj` gets none.

### Current implementation (`closed-record.impl.jvm`)

- The value mode emits `defvaluetype*` with the record field shape, and
  implements `clojure.lang.IObj`.
- `withMeta` and `assoc` call the main constructor with `__meta` and
  `__extmap`.
- `-extra-keys` returns `__extmap`.

### Tests that use metadata or hidden literal keys

The value-type suite runs the shared checks of `test/runtime/closed_record/checks/`
through `define-checks`. These shared checks use `with-meta`, `meta` or
literals with undeclared keys:

- `defclosedrecord.clj`: metadata, and `with-meta` keeps the extra keys.
- `differential.clj`: the `:with-meta` step and the `:meta` comparison.
- `equality.clj`: equality with metadata.
- `hidden_key.clj`: V-14.
- `literal.clj`: the extra-key parts of V-04.

VT-V3 excludes these parts in value mode. VT-V11 replaces them.

## Scope

### Emitter (value mode only)

- Emit the option `:value-type/closed true` (VT-D5, VT-D13).
- Do not implement `IMeta` or `IObj`; no `meta` or `withMeta` (VT-D14,
  VT-I9).
- `assoc` calls the constructor with the declared fields only (VT-D7).
- No generated reference to `__meta`, `__extmap`, `__hash` or `__hasheq`
  (VT-I4). `-extra-keys` returns nil.
- No closed-record check of constructor arguments (VT-D13). No wrapping of
  the `IllegalArgumentException` (VT-D15).
- The ordinary expansion does not change (VT-I2). The expansion fixture does
  not change.

### Dependencies

- Pin the `:value-type-test` alias to the value-type commit that satisfies
  the Prerequisite: explicit HTTPS `:git/url` and full SHA.
- Use the same SHA in the README, `doc/value-type-records.md` and other
  links to the value-type field contract.
- Keep the evidence link in `doc/performance.md` (value-type `8bb7c4f`).

### Verification

As Ramora states:

- VT-V2: the option, no hidden-field references, no `IMeta` or `IObj`.
- VT-V3 and VT-V4: the shared checks without the metadata and hidden-key
  parts. VT-V4 checks that the instance fields are the declared fields.
- VT-V9: no `with-meta` part.
- VT-V11 (new): the closed shape, on both representations.
- VT-V12 (new): `:flat? true` for arrays of `[^long x]` and `[^int x ^int y]`
  value records, in the Valhalla job only.
- The ordinary suites do not change and pass.

### CI

No new job. VT-V11 runs in both value-type jobs. VT-V12 runs in the
Valhalla job. The Valhalla job already has `--enable-preview`. VT-V12 also
needs `--add-exports=java.base/jdk.internal.value=ALL-UNNAMED` (the
`:enable-preview` alias has it). Add the export to the Valhalla test command
in `.github/workflows/ci.yml` and in `doc/development.md`.

### Documentation

- README "Value-type records":
  - Approach: no metadata in value mode.
  - Differences: no metadata; a map literal with an undeclared key throws at
    read.
  - "Arrays and flattening": replace the statement that closed value records
    are not flat with the 64-bit limit of VT-K8 and an example that is flat.
    State that flattening is a JVM choice.
- `doc/value-type-records.md`: the behaviour table rows of the Ramora public
  contract (`__meta`, `__extmap`, metadata, map literal), and the flattening
  section.
- `defclosedrecord` docstring: one sentence on metadata and on map literals
  in value mode. Regenerate `doc/API.md`.
- `CHANGELOG.md` `## [Unreleased]`: in value mode, no metadata, map literals
  with undeclared keys throw, and records with at most 64 bits of fields can
  flatten.
- Ramora `value-type-records/embodiment.md`: remove "Differences from the
  design" when they no longer apply; update the elements, the generated type
  and the verification limits.
- `mementum/state.md`: current state.

## Invariants and ownership

- value-type owns: the closed shape, removal of fields, the constructor
  check and its exception, flattening.
- closed-record owns: the closed key set and its errors, the interface list
  (no `IMeta`, `IObj`), the ordinary expansion.
- D-11 stays true: closed-record emits no value validation.
- Ordinary closed records keep metadata and hidden literal keys (C-08, C-09,
  D-09). Only value mode changes.

## Out of scope

- The value-type implementation of the closed shape (Prerequisite).
- Loosely consistent classes or non-atomic arrays (VT-D13, rejected).
- Changes to ordinary mode.
- A closed-record `ex-info` for undeclared literal keys in value mode.
- Field flattening of nested records, object size and timing measurements.
- A value-type release or a closed-record release.

## Acceptance criteria

1. `deps.edn` and the docs pin a value-type commit that satisfies the
   Prerequisite.
2. VT-V2, VT-V3, VT-V4, VT-V9 and VT-V11 pass in the Temurin 21 and the
   Valhalla value-type CI jobs. VT-V12 passes in the Valhalla job.
3. The ordinary suites pass on Clojure 1.11, 1.12 and 1.13, and the expansion
   fixture is unchanged.
4. On Valhalla `27-jep401ea3+1-1`, `(defclosedrecord Cell [^long v]
   :value-type true)` has no instance fields other than `v`, and an array of
   `Cell` from `value-type.array` is flat.
5. Ramora, README, `doc/value-type-records.md`, docstring, `doc/API.md` and
   CHANGELOG agree with VT-D13 to VT-D15. The quality checks (format, lint,
   prose, API freshness, links) pass.
6. All CI jobs pass on the last commit that changes files outside the task
   directory.
