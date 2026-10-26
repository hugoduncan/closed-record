# Design follow-ups

- [x] Define the minimal changelog contract for preparation and note selection.
  Specify the Unreleased and dated version heading forms, what counts as
  nonempty release notes, and what makes notes invalid or duplicate. State how
  these rules apply to the empty Unreleased section left by preparation and to
  prior version sections. The design requires rejection before modification
  and forbids fallback from an invalid matching section; these checks need
  explicit rules so the command and its tests use the same contract. Keep this
  within the existing small notes/preparation scope, not a general Markdown parser.
