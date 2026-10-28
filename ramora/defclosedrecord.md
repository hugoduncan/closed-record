# defclosedrecord Design

This is the architecture description of `defclosedrecord`. It uses the architecture model in the
`hugoduncan/architecture` repository (file `ramora/INDEX.md`):

```text
Purpose + Context → Architecture → Embodiment → Observed qualities
```

Read this file first. Then read only the documents that you need.

- [Purpose](defclosedrecord/purpose.md): mission, outcomes, capabilities, scope, behaviour, value, success criteria.
- [Context](defclosedrecord/context.md): users, interacting systems, constraints K-01 to K-10, evidence E-01 to E-07.
- [Principles](defclosedrecord/principles.md): principles that the decisions apply.
- [Decisions](defclosedrecord/decisions.md): index of architectural decisions D-01 to D-21.
- [Embodiment](defclosedrecord/embodiment.md): artifacts, boundaries, generated type, methods, state, allocation.
- [Invariants](defclosedrecord/invariants.md): invariants I-01 to I-09 as assertion records, with enforcement and verification.
- [Qualities](defclosedrecord/qualities.md): quality targets, trade-offs, verification plan V-01 to V-15, risks.

Compiler evidence is in [context](defclosedrecord/context.md#evidence).
Current implementation and verification limits are in
[embodiment](defclosedrecord/embodiment.md).

## Summary

- `defclosedrecord` has the same syntax as `defrecord`.
- The generated type is a map with a fixed key set.
- `assoc` of an undeclared key throws.
- `dissoc` of a declared key throws.
- Reads are not checked. Use `:keys!` for that.
- Equality and hash are the same as `defrecord`.
- The type uses the `defrecord` field layout through `deftype*`, and it does not implement `IType`.
  Thus metadata, hash caching, map literals and code constants work.
- One gap: a hand-written map literal with extra keys reads without error. The extra keys are hidden. `check` finds them.
