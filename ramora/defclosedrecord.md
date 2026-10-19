# defclosedrecord Design

This is the architecture description of `defclosedrecord`. It uses the model in
`~/projects/hugoduncan/architecture/ramora/INDEX.md`:

```text
Purpose + Context → Architecture → Embodiment → Observed qualities
```

Read this file first. Then read only the documents that you need.

- [Purpose](defclosedrecord/purpose.md): mission, capabilities, scope, behaviour, success criteria.
- [Context](defclosedrecord/context.md): users, interacting systems, constraints.
- [Decisions](defclosedrecord/decisions.md): architectural decisions D-01 to D-16.
- [Embodiment](defclosedrecord/embodiment.md): artifacts, generated type layout, interfaces.
- [Invariants](defclosedrecord/invariants.md): invariants as assertion records, with enforcement points.
- [Qualities](defclosedrecord/qualities.md): quality targets, trade-offs, verification, risks.

- Assertions marked `observed` come from two sources (2026-10-19):
  - tests on Clojure 1.11.4,
  - the Clojure 1.13.0-alpha6 source (`Compiler.java`, `LispReader.java`, `core_deftype.clj`, `RT.java`).

## Summary

- `defclosedrecord` has the same syntax as `defrecord`.
- The generated type is a map with a fixed key set.
- `assoc` of an undeclared key throws.
- `dissoc` of a declared key throws.
- The type uses the `defrecord` field layout through `deftype*`. Thus metadata, hash caching, map literals and code constants work.
- One gap: a hand-written map literal with extra keys reads without error. The extra keys are hidden. `check` finds them.
