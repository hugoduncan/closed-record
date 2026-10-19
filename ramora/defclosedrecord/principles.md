# Principles and patterns

Principles from the architecture model catalog that the decisions apply.

| Principle | Application | Decisions |
|---|---|---|
| 16 Robustness through constrained state | The key set of an instance cannot change. | D-02, D-05, D-08 |
| 3 Information hiding | `__extmap` is hidden. Only `check` gives access to it. | D-09, D-17 |
| 9 Minimality | Two public vars. No validation, no switch, no options. | D-11, D-12, D-15 |
| 12 Explicitness | Failures give `ex-info` data. `check` of a wrong value throws. | D-03, D-18 |
| 15 Substitutability | A closed record replaces a `defrecord` at correct call sites. | D-06, D-07, D-15, D-19 |
| 6 Explicit boundaries | Public and internal names. Forbidden dependencies. | D-13, D-17 |
| 7 Dependency direction | `core` → `impl.*`. No `impl` → `core`. | D-13 |
| 25 Compatibility and evolvability | Accretion only after the first release. | D-20 |
| 28 Fit to context | The design follows the compiler rules K-06 to K-08. | D-01, D-06 |

Patterns: no pattern in the catalog applies. The design is one type-level abstraction that a macro generates.
