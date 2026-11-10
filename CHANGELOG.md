# Changelog

## [Unreleased]

- Add the `defclosedrecord` option `:value-type true`. It defines the record
  as a value class through value-type, which must be on the class path. See
  the README section "Value-type records".
- With `:value-type true`, a field with `^:non-nil` metadata and a class
  tag is non-nil, through value-type. Nil gives `NullPointerException`
  and a wrong type gives `ClassCastException`. Without the option, the
  metadata has no effect. See the README part "Non-nil fields".
- With `:value-type true`, a record has no metadata, and a map literal
  with an undeclared key throws at read. A value record has only its
  declared fields, so a record with at most 64 bits of fields can
  flatten. See the README part "Arrays and flattening".
- With `:value-type true`, a full expansion outside compilation (for
  example `clojure.walk/macroexpand-all`) throws value-type
  `:not-installed` on all JVMs. `macroexpand-1` does not throw.

## [0.1.190] - 2026-10-29

- Add `defclosedrecord`, a record that rejects undeclared keys and
  prevents removal of declared keys.
