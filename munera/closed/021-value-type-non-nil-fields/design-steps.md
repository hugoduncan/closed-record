# Design steps

- [x] VT-D12: remove the claim that "a later version can add ordinary
  enforcement as an accretion". D-20 says that a change of behaviour is not
  an accretion. Today an ordinary record accepts nil in a `^:non-nil` field,
  so later enforcement is also a breaking change. State that ignoring
  `:non-nil` in ordinary mode is public behaviour with commitment high
  (D-20), and that ordinary enforcement can come only under a new name or
  option. Keep the rejection of macroexpansion errors on the D-20 basis.
- [x] Documentation, Ramora risks: replace "update the risks (VT-R1
  occurred)" with an update of VT-R1 in place to the current risk (the
  `defvaluetype*` contract can still change; signals now include VT-V6 and
  VT-V9). The Ramora document rules forbid status updates and history on
  design pages. Put any record of the `c366f15` defect only as technical
  evidence (for example, why VT-V9 reads through the lookup thunk), not as
  an event.
- [x] VT-I6: define the comparison. "Equals ... except for that field
  metadata" does not say whether the check also requires that `:non-nil`
  reaches `defvaluetype*`. A check that removes `:non-nil` everywhere passes
  when the emitter drops it. State the two parts: (a) the field symbols in the
  `defvaluetype*` field vector carry `:non-nil true` unchanged, and no other
  place in the expansion carries `:non-nil`; (b) after removal of `:non-nil`
  from that field metadata, the normalised printed expansion equals the
  expansion of the form without `:non-nil`.
- [x] VT-V10: state whether the expansion fixture form with `:non-nil`
  metadata is required. The public contract says that the ordinary expansion
  with `:non-nil` does not change (VT-I2), but "only if" makes the fixture
  form optional, and no other check verifies this claim. If it is required,
  say that it is recorded from the emitter at `f034647`, before any emitter
  change, and make acceptance criterion 3 agree.
- [x] Public contract and VT-V9 disagree on compiled constants. The contract
  lists "compiled constants" as a path that gives the host exceptions, but
  VT-V9 does not check that path, and "AOT checks beyond the existing VT-V7"
  are out of scope. A compiled constant comes from a record value that
  already exists, so nil cannot reach it except through a literal (already
  in VT-V9). Make the two agree: remove compiled constants from the
  contract's list of failing paths (keep them as a path where valid values
  work, or name value-type as owner), or add a check to VT-V9 that stays
  inside the frozen scope.
- [x] Intent: "On value output with an eligible tag, the JVM restricts the
  field" has two readings. Context uses "eligible" for any tag that
  `:non-nil` accepts (identity classes such as `String` too), but native
  null restriction applies only when the tag is a value class and the JVM
  supports native fields. With the first reading, the `^String ^:non-nil`
  field of the example has native restriction, which is false. Use the
  Context wording ("a value-class tag, on a JVM that supports native
  fields"), so that the README and Ramora "non-nil enforcement" table rows
  have one meaning.
- [x] VT-I6 has no verification plan item. Each Ramora invariant record
  names its check in `verified-by` with a VT-V id, and the VT-R1 signals
  are VT-V ids. The design defines the VT-I6 check (parts (a) and (b)) only
  inside the VT-I6 bullet, with "Verify in the value-type suite", and
  acceptance criterion 4 says only "VT-I6 is verified". Give the check a
  VT-V home, for example an extension of VT-V2 (Expansion, with
  value-type), and name it as VT-I6 `verified-by`, so that the Ramora
  record and the verification plan table agree.
- [x] Problem says "the design, the docs and VT-V6 say that `:non-nil` is
  rejected". No user doc says this: the README, `doc/`, the docstring and
  the CHANGELOG do not mention `:non-nil`. Only the Ramora pages
  (`ramora/value-type-records.md` context fact and public contract, the
  VT-V6 row) and the VT-V6 test (`errors_test.clj`, `non-nil-field-test`,
  reason `:non-nil-unsupported`) state it. Change the bullet to name these
  artifacts, so that the Documentation section stays the full list of doc
  changes (additions only, outside Ramora).

## Plan review: ambiguity

- [x] Steps "Run the value-type suite" (slices 1, 2, 3) do not name the JVM,
  the representation property or the Clojure 1.11 case. design.md says the
  new pin must not break the Clojure 1.11 case, but slice 1 does not run
  `test/value-type-clj-1.11`. plan.md Risks says "no local Valhalla JVM
  assumed", but a local Valhalla JDK exists
  (`~/.local/share/mise/installs/java/openjdk-27.0.0-jep401ea3+1-1`, the
  probe JVM). Name the commands from `doc/development.md`: identity
  (`-Dclosed-record.test.representation=identity`, Java 21 or 25), value
  output (`JAVA_CMD=<Valhalla java>`, `--enable-preview`,
  `representation=value`) and the Clojure 1.11 case. State which slices run
  each. Change the Valhalla risk to agree.
- [x] Slice 7 does not say how CI runs on the last commit. It has no push
  step, no branch, no push approval basis and no way to read the run. Also
  "Confirm all CI jobs pass" comes before "Commit slice 7", but the slice-7
  commit changes `mementum/state.md` (outside the task directory), so CI
  must run on that commit. Order the steps: commit, push, read the run,
  record the result. Name the branch (`020-value-type-records`, pull request
  #1) and the `gh run` commands, as task 020 steps did, and state whether
  the task 020 push approval (branch only, not `master`) applies.
- [x] Slices 5 and 6 give the cells of the non-nil enforcement row but not
  of the field storage type row. State the cells for Ordinary, Value output
  and Identity fallback. From design.md Context and the probe: Ordinary
  stores a non-primitive tagged field as `Object` and ignores `:non-nil`;
  in both value-type modes a `:non-nil` field stores the tag class and
  other fields store as in Ordinary. Use the same cells in the README and
  Ramora tables.

- [x] Steps slice 5 says "update the embodiment" but does not say what
  changes, and the plan expects no emitter change.
  `ramora/value-type-records/embodiment.md` has a stale id list: the Tests
  row says "VT-V1 to VT-V7" and names the test placements. Steps slices 2
  and 4 do not name the files of the VT-V9 and VT-V10 tests, so the
  embodiment cannot name them. Name the test files in slices 2 and 4 (for
  example a new `test/value-type/closed_record/value_type/non_nil_test.clj`,
  and the VT-V10 test in `test/runtime`). Then list the embodiment changes
  in slice 5: the Tests row (VT-V9 and VT-V10 with their files, the VT-V2
  extension, the `:non-nil` fixture form); the `closed-record.core` row
  (docstring sentence on `:non-nil`); the `doc/` and `README.md` row
  (non-nil fields); and `closed-record.impl.jvm` passes field metadata to
  `defvaluetype*` with no change (VT-I6).
- [x] Steps slice 6 says "a link to value-type's field contract" but does
  not give the Git ref of the link. A `master` link can change after the
  pin; a link at the pinned SHA agrees with the dependency example, as the
  `doc/performance.md` evidence link pins a SHA. State the target, for
  example
  `https://github.com/hugoduncan/value-type/blob/edc3f79de77bcf044d6234df361e090edc46c813/doc/usage.md#portable-non-nil-fields`,
  and state that a later pin change also changes this link.

## Plan review: inconsistency

- [x] Steps slice 5 narrows the Ramora public contract change to the
  pass-through example (line ~113). design.md Documentation says "update the
  public contract", and acceptance criterion 6 says that Ramora agrees with
  the design public contract. The Ramora public contract
  (`ramora/value-type-records.md`) has no statement on non-nil fields. Add a
  slice-5 step: put the design public contract statements in it (`:non-nil`
  has value-type's meaning; host exceptions with no `ex-data` on all paths;
  `map->Name` missing key gives `NullPointerException`; key-set errors come
  first; declaration errors pass through; ordinary mode ignores `:non-nil`;
  value-type owns compiled constants).
- [x] Two Ramora lists of ids become stale, and design.md and steps.md do not
  change them: (1) the index in `ramora/value-type-records.md` ("VT-D1 to
  VT-D10", "VT-I1 to VT-I5, VT-V1 to VT-V8"); (2) the job placement sentence
  after VT-D10 in `invariants-and-verification.md`. Steps slice 5 fixes only
  VT-V2 in that sentence, but design.md CI says that both value-type jobs run
  VT-V9 and VT-V2, and the runtime jobs run VT-V10. Add both changes to
  design.md Documentation and to steps slice 5.
  - Done in steps slice 5 and plan.md. design.md is not changed: this
    pass may change only plan.md, steps.md and implementation.md. The
    steps follow from design.md Documentation (it adds VT-D11, VT-D12,
    VT-I6, VT-V9, VT-V10) and design.md CI. A design pass can add the two
    items to design.md Documentation.
- [x] Slice order does not agree with the plan key decision "Record the
  expansion before any emitter change" and design.md VT-V10 ("from the
  emitter at `f034647`, before any emitter change"). Slice 2 (VT-V9) is
  where a necessary emitter change shows, but slice 4 records the
  `:non-nil` fixture form. After an emitter change in slices 2 or 3, slice
  4 records from the changed emitter. Move the fixture form and `record!`
  to slice 1 (before VT-V9), or add a slice-4 condition: if `src/` changed
  since `f034647`, record the new entry from the `f034647` emitter and
  record the diff in implementation.md.
- [x] plan.md CI and push says that the slice-7 record commit "changes only
  task files, so it needs no push or run". But the CI `Markdown links` job
  (`bb lychee:check`, `dev/link_check.clj`) checks every tracked `*.md`
  file, task files too, with online HTTP checks of bare URLs
  (`include_verbatim = false` skips only code). The record adds the pull
  request URL and the run URL. Add `bb lychee:check` before the record
  commit (or put the URLs in code spans), so that the record commit cannot
  fail the next CI run.

## Plan review, pass 3: ambiguity

- [x] Steps slice 5 says "VT-D11 and VT-D12 ... in the 8-tuple shape, with
  the bases and commitments from design.md", and "Add VT-I6" in the
  assertion record shape. design.md does not give all the fields:
  - VT-D11 has no commitment. The levels
    (`ramora/defclosedrecord/decisions.md`) give high for public behaviour,
    and VT-D11 fixes the public error contract (host exceptions, no
    `ex-data`).
  - VT-D11 and VT-D12 have no subject, assumptions or scope. design.md says
    "Basis", the shape says "rationale".
  - VT-I6 has no subject/predicate/object split, scope or confidence.
  - VT-R1 (steps slice 5) gives the signals but not the effect. Today the
    effect is "VT-D3 and VT-D5 change"; a `:non-nil` contract change also
    changes VT-D11 and VT-I6.
  State the missing values in steps slice 5 (or plan.md), so the
  implementer does not choose public commitment levels. For example:
  VT-D11 scope public API, JVM, commitment high; VT-D12 scope public API,
  all hosts; Basis → rationale; VT-D11 assumption `defvaluetype*` follows
  its field contract (VT-R1); VT-I6 scope all `:value-type true` types,
  confidence high.
  - Done in steps slice 5 (record field values) and plan.md (Key
    decisions, Ramora edits). design.md is not changed.
