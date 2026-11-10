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

## Plan ambiguity review (2026-11-10)

- [x] Decide the value-mode exclusion mechanism in `plan.md`; do not leave
      it to implementation time. Some exclusions are parts of a test, not
      whole tests: the `:with-meta` step is inside the `gen-step` generator
      of `checks/differential.clj`; the literal part of `check-test`; the
      metadata part of `serializable-test`; the reader metadata of
      `value-constant-test`. A list of test names to skip cannot exclude
      them. The checks need an in-source condition. State its form
      (recommended: a placeholder in `closed-record.checks.support`, for
      example `metadata?`, that `define-checks` replaces, as it replaces
      `support/representation`, which `checks/defclosedrecord.clj` already
      uses in `(when (= :identity support/representation) …)`), and its
      source: the `:record-opts` (`:value-type true`) or a new
      `define-checks` key. Update steps.md Slice 3 to match.
- [x] State the expected result of the Temurin 21 value-type run at the end
      of Slice 2. After the emitter change, the shared checks that use
      metadata or undeclared literal keys, `aot_test.clj` (`with-meta`) and
      `non_nil_test.clj` ("with-meta keeps the fields") fail until Slice 3.
      The plan risk "run the Temurin 21 value-type suite after each slice"
      expects a pass. Choose one: do the exclusions, the VT-V7 and the
      VT-V9 metadata removals before the emitter change (recommended: each
      slice then ends green), merge Slices 2 and 3, or list the expected
      failures.
- [x] Name the test files and the parts of the new checks. The design left
      the files of VT-V11 and VT-V12 to the plan; the plan and steps.md do
      not name them. Name the file for VT-V11 and for VT-V12 (new files in
      `test/value-type/closed_record/value_type/`, or existing ones), and
      list the VT-V11 parts (a) to (e) in steps.md. Say where the VT-V4
      field check goes (recommended: the `:value` branch of
      `representation-test` in `representation_test.clj`) and that it runs
      on value output only. Say that the VT-V12 `[^long x]` fixture with the
      VT-I7 field check is the evidence for acceptance criterion 4, or add a
      `Cell` check. The VT-V12 file loads on Temurin 21 too: say if it
      requires `value-type.array` at namespace load or only in the `:value`
      branch.
- [x] Define the scope of the VT-V2 check in Slice 2. steps.md says "no
      hidden-field references in generated bodies". The current
      `value-expansion-test` walks only the `specs` of the `defvaluetype*`
      form. Ramora VT-V2 (VT-I4) also covers the factory bodies (`->Name`,
      `map->Name`), which are outside that form. State that the check walks
      the generated methods (the fixture form has no user methods) and the
      two factory `defn` forms, and that the `IMeta`/`IObj` check reads the
      `:implements` vector.

## Plan inconsistency review (2026-11-10)

- [x] Make the Slice 5 embodiment step agree with the design and with
      `ramora/value-type-records/embodiment.md`. steps.md says to remove
      "the export and VT-V7 entries" of "Differences from the design". The
      design says to remove the differences "when they no longer apply".
      The embodiment section has an `edc3f79` pin sentence and six entries,
      all "until task 022": no `:value-type/closed` option (VT-I7, VT-I8);
      `IMeta`/`IObj` (VT-I9); literals with undeclared keys; VT-V3, VT-V7
      and no VT-V11/VT-V12; no export (VT-D10); README and
      `doc/value-type-records.md`. After the task, none applies. Change the
      step: remove the whole section (or keep only the differences that
      still apply), and update the `closed-record.impl.jvm` and Tests rows
      of "Elements that change" (today: `withMeta` "with no hash fields";
      no VT-V11 or VT-V12).
- [x] Bring `munera/plan.md` and `mementum/state.md` into agreement with
      the task files. Both say that the next step is the plan ("Next:
      plan"; "Task 022, ready for a plan"). `plan.md` and `steps.md` exist
      (`b83677fd`) and are in review. Change "Next" to the current step.
      Slice 5 then sets the final state.

## Plan ambiguity review, pass 2 (2026-11-10)

- [x] State the value-mode form of `value-constant-test` and the shape of
      the private macro in `checks/literal.clj`. The reader metadata is
      inside the literal string (`"^{:m 1} #$P.T{:a 1 :n 2}"`). In value
      mode the reader throws `IllegalArgumentException` ("Metadata can only
      be applied to IMetas") when the `constant` macro expands, because `T`
      is not `IMeta`. The plan macro "gives the guarded forms only when it
      is `true`". If it guards only the `meta` assertion, the `let` still
      reads the string and fails. If it guards the whole `let`, the test
      has no assertion and scry fails it. Choose one:
      (a) a two-branch macro that gives the metadata literal and the `meta`
      assertion in ordinary mode, and the literal without metadata
      (`"#$P.T{:a 1 :n 2}"`) in value mode;
      (b) split the test: a constant without metadata in both modes, and a
      guarded constant with metadata.
      Recommended: (a), so the ordinary test does not change.
      Either way, value mode keeps the V-05 value constant check (VT-K7:
      constants come from `create` with the Map view), so the test has
      assertions. Also state that the macro wraps
      `extra-key-constant-test` at top level (a guard in the test body
      gives a test with no assertion). Update steps.md Slice 2 to match.

## Plan ambiguity review, pass 3 (2026-11-10)

- [x] State how VT-V11 (c) calls the main constructor and how it identifies
      the `IllegalArgumentException`. Ramora VT-V11 says "called through
      reflection"; `plan.md` and steps.md Slice 3 repeat it. A record has
      three constructors: the declared fields (`a b`), with `__meta
      __extmap`, and the main constructor with all six (`__hash __hasheq`
      are `int`). Java reflection (`Constructor/newInstance`) wraps the
      constructor exception in `InvocationTargetException`, and itself
      throws `IllegalArgumentException` for a wrong argument count or type.
      Thus a test that only looks for `IllegalArgumentException` (top level
      or in the cause chain) can pass with no constructor check. Choose the
      call and the assertion, for example: get the six-parameter
      constructor with `getConstructor`; call `newInstance`; the exception
      is `InvocationTargetException` with an `IllegalArgumentException`
      cause. Say if the `__meta __extmap` constructor is also called (`create`
      uses it, VT-K7). Also call the same constructor with nil for both, and
      expect a record, so a wrong descriptor fails the test. Update
      `plan.md` (the VT-V11 item) and steps.md Slice 3 (c). Ramora does not
      change.

## Plan inconsistency review, pass 3 (2026-11-10)

- [x] Make the Slice 1 `edc3f79` check agree with the Slice 5 embodiment
      step. steps.md Slice 1 says to verify that no `edc3f79` stays outside
      Munera, mementum and Git history (`git grep edc3f79`). Slice 1 changes
      only `deps.edn`, the README and `doc/value-type-records.md`. The
      sentence "The pinned value-type (`edc3f79`) has no closed record
      shape" in `ramora/value-type-records/embodiment.md` ("Differences
      from the design") stays until Slice 5 removes the section. Thus the
      Slice 1 check fails, and from Slice 1 to Slice 5 the embodiment names
      a pin that is not the pin. Choose one: in Slice 1, change that
      sentence to the new pin (value-type `4ed8d0a3` has the closed shape;
      closed-record does not use it yet), so the check passes in Slice 1
      (recommended: the embodiment then states the current state after
      each slice); or move the `git grep edc3f79` check to Slice 5 or
      Slice 6. Update steps.md (and `plan.md` "Slice order" item 1 if
      Slice 1 changes the embodiment).

## Plan inconsistency review, pass 4 (2026-11-10)

- [x] Make the Slice 5 list of "Elements that change" rows agree with the
      design and with the Slice 5 edits. The design says to "update the
      elements" of `ramora/value-type-records/embodiment.md`. steps.md
      Slice 5 names only the `closed-record.impl.jvm`, Tests and `ci.yml`
      rows. Slice 5 also changes two other elements, and their rows then
      do not state the current state:
  - `closed-record.core`: the row says that the docstring describes the
    option and "has one sentence on `:non-nil`". Slice 5 adds a sentence
    on metadata and map literals in value mode.
  - `doc/` and `README.md`: the row lists "the option, the differences
    table, the runtime dependency, the hash cost and non-nil fields".
    Slice 5 adds: no metadata, map literals with undeclared keys,
    `value-type.value-classes` and flattening (VT-K8).
      Add both rows to the Slice 5 embodiment item in steps.md. Do not
      change `design.md` or the Ramora design pages.
