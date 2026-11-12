# Changelog

## [Unreleased]

- Add the `defclosedrecord` option `:value-type true`. It defines the record
  as a value class through value-type (`org.hugoduncan/value-type` 0.1.127
  or later, which must be on the class path). A value record:
  - has only its declared fields and no metadata, so a record with at most
    64 bits of fields can flatten;
  - throws at read for a map literal with an undeclared key;
  - is non-nil in each field that has `^:non-nil` metadata and a class tag.

  `clojure.walk/macroexpand-all` of a value record throws value-type
  `:not-installed`. See `doc/value-type-records.md`.

## [0.1.190] - 2026-10-29

- Add `defclosedrecord`, a record that rejects undeclared keys and
  prevents removal of declared keys.
