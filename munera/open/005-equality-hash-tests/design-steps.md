# Design steps

- [x] Ambiguity: metadata cases (Scope 4, Scope 5). The second table in Context has the row
      `r` and `(with-meta r {:m 1})`. That row is not a plain-map case. State if Scope 4 includes it or if only
      Scope 5 tests it. In Scope 5, state how "two equal values with different metadata" are made, so that the case
      differs from `r` and `(with-meta r {:m 1})`. For example: two values from separate factory calls, each with
      `with-meta` and different maps (`{:m 1}`, `{:m 2}`), and no `hash` call before the comparison.
- [x] Ambiguity: cross-type directions (Scope 6). Scope 4 names both directions for `=` and `.equals`. Scope 6 does
      not. State if Scope 6 asserts `(= (->E 1 2) (->DE 1 2))` and `(.equals (->E 1 2) (->DE 1 2))` only, or also
      the reverse calls. The reverse calls run `defrecord` code, not emitter code, so Acceptance 3 then needs a reason
      for each of them in `implementation.md`.
