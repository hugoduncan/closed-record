# Decisions

Each decision has this shape:
`⟨subject, choice, rejected, rationale, assumptions, qualities, scope, commitment⟩`.
The status of each decision is `intended`. An empty field is omitted only when it does not apply.

Commitment levels:

- low: internal detail. Change it freely.
- medium: internal mechanism. A change affects one emitter or one internal namespace.
- high: public behaviour or API. After the first release, a change is breaking (D-20).

## Index

- [Type structure](decisions/type-structure.md):
  D-01 type base, D-06 record identity, D-07 equality and hash, D-10 keyword lookup, D-15 syntax, D-21 method size.
- [Closed key set](decisions/closed-key-set.md):
  D-02 closed key set, D-04 `dissoc` of an undeclared key, D-05 `empty`, D-08 `map->Name`,
  D-09 reader literals and printing, D-19 missing primitive field.
- [Failure and validation](decisions/failure-and-validation.md):
  D-03 failure signal, D-11 value validation, D-12 no disable switch, D-18 `check` of other values.
- [Platform and evolution](decisions/platform-and-evolution.md):
  D-13 platform layout, D-14 Clojure version, D-17 `ClosedRecord` protocol, D-20 API compatibility.
- [Verification](decisions/verification.md): D-16 verification.
