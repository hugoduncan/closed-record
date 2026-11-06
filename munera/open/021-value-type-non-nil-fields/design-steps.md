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
