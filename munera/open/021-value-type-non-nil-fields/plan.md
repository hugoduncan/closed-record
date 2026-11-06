# Plan

## Approach

Pass `:non-nil` field metadata through to `defvaluetype*` with no emitter
change. value-type owns the field contract; closed-record owns the key set.
The work is: a dependency pin, test changes, Ramora changes and
documentation changes.

Key decisions:

- Change the pin first. With the `8bb7c4f` pin, VT-V9 cannot pass; with the
  new pin (`edc3f79`), the current VT-V6 test fails until its expected
  reason changes to `:missing-tag`. So the pin update and the VT-V6 test
  change go in the same slice, to keep every commit green.
- No emitter change expected. The probe shows that field metadata already
  reaches `defvaluetype*` (`src/closed_record/impl/jvm.clj` puts field
  metadata only in the field vector; `plain-fields` strips it elsewhere).
  If a change proves necessary, record the reason in `implementation.md`
  and the Ramora embodiment; the change must not add a value check (VT-D11)
  and must not change the ordinary expansion (VT-I2).
- VT-I6 part (a) needs the unprinted expanded form. The fixture
  `expansion` returns a printed string; reuse `normalise` and
  `strip-positions` (or expose a helper) in the VT-V2 test.
- The VT-V10 fixture form: `record!` rewrites all of
  `expansion_fixture.edn` from `forms`. Add the `:non-nil` form, then
  verify by diff that the six existing entries are unchanged. Record the
  expansion before any emitter change: this is the first step of slice 1,
  before VT-V9 (slice 2) can show a need for an emitter change. `src/` has
  no change since `f034647`, so the current emitter is the `f034647`
  emitter; slice 1 confirms this with `git diff --quiet f034647 -- src`.
- Test files: VT-V9 in a new
  `test/value-type/closed_record/value_type/non_nil_test.clj`; the VT-I6
  check of VT-V2 beside `value-expansion-test` in
  `test/value-type/closed_record/value_type/representation_test.clj`;
  VT-V10 in `test/runtime/closed_record/value_type_option_test.clj` (the
  ordinary VT-V1 placement). The Ramora embodiment Tests row names these
  files.
- README link to value-type's field contract: at the pinned SHA, not
  `master`, as the `doc/performance.md` evidence link pins a SHA:
  `https://github.com/hugoduncan/value-type/blob/edc3f79de77bcf044d6234df361e090edc46c813/doc/usage.md#portable-non-nil-fields`.
  A later pin change also changes this link.
- Ramora edits follow the record shapes in
  `ramora/value-type-records/decisions.md` (8-tuple) and
  `invariants-and-verification.md` (assertion record). No status updates
  or history on design pages; record the `c366f15` defect only as
  technical evidence. Update VT-R1 in place. design.md does not give all
  record fields (subject, assumptions, scope, VT-D11 commitment, the VT-I6
  subject/predicate/object split, the VT-R1 effect). Steps slice 5 gives
  each value, so the implementer does not choose a commitment level.
- Ramora public contract: `ramora/value-type-records.md` has no statement
  on non-nil fields. Slice 5 adds the design public contract statements
  to it, not only the new pass-through example. Slice 5 also updates the
  id lists that the new records make stale: the index in
  `ramora/value-type-records.md` and the job placement sentence after
  VT-D10 in `invariants-and-verification.md`.
- Differences tables (README and Ramora) get two rows, with the same
  cells in both tables:

  | Behaviour | Ordinary | Value output | Identity fallback |
  |---|---|---|---|
  | Non-nil enforcement | none: `:non-nil` has no effect | native null restriction with a value-class tag on a JVM that supports native fields; otherwise constructor checks | constructor checks |
  | Field storage type | primitive tag: the primitive; other fields: `Object` | `:non-nil` field: the tag class; other fields: as Ordinary | `:non-nil` field: the tag class; other fields: as Ordinary |

  Source: design.md Context (value-type contract, "Enforcement" and
  "Enabled fields have storage of the tag class") and the probe (`c` is
  `Object` in both modes).

## Test commands

From `doc/development.md`. Run from the repository root.

- `<Temurin 21 java>`:
  `/Users/duncan/.local/share/mise/installs/java/temurin-21/bin/java`.
  CI uses Temurin 21 for all jobs except the Valhalla job.
- `<Valhalla java>`:
  `/Users/duncan/.local/share/mise/installs/java/openjdk-27.0.0-jep401ea3+1-1/bin/java`
  (`27-jep401ea3+1-1`, the probe JVM).

Commands:

- T-ID, value-type suite, identity fallback:
  `JAVA_CMD=<Temurin 21 java> clojure -J-Dclosed-record.test.representation=identity -M:test:value-type-test:clj-1.12 -m scry.cli --dir test/value-type`
- T-VAL, value-type suite, value output:
  `JAVA_CMD=<Valhalla java> clojure -J--enable-preview -J-Dclosed-record.test.representation=value -M:test:value-type-test:clj-1.12 -m scry.cli --dir test/value-type`
- T-11, Clojure 1.11 value-type case:
  `JAVA_CMD=<Temurin 21 java> clojure -M:test:value-type-test -m scry.cli --dir test/value-type-clj-1.11`
- T-RT, ordinary suite on Clojure 1.11, 1.12 and 1.13 (three commands):
  `JAVA_CMD=<Temurin 21 java> clojure -M:test -m scry.cli --dir test/runtime`,
  the same with `-M:test:clj-1.12` and with `-M:test:clj-1.13`.

Slices that run them:

| Slice | Commands |
|---|---|
| 1 (fixture form, pin) | T-ID, T-VAL, T-11, T-RT |
| 2 (VT-V9) | T-ID, T-VAL |
| 3 (VT-V2) | T-ID, T-VAL; also T-RT if the slice changes `test/runtime/closed_record/expansion_fixture.clj` |
| 4 (VT-V10 test) | T-RT |
| 7 (finish) | T-ID, T-VAL, T-11, T-RT, before the push |

Slices 5 and 6 change no code or test, except the slice-6 docstring; the
slice-7 runs and quality checks cover them.

## CI and push

- Acceptance criterion 7 needs CI on the last commit that changes files
  outside the task directory. That is the slice-7 commit: it changes
  `mementum/state.md` and any quality fixes.
- Branch: `020-value-type-records` (this worktree). Pull request #1
  (https://github.com/hugoduncan/closed-record/pull/1, base `master`) is
  open for that branch. `ci.yml` runs on `pull_request`, so a push of the
  branch runs CI. Do not open a new pull request. Do not push `master`.
- Push approval: the task 020 user approval (2026-11-03) let task 020
  push the branch `020-value-type-records` with no further approval, not
  `master`. This task does not assume that the approval applies to it.
  Stop and ask the user before the first push. After approval, a fix after
  a failed run is a new commit, pushed to the same branch.
- Order in slice 7: commit; ask for push approval; push
  (`git push origin 020-value-type-records`); read the run; record the
  result in implementation.md; run `bb lychee:check`; commit. The record
  commit changes only task files, so it needs no push or run. But the CI
  `Markdown links` job (`bb lychee:check`, `dev/link_check.clj`) checks
  every tracked `*.md` file, task files included, and checks bare URLs
  online. The record adds the pull request URL and the run URL, so the
  local `bb lychee:check` before the record commit makes sure that the
  record cannot fail the next CI run.
- Read the run: `gh run list --workflow ci.yml --branch
  020-value-type-records --commit <sha>` gives the run ID;
  `gh run watch <id> --exit-status` waits; `gh run view <id> --json jobs`
  gives each job conclusion. The run passes when every job has conclusion
  `success`, including `Value-type tests (Temurin 21)` and
  `Value-type tests (Valhalla)`.
- Record: the date, the branch, the commit SHA, the pull request URL, the
  run URL and each job conclusion. A failed run also gets its cause and
  the fix commit.

## Risks

- A value-type contract change before its release can invalidate the pin.
  Signals: VT-V2, VT-V6, VT-V9 (VT-R1).
- The Prerequisite defect (stack map resolution on Valhalla) can regress in
  a later value-type commit. The pin names `edc3f79`; VT-V9 reads non-nil
  fields through the lookup thunk, so it detects a return of the defect.
- Local Valhalla runs (T-VAL) use the probe JVM on macOS. CI uses the
  same build `27-jep401ea3+1-1` on Linux x64. The CI Valhalla job is the
  gate for the VT-V9 value-output checks (acceptance criterion 2).
- The new pin can break the existing value-type suite, the Clojure 1.11
  case or the ordinary suite. Acceptance requires all CI jobs green.

## Slice order

1. Fixture form, pin and VT-V6: first add the VT-V10 `:non-nil` form to
   the expansion fixture and record it from the `f034647` emitter; then
   update `deps.edn` and the README SHA; change the VT-V6 test to the
   `:missing-tag` example; run the suites.
2. VT-V9: non-nil field tests in the value-type suite.
3. VT-V2 extension: the VT-I6 expansion checks (parts (a) and (b)).
4. VT-V10: ordinary-mode nil acceptance test (the fixture form is in
   slice 1).
5. Ramora: decisions VT-D11 and VT-D12, invariant VT-I6, verification rows
   VT-V9 and VT-V10, changed VT-V2 and VT-V6 rows, context fact, public
   contract, differences table, index id lists, job placement sentence,
   embodiment ("Elements that change": Tests, `closed-record.core`,
   `closed-record.impl.jvm`, `doc/` and `README.md` rows), VT-R1.
6. Documentation: README, docstring, `doc/API.md`, `CHANGELOG.md`.
7. Finish: `mementum/state.md`, all test commands, quality checks; commit;
   push after user approval; CI green on the pushed commit; record the
   run.
