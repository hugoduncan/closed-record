# Decisions: failure and validation

## D-03 Failure signal

- Subject: the error for a key-set failure.
- Choice: `ex-info` with data `{:type t, :record <class>, :key k}`, or `:keys ks` for a set of keys.
- `t` is one of:
  - `:closed-record/undeclared-key`
  - `:closed-record/declared-key-dissoc`
  - `:closed-record/empty`
  - `:closed-record/extra-keys`
  - `:closed-record/not-closed-record` (D-18)
- Rejected: `IllegalArgumentException` and `UnsupportedOperationException`. They do not carry data.
- Rationale: callers can dispatch on `:type`. Expectation: errors have clear data.
- Qualities: + diagnosability, + portability (`ex-info` exists on all hosts).
- Scope: key-set failures and `check`. Other failures (value casts, D-19, `java.util.Map` mutators) keep the host
  exception, as `defrecord` does.
- Commitment: high. The `:type` values and data keys are public API.

## D-11 Value validation

- Subject: validation of field values.
- Choice: none. Tests show that the type works with malli map schemas and with nil field values.
- Rejected: `:pre` checks or a built-in schema. They duplicate malli and spec.
- Rationale: K-04. Validation is a separate concern.
- Scope: all hosts.
- Commitment: high (the API has no validation hooks).

## D-12 No disable switch

- Subject: run-time configuration of the checks.
- Choice: the checks always run.
- Rejected: a dynamic var or a compile-time flag that disables the checks.
- Rationale: they cost nothing on permitted paths (D-02). A switch adds variability and allows different behaviour in test and in production.
- Scope: all hosts.
- Commitment: high.

## D-18 `check` of other values

- Subject: `(check x)` when `x` is not a closed record.
- Choice: throw `:closed-record/not-closed-record` with `{:type t, :value-class <class>}`.
- Rejected:
  - Return `x`. A call on the wrong value then looks like a successful check.
  - Let protocol dispatch fail. The error is host-specific and has no data.
- Rationale: O-01. A wrong call fails at its location.
- Qualities: + explicitness. − `check` cannot walk mixed data. The user filters with `record?` or a predicate.
- Scope: all hosts.
- Commitment: high.
