# Decisions: closed key set

## D-02 Closed key set

- Subject: the paths that can change the key set.
- Choice: check the key set in `assoc`, `without`, `cons` and `map->Name`. `->Name` has fixed arity.
- Rejected: a separate checked `assoc` function. Users can still call `clojure.core/assoc`, so the key set stays open.
- Rationale: these are the only paths that can change the key set. `conj`, `merge`, `into`, `update` and `assoc-in` all use them.
- Assumptions: `clojure.core` map functions do not change a record by another path.
- Qualities: + correctness. The check costs nothing on a permitted path: the key dispatch that selects the result is the check (on the JVM, the `identical?` tests of `defrecord` `assoc`, D-21), and only the rejected path calls the throw helper.
- Scope: all hosts.
- Commitment: high.

## D-04 `dissoc` of an undeclared key

- Subject: `without` for a key that is not declared.
- Choice: return the record unchanged.
- Rejected: throw `:closed-record/undeclared-key`. Generic code such as `(dissoc m :cache/key)` then fails,
  but no key set changes.
- Rationale: the key cannot be present, so the result is correct. A plain map and `defrecord` do the same.
  O-01 is about changes to the key set. This is not a change.
- Assumptions: a typo in a `dissoc` key has no effect on the data.
- Qualities: + interop with generic map code. − A typo in a `dissoc` key is not found.
- Scope: all hosts.
- Commitment: high.

## D-05 `empty`

- Subject: `empty`.
- Choice: throw `:closed-record/empty`.
- Rejected: return `{}`. The type then changes silently.
- Rationale: a record without its declared keys does not exist. `defrecord` also throws.
- Scope: all hosts.
- Commitment: high.

## D-08 `map->Name`

- Subject: the map factory.
- Choice: an extra key throws `:closed-record/extra-keys`. A missing key gives nil (D-19 for primitive fields).
- Rejected: put extra keys in `__extmap`, as `defrecord` does. This breaks I-01.
- Rationale: `map->Name` is the explicit construction path. It must enforce the key set.
- Scope: all hosts.
- Commitment: high.

## D-09 Reader literals and printing

- Subject: printed form and record literals.
- Choice:
  - Print as `#ns.Name{...}`.
  - Read map literals and positional literals.
  - Extra keys in a map literal go into `__extmap`. No observable result depends on `__extmap` (I-02).
  - The public function `check` throws `:closed-record/extra-keys` if `__extmap` is not empty.
- Rejected:
  - Ignore the extra keys and give no way to find them.
  - Expose `__extmap` as `defrecord` does. This breaks I-01.
  - Positional printing only. It does not close the gap, because map literals read in all cases.
- Rationale: K-07 prevents a read-time check. With this choice, every operation keeps I-01, and the gap can be found.
- Assumptions: hand-written literals with extra keys are rare. Printed output never has extra keys.
- Qualities: + correctness of operations, + round trip of printed output. − Hand-written literal errors are not found
  automatically. − Print, code constants and positional reads drop the extra keys, and after that `check` passes (I-07).
- Scope: JVM. Other hosts decide when they are added.
- Commitment: high for printing and `check`. Medium for `__extmap`.

## D-19 Missing primitive field

- Subject: `map->Name` and map literals when a primitive-hinted field (for example `^long`) has no key.
- Choice: the same as `defrecord`. `create` unboxes nil, and the host throws (on the JVM, `NullPointerException`).
- Rejected: an `ex-info` check in `map->Name`. A map literal calls `create` directly, so the two paths then fail
  differently. The check is also value validation (D-11).
- Rationale: O-02 and K-04.
- Qualities: + parity, + minimality. − The error has no `ex-data`.
- Scope: JVM.
- Commitment: medium.
