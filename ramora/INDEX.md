# Ramora Index

Index of project documentation. Read this file first. Read the other files only when you need them.

## Document rules

Design pages describe the current design: purpose, constraints, decisions, and
required behaviour. Keep technical evidence and decision rationale. Do not add
implementation progress, completed-task summaries, or superseded status updates.

Each design's `embodiment.md` describes its current implementation. Record any
difference between implementation and design, and any verification limit, only
there. Update these statements in place; do not append progress history. An
embodiment can link to separate artifact maps and supporting evidence.
Historical work records stay in Munera and Git history.

## Entries

- [defclosedrecord design](defclosedrecord.md): architecture of `defclosedrecord`.
  Contains purpose, context, principles, decisions, embodiment, invariants and qualities.
- [Release workflow design](release-workflow.md): release architecture.
  Covers simple scry-style deployment, manual verification/publication, and recovery.
- [Development quality checks](development-quality.md): shared command and scope
  ownership, generated API contracts, and staged-content hook boundaries.
