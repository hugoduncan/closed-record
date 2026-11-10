# Design steps

## Architecture review (2026-11-10)

- [x] Ramora pages outside `value-type-records/embodiment.md` that the change
      makes stale. Acceptance criterion 5 needs agreement in Ramora, but
      "Documentation" names only the embodiment, `value-type-records.md`
      and VT-R6. Name these pages too:
  - `defclosedrecord/decisions/type-structure.md`: D-01 has a "Value mode"
    note about the hash fields only. It must also name VT-D13 (the closed
    option, no hidden fields on value output). D-06 lists `IObj` and has
    no value-mode note. It must name VT-D14. D-07 and D-15 already have
    such notes.
  - `defclosedrecord/decisions/closed-key-set.md`: D-09 (hidden
    `__extmap`) has no value-mode note. It must name VT-D15.
  - `defclosedrecord/embodiment.md` ("Method behaviour" and "State":
    `-extra-keys`, `__meta`, `__extmap`, `withMeta`) and
    `embodiment/artifacts.md` (`define-checks`, if the value-mode
    exclusion of the metadata and hidden-key checks changes it): a
    pointer to the value-mode differences.
  - `value-type-records/invariants-and-verification.md`, VT-D10: the
    Valhalla job command lists only `-J--enable-preview`. VT-V12 also
    needs the `jdk.internal.value` export, which "CI" adds to `ci.yml`.
- [x] Keep the mode differences in one place. The embodiment
      (`defclosedrecord/embodiment/artifacts.md`) states that "one
      representation description holds the differences of the two modes"
      in `closed-record.impl.jvm`. The task adds new differences: the
      interface list (no `IObj`), the `meta` and `withMeta` methods, the
      constructor arguments of `assoc`, the `-extra-keys` body and the
      `:value-type/closed` option. State in "Emitter" that these go into
      the representation description, and that the shared method emitter
      and `base-interfaces` get no `value-type?` tests (VT-D8: one method
      set). The ordinary values of the description must give the current
      expansion (VT-I2).

## Ambiguity review (2026-11-10)

- [x] Say how VT-V12 runs "in the Valhalla job only". Both value-type jobs
      run all of `test/value-type` (VT-D10), so the test must select itself.
      Name the condition, for example the expected representation `:value`
      (`closed-record.test.representation`, `expectation/representation`).
      Also say what VT-V12 does when the `jdk.internal.value` export is
      missing or `value-type.value-classes` is `off` with the `:value`
      expectation: fail (recommended: the job command must give the export)
      or skip.
- [x] Define "no generated reference" in "Emitter" and VT-V2. The
      `defvaluetype*` field vector must contain `__meta __extmap ^int __hash
      ^int __hasheq` (VT-D5), and user methods (`specs*`) can name them.
      State that the rule and the VT-V2 check apply to the bodies of the
      generated methods and factories only (VT-I4), not to the field vector
      or to user methods.
- [x] Name the value-type links that change. "Other links to the value-type
      field contract" leaves open the `doc/value-type-records.md` link to
      the value-type array documentation (`doc/usage.md` at `edc3f79`),
      which is not the field contract. Say if it moves to the new SHA
      (recommended: yes, so no `edc3f79` link stays outside history). Also
      say where the new link to `doc/usage.md#deftype-forms` goes: the
      README, `doc/value-type-records.md`, or both.

## Inconsistency review (2026-11-10)

- [x] Make one pin rule. "Dependencies" pins only `4ed8d0a3…` and uses
      that SHA in all links. Acceptance criterion 1 and "Documentation"
      ("If the task pins a later commit, change both") permit a later
      commit. "If VT-V12 gives other results, change VT-K8" contradicts
      acceptance criteria 2 and 4 (VT-V12 passes; `Cell` is flat), and on
      `4ed8d0a3` with EA3 the results are already observed (VT-K8).
      Either remove the later-commit and VT-K8 clauses, or state one rule
      for a later pin: the pin, the links, Ramora (context, VT-R6, VT-K8,
      VT-V12) and acceptance criteria 1, 2 and 4 change together (VT-R7).
- [x] Make the list of value-mode exclusions agree with the shared checks
      and with Ramora VT-V3. For `checks/defclosedrecord.clj` the design
      names only "metadata, and `with-meta` keeps the extra keys". The file
      also has: `reader-literal-undeclared-key-test` (V-01; VT-V3 does not
      name a V-01 part), `equality-ignores-metadata-and-hidden-keys-test`,
      `extra-key-retention-test` (also `assoc` keeps the extra keys), the
      literal part of `check-test` (V-15, which VT-V3 names), and the
      metadata of `serializable-test` (identity fallback, so it runs in the
      Temurin value-type job). Name all of them, and add the V-01 part to
      VT-V3 in Ramora (Ramora first, as the design requires).
- [x] VT-V7 is not "no change". `test/value-type/.../aot_test.clj` reads
      `:meta (meta (with-meta p {:m 1}))` in the loaded subprocess; in value
      mode this gives `ClassCastException` (VT-D14). Move VT-V7 to the
      verification items that change: remove the metadata read, or check
      that the loaded type is not `IObj`.
