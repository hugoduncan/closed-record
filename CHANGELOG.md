# Changelog

## [Unreleased]

- Add `defclosedrecord`, a record that rejects changes to its declared keys.
- Add source-only JAR packaging with an EPL-2.0 license.
- Add a local `prepare` command that validates release notes and commits a dated
  changelog section for the next Git-count version.
- Add a manual release workflow with checks and a dry run by default.
- Add Clojars deployment and GitHub Release publication with release notes and
  JAR/POM assets when publication is explicitly enabled.
