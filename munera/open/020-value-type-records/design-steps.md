# Design steps

- [ ] Architecture: state the dependency boundary of VT-V8 in `dev/bench`.
  `dev/bench` is on the `:test` class path, and the default `:bench` run
  (Clojure 1.11.4, V-11, S-03 pass rule and exit code) has no value-type.
  The Dependencies section names only `closed-record.core`,
  `closed-record.runtime` and the ordinary test suite. Add: the VT-V8 code
  loads value-type only in the VT-V8 run (a separate entry namespace or
  case selection), the default `:bench` run and its V-11 result do not
  change, and the `:test` alias does not load value-type through
  `dev/bench`. This applies O-VT3 and VT-I2 to the bench boundary.
- [ ] Architecture: complete the `ramora/defclosedrecord/embodiment.md`
  update so that the embodiment stays current (Ramora document rules).
  The Documentation scope names only the alias, suite, CI job and
  permitted dependencies. After the change these statements are also
  false or incomplete: the Allocation option check ("options other than
  `:no-print` and `:load-ns`"); the Generated type, Method behaviour
  (`hasheq`, `hashCode` "Cached") and State (`__hash`, `__hasheq` mutable
  cache) sections, which describe only the ordinary mode and must point
  to the value mode in `ramora/value-type-records/embodiment.md`; and the
  `dev/bench/` artifact row (VT-V8).
