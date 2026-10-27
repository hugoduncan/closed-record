# Changelog

## [Unreleased]

- Add shared formatting commands (`bb clj-fmt check|fix [PATHS...]`,
  `bb clj-fmt:check`, and `bb clj-fmt:fix`) and lint commands
  (`bb clj-kondo:copy-config` and `bb clj-kondo:lint`).
- Add `bb vale` to check maintained prose and a generated
  [API reference](doc/API.md). Use `bb api-docs` to regenerate it and
  `bb api-docs --check` to check freshness without writes.
- Add staged-content formatting and lint hooks. Repairs stop commits for review
  and manual staging; hooks preserve unstaged edits and never stage changes.
- Expand CI quality checks with the shared format/lint commands, Vale, API
  freshness, and tooling tests. See the [contributor guide](README/quality-checks.md)
  for setup, commands, and hook recovery. Release publication behavior is unchanged.

- Add `bb lychee:check` and a Markdown links CI check for tracked Markdown.

- Add `defclosedrecord`, a record that rejects changes to its declared keys.
- Add source-only JAR packaging with an EPL-2.0 license.
- Add a local `prepare` command that validates release notes and commits a dated
  changelog section for the next Git-count version.
- Add a manual release workflow with checks and a dry run by default.
- Add Clojars deployment and GitHub Release publication with release notes and
  JAR/POM assets when publication is explicitly enabled.
