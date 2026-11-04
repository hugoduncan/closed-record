# Changelog

## [Unreleased]

- Add the `defclosedrecord` option `:value-type true`. It defines the record
  as a value class through value-type, which must be on the class path. See
  the README section "Value-type records".

## [0.1.190] - 2026-10-29

- Add `defclosedrecord`, a record that rejects undeclared keys and
  prevents removal of declared keys.
