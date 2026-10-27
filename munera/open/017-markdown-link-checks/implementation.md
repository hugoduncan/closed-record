# Implementation notes

## 2026-10-27 — Architecture review

- No architectural review feedback.
- `ramora/META.md` and `doc/architecture.md` are absent. Used `AGENTS.md`
  and the architecture linked from `ramora/INDEX.md`, particularly
  `ramora/defclosedrecord/principles.md` and
  `ramora/defclosedrecord/embodiment.md`. Checked current CI and dependency
  boundaries against `.github/workflows/ci.yml` and `deps.edn`.

## 2026-10-27 — Ambiguity review

- No ambiguity review feedback.

## 2026-10-27 — Inconsistency review

- No inconsistency review feedback.

## 2026-10-27 — Review handoff

- Keep link repairs separate from check execution: implementation can correct
  existing links, but `bb lychee:check` must not rewrite documents. These
  requirements were reviewed as complementary, not conflicting.

## 2026-10-27 — Planning evidence and review

- Selected installed Babashka `1.13.225` and lychee `0.24.2`; both version
  commands exited 0. GitHub release metadata confirms exact-version archives
  and SHA-256 sidecars for Linux x86-64 and macOS arm64/x86-64.
- Temporary lychee probes passed relative paths, a target with spaces,
  repository-root paths, directories, same-document headings, and duplicate
  headings (exit 0, five links checked). With
  `include_fragments = "anchor-only"`, a missing cross-document heading
  returned exit 2 and named the source, line, target, and missing fragment.
  A separate offline same-document missing-heading probe also returned 2.
  Temporary directories were removed; these are planning probes, not task
  acceptance evidence.
- Lychee's comma-separated `--scheme http,https,file` silently excluded all
  probe links. Repeated `--scheme` flags worked; use the TOML scheme array.
  Fragment validation is off by default. An offline fragment pass followed
  by an online non-fragment pass avoids imposing external fragment checks.
- Confirmed `DeLaGuardo/setup-clojure@13.4` accepts `bb`, and the installed
  Babashka can load `org.httpkit.server` (exit 0). Archive installation and
  hosted CI execution remain unverified.
- Plan ambiguity review: no actionable findings. Read `steps.md` without
  changing it during review. Exact pins, policy ownership, fixture command,
  CI placement, failure behaviour, and maintainer-decision conditions are set.
- Design inconsistency review: no actionable findings within `design.md` or
  against current CI, development docs, scry's task/CI reference, and task
  014. Task 017 explicitly permits Babashka; scry supplies a named-task
  pattern, not its permissions or mise setup. The stable design is unchanged.
