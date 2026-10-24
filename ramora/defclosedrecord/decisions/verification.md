# Decisions: verification

## D-16 Verification

- Subject: how the design is verified.
- Choice: the verification plan V-01 to V-15 in [qualities](../qualities.md#verification-plan). It includes:
  - Tests for each row of the behaviour table.
  - Differential tests against `defrecord`, including equality and hash.
  - Tests for malli and nil values.
  - Print and read round-trip tests.
  - Tests for code constants.
  - Tests that fix the observed compiler rules K-06 to K-08.
  - criterium benchmarks against `defrecord`.
- Each invariant names its tests in `verified-by`.
- Rejected: example tests only. They do not show parity with `defrecord` or catch compiler changes.
- Rationale: S-01 to S-04. R-01 needs an early signal on each Clojure version.
- Scope: the test suite runs on Clojure 1.11, 1.12 and 1.13 (aliases). The bench runs on Clojure 1.11.
  GitHub CI runs all three test aliases on Ubuntu with Temurin Java 21 for
  pushes to `master` and pull requests. Separate jobs check formatting and
  lint, and run focused build tests plus the JAR build. Performance benchmarks
  remain manual; benchmark-support tests are part of the default suite.
- Commitment: low.
