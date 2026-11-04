# Design steps

- [x] Architecture: state the dependency boundary of VT-V8 in `dev/bench`.
  `dev/bench` is on the `:test` class path, and the default `:bench` run
  (Clojure 1.11.4, V-11, S-03 pass rule and exit code) has no value-type.
  The Dependencies section names only `closed-record.core`,
  `closed-record.runtime` and the ordinary test suite. Add: the VT-V8 code
  loads value-type only in the VT-V8 run (a separate entry namespace or
  case selection), the default `:bench` run and its V-11 result do not
  change, and the `:test` alias does not load value-type through
  `dev/bench`. This applies O-VT3 and VT-I2 to the bench boundary.
- [x] Architecture: complete the `ramora/defclosedrecord/embodiment.md`
  update so that the embodiment stays current (Ramora document rules).
  The Documentation scope names only the alias, suite, CI job and
  permitted dependencies. After the change these statements are also
  false or incomplete: the Allocation option check ("options other than
  `:no-print` and `:load-ns`"); the Generated type, Method behaviour
  (`hasheq`, `hashCode` "Cached") and State (`__hash`, `__hasheq` mutable
  cache) sections, which describe only the ordinary mode and must point
  to the value mode in `ramora/value-type-records/embodiment.md`; and the
  `dev/bench/` artifact row (VT-V8).
- [x] Ambiguity: state how a value-type check knows the representation that
  it must expect. The same shared checks run on Temurin 21 (identity) and on
  Valhalla (value). The differences-table assertions "for the representation
  under test", and the VT-V4 rule "the check fails if the Valhalla job gives
  identity output", need a source of truth. Detection of the JVM capability
  alone cannot catch silent fallback. Choose and record one: an explicit
  test-side expectation (system property or environment variable that the
  job sets), a separate value-output test namespace that only the Valhalla
  command runs, or `-Dvalue-type.fallback=error` in the Valhalla job. A
  test-side expectation is not a library switch (VT-D1); state this.
- [x] Ambiguity: state how the checks that need their own JVM configuration
  run. VT-V5 needs `-Dvalue-type.fallback=error`, which breaks the identity
  checks of VT-V3 in the same JVM. The Clojure 1.11 case of VT-V1 needs
  value-type on the class path with Clojure 1.11.4, where every other
  value-type check fails. For each case, state if it runs in a subprocess
  JVM from a test, or as a separate test directory or namespace with its own
  command (and CI step), and state the alias combination of each command.
- [x] Ambiguity: define "equal to the current expansion" (VT-I2, acceptance
  criterion 2). The emitter uses `gensym` (`p__`, `this__`, `c#`, `h#`), so
  two expansions are not `=`. State the reference (for example, a fixture
  recorded from the emitter at `ac25942`, or the expansion of no option
  against `:value-type false`) and the comparison (for example, equality
  after gensym normalisation). Also state where the "does not load
  `value-type.core`" check runs: in the ordinary suite value-type is not on
  the class path, so the check there cannot fail; the Temurin value-type job
  has value-type on the class path.
- [x] Ambiguity: name "the existing bench structure" for VT-V8. Two
  structures exist: the criterium fork harness `closed-record.bench`
  (`:bench` alias, 5 forks, S-03 pass rule, exit code) and the Java probes
  in `dev/bench/valhalla/` (task 019). Name the one to use and the alias
  combination of the VT-V8 command (value-type, `:clj-1.12`, Valhalla
  `JAVA_CMD`, `--enable-preview`).
- [x] Inconsistency: the CI section puts the identity part of VT-V7 in the
  Temurin 21 value-type job. The VT-D10 placement paragraph in
  `ramora/value-type-records/invariants-and-verification.md` names only
  VT-V1, VT-V2, VT-V3, VT-V5 and VT-V6 for Temurin 21 jobs, and only the
  value-output part of VT-V7 for the Valhalla job. Add this paragraph to the
  Documentation update list for that page, so that it names the identity
  part of VT-V7 in the Temurin 21 job.
